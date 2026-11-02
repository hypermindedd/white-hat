Here is the result of "fetch" for the Page with URL https://app.notion.com/p/3c36f520f93e81459ac0fc036497750d as of 2026-09-12T08:20:22.093Z:
<page url="https://app.notion.com/p/3c36f520f93e81459ac0fc036497750d" icon="🧠">
<ancestor-path>
<parent-page url="https://app.notion.com/p/84e6f520f93e82fb95d6812289f80363" title="Teamspace Home"/>
</ancestor-path>
<properties>
{"title":"SECOND MIND — V1 Build Project"}
</properties>
<iconMetadata>{"type":"emoji","emoji":"🧠"}</iconMetadata>
<content>
<callout icon="✅" color="green_bg">
<b>CANONICAL CONSISTENCY AUDIT — 2026-08-22</b>

کل زنجیره SECOND MIND V1 از Product Specification تا Release Governance ممیزی شد. Scope فریز‌شده حفظ شده و اصلاحات consistency شامل Decision Object Schema، Mode/Capability matrices، Evidence/Confidence/Verdict semantics، Evaluation weights، Failure-code namespace، Runtime Knowledge boundary و Release authority chain در اسناد Canonical اعمال شده است. این Audit قابلیت جدیدی به V1 اضافه نمی‌کند.
</callout>

<callout icon="🧪" color="blue_bg">
<b>AUTHORITATIVE BUILD STRATEGY — GPTs-FIRST VALIDATION</b>

از این نقطه، مسیر ساخت رسمی SECOND MIND V1 به‌صورت <b>GPTs-First → Validation → Productization</b> تعریف می‌شود. این تصمیم <b>Scope محصول را تغییر نمی‌دهد</b> و فقط Runtime و ترتیب پیاده‌سازی را مشخص می‌کند.
</callout>

# GPTs-FIRST IMPLEMENTATION STRATEGY — استراتژی ساخت مبتنی بر GPTs

## 1. Strategic Decision — تصمیم راهبردی

نسخه اول Intelligence Engine باید ابتدا به شکلی طراحی شود که بتوان آن را مستقیماً داخل یک <b>Custom GPT / GPTs Runtime</b> پیاده‌سازی و تست کرد.

هدف این مرحله ساخت محصول نهایی نیست؛ هدف ساخت یک <b>High-Fidelity Intelligence Prototype</b> است که رفتار واقعی Second Mind را قبل از سرمایه‌گذاری روی Web Application اثبات کند.

## 2. Runtime Sequence — توالی رسمی اجرا

<b>Stage A — Intelligence Specification</b>

- Decision Ontology
- Challenge Protocol
- Capability Contracts
- Mode × Capability Routing
- Evidence / Assumption Logic
- Verdict Logic
- Confidence / Uncertainty Rules
- Output Contracts
- Safety Rules
- Evaluation Rubric

<b>Stage B — GPTs Implementation Package</b>

- Master System Instructions
- Mode Instructions
- Capability Instructions
- Routing Rules
- Structured Output Contracts
- Knowledge Files / Reference Specifications where required
- Conversation Starters / Mode Entry Points
- GPTs Configuration
- Test Harness and Golden Cases

<b>Stage C — GPTs Validation</b>

GPTs باید روی تصمیم‌های واقعی و Golden Test Set آزمایش شود تا مشخص شود:

- آیا Decision Framing درست است؟
- آیا Critical Assumptions واقعاً کشف می‌شوند؟
- آیا Blind Spotها generic نیستند؟
- آیا Steelman Counterargument قوی است؟
- آیا Verdict از Analysis پشتیبانی می‌شود؟
- آیا Confidence کالیبره است؟
- آیا Next Best Action دقیق است؟
- آیا کاربر واکنش «I hadn’t thought of that» دارد؟
- آیا Repeat Challenge Behavior شکل می‌گیرد؟

<b>Stage D — Intelligence Freeze</b>

پس از Validation، رفتار Engine، Prompt Architecture، Capability Logic، Output Contracts و Evaluation Rules تثبیت و Freeze می‌شوند.

<b>Stage E — Productization</b>

فقط پس از اثبات Intelligence، همان Engine از GPTs Runtime به معماری مستقل منتقل می‌شود:

<code>Web App → Backend → Decision Challenge Engine → Model/API → Structured Output → UI</code>

## 3. Portability Requirement — الزام قابلیت انتقال

هر چیزی که در GPTs ساخته می‌شود باید از ابتدا <b>Runtime-Portable</b> باشد.

منطق محصول نباید به قابلیت اختصاصی ChatGPT گره بخورد مگر در لایه Adapter.

بنابراین باید میان این دو تفکیک حفظ شود:

- <b>SECOND MIND CORE IP:</b> Ontology, Challenge Protocol, Capability Logic, Routing, Verdict, Output Contracts, Evaluation
- <b>GPTs ADAPTER:</b> Custom GPT Instructions, Knowledge Upload Configuration, Conversation Starters, GPT-specific settings

در Productization فقط Adapter عوض می‌شود؛ Intelligence Core نباید از نو طراحی شود.

## 4. Architecture Constraint — محدودیت معماری

V1 همچنان <b>Single Model + Multi-Stage Reasoning</b> است.

GPTs نباید بهانه‌ای برای تبدیل سیستم به یک Prompt بزرگ و بدون معماری شود.

Prompt باید محصول Specification باشد؛ Specification نباید محصول Prompt باشد.

## 5. GPTs Validation Principle — اصل اعتبارسنجی

GPTs در این پروژه <b>Final Product نیست</b>؛ نقش آن:

<b>Intelligence Prototype + Behavioral Validation Environment + Prompt/Reasoning Testbed</b>

است.

هدف نهایی همچنان تبدیل Second Mind به محصول مستقل است.

## 6. Build Priority Override — تقدم اجرایی

تا قبل از تکمیل و تست GPTs، موارد زیر در اولویت توسعه نیستند:

- Production Web UI
- Authentication Infrastructure
- Payments
- Full Database Layer
- Product Analytics Infrastructure
- Deployment Architecture

مگر در حد ابزار حداقلی موردنیاز برای Validation.

## 7. Next Artifact — خروجی بعدی

خروجی بعدی پروژه باید:

<b>SECOND MIND — V1 Intelligence Engine Specification (GPTs-First, Runtime-Portable)</b>

باشد.

این سند باید مستقیماً امکان استخراج <b>SECOND MIND GPTs Build Package</b> را فراهم کند.

---

<callout icon="🔒" color="gray_bg">
<b>V1 Product Scope: FROZEN</b>

این صفحه مرجع اجرایی پروژه SECOND MIND است. Scope نسخه اول فقط با تصمیم صریح برای بازکردن Product Freeze قابل تغییر است.
</callout>

<table-of-contents />

---

# SECOND MIND — V1 PRODUCT SPECIFICATION

<p>
<b>Document ID:</b> SM-SPEC-V1-001  
</p>

<p>
<b>Version:</b> 1.0  
</p>

<p>
<b>Status:</b> V1 Implementation Specification  
</p>

<p>
<b>Source of Truth:</b> SECOND MIND — Canonical Product Definition v1.0  
</p>

<p>
<b>Product Scope:</b> FROZEN  
</p>

<p>
<b>Product:</b> SECOND MIND — ذهن دوم  
</p>

<b>System Category:</b> AI Decision Challenge System — سیستم هوشمند به‌چالش‌کشیدن تصمیم

---

# 0. DOCUMENT AUTHORITY — اعتبار سند

این سند، Canonical Product Definition را به یک <b>Implementation-Ready Product Specification — مشخصات محصول قابل‌پیاده‌سازی</b> تبدیل می‌کند.

