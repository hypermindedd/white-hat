# پرامپت آماده ادامه — موتور مستقل و تکمیل اتصال

```text
پروژه SECOND MIND در ریپوی white-hat را از نسخه مستقل 0.3.0-alpha.1 ادامه بده. GPT Builder نمی‌سازیم. ابتدا docs/standalone/ADR-009-FA.md، STATUS/GAPS/ROADMAP، product/commercial-scope.json و apps/second-mind/README-FA.md را بخوان.
برش فعلی: وب‌اپ Node 24.x + SQLite + پنل فارسی؛ users/sessions/roles، مالکیت داده، تصمیم/گفتگو/تاریخچه، export/delete، quota/idempotency، provider مستقیم، schema validation، ارزیابی انسانی و audit. ۲۳ تست نرم‌افزاری با provider fixture پاس شده‌اند؛ اجرای واقعی مدل صفر است و کیفیت reasoning تأیید نشده. فایل‌های legacy GPT پیش‌نیاز یا Runtime فعال نیستند.
هدف مرحله بعد: اتصال مجاز API با مدل واقعی، ثبت engine/model/config و خروجی دست‌نخورده؛ اجرای مستقیم ۱۵ Smoke و تکمیل نخستین batch Golden؛ آزمون صفحات احراز‌شده در مرورگر توسط حساب کاربر. کلید را در چت/کد/لاگ نخواه و ثبت نکن؛ تنظیمات سرور یا محیط امن. پاسخ fixture را نتیجه واقعی جا نزن. بعد فایل/تصویر/URL/Voice را با provenance و کنترل injection تکمیل کن.
تست node --test tests/*.test.mjs از apps/second-mind و verify_standalone را اجرا کن. محدودیت‌ها: alpha یک workspace؛ native installer/پرداخت/هویت production/tenant/backup-restore/امنیت تجاری تکمیل نشده. سهمیه/role/CSRF/idempotency و confidence ceiling حفظ شوند. هر اصلاح، علت ریشه‌ای و رگرسیون داشته باشد. معیارهای پانزده‌بُعدی/۶۰ کیس بدون تغییر حفظ شوند؛ G01/G10 را ساکت نبند.
برای مک/ویندوز از همان Core/API و UI استفاده کن؛ launcher را installer امضاشده اعلام نکن. نصب روی OSهای واقعی شواهد مستقل می‌خواهد. مرحله را با نسخه، evidence، known gaps، rollback و prompt بعدی در چت جدا ببند. انتقال به GitHub تنها پس از احراز مجوز صریح همین بسته/مقصد انجام شود؛ رد قبلی را با مسیر دیگر دور نزن.
```
