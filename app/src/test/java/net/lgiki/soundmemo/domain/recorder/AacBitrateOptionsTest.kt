package net.lgiki.soundmemo.domain.recorder

import org.junit.Assert.assertEquals
import org.junit.Test

class AacBitrateOptionsTest {
    @Test
    fun valuesForRanges_filtersCommonValuesToDeviceRange() {
        val values = AacBitrateOptions.valuesForRanges(listOf(BitrateRange(min = 100_000, max = 260_000)))

        assertEquals(listOf(128_000, 160_000, 192_000, 256_000), values)
    }

    @Test
    fun valuesForRanges_usesFallbackWhenRangesMissing() {
        val values = AacBitrateOptions.valuesForRanges(null)

        assertEquals(listOf(96_000, 128_000, 192_000, 320_000), values)
    }

    @Test
    fun valuesForRanges_usesRangeBoundsWhenNoCommonValuesFit() {
        val values = AacBitrateOptions.valuesForRanges(listOf(BitrateRange(min = 10_000, max = 20_000)))

        assertEquals(listOf(10_000, 20_000), values)
    }

    @Test
    fun valuesForRanges_doesNotOfferValuesInGapsBetweenEncoders() {
        val values = AacBitrateOptions.valuesForRanges(
            listOf(
                BitrateRange(min = 64_000, max = 160_000),
                BitrateRange(min = 256_000, max = 320_000),
            ),
        )

        assertEquals(listOf(64_000, 96_000, 128_000, 160_000, 256_000, 320_000), values)
    }

    @Test
    fun closestSupported_returnsNearestOption() {
        val value = AacBitrateOptions.closestSupported(
            value = 300_000,
            options = listOf(96_000, 128_000, 192_000, 320_000),
        )

        assertEquals(320_000, value)
    }
}
