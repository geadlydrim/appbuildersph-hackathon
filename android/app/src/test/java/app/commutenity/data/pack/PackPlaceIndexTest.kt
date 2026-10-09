package app.commutenity.data.pack

import app.commutenity.domain.Place
import org.junit.Assert.assertEquals
import org.junit.Test

class PackPlaceIndexTest {
    private val index = PackPlaceIndex(
        listOf(
            PackPlace(
                place = Place("ayala-center", "Ayala Center", "", inMakati = true),
                aliases = listOf("Ayala", "Glorietta"),
            ),
            PackPlace(
                place = Place("dela-rosa", "Dela Rosa Street", "", inMakati = true),
                aliases = listOf("Dela Rosa", "Pio del Pilar"),
            ),
        ),
    )

    @Test
    fun findsExactName() {
        assertEquals(listOf("Ayala Center"), index.search("AYALA CENTER").map { it.name })
    }

    @Test
    fun findsAlias() {
        assertEquals(listOf("Ayala Center"), index.search("Glorietta").map { it.name })
    }

    @Test
    fun findsSmallTypoInAlias() {
        assertEquals(listOf("Ayala Center"), index.search("glorieta").map { it.name })
    }

    @Test
    fun teamPlaceWinsATieWithAnOsmPlace() {
        // The hero destination must not lose to an OSM bus stop that happens to share the name.
        val tied = PackPlaceIndex(
            listOf(
                PackPlace(Place("osm-n1", "Dela Rosa", "", inMakati = true), aliases = emptyList()),
                PackPlace(Place("dela-rosa-pio-del-pilar", "Dela Rosa St, Pio del Pilar", "", inMakati = true),
                    aliases = listOf("Dela Rosa"), preferred = true),
            ),
        )
        assertEquals("dela-rosa-pio-del-pilar", tied.search("Dela Rosa").first().id)
    }

    @Test
    fun aPlaceContainingTheWholeQueryBeatsAShortNameInsideTheQuery() {
        val ayala = PackPlaceIndex(
            listOf(
                PackPlace(Place("osm-a", "Ayala", "", inMakati = true), aliases = emptyList()),
                PackPlace(Place("osm-g", "Ayala Triangle Gardens", "", inMakati = true), aliases = emptyList()),
            ),
        )
        assertEquals("osm-g", ayala.search("Ayala Triangle").first().id)
    }
}
