# شروع ساخت Kotlin و اپ‌های White Hat

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

آزمون‌های AI فعلی fixture هستند؛ کلید AI لازم ندارند و کیفیت تحلیل واقعی را تأیید نمی‌کنند. تست سرور از PostgreSQL واقعی embedded استفاده می‌کند. برای `npm start` راهنمای خود alpha را بخوان؛ runtime قدیمی هنوز پنل را میزبانی می‌کند و به API جدید متصل نشده است. کد KMP فعلی JVM و JS را پیکربندی می‌کند؛ Android/iOS و Compose UI در مرحله اپ با toolchain همان هدف افزوده و آزمایش می‌شوند.

README اصلی و اسناد انتقال 0.1.0 اسنادی تاریخی‌اند. این راهنما نقطه ورود مرحله جدید است؛ archiveهای تاریخی و متن منابع Notion بازنویسی نشده‌اند.
