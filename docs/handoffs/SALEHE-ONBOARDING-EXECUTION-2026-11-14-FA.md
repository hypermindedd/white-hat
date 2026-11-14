# White Hat / Second Mind — راهنمای تحویل پروژه و دستور ادامه برای Salehe

**تاریخ بازبینی:** 2026-11-14  
**مرجع اجرایی:** `hypermindedd/white-hat`، PR [#2](https://github.com/hypermindedd/white-hat/pull/2)، شاخه `build/kotlin-platform-foundation-20261111`  
**HEAD بررسی‌شده پیش از نگارش:** `65523c4bf92abc4b3f8315b2c287ee83634d0ba7`  
**وضعیت:** Handover / NOT a production release / NOT S2 acceptance  
**مخاطب:** Salehe، Chief، مهندسان و بازبینان فنی White Hat.

> این سند راهنمای عملیاتی بر اساس شواهد موجود در تاریخ بالاست. پیش از هر توسعه، HEAD/CI/اسناد جدیدتر را مجدداً بخوان. ادعای PASS فقط با لاگ/تست واقعی روی نسخه مشخص مجاز است. این فایل جایگزین کد یا گزارش اجرای جدید نیست.

## 0. هشدارهای فوری و کنترل دسترسی

1. **مخزن در بررسی 2026-11-14، Public است.** نام `private` در برخی متن‌های قدیمی واقعیت فعلی مخزن نیست. هیچ secret، credential، داده مشتری، transcript یا محتوای شخصی را commit نکن. مالک باید تصمیم جداگانه درباره خصوصی‌کردن مخزن و سیاست اعضا بگیرد.
2. سطح مجوز خواندن برای نام‌های بررسی‌شده `salehe` و `saleheebrahimi2029-hub` از API، `read` گزارش شد؛ این خروجی به‌تنهایی نشان نمی‌دهد که دسترسی اختصاصی فراتر از خواندن مخزن عمومی داده شده است. برای push مستقیم باید Write یا بالاتر توسط مالک اعطا شود؛ در غیر این صورت Fork + PR.
3. PR #2 در زمان بررسی **Draft, open, unmerged** و PR #1 طراحی مذاکره زنده هم **Draft, open, unmerged** است؛ `main` را با head PR #2 یکی فرض نکن.
4. هیچ استقرار واقعی، مهاجرت دیتای مشتری، آزمون AI زنده، آزمون IdP واقعی، آزمون مرورگر کامل یا ساخت native تأیید نشده است.

## 1. چشم‌انداز و مرز محصول

**مأموریت:** به تصمیم‌گیرنده کمک کنیم *قبل از تعهد منابع* فرض شکننده، مجهول مهم، قوی‌ترین مخالفت، ریسک و اقدام بعدی را ببیند؛ «تصمیمت را بده؛ قبل از برخورد با واقعیت، ضعف‌هایش را بررسی می‌کنیم.» مخاطب اولیه: بنیان‌گذار، مدیر، تیم رهبری و تصمیم‌گیر حرفه‌ای.

**محصول نهایی:** یک سامانه مستقل تجاری با وب‌اپ و پنل، API یکپارچه، ذخیره و هویت چند workspace، محاسبه مصرف واقعی، مدیریت نقش و پشتیبانی، ارزیابی کیفیت، و کلاینت Android/iOS/macOS/Windows. مدل‌های AI و صوت سرویس‌دهنده قابل تعویض‌اند؛ **ما GPT یا LLM جدید آموزش نمی‌دهیم**. داده‌های تصمیم و احکام متعلق به سرویس و قرارداد White Hat هستند، نه مدل یا مرورگر.

**هفت Mode موجود:** Challenge It، Kill My Idea، Find My Blind Spots، Pre-Mortem، Reality Check، Compare، Devil's Advocate. این هفت حالت یک هسته و ontology دارند؛ نه هفت برنامه مستقل. ۱۱ قابلیت Core و ۸ قابلیت شرطی در اسناد مرجع تعریف شده‌اند. هر خروجی باید ادعا/شاهد/فرض/مجهول را تفکیک کند. Confidence احتمال موفقیت نیست.

**مرز قابلیت مذاکره زنده:** PR #1 فعلاً *فقط design* است. استفاده مجدد از transport صوت Elyora ممکن است، ولی شخصیت آموزشی، حساب‌ها، DB و prompt آن قابل انتقال نیست. مذاکره «نوع تعامل» است نه Mode هشتم؛ سخنگوی صوتی صاحب authority حکم نیست؛ نتیجه باید از همان هسته با validator عبور کند. تماس با شخص ثالث یا پذیرش تعهد بیرونی در دامنه فعلی نیست.

**مرزهای تصمیم تجاری:** حافظه تحلیلی بین تصمیم‌ها، اقدام خارجی خودکار، خودآموزی از اطلاعات حساس کاربران، چندعاملی و analytics جمعیت‌شناختی بدون قرارداد/رضایت، در scope فعلی پذیرفته نشده‌اند. پیشنهاد آینده را با قابلیت پیاده‌شده اشتباه نگیر.

## 2. آنچه واقعاً داریم — با شاهد

| لایه/دارایی | موجود | وضعیت و محدودیت |
|---|---|---|
| منبع دانش | 17 snapshot از Notion، manifest و hashها | مستندات معتبرِ منتقل‌شده؛ اجرای موتور نیست |
| چشم‌انداز/قواعد | `docs/VISION-FA.md`، `docs/INDEX.md`، `docs/sources/*` | نسخه‌های تاریخی و قراردادهای معنایی؛ تعارض‌ها را trace کن |
| alpha مستقل v0.3.0-alpha.1 | `apps/second-mind/` | UI فارسی RTL + Node HTTP + SQLite + حساب، تصمیم/تاریخچه، مدیریت alpha، ۷ Mode، provider adapter؛ نسخه تجاری نیست |
| baseline alpha | `apps/second-mind/evidence/standalone/test-report.json` | 23 آزمون Node fixture؛ AI زنده = 0 طبق اسناد ثبت‌شده |
| معماری چندسکویی | `docs/architecture/ADR-010-KOTLIN-PLATFORMS-FA.md` | Kotlin/JVM+Ktor در سرور؛ KMP/Compose هدف اپ‌ها؛ وب DOM/JS با مهاجرت تدریجی TypeScript |
| قرارداد مشترک | `shared/contracts/` | 7 mode و validator خروجی flat قدیمی؛ 8 تست JVM، 42 مورد parity، JS compile؛ canonical 1.0 هنوز معادل legacy نیست |
| backend Kotlin S1 | `server/domain`, `application`, `adapters`, `api` | PostgreSQL/Flyway، JWT/JWKS resource server، workspace/membership، تصمیم revisionدار، runs، quota، idempotency، lease/fencing، cancel، receipt/reconciliation، OpenAPI v1 |
| شواهد backend | `evidence/kotlin-api/report.json` | **28 تست Kotlin + 23 Node**، PostgreSQL واقعی 16.15 و HTTP loopback؛ provider و IdP fixture هستند |
| وب BFF S2 | `apps/web-bff/` | OIDC Code+PKCE، session cookie، CSRF/origin، proxy allowlist، پنل DOM و عملیات API v1؛ هنوز fixture slice |
| شواهد S2 | `evidence/web-bff/report.json` | 4/4 آزمون محلی روی Node 22.16، هدف Node >=24.15؛ browser E2E blocked؛ اتصال واقعی Ktor/Postgres/IdP اثبات نشده |
| قرارداد API | `contracts/openapi/openapi.json` | OpenAPI 3.1، `/api/v1`، پاسخ `legacy-alpha.1`; canonical envelope v1 هنوز migration لازم دارد |
| صوت و مذاکره | PR [#1](https://github.com/hypermindedd/white-hat/pull/1)، `docs/design/LIVE-NEGOTIATION-v0.1.0-FA.md` در شاخه طراحی | فقط قرارداد/طرح؛ route/UI/تست runtime ندارد |
| اپ native، پرداخت، امنیت تولید | مسیر مصوب در ADR-010 | هنوز خروجی قابل پذیرش و نصب/انتشار ندارند |

**تمایز حیاتی:** پنل بازیابی‌شده `apps/second-mind` آخرین پنل جامع طراحی‌شده در گفتگوها *احراز نشده*. منبع جدیدتر را با commit/hash اثبات کن؛ UI پنل Elyora را اشتباهاً White Hat تلقی نکن. اگر منبع تازه یافت نشد، adapter موجود را پیش ببر و فاصله‌ها را ثبت کن.

## 3. اکنون دقیقاً چه فازی است؟

در نقشه اجرایی **جدید Kotlin** فازها چنین‌اند (این شماره‌ها با نقشه تاریخی GPT و S00–S09 alpha یکی نیستند):

| فاز | خروجی مقرر | وضعیت در 2026-11-14 | Gate لازم |
|---|---|---|---|
| S0 — بازیابی و معماری | alpha preserved، ADR، قرارداد مشترک، hash و baseline | **انجام و شواهد محلی ثبت**؛ بازبینی مستقل باز | provenance و regression قابل تکرار |
| S1 — API Kotlin | PostgreSQL، Ktor، auth resource server، ownership، queue/runs، OpenAPI | **برش کد و تست محلی انجام**؛ remote CI شکست خورده و release gate باز | CI سبز، مرور معماری و آزمون یکپارچگی |
| S2 — وب، پنل، BFF و نقش‌ها | login OIDC و وب متصل به API، roles، UX و browser E2E | **در حال اجرا / ناقص**؛ BFF و UI adapter روی mock تست شده‌اند | اتصال واقعی، نشست پایدار، RBAC/پنل پشتیبانی، RTL/دسترسی‌پذیری و E2E |
| S3 — هوشمندی معتبر و صوت زنده | 60 case کامل، ارزیابی انسانی 15بعدی، provider واقعی، transport صوت | **انجام نشده**؛ design صوت موجود | quality benchmark، امنیت/مصرف صوت و همه تست‌های واقعی |
| S4 — اپ‌های Android/iOS/macOS/Windows | KMP client + Compose، بسته‌های واقعی، UX و device test | **انجام نشده** | build/install/signing و آزمون دستگاه/ارتقا |
| S5 — تجاری‌سازی و انتشار | قیمت/پرداخت/اشتراک، backup/restore، امنیت/بار، پایلوت، rollout | **انجام نشده** | GO/NO-GO مستند با metrics، اقتصاد، امنیت و عملیات |

در نقشه تاریخی `docs/ROADMAP-FA.md` اول مسیر GPTs-first آمده، ولی طبق `apps/second-mind/docs/standalone/ADR-009-FA.md` مسیر محصول مستقل جای آن را گرفته است. از تغییر برنامه و نسخه‌گذاری بدون رجیستر تعارض اجتناب کن؛ قراردادهای معنایی تاریخی همچنان معتبرند.

## 4. وضعیت واقعی CI در آخرین بررسی

- HEAD پیش از نگارش این handoff: `65523c4bf92abc4b3f8315b2c287ee83634d0ba7`.
- Kotlin API workflow run [43047177904](https://github.com/hypermindedd/white-hat/actions/runs/43047177904): **failure**.
- Web BFF workflow run [43047177933](https://github.com/hypermindedd/white-hat/actions/runs/43047177933): **failure**.
- در اتصال GitHub فعلی، jobs برای هر دو run `steps: null` و `logs_url: null` داشتند؛ دریافت job log خطای `404 BlobNotFound` داد. علت شکست احراز نشده است. «کد خراب است»، «مشکل پول است» یا «مجوز Action» هیچ‌کدام نتیجه قطعی نیستند.
- نخست در UI GitHub، Job Summary / annotations / billing/runner eligibility / workflow permissions و خطای scheduler را بررسی کن؛ سپس rerun پس از رفع علت و لاگ قابل ارجاع ثبت کن. status workflow و commit SHA را با زمان بررسی بنویس.
- PASS محلی S1 یا چهار تست mock BFF به‌معنی PASS CI یا production نیست.

## 5. معماری واحد و قوانین غیرقابل نقض

`Browser → Node BFF (auth/session/CSRF only) → Kotlin Ktor /api/v1 → domain/application → PostgreSQL + provider adapter`.

اپ‌های KMP در آینده **همان API** را مصرف می‌کنند؛ هیچ client صاحب permission، pricing، AI policy، tenant authority یا ثبت نتیجه نیست.

1. Kotlin `server` مرجع state، تصمیم، workspace، quota، idempotency و usage است. BFF فقط مرز transport/auth و session؛ دیتابیس/موتور دوم ممنوع.
2. alpha Node/SQLite را تا migration تأییدشده دست‌نخورده نگه دار؛ دو writer روی داده واقعی نساز.
3. خروجی `legacy-alpha.1` را بی‌دلیل `canonical 1.0` اعلام نکن. ontology و mode IDs، invariants، acceptance fixture و OpenAPI باید versioned باشند.
4. private decision حتی برای مدیر workspace بدون permission مستقل قابل مشاهده نیست. سرور باید deny-by-default، membership و مالکیت را enforce کند؛ 401/403/404/409/429 و unknown usage را دقیق حفظ کن.
5. provider response نامعلوم **نباید** سبب retry پولی خودکار، صفر فرضی usage یا موفقیت ساختگی شود. لغو UI لزوماً لغو هزینه provider نیست.
6. token/secret در مرورگر، localStorage، repo، لاگ عمومی و PR ممنوع. BFF production نیازمند session store پایدار/رمزگذاری‌شده و IdP واقعی است.
7. آزمون کد ≠ ارزیابی کیفیت استدلال. output صحیح schema ≠ judgment صحیح. هیچ PASS بدون سناریو، خروجی خام، rubric و بازبین.
8. فقط معماری مصوب؛ no patch، version everything، تست قبل از خروجی، rollback و traceability. تصمیم دامنه جدید با ADR و acceptance ثبت شود.
9. داده‌های حساس analytics/آموزش مدل با رضایت، retention، حذف و حداقل‌سازی جمع‌آوری؛ analytics عملیات، داده محصول و training را مخلوط نکن.
10. قبل از هر cutover واقعی: backup، rehearsal روی کپی، انتقال مالکیت account/workspace/decision/run، کنترل parity و مسیر برگشت داده-aware.

## 6. دستور اجرایی قدم‌به‌قدم برای Salehe

### مرحله A — روز شروع: ایمنی، دریافت مرجع و تکرار baseline

1. با حساب خودت به [مخزن](https://github.com/hypermindedd/white-hat) وارد شو. `main` را برای توسعه مبنا نگیر؛ PR #2 و HEAD تازه را بخوان. دسترسی Write را از مالک درخواست کن یا Fork+PR بساز. بدون دسترسی، مستقیم `main` را تغییر نده.
2. دستورها در محیط مجاز:
   ```bash
   git clone https://github.com/hypermindedd/white-hat.git
   cd white-hat
   git fetch --all --prune
   git switch build/kotlin-platform-foundation-20261111
   git pull --ff-only
   git rev-parse HEAD
   git status --short
   ```
3. به ترتیب بخوان: `KOTLIN-START-HERE-FA.md`، `docs/VISION-FA.md`، `docs/INDEX.md`، `docs/architecture/ADR-010-KOTLIN-PLATFORMS-FA.md`، `docs/architecture/ADR-011-API-MIGRATION-SLICE-FA.md`، `docs/stages/S1-CLOSURE-FA.md`، `docs/stages/S2-IMPLEMENTATION-FA.md`، `server/README-FA.md`، `apps/web-bff/README-FA.md` و دو evidence report.
4. روی **JDK 21 کامل**، **Node 24.15+ از شاخه 24** و DB آزمون PostgreSQL 16، source hash و test را بازاجرا کن. از DB production یا داده واقعی برای تست استفاده نکن:
   ```bash
   python3 scripts/verify_docs.py
   node scripts/generate_legacy_parity.mjs
   node scripts/generate_server_policy.mjs
   node scripts/generate_api_contract.mjs
   (cd apps/second-mind && npm test)
   (cd apps/web-bff && npm run check && npm test)
   ./gradlew --no-daemon :shared:contracts:jvmTest :shared:contracts:compileKotlinJs :server:api:test :server:adapters:test :server:api:installDist
   python3 scripts/verify_api_samples.py
   ```
   *یادآوری:* verifier نمونه‌ها به `jsonschema` نیاز دارد (مطابق workflow pin شده). هر شکست را با نسخه toolchain، فرمان و log جدا ثبت کن؛ از خروجی تاریخی به جای اجرای خودت استفاده نکن.
5. دو Actions fail اخیر را بررسی کن و نخست علت مشخص و قابل‌تکرار را رفع کن؛ اقدام حدسی روی YAML نکن. پنل جدیدتر ادعاشده را با source pin بررسی کن.

**خروجی A:** `evidence/s2b/baseline-<date>.md` با SHA، نسخه ابزار، counts، نتیجه فرمان، وضعیت CI و blocker، و تصمیم روشن source-of-truth پنل.

### مرحله B — S2B واقعی: اتصال end-to-end

1. با **issuer محلی fixture و PostgreSQL تست**، API Kotlin/Ktor واقعی + BFF واقعی را با configuration ایمن بالا بیاور؛ UI را به mock سرور متکی نگه ندار. از دستور `server/README-FA.md` و `.env.example` فقط قالب تنظیمات را بگیر.
2. سناریوهای واقعی تحت تست: OIDC login/refresh/logout/replay، CSRF، workspace ایجاد، عضو/نقش، هفت Mode، تصمیم و revision conflict، run/idempotency، polling، cancel/late receipt، unknown usage، `reconciliation_required`، قطع/وصل، 401/403/404/409/429.
3. Playwright/Chromium در محیطی با مجوز loopback/automation اجرا شود. وضعیت قبلی `ERR_BLOCKED_BY_ADMINISTRATOR` به‌معنای PASS نیست. RTL، viewport موبایل/دسکتاپ، keyboard focus، screen reader/accessibility و حالت‌های empty/loading/error را تست کن.
4. هیچ شماره/تاریخچه run اختراعی نساز. برای run history و member listing، API مجاز سمت سرور طراحی/تست کن؛ role builder در UI بدون enforce سمت سرور مجاز نیست.
5. session store production-grade، revoke/rotation، abuse control، چند replica، secret manager و IdP واقعی را جداگانه به‌عنوان gate بعد از integration محلی بپذیر.

**خروجی B:** browser E2E log/video یا trace، تست جداسازی role/tenant، OpenAPI drift report، محدودیت امنیتی با severity و SHA.

### مرحله C — تکمیل S2 و آماده‌سازی S3

1. **پنل کامل مدیر و پشتیبانی:** permission matrix با نقش‌های قابل‌تعریف و دامنه مجوز؛ staff provisioning/deprovision، audit، support workflow، گزارش مصرف و هزینه واقعی، گزارش خطا و latency، feature flags، feedback/quality queue. متن خصوصی تصمیم را به SUPPORT پیش‌فرض نده. تفکیک dashboard کاربران و مدیران الزام است.
2. **داده و امنیت:** RLS با tenant isolation، migration rehearsal SQLite → PostgreSQL روی کپی، backup+restore drill، rollback پس از write، مدیریت داده/consent، throttling، abuse/error budgeting.
3. **کیفیت AI:** 60 سناریوی golden کامل (نه صرفاً کاتالوگ)، 15 بُعد ارزیابی، روی مدل واقعی و همراه خروجی خام؛ میانگین وزنی >=4/5؛ هر Mode >=3.8؛ آستانه‌های D03>=4.2، D09>=4.1، D11>=4، D13>=3.8؛ حداقل 95٪ کیس‌ها در ابعاد بحرانی >=3؛ صفر S4، Prompt Injection صددرصد، S3 بحرانی حل‌نشده صفر. مقایسه کور با baseline و بررسی انسان؛ هزینه و latency اندازه‌گیری شوند.
4. **مذاکره زنده:** PR #1 را فقط پس از source pin و تست compatibility donor با معماری کنونی reconcile کن؛ `VoiceTransport`, session/turn coordinator، budget، transcript نهایی، barge-in، reconnect، output validation، feature flag و آزمون دستگاه. persona/DB/Auth الیورا منتقل نشود.
5. **کلاینت‌ها:** پس از تثبیت API، `shared/client` و KMP/Compose را برای Android/iOS/macOS/Windows از یک API بساز؛ build/test روی دستگاه واقعی، signing/notarization/store و نصب/ارتقا، همه گیت مستقل‌اند.
6. **تجاری‌سازی:** pricing/entitlements، metering واقعی، checkout/webhook و reconciliation، retention/consent، backup/restore، security/load، pilot اندازه‌گیری‌شده، unit economics و GO/NO-GO نهایی.

### مرحله D — روش تحویل هر PR

برای هر تغییر: (i) مسئله و acceptance ID، (ii) SHA پایه، (iii) ADR/contract و دامنه، (iv) کد و migration، (v) تست واقعی + raw evidence، (vi) امنیت/حریم خصوصی، (vii) rollback، (viii) residual risk، (ix) next prompt. هیچ PR بدون شواهد به `main` merge نشود؛ merge و انتشار تصمیم مجزای release هستند. با تعطیلی مرحله، `docs/stages/S<n>-CLOSURE-FA.md` و `docs/stages/S<n+1>-NEXT-CHAT-FA.md` را به‌روز کن. مختص مرحله S2B ابتدا گزارش CI/E2E و پنل را جمع کن؛ مجبور نیستی تمامی محصول را در یک PR تمام کنی.

## 7. تعریف «تمام‌شدن» و اولویت‌های بعدی

**اولویت 1 — Blocker:** خواندن دقیق آخرین HEAD، رفع CI بر اساس root cause قابل مشاهده، baseline مطابق toolchain.  
**اولویت 2 — Blocker:** اتصال واقعی Kotlin + PostgreSQL + BFF + fixture IdP و E2E مرورگر.  
**اولویت 3 — پیش از S2 closure:** نقش/پشتیبانی و access test، session پایدار، UX و خطاهای کامل.  
**اولویت 4 — پیش از release:** AI benchmark واقعی، migrate/restore، امنیت/بار، سرویس billing، اپ‌ها/صوت مطابق گیت مستقل.

تا این موارد و پذیرش مستقل انجام نشده‌اند، برچسب محصول: **ALPHA / NOT PRODUCTION READY**. درصد پیشرفت کلی بدون وزن‌گذاری و evidence معتبر نیست.

## 8. پرسش‌های باز مالک/تیم که باید در issue/ADR ثبت شوند

- مشخصات واقعی و سورس آخرین طراحی جامع White Hat panel چیست؟ اگر فقط mockup است، قرارداد آن با API چیست؟
- نام تجاری نهایی روی رابط SECOND MIND است یا White Hat؟ نام repository به‌تنهایی تغییر برند نیست.
- IdP، محل استقرار، دامنه محصول، محدودیت و دسته‌های داده حساس چیست؟
- چه نقش‌هایی، چه حقوقی، چه دوره نگهداری/حذف و کدام موارد analytics با رضایت کاربر باید فعال شوند؟
- provider/modelهای آزمون، بودجه Golden و human review و تعریف واحد اقتصاد مصرف کدام‌اند؟
- ظرفیت واقعی توسعه و اولویت انتشار ابتدا وب یا هم‌زمان اپ‌ها چیست؟

بازبودن این تصمیم‌ها نباید بهانه شروع معماری موازی، نوشتن قیمت فرضی یا ادعای عملکرد بدون آزمون شود.

## 9. لینک‌های مرجع و پرامپت آماده اجرا

- [S0/S1 entry](../../KOTLIN-START-HERE-FA.md)
- [سرویس Kotlin](../../server/README-FA.md)
- [چک‌پوینت S2](../stages/S2-IMPLEMENTATION-FA.md)
- [شواهد S1](../../evidence/kotlin-api/report.json)
- [شواهد S2](../../evidence/web-bff/report.json)
- [راهنمای BFF](../../apps/web-bff/README-FA.md)
- [چشم‌انداز](../VISION-FA.md)
- [طراحی صوت در PR #1](https://github.com/hypermindedd/white-hat/pull/1)

**نام چت/Work بعدی:** `White Hat | S2B — CI + Real Kotlin-BFF E2E + Salehe Handoff`.

**پرامپت:** «در مخزن `hypermindedd/white-hat`، PR #2 را از HEAD واقعی آغاز کن. سند `docs/handoffs/SALEHE-ONBOARDING-EXECUTION-2026-11-14-FA.md` و فایل‌های مرجع آن را بخوان. سرور Kotlin، alpha یا هسته هفت Mode را دوباره نساز. ابتدا CI failure را با شواهد ریشه‌یابی و toolchain را تکرار کن؛ سپس Kotlin+PostgreSQL+BFF+local IdP را یکپارچه و E2E مرورگر/دسترسی‌ها/RTL را اجرا کن. پنل جامع جدیدتر را source-pin کن یا شکاف آن را صریح ثبت کن. تغییرهای کد، تست، evidence، rollback و گزارش گیت را در همان مخزن و PR ثبت کن. هیچ تست mock را real یا CI PASS ننام. در انتهای مرحله سند closure و دستور چت بعدی را ارائه کن.»
