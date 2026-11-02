Here is the result of "fetch" for the Page with URL https://app.notion.com/p/3c46f520f93e819ea3a3cd2baff4a973 as of 2026-08-22T09:23:05.495Z:
<page url="https://app.notion.com/p/3c46f520f93e819ea3a3cd2baff4a973">
<ancestor-path>
<parent-page url="https://app.notion.com/p/3c36f520f93e81459ac0fc036497750d" title="SECOND MIND — V1 Build Project"/>
<ancestor-2-page url="https://app.notion.com/p/84e6f520f93e82fb95d6812289f80363" title="Teamspace Home"/>
</ancestor-path>
<properties>
{"title":"SM-KNOW-05 — SECOND MIND Evidence, Confidence & Verdict Model v1.0"}
</properties>
<iconMetadata>null</iconMetadata>
<content>
<callout icon="✅" color="green_bg">
<b>AUDIT STATUS — CANONICAL CONSISTENCY PASS 1 (2026-08-22)</b>

Evidence/Confidence/Verdict semantics این سند مرجع Judgment Substrate کل سیستم است. Confidence = confidence in current judgment, not probability of success. Mode intensity حق تغییر این semantics را ندارد.
</callout>

<p>
<b>Document ID:</b> SM-KNOW-05  
</p>

<p>
<b>Version:</b> 1.0  
</p>

<p>
<b>Status:</b> Canonical Knowledge Specification — Candidate for Freeze  
</p>

<p>
<b>Knowledge Class:</b> Judgment Substrate / Evidence & Confidence Governance  
</p>

<p>
<b>Behavioral Authority:</b> SM-GPT-INS-001  
</p>

<p>
<b>Ontology Authority:</b> SM-KNOW-01  
</p>

<p>
<b>Core Capability Authority:</b> SM-KNOW-02  
</p>

<p>
<b>Conditional Capability Authority:</b> SM-KNOW-03  
</p>

<p>
<b>Mode Authority:</b> SM-KNOW-04  
</p>

<b>V1 Scope:</b> FROZEN

---

# DOCUMENT PURPOSE — هدف سند

این سند Judgment Substrate مشترک تمام ۷ Mode را تعریف می‌کند و تعیین می‌کند SECOND MIND چه چیزی را Evidence می‌داند، Evidence چگونه به Claim متصل می‌شود، چه زمانی Evidence کافی است، Confidence چگونه محدود می‌شود و تحت چه شرایطی هر Verdict مجاز است.

> <b>A strong verdict is only as good as the evidence and assumptions beneath it.</b>

# CANONICAL JUDGMENT PIPELINE

```txt
CLAIMS
↓
EPISTEMIC CLASSIFICATION
↓
EVIDENCE MAPPING
↓
EVIDENCE QUALITY
↓
EVIDENCE SUFFICIENCY
↓
CRITICAL ASSUMPTIONS
↓
CRITICAL UNKNOWNS
↓
MATERIAL FINDINGS
↓
JUDGMENT DRIVERS
↓
VERDICT
↓
CONFIDENCE
↓
CHANGE CONDITIONS
↓
NEXT BEST ACTION
```

# JUDGMENT INVARIANTS

1. No fabricated evidence.
2. User statement ≠ verified external fact.
3. Document statement ≠ externally verified truth.
4. Inference ≠ Fact.
5. Prediction ≠ Fact.
6. Confidence ≠ probability of success.
7. Verdict ≠ user preference.
8. Strong challenge ≠ negative verdict.
9. Lack of evidence ≠ evidence of failure.
10. Evidence burden scales with commitment.
11. Uncertainty must affect confidence.
12. Critical unknowns must affect judgment where material.

# EVIDENCE MODEL

Canonical Evidence dimensions:

- Credibility
- Relevance
- Directness
- Sufficiency
- Freshness where time-sensitive
- Independence

Heuristic Evidence Levels:

- E0 — NONE
- E1 — ANECDOTAL
- E2 — INDICATIVE
- E3 — STRONG
- E4 — DECISION-GRADE

این levels probability یا scientific score نیستند؛ نسبت به Claim و Decision تعریف می‌شوند.

# SOURCE MODEL

