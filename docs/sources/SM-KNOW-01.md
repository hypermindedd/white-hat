Here is the result of "fetch" for the Page with URL https://app.notion.com/p/3c46f520f93e81bb9b49e7112bb8738e as of 2026-08-22T09:22:48.457Z:
<page url="https://app.notion.com/p/3c46f520f93e81bb9b49e7112bb8738e">
<ancestor-path>
<parent-page url="https://app.notion.com/p/3c36f520f93e81459ac0fc036497750d" title="SECOND MIND — V1 Build Project"/>
<ancestor-2-page url="https://app.notion.com/p/84e6f520f93e82fb95d6812289f80363" title="Teamspace Home"/>
</ancestor-path>
<properties>
{"title":"SM-KNOW-01 — SECOND MIND Decision Ontology v1.0"}
</properties>
<iconMetadata>null</iconMetadata>
<content>
<callout icon="✅" color="green_bg">
<b>AUDIT STATUS — CANONICAL CONSISTENCY PASS 1 (2026-08-22)</b>

Decision Object Schema این سند مرجع semantic کل زنجیره است. Downstream documents حق تعریف schema ناسازگار ندارند.
</callout>

<p>
<b>Document ID:</b> SM-KNOW-01  
</p>

<p>
<b>Version:</b> 1.0  
</p>

<p>
<b>Status:</b> Canonical Knowledge Specification — Candidate for Freeze  
</p>

<p>
<b>Knowledge Class:</b> Foundational Ontology / Runtime Reference Contract  
</p>

<p>
<b>Behavioral Authority:</b> SM-GPT-INS-001 — Master System Instructions v1.0  
</p>

<p>
<b>V1 Scope:</b> FROZEN  
</p>

<b>Runtime:</b> GPTs-first / Runtime-portable

---

# DOCUMENT PURPOSE — هدف سند

این سند زبان رسمی و مدل مفهومی Decision در SECOND MIND V1 را تعریف می‌کند: سیستم دقیقاً چه چیزی را به‌عنوان موضوع تصمیم می‌فهمد، چگونه آن را ساختاربندی می‌کند، Fact / Assumption / Inference / Unknown را چگونه جدا نگه می‌دارد و چه چیزی را حق ندارد اختراع کند.

این سند Prompt نیست؛ <b>Knowledge Contract</b> برای Decision Challenge Engine است.

# CANONICAL FOUNDATION

Decision Ontology بر اصول زیر بنا شده است:

1. <b>One Decision Object, Multiple Modes</b> — تمام ۷ Mode از یک Ontology مشترک استفاده می‌کنند.
2. <b>Explicit Before Inferred</b> — داده صریح بر استنباط اولویت دارد.
3. <b>Unknown Is a Valid State</b> — نبود داده با اختراع پر نمی‌شود.
4. <b>User Claim Is Not Automatically Fact</b> — گفته کاربر از حقیقت خارجی جدا می‌ماند.
5. <b>Decision-Relevance Over Data Collection</b> — فقط اطلاعات تصمیم‌ساز prominent می‌شوند.
6. <b>Preserve Provenance</b> — منشأ Claim/Evidence حفظ می‌شود.
7. <b>Preserve Uncertainty</b> — Normalization ابهام واقعی را پنهان نمی‌کند.
8. <b>Minimal Sufficient Representation</b> — Ontology به CRM تبدیل نمی‌شود.
9. <b>No Scope Expansion Through Ontology</b> — field جدید نباید capability جدید بسازد.
10. <b>Runtime-Neutral Core</b> — Core به GPT UI وابسته نیست.

# DECISION OBJECT

Decision Object واحد استانداردی است که SECOND MIND آن را Challenge می‌کند: representation ساختاریافته یک consequential question, commitment, position, option, idea, plan, claim, action, artifact یا choice که کاربر می‌خواهد پیش از تصمیم/اقدام/ساخت/ارسال/انتشار/سرمایه‌گذاری/انتخاب/دفاع ارزیابی شود.

Canonical conceptual fields:

