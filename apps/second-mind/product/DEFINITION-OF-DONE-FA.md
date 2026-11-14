# تعریف اتمام محصول تجاری مستقل

نسخه 2.0 — دستور جاری مالک در ADR-009. **alpha اجرایی با محصول قابل‌عرضه تجاری یکی نیست.**

| حوزه | شرط اتمام |
|---|---|
| هوشمندی | ۷ Mode متمایز، حکم/اطمینان/NBA معتبر، ۶۰ کیس کامل، threshold پانزده‌بُعدی، رگرسیون و بازبینی انسانی |
| ورودی/شواهد | متن/سند/تصویر/URL/Voice طبق قرارداد فعال‌سازی؛ provenance، محدودیت extraction، injection و freshness واقعی |
| حساب/سازمان | ثبت‌نام، ایمیل، reset/MFA مصوب، role/tenant/session lifecycle و آزمون دسترسی منفی |
| پنل مشتری | onboarding، ساخت/ادامه تصمیم، evidence، history/version، جستجو، خروجی، feedback، پلن/مصرف/صورتحساب |
| پنل مدیر | کاربران/نقش/سازمان، پلن/quota، provider/secrets، policy/config version، feature flags، quality/failure و پشتیبانی |
| تجاری | پلن و entitlement، metering دقیق، پرداخت/webhook/reconciliation، لغو و اقتصاد واقعی |
| امنیت/داده | consent/retention/export/delete، secrets، abuse limits، dependency/security review و incident process |
| عملیات | استقرار، health/alerts، SLO/load، backup/restore و migration/rollback آزموده |
| دسکتاپ | در صورت release مک/ویندوز: installer/update/signing واقعی روی هر معماری هدف؛ launcher به معنی installer نیست |
| ارزش/انتشار | بازگشت برای تصمیم دوم، baseline کور، known defects، راهنمای کاربر/پشتیبانی و تصمیم انتشار نسخه دقیق |

موتور فعلی single model با مراحل منطقی است؛ ۷ Mode به معنی هفت agent نیست. حافظه شناختی بلندمدت، outcome learning، bias profile، چندعاملی و اقدام خارجی خودکار تا تصمیم دامنه مستقل وارد محصول نمی‌شوند. تاریخچه/حساب/سازمان برای ارائه سرویس تجاری با آن قابلیت‌های شناختی یکی نیست.

شرط مشترک: هیچ PASS بدون شواهد، هیچ output/score/قیمت/درآمد ساختگی، هیچ میانگین جبران‌کننده شکست بحرانی، هیچ تغییر Scope ساکت. اگر URL/Voice یا native در یک release فعال نیست، وضعیت و اثر آن روی نام/دامنه همان release باید صریح باشد.
