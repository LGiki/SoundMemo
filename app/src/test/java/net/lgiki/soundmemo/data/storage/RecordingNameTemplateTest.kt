package net.lgiki.soundmemo.data.storage

import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordingNameTemplateTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun generate_defaultTemplateAppendsUniqueIdToFileName() {
        val generated = RecordingNameTemplate.generate(
            template = DEFAULT_RECORDING_NAME_TEMPLATE,
            now = 1_234L,
            uniqueSuffix = "abcdef12-3456-7890",
        )

        assertEquals("SoundMemo_19700101_000001_abcdef12.m4a", generated.fileName)
        assertEquals("SoundMemo_19700101_000001", generated.displayName)
    }

    @Test
    fun generate_explicitIdDoesNotAppendSecondId() {
        val generated = RecordingNameTemplate.generate(
            template = "Meeting_{date}_{id}",
            extension = "aac",
            now = 1_234L,
            uniqueSuffix = "abcdef12-3456-7890",
        )

        assertEquals("Meeting_19700101_abcdef12.aac", generated.fileName)
        assertEquals("Meeting_19700101_abcdef12", generated.displayName)
    }

    @Test
    fun generate_usesRequestedAudioExtension() {
        val wav = RecordingNameTemplate.generate(
            template = "Meeting_{date}_{id}",
            extension = "wav",
            now = 1_234L,
            uniqueSuffix = "abcdef12-3456-7890",
        )
        val mp3 = RecordingNameTemplate.generate(
            template = "Meeting_{date}_{id}",
            extension = "mp3",
            now = 1_234L,
            uniqueSuffix = "abcdef12-3456-7890",
        )

        assertEquals("Meeting_19700101_abcdef12.wav", wav.fileName)
        assertEquals("Meeting_19700101_abcdef12.mp3", mp3.fileName)
    }

    @Test
    fun isValid_rejectsUnknownTokens() {
        assertFalse(RecordingNameTemplate.isValid("Recording_{counter}"))
        assertEquals(setOf("counter"), RecordingNameTemplate.unknownTokens("Recording_{counter}"))
    }

    @Test
    fun isValid_rejectsUnclosedAndMalformedBraceTokens() {
        assertTrue(RecordingNameTemplate.unknownTokens("Recording_{date").isEmpty())
        assertFalse(RecordingNameTemplate.isValid("Recording_{date"))
        assertFalse(RecordingNameTemplate.isValid("Recording_date}"))
        assertFalse(RecordingNameTemplate.isValid("Recording_{date_}"))
        assertEquals(setOf("date_"), RecordingNameTemplate.unknownTokens("Recording_{date_}"))
    }

    @Test
    fun generate_sanitizesUnsafeFileNameCharacters() {
        val generated = RecordingNameTemplate.generate(
            template = "Meeting: {date}/A?",
            now = 1_234L,
            uniqueSuffix = "abcdef12-3456-7890",
        )

        assertEquals("Meeting_ 19700101_A__abcdef12.m4a", generated.fileName)
        assertEquals("Meeting_ 19700101_A_", generated.displayName)
    }

    @Test
    fun generate_blankRenderedNameFallsBackToDefault() {
        val generated = RecordingNameTemplate.generate(
            template = ".",
            now = 1_234L,
            uniqueSuffix = "abcdef12-3456-7890",
        )

        assertEquals("SoundMemo_19700101_000001_abcdef12.m4a", generated.fileName)
        assertEquals("SoundMemo_19700101_000001", generated.displayName)
    }

    @Test
    fun preview_usesSampleIdWithoutExtension() {
        val preview = RecordingNameTemplate.preview(
            template = "Interview_{time}_{id}",
            now = 1_234L,
            sampleId = "a1b2c3d4",
        )

        assertEquals("Interview_000001_a1b2c3d4", preview)
        assertTrue(RecordingNameTemplate.isValid("Interview_{time}_{id}"))
    }

    @Test
    fun generate_truncatesMultibyteNameWithinSafeFileNameLimit() {
        val generated = RecordingNameTemplate.generate(
            template = "录".repeat(200),
            extension = "wav",
            uniqueSuffix = "abcdef12-3456-7890",
        )

        assertTrue(
            generated.fileName.toByteArray(Charsets.UTF_8).size <=
                RecordingNameTemplate.MAX_COMPLETE_FILE_NAME_BYTES,
        )
        assertFalse(generated.displayName.endsWith('\uFFFD'))
    }

    @Test
    fun forPart_keepsFirstNameAndAddsOrderedSuffixes() {
        val generated = GeneratedRecordingName("Meeting_abcdef12.wav", "Meeting")

        assertEquals(generated, RecordingNameTemplate.forPart(generated, 1))
        assertEquals(
            "Meeting_abcdef12_part02.wav",
            RecordingNameTemplate.forPart(generated, 2).fileName,
        )
    }
}
