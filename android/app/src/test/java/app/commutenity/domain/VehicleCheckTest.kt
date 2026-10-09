package app.commutenity.domain

import app.commutenity.ai.Intent
import app.commutenity.ai.ParserOutput
import app.commutenity.data.pack.CommutePack
import app.commutenity.data.pack.PackTripSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VehicleCheckTest {
    private val hero = listOf("LRT", "Buendia - LRT")

    @Test
    fun exactAndSpacingVariantsRideThis() {
        assertEquals(VehicleVerdict.RideThis("Buendia - LRT"), VehicleCheck.check("Buendia - LRT", hero))
        assertEquals(VehicleVerdict.RideThis("Buendia - LRT"), VehicleCheck.check("buendia lrt", hero))
        assertEquals(VehicleVerdict.RideThis("LRT"), VehicleCheck.check("LRT", hero))
    }

    @Test
    fun oneTypoInALongWordStillMatches() {
        assertEquals(VehicleVerdict.RideThis("Buendia - LRT"), VehicleCheck.check("Buendya LRT", hero))
    }

    @Test
    fun aDifferentRouteSaysLookFor() {
        assertEquals(VehicleVerdict.LookFor(hero), VehicleCheck.check("PASAY - GUADALUPE", hero))
    }

    @Test
    fun aPartialMatchIsNeverYes() {
        // "Buendia" alone could be a different Buendia route; only "LRT" or "Buendia - LRT" is a yes.
        assertEquals(VehicleVerdict.NotSure(hero), VehicleCheck.check("Buendia - Ayala", hero))
    }

    @Test
    fun garbageOrNoTripIsNotSure() {
        assertEquals(VehicleVerdict.NotSure(hero), VehicleCheck.check("  ?! ", hero))
        assertEquals(VehicleVerdict.NotSure(emptyList()), VehicleCheck.check("LRT", emptyList()))
    }

    @Test
    fun heroTripOnScreenAnswersFromThePackSignboards() {
        val pack = CommutePack.parse(File("../../data/pack/hero-trip.source.json").readText())
        val source = PackTripSource(pack)
        val rufino = pack.places.first { it.id == "va-rufino" }.toPlace()
        val delaRosa = pack.places.first { it.id == "dela-rosa-pio-del-pilar" }.toPlace()
        val onScreen = HomeState(origin = rufino, destination = delaRosa, asking = true)

        fun ask(text: String) = reduce(
            onScreen,
            HomeEvent.AskParsed(ParserOutput(Intent.VEHICLE_CHECK, null, null, null, text)),
            source,
        ).askFeedback!!

        assertTrue(ask("Buendia LRT").contains("Yes, ride this"))
        assertTrue(ask("PASAY - GUADALUPE").contains("No, look for \"LRT\" or \"Buendia - LRT\""))
    }
}