```txt
object_id
decision_type
decision_statement
decision_question
objective
desired_outcome
context
options
constraints
stakeholders
time_horizon
commitment
claims
evidence
facts
assumptions
inferences
unknowns
selected_mode
input_sources
context_sufficiency
ontology_warnings
```

در GPTs این object internal است؛ در Product آینده می‌تواند typed backend object شود.

# DECISION TYPES

Allowed Frozen V1 types:

1. DECISION
2. IDEA
3. PLAN
4. STRATEGY
5. CLAIM
6. ACTION
7. OPTION
8. PROPOSAL
9. CAMPAIGN
10. PRODUCT
11. CAREER\_MOVE
12. INVESTMENT
13. MESSAGE
14. DOCUMENT

Classification برای Routing است و نباید Analysis را محدود کند.

# DECISION STATEMENT / QUESTION / OBJECTIVE

<p>
<b>Decision Statement</b> بیان neutral و کوتاه موضوع است.  
</p>

<p>
<b>Decision Question</b> سؤال judgment-ready است که Verdict باید پاسخ دهد.  
</p>

<b>Objective</b> مشخص می‌کند «بهتر» یعنی چه.

مثال:

<p>
Statement: افزایش بودجه تبلیغات.  
</p>

<p>
Question: آیا بودجه تبلیغات باید دو برابر شود؟  
</p>

Objective: افزایش qualified leads بدون تخریب CAC.

این سه مفهوم نباید merge شوند.

# OBJECTIVE CONTRACT

Objective باید outcome-oriented، decision-relevant و sufficiently specific باشد. Primary objective ترجیحاً یکی است؛ Secondary فقط اگر material باشد. Inferred objective باید uncertainty خود را حفظ کند. Objective conflict نباید silently حل شود.

# OPTIONS / CHOICE SET

User options و Generated alternatives جدا می‌مانند. Generated option هرگز به user نسبت داده نمی‌شود. Missing Option، Hybrid، Delay، Test و Do Nothing فقط در صورت Material بودن اضافه می‌شوند.

> Comparison quality cannot exceed Choice Set quality.

# CONSTRAINTS

Constraint categories: Financial، Time، Resource، Legal/Regulatory، Technical، Operational، Strategic، Contractual، Personal/Preference، Stakeholder.

Hard Constraint نقض‌ناپذیر/غیرقابل‌اجراست؛ Soft Constraint trade-off دارد. Preference نباید به Hard Constraint تبدیل شود.

# STAKEHOLDERS

فقط Stakeholderهای Material مدل می‌شوند: role، interest، influence، response uncertainty. Stakeholder belief با external truth متفاوت است، اما خود belief ممکن است decision-material باشد.

# COMMITMENT & IRREVERSIBILITY

Commitment از money، time، reputation، strategic lock-in، organizational dependency، contractual exposure و opportunity cost تأثیر می‌گیرد.

Irreversibility از Impact جداست. High-impact decision ممکن است reversible باشد و بالعکس.

# CLAIM MODEL

Claim propositionی است که user/document/source بیان می‌کند یا reasoning به آن وابسته است.

Types:

- Descriptive
- Causal
- Predictive
- Comparative
- Normative
- Quantitative
- Capability
- Constraint

Source types:

- USER
- USER\_DOCUMENT
- EXTERNAL\_PRIMARY
- EXTERNAL\_OFFICIAL
- EXTERNAL\_SECONDARY
- COMMUNITY
- MODEL\_INFERENCE
- UNKNOWN

Source و Truth جدا هستند.

# EVIDENCE MODEL

Evidence Claim را support، contradict یا contextualize می‌کند.

Quality dimensions:

- Credibility
- Relevance
- Directness
- Sufficiency
- Freshness where material

Heuristic levels:

- E0 None
- E1 Anecdotal
- E2 Indicative
- E3 Strong
- E4 Decision-grade

Evidence strength relational است: یک Evidence ممکن است برای یک Claim قوی و برای Claim دیگر ناکافی باشد.

# FACT

Fact = proposition sufficiently established for current decision analysis.

Subtypes:

- Verified External Fact
- User-Provided Operational Fact
- Document Fact

