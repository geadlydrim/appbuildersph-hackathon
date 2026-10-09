# Where are current official fares published, so we can cite them?

**Ticket:** [#5](https://github.com/geadlydrim/appbuildersph-hackathon/issues/5) (wayfinder research)
**Researched:** 2026-10-09. Every URL below was accessed on 2026-10-09.

**Question.** Where are current, citable fare tables published for Metro Manila jeepneys (traditional and modern), city buses, UV Express, P2P, MRT-3, LRT-1, and LRT-2? Record the issuing body, URL, effective date, and fare structure (base fare plus per-km, or stop-to-stop matrix). This decides how much of the pack's fare data can be `collected` versus `known` or `mock` ([data plan §3](https://github.com/geadlydrim/appbuildersph-hackathon/blob/master/docs/fmd/data-commutenity.md#3-collection-protocol)).

## Bottom line

- **All three rail lines have official, citable station-to-station matrices on the operator's own site, so they can be `collected`.** LRT-1: LRMC (effective 2025-04-02). LRT-2: LRTA (base matrix effective 2023-08-02). MRT-3: DOTr-MRT3 (regular matrix; no printed effective date). The matrices are images or image-only PDFs, so a teammate has to transcribe the station pairs for the demo corridor by hand.
- **Rail fares are not what the old matrices say.** Since 2026-03-23 DOTr applies a 50% across-the-board discount on **MRT-3 and LRT-2** (LRTA page: "Effective March 23, 2026"). LRT-1 is not covered by it (my only source for that is a non-official blog, so treat it as `[UNVERIFIED]`). Store the full fare plus the discount as a separate, dated, sourced overlay. Do not quote old full fares as current.
- **Jeepneys and city buses are in effect since 2026-09-28, and the structure is clear: minimum fare for the first N km, plus a per-km rate.** Traditional jeepney ₱14 for the first 4 km, +₱2/km. Modern jeepney ₱17 for the first 4 km, +₱2.40/km. Metro Manila ordinary bus ₱15 for the first 5 km, +₱2.49/km. Aircon bus ₱18 for the first 5 km, +₱2.98/km. **But I could not open the LTFRB page itself** (`ltfrb.gov.ph` is behind a Cloudflare bot check that returns HTTP 403 to every non-human client I tried). The numbers come from news reports quoting the DOTr/LTFRB announcement, plus a reproduction of the LTFRB jeepney fare guide. `[UNVERIFIED]` against the LTFRB document until someone downloads it in a normal browser (about 2 minutes).
- **UV Express and P2P cannot be computed from what is published in a form we could read.** UV Express has only a provisional per-km rate (₱2.60/km traditional, ₱3/km modern) with no minimum fare found. P2P fares are per route, "+15% over the existing fare for each route", and I found no per-route table. Use `known` where a teammate rides that exact route, otherwise `mock` with a "sample data" tag.
- **Recommendation:** `collected` for LRT-1, LRT-2, and MRT-3 (cite the matrix and the discount source). `collected` for jeepney and city bus **only after** a teammate downloads the LTFRB fare guides and commits them with the retrieval date; until then label them `known`. `known`/`mock` for UV Express and P2P. Jeepney and bus fares depend on road distance, and the pack has no distance field, so the per-segment km is itself a `known` estimate even when the fare formula is `collected`.

## Summary by mode

| Mode | Issuing body | Effective | Fare structure | Official URL read? | Pack class |
|---|---|---|---|---|---|
| Traditional jeepney | LTFRB (approved by DOTr) | 2026-09-28 | ₱14 first 4 km, +₱2/km (table to 50 km) | No (403). Fare-guide image seen via Rappler | `collected` after manual download, else `known` |
| Modern jeepney | LTFRB | 2026-09-28 | ₱17 first 4 km, +₱2.40/km | No (403) | same |
| City/Metro Manila bus, ordinary | LTFRB | 2026-09-28 | ₱15 first 5 km, +₱2.49/km | No (403) | same |
| City/Metro Manila bus, aircon | LTFRB | 2026-09-28 | ₱18 first 5 km, +₱2.98/km | No (403) | same |
| UV Express | LTFRB (provisional) | 2026-09-28 reported | ₱2.60/km traditional, ₱3/km modern. Minimum unknown | No | `known` or `mock` |
| P2P bus | LTFRB | 2026-09-28 | Existing per-route fare +15%. No base table found | No | `known` (own route) or `mock` |
| MRT-3 | DOTr-MRT3 | Matrix date not printed; 50% discount from 2026-03-23 | 13×13 station matrix, ₱13 to ₱28 (full fare) | Yes | `collected` |
| LRT-1 | LRMC | 2025-04-02 | 25×25 matrix. SVC ₱17 to ₱52 between distinct stations (₱16 on the same-station diagonal), SJT ₱20 to ₱55. Boarding ₱16.25 + ₱1.47/km | Yes | `collected` |
| LRT-2 | LRTA | Matrix 2023-08-02; 50% discount from 2026-03-23 | 13×13 matrix, SVC ₱14 to ₱33 between distinct stations (full fare), halved in 2026 | Yes | `collected` |

## 1. Jeepneys, city buses, UV Express, P2P (LTFRB)

### Where it is published

- **Canonical page:** <https://ltfrb.gov.ph/fare-rates/> ("Find updated fare rates for public transportation regulated by the LTFRB"). Issuances are at <https://ltfrb.gov.ph/issuances/memorandum-circulars>. Both appear in search results but both returned **HTTP 403 "Just a moment…"** (Cloudflare managed challenge) to `curl`, to a headless Chromium session in headless and visible mode, and to a reader proxy. I could not read either page. The URL is identified in an LTFRB-memo news report (below) as the place to download the fare guide.
- **Official statement that the page is where fare guides live.** The Manila Times (news pointer, not an official document) reports a 2026-09-25 LTFRB memorandum authorizing operators to "download and print the applicable temporary fare guide … from the LTFRB website at https://ltfrb.gov.ph/fare-rates/". It also says the temporary guide is **valid only until 2026-12-31**, and that from 2027-01-01 operators must display the official fare guide/matrix issued by the LTFRB. Source: <https://www.manilatimes.net/2026/09/30/news/ltfrb-fast-tracks-fare-guide-for-nationwide-puv-fare-hike/2435623> (accessed 2026-10-09).
- **Memorandum circular number:** not found. Do not cite a circular number. The only LTFRB circular number I saw in this research was MC 2026-059 (fixed TNVS pickup fare, outside this ticket's scope), via a search snippet of a PNA page I could not open.

### What is in effect (news pointers quoting the DOTr/LTFRB announcement)

Timeline: LTFRB announced the new fares on 2026-03-17. They were then suspended. DOTr Secretary Giovanni Lopez approved implementation on 2026-09-25, effective **Monday 2026-09-28**.

| Item | Value | Source |
|---|---|---|
| Traditional jeepney | ₱13 → **₱14** for the first 4 km, **+₱2 per succeeding km** (was ₱1.80) | Manila Times 2026-09-25 quoting Lopez; Philstar 2026-09-27; Rappler 2026-09-28 |
| Modern jeepney | ₱15 → **₱17** for the first 4 km, **+₱2.40 per succeeding km** (was ₱2.20) | same |
| Ordinary city/Metro Manila bus | **₱15 for the first 5 km, +₱2.49 per succeeding km** | Philstar 2026-09-27; Rappler 2026-09-28 |
| Aircon city/Metro Manila bus | **₱18 for the first 5 km, +₱2.98 per succeeding km** | same |
| P2P | **15% increase on the existing fare of each route** | Philstar; Rappler |
| UV Express | provisional increase: **₱2.60/km traditional, ₱3/km modern** | Philstar; Rappler |
| Mandatory discount | **20%** for students, senior citizens, PWDs | Manila Times 2026-09-25 quoting Lopez |

URLs (all accessed 2026-10-09):
- <https://www.manilatimes.net/2026/09/25/news/dotr-approves-fare-adjustment-for-puvs/2433106> (Lopez quotes; jeepney numbers; 20% discount; "complete fare matrices … may be downloaded from the LTFRB's website")
- <https://www.philstar.com/headlines/2026/09/27/2559277/jeepney-fares-p14-starting-sept-28-buses-taxis-tnvs-also-hike-rates> (bus, P2P, UV Express numbers; says the LTFRB announced them "in an advisory posted Saturday" 2026-09-26)
- <https://www.rappler.com/philippines/fare-hikes-guide-jeepney-bus-taxi-uv-express-september-28-2026/> (same numbers, plus the jeepney fare-guide image below)

**Conflict to resolve at download time:** the ASTIG blog (<https://astig.ph/puv-fare-hike-2026-jeepney-p14-dotr-approval>, non-official) says the ordinary bus ₱15 covers "the first kilometer". Philstar and Rappler both say "first five kilometers". I used 5 km. The LTFRB bus fare guide will settle it.

### Jeepney distance table (best evidence of the structure)

The Rappler article embeds an image titled **"PUJ GENERAL FARE GUIDE — EFFECTIVE: September 28, 2026"** (<https://www.rappler.com/tachyon/2026/09/Public-Utility-Jeepney-Fare-Guide-September-2026.jpeg>, accessed 2026-10-09). It lists old and new fares for 1 to 50 km, regular and student/elderly/PWD. It looks like the LTFRB document, but Rappler credits it only as "the table shows the fare for public utility jeepneys", so `[UNVERIFIED]` as to origin until compared with the LTFRB download. Read from the image:

- 1 to 4 km: ₱14.00 regular, ₱11.25 student/elderly/PWD.
- 5 km ₱16.00, 6 km ₱18.00, 10 km ₱26.00, 20 km ₱46.00, 50 km ₱106.00. That is exactly ₱14 + ₱2 × (km − 4).
- Discounted column is 80% of regular, rounded to the nearest ₱0.25 (14.00 → 11.25; 106.00 → 84.75).
- The old table moved in uneven ₱1.75/₱2.00 steps because it rounded ₱1.80/km to the quarter peso. The new ₱2.00/km is exact, so no rounding question arises for traditional jeepneys.
- **Modern jeepney (₱2.40/km) rounding rule is unknown** (whole table not seen). `[UNVERIFIED]`. Compute from the LTFRB modern-jeepney table, not from the formula.

### What could NOT be found or verified

- Any LTFRB-hosted document (page, PDF, or memorandum circular) for the 2026 fares. The site blocked me. This is the single biggest gap.
- The modern jeepney, city bus, UV Express, and P2P fare tables themselves.
- A UV Express minimum fare or first-N-km rule. The "₱13 for the first 4 km" figure for UV Express appears only on a non-primary AI-generated encyclopedia page, which I did not use.
- Per-route P2P fares (old or new). The 15% rule needs the old fare for each route.
- The LTFRB FOI portal has "UV Express" and "UV Express Fare Matrix" entries (<https://www.foi.gov.ph/agencies/ltfrb/uv-express>, <https://www.foi.gov.ph/requests/uv-express-fare-matrix>). Both are `foi.gov.ph` pages; the first returned 403 to me and the second appeared only as a search-result snippet ("Published by LTFRB on Jan. 18, 2024"), which pre-dates the 2026 increase. A pointer only.
- Other news outlets and mirrors: `pna.gov.ph` served a "We are going live shortly" holding page and 404s on 2026-10-09; `pia.gov.ph`, `dotr.gov.ph`, and `inquirer.net` returned 403. The PIA article "LTFRB reminds PUV operators, drivers to display updated fare matrix" and the PNA articles 1284951 and 1285005 exist (found in search results) but I could not read them.

### Action that unblocks `collected` for these modes

One teammate opens <https://ltfrb.gov.ph/fare-rates/> in a normal browser, downloads the PUJ (traditional and modern), city-bus (ordinary and aircon), UV Express, and P2P guides, commits them under the repo (for example `data/pack/sources/`) with the retrieval date, and reads the real first-N-km rule and P2P route fares off them. That turns the rows above from `[UNVERIFIED]` into `collected` and resolves the bus "first 1 km vs first 5 km" conflict.

## 2. MRT-3 (DOTr-MRT3)

- **Issuing body:** DOTr (MRT-3 operator site "Official DOTr-MRT3 Website").
- **Regular fare matrix:** <https://www.dotrmrt3.gov.ph/fare-matrix.pdf> ("METRO RAIL TRANSIT LINE 3 — FARE MATRIX | REGULAR"; accessed 2026-10-09). One-page image PDF; I rendered it and read it. HTTP `Last-Modified: 2026-04-10`; PDF creation date 2026-01-19. **No effective date is printed on the matrix.**
- **Structure:** 13 stations × 13 stations (North Ave., Quezon Ave., GMA Kamuning, Araneta-Cubao, Santolan-Annapolis, Ortigas, Shaw Blvd., Boni, Guadalupe, Buendia, Ayala, Magallanes, Taft Ave.). Fare steps: ₱13, ₱16, ₱20, ₱24, ₱28. Minimum ₱13 (up to 2 stations); maximum ₱28 (North Ave. ↔ Taft Ave.). The matrix is symmetric in what I read.
- **Student / senior / PWD matrix:** <https://www.dotrmrt3.gov.ph/discounted-fare-matrix.pdf> ("FARE MATRIX | STUDENTS, SC, PWD"), ₱6 to ₱14 in whole pesos. Same `Last-Modified` date. This is the 50% concession matrix, not the all-passenger one.
- **50% across-the-board discount since 2026-03-23:** stated on the official LRTA page (<https://www.lrta.gov.ph/tickets-and-fares>): "Effective March 23, 2026, the Department of Transportation (DOTr) implemented a 50% across-the-board fare discount for all LRT-2 and MRT-3 passengers." The page was last modified 2026-10-09, so the discount is still presented as live. Single-journey tickets and stored-value cards both get it. News pointers: Manila Times 2026-03-20 (<https://www.manilatimes.net/2026/03/20/news/national/mrt-3-lrt-2-fares-cut-by-50-starting-march-23/2303928>), Philstar post quoting DOTr-MRT3 (<https://x.com/PhilippineStar/status/2034889146049946081>, read via a mirror). Reported discounted range ₱6.50 to ₱14 (Inquirer headline snippet; ASTIG blog). No end date announced ("until further notice" per the ASTIG blog's reading of Inquirer; the Inquirer article itself did not load). `[UNVERIFIED]` that it still holds on the day of the demo.
- **Not found:** an all-passenger 50% MRT-3 matrix on `dotrmrt3.gov.ph` (the site's news list has no March 2026 item; its latest item is dated 2026-09-30 and is unrelated). So the discounted MRT-3 value is "regular matrix × 0.5" by the LRTA statement, not a number read from a DOTr-MRT3 table. Rounding for single-journey tickets on MRT-3 is unknown.

## 3. LRT-1 (LRMC)

- **Issuing body:** Light Rail Manila Corporation (private operator), fares set by DOTr Rail Regulatory Unit decision.
- **Effective date:** **2025-04-02**. Announcement: <https://lrmc.ph/2025/02/18/new-lrt-1-fares-effective-2-april-2025> (accessed 2026-10-09), citing a DOTr notice dated 2025-02-14. Stated formula: **boarding fare ₱16.25 + ₱1.47 per km**.
- **Matrix page:** <https://lrmc.ph/our-business-featured/fare-matrix/> (two PNGs, page modified 2025-03-31). Direct images:
  - <https://lrmc.ph/wp-content/uploads/2025/03/New-SJT-fare-matrix-effective-April-2-2025-1.png>
  - <https://lrmc.ph/wp-content/uploads/2025/03/New-SVC-fare-matrix-effective-April-2-2025-1.png>
- **Gotcha:** the two image **filenames are swapped relative to the titles painted on them**. The file named `…SJT…` is titled "New LRT-1 **Stored Value** Fare Matrix" (values ₱16 to ₱52 including the diagonal); the file named `…SVC…` is titled "New LRT-1 **Single Journey** Fare Matrix" (values ₱20 to ₱55). Trust the painted titles. The ranges also agree with a third-party table (mypinas.ph), which I did not otherwise rely on.
- **Structure:** 25×25 station matrix (Dr. Santos, Ninoy Aquino Avenue, PITX, MIA Road, Redemptorist-Aseana, Baclaran, EDSA, Libertad, Gil Puyat, Vito Cruz, Quirino, Pedro Gil, UN Avenue, Central, Carriedo, D. Jose, Bambang, Tayuman, Blumentritt, Abad Santos, R. Papa, 5th Avenue, Monumento, Balintawak, Fernando Poe Jr.). Both matrices say "Effective April 2, 2025". Stored value, as read from the image: ₱17 (adjacent stations) to ₱52 (Dr. Santos ↔ Fernando Poe Jr.), with ₱16 printed on the same-station diagonal. Single journey: ₱20 minimum to ₱55 maximum.
- **Superseded, do not use:** the 2023 fares (₱13.29 + ₱1.21/km, SVC ₱14 to ₱35, SJT ₱15 to ₱35): <https://lrmc.ph/2023/07/31/new-fares-for-lrt-1-starting-august-2>.
- **Discount:** 50% for students, senior citizens, PWDs; ID presented at teller for SJT (<https://lrmc.ph/2025/08/13/lrmc-simplifies-discounted-lrt-1-fares-for-students-senior-citizens-and-pwds>).
- **2026 across-the-board 50% cut:** the LRTA page and DOTr statements I read name only LRT-2 and MRT-3. The ASTIG blog says LRT-1 was excluded because LRMC is private (<https://astig.ph/mrt3-lrt2-half-price-fares-2026-schedule-guide>). I found **no LRMC statement either way**; LRMC's own site still shows the April 2025 matrix and was last updated for this in 2025. Treat "LRT-1 is full fare" as `[UNVERIFIED]` beyond the absence of any official LRT-1 discount notice.

## 4. LRT-2 (LRTA)

- **Issuing body:** Light Rail Transit Authority (government); fare approved by DOTr.
- **Page:** <https://www.lrta.gov.ph/tickets-and-fares> (modified 2026-10-09). Base matrix image: <https://www.lrta.gov.ph/wp-content/uploads/2025/09/FareMatrixLine2_50Discount-conv-1.png> ("LRTA-2 Fare Matrix — Effective August 2, 2023", stored-value). Footnote on it: 50% for students from 2025-06-20, senior citizens and PWDs from 2025-07-16.
- **Fare adjustment notice:** <https://www.lrta.gov.ph/lrt-2-fare-adjustment> (posted 2023-08-01). Boarding ₱13.29 + ₱1.21/km. SVC minimum ₱14, maximum ₱33 (Recto ↔ Antipolo); SJT minimum ₱15, maximum ₱35. The matrix image agrees: the lowest fare between two distinct stations is ₱14 (for example J. Ruiz ↔ Gilmore); the ₱13 cells are the same-station diagonal.
- **2026 discounted matrices (effective 2026-03-23):**
  - Stored value, 50% discounted: <https://www.lrta.gov.ph/wp-content/uploads/2026/04/Discount_Matrix_OriginSVC_SJT_1.png> (₱7.00 to ₱16.50 between distinct stations; ₱6.50 on the same-station diagonal).
  - Second matrix: <https://www.lrta.gov.ph/wp-content/uploads/2026/04/Discount_Matrix_OriginSVC_SJT_2.png> (₱8 to ₱18, whole pesos). Its title is cropped out of the image; the values are consistent with the single-journey ticket matrix (half of ₱15 to ₱35 rounded to whole pesos), so I take it as SJT `[UNVERIFIED]` by title.
  - Both matrices looked symmetric in the cells I spot-checked (Recto ↔ Legarda, Recto ↔ Antipolo, Santolan ↔ Marikina). I did not check every cell.
- **Structure:** 13×13 station matrix (Recto, Legarda, Pureza, V. Mapa, J. Ruiz, Gilmore, Betty Go, Cubao, Anonas, Katipunan, Santolan, Marikina, Antipolo).
- **Not found:** whether the 2026 half-price scheme has an end date (none stated on the LRTA page).

## 5. Discounts (brief)

| Mode | Student / senior / PWD | Source |
|---|---|---|
| Jeepney, bus, UV Express | 20% mandatory | Manila Times quoting Lopez; the jeepney guide image shows the discounted column (80%, quarter-peso rounding) |
| MRT-3 | 50% (since 2025). The DOTr-MRT3 post says the same concession is maintained alongside the 2026 all-passenger cut | dotrmrt3.gov.ph discounted matrix (₱6 to ₱14, whole pesos); Philstar/DOTr-MRT3 post |
| LRT-2 | 50% (students 2025-06-20; SC/PWD 2025-07-16) | LRTA matrix footnote |
| LRT-1 | 50% (ID at teller for SJT) | LRMC 2025-08-13 |

## 6. Recommendation: pack classes

| Mode | Class | What to store, and the cite |
|---|---|---|
| LRT-1 | `collected` | Transcribe the needed station pairs from the LRMC matrix (SVC and SJT). `source`: "LRMC fare matrix effective 2025-04-02", URL above, accessed 2026-10-09. Note the swapped filenames. |
| LRT-2 | `collected` | Store the full fare from the LRTA matrix and a separate `discount: 50%, since 2026-03-23` overlay with the LRTA page as its source; or store the 2026 half-price values directly from the 2026 LRTA matrix. Cite both. |
| MRT-3 | `collected` | Full fare from `fare-matrix.pdf`, plus the same 50% overlay, citing the LRTA page (an official statement) because DOTr-MRT3 posts no all-passenger discount matrix. Say "no end date announced" in the app. |
| Traditional jeepney | `collected` after the LTFRB download, else `known` | ₱14 first 4 km + ₱2/km, from the PUJ General Fare Guide effective 2026-09-28. Fare per segment needs km from OSM or team estimate (`known`). |
| Modern jeepney | same | ₱17 first 4 km + ₱2.40/km. Read the actual table for rounding. |
| City bus (ordinary/aircon) | same | ₱15/₱18 first 5 km + ₱2.49/₱2.98/km. Confirm the "first 5 km" rule on the LTFRB download. |
| UV Express | `known` or `mock` | ₱2.60/km traditional or ₱3/km modern is provisional and has no minimum. A teammate who rides a UV Express route can give a `known` fare. Otherwise `mock` with the "sample data" tag. |
| P2P | `known` (own route) or `mock` | No per-route table found. Do not apply "+15%" to a guessed old fare and call it `collected`. |

Implementation notes for the data owner:
- The fare date matters. For every fare store `source`, URL, `effective`, and `retrieved: 2026-10-09`. The pack shows "data as of" so a viewer can see that road fares changed on 2026-09-28 and that the LTFRB temporary guide expires 2026-12-31.
- Keep the rail discount as its own field so one switch fixes the pack if DOTr ends it.
- Do not use the 2023 LRT matrices or any pre-2026-09-28 jeepney/bus fare as current.

## Sources

Primary (official, read):
- LRMC: <https://lrmc.ph/2025/02/18/new-lrt-1-fares-effective-2-april-2025>, <https://lrmc.ph/our-business-featured/fare-matrix/> and the two matrix PNGs above, <https://lrmc.ph/2025/08/13/lrmc-simplifies-discounted-lrt-1-fares-for-students-senior-citizens-and-pwds>, <https://lrmc.ph/2023/07/31/new-fares-for-lrt-1-starting-august-2>, <https://lrmc.ph/newsroom/news-releases> (no 2026 fare item seen).
- LRTA: <https://www.lrta.gov.ph/tickets-and-fares>, <https://www.lrta.gov.ph/lrt-2-fare-adjustment>, and the three matrix PNGs above.
- DOTr-MRT3: <https://www.dotrmrt3.gov.ph/fare-matrix.pdf>, <https://www.dotrmrt3.gov.ph/discounted-fare-matrix.pdf>, <https://www.dotrmrt3.gov.ph/news>.

Primary but not readable (blocked):
- LTFRB: <https://ltfrb.gov.ph/fare-rates/>, <https://ltfrb.gov.ph/issuances/memorandum-circulars> (HTTP 403, Cloudflare bot challenge).
- DOTr, PIA, FOI portal, Inquirer, PNA (403 or holding page on 2026-10-09).

News, used only as pointers to the official announcement (not official documents):
- The Manila Times: <https://www.manilatimes.net/2026/09/25/news/dotr-approves-fare-adjustment-for-puvs/2433106>, <https://www.manilatimes.net/2026/09/30/news/ltfrb-fast-tracks-fare-guide-for-nationwide-puv-fare-hike/2435623>, <https://www.manilatimes.net/2026/03/20/news/national/mrt-3-lrt-2-fares-cut-by-50-starting-march-23/2303928> (paywalled beyond the lead).
- Philstar: <https://www.philstar.com/headlines/2026/09/27/2559277/jeepney-fares-p14-starting-sept-28-buses-taxis-tnvs-also-hike-rates>; <https://x.com/PhilippineStar/status/2034889146049946081>.
- Rappler: <https://www.rappler.com/philippines/fare-hikes-guide-jeepney-bus-taxi-uv-express-september-28-2026/>.
- ASTIG (blog, lowest trust, used only for the discount-scope remarks and to flag the 1 km vs 5 km conflict): <https://astig.ph/mrt3-lrt2-half-price-fares-2026-schedule-guide>, <https://astig.ph/puv-fare-hike-2026-jeepney-p14-dotr-approval>.
