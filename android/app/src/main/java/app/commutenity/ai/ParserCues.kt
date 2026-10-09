package app.commutenity.ai

/** The one config file for the hybrid parser: the LLM prompt and the keyword cue lists. */
object ParserCues {
    const val SYSTEM_PROMPT = """You copy place names and signboard text out of Metro Manila commute questions (English, Tagalog, or Taglish). Reply with JSON only.
Trip question: origin is where the rider starts (after "galing", "mula sa", "from"); destination is where they are going (after "sa", "papunta", "pa-", "to"). Use null for a place the question does not name.
Vehicle question: vehicle_text is the signboard or route text the rider read.
Q: Paano pumunta sa Cubao galing Fairview?
A: {"origin":"Fairview","destination":"Cubao"}
Q: Saan masarap kumain?
A: {"origin":null,"destination":null}
Q: Tama ba 'tong bus? QUIAPO - CUBAO nakalagay
A: {"vehicle_text":"QUIAPO - CUBAO"}"""

    private val IGNORE = setOf(RegexOption.IGNORE_CASE)

    val VEHICLE = Regex(
        """\b(tama ba|tamang|ito ba|eto ba|right (jeep|jeepney|bus|van)|correct (jeep|jeepney|bus|van|vehicle)|karatula|signboard|nakasulat|nakalagay|it says)\b""",
        IGNORE,
    )

    /** First match wins, so the more specific transfer cue goes first. */
    val PREFERENCE: List<Pair<Preference, Regex>> = listOf(
        Preference.FEWEST_TRANSFERS to Regex("""\b(walang lipat|walang transfer|konting lipat|isang sakay|diretso|direct|no transfers?|fewest transfers?|less transfers?)\b""", IGNORE),
        Preference.CHEAPEST to Regex("""\b(pinakamura|mura|tipid|cheap|cheapest|cheaper|least expensive)\b""", IGNORE),
        Preference.FASTEST to Regex("""\b(pinakamabilis|mabilis|bilis|fastest|quickest|fast)\b""", IGNORE),
    )
}
