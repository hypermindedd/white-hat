# قرارداد API برش مستقل

نسخه alpha؛ این فایل مستند endpointهای واقعی فعلی است. بدنه JSON، خطا شامل error و request_id؛ احراز هویت با session cookie و write با CSRF و origin صریح. API عمومی تجاری/token scopes هنوز طراحی نهایی نشده.

| مسیر | روش | دسترسی و رفتار |
|---|---|---|
| /api/health | GET | سلامت محدود و نیاز به setup؛ بدون داده خصوصی |
| /api/setup | POST | فقط قبل اولین حساب؛ ساخت مدیر با hash رمز؛ race دوباره چک می‌شود |
| /api/login و /api/logout | POST | ورود/خروج و مدیریت session؛ خروج CSRF می‌خواهد |
| /api/me | GET | کاربر، CSRF، هفت Mode و تنظیمات غیرمحرمانه |
| /api/challenges | GET/POST | جستجو/فیلتر داده خود کاربر یا ساخت پیش‌نویس |
| /api/challenges/:id | GET/PATCH/DELETE | فقط مالک؛ ویرایش context قدیمی را invalidate و runها را حفظ می‌کند |
| /api/challenges/:id/mode | POST | تغییر Mode با حفظ context همین تصمیم؛ هنگام running ممنوع |
| /api/challenges/:id/analyze | POST | اجرای مستقیم provider؛ Idempotency-Key لازم؛ وضعیت running و run ID |
| /api/challenges/:id/followups | POST | پیام جدید در همان تصمیم و run تازه؛ idempotency لازم |
| /api/challenges/:id/export | GET | JSON تصمیم/گفتگو/runهای متعلق به کاربر |
| /api/challenges/:id/feedback | POST | بازخورد مفید/نیازمند بازبینی |
| /api/stats | GET | شمارش واقعی تصمیم/مصرف خود کاربر؛ بدون درآمد/هزینه حدسی |
| /api/evaluation-policy | GET | ۱۵ بُعد، وزن‌ها و حدهای canonical |
| /api/admin/settings | GET/PATCH | مدل/کلید در حافظه و سقف؛ کلید هرگز برنمی‌گردد |
| /api/admin/users | GET/POST | فهرست و ساخت عضو/مدیر توسط مدیر workspace |
| /api/admin/users/:id | PATCH | وضعیت فعال؛ آخرین مدیر قابل‌غیرفعال‌کردن نیست؛ session عضو غیرفعال revoke |
| /api/admin/runs و /api/admin/runs/:id | GET | مرور اجراها و خروجی برای ارزیابی؛ مشاهده خروجی audit می‌شود |
| /api/admin/evaluations/:runId | POST | هر ۱۵ نمره و شاهد لازم؛ میانگین و critical pass، بدون release approval |
| /api/admin/audit و /api/admin/health | GET | رویداد/سلامت؛ فقط مدیر |

## قواعد خطا و تکرار

401 یعنی session لازم؛ 403 یعنی role/CSRF/origin رد شده؛ 404 برای داده متعلق به دیگری؛ 409 برای provider تنظیم‌نشده، اجرا در جریان یا idempotency متفاوت؛ 429 برای سقف مصرف. replay همان کلید و همان hash همان run را برمی‌گرداند؛ کلید برای ورودی متفاوت رد می‌شود. خطای provider یا خروجی schema-invalid در run ثبت می‌شود و نتیجه معتبر تولید نمی‌کند.

schema خروجی و واژگان حکم در `apps/second-mind/server/core/engine.mjs` مرجع کدی‌اند. unknown حیاتی با confidence بالا رد می‌شود؛ NEXT ACTION ناقص یا mode/verdict ناسازگار رد می‌شود. این کنترل‌ها کشف همه hallucinationها یا اعتبار حکم را تضمین نمی‌کنند؛ ارزیابی انسانی و benchmark لازم است.

اجرای نیمه‌تمام بعد از restart failed می‌شود. recovery خودکار job و reconciliation مصرف نامطمئن هنوز در گیت عملیات باز است. این مرز باید قبل سرویس تجاری حل شود.
