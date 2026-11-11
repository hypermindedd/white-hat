# معماری سیستم مستقل — v0.3.0-alpha.1

تصمیم فعال در ADR-009: Web-first مستقل؛ API و Core قابل‌استفاده برای مک/ویندوز. این معماری، وابستگی به ساخت Custom GPT ندارد.

| لایه | پیاده‌سازی فعلی | مرز توسعه تجاری |
|---|---|---|
| UI | پنل فارسی RTL، ورودی/تصمیم/تاریخچه، مدیریت، تنظیمات، کیفیت و سلامت | E2E کامل، accessibility، onboarding، سازمان/پلن/صورتحساب |
| Application API | Node HTTP، قرارداد request/response، session/csrf/role و مالکیت | API versioning نهایی، public auth، tenant و rate limit سراسری |
| Core | ontology/policy متن نسخه‌دار، هفت Mode، schema و validator | ۶۰ کیس API، ارزیابی انسانی، multimodal/evidence adapters |
| Model adapter | OpenAI Responses با strict JSON، timeout، refusal/incomplete handling | providerهای دیگر با parity؛ live integration هنوز انجام نشده |
| Storage | SQLite در پوشه مستقل کاربر؛ users/sessions/challenges/messages/runs/evaluations/audit | PostgreSQL/صف پایدار در استقرار چندکاربره بزرگ؛ migration نسخه‌دار |
| Operations | health، خطا، usage، core hash و audit append-only | tracing/alerts، secrets manager، backup/restore و rollout |
| Desktop | launcher توسعه برای اجرای وب‌اپ روی دستگاه دارای Node | shell بومی، نصب‌کننده امضاشده، keychain/update و تست روی OS واقعی |

## مرزهای داده و اعتماد

هر Challenge مالک user_id دارد؛ عضو عادی داده عضو دیگر را نمی‌بیند. مدیر workspace به خروجی اجرا برای ارزیابی دسترسی دارد و مشاهده آن audit می‌شود. این مدل هنوز tenant سازمانی نیست. session token فقط به‌صورت hash در DB؛ رمز با scrypt؛ cookie HttpOnly/SameSite؛ write به origin و CSRF معتبر نیاز دارد. این کنترل‌های alpha گواه امنیت production نیستند.

کلید API در مرورگر یا DB ذخیره نمی‌شود؛ فقط محیط/حافظه سرور. نام مدل/سقف مصرف در DB نگه‌داری می‌شود. production به vault/KMS و rotation/ACL نیاز دارد. داده درخواست هنگام اجرای واقعی به provider انتخابی ارسال می‌شود؛ policy داده و consent تجاری در S03/S06 تکمیل می‌شوند.

هر run نسخه Core، hash، مدل، شناسه پاسخ و usage واقعی را نگه می‌دارد. idempotency با کلید و hash درخواست مانع اجرای تکراری است. درخواست ناموفقِ ارسال‌شده سهمیه تلاش را مصرف می‌کند؛ هزینه ساختگی به کاربر نشان داده نمی‌شود. restart اجرای نیمه‌تمام را failed می‌کند؛ queue پایدار/حسابداری مصرف نامطمئن برای production باز است.

## تصمیم و گفتگو

کاربر → input validation → مالکیت/سهمیه/idempotency → Core/Mode → provider → schema و invariant validation → ثبت نتیجه/usage → renderer. خطا/timeout/refusal خروجی معتبرنما ایجاد نمی‌کند. پیگیری همان تصمیم، history محدود همان گفتگو را به Core می‌دهد. ویرایش متن ورودی، context گفتگو را پاک و runهای گذشته را برای trace حفظ می‌کند. تصمیم مستقل، context مستقل دارد.

## استقرار و مقیاس

alpha پیش‌فرض روی loopback است؛ بدون وابستگی نصب npm. Node 24.15+ لازم است و در محیط جاری Node 24.22.1 آزمایش شد. SQLite همگام برای این برش کوچک انتخاب شده؛ برای ظرفیت تجاری و چندسرور، DB/queue/usage transaction در S06 دوباره طراحی و سنجیده می‌شود. microservice یا ظرفیت میلیون‌کاربر ادعا نشده. مدیریت پلن/حدود/زبان بدون تغییر کد هدف محصول است؛ تغییر semantics/کیفیت همیشه نسخه و رگرسیون می‌خواهد.

مراجع فنی بررسی‌شده در 2026-11-03: [Node SQLite v24](https://nodejs.org/download/release/latest-v24.x/docs/api/sqlite.html) و [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses). docs مدل/availability فعلی حساب یا موفقیت اتصال این برنامه را ثابت نمی‌کنند.
