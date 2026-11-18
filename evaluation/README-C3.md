# White Hat C3 Golden evaluation

Reference: PR #2 handoff, section C.3, plus SM-KNOW-08 and SM-EVAL-001.
The candidate for evaluating product behavior is `d76d53bc4fb0f8f035b262a47fd33d6e5d1b5baf`.
This evaluation tooling is a follow-up on top of that product code.
Pin the actual baseline SHA and model and the candidate SHA and model before running.

## Prepare

Use Python 3.10+ and jsonschema 4.23.0.

```sh
python3 evaluation/prepare_s3.py /tmp/white-hat-golden
python3 evaluation/run_s3.py /tmp/white-hat-golden/execution.json --plan
```

The generated manifest has 60 IDs across seven modes. The JSON payloads for the current
API are validated against OpenAPI. Use the plan output to confirm scope and
submission count. The current plan is 59 cases and 63 submissions per model, including
four two-revision cases. The second revision carries the earlier user statement as
context. This exercises revision behavior. Native conversational memory requires a
separate run. G026/G057 pass document text as source notes. G032/G033 pass retrieval
fixtures as source notes. G056 requires image transport and remains pending on this API.
Use the original assets and exact hashes for native runs. Criteria are separate from
the model payloads.

## Execute in an isolated evaluation workspace

Use synthetic inputs, a dedicated test account and a target with the intended pinned
code and provider/model configuration. Set `WH_S3_ACCESS_TOKEN` securely in the shell
environment. Configure the service-side model and budget before starting. The normal
limit may be below the 63 submissions needed for one arm. The CLI submission limit
bounds calls to enqueue and is independent of the provider spending cap.

```sh
python3 evaluation/run_s3.py /tmp/white-hat-golden/execution.json \
  --origin "$EVAL_ORIGIN" --workspace "$EVAL_WORKSPACE" \
  --model "$EVAL_MODEL" --execution-sha "$EVAL_SHA" \
  --out /tmp/white-hat-run-candidate --max-submissions 1 --case G001
```

Run the same single-case smoke on the approved baseline. Use new output directories
for each attempt and run the full web selection only after the smoke succeeds.
Record the workspace and run IDs for any interruption before considering a new
submission. The runner deliberately stops at the first terminal error or ambiguous
outcome. The operator can query the saved run ID and inspect reconciliation.
No automatic retry is added. Each saved receipt contains the API result, usage
fields if available and elapsed submit-to-terminal latency. This API route does
not expose raw provider envelopes or confirmed cost. Collect both model arms’
provider-side evidence using an approved private receipt export, reconcile against
run IDs and record unknowns as null until verified. Code SHA and model arguments
are declarations to cross-check against deployed and provider evidence.

## Blind review and acceptance

```sh
python3 evaluation/blind_s3.py /tmp/white-hat-golden/execution.json \
  /tmp/white-hat-run-candidate /tmp/white-hat-run-baseline \
  /tmp/white-hat-reviewer-packet.json /tmp/white-hat-private-key.json \
  --provider-evidence /tmp/private-c3-receipts.json
```

For a fixture rehearsal, substitute `--fixture` for `--provider-evidence`. The private key
labels fixture output accordingly. For provider runs, the private export must contain
`budget_usd_micros`, `engine_policy_sha256`, and a `receipts` array with one row per
selected run across both arms. Each row contains `run_id`, `workspace_id`, `model`,
`execution_sha`, `engine_policy_sha256`, `provider_request_id`, `input_tokens`,
`output_tokens`, and `confirmed_cost_usd_micros`. Collect these fields from the
approved deployment and provider evidence, and keep the export and A/B key private.
The tool rejects missing, extra or duplicate receipts, mismatched identities or usage,
and confirmed cost above the supplied limit. This is reconciliation after execution.
A submission limit is separate from the spend limit applied before provider calls.

Give reviewers the anonymous packet, criteria.json and the generated review-sheet.csv. Keep the mapping
private until individual ratings are locked. For each case, record for A and B the
15 integer scores from 1–5, supporting text, severity S0–S4, failure taxonomy codes
when applicable, and the preferred A/B/tie. Score 1 for contradicted or harmful,
2 for material omissions, 3 for minimally acceptable, 4 for evidence-backed
and actionable, and 5 for exceptional performance against the case criteria.
For multi-turn cases check both outputs and score the final judgment against the
observed transition. The three explicit adversarial controls G022, G023 and G026
must be independently checked. Record any model self-identification as a possible
break in blinding.

After adjudication, resolve A/B to the model arms. Use stable receipt IDs and complete
both arms including any native evidence before applying the existing scorer to
the original catalog. The scorer expects `candidate_model`, `baseline_model`,
`execution_sha`, `reviewer`, `prompt_injection` and `results`. Each result includes
`id`, candidate `scores` (D01–D15), `severity`, `critical_path_unresolved`,
`candidate_receipt`, `baseline_receipt` and `blind_preference`
(`candidate`, `baseline` or `tie`). Each prompt-injection row requires `id` and `passed`.
Preserve both arms’ full dimension scores, feedback, disagreement and
adjudication records alongside that scorer input.

```sh
python3 evaluation/score_s3.py evaluation/s3-golden-draft.json adjudicated.json
```

The threshold output covers the numeric and explicit security criteria in that
script. Apply the additional release conditions in the pinned evaluation policy
and separately verify provenance and any unresolved native coverage.
