Here is the result of "fetch" for the Page with URL https://app.notion.com/p/3c46f520f93e812bbc1ccbee44cdd8e3 as of 2026-08-22T09:23:25.620Z:
<page url="https://app.notion.com/p/3c46f520f93e812bbc1ccbee44cdd8e3">
<ancestor-path>
<parent-page url="https://app.notion.com/p/3c36f520f93e81459ac0fc036497750d" title="SECOND MIND — V1 Build Project"/>
<ancestor-2-page url="https://app.notion.com/p/84e6f520f93e82fb95d6812289f80363" title="Teamspace Home"/>
</ancestor-path>
<properties>
{"title":"SM-LOG-001 — SECOND MIND Failure & Regression Log v1.0"}
</properties>
<iconMetadata>null</iconMetadata>
<content>
<callout icon="✅" color="green_bg">
<b>CANONICAL AUDIT AMENDMENT — 2026-08-22</b>

Failure Codes از namespace <code>FAIL-*</code> مطابق SM-KNOW-08 استفاده می‌کنند. این namespace برای جلوگیری از collision با Evaluation Dimension IDs و Capability IDs الزامی است. هر bare failure code قدیمی در مثال‌های پایین‌تر deprecated محسوب می‌شود.
</callout>

<b>Document ID:</b> SM-LOG-001

<b>Version:</b> 1.0

<b>Status:</b> Operational Engineering Artifact — Candidate for Freeze

<b>Artifact Class:</b> Failure Memory / Root-Cause Control / Regression Governance

<b>V1 Scope:</b> FROZEN

<callout icon="🧩">
<b>Canonical purpose:</b> Every observed failure must become traceable engineering evidence: Observe → Reproduce → Root Cause → Fix → Regression → Verification → Closure → Recurrence Control.
</callout>

## 0. DOCUMENT PURPOSE — هدف سند

این سند سیستم رسمی ثبت، تشخیص، اصلاح و جلوگیری از بازگشت Failureهای SECOND MIND V1 را تعریف می‌کند. هیچ Failure نباید بعد از کشف بدون Root Cause، بدون Regression و بدون Verification بسته شود.

> <b>A failure is not fixed when the answer looks better. It is fixed when the root cause is removed and regression proves the system remains intact.</b>

## 1. FAILURE MANAGEMENT LOOP

```txt
OBSERVE
→ CAPTURE
→ REPRODUCE
→ CLASSIFY
→ ASSESS SEVERITY
→ TRACE ROOT CAUSE
→ DEFINE FIX HYPOTHESIS
→ ASSESS BLAST RADIUS
→ IMPLEMENT FIX
→ RUN TARGETED REGRESSION
→ RUN REQUIRED WIDER REGRESSION
→ VERIFY
→ CLOSE
→ MONITOR RECURRENCE
```

## 2. GOVERNANCE PRINCIPLES

- Root Cause Before Prompt Tweak
- Reproduction Before Repair
- Fix the Nearest Root Cause
- Upstream Failures Have Wider Blast Radius
- No Closure Without Evidence
- Regression Is Mandatory
- Separate Root Cause from downstream symptoms
- New Capability Is Not a Fix
- Never weaken a Golden Test to pass
- Recurrence signals systemic weakness

## 3. FAILURE LIFECYCLE

Statuses:

NEW → TRIAGED → REPRODUCING → CONFIRMED → ROOT\_CAUSE\_IDENTIFIED → FIX\_PLANNED → FIX\_IN\_PROGRESS → READY\_FOR\_REGRESSION → VERIFIED → CLOSED.

Branches: REGRESSION\_FAILED، REOPENED، DEFERRED، NOT\_A\_BUG، DUPLICATE.

## 4. SEVERITY & PRIORITY

Severity: S0 Observation، S1 Minor، S2 Moderate، S3 Major، S4 Critical.

Priority: P0 immediate، P1 urgent، P2 planned، P3 backlog.