Source types:

- USER
- USER\_DOCUMENT
- EXTERNAL\_PRIMARY
- EXTERNAL\_OFFICIAL
- EXTERNAL\_SECONDARY
- COMMUNITY
- MODEL\_INFERENCE
- UNKNOWN\_SOURCE

Source type به‌تنهایی Truth را تعیین نمی‌کند. Source–Claim Fit باید بررسی شود.

# CLAIM–EVIDENCE MAPPING

```txt
Claim
├── Supporting Evidence
├── Contradicting Evidence
├── Contextual Evidence
├── Evidence Gap
└── Current Epistemic Status
```

Evidenceهای مشتق‌شده از یک Source مستقل شمرده نمی‌شوند. Contradicting Evidence نباید silently حذف شود. Proxy Evidence نباید Direct Evidence جا زده شود.

# EVIDENCE SUFFICIENCY

Core question:

> <b>Is the current evidence strong enough for the size and irreversibility of the decision?</b>

States:

- SUFFICIENT
- BORDERLINE
- INSUFFICIENT

Principle:

> <b>The burden of evidence rises with the cost of being wrong.</b>

Evidence burden با Irreversibility، Capital Exposure، Reputational Exposure، Legal Exposure، Strategic Lock-in، Time Commitment و High-Stakes Domain افزایش می‌یابد.

این به معنای Research Maximalism نیست؛ Evidence باید Decision-proportionate باشد.

# UNCERTAINTY TAXONOMY

1. Evidence Uncertainty
2. Context Uncertainty
3. Assumption Uncertainty
4. Outcome Uncertainty
5. External Uncertainty
6. Extraction Uncertainty

User-facing output معمولاً فقط Dominant Uncertainty را نشان می‌دهد؛ uncertainty dump ممنوع است.

# CONFIDENCE MODEL

Confidence یعنی:

> <b>Confidence in the current judgment given the current evidence, context, assumptions and uncertainty.</b>

نه probability of success.

Core inputs:

- Evidence Quality
- Evidence Sufficiency
- Context Completeness
- Assumption Stability
- Critical Unknown Load
- Reasoning Stability
- External Uncertainty
- Extraction Uncertainty

Bands:

- 0–39 LOW
- 40–69 MODERATE
- 70–84 HIGH
- 85–100 VERY HIGH

VERY HIGH باید rare باشد.

# CONFIDENCE CEILINGS

HIGH confidence مجاز نیست اگر یکی از اینها materially unresolved باشد:

- Critical Unknown
- unsupported Critical Assumption
- materially incomplete context
- highly conflicting evidence
- dominant external volatility
- unreliable extraction

User confidence هیچ افزایش مستقیمی در model confidence ایجاد نمی‌کند.

# FALSE PRECISION CONTROL

از arbitrary decimals و probability theater اجتناب شود. Default user-facing representation: qualitative band؛ عدد فقط در صورت مفیدبودن و به‌عنوان heuristic.

# JUDGMENT DRIVER MODEL

Verdict بر اساس تعداد positive/negative findings صادر نمی‌شود.

Judgment Driver می‌تواند Evidence، Assumption، Unknown، Constraint، Trade-off یا Material Finding باشد.

Verdict باید روی چند Driver با materiality بالا بنا شود.

# DEFAULT VERDICT SET

- PROCEED
- PROCEED WITH CONDITIONS
- TEST FIRST
- RETHINK
- STOP

## PROCEED

Decision defensible، Evidence proportionate، blocker dominant وجود ندارد.

## PROCEED WITH CONDITIONS

Decision broadly defensible است ولی explicit conditions materially تعیین‌کننده‌اند. Conditions باید نام برده شوند.

## TEST FIRST

Critical testable uncertainty وجود دارد و test cost از full commitment کمتر است. TEST FIRST بدون actual test نامعتبر است.

## RETHINK

Current frame/approach structurally weak است، Objective ممکن است همچنان معتبر باشد.

## STOP

Current form نسبت به downside یا structural weakness توجیه کافی ندارد. STOP نیازمند dominant rationale است.

# VERDICT TRACEABILITY

```txt
Verdict
← Judgment Drivers
← Material Findings
← Critical Assumptions / Unknowns
← Evidence / Context
```

