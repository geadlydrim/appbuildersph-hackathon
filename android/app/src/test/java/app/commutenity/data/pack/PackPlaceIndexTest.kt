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
}
