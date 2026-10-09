package app.commutenity.data.pack

import android.content.res.AssetManager
import app.commutenity.domain.Place
import org.json.JSONObject

object PackAssetLoader {
    fun load(assets: AssetManager, fileName: String): PackPlaceIndex {
        val json = assets.open(fileName).bufferedReader().use { it.readText() }
        val places = JSONObject(json).getJSONArray("places")
        return PackPlaceIndex(
            List(places.length()) { index ->
                val place = places.getJSONObject(index)
                PackPlace(
                    place = Place(
                        id = place.getString("id"),
                        name = place.getString("name"),
                        area = place.optString("area"),
                        inMakati = true,
                    ),
                    aliases = place.optJSONArray("aliases")?.let { aliases ->
                        List(aliases.length()) { aliases.getString(it) }
                    }.orEmpty(),
                )
            },
        )
    }
}
