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
    private val tokenRegex = Regex("""\{([A-Za-z]+)\}""")
    private val unsafeFileNameChars = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
    val supportedTokens = listOf("date", "time", "id")
    private val supportedTokenSet = supportedTokens.toSet()

    fun unknownTokens(template: String): Set<String> =
        tokenRegex.findAll(template)
            .map { it.groupValues[1] }
            .filterNot { it in supportedTokenSet }
            .toSet()

    fun isValid(template: String): Boolean = unknownTokens(template).isEmpty()

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
        val fileBaseName = if ("{id}" in safeTemplate) sanitized else "${sanitized}_$id"
        return GeneratedRecordingName(
            fileName = "$fileBaseName.$extension",
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

    fun previewToken(
        token: String,
        now: Long = System.currentTimeMillis(),
        sampleId: String = "a1b2c3d4",
    ): String = replacements(now, sampleId)[token].orEmpty()

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
}