این Specification حق ندارد:

- قابلیت جدیدی به V1 اضافه کند.
- Non-Goalهای V1 را وارد Scope کند.
- ماهیت محصول را تغییر دهد.
- Multi-Agent Architecture ایجاد کند.
- حافظه بلندمدت تصمیم ایجاد کند.
- Outcome Tracking ایجاد کند.
- Automation یا Autonomous Action ایجاد کند.
- پیچیدگی جدیدی به UX تحمیل کند.

در صورت تعارض میان این Specification و Canonical Product Definition:

<b>Canonical Product Definition مرجع بالاتر است.</b>

اصل حاکم:

> <b>Deep Intelligence. Simple Product.</b><br />هوشمندی عمیق؛ محصول ساده.

---

# 1. PRODUCT SYSTEM DEFINITION — تعریف سیستم

SECOND MIND یک سیستم پشتیبانی تصمیم مبتنی بر AI است که یک <b>Decision Object — موضوع تصمیم</b> را دریافت کرده و قبل از برخورد آن با واقعیت، آن را تحت یک فرآیند ساختاریافته Challenge قرار می‌دهد.

سیستم باید بتواند:

<b>Understand → Frame → Reframe → Decompose → Challenge → Evaluate → Synthesize → Judge → Act</b>

یعنی:

<b>درک → صورت‌بندی → بازتعریف → تجزیه → به‌چالش‌کشیدن → ارزیابی → جمع‌بندی → حکم → اقدام</b>

هدف سیستم پیش‌بینی قطعی آینده نیست.

هدف:

<b>کاهش احتمال تصمیم ضعیف از طریق آشکارکردن ضعف‌های reasoning پیش از اقدام است.</b>

---

# 2. PRODUCT BOUNDARY — مرز محصول

## 2.1 SECOND MIND IS

Second Mind:

- Decision Challenge System
- Decision Support System
- Structured Reasoning System
- Adversarial Evaluation System
- Uncertainty-Aware Judgment System

است.

## 2.2 SECOND MIND IS NOT

Second Mind:

- General Chatbot
- Search Engine
- Personal Assistant
- Autonomous Agent
- Workflow Automation System
- Professional Adviser
- Prediction Engine
- Long-Term Decision Memory System

نیست.

---

# 3. V1 SYSTEM ARCHITECTURE — معماری کلان V1

معماری منطقی:

```txt
USER
  ↓
INPUT LAYER
  ↓
INPUT NORMALIZATION
  ↓
DECISION OBJECT BUILDER
  ↓
MODE ROUTER
  ↓
DECISION FRAMING
  ↓
PROBLEM REFRAMING
  ↓
EVIDENCE / ASSUMPTION DECOMPOSITION
  ↓
CAPABILITY ROUTER
  ↓
CORE COGNITIVE ANALYSIS
  ↓
CONDITIONAL ANALYSIS
  ↓
CHALLENGE LAYER
  ↓
EVALUATION LAYER
  ↓
SYNTHESIS LAYER
  ↓
JUDGMENT LAYER
  ↓
VERDICT + CONFIDENCE
  ↓
NEXT BEST ACTION
  ↓
OUTPUT VALIDATION
  ↓
STRUCTURED RESULT
  ↓
UI
```

تمام هفت Mode روی همین Pipeline اجرا می‌شوند.

---

# 4. DECISION CHALLENGE ENGINE — موتور اصلی

Decision Challenge Engine هسته IP محصول است.

Engine از 10 ماژول منطقی تشکیل می‌شود:

## DCE-01 — Input Interpretation

تشخیص اینکه کاربر چه چیزی ارائه کرده و درباره چه چیزی تصمیم می‌گیرد.

## DCE-02 — Decision Framing

تبدیل ورودی خام به Decision Object مشخص.

## DCE-03 — Problem Reframing

بررسی اینکه آیا مسئله واقعی همان چیزی است که کاربر بیان کرده است.

## DCE-04 — Epistemic Decomposition

تفکیک:

- Fact
- Assumption
- Inference
- Unknown

## DCE-05 — Capability Routing

انتخاب تحلیل‌های لازم بر اساس Mode و Context.

## DCE-06 — Challenge Generation

تولید مهم‌ترین چالش‌ها علیه Decision Object.

## DCE-07 — Evaluation

ارزیابی قدرت شواهد، فرضیات، ریسک‌ها و استدلال‌ها.

## DCE-08 — Synthesis

ترکیب یافته‌ها و حذف موارد کم‌اهمیت یا تکراری.

## DCE-09 — Judgment

تولید Verdict، Confidence و Uncertainty.

## DCE-10 — Action Resolution

تعیین Next Best Action.

---

# 5. DECISION ONTOLOGY — هستی‌شناسی تصمیم

تمام تحلیل‌ها باید حول یک مدل استاندارد Decision Object انجام شوند.

## 5.1 Decision Object

```json
{
  "object_id": "runtime-generated",
  "decision_type": "",
  "decision_statement": "",
  "decision_question": "",
  "objective": {
    "primary": "",
    "secondary": [],
    "conflicts": []
  },
  "desired_outcome": "",
  "context": "",
  "options": {
    "user_provided": [],
    "generated": []
  },
  "constraints": [],
  "stakeholders": [],
  "time_horizon": "",
  "commitment": {},
  "claims": [],
  "evidence": [],
  "facts": [],
  "assumptions": [],
  "inferences": [],
  "unknowns": [],
  "selected_mode": "",
  "input_sources": [],
  "context_sufficiency": "",
  "ontology_warnings": []
}
```

تمام فیلدها الزاماً توسط کاربر ارائه نمی‌شوند.

سیستم باید فقط اطلاعات قابل‌استنتاج با Confidence کافی را استخراج کند و موارد دیگر را Unknown نگه دارد.

---

# 6. DECISION TYPES — انواع Decision Object

V1 باید حداقل بتواند ورودی را در این کلاس‌های مفهومی پردازش کند:

- Decision
- Idea
- Plan
- Strategy
- Claim
- Action
- Option
- Proposal
- Campaign
- Product
- Career Move
- Investment
- Message
- Document

Classification برای Routing استفاده می‌شود و نباید محدودیت مصنوعی برای کاربر ایجاد کند.

---

# 7. INPUT CONTRACT — قرارداد ورودی

## 7.1 Supported Product Inputs

V1 Product Capability:

- Text
- Image
- Screenshot
- Document
- URL
- Voice

Release sequencing می‌تواند:

<b>Text → Image/File → URL/Voice</b>

باشد.

## 7.2 Normalized Input Object

تمام ورودی‌ها قبل از Engine باید به یک Representation مشترک تبدیل شوند:

```json
{
  "raw_input_type": "",
  "extracted_content": "",
  "attachments": [],
  "user_instruction": "",
  "selected_mode": "",
  "language": "",
  "extraction_confidence": 0.0
}
```

---

# 8. INSUFFICIENT CONTEXT POLICY — سیاست اطلاعات ناکافی

Second Mind نباید برای کامل‌کردن تحلیل، اطلاعات اختراع کند.

سه وضعیت وجود دارد:

## SUFFICIENT

اطلاعات برای Challenge معنادار کافی است.

→ تحلیل اجرا شود.

## PARTIALLY SUFFICIENT

تحلیل ممکن است اما مجهولات مهم وجود دارند.

→ تحلیل انجام شود و Unknownها صریحاً نمایش داده شوند.

## CRITICALLY INSUFFICIENT

یک یا چند مجهول، اساس تحلیل را غیرقابل‌اعتماد می‌کند.

