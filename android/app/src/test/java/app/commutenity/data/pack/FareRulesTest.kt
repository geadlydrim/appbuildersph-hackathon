package app.commutenity.data.pack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FareRulesTest {
    @Test
    fun airConditionedBus() {
        assertEquals(18, FareRules.fare("bus_aircon", 5000))
        assertEquals(21, FareRules.fare("bus_aircon", 5001)) // 18 + 2.98 = 20.98
        assertEquals(24, FareRules.fare("bus_aircon", 6200)) // 18 + 2 * 2.98 = 23.96
        assertEquals(33, FareRules.fare("bus_aircon", 10_000)) // 18 + 5 * 2.98 = 32.90
    }

    @Test
    fun ordinaryBus() {
        assertEquals(15, FareRules.fare("bus_ordinary", 5000))
        assertEquals(17, FareRules.fare("bus_ordinary", 5001)) // 15 + 2.49 = 17.49
        assertEquals(22, FareRules.fare("bus_ordinary", 7200)) // 15 + 3 * 2.49 = 22.47
    }

    @Test
    fun traditionalJeep() {
        assertEquals(14, FareRules.fare("jeep_traditional", 4000))
        assertEquals(16, FareRules.fare("jeep_traditional", 4001)) // 14 + 2.00
        assertEquals(26, FareRules.fare("jeep_traditional", 9500)) // 14 + 6 * 2.00
    }

    @Test
    fun modernJeep() {
        assertEquals(17, FareRules.fare("jeep_modern", 4000))
        assertEquals(19, FareRules.fare("jeep_modern", 4001)) // 17 + 2.40 = 19.40
        assertEquals(31, FareRules.fare("jeep_modern", 9500)) // 17 + 6 * 2.40 = 31.40
    }

    @Test
    fun theHeroRideIsTheMinimumJeepFare() {
        assertEquals(14, FareRules.fare("jeep_traditional", 848))
        assertEquals(18, FareRules.fare("bus_aircon", 0))
    }

    @Test
    fun halfAPesoRoundsUp() {
        assertEquals(140, FareRules.fare("bus_ordinary", 55_000)) // 15 + 50 * 2.49 = 139.50
    }

    @Test
    fun unknownRuleHasNoFare() {
        assertNull(FareRules.fare("tricycle", 1000))
    }
}
