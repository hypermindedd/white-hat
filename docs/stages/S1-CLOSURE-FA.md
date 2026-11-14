# White Hat — پایان برش API کاتلین

مرحله: S1 · تاریخ: 2026-11-11 · API: 0.4.0-alpha.1

این مرحله کدنویسی backend migration slice را می‌بندد. وب‌اپ کامل، ورود عمومی و اپ native هنوز آماده اعلام نشده‌اند. مرجع اندازه‌گیری‌ها [گزارش مرحله](../../evidence/kotlin-api/report.json) است؛ نتیجه CI جداگانه در PR و GitHub Actions دیده می‌شود. اجرای خودکار ثبت‌شده برای commit کد، پیش از هر step شکست خورد و لاگ نداشت؛ دلیل دقیق از اتصال فعلی قابل بازیابی نبود. بنابراین CI تأیید نشده و گیت آن باز است؛ [مشاهده ثبت‌شده](../../evidence/kotlin-api/ci-observation.json).

| بخش | نتیجه قابل‌بررسی |
|---|---|
| معماری کد | domain / application / adapters / api؛ version catalog و generated contracts |
| هویت API | اعتبارسنجی RS256، issuer/audience/expiry/access-kind؛ نقش از عضویت DB |
| داده | PostgreSQL با migration/checksum؛ جداسازی workspace و private ownership |
| تحلیل | هفت Mode، policy pinned، provider adapter و output validator؛ fixture در آزمون |
| اجرای پایدار | idempotency، snapshot/revision، quota تراکنشی، lease و fencing |
| خرابی | cancel، late receipt، عدم retry نامطمئن، recovery بدون provider |
| قرارداد | OpenAPI 3.1 و بررسی هفت پاسخ HTTP واقعی در تست |
| اتوماسیون | workflow تست، receipt و source/schema drift checks؛ بدون deploy |

آزمون نهایی: ۲۸ تست Kotlin، ۲۳ تست Node، PostgreSQL 16.15 واقعی، HTTP واقعی Netty روی loopback و ۷ پاسخ معتبر مطابق OpenAPI. AI و issuer آزمون fixture هستند.

عملکرد روی مدل AI واقعی، IdP واقعی یا production با تست‌های fixture ثابت نمی‌شود. role builder جامع، پنل جدید، BFF/login، migration SQLite و رابط reconciliation هنوز کار مرحله بعدند. جداسازی queryها آزموده شده؛ RLS production پیاده نشده است. تست انسانی و مستقل نهایی باز است.

در بازبینی همین مرحله، بازیابی expired lease در نبود provider و حفظ usage پاسخ دیرهنگام تقویت شد. شکست اولیه ساخت به dependency/import و ambiguity annotation مربوط بود و رفع شد. تست DB ابتدا به Unix socket محیط برخورد کرد؛ TCP loopback استفاده شد. تست Netty نیز با HTTP واقعی روی loopback اجرا می‌شود؛ HTTP client محدودیت زمان دارد و روی event loop مسدود نمی‌شود. خطاهای محیط و مرحله تست، بدون اختراع PASS، در شواهد توضیح داده می‌شوند.

هیچ استقرار، secret یا DB واقعی قبلی تغییر نکرده است. rollback این تغییر در این مرحله revert commit/PR است. پس از اولین writes واقعی، revert کد به‌تنهایی کافی نیست؛ مسیر backup و replay/inverse migration طبق ADR-010 لازم است.

**چت بعدی:** White Hat | مرحله ۲ — اتصال وب و پنل به Kotlin

**محیط:** Work. [پرامپت کامل ادامه](S2-NEXT-CHAT-FA.md).