→ سؤال Clarifying محدود و Decision-Critical پرسیده شود.

اصل:

<b>Ask only when the answer could materially change the analysis.</b>

---

# 9. MODE ROUTING — انتخاب Mode

دو مسیر وجود دارد.

## Explicit Routing

کاربر یکی از Modeهای 02 تا 07 را انتخاب می‌کند.

Engine باید همان Mode را اجرا کند.

## Adaptive Routing

کاربر:

<b>CHALLENGE IT</b>

را انتخاب می‌کند.

سیستم بر اساس Decision Object قابلیت‌های مناسب را فعال می‌کند.

Challenge It نباید تمام قابلیت‌ها را کورکورانه اجرا کند.

هدف:

<b>Minimum sufficient analytical coverage.</b>

---

# 10. MODE × CAPABILITY MATRIX

Legend:

<p>
<b>P = Primary</b>  
</p>

<p>
<b>S = Supporting</b>  
</p>

<b>C = Conditional</b>

<table fit-page-width="true" header-row="true">
<tr>
<td>
Capability
</td>
<td>
Challenge
</td>
<td>
Kill Idea
</td>
<td>
Blind Spots
</td>
<td>
Pre-Mortem
</td>
<td>
Reality
</td>
<td>
Compare
</td>
<td>
Devil
</td>
</tr>
<tr>
<td>
Decision Framing
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
</tr>
<tr>
<td>
Problem Reframing
</td>
<td>
P
</td>
<td>
P
</td>
<td>
S
</td>
<td>
S
</td>
<td>
P
</td>
<td>
P
</td>
<td>
S
</td>
</tr>
<tr>
<td>
F/A/I/U Separation
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
</tr>
<tr>
<td>
Critical Assumptions
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
</tr>
<tr>
<td>
Blind Spots
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
S
</td>
<td>
P
</td>
<td>
P
</td>
<td>
S
</td>
</tr>
<tr>
<td>
Counterargument
</td>
<td>
P
</td>
<td>
P
</td>
<td>
S
</td>
<td>
S
</td>
<td>
S
</td>
<td>
S
</td>
<td>
P
</td>
</tr>
<tr>
<td>
Pre-Mortem
</td>
<td>
P
</td>
<td>
P
</td>
<td>
S
</td>
<td>
P
</td>
<td>
S
</td>
<td>
C
</td>
<td>
C
</td>
</tr>
<tr>
<td>
Critical Unknowns
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
</tr>
<tr>
<td>
Change My Mind
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
</tr>
<tr>
<td>
Verdict
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
</tr>
<tr>
<td>
Next Best Action
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
<td>
P
</td>
</tr>
</table>

Conditional capabilities توسط Routing Layer فعال می‌شوند.

---

# 11. CONDITIONAL CAPABILITY ROUTING

## Consequence Analysis

فعال شود وقتی Decision شامل Action یا Commitment است.

## Second-Order Consequences

فعال شود وقتی پیامدهای مستقیم می‌توانند رفتار سیستم، بازار، افراد یا منابع را تغییر دهند.

## Trade-off Analysis

فعال شود وقتی انتخاب موجب قربانی‌شدن یک ارزش، منبع یا هدف دیگر می‌شود.

## Opportunity Cost

فعال شود وقتی منابع محدود به یک انتخاب تخصیص داده می‌شوند.

## Reversibility

فعال شود وقتی تصمیم Commitment واقعی ایجاد می‌کند.

## Alternative Generation

فعال شود وقتی گزینه‌های کاربر احتمالاً Exhaustive نیستند.

## Evidence Sufficiency

فعال شود وقتی Verdict به Claim یا Evidence وابسته است.

## Failure Mode Analysis

فعال شود وقتی تصمیم قابلیت اجرا و شکست عملیاتی دارد.

---

# 12. CORE CAPABILITY SPECIFICATIONS

## CORE-01 — Decision Framing

### Objective

تبدیل ورودی مبهم به سؤال تصمیم مشخص.

### Required Output

- Decision
- Objective
- Desired Outcome
- Constraints
- Decision Horizon
- Missing Context

### Quality Rule

Decision Statement باید به اندازه کافی دقیق باشد که Verdict درباره آن معنا داشته باشد.

---

# 13. CORE-02 — Problem Reframing

سیستم باید بررسی کند:

> آیا کاربر در حال حل مسئله درست است؟

Reframe فقط زمانی نمایش داده شود که تفاوت معناداری با Frame اولیه وجود داشته باشد.

خروجی:

```txt
Stated Problem
Underlying Problem
Why the distinction matters
```

سیستم نباید صرفاً برای نشان‌دادن هوشمندی، مسئله را Reframe کند.

---

# 14. CORE-03 — FACT / ASSUMPTION / INFERENCE / UNKNOWN

## Fact

داده‌ای که در Context موجود، به‌عنوان واقعیت قابل اتکا ارائه یا تأیید شده است.

## Assumption

گزاره‌ای که تصمیم بر آن متکی است اما اثبات نشده.

## Inference

نتیجه‌ای که از داده‌ها استنباط شده اما مستقیماً Fact نیست.

## Unknown

اطلاعاتی که وجود ندارد یا تعیین آن ممکن نیست.

اصل حیاتی:

<b>User statements are not automatically Facts.</b>

ادعاهای کاربر می‌توانند Assumption یا Claim باشند.

---

# 15. CORE-04 — CRITICAL ASSUMPTION DETECTION

هر Assumption بر اساس دو معیار بررسی شود:

<b>Dependency × Uncertainty</b>

Critical Assumption فرضی است که:

1. Decision شدیداً به آن وابسته است.
2. صحت آن به اندازه کافی اثبات نشده است.

اولویت:

```txt
Criticality ≈ Decision Dependency × Probability of Being Wrong
```

این یک مدل مفهومی است و نباید به‌عنوان احتمال آماری جعلی نمایش داده شود.

---

# 16. CORE-05 — BLIND SPOT DETECTION

Engine باید حداقل این حوزه‌ها را در صورت ارتباط Scan کند:

- Financial
- Strategic
- Operational
- Competitive
- Human
- Behavioral
- Stakeholder
- Timing
- Dependency
- Opportunity Cost
- Regulatory

Blind Spot باید:

<b>Relevant + Material + Missing</b>

باشد.

صرفاً چیزی که کاربر ذکر نکرده Blind Spot محسوب نمی‌شود.

---

# 17. CORE-06 — STRONGEST COUNTERARGUMENT

Engine نباید Strawman تولید کند.

Counterargument باید:

- منطقی باشد.
- منصفانه باشد.
- به Decision واقعی حمله کند.
- از قوی‌ترین Evidence موجود استفاده کند.
- مهم‌ترین Assumption را هدف بگیرد.

هدف:

<b>Steelman Opposition</b>

---

# 18. CORE-07 — PRE-MORTEM

فرض عملیاتی:

> Decision اجرا شد و شکست خورد.

Engine از Outcome شکست به عقب حرکت می‌کند.

هر Failure Mode:

```json
{
  "failure": "",
  "cause": "",
  "likelihood": "",
  "impact": "",
  "detectability": "",
  "preventability": "",
  "early_warning": "",
  "mitigation": ""
}
```

Likelihood نباید بدون داده به درصد جعلی تبدیل شود.

مقادیر کیفی ترجیحی:

- Low
- Medium
- High

---

# 19. CORE-08 — DECISION-CRITICAL UNKNOWNS

هر Unknown مهم نیست.

Decision-Critical Unknown یعنی:

<b>اطلاعاتی که پاسخ آن احتمالاً می‌تواند Verdict یا Action را تغییر دهد.</b>