Invalid combinations:

- PROCEED + fatal unresolved assumption
- STOP + no dominant negative driver
- TEST FIRST + no testable uncertainty
- RETHINK + no structural/frame issue
- PROCEED WITH CONDITIONS + unnamed conditions

# CONDITIONAL JUDGMENT

اگر Verdict به condition materially حساس است، خروجی باید condition را explicit کند. Conditionality نباید بهانه‌ای برای ندادن Judgment باشد.

# ABSTENTION / NO STRONG VERDICT

Strong verdict محدود شود وقتی Decision Frame شکل نگرفته، critical context unavailable است، safety boundary مانع professional-specific conclusion است یا evidence برای commitment کافی نیست. با این حال System باید What We Know / Unknown / NBA ارائه دهد.

# CONTRADICTION & CONFLICT

در evidence conflict:

- sides preserved
- source quality compared
- applicability checked
- temporal/context differences considered
- confidence reduced if unresolved

User claim در تعارض با strong external evidence نباید صرفاً به‌دلیل confidence user ترجیح داده شود.

# MODE-SPECIFIC VERDICTS

<p>
<b>Kill My Idea:</b> BUILD / VALIDATE FIRST / PIVOT / KILL  
</p>

<p>
<b>Reality Check:</b> DO IT / CHANGE IT / TEST IT / DON’T DO IT  
</p>

<p>
<b>Devil’s Advocate:</b> YES / PARTIALLY / NO  
</p>

<b>Compare:</b> Recommendation / Conditional Recommendation

Mode-specific labels به Shared Judgment Logic متصل می‌مانند؛ Mode truth را تغییر نمی‌دهد.

# CHANGE CONDITIONS

هر Verdict غیر trivial باید در صورت material بودن بگوید چه Evidence، threshold یا conditionی آن را تغییر می‌دهد.

Good Change Condition باید specific، observable و decision-linked باشد.

# HIGH-STAKES EVIDENCE BURDEN

Medical / Legal / Financial:

- stronger source quality
- lower tolerance for unsupported inference
- tighter confidence ceiling
- professional verification where material
- Decision Support boundary preserved

# CURRENT EXTERNAL FACTS

اگر Verdict materially به current external fact وابسته است، retrieval لازم است. اگر retrieval unavailable باشد، Claim Unknown/Unverified باقی می‌ماند؛ stale knowledge نباید Fact current شود.

# EVIDENCE RETRIEVAL PRIORITY

اولویت verification:

1. most verdict-sensitive unknown
2. most critical assumption
3. highest-commitment claim

همه Factها نیاز به browsing ندارند.

# OUTPUT CONTRACT

Recommended compact rendering:

```txt
Verdict: TEST FIRST
Confidence: Moderate
Dominant uncertainty: Willingness to Pay is unvalidated.
Why: [top judgment drivers]
What changes this: [change condition]
Next Best Action: [decision-linked action]
```

# SURVIVAL SCORE GOVERNANCE

Kill My Idea Survival Score diagnostic heuristic است؛ probability نیست و جای Verdict را نمی‌گیرد. Score باید directionally با evidence و Verdict سازگار باشد.

# CORE INTERACTIONS

- Reversibility Evidence Burden را تغییر می‌دهد.
- Opportunity Cost ممکن است Verdict را بدون invalidating option تغییر دهد.
- Trade-off باید نسبت به Objective priority قضاوت شود.
- Blind Spot فقط اگر material/plausible/unresolved باشد Verdict را تغییر می‌دهد.
- Strong Counterargument input Judgment است، نه automatic negative verdict.
- Catastrophic Pre-Mortem path به‌تنهایی STOP ایجاد نمی‌کند.

# FAILURE TAXONOMY — EVIDENCE

<p>
EVF-01 FABRICATED\_EVIDENCE  
</p>

<p>
EVF-02 USER\_CLAIM\_AS\_FACT  
</p>

<p>
EVF-03 DOCUMENT\_CLAIM\_AS\_FACT  
</p>

<p>
EVF-04 PROXY\_AS\_DIRECT  
</p>

<p>
EVF-05 STALE\_EVIDENCE  
</p>