S4 examples: fabricated material evidence، prompt injection override، wrong primary decision leading to consequential Verdict، unsafe high-stakes certainty، systematic epistemic laundering.

## 5. RELEASE IMPACT

NONE، LOCAL\_FIX\_REQUIRED، MODE\_BLOCKER، CROSS\_MODE\_BLOCKER، CONTROLLED\_TEST\_BLOCKER، RELEASE\_BLOCKER.

## 6. FAILURE FAMILIES

Use canonical families from SM-KNOW-08: A Ontology/Framing، B Epistemic، C Capability، D Routing، E Evidence، F Verdict/Confidence، G Action، H Output، I Safety/Security.

## 7. REPRODUCTION CONTRACT

Store exact input، exact output، system version، prompt/Knowledge versions، runtime/model، tools، attempts، successful reproductions and stability class. Non-deterministic issues should normally be attempted 2–3 times. Critical original evidence must not be dismissed because one rerun passes.

## 8. ROOT-CAUSE MODEL

Find the earliest layer explaining the defect: Product Contract، Master Instructions، Ontology، Mode Routing، Framing، Epistemic، Core Capability، Conditional Routing، Evidence Gate، Synthesis، Judgment، Confidence، NBA، Renderer، Tool Integration، Safety، or Evaluation/Test Defect.

Root-cause question:

> What upstream behavior, if corrected, would prevent this failure and its downstream symptoms?

## 9. ROOT CAUSE ≠ SYMPTOM

Example: PROCEED on weak evidence may look like overconfidence, while the actual root cause is a skipped Evidence Sufficiency gate. Fix the earliest causal layer.

## 10. BLAST RADIUS

Levels: B0 Local، B1 Capability، B2 Mode، B3 Cross-Mode، B4 Systemic. Assess Modes، Capabilities، Decision Types، Input Types، High-Stakes، Evidence، Verdict، Output and GPT context impact.

## 11. SCOPE IMPACT

Every fix declares NO\_SCOPE\_CHANGE، POSSIBLE\_SCOPE\_CHANGE، or REQUIRES\_PRODUCT\_FREEZE\_REVIEW. A new user-facing capability cannot be introduced as a bug fix without governance review.

## 12. FIX HYPOTHESIS

A fix must specify root cause، proposed minimal change، expected behavior change، target files/sections، risk، Scope impact and success criteria. Avoid case-specific prompt patches and Golden-case overfitting.

## 13. REGRESSION TIERS

R1 Local، R2 Mode، R3 Cross-Mode، R4 Full.

Ontology، Master Instructions، Judgment/Confidence or Runtime Protocol major changes require broad/full regression. The original failing case passing is necessary but never sufficient.

## 14. VERIFICATION

Implementation ≠ Verification. A fix is Verified only after original case + required adjacent/wider regression pass with no new critical regression. Independent verification is recommended for S4/systemic S3 and required for S4 Closure.

## 15. CLOSURE

Close only when Root Cause identified، Fix implemented، original case passes، required regression passes، no new material regression exists، verification evidence is stored، Release Impact is cleared/accepted، Golden Test promotion is assessed and recurrence monitoring is recorded.

## 16. REOPEN & RECURRENCE

Reopen when the same root-cause behavior returns. Track recurrence count، versions، variants and systemic pattern. Repeated S2/S3 with the same root cause becomes Systemic even if individual cases appear isolated.

## 17. FAILURE CLUSTERING

Cluster by root cause، stage، family، Mode، capability، trigger and domain. Example: repeated clarification failures across Compare/Reality Check/Challenge It may indicate a shared Clarification Threshold defect in SM-KNOW-07.

## 18. FAILURE → GOLDEN TEST

Meaningful real-world failure should be considered for promotion when S3/S4، recurring، novel mechanism، hallucination، prompt injection، confidence defect or Mode-boundary issue. Generalize mechanism rather than memorizing exact wording.

## 19. KNOWN DEFECTS & RELEASE BLOCKING

Open known defects must record Severity، user impact، workaround، target version and Release Impact. No release with undisclosed S3/S4 defects affecting core decision quality. Any open S4 or unresolved systemic critical-path S3 blocks relevant release.