هر مورد باید شامل:

- Unknown
- Why It Matters
- Decision Impact
- How to Resolve

باشد.

---

# 20. CORE-09 — WHAT WOULD CHANGE MY MIND?

Engine باید Recommendation خود را Falsifiable نگه دارد.

برای هر Verdict باید بتواند بگوید:

> چه Evidence یا تغییر شرایطی باعث می‌شود این Verdict تغییر کند؟

خروجی:

```json
{
  "current_position": "",
  "change_condition": "",
  "possible_new_verdict": ""
}
```

---

# 21. CORE-10 — VERDICT ENGINE

Verdict عمومی V1:

- PROCEED
- PROCEED WITH CONDITIONS
- TEST FIRST
- RETHINK
- STOP

Mode-specific Verdictها می‌توانند طبق Canonical Definition جایگزین شوند.

Verdict نباید صرفاً Summary باشد.

باید <b>Judgment</b> باشد.

---

# 22. CORE-11 — NEXT BEST ACTION

Next Best Action باید:

- Specific
- Executable
- Decision-Relevant
- Information-Gaining
- Proportionate

باشد.

خروجی ضعیف:

> Market research بیشتری انجام بده.

خروجی صحیح:

> با 10 مشتری بالقوه‌ای که اکنون برای حل همین مشکل هزینه می‌کنند مصاحبه کن و بررسی کن آیا حداقل 3 نفر حاضرند برای Prototype مشخص پول پرداخت کنند.

هدف Action باید مشخص کند کدام Unknown یا Assumption بررسی می‌شود.

---

# 23. EVIDENCE MODEL

Evidence باید بر اساس منشأ و کیفیت تفکیک شود.

## E0 — No Evidence

صرفاً ادعا یا فرض.

## E1 — Anecdotal

تجربه فردی یا نمونه محدود.

## E2 — Indicative

داده مرتبط اما محدود.

## E3 — Strong

شواهد مستقیم و نسبتاً معتبر.

## E4 — Decision-Grade

شواهد مستقیم، مرتبط و کافی برای تصمیم موردنظر.

این Scale برای Calibration داخلی است.

نباید بدون دلیل به کاربر نمایش داده شود.

---

# 24. EVIDENCE SUFFICIENCY

اصل:

> <b>The burden of evidence should rise with the consequence of the decision.</b>

هرچه تصمیم:

- پرهزینه‌تر
- غیرقابل‌برگشت‌تر
- پرریسک‌تر
- طولانی‌مدت‌تر

باشد، Evidence Requirement بالاتر می‌رود.

---

# 25. ASSUMPTION MODEL

Assumption Object:

```json
{
  "statement": "",
  "type": "",
  "dependency": "low|medium|high",
  "uncertainty": "low|medium|high",
  "criticality": "low|medium|high",
  "evidence": [],
  "validation_method": ""
}
```

هدف مدل:

تشخیص Assumptionهایی است که باید قبل از Commitment آزمایش شوند.

---

# 26. CHALLENGE PROTOCOL

هر Analysis باید از پروتکل مشترک زیر پیروی کند:

### STEP 1 — Understand

کاربر واقعاً چه می‌خواهد؟

### STEP 2 — Frame

Decision دقیق چیست؟

### STEP 3 — Reframe

آیا مسئله واقعی متفاوت است؟

### STEP 4 — Decompose

Facts / Assumptions / Inferences / Unknowns.

### STEP 5 — Identify Dependencies

تصمیم روی چه چیزهایی ایستاده است؟

### STEP 6 — Attack

قوی‌ترین مسیرهای شکست یا مخالفت چیست؟

### STEP 7 — Expand

چه چیزی دیده نشده؟

### STEP 8 — Evaluate

Evidence و Risk چقدر قوی‌اند؟

### STEP 9 — Synthesize

کدام موارد واقعاً Material هستند؟

### STEP 10 — Judge

Verdict چیست؟

### STEP 11 — Calibrate

Confidence و Uncertainty چیست؟

### STEP 12 — Act

بهترین اقدام بعدی چیست؟

---

# 27. ADVERSARIAL FAIRNESS RULE

Second Mind باید Decision را Challenge کند، نه کاربر را.

ممنوع:

- تحقیر
- تمسخر
- مخالفت نمایشی
- بدبین‌بودن مصنوعی
- تولید ریسک صرفاً برای افزایش تعداد موارد

قاعده:

> <b>Maximum intellectual pressure. Minimum rhetorical aggression.</b>

---

# 28. PRIORITIZATION MODEL

Engine نباید تمام یافته‌ها را با وزن یکسان ارائه دهد.

هر Finding بر اساس چهار بعد مفهومی اولویت‌بندی شود:

- Materiality
- Decision Impact
- Uncertainty
- Actionability

موارد کم‌اهمیت باید حذف یا Down-rank شوند.

هدف:

<b>Signal > Volume</b>

---

# 29. CONFIDENCE MODEL

Confidence پاسخ به این سؤال است:

> Second Mind چقدر به Verdict خود بر اساس اطلاعات موجود اطمینان دارد؟

Confidence عددی:

<b>0–100</b>

اما این عدد احتمال وقوع آینده نیست.

سه مؤلفه اصلی:

## Evidence Quality

قدرت شواهد.

## Context Completeness

کامل‌بودن اطلاعات تصمیم.

## Reasoning Stability

اینکه آیا تغییر کوچک در فرضیات، Verdict را عوض می‌کند یا خیر.

Conceptual Model:

```txt
Confidence =
Evidence Quality
+ Context Completeness
+ Reasoning Stability
```

با وزن‌هایی که در Evaluation Calibration تعیین می‌شوند.

---

# 30. CONFIDENCE BANDS

## 0–39 — LOW

اطلاعات یا Evidence برای Verdict قوی کافی نیست.

## 40–69 — MODERATE

Verdict قابل استفاده است اما Unknownهای Material وجود دارند.

## 70–84 — HIGH

شواهد و Context نسبتاً قوی هستند.

## 85–100 — VERY HIGH

فقط زمانی مجاز است که Evidence بسیار قوی و Unknownهای تصمیم‌ساز محدود باشند.

سیستم نباید به‌صورت پیش‌فرض Confidence بالا تولید کند.

---

# 31. UNCERTAINTY MODEL

Confidence و Uncertainty مترادف نیستند.

Uncertainty باید منشأ داشته باشد.

انواع:

- Evidence Uncertainty
- Context Uncertainty
- Outcome Uncertainty
- Assumption Uncertainty
- External Uncertainty

خروجی باید مهم‌ترین منبع عدم‌قطعیت را مشخص کند.

---

# 32. VERDICT LOGIC

## PROCEED

زمانی که:

مزیت تصمیم قوی است و ریسک Material حل‌نشده مانع اصلی نیست.

## PROCEED WITH CONDITIONS

تصمیم قابل دفاع است اما Conditions مشخص باید رعایت شوند.

## TEST FIRST

Critical Assumption هنوز اثبات نشده و می‌توان آن را با هزینه معقول آزمایش کرد.

## RETHINK

Frame، Strategy یا Approach مشکل بنیادی دارد اما مسئله الزاماً مردود نیست.

## STOP

شواهد موجود نشان می‌دهد ادامه تصمیم در شکل فعلی توجیه کافی ندارد.

---

# 33. MODE-SPECIFIC VERDICTS

## Kill My Idea

- BUILD
- VALIDATE FIRST
- PIVOT
- KILL

## Reality Check

- DO IT
- CHANGE IT
- TEST IT
- DON'T DO IT

## Devil's Advocate

- YES
- PARTIALLY
- NO

## Compare

