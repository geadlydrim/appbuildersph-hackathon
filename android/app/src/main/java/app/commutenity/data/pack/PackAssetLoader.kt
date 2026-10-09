package app.commutenity.data.pack

import android.content.res.AssetManager

object PackAssetLoader {
    fun load(assets: AssetManager, fileName: String): PackPlaceIndex =
        CommutePack.parse(assets.open(fileName).bufferedReader().use { it.readText() }).placeIndex()
}
