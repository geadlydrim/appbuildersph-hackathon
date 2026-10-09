package app.commutenity.data.qa

import android.content.Context
import app.commutenity.domain.QaPost
import app.commutenity.domain.QaRoute
import app.commutenity.domain.QaState
import app.commutenity.domain.QaThread
import org.json.JSONObject

/**
 * Mock rider questions and answers from `data/mock/rider-qa.json`. Every post here is a hand-written sample:
 * no account, no real rider. An answer is tied to a trip only when its `worked_candidate` is a real candidate
 * for that pair; any other key is dropped so it counts for nothing.
 */
object RiderQaLoader {
    private const val ASSET_PATH = "mock/rider-qa.json"

    fun fromAssets(context: Context, validKeys: (fromId: String, toId: String) -> Set<String>): QaState =
        parse(context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }, validKeys)

    fun parse(json: String, validKeys: (fromId: String, toId: String) -> Set<String>): QaState {
        val threads = JSONObject(json).getJSONArray("threads")
        return QaState(
            threads = List(threads.length()) { index ->
                val thread = threads.getJSONObject(index)
                val fromId = thread.getString("from_place_id")
                val toId = thread.getString("to_place_id")
                val route = QaRoute(fromId, toId, thread.getString("from"), thread.getString("to"))
                val allowed = validKeys(fromId, toId)

                val question = thread.getJSONObject("question")
                val answers = thread.getJSONArray("answers")
                QaThread(
                    id = thread.getString("id"),
                    question = QaPost(
                        id = question.getString("id"),
                        body = question.getString("body"),
                        placeName = "${route.from} → ${route.to}",
                        score = question.getInt("score"),
                        route = route,
                        sample = true,
                        mine = false,
                    ),
                    comments = List(answers.length()) { answerIndex ->
                        val answer = answers.getJSONObject(answerIndex)
                        val worked = if (answer.isNull("worked_candidate")) null else answer.getString("worked_candidate")
                        QaPost(
                            id = answer.getString("id"),
                            body = answer.getString("body"),
                            placeName = null,
                            score = answer.getInt("score"),
                            workedTrip = worked?.takeIf { it in allowed },
                            sample = true,
                            mine = false,
                        )
                    },
                )
            },
        )
    }
}