Recommendation باید Option مشخص یا Do Nothing / Missing Option را انتخاب کند.

---

# 34. CHALLENGE IT — OUTPUT CONTRACT

```json
{
  "decision": "",
  "objective": "",
  "assumptions": [],
  "blind_spots": [],
  "strongest_case_against": "",
  "failure_modes": [],
  "critical_unknowns": [],
  "change_conditions": [],
  "must_answer_question": "",
  "verdict": "",
  "confidence": 0,
  "uncertainty": "",
  "rationale": "",
  "next_best_action": ""
}
```

UI Mapping:

1. YOUR DECISION
2. WHAT YOU'RE ASSUMING
3. WHAT YOU'RE NOT SEEING
4. STRONGEST CASE AGAINST
5. HOW THIS COULD FAIL
6. CRITICAL UNKNOWNS
7. WHAT CHANGES THE ANSWER
8. THE QUESTION YOU MUST ANSWER
9. VERDICT + CONFIDENCE
10. NEXT BEST ACTION

---

# 35. KILL MY IDEA — OUTPUT CONTRACT

```json
{
  "idea": "",
  "problem_reality": "",
  "pain_severity": "",
  "willingness_to_pay": "",
  "alternatives": [],
  "competition": [],
  "distribution_risk": [],
  "retention_risk": [],
  "economic_risk": [],
  "defensibility": "",
  "critical_assumptions": [],
  "why_it_dies": [],
  "biggest_weakness": "",
  "what_could_save_it": [],
  "kill_test": "",
  "survival_score": 0,
  "verdict": "",
  "confidence": 0
}
```

Survival Score باید یک <b>heuristic evaluation score</b> باشد، نه احتمال آماری موفقیت.

---

# 36. FIND MY BLIND SPOTS — OUTPUT CONTRACT

```json
{
  "decision": "",
  "visible_factors": [],
  "hidden_factors": [],
  "dangerously_missing": [],
  "unknowns": [],
  "biggest_blind_spot": "",
  "decision_impact": "",
  "verdict": "",
  "confidence": 0,
  "next_best_action": ""
}
```

---

# 37. PRE-MORTEM — OUTPUT CONTRACT

```json
{
  "decision": "",
  "failure_assumption": "",
  "most_likely_failure_modes": [],
  "catastrophic_failure": "",
  "early_warning_signals": [],
  "preventable_now": [],
  "mitigation_priorities": [],
  "critical_unknowns": [],
  "verdict": "",
  "confidence": 0,
  "next_best_action": ""
}
```

---

# 38. REALITY CHECK — OUTPUT CONTRACT

```json
{
  "decision": "",
  "intended_outcome": "",
  "likely_outcome": "",
  "intent_outcome_gap": "",
  "immediate_consequences": [],
  "second_order_consequences": [],
  "best_case": "",
  "base_case": "",
  "worst_case": "",
  "reversibility": "",
  "critical_assumptions": [],
  "verdict": "",
  "confidence": 0,
  "next_best_action": ""
}
```

---

# 39. COMPARE — OUTPUT CONTRACT

```json
{
  "objective": "",
  "options": [],
  "criteria": [],
  "tradeoffs": [],
  "risks": [],
  "upside": [],
  "downside": [],
  "opportunity_cost": [],
  "reversibility": [],
  "strategic_fit": [],
  "critical_assumptions": [],
  "missing_option": "",
  "do_nothing": "",
  "recommendation": "",
  "reasoning": "",
  "confidence": 0,
  "next_best_action": ""
}
```

---

# 40. DEVIL'S ADVOCATE — OUTPUT CONTRACT

```json
{
  "position": "",
  "strongest_case_against": "",
  "opponent_evidence": [],
  "attacked_assumptions": [],
  "hard_questions": [],
  "where_opponent_is_right": [],
  "where_opponent_is_weak": [],
  "survival_test": "",
  "confidence": 0,
  "next_best_action": ""
}
```

---

# 41. PROMPT / REASONING ARCHITECTURE

V1 نباید یک Prompt عظیم و واحد باشد.

معماری Prompt منطقی:

```txt
SYSTEM IDENTITY
      ↓
GLOBAL REASONING RULES
      ↓
DECISION OBJECT
      ↓
MODE INSTRUCTION
      ↓
CAPABILITY INSTRUCTIONS
      ↓
EVIDENCE / UNCERTAINTY RULES
      ↓
VERDICT RULES
      ↓
OUTPUT SCHEMA
      ↓
OUTPUT VALIDATION
```

این معماری همچنان:

<b>Single Model + Multi-Stage Reasoning</b>

است.

هر Stage لزوماً API Call مستقل نیست.

Implementation باید Cost/Quality را Benchmark کند.

---

# 42. INTERNAL REASONING STAGES

Recommended runtime stages:

## Stage A — Parse

ساخت Decision Object.

## Stage B — Analyze

Facts / Assumptions / Unknowns / Reframe.

## Stage C — Challenge

Counterarguments / Blind Spots / Failure Modes.

## Stage D — Evaluate

Evidence / Materiality / Alternatives / Consequences.

## Stage E — Judge

Verdict / Confidence / Change Conditions.

## Stage F — Render

Output Contract.

این Stages معماری منطقی هستند و قابلیت جدید محسوب نمی‌شوند.

---

# 43. OUTPUT VALIDATION LAYER

قبل از نمایش Result، سیستم باید بررسی کند:

## Schema Validity

تمام فیلدهای Required موجودند؟

## Internal Consistency

Verdict با Analysis تناقض ندارد؟

## Evidence Integrity

Fact جعلی تولید نشده؟

## Confidence Consistency

Confidence با Evidence سازگار است؟

## Actionability

Next Best Action عملی است؟

## Duplication

یافته‌های تکراری حذف شده‌اند؟

## Prioritization

مهم‌ترین یافته‌ها برجسته شده‌اند؟

## Safety

High-Stakes Boundary رعایت شده؟

---

# 44. SAFETY BOUNDARIES

Second Mind می‌تواند تصمیم‌های:

- Medical
- Legal
- Financial

را Challenge کند.

اما نباید خود را جایگزین متخصص معرفی کند.

در High-Stakes Context:

سیستم باید:

- Unknownها را واضح‌تر کند.
- Confidence را محافظه‌کارانه‌تر Calibration کند.
- Evidence Requirement را بالا ببرد.
- مرز Decision Support را اعلام کند.
- در صورت نیاز Next Best Action را به Verification تخصصی متصل کند.

---

# 45. ANTI-HALLUCINATION RULES

سیستم نباید:

- Market Data اختراع کند.
- قانون اختراع کند.
- Research اختراع کند.
- Quote اختراع کند.
- Source اختراع کند.
- Probability جعلی ایجاد کند.
- User Claim را Fact اعلام کند.
- Unknown را با حدس پنهان کند.

اگر Evidence وجود ندارد:

<b>Unknown / Assumption</b>

ثبت شود.

---

# 46. SCENARIO ANALYSIS RULE

Best / Base / Worst Case:

پیش‌بینی قطعی نیستند.

Base Case باید:

<b>Most defensible scenario under current evidence</b>

باشد.

نه صرفاً سناریوی متوسط یا خوش‌بینانه.

---

# 47. USER EXPERIENCE SPECIFICATION

صفحه اصلی:

```txt
SECOND MIND

What are you thinking about?

[ INPUT ]

[ Attach ]

HOW SHOULD I CHALLENGE IT?

[ CHALLENGE IT ]

Kill My Idea
Find My Blind Spots
Pre-Mortem
Reality Check
Compare
Devil's Advocate
```

UI نباید Capabilityهای داخلی را به کاربر تحمیل کند.

