# سرور Kotlin — برش API نسخه 0.4.0-alpha.1

این پوشه سرور مستقل جدید را در چهار ماژول نگه می‌دارد. `domain` مدل و portها؛ `application` اجرای job و اعتبارسنجی؛ `adapters` اتصال PostgreSQL و provider؛ `api` احراز هویت و HTTP و bootstrap. کد پنل alpha در `apps/second-mind` محفوظ است؛ پنل متصل به این API در `apps/web-bff` و کلاینت بومی مشترک در `shared/client` قرار دارد.

## اجرا و تنظیم

JDK کامل 21 و PostgreSQL 16 لازم‌اند. نسخه دقیق آزمون در گزارش همین مرحله ثبت می‌شود؛ embedded PostgreSQL فقط وابستگی تست است و برای استقرار استفاده نمی‌شود. قالب تنظیم‌ها در `.env.example` است؛ برنامه آن فایل را خودکار بارگذاری نمی‌کند. تنظیم‌ها را از محیط اجرا یا secret manager تأمین کن.

```bash
./gradlew :server:api:installDist
server/api/build/install/api/bin/api --migrate
server/api/build/install/api/bin/api
```

دستور مهاجرت باید با credential مناسب migration اجرا شود. سرویس عادی با role محدود و مجوز لازم برای داده‌ها و خواندن history مهاجرت اجرا شود. مسیر عادی schema را خودکار تغییر نمی‌دهد؛ validate و pending migration بررسی می‌شوند. این schema شروع PostgreSQL است؛ ابزار تمرین انتقال SQLite در `apps/web-bff/tools/legacy-cutover.mjs` وجود دارد و فقط روی دادهٔ مصنوعی آزموده می‌شود؛ مهاجرت واقعی و سیاست نگاشت هنوز پذیرفته نشده‌اند. دو runtime نباید به یک دیتابیس زنده مشترک بنویسند.

`WH_DATABASE_URL` آدرس JDBC؛ `WH_DATABASE_USER/PASSWORD` حساب DB؛ `WH_AUTH_ISSUER/JWKS_URL/AUDIENCE` تنظیم IdP؛ `WH_AI_API_KEY/MODEL` اتصال AI؛ `WH_DAILY_RUN_LIMIT` سقف تلاش هر عضو در هر workspace در روز UTC. کلید یا نام مدل پیش‌فرض ساختگی نداریم. بدون provider، CRUD کار می‌کند و درخواست اجرای تحلیل رد می‌شود.

هویت این برش یک **resource server** است: access token با RS256، issuer و audience تنظیم‌شده، sub معتبر، exp و `token_use=access`. JWKS از URL HTTPS تنظیم‌شده با cache/rate limit دریافت می‌شود. IdP باید claim مخصوص access token را تولید کند. ورود OIDC، PKCE، refresh و BFF کوکی‌محور در `apps/web-bff` پیاده شده‌اند و resource server همچنان در کاتلین باقی می‌ماند. توکن احراز هویت در localStorage پنل ذخیره نمی‌شود؛ هویت واقعی و استقرار هدف گیت مستقل دارند. نقش workspace همیشه از DB خوانده می‌شود و از body یا claim نقش پذیرفته نمی‌شود.

سرور پیش‌فرض روی `127.0.0.1:8081` است. برای استقرار، reverse proxy با TLS و محدودیت درخواست/زمان، شبکه DB خصوصی و دسترسی محدود لازم‌اند؛ استقرار عمومی در این تحویل انجام نشده است.

## قرارداد و رفتار

مرجع API: [OpenAPI](../contracts/openapi/openapi.json)، در runtime نیز `/api/v1/openapi.json`. `/health` فقط liveness است. JSON و خطاها `request_id` دارند. بدنه JSON محدود به ۱۴۰۰۰۰ بایت است و فیلد ناشناخته پذیرفته نمی‌شود.

| مسیر | رفتار |
|---|---|
| GET `/api/v1/me` | subject و عضویت‌های فعال |
| GET `/api/v1/modes` | شناسه و واژگان حکم هفت Mode |
| POST `/api/v1/workspaces` | workspace تازه با مالک همان subject |
| POST `/api/v1/workspaces/{workspace}/members` | مدیریت عضویت فقط توسط OWNER؛ انتقال مالک exposed نیست |
| GET/POST `/api/v1/workspaces/{workspace}/decisions` | فهرست حداکثر ۱۰۰ تصمیم خود فرد یا ساخت تصمیم |
| GET/PUT `.../decisions/{decision}` | مشاهده یا جایگزینی کامل با expected_revision |
| POST `.../decisions/{decision}/runs` | expected_revision و Idempotency-Key؛ پاسخ 202 و run |
| GET `.../runs/{run}` | وضعیت، خطا، نتیجه معتبر و مصرف واقعی همان run |
| POST `.../runs/{run}/cancel` | لغو صف یا درخواست لغو اجرای در جریان |
| GET `.../admin/usage` | aggregate سازمان برای OWNER/ADMIN؛ بدون متن تصمیم دیگران |

