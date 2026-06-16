package net.lgiki.soundmemo.data.storage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

const val DEFAULT_RECORDING_NAME_TEMPLATE = "SoundMemo_{timestamp}"

data class GeneratedRecordingName(
    val fileName: String,
    val displayName: String,
)

object RecordingNameTemplate {
    private val tokenRegex = Regex("""\{([A-Za-z]+)\}""")
    private val unsafeFileNameChars = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
    private val supportedTokens = setOf("date", "time", "timestamp", "id")

    fun unknownTokens(template: String): Set<String> =
        tokenRegex.findAll(template)
            .map { it.groupValues[1] }
            .filterNot { it in supportedTokens }
            .toSet()

    fun isValid(template: String): Boolean = unknownTokens(template).isEmpty()

    fun generate(
        template: String,
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
        val fileBaseName = if ("{id}" in safeTemplate) sanitized else "${sanitized}_$id"
        return GeneratedRecordingName(
            fileName = "$fileBaseName.m4a",
            displayName = sanitized,
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

    private fun render(template: String, now: Long, id: String): String {
        val date = Date(now)
        val replacements = mapOf(
            "date" to SimpleDateFormat("yyyyMMdd", Locale.US).format(date),
            "time" to SimpleDateFormat("HHmmss", Locale.US).format(date),
            "timestamp" to SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date),
            "id" to id,
        )
        return tokenRegex.replace(template) { match ->
            replacements[match.groupValues[1]] ?: match.value
        }
    }

    private fun sanitize(value: String): String =
        value.trim()
            .replace(unsafeFileNameChars, "_")
            .replace(Regex("""\s+"""), " ")
            .trim('.', ' ')
}