---

# 48. RESULT EXPERIENCE

Hierarchy پیشنهادی:

```txt
DECISION
↓
KEY CHALLENGE
↓
TOP FINDINGS
↓
CRITICAL UNKNOWN
↓
VERDICT
↓
CONFIDENCE
↓
NEXT BEST ACTION
```

جزئیات تحلیلی می‌توانند Expandable باشند.

هدف:

کاربر در چند ثانیه بفهمد:

1. مشکل اصلی چیست؟
2. Second Mind چه موضعی دارد؟
3. اکنون چه کاری باید انجام دهد؟

---

# 49. MVP TECHNICAL ARCHITECTURE

```txt
Browser
   ↓
Next.js Web Application
   ↓
Application API
   ↓
Authentication / Usage Control
   ↓
Input Processing
   ↓
Decision Challenge Orchestrator
   ↓
OpenAI API
   ↓
Schema Validation
   ↓
Result Persistence
   ↓
UI Renderer
```

Supporting Infrastructure:

- Next.js
- Vercel
- OpenAI API
- Supabase
- Supabase Auth
- Stripe
- PostHog
- Sentry

این Stack جهت فنی اولیه است و تغییر Vendor بدون تغییر Product Scope مجاز است.

---

# 50. MINIMAL DATA MODEL

## User

```txt
id
email
created_at
plan
usage
```

## Challenge

```txt
id
user_id
mode
input_type
decision_object
structured_output
created_at
```

## Attachment

```txt
id
challenge_id
type
storage_reference
metadata
```

## Usage Event

```txt
id
user_id
challenge_id
event_type
timestamp
```

Basic History فقط برای مشاهده Challengeهای V1 است و نباید به Cross-Decision Intelligence تبدیل شود.

---

# 51. OBSERVABILITY REQUIREMENTS

حداقل Metrics:

- Challenges Started
- Challenges Completed
- Mode Selected
- Challenge It Usage
- Repeat Challenge Rate
- Output Failure Rate
- Schema Failure Rate
- Average Latency
- Cost per Challenge
- User Feedback
- Retry Rate

North Star:

<b>Repeat Challenge Behavior</b>

---

# 52. EVALUATION FRAMEWORK

هر خروجی باید بر اساس Rubric استاندارد ارزیابی شود.

ابعاد اصلی:

## EVAL-01 — Decision Understanding

آیا تصمیم درست فهمیده شده؟

## EVAL-02 — Relevance

آیا تحلیل مرتبط است؟

## EVAL-03 — Insight Novelty

آیا نکته‌ای Material کشف شده که بدیهی نیست؟

## EVAL-04 — Assumption Quality

آیا فرضیات حیاتی درست شناسایی شده‌اند؟

## EVAL-05 — Blind Spot Quality

آیا Blind Spot واقعی پیدا شده؟

## EVAL-06 — Counterargument Strength

آیا مخالفت Steelman است؟

## EVAL-07 — Failure Analysis

آیا Failure Modes واقع‌گرایانه‌اند؟

## EVAL-08 — Evidence Discipline

آیا Fact و Assumption جدا شده‌اند؟

## EVAL-09 — Judgment Quality

آیا Verdict از Analysis پشتیبانی می‌شود؟

## EVAL-10 — Calibration

آیا Confidence منطقی است؟

## EVAL-11 — Actionability

آیا Next Best Action واقعاً قابل اجراست؟

## EVAL-12 — Prioritization

آیا مهم‌ترین مسائل از Noise جدا شده‌اند؟

---

# 53. EVALUATION SCALE

هر Dimension:

<p>
<b>1 — Failed</b>  
</p>

<p>
<b>2 — Weak</b>  
</p>

<p>
<b>3 — Acceptable</b>  
</p>

<p>
<b>4 — Strong</b>  
</p>

<b>5 — Exceptional</b>

Quality Gate اولیه:

هیچ خروجی Production نباید در:

- Evidence Discipline
- Judgment Quality
- Decision Understanding

امتیاز زیر 3 داشته باشد.

هدف کلی قبل از Release:

<b>Average ≥ 4.0</b>

روی Golden Test Set.

---

# 54. “I HADN’T THOUGHT OF THAT” METRIC

Metric کیفی اصلی:

> آیا حداقل یک Finding مهم وجود دارد که احتمالاً reasoning اولیه کاربر را گسترش دهد؟

این معیار نباید سیستم را به Novelty مصنوعی سوق دهد.

اولویت:

<b>Useful Novelty > Surprising Novelty</b>

---

# 55. GOLDEN TEST SET

قبل از MVP باید مجموعه‌ای ثابت از Decision Cases ایجاد شود.

حداقل Domains مطابق Canonical Definition:

1. Founder
2. Product
3. Pricing
4. Career
5. Marketing
6. Investment
7. Strategy
8. Comparison
9. Communication
10. Project

برای هر Domain باید Cases با:

- Good Decision
- Bad Decision
- Ambiguous Decision
- Insufficient Evidence
- Hidden Fatal Assumption

وجود داشته باشد.

حداقل اولیه:

<b>50 Golden Cases</b>

این عدد حجم Test Set است، نه قابلیت جدید محصول.

---

# 56. REPRESENTATIVE TEST SCENARIOS

## T01 — Founder

«می‌خواهم SaaS مدیریت پروژه برای فریلنسرها بسازم.»

انتظار:

Problem Reality و Distribution Assumptions Challenge شوند.

## T02 — Pricing

«قیمت محصول را نصف کنم تا فروش بیشتر شود.»

انتظار:

Margin، positioning، elasticity و second-order effects بررسی شوند.

## T03 — Career

«شغل فعلی‌ام را ترک کنم و تمام‌وقت روی Startup کار کنم.»

انتظار:

Runway، reversibility و assumptions بررسی شوند.

## T04 — Investment

«۳۰٪ سرمایه‌ام را روی این فرصت بگذارم.»

انتظار:

Concentration، downside، evidence sufficiency و uncertainty بررسی شوند.

## T05 — Marketing

«برای رشد سریع تمام بودجه را روی influencer marketing بگذاریم.»

انتظار:

Attribution، dependency، CAC uncertainty و alternative allocation بررسی شوند.

## T06 — Compare

«گزینه A یا B؟»

انتظار:

ابتدا Objective مشخص شود.

## T07 — Devil's Advocate

کاربر یک Strategy قوی ارائه می‌دهد.

انتظار:

سیستم حتی در Strategy خوب، قوی‌ترین مخالفت منصفانه را بسازد.

## T08 — Insufficient Context

«آیا این کار را انجام بدهم؟»

انتظار:

سیستم hallucinate نکند و Context تصمیم‌ساز درخواست کند.

---

# 57. MODE-SPECIFIC QUALITY TESTS

## Kill My Idea

نباید صرفاً ایده را تخریب کند.

باید مشخص کند چه Evidence می‌تواند آن را نجات دهد.

## Blind Spots

نباید لیست عمومی Risk تولید کند.

## Pre-Mortem

Failure باید causal باشد، نه generic.

## Reality Check

Likely Outcome باید از Intended Outcome تفکیک شود.

## Compare

نباید بدون Objective گزینه برنده اعلام کند.

## Devil's Advocate

نباید Strawman ایجاد کند.

## Challenge It

باید مهم‌ترین Combination از Capabilityها را انتخاب کند.

---

# 58. FAILURE TAXONOMY — طبقه‌بندی خطاهای سیستم

## F01 — Misframing

تصمیم اشتباه فهمیده شده.

## F02 — Generic Advice

تحلیل قابل استفاده برای هر تصمیم است.

## F03 — Fake Insight

