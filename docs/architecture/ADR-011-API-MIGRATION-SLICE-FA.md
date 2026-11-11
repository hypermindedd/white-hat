# ADR-011 — برش اجرایی سرور و مرز مهاجرت

تاریخ 2026-11-11 · والد: ADR-010 · نسخه API: 0.4.0-alpha.1

این مرحله، backend Kotlin را به‌عنوان مقصد مهاجرت پیاده می‌کند؛ runtime قبلی همچنان تنها میزبان پنل alpha است. این وضعیت موقت، دو موتور مستقل با semantics جدید ایجاد نمی‌کند: policy، Mode و schema از سورس قبلی تولید و کنترل drift می‌شوند. هیچ DB زنده مشترکی میان دو writer وجود ندارد.

قرارداد فعلی `/api/v1` و خروجی `legacy-alpha.1` است. façade مسیرهای cookie alpha هنوز ساخته نشده، پس اتصال UI ادعا نمی‌شود. canonical envelope 1.0 نیازمند Decision Object واقعی و mapper آگاه از مجهولات است؛ mapper حدسی برای پرکردن fieldها ساخته نشده است.

مالکیت: `PostgresStore` مسیر همه mutationهاست؛ `RunWorker` claim و completion را هماهنگ می‌کند؛ provider فقط output/receipt می‌دهد؛ validator اجازه نمایش نتیجه می‌دهد. state عوض‌شده یا lease منقضی نمی‌تواند با نتیجه دیرهنگام overwrite شود. schema با Flyway و checksum کنترل می‌شود. audit عملیاتی append-only است؛ این مکانیزم جایگزین WORM یا کنترل DB owner نیست.

مدل دسترسی این برش: subject تک‌issuer + عضویت workspace و چهار نقش OWNER/ADMIN/MEMBER/SUPPORT. این نقش‌ها قالب اجرایی محدود اولیه‌اند؛ permission builder جامع، نقش مالی/ارزیاب، delegation، SSO چند issuer و RLS هنوز پیاده نشده‌اند. منابع private با مالکیت فرد محدودند و admin فقط aggregate مصرف مجاز دارد. workspace مخفی یا منبع فرد دیگر 404 می‌دهد، role نامجاز در workspace خود فرد 403.

resource server با JWT/JWKS پیاده شده است؛ full authentication flow وب و اپ‌ها مرحله بعد است. توکن access نیاز به `token_use=access`، audience و issuer مخصوص API و exp دارد؛ ID token جای access token پذیرفته نمی‌شود. cookie session قدیمی به این API تعمیم داده نمی‌شود.

به‌جای صف جدا، خود جدول runs یک job پایدار با lease و fencing token است. رزرو سهمیه همراه run commit می‌شود. HTTP/AI exactly-once خارجی تضمین نمی‌شود؛ وضعیت نامعلوم به reconciliation می‌رود. در این مرحله retry خودکار provider و resolver عملیاتی مالی نداریم. daily attempt quota در UTC تعریف شده و cancellation آن را آزاد نمی‌کند؛ این مدل صورتحساب تجاری نیست.

PostgreSQL هدف توسعه 16 است؛ test binary نسخه‌دار صرفاً برای آزمون است. انتخاب نسخه patch تولید باید با پشتیبانی فعلی، security gate و restore test تأیید شود. نسخه ۱۴ پیش‌فرض library به‌صورت خودکار مبنای production نمی‌شود. [سیاست نسخه PostgreSQL](https://www.postgresql.org/support/versioning/).

تغییرات این مرحله برای review و آزمون‌اند. هیچ دیتای واقعی، secret یا استقرار قبلی تغییر نکرده است. rollback فعلی revert commit است؛ پس از پذیرش writes واقعی، قواعد ADR-010 و migration معکوس/forward recovery لازم‌اند. schema V1 این مرحله به دیتابیس جدید اختصاص دارد؛ با migration V1 قدیمی SQLite اشتباه گرفته نشود.

مرز پایان مرحله: code compile، نصب توزیع JVM، آزمون API و DB و provider fixture و schema واقعی HTTP response. مرز بعد: بازیابی آخرین پنل، BFF/login و اتصال UI. independent review و پذیرش production همچنان باز هستند.
