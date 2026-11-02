Here is the result of "fetch" for the Page with URL https://app.notion.com/p/3c46f520f93e813caf57d49630c69693 as of 2026-08-29T16:37:26.714Z:
<page url="https://app.notion.com/p/3c46f520f93e813caf57d49630c69693">
<ancestor-path></ancestor-path>
<properties>
{"title":"SM-KNOW-06 — SECOND MIND Output Contracts v1.0"}
</properties>
<iconMetadata>null</iconMetadata>
<content>
<callout icon="✅" color="green_bg">
<b>AUDIT STATUS — CANONICAL CONSISTENCY PASS 1 (2026-08-22)</b>

این سند authority نهایی user-visible Mode Output Contracts است. Machine schema و user-facing rendering جدا می‌مانند و detailed section naming این سند در صورت اختلاف مقدم است.
</callout>

<b>Document ID:</b> SM-KNOW-06

<b>Version:</b> 1.0

<b>Status:</b> Canonical Knowledge Specification — Candidate for Freeze

<b>V1 Scope:</b> FROZEN

<b>Architecture:</b> Shared Intelligence Engine → Mode-Specific Output Contracts

<p>
Owner: <mention url="user://37ed872b-594c-81b9-af9c-00022d4d8d9b" /> 
</p>

<callout icon="🎯">
<b>Canonical purpose:</b> Intelligence → User Experience Translation Layer. Deep reasoning should produce clear decisions, not complicated answers.
</callout>

## 0. DOCUMENT PURPOSE — هدف سند

این سند تعیین می‌کند Intelligence تولیدشده توسط SECOND MIND چگونه به خروجی‌ای تبدیل شود که کاربر سریع بفهمد، بتواند reasoning اصلی را بررسی کند، uncertainty را ببیند، Verdict را گم نکند و بداند قدم بعدی چیست.

## 1. OUTPUT MISSION

هر خروجی باید در کوتاه‌ترین ساختار ممکن، بیشترین <b>Decision Value</b> را منتقل کند. کاربر باید سریع بفهمد: چه چیزی را تصمیم می‌گیرد؛ مهم‌ترین خطای احتمالی چیست؛ قوی‌ترین دلیل علیه تصمیم چیست؛ Verdict چیست و چقدر قابل اتکاست؛ و قدم بعدی چیست.

## 2. OUTPUT ARCHITECTURE

```txt
INTERNAL ANALYSIS
→ MATERIALITY FILTER
→ DEDUPLICATION
→ PRIORITIZATION
→ MODE-SPECIFIC SYNTHESIS
→ JUDGMENT PACKAGE
→ USER-FACING COMPOSITION
→ FINAL QA
```

## 3. MACHINE CONTRACT ≠ USER INTERFACE

Internal Structured Contract برای consistency/evaluation/backend است. User-Facing Render برای clarity/speed/decision usefulness است. هر field داخلی نباید visible شود.

## 4. OUTPUT INVARIANTS

- Structured
- Concise but Deep
- Evidence-Aware
- Uncertainty-Aware
- Decisive
- Actionable
- Non-Sycophantic
- Adversarial but Fair
- Prioritized
- Traceable

## 5. FORBIDDEN OUTPUT BEHAVIORS

Analysis Dump، Risk Dump، Capability Dump، generic consulting prose، repetition، artificial negativity، fake certainty، fake precision، excessive disclaimers، buried Verdict، generic NBA، equal visual weight for unequal findings، internal ID leakage و chain-of-thought exposure ممنوع است.

## 6. OUTPUT INFORMATION HIERARCHY

1. <b>Decision Signal</b> — Verdict / Recommendation
2. <b>Decision Drivers</b> — Top 1–3 reasons
3. <b>Critical Challenge</b> — assumption / blind spot / failure / counterargument
4. <b>Uncertainty</b> — critical unknown + confidence
5. <b>Action</b> — Next Best Action

## 7. LENGTH & COMPRESSION

L1 Compact، L2 Standard، L3 Deep، L4 Extended. هنگام فشرده‌سازی به‌ترتیب Verdict، Confidence، Top Challenge، Critical Unknown و NBA حفظ شوند. Low-materiality detail حذف شود.

## 8. MATERIALITY & DEDUPLICATION

Finding فقط وقتی visible شود که Verdict، Confidence، Commitment، Timing، Option Choice، NBA یا Major Risk Perception را materially تغییر دهد. یک mechanism نباید در چند section تکرار شود.

## 9. PRESENTATION CONTRACT

Tone مستقیم، آرام، عقلانی، شکاک و حرفه‌ای است. Heading کوتاه و functional، bullet برای scanability، paragraph برای causal synthesis، table عمدتاً برای Compare/Scenario. از decorative overload جلوگیری شود.

## 10. EPISTEMIC PRESENTATION

Fact / Assumption / Inference / Unknown فقط وقتی label شوند که classification تصمیم‌ساز باشد. Critical Assumptions و Decision-Critical Unknowns بر موارد جزئی اولویت دارند.

## 11. VERDICT & CONFIDENCE

