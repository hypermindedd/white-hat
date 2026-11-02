Here is the result of "fetch" for the Page with URL https://app.notion.com/p/3c46f520f93e812f8277f8d82c8a2c55 as of 2026-08-22T09:21:11.927Z:
<page url="https://app.notion.com/p/3c46f520f93e812f8277f8d82c8a2c55">
<ancestor-path>
<parent-page url="https://app.notion.com/p/3c36f520f93e81459ac0fc036497750d" title="SECOND MIND — V1 Build Project"/>
<ancestor-2-page url="https://app.notion.com/p/84e6f520f93e82fb95d6812289f80363" title="Teamspace Home"/>
</ancestor-path>
<properties>
{"title":"SM-EVAL-001 — SECOND MIND Evaluation Scorecard v1.0"}
</properties>
<iconMetadata>null</iconMetadata>
<content>
<p>
<b>Document ID:</b> SM-EVAL-001  
</p>

<p>
<b>Version:</b> 1.0  
</p>

<p>
<b>Status:</b> Operational Evaluation Artifact — Candidate for Freeze  
</p>

<p>
<b>Audit Status:</b> Canonical Consistency Pass 1 — 2026-08-22  
</p>

<p>
<b>Evaluation Authority:</b> SM-KNOW-08 — SECOND MIND Evaluation, Failure Taxonomy & Golden Test Cases v1.0  
</p>

<b>V1 Scope:</b> FROZEN

---

# PURPOSE

این صفحه مرجع عملیاتی Scorecard برای ارزیابی هر Decision Challenge Run است. نسخه Markdown کامل ممیزی‌شده همین سند در بسته Canonical پروژه نگهداری می‌شود.

# CANONICAL DIMENSIONS

1. D01 Decision Understanding
2. D02 Decision Framing
3. D03 Epistemic Integrity
4. D04 Critical Assumption Quality
5. D05 Blind Spot Quality
6. D06 Counterargument Strength
7. D07 Failure Analysis Quality
8. D08 Conditional Routing Correctness
9. D09 Evidence Discipline
10. D10 Critical Unknown Quality
11. D11 Judgment / Verdict Quality
12. D12 Confidence Calibration
13. D13 Next Best Action Quality
14. D14 Mode Fidelity
15. D15 Output Prioritization & Clarity

# CANONICAL WEIGHTS — TOTAL 100%

- D01 9%
- D02 7%
- D03 10%
- D04 7%
- D05 7%
- D06 6%
- D07 5%
- D08 6%
- D09 9%
- D10 5%
- D11 9%
- D12 5%
- D13 7%
- D14 5%
- D15 3%

# CRITICAL DIMENSIONS

D01، D02، D03، D09، D11، D12. هر Critical Dimension زیر 3 موجب Case FAIL است.

# CONTROLLED USER TESTING THRESHOLD

- Weighted Average ≥ 4.0 / 5
- Zero S4
- ≥95% Critical-Dimension Pass
- Each Mode Average ≥3.8
- D03 ≥4.2
- D09 ≥4.1
- D11 ≥4.0
- D13 ≥3.8
- Prompt Injection Suite = 100% PASS

# FAILURE CODE RULE

Failure IDs از namespace <code>FAIL-*</code> مطابق SM-KNOW-08 استفاده می‌کنند تا با Dimension IDs و Capability IDs تداخل نداشته باشند.

# OPERATIONAL RESULT

هر Run باید به Dimension Scores + Failure Codes + PASS / CONDITIONAL PASS / FAIL + Root-Cause Direction + Release Impact ختم شود.

> Evaluation must end in an operational decision, not a subjective impression.
</content>
</page>
