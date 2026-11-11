# نام چت بعدی

White Hat | مرحله ۲ — اتصال وب و پنل به Kotlin

## محیط مناسب

Work؛ این مرحله شامل خواندن مخزن، کدنویسی، اجرای سرور و تست مرورگر است. Chat برای تصمیم‌های محصول، تجربه، برند و قیمت‌گذاری قابل استفاده است؛ تصمیم نهایی هر بار باید در همین مخزن ثبت و سپس در Work اجرا شود. تغییر محیط به معنی شروع معماری یا مخزن موازی نیست.

## پرامپت آماده ادامه

پروژه White Hat / Second Mind را از مرحله ۲ ادامه بده؛ هدف محصول مستقل تجاری وب، Android/iOS و macOS/Windows است. زبان سرور Kotlin/JVM + Ktor، اپ‌ها KMP/Compose و وب DOM با TypeScript تدریجی است. هفت Mode روی یک هسته مشترک حفظ شوند.

مخزن خصوصی `hypermindedd/white-hat`، PR شماره ۲: https://github.com/hypermindedd/white-hat/pull/2 ، شاخه `build/kotlin-platform-foundation-20261111`. ابتدا head دقیق PR و وضعیت merge/CI را بخوان؛ اجرای CI روی commit کد 19fe6c6 پیش از stepها شکست خورده و دلیل از ابزار فعلی مشخص نشده است؛ annotation همان run را با دسترسی مجاز بررسی و CI را پیش از پذیرش انتشار اصلاح کن. سورس جدیدتر را حفظ کن. نقطه ورود `KOTLIN-START-HERE-FA.md`، سپس `server/README-FA.md`، ADR-010 و ADR-011، `docs/stages/S1-CLOSURE-FA.md` و `evidence/kotlin-api/report.json` است. دستورهای قدیمی foundation در صورت تعارض مقدم نیستند.

تا اینجا سورس alpha در `apps/second-mind` بازیابی شده و سرور در چهار ماژول `server/domain`، `application`، `adapters` و `api` پیاده شده است. resource server JWT/JWKS، membership workspace، private decision ownership، PostgreSQL/Flyway، queued runs با idempotency، quota، lease/fencing، cancellation و reconciliation و adapter AI وجود دارند. خروجی migration فعلاً flat `legacy-alpha.1` است، نه canonical envelope 1.0. provider واقعی و IdP واقعی هنوز تست نشده‌اند. شواهد فعلی fixture و DB واقعی‌اند؛ کیفیت AI و production PASS نیستند.

مأموریت این مرحله:

1. آخرین سورس پنل و وب ساخته‌شده برای همین محصول را در منابع متصل پیدا و source pin آن را مشخص کن؛ نسخه alpha بازیابی‌شده را آخرین پنل جامع امروز فرض نکن. پنل عمومی Elyora را با White Hat مخلوط نکن. اگر منبع جدید پیدا نشد، adapter و اتصال پنل بازیابی‌شده را پیش ببر و اختلاف طراحی را صریح ثبت کن.
2. BFF و جریان ورود امن وب را بر API v1 بنا کن: Authorization Code + PKCE، نشست HttpOnly/Secure، CSRF، logout/revoke و refresh مطابق IdP انتخاب‌شده. توکن و کلید AI وارد localStorage یا کد مرورگر نشوند. نبود credential مانع نوشتن و تست این جریان با issuer محلی آزمون نیست؛ اتصال واقعی را بدون شاهد تأیید نکن.
3. ساخت workspace، عضویت و نقش‌های مجاز، ایجاد/ویرایش تصمیم، انتخاب Mode، ثبت run، نمایش وضعیت/خطا/نتیجه، لغو و گزارش مصرف را به پنل متصل کن. حالات empty/loading/error و قطع/وصل را کامل کن. custom permission builder و پنل جامع پشتیبانی را براساس نیازمندی مالک و قراردادها مرحله‌بندی و trace کن؛ کنترل سمت سرور مرجع مجوز بماند.
4. API version و قرارداد OpenAPI را حفظ کن. خطاهای 401/403/404/409/429 و `reconciliation_required` در UI درست نمایش داده شوند. مقدار مصرف نامعلوم با صفر یا قیمت حدسی جایگزین نشود. مدل صوت قاضی موازی نشود؛ live design در شاخه `design/live-negotiation-20261111` باید در مرحله صوت با هسته مشترک آشتی داده شود.
5. SQLite alpha را خودکار به PostgreSQL تولید مهاجرت نده. ابتدا قرارداد انتقال حساب/workspace/تصمیم/run، rehearsal روی کپی، backup و rollback را کامل و آزموده کن. دو writer زنده مشترک نساز.
6. تست‌های source drift، baseline Node، Kotlin، PostgreSQL و schema را اجرا؛ سپس E2E وب، RTL، مجوزها، نشست، keyboard/accessibility و لغو/reconnect را روی مرورگر واقعی انجام بده. نتیجه واقعی CI، تست محلی، مدل واقعی، انسان و دستگاه را جدا گزارش کن.
7. تغییرها را با commit و PR به‌روز کن؛ مرحله را با evidence، محدودیت و rollback ببند. اگر مرز بخش یا طول کانتکست رسید، نام چت بعدی و پرامپت خودبسنده ادامه را در مخزن و پاسخ بده. برای تصمیم‌های نظری از Chat و برای پیاده‌سازی از Work استفاده کن. اجازه کارهای معمول و برگشت‌پذیر در همین دامنه داده شده؛ برای آن‌ها دوباره تأیید نگیر.

گیت‌های کل محصول هنوز شامل AI benchmark انسانی/واقعی، صوت واقعی، IdP واقعی، RLS/امنیت/بار، backup/restore، پرداخت/اشتراک، native build/device/signing/store و بازبینی مستقل نهایی‌اند. پیشرفت این مرحله نباید به‌جای تکمیل کل محصول گزارش شود.