مالک workspace هم بدون مجوز جدا متن خصوصی تصمیم دیگران را نمی‌خواند. SUPPORT در این برش به محتوای تصمیم و گزارش مالی دسترسی ندارد. تغییر عضویت روی درخواست بعدی اثر دارد. مدل permission سفارشی کامل پنل در مرحله بعد است. جداسازی فعلی در queryها و application اعمال و تست شده؛ RLS و مدل roleهای DB production هنوز گیت جدا دارند.

هر run snapshot ثابت ورودی، revision و hash کامل policy دارد. قرارداد خروجی فعلی **legacy-alpha.1** است؛ با envelope canonical 1.0 یکی نیست. prompt/schema هفت Mode از engine قبلی به‌شکل قابل‌تکرار تولید شده‌اند. هیچ دانش یا شخصیت آموزشی Elyora وارد سرور نشده است.

## صف، لغو و هزینه نامطمئن

رزرو سهمیه و ثبت run در یک تراکنش‌اند. کلید تکراری با ورودی یکسان همان run را می‌دهد؛ تغییر ورودی/کلید استفاده‌شده conflict است. worker با `FOR UPDATE SKIP LOCKED` و lease/token یکتا کار می‌گیرد. provider خارج از تراکنش DB فراخوانی می‌شود؛ credential، prompt و transcript در audit عمومی ذخیره نمی‌شوند.

لغو queued فوری است؛ لغو running به `cancellation_requested` می‌رود. نتیجهٔ بعدی نمایش داده نمی‌شود و usage/receipt واقعی حفظ می‌شود؛ لغو وعده حذف هزینه نیست. زمان provider محدود به ۶۰ ثانیه و lease به ۱۲۰ ثانیه است. پاسخ دیرهنگام authority ثبت حکم ندارد. lease منقضی حتی بدون پیکربندی provider بازیابی می‌شود.

timeout، خطای مبهم یا قطع worker به `reconciliation_required` می‌رود و خودکار دوباره اجرا نمی‌شود. تا بررسی receipt و حل مجاز این وضعیت، run جدید برای همان تصمیم مسدود است. مسیرهای incidents، operator permissions و اصلاح هزینه در API وجود دارند؛ پذیرش کامل رابط عملیاتی و تطبیق مالی پیش از سرویس تجاری لازم است. هزینه پولی از توکن حدس زده نمی‌شود؛ مقدار نامعلوم null می‌ماند. receipt واقعی دیرهنگام نیز نگه داشته می‌شود.

## تست و حدود تحویل

```bash
node scripts/generate_legacy_parity.mjs
node scripts/generate_server_policy.mjs
node scripts/generate_api_contract.mjs
./gradlew :shared:contracts:jvmTest :shared:contracts:compileKotlinJs :server:api:test :server:adapters:test
python3 scripts/verify_api_samples.py
```

بررسی آخر به Python package `jsonschema` نیاز دارد. تست API از Ktor test host، Netty روی HTTP loopback واقعی و PostgreSQL واقعی embedded استفاده می‌کند؛ provider و JWT issuer در تست fixture هستند. آزمون مرورگر روی BFF، Netty و PostgreSQL واقعی با هویت مصنوعی، تمرین مصنوعی انتقال SQLite، RLS، backup/restore و چرخهٔ کلاینت مشترک در workflow بازبینی تعریف شده‌اند. نتیجهٔ هر اجرا باید روی SHA همان نسخه از خروجی خام خوانده شود. AI و IdP واقعی، TLS استقرار هدف، پذیرش کامل رابط، صوت، امنیت/بار، سیاست داده و نصب و امضای دستگاه‌ها هنوز گیت باز دارند. تست ساخت/قرارداد معادل benchmark هوشمندی نیست.

CI در `.github/workflows/kotlin-api.yml` تعریف شده و مستقل از نتیجه اجرای محلی گزارش می‌شود. هیچ workflow این مرحله deploy نمی‌کند. raw stage receipts در `evidence/kotlin-api` قرار می‌گیرند.