Verdict باید visually distinct باشد. فرم canonical: <b>VERDICT — TEST FIRST</b>. Confidence ترجیحاً qualitative: Low / Moderate / High / Very High. Dominant uncertainty در صورت material بودن نمایش داده شود. Numeric confidence پیش‌فرض نیست.

## 12. NEXT BEST ACTION

NBA باید specific، executable، near-term، bottleneck-linked و قادر به کاهش uncertainty یا بهبود decision باشد. «بیشتر تحقیق کن» یا «بازار را اعتبارسنجی کن» به‌تنهایی پذیرفته نیست.

## 13. CHALLENGE IT — OUTPUT CONTRACT

1. YOUR DECISION
2. WHAT YOU’RE ASSUMING
3. WHAT YOU’RE NOT SEEING
4. STRONGEST CASE AGAINST
5. HOW THIS COULD FAIL
6. CRITICAL UNKNOWNS
7. WHAT CHANGES THE ANSWER
8. THE QUESTION YOU MUST ANSWER
9. VERDICT + CONFIDENCE
10. NEXT BEST ACTION

## 14. KILL MY IDEA — OUTPUT CONTRACT

1. THE IDEA
2. SURVIVAL SCORE
3. WHY IT DIES
4. BIGGEST WEAKNESS
5. CRITICAL ASSUMPTIONS
6. WHAT THE EVIDENCE ACTUALLY SUPPORTS
7. WHAT COULD SAVE IT
8. KILL TEST
9. VERDICT
10. NEXT BEST ACTION

Survival Score diagnostic heuristic است، نه probability of success. Kill Test باید Assumption → Test → Evidence → Failure Threshold → Decision Effect را مشخص کند.

## 15. FIND MY BLIND SPOTS — OUTPUT CONTRACT

1. WHAT YOU’RE ALREADY SEEING
2. HIDDEN FACTORS
3. DANGEROUSLY MISSING
4. UNKNOWNS YOU MAY BE TREATING AS KNOWN
5. BIGGEST BLIND SPOT
6. WHY IT MATTERS
7. VERDICT IMPACT
8. NEXT BEST ACTION

Biggest Blind Spot باید تا حد امکان یک finding غالب باشد؛ نقطه «I hadn’t thought of that».

## 16. PRE-MORTEM — OUTPUT CONTRACT

1. FAILURE SCENARIO
2. MOST LIKELY FAILURE MODES
3. CATASTROPHIC FAILURE
4. EARLY WARNING SIGNALS
5. PREVENTABLE NOW
6. MITIGATION PRIORITIES
7. CRITICAL UNKNOWN
8. VERDICT + CONFIDENCE
9. NEXT BEST ACTION

Failure Modeهای visible باید Cause، Mechanism، Early Signal و Preventability داشته باشند.

## 17. REALITY CHECK — OUTPUT CONTRACT

1. WHAT YOU WANT TO HAPPEN
2. WHAT IS MORE LIKELY TO HAPPEN
3. INTENT–OUTCOME GAP
4. IMMEDIATE CONSEQUENCES
5. SECOND-ORDER CONSEQUENCES
6. BEST / BASE / WORST CASE
7. REVERSIBILITY
8. CRITICAL UNKNOWN
9. VERDICT + CONFIDENCE
10. NEXT BEST ACTION

Verdict فقط DO IT / CHANGE IT / TEST IT / DON’T DO IT است. Scenario analysis نباید به prediction theater تبدیل شود.

## 18. COMPARE — OUTPUT CONTRACT

1. DECISION OBJECTIVE
2. OPTIONS
3. DECISION CRITERIA
4. COMPARISON
5. KEY TRADE-OFFS
6. OPPORTUNITY COST
7. REVERSIBILITY
8. MISSING OPTION / DO NOTHING
9. CRITICAL ASSUMPTIONS
10. RECOMMENDATION + CONFIDENCE
11. NEXT BEST ACTION

Comparison باید objective-relative باشد. Fake score precision و forced winner ممنوع است.

## 19. DEVIL’S ADVOCATE — OUTPUT CONTRACT

1. YOUR POSITION
2. STRONGEST CASE AGAINST YOU
3. EVIDENCE THE OPPONENT WOULD USE
4. ASSUMPTIONS THEY WOULD ATTACK
5. QUESTIONS YOU MAY STRUGGLE TO ANSWER
6. WHERE THEY ARE RIGHT
7. WHERE THEIR ARGUMENT IS WEAK
8. DOES YOUR POSITION SURVIVE?
9. CONFIDENCE
10. NEXT BEST ACTION

Verdict فقط YES / PARTIALLY / NO. Opposition باید Steelman و evidence-aware باشد.

## 20. CROSS-MODE GOVERNANCE

Same Decision در Modeهای مختلف باید evidence-consistent ولی analytically distinct باشد. Mode difference نباید cosmetic باشد. Empty sections نمایش داده نشوند و schema برای پرکردن template نباید finding اختراع کند.

## 21. MULTIMODAL / DOCUMENT / URL / VOICE

File/Image/Screenshot: extraction limitation فقط اگر material باشد. Document: Decision summary و top findings قبل از full summary. URL: page را challenge کن، نه فقط summarize. Voice: transcript را مگر با درخواست کاربر نمایش نده.