نکته ظاهراً عمیق اما بدون ارزش تصمیمی.

## F04 — Hallucinated Fact

Fact جعلی.

## F05 — False Precision

عدد یا احتمال بدون مبنا.

## F06 — Weak Opposition

Counterargument ضعیف.

## F07 — Risk Dump

لیست بلند بدون اولویت.

## F08 — Unsupported Verdict

Verdict با Analysis سازگار نیست.

## F09 — Overconfidence

Confidence بیش از Evidence.

## F10 — Non-Actionable Ending

Next Action مبهم.

## F11 — Unnecessary Clarification

سؤال‌هایی که برای تصمیم ضروری نیستند.

## F12 — Sycophancy

همسویی بدون Evidence.

---

# 59. ROUTING LOGIC — LOGICAL PSEUDOCODE

```txt
RECEIVE INPUT

NORMALIZE INPUT

BUILD DECISION OBJECT

IF context is critically insufficient:
    ASK decision-critical clarification
ELSE:
    IF explicit mode selected:
        USE selected mode
    ELSE:
        USE Challenge It routing

RUN Decision Framing

RUN Problem Reframing when material

SEPARATE
    Facts
    Assumptions
    Inferences
    Unknowns

IDENTIFY Critical Assumptions

ACTIVATE Mode Primary Capabilities

ACTIVATE relevant Conditional Capabilities

GENERATE Challenge Findings

RANK by Materiality

REMOVE low-value duplication

IDENTIFY Decision-Critical Unknowns

GENERATE Change Conditions

GENERATE Verdict

CALIBRATE Confidence

GENERATE Next Best Action

VALIDATE output

RENDER mode-specific schema
```

---

# 60. BUILD SEQUENCE

## BUILD 0 — Specification Freeze

این سند Review و Freeze شود.

Deliverable:

<b>Approved V1 Product Specification</b>

## BUILD 1 — Evaluation Harness

قبل از UI:

- Golden Cases
- Rubric
- Expected Behaviors
- Failure Taxonomy

ساخته شود.

## BUILD 2 — Decision Ontology

پیاده‌سازی:

- Input normalization
- Decision Object
- F/A/I/U model
- Assumption model

## BUILD 3 — Core Challenge Engine

پیاده‌سازی 11 Core Capability.

## BUILD 4 — Conditional Intelligence

پیاده‌سازی 8 Conditional Capability و Routing.

## BUILD 5 — Mode Layer

پیاده‌سازی:

1. Challenge It
2. Kill My Idea
3. Blind Spots
4. Pre-Mortem
5. Reality Check
6. Compare
7. Devil's Advocate

## BUILD 6 — Judgment System

- Verdict
- Confidence
- Uncertainty
- Change Conditions
- Next Best Action

## BUILD 7 — Structured Output Layer

- Schemas
- Validation
- Error handling
- Rendering contracts

## BUILD 8 — Engine Benchmark

Golden Test Set روی Engine اجرا شود.

تا زمانی که Quality Gate پاس نشده:

<b>UI expansion ممنوع است.</b>

## BUILD 9 — Web Prototype

- Input
- Attachments
- Mode Selection
- Challenge
- Result

## BUILD 10 — MVP Infrastructure

- Authentication
- Accounts
- Basic History
- Usage Limits
- Payments
- Sharing
- Analytics
- Monitoring

## BUILD 11 — Validation

اندازه‌گیری:

- Repeat Usage
- Retention
- Output Quality
- Mode Popularity
- Cost
- Feedback
- Willingness to Pay

---

# 61. ACCEPTANCE CRITERIA — PRODUCT LEVEL

SECOND MIND V1 زمانی از نظر Product Specification آماده است که:

## AC-01

هر 7 Mode قابل اجرا باشد.

## AC-02

هر 11 Core Capability پیاده‌سازی شده باشد.

## AC-03

هر 8 Conditional Capability قابل Routing باشد.

## AC-04

تمام Modeها از Shared Engine استفاده کنند.

## AC-05

Output هر Mode Schema-valid باشد.

## AC-06

Fact / Assumption / Inference / Unknown به‌صورت قابل اتکا تفکیک شوند.

## AC-07

Engine بتواند Critical Assumption را شناسایی کند.

## AC-08

Engine حداقل یک Strong Counterargument در Context مناسب تولید کند.

## AC-09

Verdict از Findings قابل دفاع باشد.

## AC-10

Confidence با Evidence و Unknowns سازگار باشد.

## AC-11

هر خروجی دارای Next Best Action مشخص باشد.

## AC-12

Generic Risk Dump در Quality Gate رد شود.

## AC-13

Hallucinated Evidence در Quality Gate Critical Failure محسوب شود.

## AC-14

سیستم در اطلاعات ناکافی Unknown ایجاد کند، نه Fact جعلی.

## AC-15

High-Stakes Boundary رعایت شود.

## AC-16

Challenge It بتواند Adaptive Capability Routing انجام دهد.

## AC-17

Compare بدون مشخص‌بودن Objective Recommendation قطعی تولید نکند.

## AC-18

Devil's Advocate از Steelman استفاده کند.

## AC-19

Pre-Mortem Failure Modes دارای causal logic باشند.

## AC-20

Reality Check میان Intended و Likely Outcome تمایز ایجاد کند.

---

# 62. ENGINE RELEASE GATE

Engine برای ورود به Web Prototype باید:

- Golden Test Set را اجرا کند.
- Average Evaluation ≥ 4.0/5 داشته باشد.
- Critical Evaluation Dimensions هیچ‌کدام زیر 3 نباشند.
- Hallucination Critical Failure نداشته باشد.
- Structured Output Reliability قابل قبول باشد.
- Verdict Consistency قابل قبول باشد.
- Next Best Action Quality قابل قبول باشد.

---

# 63. MVP SUCCESS SIGNALS

V1 با Revenue به‌تنهایی قضاوت نمی‌شود.

Primary Behavioral Signal:

# Repeat Challenge Behavior

سؤال:

> آیا کاربر برای تصمیم مهم بعدی دوباره Second Mind را باز می‌کند؟

رفتار مطلوب:

> «قبل از اینکه این کار را انجام بدهم، اول بندازمش تو Second Mind.»

Supporting Signals:

- Repeat Usage
- Challenges per Active User
- Mode Reuse
- Result Completion
- User-rated Insight
- “I hadn’t thought of that” response
- Willingness to Pay

---

# 64. IP BOUNDARY

ارزش اصلی Second Mind نباید صرفاً در Model Provider باشد.

Core IP:

## Decision Ontology

Second Mind چگونه Decision را مدل می‌کند.

## Challenge Protocol

چگونه تصمیم را تحت فشار شناختی قرار می‌دهد.

## Capability Logic

چه نوع reasoningهایی وجود دارند.

## Routing Logic

چه زمانی کدام Capability فعال می‌شود.

## Evidence & Assumption Logic

چگونه دانسته و نادانسته جدا می‌شوند.

## Judgment Logic

چگونه از Analysis به Verdict می‌رسیم.

## Output Contracts

چگونه reasoning به خروجی قابل استفاده تبدیل می‌شود.

## Evaluation Framework

چگونه کیفیت Intelligence سنجیده می‌شود.

---

# 65. V1 SCOPE PROTECTION

موارد زیر همچنان خارج از V1 هستند:

- Personal Decision Memory
- Outcome Tracking
- Decision Replay
- Personal Bias Profile
- Confidence Calibration Over Time
- Team Collaboration
- Organizational Intelligence
- Multi-Agent Architecture
- Automation
- Autonomous External Actions
- Cross-Decision Intelligence

