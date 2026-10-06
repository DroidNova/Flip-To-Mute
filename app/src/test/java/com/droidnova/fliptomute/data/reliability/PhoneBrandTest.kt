package com.droidnova.fliptomute.data.reliability

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneBrandTest {

    @Test fun knownManufacturers_mapToTheirFamily() {
        val expected = mapOf(
            "Xiaomi" to PhoneBrand.XIAOMI,
            "Redmi" to PhoneBrand.XIAOMI,
            "POCO" to PhoneBrand.XIAOMI,
            "samsung" to PhoneBrand.SAMSUNG,
            "OPPO" to PhoneBrand.OPPO_FAMILY,
            "realme" to PhoneBrand.OPPO_FAMILY,
            "OnePlus" to PhoneBrand.OPPO_FAMILY,
            "vivo" to PhoneBrand.VIVO,
            "iQOO" to PhoneBrand.VIVO,
            "HUAWEI" to PhoneBrand.HUAWEI_FAMILY,
            "HONOR" to PhoneBrand.HUAWEI_FAMILY,
        )
        expected.forEach { (manufacturer, brand) -> assertEquals(manufacturer, brand, PhoneBrand.from(manufacturer)) }
    }

    @Test fun unknownBlankOrMissing_fallBackToGenericSteps() {
        assertEquals(PhoneBrand.OTHER, PhoneBrand.from("Google"))
        assertEquals(PhoneBrand.OTHER, PhoneBrand.from(""))
        assertEquals(PhoneBrand.OTHER, PhoneBrand.from(null))
    }

    @Test fun surroundingSpaces_areIgnored() {
        assertEquals(PhoneBrand.SAMSUNG, PhoneBrand.from("  Samsung "))
    }
}