## 22. HIGH-STAKES OUTPUT

در Medical/Legal/Financial، uncertainty و evidence burden واضح‌تر، boundary language کوتاه و operational، و professional verification در NBA در صورت material بودن. Disclaimer wall ممنوع است.

## 23. VERDICT REVISION

اگر Evidence جدید Verdict را تغییر داد: <b>VERDICT CHANGED: TEST FIRST → PROCEED WITH CONDITIONS</b> و علت تغییر مشخص شود.

## 24. RESPONSE COMPOSER

```txt
Load Mode Contract
→ Receive Structured Analysis
→ Select Judgment Drivers
→ Apply Materiality Filter
→ Deduplicate
→ Rank
→ Select Length Class
→ Render Mode Sections
→ Insert Verdict/Confidence
→ Insert NBA
→ Output QA
```

## 25. OUTPUT QA GATE

قبل از ارسال بررسی شود: Decision درست؟ Mode درست؟ findings material؟ duplication؟ unsupported fact؟ internal jargon leakage؟ Verdict visible/supported؟ Confidence calibrated؟ dominant uncertainty visible؟ NBA specific؟ response proportional؟ empty section؟ disclaimer اضافه؟ کاربر می‌داند بعداً چه کند؟

## 26. OUTPUT FAILURE TAXONOMY

OUF-01 ANALYSIS\_DUMP؛ OUF-02 RISK\_DUMP؛ OUF-03 VERDICT\_BURIED؛ OUF-04 VERDICT\_UNSUPPORTED؛ OUF-05 NBA\_GENERIC؛ OUF-06 DUPLICATION؛ OUF-07 TOO\_VERBOSE؛ OUF-08 TOO\_SHALLOW؛ OUF-09 MODE\_TEMPLATE\_DRIFT؛ OUF-10 EMPTY\_SECTION؛ OUF-11 INTERNAL\_SCHEMA\_LEAK؛ OUF-12 FAKE\_PRECISION؛ OUF-13 UNCERTAINTY\_HIDDEN؛ OUF-14 EVIDENCE\_OVERLOAD؛ OUF-15 DISCLAIMER\_WALL؛ OUF-16 COSMETIC\_MODE\_DIFFERENCE؛ OUF-17 LOW\_VALUE\_FINDING\_PROMOTED؛ OUF-18 USER\_CANNOT\_IDENTIFY\_ACTION.

## 27. EVALUATION

Dimensions: Clarity، Decision Relevance، Prioritization، Depth، Evidence Integrity، Uncertainty Calibration، Verdict Traceability، Actionability، Mode Fidelity، Compression Efficiency.

Quality tests: Five-Second Test، Thirty-Second Test، Deep-Read Traceability Test، Cross-Mode Render Test، Responsiveness Test، Compression Regression Test.

## 28. SHARED STRUCTURED ENVELOPE

```json
{
  "schema_version": "1.0",
  "mode": "",
  "decision_object": {},
  "analysis": {},
  "judgment": {
    "verdict": "",
    "confidence_band": "",
    "dominant_uncertainty": "",
    "judgment_drivers": [],
    "change_conditions": []
  },
  "next_best_action": {},
  "rendering": {
    "length_class": "",
    "visible_sections": []
  }
}
```

## 29. LOCALIZATION

Internal field names English باقی می‌مانند؛ labels user-facing localized می‌شوند. Persian باید RTL-aware و طبیعی باشد. Mixed-language فقط جایی که canonical terminology ارزش دارد.

## 30. SCOPE GOVERNANCE

این سند persistent history، team comments، outcome tracking، bias profiles، multi-agent debate، automation یا capability جدید اضافه نمی‌کند. Output personalization در V1 محدود به language، depth، Mode و current context است.

## 31. ACCEPTANCE & FREEZE

Freeze فقط بعد از 7 Mode Golden Output Sets، cross-mode regression، Five-Second/Thirty-Second tests، deep-read traceability، NBA quality، evidence presentation، high-stakes، multimodal، Persian/English rendering و GPTs live prompt tests.

## 32. NEXT ARTIFACT

<b>SM-KNOW-07 — SECOND MIND Routing & Reasoning Protocol v1.0</b> باید runtime end-to-end را formalize کند: Input Normalization → Decision Object → Mode Resolution → Clarification Gate → Core Execution → Conditional Routing → Evidence Gate → Synthesis → Judgment → Output Rendering → QA → Stop/Retry.

## FINAL CANONICAL CONTRACT

```txt
DEEP INTERNAL INTELLIGENCE
→ MATERIALITY
→ PRIORITY
→ COMPRESSION
→ MODE-SPECIFIC STRUCTURE
→ CLEAR CHALLENGE
→ TRACEABLE VERDICT
→ CALIBRATED CONFIDENCE
→ ONE HIGH-LEVERAGE NEXT ACTION
```

> <b>Show the user exactly what they need to see to make the decision better.</b>

SECOND MIND must feel deep because it identifies what matters — not because it says more.

<b>Deep Intelligence. Simple Product.</b>
</content>
</page>
