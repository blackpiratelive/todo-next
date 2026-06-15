package com.todonext.planify.data.remote

import com.todonext.planify.data.local.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

data class CalDavTodo(
    val href: String,
    val eTag: String,
    val uid: String,
    val summary: String,
    val completed: Boolean,
    val due: Long?,
    val priority: Int,
    val description: String? = null,
    val categories: String? = null
)

data class PutResult(val href: String, val eTag: String?)

class CalDavClient(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) {

    private val xmlMediaType = "application/xml; charset=utf-8".toMediaType()
    private val icalMediaType = "text/calendar; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val authenticated = original.newBuilder()
                .header("Authorization", Credentials.basic(username, password))
                .build()
            chain.proceed(authenticated)
        }
        .build()

    /**
     * Discovers a VTODO-supporting calendar URL on the Nextcloud server.
     *
     * Flow:
     * 1. PROPFIND on user principal to get current-user-principal
     * 2. PROPFIND on principal to get calendar-home-set
     * 3. PROPFIND on calendar-home-set to find a calendar supporting VTODO
     */
    suspend fun discoverCalendarUrl(): String? = withContext(Dispatchers.IO) {
        try {
            // Step 1: Get user principal
            val principalUrl = "${serverUrl}/remote.php/dav/principals/users/$username/"
            val principalBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <d:propfind xmlns:d="DAV:">
                    <d:prop>
                        <d:current-user-principal />
                    </d:prop>
                </d:propfind>
            """.trimIndent()

            val principalResponse = executePropfind(principalUrl, principalBody, depth = "0")
            val principalHref = extractTextContent(principalResponse, "current-user-principal", "href")
                ?: return@withContext fallbackCalendarUrl()

            // Step 2: Get calendar-home-set
            val resolvedPrincipalUrl = resolveUrl(principalHref)
            val calHomeBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                    <d:prop>
                        <c:calendar-home-set />
                    </d:prop>
                </d:propfind>
            """.trimIndent()

            val calHomeResponse = executePropfind(resolvedPrincipalUrl, calHomeBody, depth = "0")
            val calHomeHref = extractTextContent(calHomeResponse, "calendar-home-set", "href")
                ?: return@withContext fallbackCalendarUrl()

            // Step 3: Find VTODO-supporting calendar
            val resolvedCalHomeUrl = resolveUrl(calHomeHref)
            val calListBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav" xmlns:cs="http://calendarserver.org/ns/">
                    <d:prop>
                        <d:resourcetype />
                        <d:displayname />
                        <c:supported-calendar-component-set />
                    </d:prop>
                </d:propfind>
            """.trimIndent()

            val calListResponse = executePropfind(resolvedCalHomeUrl, calListBody, depth = "1")
            findVTodoCalendar(calListResponse) ?: fallbackCalendarUrl()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackCalendarUrl()
        }
    }

    /**
     * Fetches all VTODO items from the given calendar URL using a REPORT request.
     */
    suspend fun fetchTodos(calendarUrl: String): List<CalDavTodo> = withContext(Dispatchers.IO) {
        val resolvedUrl = resolveUrl(calendarUrl)
        val reportBody = """
            <?xml version="1.0" encoding="UTF-8"?>
            <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                <d:prop>
                    <d:getetag />
                    <c:calendar-data />
                </d:prop>
                <c:filter>
                    <c:comp-filter name="VCALENDAR">
                        <c:comp-filter name="VTODO" />
                    </c:comp-filter>
                </c:filter>
            </c:calendar-query>
        """.trimIndent()

        val request = Request.Builder()
            .url(resolvedUrl)
            .method("REPORT", reportBody.toRequestBody(xmlMediaType))
            .header("Depth", "1")
            .header("Content-Type", "application/xml; charset=utf-8")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()

        if (!response.isSuccessful && response.code != 207) {
            return@withContext emptyList()
        }

        parseMultiStatusForTodos(body)
    }

    /**
     * Creates or updates a VTODO on the server via PUT.
     * If [href] is null, generates a new href based on a UUID.
     */
    suspend fun putTodo(
        calendarUrl: String,
        href: String?,
        vtodoIcal: String
    ): PutResult = withContext(Dispatchers.IO) {
        val targetHref = href ?: "${calendarUrl.trimEnd('/')}/${UUID.randomUUID()}.ics"
        val resolvedUrl = resolveUrl(targetHref)

        val requestBuilder = Request.Builder()
            .url(resolvedUrl)
            .put(vtodoIcal.toRequestBody(icalMediaType))
            .header("Content-Type", "text/calendar; charset=utf-8")

        // If updating an existing resource, we could add If-Match header
        // For simplicity, we allow overwrites

        val response = client.newCall(requestBuilder.build()).execute()
        val eTag = response.header("ETag")
        response.close()

        PutResult(href = targetHref, eTag = eTag)
    }

    /**
     * Deletes a VTODO on the server via DELETE with If-Match for safe deletion.
     */
    suspend fun deleteTodo(
        calendarUrl: String,
        href: String,
        eTag: String
    ): Boolean = withContext(Dispatchers.IO) {
        val resolvedUrl = resolveUrl(href)

        val request = Request.Builder()
            .url(resolvedUrl)
            .delete()
            .header("If-Match", eTag)
            .build()

        val response = client.newCall(request).execute()
        val success = response.isSuccessful || response.code == 204
        response.close()
        success
    }

    /**
     * Parses a basic iCalendar VTODO string into a [CalDavTodo].
     */
    fun parseVTodo(icalData: String, href: String = "", eTag: String = ""): CalDavTodo {
        var uid = ""
        var summary = ""
        var completed = false
        var due: Long? = null
        var priority = 0
        var description: String? = null
        var categories: String? = null

        // Handle unfolded lines (RFC 5545: lines starting with space/tab are continuations)
        val unfoldedData = icalData
            .replace("\r\n ", "")
            .replace("\r\n\t", "")
            .replace("\n ", "")
            .replace("\n\t", "")

        val lines = unfoldedData.lines()
        var inVTodo = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "BEGIN:VTODO") {
                inVTodo = true
                continue
            }
            if (trimmed == "END:VTODO") {
                break
            }
            if (!inVTodo) continue

            when {
                trimmed.startsWith("UID:") -> {
                    uid = trimmed.substringAfter("UID:")
                }
                trimmed.startsWith("SUMMARY:") -> {
                    summary = trimmed.substringAfter("SUMMARY:")
                }
                trimmed.startsWith("DESCRIPTION:") -> {
                    description = trimmed.substringAfter("DESCRIPTION:")
                }
                trimmed.startsWith("CATEGORIES:") -> {
                    categories = trimmed.substringAfter("CATEGORIES:")
                }
                trimmed.startsWith("STATUS:COMPLETED") -> {
                    completed = true
                }
                trimmed.startsWith("COMPLETED:") -> {
                    completed = true
                }
                trimmed.startsWith("DUE") -> {
                    // DUE;VALUE=DATE:20260115 or DUE:20260115T120000Z
                    val dateStr = trimmed.substringAfter(":")
                    due = parseICalDate(dateStr)
                }
                trimmed.startsWith("PRIORITY:") -> {
                    priority = trimmed.substringAfter("PRIORITY:").toIntOrNull() ?: 0
                }
            }
        }

        return CalDavTodo(
            href = href,
            eTag = eTag,
            uid = uid,
            summary = summary,
            completed = completed,
            due = due,
            priority = priority,
            description = description,
            categories = categories
        )
    }

    /**
     * Builds a complete iCalendar VTODO string from a [TaskEntity].
     */
    fun buildVTodo(task: TaskEntity): String {
        val uid = task.id
        val now = formatICalDateTime(System.currentTimeMillis())

        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Planify//NONSGML Planify//EN")
        sb.appendLine("BEGIN:VTODO")
        sb.appendLine("UID:$uid")
        sb.appendLine("DTSTAMP:$now")
        sb.appendLine("CREATED:$now")
        sb.appendLine("LAST-MODIFIED:$now")
        sb.appendLine("SUMMARY:${escapeICalText(task.title)}")

        task.description?.let { desc ->
            sb.appendLine("DESCRIPTION:${escapeICalText(desc)}")
        }

        if (task.isCompleted) {
            sb.appendLine("STATUS:COMPLETED")
            sb.appendLine("COMPLETED:$now")
            sb.appendLine("PERCENT-COMPLETE:100")
        } else {
            sb.appendLine("STATUS:NEEDS-ACTION")
        }

        task.dueDate?.let { dueMillis ->
            sb.appendLine("DUE:${formatICalDateTime(dueMillis)}")
        }

        if (task.priority > 0) {
            sb.appendLine("PRIORITY:${task.priority}")
        }

        task.label?.let { label ->
            sb.appendLine("CATEGORIES:${escapeICalText(label)}")
        }

        sb.appendLine("END:VTODO")
        sb.appendLine("END:VCALENDAR")

        return sb.toString()
    }

    // ──────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────

    private fun executePropfind(url: String, body: String, depth: String): String {
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", body.toRequestBody(xmlMediaType))
            .header("Depth", depth)
            .header("Content-Type", "application/xml; charset=utf-8")
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun resolveUrl(path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            "${serverUrl.trimEnd('/')}${if (path.startsWith("/")) "" else "/"}$path"
        }
    }

    private fun fallbackCalendarUrl(): String {
        return "${serverUrl.trimEnd('/')}/remote.php/dav/calendars/$username/tasks/"
    }

    private fun parseXml(xml: String): Document? {
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()
            builder.parse(InputSource(StringReader(xml)))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractTextContent(xml: String, parentLocalName: String, childLocalName: String): String? {
        val doc = parseXml(xml) ?: return null
        val elements = doc.getElementsByTagName("*")
        for (i in 0 until elements.length) {
            val element = elements.item(i) as? Element ?: continue
            if (element.localName == parentLocalName) {
                val children = element.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j) as? Element ?: continue
                    if (child.localName == childLocalName) {
                        return child.textContent?.trim()
                    }
                }
            }
        }
        return null
    }

    private fun findVTodoCalendar(xml: String): String? {
        val doc = parseXml(xml) ?: return null

        // Find all <response> elements
        val responses = doc.getElementsByTagNameNS("DAV:", "response")
        if (responses.length == 0) {
            // Try without namespace
            return findVTodoCalendarFallback(doc)
        }

        for (i in 0 until responses.length) {
            val response = responses.item(i) as? Element ?: continue
            val href = getChildTextByLocalName(response, "href") ?: continue

            // Check if this is a calendar resource
            if (!isCalendarResource(response)) continue

            // Check if it supports VTODO
            if (supportsVTodo(response)) {
                return href
            }
        }

        return null
    }

    private fun findVTodoCalendarFallback(doc: Document): String? {
        val allElements = doc.getElementsByTagName("*")
        var currentHref: String? = null
        var hasCalendar = false
        var hasVTodo = false

        for (i in 0 until allElements.length) {
            val element = allElements.item(i) as? Element ?: continue

            when (element.localName) {
                "response" -> {
                    if (hasCalendar && hasVTodo && currentHref != null) {
                        return currentHref
                    }
                    currentHref = null
                    hasCalendar = false
                    hasVTodo = false
                }
                "href" -> {
                    if (currentHref == null) {
                        currentHref = element.textContent?.trim()
                    }
                }
                "calendar" -> hasCalendar = true
                "comp" -> {
                    val name = element.getAttribute("name")
                        .ifEmpty { element.getAttributeNS(null, "name") }
                    if (name == "VTODO") hasVTodo = true
                }
            }
        }

        // Check last response
        if (hasCalendar && hasVTodo && currentHref != null) {
            return currentHref
        }

        return null
    }

    private fun isCalendarResource(response: Element): Boolean {
        val allElements = response.getElementsByTagName("*")
        for (i in 0 until allElements.length) {
            val el = allElements.item(i) as? Element ?: continue
            if (el.localName == "calendar") return true
        }
        return false
    }

    private fun supportsVTodo(response: Element): Boolean {
        val allElements = response.getElementsByTagName("*")
        for (i in 0 until allElements.length) {
            val el = allElements.item(i) as? Element ?: continue
            if (el.localName == "comp") {
                val name = el.getAttribute("name")
                    .ifEmpty { el.getAttributeNS(null, "name") }
                if (name == "VTODO") return true
            }
        }
        return false
    }

    private fun getChildTextByLocalName(parent: Element, localName: String): String? {
        val children = parent.getElementsByTagName("*")
        for (i in 0 until children.length) {
            val child = children.item(i) as? Element ?: continue
            if (child.localName == localName) {
                return child.textContent?.trim()
            }
        }
        return null
    }

    private fun parseMultiStatusForTodos(xml: String): List<CalDavTodo> {
        val doc = parseXml(xml) ?: return emptyList()
        val todos = mutableListOf<CalDavTodo>()

        val allElements = doc.getElementsByTagName("*")
        var currentHref: String? = null
        var currentETag: String? = null
        var currentCalData: String? = null
        var inResponse = false

        for (i in 0 until allElements.length) {
            val element = allElements.item(i) as? Element ?: continue

            when (element.localName) {
                "response" -> {
                    // Process previous response if we have data
                    if (inResponse && currentHref != null && currentCalData != null) {
                        val todo = parseVTodo(
                            icalData = currentCalData!!,
                            href = currentHref!!,
                            eTag = currentETag ?: ""
                        )
                        if (todo.summary.isNotBlank()) {
                            todos.add(todo)
                        }
                    }
                    currentHref = null
                    currentETag = null
                    currentCalData = null
                    inResponse = true
                }
                "href" -> {
                    if (inResponse && currentHref == null) {
                        currentHref = element.textContent?.trim()
                    }
                }
                "getetag" -> {
                    currentETag = element.textContent?.trim()?.removeSurrounding("\"")
                }
                "calendar-data" -> {
                    currentCalData = element.textContent?.trim()
                }
            }
        }

        // Process last response
        if (inResponse && currentHref != null && currentCalData != null) {
            val todo = parseVTodo(
                icalData = currentCalData!!,
                href = currentHref!!,
                eTag = currentETag ?: ""
            )
            if (todo.summary.isNotBlank()) {
                todos.add(todo)
            }
        }

        return todos
    }

    private fun parseICalDate(dateStr: String): Long? {
        return try {
            when {
                // DATE-TIME with UTC: 20260115T120000Z
                dateStr.length == 16 && dateStr.endsWith("Z") -> {
                    val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    sdf.parse(dateStr)?.time
                }
                // DATE-TIME without timezone: 20260115T120000
                dateStr.contains("T") && dateStr.length >= 15 -> {
                    val cleanDate = dateStr.substringBefore("Z")
                    val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    sdf.parse(cleanDate)?.time
                }
                // DATE only: 20260115
                dateStr.length == 8 -> {
                    val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    sdf.parse(dateStr)?.time
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatICalDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(millis))
    }

    private fun escapeICalText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }
}
