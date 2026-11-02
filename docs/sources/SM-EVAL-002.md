Here is the result of "fetch" for the Page with URL https://app.notion.com/p/3c46f520f93e8185819ee113dcbb9d59 as of 2026-08-22T09:22:23.663Z:
<page url="https://app.notion.com/p/3c46f520f93e8185819ee113dcbb9d59">
<ancestor-path>
<parent-page url="https://app.notion.com/p/3c36f520f93e81459ac0fc036497750d" title="SECOND MIND — V1 Build Project"/>
<ancestor-2-page url="https://app.notion.com/p/84e6f520f93e82fb95d6812289f80363" title="Teamspace Home"/>
</ancestor-path>
<properties>
{"title":"SM-EVAL-002 — SECOND MIND Execution-Aligned Evaluation & Release Control v1.0"}
</properties>
<iconMetadata>null</iconMetadata>
<content>
<p>
<b>Document ID:</b> SM-EVAL-002  
</p>

<p>
<b>Version:</b> 1.0  
</p>

<p>
<b>Status:</b> Canonical Evaluation Control Artifact — Candidate for Freeze  
</p>

<p>
<b>Audit Status:</b> Canonical Consistency Pass 1 — 2026-08-22  
</p>

<p>
<b>Artifact Class:</b> Execution-to-Evaluation Traceability / Non-Compensatory Quality Control / Release Evidence Governance  
</p>

<p>
<b>Evaluation Authority:</b> SM-KNOW-08  
</p>

<p>
<b>Scorecard Authority:</b> SM-EVAL-001  
</p>

<p>
<b>Runtime Authority:</b> SM-KNOW-07  
</p>

<p>
<b>Final Release Authority:</b> SM-REL-001  
</p>

<b>V1 Scope:</b> FROZEN

---

# 0. PURPOSE

SM-EVAL-002 یک Scorecard دوم نیست. وظیفه آن اتصال <b>Runtime Execution → Evaluation Evidence → Failure Classification → Regression → Release Evidence</b> است. این سند تضمین می‌کند نمره خوب نتواند یک defect بنیادی را پنهان کند.

# 1. NON-COMPENSATORY QUALITY RULE

هیچ میانگین یا Dimension قوی نمی‌تواند این موارد را جبران کند: Material hallucination، Wrong Primary Decision، Epistemic Laundering، Prompt Injection Override، Unsafe High-Stakes Certainty، Unsupported Consequential Verdict.

# 2. EXECUTION-TO-EVALUATION TRACEABILITY

<table fit-page-width="true" header-row="true">
<tr>
<td>
Runtime Stage
</td>
<td>
Evaluation Evidence
</td>
<td>
Primary Dimension
</td>
<td>
Authority
</td>
</tr>
<tr>
<td>
Decision Object
</td>
<td>
Correct primary decision
</td>
<td>
D01
</td>
<td>
SM-KNOW-01/07
</td>
</tr>
<tr>
<td>
Framing/Reframing
</td>
<td>
Neutral judgment-ready frame
</td>
<td>
D02
</td>
<td>
SM-KNOW-02/07
</td>
</tr>
<tr>
<td>
Epistemic Structuring
</td>
<td>
F/A/I/U + provenance
</td>
<td>
D03
</td>
<td>
SM-KNOW-01/05
</td>
</tr>
<tr>
<td>
Core Intelligence
</td>
<td>
Assumptions/blind spots/counterargument/failure
</td>
<td>
D04–D07/D10
</td>
<td>
SM-KNOW-02
</td>
</tr>
<tr>
<td>
Conditional Routing
</td>
<td>
Trigger/suppression correctness
</td>
<td>
D08
</td>
<td>
SM-KNOW-03/07
</td>
</tr>
<tr>
<td>
Evidence Gate
</td>
<td>
Claim-evidence fit/sufficiency
</td>
<td>
D09
</td>
<td>
SM-KNOW-05
</td>
</tr>
<tr>
<td>
Judgment
</td>
<td>
Traceable mode-valid Verdict
</td>
<td>
D11
</td>
<td>
SM-KNOW-05/07
</td>
</tr>
<tr>
<td>
Confidence
</td>
<td>
Uncertainty-sensitive calibration
</td>
<td>
D12
</td>
<td>
SM-KNOW-05
</td>
</tr>
<tr>
<td>
NBA
</td>
<td>
Executable decision-closing action
</td>
<td>
D13
</td>
<td>
SM-KNOW-02/07
</td>
</tr>
<tr>
<td>
Mode
</td>
<td>
Distinct analytical job
</td>
<td>
D14
</td>
<td>
SM-KNOW-04
</td>
</tr>
<tr>
<td>
Rendering
</td>
<td>
Prioritized user output
</td>
<td>
D15
</td>
<td>
SM-KNOW-06
</td>
</tr>
</table>