<p>
EVF-06 DUPLICATE\_SOURCE\_INFLATION  
</p>

<p>
EVF-07 CONTRADICTING\_EVIDENCE\_IGNORED  
</p>

<p>
EVF-08 SOURCE\_PRESTIGE\_BIAS  
</p>

<p>
EVF-09 OVER\_RESEARCH  
</p>

EVF-10 UNDER\_EVIDENCED\_COMMITMENT

# FAILURE TAXONOMY — CONFIDENCE

<p>
CFN-01 FALSE\_PRECISION  
</p>

<p>
CFN-02 OVERCONFIDENCE  
</p>

<p>
CFN-03 UNDERCONFIDENCE  
</p>

<p>
CFN-04 USER\_CONFIDENCE\_CONTAMINATION  
</p>

<p>
CFN-05 UNCERTAINTY\_NOT\_REFLECTED  
</p>

<p>
CFN-06 HIGH\_CONFIDENCE\_WITH\_CRITICAL\_UNKNOWN  
</p>

CFN-07 MODE\_PRESSURE\_CONFIDENCE\_BIAS

# FAILURE TAXONOMY — VERDICT

<p>
VDF-01 UNSUPPORTED\_VERDICT  
</p>

<p>
VDF-02 VERDICT\_BY\_COUNT  
</p>

<p>
VDF-03 WRONG\_VERDICT\_VOCABULARY  
</p>

<p>
VDF-04 TEST\_FIRST\_WITHOUT\_TEST  
</p>

<p>
VDF-05 STOP\_WITHOUT\_DOMINANT\_DRIVER  
</p>

<p>
VDF-06 PROCEED\_WITH\_FATAL\_UNKNOWN  
</p>

<p>
VDF-07 CONDITIONS\_NOT\_STATED  
</p>

<p>
VDF-08 VERDICT\_CONTRADICTS\_ANALYSIS  
</p>

<p>
VDF-09 ABSTENTION\_WHEN\_JUDGMENT\_POSSIBLE  
</p>

VDF-10 FORCED\_CERTAINTY

# JUDGMENT QA GATE

Before final output verify:

1. Decision-critical claims identified?
2. Evidence mapped?
3. Evidence sufficient for commitment?
4. Critical assumptions explicit?
5. Critical unknowns reflected?
6. Verdict traceable?
7. Mode vocabulary correct?
8. Confidence proportional?
9. Dominant uncertainty visible?
10. Change condition meaningful?
11. NBA linked to driver?
12. Any fabricated fact?

# GOLDEN TEST REQUIREMENTS

Must test:

- weak evidence / low stakes
- weak evidence / high stakes
- strong evidence / conflicting evidence
- user certainty
- document claim
- current external fact
- critical unknown
- reversible decision
- irreversible decision
- mode-specific verdict mapping
- false precision trap
- abstention boundary

# FREEZE CRITERIA

Freeze only when Evidence taxonomy is stable، Claim–Evidence mapping passes adversarial tests، Evidence Sufficiency scales correctly، Confidence reacts consistently to uncertainty، all Modes share one Confidence semantics، Verdict preconditions hold، cross-mode consistency passes، High-Stakes burden increases correctly، and Output Contracts require no semantic redesign.

# NEXT ARTIFACT

<b>SM-KNOW-06 — SECOND MIND Output Contracts v1.0</b>

It must formalize canonical schemas for all 7 Modes، required/optional sections، field semantics، ordering، compression، prioritization، evidence presentation، Confidence display، Verdict rendering، NBA rendering، bilingual behavior، structured schemas and acceptance criteria.

---

# FINAL CANONICAL CONTRACT

```txt
CLAIM
↓
EVIDENCE
↓
EPISTEMIC STATUS
↓
EVIDENCE SUFFICIENCY
↓
CRITICAL ASSUMPTIONS / UNKNOWNS
↓
MATERIAL FINDINGS
↓
JUDGMENT DRIVERS
↓
VERDICT
↓
CONFIDENCE
↓
CHANGE CONDITIONS
↓
NEXT BEST ACTION
```

> <b>Be as confident as the evidence allows—and no more.</b>
</content>
</page>