Operational acceptance برای جلوگیری از توقف غیرضروری مجاز است؛ اما market/external claims decision-critical نباید بدون Evidence verified شوند.

# ASSUMPTION

Assumption = unverified proposition that the reasoning or decision depends on.

برای هر assumption مهم: decision dependency، evidence strength، uncertainty، consequence if false، testability، validation method و criticality قابل‌مدل‌سازی است.

Critical Assumption:

> Assumption whose falsity could materially change or collapse the decision.

Conceptual criticality:

<b>Decision Dependency × Uncertainty × Consequence If Wrong</b>

# INFERENCE

Inference = proposition derived from facts/evidence but not directly established.

Correlation/sequence نباید به causation ارتقا یابد. Alternative explanations در causal inference باید در صورت Material بودن حفظ شوند.

# UNKNOWN

Unknown = decision-relevant information that cannot currently be established with adequate support.

Decision-Critical Unknown فقط Unknownی است که می‌تواند Verdict، Preferred Option، Timing، Commitment Size یا Next Best Action را تغییر دهد.

همه missing details Unknown مهم نیستند.

# EPISTEMIC BOUNDARIES

<p>
User confidence ≠ Evidence.  
</p>

<p>
Document polish ≠ Evidence quality.  
</p>

<p>
Source says X ≠ X is true.  
</p>

<p>
Absence of evidence ≠ Evidence of absence.  
</p>

<p>
Proxy evidence ≠ Direct evidence.  
</p>

<p>
Sample observation ≠ Population generalization.  
</p>

<p>
Prediction ≠ Fact.  
</p>

Precision ≠ Verification.

# EPISTEMIC STATE CHANGE

Propositionها فقط با تغییر Evidence/Context reclassify می‌شوند. مدل حق ندارد برای رسیدن به Verdict دلخواه Assumption را Fact کند.

# CONTEXT SUFFICIENCY

<p>
<b>SUFFICIENT:</b> Judgment meaningful ممکن است.  
</p>

<p>
<b>PARTIALLY\_SUFFICIENT:</b> analysis ممکن است ولی Material uncertainty وجود دارد؛ proceed + label + calibrate.  
</p>

<b>CRITICALLY\_INSUFFICIENT:</b> Decision Frame/Verdict بدون clarification arbitrary می‌شود؛ minimum decision-critical question پرسیده شود.

# MULTIPLE DECISIONS

اگر ورودی چند Decision دارد: identify → coupling check → primary decision → sub-decisions. Independent decisions نباید در یک Verdict collapse شوند.

Primary Decision بر اساس explicit intent، commitment، sensitivity و dependency انتخاب می‌شود؛ user intent اولویت دارد.

# DECISION FRAMING FAILURES

Ontology باید این Failureها را تشخیص دهد:

- Solution Bias
- False Binary
- Objective Confusion
- Symptom as Problem
- Normative Assumption
- Objective Conflict
- Multiple Decisions Collapsed

Reframing فقط وقتی انجام شود که materially decision-improving باشد.

# NORMALIZATION PIPELINE

```txt
RAW INPUT
↓
INPUT SOURCE CLASSIFICATION
↓
CONTENT EXTRACTION
↓
DECISION CANDIDATE DETECTION
↓
PRIMARY DECISION SELECTION
↓
DECISION TYPE CLASSIFICATION
↓
DECISION STATEMENT
↓
DECISION QUESTION
↓
OBJECTIVE EXTRACTION
↓
CONTEXT / OPTIONS / CONSTRAINTS / STAKEHOLDERS
↓
CLAIM EXTRACTION
↓
EPISTEMIC CLASSIFICATION
↓
EVIDENCE LINKING
↓
ASSUMPTION / UNKNOWN DETECTION
↓
CONTEXT SUFFICIENCY
↓
ONTOLOGY VALIDATION
↓
ROUTING READY DECISION OBJECT
```

Normalization باید user meaning را حفظ کند؛ silent completion، hidden verdict و category forcing ممنوع است.

# MULTIMODAL NORMALIZATION

<p>
Text → semantic extraction.  
</p>

