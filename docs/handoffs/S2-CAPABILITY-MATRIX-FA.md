# White Hat — ماتریس قابلیت و شاهد S2

مرجع انتخاب پنل و سیاست محیط: ADR-012. این ماتریس جای خروجی اجرای واقعی را نمی‌گیرد؛ نتیجه و SHA نهایی در S2-CLOSURE ثبت می‌شوند. همهٔ مسیرهای وب از DOM به BFF و سپس همین API Kotlin می‌روند.

| معیار | پیاده‌سازی موجود | شاهد اجرایی لازم |
|---|---|---|
| C1-01، workspace/decision و هفت Mode | `panel.js`: overview/decisionForm/history/detail؛ `/me`، `/modes`، decisions/runs | seven-mode API samples و `actualHttpRouteQueuesAndWorkerCompletesAllSevenModes`، مرورگر مشترک |
| C1-02، نقش و منع پیش‌فرض | Kotlin `PostgresStore.requireRole` و routing API؛ nav فقط نمایش | organization/member/support/owner test و JWT invalid/expired؛ UI به‌تنهایی مجوز نیست |
| C1-03، پشتیبانی محدود | members، support roles، operator permissions، incidents | grant/revoke در نشست باز؛ private content و مبلغ برای SUPPORT ممنوع |
| C1-04، لغو و tenant isolation | عضویت فعال server-side، RLS و workspace-scoped query | customSupportRoleRevocation و browser نقش‌های پنج‌گانه؛ درخواست خارج workspace برابر 404 |
| C1-05، usage/finance/audit | usage، pilot usage، confirmed costs/corrections، operations metrics؛ audit append-only | مقادیر unknown برابر null، receipt/correction idempotency و دادهٔ خصوصی محدود |
| C1-06، feedback و کارکنان | quality queue، feedback enable/withdraw/retention، members active | feedback retention/purge و staff grant/revoke در API/مرورگر |
| C1-07، سابقهٔ واقعی | `runHistory` و `members` از API | دادهٔ سرور، مالکیت شخصی و نمایش reconciliation؛ شناسهٔ نمایشی جای داده نیست |
| C1-08، تجربه و دسترس‌پذیری | source pin شش فایل DOM/BFF/visual assets؛ RTL و focus | مرورگر مشترک + real-Keycloak reference؛ axe WCAG A/AA، ARIA، کیبورد و عرض موبایل؛ آزمون انسانی AT برای انتشار تولید باز |
| C2-01، authority واحد | Kotlin/domain/application/adapters؛ DB نشست BFF فقط سه جدول transport | hard gates معماری Kotlin و source pin؛ mutation tests writer دوم و cycle |
| C2-02، امنیت داده | API role غیرمالک/NOBYPASSRLS؛ جداسازی tenant و ownership | query بدون context دادهٔ تصمیم را نمی‌بیند؛ RLS و API منفی‌های بین workspace |
| C2-03، نشست | PG store رمزگذاری‌شده؛ دو BFF با کلید مشترک؛ verifier تولیدی Kotlin | TLS/PKCE واقعی Keycloak، CA نامعتبر رد، شکست یک replica، refresh هم‌زمان و rotation واقعی امضای IdP، logout/revoke/حذف نشست |
| C2-04، مهاجرت | legacy-cutover روی snapshot read-only و mapping صریح | mapping معتبر/قرنطینه، عدم انتقال credential، رد ambiguous/missing-owner |
| C2-05، rollback داده | transaction rollback و restore cluster مستقل | rollback پس از insert و حفظ committed writes مقصد؛ نه فقط revert کد |
| C2-06، بازیابی | fixture دو cluster و backup/restore در همان workflow | برابری دادهٔ checkpoint و rehearsal حداکثر ۱۲۰ ثانیه؛ RPO/RTO تولید نیازمند workload واقعی مستقل است |
| C2-07، سیاست و حذف | سیاست v1 در ADR-012؛ reference فقط synthetic، feedback opt-in، بدون training | حذف نشست در logout، destroy realm/database/volume/credentials و trust موقت؛ artifact بدون credential |

## قراردادهای تجاری و اپ‌ها

`CommercialPlans` و `admin/plan-preview` بخشی از همین Kotlin هستند؛ status=proposal و billing_enabled=false. هیچ paid access از GUI یا quote تولید نمی‌شود. native clients همین `/api/v1` را مصرف می‌کنند؛ build با نصب و signing یکی نیست. real-model quality، donor صوت Elyora، payment و pilot/production پذیرش مستقل مراحل بعد دارند.
