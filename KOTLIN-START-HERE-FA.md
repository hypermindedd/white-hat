# شروع ساخت Kotlin و اپ‌های White Hat

این شاخه، مرحله معماری و بازیابی سورس و اولین ماژول قابل‌اشتراک است. محصول کامل Kotlin یا اپ منتشرشده نیست.

- [انتخاب زبان، پلتفرم، مرزها و مهاجرت](docs/architecture/ADR-010-KOTLIN-PLATFORMS-FA.md)
- [دستور ادامه برای Chief](docs/architecture/CHIEF-HANDOFF-FA.md)
- [سورس مستقل بازیابی‌شده](apps/second-mind/README-FA.md)
- [وضعیت و شواهد این مرحله](evidence/kotlin-foundation/STATUS-FA.md)

پیش‌نیاز foundation: **JDK 21 کامل**، نه فقط JRE؛ Node 24.15+ در خانواده 24 برای runtime alpha. در Windows معادل `gradlew.bat` استفاده شود.

```bash
./gradlew :shared:contracts:jvmTest :shared:contracts:compileKotlinJs
```

```bash
cd apps/second-mind
npm test
npm start
```

آزمون‌های فعلی fixture هستند؛ کلید AI لازم ندارند و کیفیت تحلیل واقعی را تأیید نمی‌کنند. برای `npm start` مسیر نصب و راه‌اندازی خود alpha را بخوان؛ سرور Kotlin در این مرحله وجود ندارد. کد KMP فعلی JVM و JS را پیکربندی می‌کند؛ Android/iOS و Compose UI در مرحله اپ با toolchain همان هدف افزوده و آزمایش می‌شوند.

README اصلی و اسناد انتقال 0.1.0 اسنادی تاریخی‌اند. این راهنما نقطه ورود مرحله جدید است؛ archiveهای تاریخی و متن منابع Notion بازنویسی نشده‌اند.