<p>
Image/Screenshot → visible content + extraction uncertainty.  
</p>

<p>
Document → purpose/decision/claims/evidence/assumptions.  
</p>

<p>
URL → retrieved content + provenance.  
</p>

Voice → transcript + material transcription uncertainty.

Extraction uncertainty از Decision uncertainty جداست.

# DERIVED OBJECTS

این موارد input ontology نیستند و downstream Engine آنها را می‌سازد:

- Finding
- Blind Spot
- Failure Mode
- Must-Answer Question
- Change Condition
- Verdict
- Confidence
- Next Best Action

Verdict باید از Evidence → Assumptions → Material Findings → Critical Unknowns → Judgment trace شود.

# MATERIALITY & DECISION SENSITIVITY

Materiality = degree to which a proposition/finding can change decision direction, timing, commitment or verdict.

Decision Sensitivity = how much Judgment changes if the factor changes.

High-sensitivity factors باید برای Critical Assumptions، Unknowns، Must-Answer Question و NBA اولویت بگیرند.

# DEPENDENCY & ASSUMPTION STACK

Decision dependency chain ممکن است چنین باشد:

```txt
Decision
→ Assumption
→ Dependency
→ External Response
→ Outcome
```

Founder example:

```txt
Users have pain
→ pain is frequent
→ users seek solution
→ solution solves pain
→ users will pay
→ channel can reach them
→ economics work
```

Engine باید structural/deep assumptions را پیدا کند، نه فقط surface assumptions.

# EVIDENCE–ASSUMPTION MAP

```txt
Assumption
├── Supporting Evidence
├── Contradicting Evidence
├── Evidence Gap
└── Validation Method
```

# UNKNOWN–ACTION MAP

```txt
Unknown
→ Resolution Method
→ Expected Evidence
→ Decision Effect
```

# COMMITMENT–EVIDENCE MAP

```txt
Higher Commitment
+ Higher Irreversibility
+ Higher Downside
→ Higher Evidence Burden
```

Governance heuristic است، نه deterministic formula.

# CANONICAL EXAMPLES

<p>
<b>Founder Idea:</b> WTP stated by founder = Assumption until supported.  
</p>

<p>
<b>Pricing:</b> Sales decline = observation; “price caused it” = hypothesis/assumption.  
</p>

<p>
<b>Compare:</b> objective must define “better.”  
</p>

<p>
<b>Pitch Deck:</b> “Deck says TAM=\$12B” can be Fact; actual TAM remains unverified.  
</p>

<p>
<b>Message:</b> challenge communication decision before merely rewriting.  
</p>

<p>
<b>Strategy:</b> preserve theory of advantage, dependencies and trade-offs.  
</p>

<p>
<b>User certainty:</b> confidence in idea is not evidence.  
</p>

<p>
<b>Sample:</b> 7/8 interviewees saying “would buy” does not mean 87.5% of market will buy.  
</p>

<p>
<b>Investment:</b> do not infer risk tolerance or obligations.  
</p>

<b>False Binary:</b> preserve possibility of missing options.

# ONTOLOGY WARNINGS

Internal warnings may include:

AMBIGUOUS\_DECISION، UNKNOWN\_OBJECTIVE، CONFLICTING\_OBJECTIVES، INCOMPLETE\_CHOICE\_SET، UNVERIFIED\_CRITICAL\_CLAIM، CRITICAL\_ASSUMPTION\_UNTESTED، EVIDENCE\_CONFLICT، STALE\_EVIDENCE، MULTIPLE\_DECISIONS، DOCUMENT\_CLAIM\_NOT\_VERIFIED، HIGH\_IRREVERSIBILITY، HIGH\_STAKES\_DOMAIN، EXTRACTION\_UNCERTAINTY.

# ONTOLOGY FAILURE CODES

<p>
O01 NO\_DECISION\_OBJECT  
</p>

<p>
O02 WRONG\_PRIMARY\_DECISION  
</p>

<p>
O03 WRONG\_DECISION\_TYPE  
</p>

<p>
O04 BIASED\_DECISION\_FRAME  
</p>

