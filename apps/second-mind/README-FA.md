# SECOND MIND — سیستم مستقل v0.3.0-alpha.1

وب‌اپ محلی واقعی با پنل فارسی، سرور و ذخیره‌سازی پایدار. بدون GPT Builder و بدون dependency نصب npm. این نسخه alpha توسعه است؛ محصول تجاری تأییدشده یا installer بومی نیست.

## اجرا روی دستگاه

۱. Node.js **24.15 یا بالاتر از خانواده 24.x** نصب باشد. محیط فعلی با Node 24.22.1 روی Linux آزمایش شده؛ اجرای واقعی مک/ویندوز هنوز تأیید نشده.
۲. پوشه second-mind را باز کنید. در ترمینال همان پوشه:

```bash
npm start
```

۳. آدرس نمایش‌داده‌شده، پیش‌فرض `http://127.0.0.1:4317`، را در مرورگر باز کنید. اولین بار، حساب مدیر را خودتان بسازید؛ رمز پیش‌فرض یا حساب ازپیش‌ساخته وجود ندارد.
۴. از تنظیمات، مدل دارای دسترسی و API key خود را وارد کنید. نام مدل، کلید یا اعتبار مصرف فرض نشده. کلید فقط در حافظه سرور می‌ماند؛ پس از restart دوباره لازم است یا آن را با OPENAI_API_KEY در محیط سرور تنظیم کنید. کلید را در سورس یا چت قرار ندهید.
۵. ابتدا پیش‌نویس بسازید، سپس با اتصال واقعی تحلیل کنید. داده تصمیم در آن اجرای واقعی به ارائه‌دهنده مدل ارسال می‌شود. هیچ پاسخ AI نمایشی در حالت بی‌اتصال ساخته نمی‌شود.

`START-MAC.command` و `START-WINDOWS.cmd` فقط launcher توسعه با Node نصب‌شده‌اند؛ نه DMG/EXE امضاشده. نسخه وب دارای manifest و public-shell service worker است؛ امکان نصب مرورگری به پشتیبانی همان مرورگر وابسته است. API و داده تصمیم offline cache نمی‌شوند؛ تحلیل آفلاین فعال نیست.

## آنچه کار می‌کند

راه‌اندازی اولیه/ورود/خروج؛ session و نقش؛ ساخت و غیرفعال‌سازی عضو توسط مدیر؛ تصمیم و ادامه گفتگو؛ ۷ Mode؛ جستجو/فیلتر تاریخچه؛ export JSON و حذف داده متعلق به کاربر؛ quota و idempotency؛ API مدل، timeout/refusal/incomplete و schema/invariant validation؛ token usage واقعی در صورت دریافت؛ feedback؛ نمره انسانی با شاهد پانزده‌بُعدی؛ health و audit append-only.

## تست

```bash
npm test
```

provider فقط در unit/integration tests با fixture صریح جایگزین می‌شود؛ برنامه کاربر چنین حالت AI جعلی ندارد. موفقیت ۲۳ تست، کیفیت reasoning یا اتصال زنده را تأیید نمی‌کند.

## داده و تنظیمات

پیش‌فرض داده خارج سورس: مک در Application Support/SecondMind؛ ویندوز در APPDATA/SecondMind؛ Linux در .local/share/SecondMind. با SECOND_MIND_DATA_DIR مسیر دلخواه داده تعیین می‌شود. قبل تغییر/upgrade از DB و فایل‌های WAL/SHM به روش سازگار نسخه پشتیبان بگیرید؛ drill رسمی backup/restore هنوز اجرا نشده.

متغیرهای اختیاری: PORT، HOST، APP_ORIGIN، SECOND_MIND_DATA_DIR، OPENAI_API_KEY، SECOND_MIND_MODEL. پیش‌فرض loopback؛ bind غیرمحلی origin صریح لازم دارد. production به HTTPS reverse proxy و APP_ORIGIN امن نیاز دارد؛ این شرط به‌تنهایی production readiness ایجاد نمی‌کند. مدیریت secrets، tenant، queue پایدار، پرداخت، ایمیل/reset/MFA، rate limit سراسری، ممیزی امنیت و load test هنوز در نقشه ساخت‌اند.

## وضعیت ادغام

adapter فعلی مستقیم OpenAI Responses است؛ هیچ کلید واقعی در محیط ساخت موجود نبود و call زنده انجام نشد. فایل/تصویر/URL/Voice و مرور صفحات حفاظت‌شده، installer مک/ویندوز، عملیات و پرداخت در این نسخه کامل نشده‌اند. محدودیت‌ها در اسناد release همراه بسته ثبت شده‌اند.
