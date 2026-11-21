# استقرار آزمایشی وایت‌هت در Railway

پروژهٔ خصوصی `white-hat-voice-pilot` و محیط `staging` را باز کن. PostgreSQL دارای دیسک پایدار است و باید در شبکهٔ خصوصی باقی بماند. سرویس `whitehat-web` با Railpack، Gradle و npm از همین مخزن ساخته می‌شود. Kotlin روی loopback و BFF روی پورت Railway اجرا می‌شوند. یک replica برای پایلوت کافی است. برای دسترسی از مرورگر، دامنهٔ HTTPS فقط روی BFF و ارائه‌دهندهٔ هویت تنظیم می‌شود.

## اتصال و ترتیب راه‌اندازی

1. Railway App را با گزینهٔ **Only select repositories** فقط به `hypermindedd/white-hat` متصل کن. نسخهٔ بررسی‌شدهٔ شاخهٔ استقرار را با SHA ثابت انتخاب کن. متغیرها را پیش از راه‌اندازی نسخه ثبت کن. در تنظیمات سرویس، مسیر بررسی سلامت را روی `/health` با مهلت ۱۲۰ ثانیه و سیاست restart را روی `ON_FAILURE` با سه بار تلاش بگذار. Builder روی `RAILPACK` باشد. فرمان build و start از `railpack.json` در ریشه خوانده می‌شود.
2. در PostgreSQL آزمایشی، دیتابیس API، نشست‌ها و Keycloak را جدا کن. برای API و نشست‌ها نقش‌های runtime مستقل، بدون مالکیت database/schema، بدون `SUPERUSER` یا `BYPASSRLS` و با محدودیت CONNECT به دیتابیس مقصد بساز. با نقش migration روی دیتابیس API، `bash build/railway/api/bin/api --migrate` را در یک فرایند موقت داخل شبکهٔ محیط اجرا کن. فهرست migrationهای اعمال‌شده باید به V12 برسد. برای runtime کاتلین، مجوز کاربردی جداول و SELECT روی `flyway_schema_history` لازم است. مجوز نوشتن تاریخچهٔ Flyway را به runtime نده. با مالک دیتابیس نشست، `build/railway/web/db/session-store.sql` را اعمال کن و به runtime نشست فقط مجوز خواندن و نوشتن سه جدول تعریف‌شده در همان فایل بده. نقش و رمز migration فقط در فرایند موقت باشند. چون قالب Railway از PostgreSQL 18 استفاده می‌کند، روی همین نسخه مهاجرت و RLS را با دادهٔ ساختگی تأیید کن.
3. از یک ارائه‌دهندهٔ OIDC آزمایشی موجود استفاده کن. برای Keycloak، در صورت نیاز از توزیع رسمی ZIP/TAR در محیط پذیرندهٔ JVM و حالت `start` با PostgreSQL اختصاصی، آدرس HTTPS ثابت و پراکسی `xforwarded` استفاده کن. Realm `whitehat-pilot` پذیرش ثبت‌نام عمومی نداشته باشد. کلاینت عمومی `whitehat-web` با Authorization Code، PKCE اجباری S256 و redirect دقیق `https://<web-domain>/auth/callback` بساز. Direct Access Grants و Implicit Flow خاموش باشند. روی access token، mapper با Custom Audience برابر `whitehat-api` و claim `token_use=access` تنظیم کن. یک حساب ساختگی برای مالک و یک حساب جدا برای آزمون جداسازی بساز.
4. متغیرهای runtime را در `whitehat-web` تنظیم کن. از ارجاع خصوصی Railway برای میزبان پایگاه داده استفاده کن و رمزها را در متغیرهای محافظت‌شدهٔ همان محیط نگه دار.

| بخش | متغیر | مقدار |
| --- | --- | --- |
| API | `WH_DATABASE_URL` | JDBC روی میزبان خصوصی، دیتابیس API |
| API | `WH_DATABASE_USER`, `WH_DATABASE_PASSWORD` | نقش محدود API و رمز همان محیط |
| API | `WH_AUTH_ISSUER` | `https://<idp-domain>/realms/whitehat-pilot` |
| API | `WH_AUTH_JWKS_URL` | issuer به‌علاوهٔ `/protocol/openid-connect/certs` |
| API | `WH_AUTH_AUDIENCE` | `whitehat-api` |
| وب | `WH_WEB_ORIGIN` | `https://<web-domain>` |
| وب | `WH_OIDC_ISSUER`, `WH_OIDC_CLIENT_ID` | همان issuer و `whitehat-web` |
| وب | `WH_WEB_SESSION_DATABASE_URL` | URL خصوصی دیتابیس نشست با نقش BFF |
| وب | `WH_WEB_SESSION_KEYS` | کلید تصادفی ۳۲ بایتی، Base64 |
| آزمایش | `WH_VOICE_PILOT`, `WH_G001_PILOT` | ابتدا هر دو صفر |

## پذیرش و بازگشت

- از deployment سالم و `/health` شروع کن. از مرورگر ورود PKCE، هفت Mode، ماندن نشست پس از بارگذاری مجدد، خروج، رد درخواست غیرمجاز و جداسازی دو workspace را بررسی کن. برای Railway، شناسهٔ deployment و SHA را کنار رسید QA ثبت کن.
- بعد از قبولی پایه، مراحل [پایلوت صدا و داوری](VOICE-PILOT-STAGING-FA.md) را با workspace آزمایشی، کلید اختصاصی و سقف مصرف دنبال کن. این چیدمان ورودی میکروفن و تأیید متن را می‌آزماید. مذاکرهٔ صوتی دوطرفه قابلیت جداگانه‌ای است.
- در صورت خطای صدا یا داوری، flag مربوط را خاموش کن و سرویس را دوباره مستقر کن. رکوردهای شمارش و سقف مصرف را نگه دار. پیش از بازگشت به SHA قبلی، سازگاری آن با نسخهٔ اعمال‌شدهٔ Flyway را بررسی کن.
