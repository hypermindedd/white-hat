# دستور ادامه White Hat — Kotlin و اپ‌ها

مرجع تصمیم: [ADR-010](ADR-010-KOTLIN-PLATFORMS-FA.md). دستور فعلی مالک مسیر مستقل Kotlin را بر GPTs-first تاریخی مقدم می‌کند. نام دامنه محصول همچنان Second Mind است؛ پروژه امنیت سایبری ساخته نشود.

سورس alpha از بسته مستقل مالک بازیابی و در `apps/second-mind/` حفظ شده؛ این بسته را با آخرین پنل جامع امروز یکی فرض نکن. ابتدا source pin پنل جدید را بازیابی و اختلاف UI را مستند کن؛ missing UI مانع تعریف API نمی‌شود ولی ادغام طراحی نادیده انجام نشود.

۱. `npm test` در alpha و تست‌های shared را اجرا و receipt را بخوان. در Kotlin فعلاً فقط ماژول قرارداد migration وجود دارد؛ backend Kotlin یا اپ ساخته‌شده فرض نکن.
۲. از routeهای واقعی alpha، OpenAPI و fixture request/response/error تهیه کن. قرارداد flat alpha با envelope canonical 1.0 فرق دارد؛ explicit version adapter و unknown preservation لازم است.
۳. Ktor/JVM را با domain مستقل، PostgreSQL migration و access service پیاده کن. shared client فقط contract/input/UI state دارد؛ هسته محرمانه و کلید provider روی server بماند.
۴. ابتدا مسیر ورود → عضویت workspace → ایجاد تصمیم → run idempotent → provider → validate → receipt → نمایش را end-to-end کامل کن. تست مجوز و tenant isolation و نتیجه نامطمئن provider الزامی است.
۵. alpha و Kotlin روی DB زنده دو writer همزمان نداشته باشند. SQLite را مستقیم به عنوان DB چندمستاجری جدید معرفی نکن. rollback بعد از writes جدید نیاز به مسیر انتقال برگشت دارد.
۶. پنل و وب DOM حفظ شوند؛ TypeScript تدریجی. Android/iOS با KMP+Compose و macOS/Windows با Compose Desktop، هر کدام روی دستگاه/OS واقعی آزموده شوند. native installer و PWA را جدا گزارش کن.
۷. طراحی live در شاخه جدا را با منبع و قرارداد آشتی بده؛ voice model جایگزین هسته تحلیل نشود. feature flag پیش‌فرض خاموش تا acceptance.
۸. گیت‌های model benchmark، انسان، دستگاه، امنیت، پشتیبان‌گیری، بار، پرداخت و امضا بازند. structural PASS و fixture PASS هیچ‌کدام مجوز انتشار نیست.

پرامپت آماده ادامه:

«پروژه hypermindedd/white-hat را از شاخه Kotlin foundation ادامه بده. ADR-010، CHIEF-HANDOFF-FA و evidence/kotlin-foundation را بخوان. ابتدا baseline و سورس پنل جدید را احراز کن؛ سپس مرحله ۱: قرارداد OpenAPI و Kotlin/Ktor با PostgreSQL، احراز هویت، tenancy و برش تصمیم تا نتیجه. هفت Mode و source pin را حفظ کن؛ وب موجود را به API متصل کن؛ state writer موازی و تحلیل ساختگی نساز. خروجی را با آزمون، شواهد، محدودیت و rollback در PR ثبت کن.»
