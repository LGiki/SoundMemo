package net.lgiki.soundmemo.data.storage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

const val DEFAULT_RECORDING_NAME_TEMPLATE = "SoundMemo_{date}_{time}"

data class GeneratedRecordingName(
    val fileName: String,
    val displayName: String,
)

object RecordingNameTemplate {
    private val tokenRegex = Regex("""\{([^{}]*)\}""")
    private val unsafeFileNameChars = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
    val supportedTokens = listOf("date", "time", "id")
    private val supportedTokenSet = supportedTokens.toSet()

    fun unknownTokens(template: String): Set<String> =
        tokenRegex.findAll(template)
            .map { it.groupValues[1] }
            .filterNot { it in supportedTokenSet }
            .toSet()

    fun hasMalformedBraces(template: String): Boolean =
        tokenRegex.replace(template, "").any { it == '{' || it == '}' }

    fun isValid(template: String): Boolean =
        unknownTokens(template).isEmpty() && !hasMalformedBraces(template)

    fun generate(
        template: String,
        extension: String = "m4a",
        now: Long = System.currentTimeMillis(),
        uniqueSuffix: String = UUID.randomUUID().toString(),
    ): GeneratedRecordingName {
        val id = uniqueSuffix.take(8)
        val normalizedTemplate = template.trim().ifBlank { DEFAULT_RECORDING_NAME_TEMPLATE }
        val safeTemplate = if (isValid(normalizedTemplate)) normalizedTemplate else DEFAULT_RECORDING_NAME_TEMPLATE
        val rendered = render(safeTemplate, now, id)
        val sanitized = sanitize(rendered).ifBlank {
            render(DEFAULT_RECORDING_NAME_TEMPLATE, now, id)
        }
        val uniqueSuffix = if ("{id}" in safeTemplate) "" else "_$id"
        val extensionSuffix = ".$extension"
        val maxRenderedBytes = (
            MAX_COMPLETE_FILE_NAME_BYTES -
                PART_SUFFIX_RESERVED_BYTES -
                uniqueSuffix.toByteArray(Charsets.UTF_8).size -
                extensionSuffix.toByteArray(Charsets.UTF_8).size
            ).coerceAtLeast(1)
        val safeDisplayName = truncateUtf8(sanitized, maxRenderedBytes).ifBlank {
            truncateUtf8(render(DEFAULT_RECORDING_NAME_TEMPLATE, now, id), maxRenderedBytes)
        }
        val fileBaseName = "$safeDisplayName$uniqueSuffix"
        return GeneratedRecordingName(
            fileName = "$fileBaseName$extensionSuffix",
            displayName = safeDisplayName,
        )
    }

    fun preview(
        template: String,
        now: Long = System.currentTimeMillis(),
        sampleId: String = "a1b2c3d4",
    ): String {
        if (!isValid(template)) return ""
        val rendered = render(template.trim().ifBlank { DEFAULT_RECORDING_NAME_TEMPLATE }, now, sampleId)
        return sanitize(rendered).ifBlank {
            render(DEFAULT_RECORDING_NAME_TEMPLATE, now, sampleId)
        }
    }

    fun previewToken(
        token: String,
        now: Long = System.currentTimeMillis(),
        sampleId: String = "a1b2c3d4",
    ): String = replacements(now, sampleId)[token].orEmpty()

    fun forPart(generatedName: GeneratedRecordingName, partIndex: Int): GeneratedRecordingName {
        require(partIndex >= 1) { "Recording part index must be positive" }
        if (partIndex == 1) return generatedName
        val file = java.io.File(generatedName.fileName)
        val suffix = "_part${partIndex.toString().padStart(2, '0')}"
        return generatedName.copy(
            fileName = "${file.nameWithoutExtension}$suffix.${file.extension}",
        )
    }

    private fun render(template: String, now: Long, id: String): String {
        val replacements = replacements(now, id)
        return tokenRegex.replace(template) { match ->
            replacements[match.groupValues[1]] ?: match.value
        }
    }

    private fun replacements(now: Long, id: String): Map<String, String> {
        val date = Date(now)
        return mapOf(
            "date" to SimpleDateFormat("yyyyMMdd", Locale.US).format(date),
            "time" to SimpleDateFormat("HHmmss", Locale.US).format(date),
            "id" to id,
        )
    }

    private fun sanitize(value: String): String =
        value.trim()
            .replace(unsafeFileNameChars, "_")
            .replace(Regex("""\s+"""), " ")
            .trim('.', ' ')

    internal fun truncateUtf8(value: String, maxBytes: Int): String {
        if (maxBytes <= 0) return ""
        if (value.toByteArray(Charsets.UTF_8).size <= maxBytes) return value
        val result = StringBuilder()
        var byteCount = 0
        var offset = 0
        while (offset < value.length) {
            val codePoint = value.codePointAt(offset)
            val text = String(Character.toChars(codePoint))
            val codePointBytes = text.toByteArray(Charsets.UTF_8).size
            if (byteCount + codePointBytes > maxBytes) break
            result.append(text)
            byteCount += codePointBytes
            offset += Character.charCount(codePoint)
        }
        return result.toString().trimEnd('.', ' ')
    }

    internal const val MAX_COMPLETE_FILE_NAME_BYTES = 240
    private const val PART_SUFFIX_RESERVED_BYTES = 12
}