<p>
O05 OBJECTIVE\_UNKNOWN\_IGNORED  
</p>

<p>
O06 OBJECTIVE\_CONFLICT\_IGNORED  
</p>

<p>
O07 USER\_CLAIM\_AS\_FACT  
</p>

<p>
O08 DOCUMENT\_CLAIM\_AS\_FACT  
</p>

<p>
O09 ASSUMPTION\_AS\_FACT  
</p>

<p>
O10 INFERENCE\_AS\_FACT  
</p>

<p>
O11 FABRICATED\_FIELD  
</p>

<p>
O12 PROVENANCE\_LOSS  
</p>

<p>
O13 CRITICAL\_UNKNOWN\_MISSED  
</p>

<p>
O14 CRITICAL\_ASSUMPTION\_MISSED  
</p>

<p>
O15 INCOMPLETE\_CHOICE\_SET\_IGNORED  
</p>

<p>
O16 GENERATED\_OPTION\_MISATTRIBUTED  
</p>

<p>
O17 CONTEXT\_SUFFICIENCY\_ERROR  
</p>

<p>
O18 MULTIPLE\_DECISIONS\_COLLAPSED  
</p>

<p>
O19 STALE\_EVIDENCE\_IGNORED  
</p>

O20 EXTRACTION\_UNCERTAINTY\_IGNORED

# ONTOLOGY VALIDATION

Before routing verify:

1. real Decision Object exists
2. primary decision identifiable
3. decision type plausible
4. statement neutral
5. judgment question clear
6. objective sufficiently understood
7. options preserved
8. constraints correctly represented
9. claims separated from evidence
10. F/A/I/U defensible
11. critical unknowns visible
12. provenance preserved
13. context sufficiency classified
14. warnings identified
15. object routing-ready

# ACCEPTANCE CRITERIA

Ontology must support all 14 Frozen Decision Types، all Frozen input types، all 7 Modes، preserve intent/provenance/uncertainty، distinguish Decision/Objective/Claim/Evidence/F/A/I/U، identify Critical Assumptions and Unknowns، distinguish generated alternatives، support Verdict traceability and future typed backend migration—without adding a new V1 capability.

# DOWNSTREAM CONTRACTS

<p>
SM-KNOW-02 consumes this ontology for Core Capability contracts.  
</p>

<p>
SM-KNOW-03 uses ontology fields for conditional triggers.  
</p>

<p>
SM-KNOW-04 may emphasize fields but must not fork ontology.  
</p>

<p>
SM-KNOW-05 derives Judgment from ontology + findings.  
</p>

<p>
SM-KNOW-06 renders derived intelligence, not raw schema.  
</p>

SM-KNOW-07 evaluates ontology failures.

# FREEZE CRITERIA

Freeze when all 14 types normalize correctly، F/A/I/U classification is stable، Claim/Evidence separation passes adversarial tests، Decision/Objectives are consistently separated، Context Sufficiency works، multi-decision inputs are handled، no downstream specification requires ontology redesign، and no Scope expansion exists.

# NEXT ARTIFACT

<b>SM-KNOW-02 — SECOND MIND Core Capability Contracts v1.0</b>

It must define all 11 Frozen Core Cognitive Capabilities as executable contracts: ID، Purpose، Ontology Inputs، Activation، Reasoning Objective، Required Checks، Derived Outputs، Priority، Interactions، Failure Modes، Quality Gates، Mode weighting، Examples and Evaluation criteria.

---

# FINAL CANONICAL CONTRACT

```txt
RAW HUMAN INPUT
↓
NORMALIZATION
↓
DECISION OBJECT
↓
EPISTEMIC STRUCTURE
↓
ROUTING
↓
CHALLENGE
↓
DERIVED FINDINGS
↓
JUDGMENT
↓
CONFIDENCE
↓
NEXT BEST ACTION
```

> <b>If the Decision Object is wrong, every intelligent step after it can be precisely wrong.</b>

Decision Ontology therefore is the semantic foundation of the SECOND MIND Decision Challenge Engine.
</content>
</page>
