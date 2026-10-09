package app.commutenity.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridParserTest {
    private val q1 = "Paano pumunta sa Dela Rosa, Pio del Pilar mula sa Ayala Center?"
    private val q2 = "Pa-Greenbelt ako galing Guadalupe, ano pinakamabilis?"
    private val q4 = "frm RCBC pa-Pio del Pilar, ung walang lipat sana"
    private val q5 = "Ito ba yung tamang jeep? Nakasulat sa karatula: PASAY - GUADALUPE"
    private val q6 = "Uulan ba mamaya?"
    private val q8 = "Paano pumunta sa Poblacion?"
    private val q10 = "Is this the right bus? It says 'AYALA - FTI'"

    // ---- vehicle cues ----

    @Test
    fun vehicleQuestionsAreDetected() {
        assertTrue(HybridParser.isVehicleQuestion(q5))
        assertTrue(HybridParser.isVehicleQuestion(q10))
    }

    @Test
    fun tripQuestionIsNotAVehicleQuestion() {
        assertFalse(HybridParser.isVehicleQuestion(q8))
    }

    @Test
    fun fieldsFollowQuestionKind() {
        assertEquals(listOf("vehicle_text"), HybridParser.fieldsFor(q5))
        assertEquals(listOf("origin", "destination"), HybridParser.fieldsFor(q8))
    }

    // ---- preference ----

    @Test
    fun fastestCue() {
        assertEquals(Preference.FASTEST, HybridParser.preferenceOf("ano pinakamabilis"))
    }

    @Test
    fun cheapestCue() {
        assertEquals(Preference.CHEAPEST, HybridParser.preferenceOf("cheapest"))
    }

    @Test
    fun fewestTransfersCue() {
        assertEquals(Preference.FEWEST_TRANSFERS, HybridParser.preferenceOf("ung walang lipat sana"))
    }

    @Test
    fun fewestTransfersWinsOverCheapWordInSameSentence() {
        assertEquals(
            Preference.FEWEST_TRANSFERS,
            HybridParser.preferenceOf("Gusto ko yung mura pero walang lipat"),
        )
    }

    @Test
    fun noPreferenceCueIsNull() {
        assertNull(HybridParser.preferenceOf(q8))
    }

    // ---- copied ----

    @Test
    fun copiedStripsPaPrefix() {
        assertEquals("Greenbelt", HybridParser.copied("Pa-Greenbelt", "Pa-Greenbelt ako galing Guadalupe"))
    }

    @Test
    fun copiedRejectsInventedPlace() {
        assertNull(HybridParser.copied("Cubao", "Paano pumunta sa Poblacion?"))
    }

    @Test
    fun copiedRejectsPartialWordMatch() {
        assertNull(HybridParser.copied("Pio", "Paano pumunta sa Pioneer St?"))
    }

    @Test
    fun copiedTrimsEdgePunctuation() {
        assertEquals("Poblacion", HybridParser.copied("Poblacion?", q8))
    }

    @Test
    fun copiedNullAndBlankAreNull() {
        assertNull(HybridParser.copied(null, q8))
        assertNull(HybridParser.copied("  ?! ", q8))
    }

    // ---- assemble ----

    @Test
    fun assembleQ1() {
        val out = HybridParser.assemble(q1, mapOf("origin" to "Ayala Center", "destination" to "Dela Rosa, Pio del Pilar"))
        assertEquals(
            ParserOutput(Intent.TRIP, "Ayala Center", "Dela Rosa, Pio del Pilar", null, null),
            out,
        )
    }

    @Test
    fun assembleQ2() {
        val out = HybridParser.assemble(q2, mapOf("origin" to "Guadalupe", "destination" to "Pa-Greenbelt"))
        assertEquals(ParserOutput(Intent.TRIP, "Guadalupe", "Greenbelt", Preference.FASTEST, null), out)
    }

    @Test
    fun assembleQ4DropsInventedDestination() {
        val out = HybridParser.assemble(q4, mapOf("origin" to "Pio del Pilar", "destination" to "Cubao"))
        assertEquals(ParserOutput(Intent.TRIP, "Pio del Pilar", null, Preference.FEWEST_TRANSFERS, null), out)
    }

    @Test
    fun assembleQ5() {
        val out = HybridParser.assemble(q5, mapOf("vehicle_text" to "PASAY - GUADALUPE"))
        assertEquals(ParserOutput(Intent.VEHICLE_CHECK, null, null, null, "PASAY - GUADALUPE"), out)
    }

    @Test
    fun assembleQ6NoPlacesIsOther() {
        val out = HybridParser.assemble(q6, mapOf("origin" to null, "destination" to null))
        assertEquals(ParserOutput(Intent.OTHER, null, null, null, null), out)
    }

    @Test
    fun assembleQ8EchoedPlaceKeepsDestinationOnly() {
        val out = HybridParser.assemble(q8, mapOf("origin" to "Poblacion", "destination" to "Poblacion"))
        assertEquals(ParserOutput(Intent.TRIP, null, "Poblacion", null, null), out)
    }

    @Test
    fun assembleQ10() {
        val out = HybridParser.assemble(q10, mapOf("vehicle_text" to "AYALA - FTI"))
        assertEquals(ParserOutput(Intent.VEHICLE_CHECK, null, null, null, "AYALA - FTI"), out)
    }

    @Test
    fun assemblePreferenceDroppedWhenNoPlaceSurvives() {
        val out = HybridParser.assemble("ano pinakamabilis?", mapOf("origin" to null, "destination" to null))
        assertEquals(ParserOutput(Intent.OTHER, null, null, null, null), out)
    }

    // ---- schema ----

    @Test
    fun schemaForOriginDestination() {
        val schema = HybridParser.schemaFor(listOf("origin", "destination"))
        assertEquals(
            "{\"type\":\"object\",\"properties\":{" +
                "\"origin\":{\"type\":[\"string\",\"null\"]}," +
                "\"destination\":{\"type\":[\"string\",\"null\"]}}," +
                "\"required\":[\"origin\",\"destination\"],\"additionalProperties\":false}",
            schema,
        )
        assertTrue(schema.contains("\"additionalProperties\":false"))
    }

    // ---- toJson ----

    @Test
    fun toJsonTrip() {
        val json = ParserOutput(Intent.TRIP, "Ayala Center", null, Preference.FEWEST_TRANSFERS, null).toJson()
        assertEquals(
            "{\"intent\":\"trip\",\"origin\":\"Ayala Center\",\"destination\":null," +
                "\"preference\":\"fewest_transfers\",\"vehicle_text\":null}",
            json,
        )
    }

    @Test
    fun toJsonVehicleCheckEscapesQuotesAndBackslashes() {
        val json = ParserOutput(Intent.VEHICLE_CHECK, null, null, null, "AYALA \"FTI\" \\ 2").toJson()
        assertEquals(
            "{\"intent\":\"vehicle_check\",\"origin\":null,\"destination\":null," +
                "\"preference\":null,\"vehicle_text\":\"AYALA \\\"FTI\\\" \\\\ 2\"}",
            json,
        )
    }
}
