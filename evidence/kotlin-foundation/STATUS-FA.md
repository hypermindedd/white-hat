# وضعیت مرحله معماری Kotlin — 2026-11-11

**مرحله foundation آزمایش شد؛ محصول کامل، اپ بومی و انتشار هنوز انجام نشده‌اند.**

| بررسی | شاهد واقعی |
|---|---|
| بازیابی source | ۲۱ فایل با hashهای manifest تطبیق دارند؛ UI بازیابی‌شده تغییر نکرده |
| runtime قبلی | ۲۳ آزمون Node موفق؛ providerها fixture هستند |
| Kotlin/JVM | ۸ آزمون موفق؛ یکی شامل تطبیق پذیرش ۴۲ نمونه با validator قبلی |
| Kotlin/JS | compile موفق؛ آزمون اجرا در JS انجام نشده |
| منابع Notion | ۱۷ شناسه و hash منبع محفوظ؛ بررسی اسناد مستقل از پذیرش runtime |

این مرحله هفت Mode و قرارداد flat alpha را حفظ می‌کند. schema canonical 1.0 هنوز به OpenAPI و mapper اجرایی تبدیل نشده است. اعتبارسنجی output، ارزیابی کیفیت AI یا کشف همه ادعاهای ساختگی نیست.

شواهد: [گزارش ماشینی](report.json)، [تست alpha](node-test.log)، [ساخت Kotlin](kotlin-build.log)، [نمونه‌های تطبیق](legacy-parity-cases.json). XMLهای JVM در همین پوشه‌اند. برای تکرار: `node scripts/generate_legacy_parity.mjs` سپس فرمان‌های [راهنمای شروع](../../KOTLIN-START-HERE-FA.md).

مانع محیط رفع شد: Java اولیه فقط JRE بود؛ JDK کامل با checksum معتبر تهیه شد. تنظیم proxy و truststore فقط برای process محلی اعمال شد؛ TLS غیرفعال نشده و تنظیم محیط خصوصی وارد سورس پروژه نشده است.

گیت‌های باز: بازیابی آخرین پنل جامع، Ktor و PostgreSQL، قرارداد API، هویت و tenancy، مدل/صوت واقعی، آزمون‌های بومی و انسانی، امنیت/بار/backup، پرداخت و امضا/فروشگاه. بازبینی مستقل نهایی انجام نشده است. مرحله بعد: [دستور Chief](../../docs/architecture/CHIEF-HANDOFF-FA.md).

بازگشت این PR: عدم merge یا revert commit. هیچ DB زنده، استقرار یا محیط قبلی تغییر نکرده است. rollback آینده بعد از نوشتن داده جدید باید طبق ADR-010 انجام شود.
