# شروع ساخت Kotlin و اپ‌های White Hat

**ادامهٔ جاری — checkpoint 07:** [پذیرش مهندسی S2 در reference staging](docs/stages/S2-CLOSURE-FA.md) ثبت شد. کد `b6206383e6af9c39e4ae8c61ec7c6f9970acecc4` و recovery/wh-s2-accepted-reference-20261117 مرجع‌اند. [ادامهٔ صالحه در S3](docs/handoffs/SALEHE-S3-START-20261117-FA.md). تولید و ورود دادهٔ مشتری هنوز پذیرفته نیستند.

این شاخه، معماری، سورس بازیابی‌شده، ماژول قرارداد مشترک و برش اجرایی API کاتلین را دارد. محصول کامل و اپ منتشرشده نیست.

- [انتخاب زبان، پلتفرم، مرزها و مهاجرت](docs/architecture/ADR-010-KOTLIN-PLATFORMS-FA.md)
- [دستور ادامه برای Chief](docs/architecture/CHIEF-HANDOFF-FA.md)
- [سورس مستقل بازیابی‌شده](apps/second-mind/README-FA.md)
- [وضعیت و شواهد این مرحله](evidence/kotlin-foundation/STATUS-FA.md)
- [سرور Kotlin و راه‌اندازی](server/README-FA.md)
- [بستن مرحله API](docs/stages/S1-CLOSURE-FA.md)
- [نام و پرامپت چت بعدی](docs/stages/S2-NEXT-CHAT-FA.md)

پیش‌نیاز foundation: **JDK 21 کامل**، نه فقط JRE؛ Node 24.15+ در خانواده 24 برای runtime alpha. در Windows معادل `gradlew.bat` استفاده شود.

```bash
./gradlew :shared:contracts:jvmTest :shared:contracts:compileKotlinJs :server:api:test :server:adapters:test
```

```bash
cd apps/second-mind
npm test
npm start
```

آزمون‌های AI فعلی fixture هستند؛ کلید AI لازم ندارند و کیفیت تحلیل واقعی را تأیید نمی‌کنند. تست سرور از PostgreSQL واقعی embedded استفاده می‌کند. برای اجرای آلفای بازیابی‌شده راهنمای همان پوشه را بخوان. وب متصل به API کاتلین در `apps/web-bff` است؛ نشست رمزگذاری‌شدهٔ PostgreSQL، تاریخچه و ابزارهای پشتیبانی دارد. کلاینت مشترک در `shared/client` و میزبان‌های Android، iOS و Desktop در `apps` قرار دارند. ساخت، نصب، هویت واقعی و امضای هر مقصد گیت مستقل دارند. وضعیت جاری و محدودیت پذیرش در [دفتر موانع مرحلهٔ دوم](docs/stages/S2-BLOCKERS-FA.md) ثبت شده است.

README اصلی و اسناد انتقال 0.1.0 اسنادی تاریخی‌اند. این راهنما نقطه ورود مرحله جدید است؛ archiveهای تاریخی و متن منابع Notion بازنویسی نشده‌اند.