# 3. EVALUATION EVIDENCE REQUIREMENT

هر score در Release Suite باید به رفتار مشاهده‌شده، missing required behavior یا raw output reference متصل باشد. نمره بدون Evidence، Release Evidence معتبر نیست.

# 4. GATE PRECEDENCE

1. Safety / Prompt Injection
2. Epistemic Integrity
3. Decision & Framing
4. Evidence
5. Judgment / Confidence
6. Routing / Mode
7. NBA / Actionability
8. Output UX
9. Aggregate Score

# 5. VERDICT TRACEABILITY CONTROL

Required chain:

```txt
Evidence / Context
→ Critical Assumptions / Unknowns
→ Material Findings
→ Judgment Drivers
→ Verdict
```

اگر این chain قابل بازسازی نباشد D11 نمی‌تواند Strong تلقی شود.

# 6. CONFIDENCE TRACEABILITY CONTROL

Confidence باید Evidence Sufficiency، Critical Unknowns، conflict، external volatility و extraction uncertainty را منعکس کند. Fluency یا user certainty هیچ اثر مثبت مستقیمی ندارد.

# 7. NBA CLOSURE CONTROL

Strong NBA:

```txt
Exact Action
→ Target Assumption / Unknown
→ Evidence or Result
→ Decision Effect
```

# 8. VERSION INTEGRITY

هر benchmark باید Exact System Version، Master Instructions Version و Knowledge Manifest ثابت داشته باشد. تغییر وسط Run، benchmark را invalid می‌کند.

# 9. REGRESSION VALIDITY

Fix فقط زمانی Improvement است که target behavior بهتر شود و هیچ S4، Critical-Dimension regression، Cross-Mode truth instability یا material user-utility loss جدید ایجاد نشود.

# 10. RELEASE EVIDENCE PACKAGE

Release Candidate باید raw outputs، completed SM-EVAL-001 scorecards، failures، regression evidence، version manifest و release decision rationale را حفظ کند. <b>No package → no valid release approval.</b>

# 11. EVALUATION CONTROL FAILURE CODES

- ECTRL-01 UNTRACEABLE\_SCORE
- ECTRL-02 VERSION\_MIXED\_RUN
- ECTRL-03 CRITICAL\_FAILURE\_COMPENSATED\_BY\_AVERAGE
- ECTRL-04 RAW\_OUTPUT\_MISSING
- ECTRL-05 REGRESSION\_SCOPE\_INSUFFICIENT
- ECTRL-06 UNVERIFIED\_FIX
- ECTRL-07 RELEASE\_EVIDENCE\_INCOMPLETE
- ECTRL-08 EVALUATOR\_CONFIDENCE\_UNRESOLVED
- ECTRL-09 CROSS\_MODE\_TRUTH\_DRIFT
- ECTRL-10 BASELINE\_INVALID
- ECTRL-11 GOLDEN\_EXPECTATION\_LAUNDERING
- ECTRL-12 USER\_UTILITY\_DISCONNECTED

# 12. USER-OUTCOME BRIDGE

Benchmark quality باید با Repeat Challenge Behavior، Decision-Impact Insight Rate، False-Insight Rate، Verdict Usefulness و NBA Executability مقایسه شود. Benchmark بالا با utility پایین یک Evaluation Framework review را Trigger می‌کند.

# 13. AUTHORITY BOUNDARY

SM-EVAL-001 نمره می‌دهد. SM-EVAL-002 اعتبار فرآیند ارزیابی و release evidence را کنترل می‌کند. SM-LOG-001 Failure lifecycle را مدیریت می‌کند. SM-REL-001 تصمیم نهایی GO / CONDITIONAL GO / NO-GO را می‌گیرد.

# FINAL CONTRACT

> <b>The unit of quality is not reasoning volume or an average score. It is decision improvement produced by a traceable, evidence-disciplined, regression-stable reasoning process.</b>
</content>
</page>