هر Requirement جدید مرتبط با این موارد باید:

<b>V2+ BACKLOG</b>

برچسب بخورد.

---

# 66. PRODUCT FREEZE CHANGE CONTROL

اگر Requirement جدیدی پیشنهاد شد، باید چهار سؤال پاسخ داده شود:

1. آیا بدون آن V1 نمی‌تواند Promise اصلی را انجام دهد؟
2. آیا Requirement جدید واقعاً Capability جدید است یا Implementation Detail؟
3. آیا Canonical Definition آن را پشتیبانی می‌کند؟
4. آیا اضافه‌کردن آن Scope را تغییر می‌دهد؟

اگر Capability جدید باشد:

<b>Reject from V1 → V2+ Backlog</b>

مگر اینکه تصمیم رسمی برای بازکردن Product Freeze گرفته شود.

---

# 67. FINAL V1 SYSTEM DEFINITION

SECOND MIND V1 در سطح سیستم عبارت است از:

```txt
MULTIMODAL DECISION INPUT
            ↓
     DECISION OBJECT
            ↓
      SHARED ENGINE
            ↓
  11 CORE CAPABILITIES
            +
8 CONDITIONAL CAPABILITIES
            ↓
      7 USER MODES
            ↓
  STRUCTURED CHALLENGE
            ↓
     MATERIAL FINDINGS
            ↓
   VERDICT + CONFIDENCE
            ↓
    NEXT BEST ACTION
```

---

# 68. FINAL PRODUCT CONTRACT

SECOND MIND باید بتواند برای یک Decision Object معتبر پاسخ دهد:

### 1. What exactly are you deciding?

دقیقاً چه تصمیمی می‌گیری؟

### 2. What are you assuming?

چه چیزهایی را فرض گرفته‌ای؟

### 3. What might you be missing?

چه چیزی را ممکن است نبینی؟

### 4. What is the strongest case against this?

قوی‌ترین استدلال علیه آن چیست؟

### 5. How could this fail?

چگونه می‌تواند شکست بخورد؟

### 6. What don't we know that matters?

چه چیزی را نمی‌دانیم که برای تصمیم مهم است؟

### 7. What evidence would change the conclusion?

چه چیزی می‌تواند نتیجه را تغییر دهد؟

### 8. What is the judgment?

حکم چیست؟

### 9. How confident should we be?

چقدر باید به این حکم اطمینان داشت؟

### 10. What should happen next?

اقدام بعدی دقیقاً چیست؟

---

# 69. DEFINITION OF DONE — تعریف نهایی تکمیل V1

Second Mind V1 زمانی Product-Complete محسوب می‌شود که یک Professional بتواند بدون Prompt Engineering:

1. Decision، Idea، Plan، Strategy، Claim، Action، Option یا Artifact خود را وارد کند.
2. Mode مناسب را انتخاب کند یا Challenge It را بزند.
3. سیستم Decision را به‌درستی Frame کند.
4. Facts، Assumptions، Inferences و Unknowns را از هم جدا کند.
5. Critical Assumptions را کشف کند.
6. Blind Spots واقعی را پیدا کند.
7. Strong Counterargument بسازد.
8. Failure Paths را تحلیل کند.
9. Decision-Critical Unknowns را مشخص کند.
10. Evidence Sufficiency را در صورت ارتباط بررسی کند.
11. پیامدها، Trade-offs، Opportunity Costs، Reversibility و Alternatives را در صورت نیاز تحلیل کند.
12. Findings را بر اساس Materiality اولویت‌بندی کند.
13. Verdict مشخص ارائه کند.
14. Confidence و Uncertainty را بدون False Precision بیان کند.
15. مشخص کند چه Evidenceای می‌تواند Verdict را تغییر دهد.
16. یک Next Best Action دقیق ارائه کند.

و کیفیت خروجی به حدی باشد که به‌صورت تکرارشونده واکنش زیر را ایجاد کند:

> <b>“I hadn’t thought of that.”</b><br />«اصلاً به این فکر نکرده بودم.»

بدون اینکه سیستم برای ایجاد این واکنش به:

- Novelty مصنوعی
- Hallucination
- Contrarianism نمایشی
- Risk Dump
- False Precision

متوسل شود.

---

# 70. IMPLEMENTATION DIRECTIVE — دستور اجرایی

با تصویب و Freeze این Specification، مرحله Product Definition و Product Specification خاتمه می‌یابد.

مرحله بعد نباید:

- UI Design
- Branding
- Landing Page
- Payment Integration
- Database Expansion

باشد.

مرحله بعد باید:

# SECOND MIND — INTELLIGENCE ENGINE SPECIFICATION

باشد.

تمرکز آن باید روی تبدیل اجزای تعریف‌شده در این سند به قراردادهای دقیق Engine باشد، شامل:

<b>Decision Ontology → Capability Contracts → Routing Rules → Challenge Protocol → Evidence/Assumption Logic → Judgment Logic → Output Schema Definitions → Evaluation Harness</b>

تا بتوان قبل از ساخت Web Application، Intelligence اصلی Second Mind را به‌صورت مستقل اجرا، Benchmark و اصلاح کرد.

---

# END OF SPECIFICATION

<p>
<b>SECOND MIND — V1 Product Specification</b>  
</p>

<p>
<b>Version:</b> 1.0  
</p>

<p>
<b>Scope:</b> FROZEN  
</p>

<p>
<b>Architecture:</b> One Shared Decision Challenge Engine  
</p>

<p>
<b>Runtime Principle:</b> Single Model + Multi-Stage Reasoning  
</p>

<p>
<b>Product Principle:</b> Deep Intelligence. Simple Product.  
</p>

<p>
<b>Primary Validation Signal:</b> Repeat Challenge Behavior  
</p>

<p>
<b>Primary Quality Signal:</b> “I hadn’t thought of that.”  
</p>

<b>Next Artifact:</b> SECOND MIND — Intelligence Engine Specification

<page url="https://app.notion.com/p/3c46f520f93e81dba5dcc093b3a06184">
SM-INT-V1-001 — SECOND MIND V1 Intelligence Engine Specification v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e81c9a7ddc10f0c1aceb4">
SM-GPT-V1-001 — SECOND MIND GPTs Build Specification v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e81bb9b49e7112bb8738e">
SM-KNOW-01 — SECOND MIND Decision Ontology v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e815a8b71f1da56b05bd9">
SM-KNOW-02 — SECOND MIND Core Capability Contracts v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e81daabf7d038b7d988ee">
SM-KNOW-03 — SECOND MIND Conditional Capability Contracts v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e81a6982dd59408ed60ff">
SM-KNOW-04 — SECOND MIND Mode Specifications v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e819ea3a3cd2baff4a973">
SM-KNOW-05 — SECOND MIND Evidence, Confidence & Verdict Model v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e81c090edd61fbad2b227">
SM-KNOW-07 — SECOND MIND Routing & Reasoning Protocol v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e81f4a82dfa4da7ca4b28">
SM-KNOW-08 — SECOND MIND Evaluation, Failure Taxonomy & Golden Test Cases v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e8185819ee113dcbb9d59">
SM-EVAL-002 — SECOND MIND Execution-Aligned Evaluation & Release Control v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e812bbc1ccbee44cdd8e3">
SM-LOG-001 — SECOND MIND Failure & Regression Log v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e81578fdee8e233e54513">
SM-REL-001 — SECOND MIND GPTs Release Checklist v1.0
</page>

<page url="https://app.notion.com/p/3c46f520f93e812f8277f8d82c8a2c55">
SM-EVAL-001 — SECOND MIND Evaluation Scorecard v1.0
</page>
</content>
</page>
