# وضعیت واقعی — سیستم مستقل — 2026-11-03

نسخه فعال: **0.3.0-alpha.1**. مسیر GPT قبلی با دستور مالک در [ADR-009](standalone/ADR-009-FA.md) بازنشسته شد.

| بخش | وضعیت واقعی |
|---|---|
| منابع | ۱۷ snapshot تاریخی حفظ‌شده؛ مرجع رفتار و ارزیابی |
| معماری | Core/API/UI جدا؛ یک workspace alpha؛ multi-tenant production هنوز نیست |
| برنامه | وب‌اپ چندفایلی، سرور، DB واقعی، session/role، تصمیم/گفتگو/تاریخچه، export/delete، سهمیه/idempotency |
| پنل مدیر | ساخت/فعال‌سازی کاربر، تنظیم مدل/کلید/حدود، اجراها، audit/health، نمره انسانی با شاهد |
| AI adapter | کد اتصال مستقیم API و validation آماده؛ کلید واقعی موجود نبود؛ live model runs=0 |
| تست برنامه | ۲۳ تست integration/provider/contract با fixture مستقل؛ نتیجه و دامنه در evidence |
| مرورگر | صفحه آغاز/ساخت حساب در مرورگر باز و بصری بررسی شد؛ ورود و صفحات احراز‌شده در مرورگر تکمیل نشده |
| کیفیت reasoning | NOT EVALUATED؛ تست نرم‌افزار یا schema جای benchmark را نمی‌گیرد |
| ۶۰ Golden | کاتالوگ موجود؛ کیس کامل اجرایی و holdout هنوز لازم |
| مک/ویندوز | launcher توسعه دارد؛ installer بومی/امضاشده ساخته یا تست نشده |
| تجاری | tenant/هویت production، پرداخت، backup/restore، SLO و ظرفیت production هنوز باز |
| انتشار | NOT COMMERCIAL-READY؛ GO صادر نشده |

## گیت‌ها

Architecture: مبنای نسخه ثبت؛ Human review باز. Test: کنترل برنامه/adapter؛ AI زنده و E2E احراز‌شده باز. Audit: خودممیزی و limitations؛ تأیید مستقل امنیت نیست. Version: source و app version و hash. Stage Boundary: بسته اجرایی و prompt بعدی. Integration: provider fixture تست شده؛ live نشده. Retroactive Impact: مسیر و اسناد طبق ADR-009 بازنگری شده‌اند.

[گزارش تست](../evidence/standalone/test-report.json) · [قابلیت‌ها/کمبودها](../product/commercial-scope.json) · [برنامه](ROADMAP-FA.md) · [اجرای محلی](../README-FA.md)
