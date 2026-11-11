// Deterministic migration fixtures. No model/network calls and no user data.
// Default checks for drift; --write regenerates both representations in place.
import {readFileSync, writeFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {MODES, validateOutput} from '../apps/second-mind/server/core/engine.mjs';
import {validResult} from '../apps/second-mind/tests/fixtures.mjs';

const cases = [];
function add(id, mode, value) {
  let accepted = true;
  try { validateOutput(value, mode); } catch { accepted = false; }
  cases.push({id, mode, accepted, value});
}
for (const mode of MODES) {
  const value = validResult(mode.id);
  add(mode.id + '-valid', mode.id, value);
  add(mode.id + '-wrong-mode', mode.id, {...value, mode:'unknown'});
  add(mode.id + '-high-confidence', mode.id, {...value, confidence:'HIGH'});
  add(mode.id + '-missing-summary', mode.id, Object.fromEntries(Object.entries(value).filter(([key]) => key !== 'summary')));
  add(mode.id + '-bad-next-action', mode.id, {...value, next_action:{...value.next_action, observable_result:''}});
  add(mode.id + '-needs-context', mode.id, {...value, verdict:'NEEDS_CONTEXT', clarification_question:'هدف تصمیم چیست؟'});
}
const rows = cases.map(value => '"""' + JSON.stringify(value) + '"""').join(',\n');
const kotlin = `package whitehat.contracts
import kotlinx.serialization.json.*
import kotlin.test.*
class LegacyParityTest {
 @Test fun recoveredEngineAcceptanceIsPreserved() {
  val cases = listOf(\n${rows}\n).map { Json.parseToJsonElement(it).jsonObject }
  assertEquals(42, cases.size)
  for (row in cases) {
   val mode=Mode.fromWire(row.getValue("mode").jsonPrimitive.content)!!
   val expected=row.getValue("accepted").jsonPrimitive.boolean
   assertEquals(expected, LegacyOutputValidator.validate(row.getValue("value"),mode).isEmpty(),row.getValue("id").jsonPrimitive.content)
  }
 }
}
`;
const artifacts = [
  ['../evidence/kotlin-foundation/legacy-parity-cases.json', JSON.stringify(cases, null, 2) + '\n'],
  ['../shared/contracts/src/commonTest/kotlin/whitehat/contracts/LegacyParityTest.kt', kotlin]
];
for (const [relative, content] of artifacts) {
  const path = fileURLToPath(new URL(relative, import.meta.url));
  if (process.argv.includes('--write')) writeFileSync(path, content);
  else if (readFileSync(path, 'utf8') !== content) throw new Error('PARITY_FIXTURE_DRIFT: ' + relative);
}
console.log('42 legacy acceptance fixtures verified; no AI run.');