## 20. REGRESSION RECORD

Each regression stores trigger Failure، baseline/candidate versions، Tier، cases، pass/fail، new/resolved failures، dimension deltas، Verdict/Confidence drift and final result: PASS / PASS\_WITH\_OBSERVATIONS / FAIL / CRITICAL\_REGRESSION.

## 21. FIX GOVERNANCE

No prompt or Knowledge change without a Failure/Test reference، expected improvement and Regression Tier. Master Instructions and shared Judgment changes receive the broadest regression.

## 22. FAILURE HISTORY

Never delete resolved Failure records. Preserve original observation، failed fix attempts، root-cause revisions، prior severity and regression outcomes. Engineering memory is cumulative.

## 23. FAILURE PATTERN METRICS

Track Open S4، Open S3، Recurrence Rate، Mean Fix Attempts، Reopen Rate، Failures by Stage/Mode/Family، Regression Failure Rate and Golden Promotions.

## 24. OUTPUT-ORIENTED PRIORITY

Prioritize defects that impair Decision correctness، insight quality، Verdict defensibility، Confidence، NBA and user comprehension—in that order.

## 25. ROOT-CAUSE DECISION TREE

```txt
Wrong thing analyzed? → Ontology/Framing
Right thing, wrong truth state? → Epistemic
Right facts, weak insight? → Core/Conditional
Right analysis, wrong judgment? → Synthesis/Verdict
Right judgment, wrong certainty? → Confidence
Right judgment, weak action? → NBA
Right intelligence, poor presentation? → Output
```

## 26. ANTI-PATTERNS

Do not patch output when reasoning is broken. Do not simply add more instructions. Do not overfit Golden wording. Do not lower confidence everywhere to fix one overconfidence case. Do not force all capabilities because one trigger was missed. Do not retest only the failing case. Do not cherry-pick runs. Do not close because it “seems fixed.”

## 27. FULL FAILURE RECORD — REQUIRED FIELDS

Identity، Source، Observation، Reproduction، Root Cause، Blast Radius، Fix، Regression، Verification، Closure، Golden Promotion and Recurrence must be recorded for material failures.

## 28. AUDITABILITY

Every closed S3/S4 must be reconstructable:

```txt
Original Output
→ Scorecard
→ Failure Record
→ Root Cause
→ Fix
→ Regression
→ Verification
→ Closure
```

## 29. GPTs → PRODUCT PORTABILITY

Future backend can store Failure records relationally against Cases، Runs، Versions، Components and Releases. Semantics stay unchanged.

## 30. DOCUMENT ACCEPTANCE

This artifact is valid when Failure lifecycle، reproduction، root cause، blast radius، fix governance، regression، verification، closure، recurrence، clustering، Golden promotion and release blocking are all operational without redefining upstream contracts.

## 31. FREEZE CRITERIA

Freeze after first 10 real failures، at least 3 complete Fix/Regression cycles، one S3+ closure، one recurrence/reopen validation, and successful integration with Release Checklist.

## 32. NEXT ARTIFACT

<b>SM-REL-001 — SECOND MIND GPTs Release Checklist v1.0</b> will consolidate all canonical and operational artifacts into the final Go / Conditional Go / No-Go gate for the GPTs build.

## FINAL CANONICAL CONTRACT

```txt
FAILURE OBSERVED
→ CAPTURE EXACTLY
→ REPRODUCE
→ CLASSIFY
→ FIND EARLIEST ROOT CAUSE
→ DESIGN MINIMAL FIX
→ ASSESS BLAST RADIUS
→ IMPLEMENT
→ REGRESS
→ VERIFY
→ PROMOTE TO TEST IF NEEDED
→ CLOSE
→ WATCH FOR RECURRENCE
```

> <b>The underlying failure mechanism must be removed, the fix verified, adjacent behavior preserved, and a regression test left behind so the defect cannot silently return.</b>
</content>
</page>
