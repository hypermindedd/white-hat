# دستور صالحه — ادامه پس از پذیرش مرجع S2

شناسهٔ تحویل: `WH-S2-CLOSURE-20261117-07` · تاریخ: ۱۷ نوامبر ۲۰۲۶.

## نقطهٔ شروع قطعی

- مخزن: `hypermindedd/white-hat`؛ ادامه روی همان `review/salehe-stage-c-verified-20261116` و PR #3. شاخهٔ جدید صرفاً برای تغییر یا fork مجاز است، نه معماری یا runtime موازی.
- کد پذیرفته‌شدهٔ مرجع: `b6206383e6af9c39e4ae8c61ec7c6f9970acecc4`؛ درخت: `f30700605d0d8a0bbfeba2aa5810839124f920db`.
- بازیابی: `recovery/wh-s2-accepted-reference-20261117` روی همان کد. آخرین HEAD ممکن است کامیت مستندات بعد از کد باشد؛ CI به کد آزموده‌شده نسبت داده شود.
- ابتدا [پایان S2](../stages/S2-CLOSURE-FA.md)، [تصمیم مرجع و سیاست داده](../architecture/ADR-012-S2-REFERENCE-BASELINE-FA.md)، [ماتریس قابلیت‌ها](S2-CAPABILITY-MATRIX-FA.md) و [رسید خام بررسی‌شده](../../evidence/s2-acceptance/20261117.json) را بخوان.

**S2 مهندسی در reference staging پذیرفته شد؛ مجوز تولید/ورود دادهٔ مشتری صادر نشده است.** هویت Keycloak و TLS واقعی، حساب و provider تحلیل مصنوعی‌اند. پذیرش دستی screen-reader، استقرار مشتری، مدل واقعی، Voice، native distribution و billing گیت‌های بعدی‌اند. نیازی به تکرار import، بازسازی Kotlin، ساخت BFF جدید یا ورود پنل Elyora نیست.

## اقدامات مستقیم

۱. دریافت checkpoint 07 را در Issue #9 با UTC، HEAD واقعی و مسیر write/fork تأیید کن. Issue #4 قبلاً با بررسی خام شواهد بسته شده است. source pin پنل اکنون تصمیم صریح ADR-012 با manifest و hard gate است؛ منتظر منبع خارجی نامعلوم نمان. اگر طراحی تازه‌ای تحویل شد، delta نسخه‌دار همین UI باشد.

۲. scope مرحلهٔ بعد S3/C3 است: ارزیابی واقعی تصمیم‌یار. `evaluation/README-C3.md` و قراردادهای rubric را بخوان؛ baseline/candidate مدل، سقف هزینه، ارائه‌دهنده، workspace آزمایشی و داور انسانی را مشخص کن. receipt واقعی و خروجی خام و version prompt/engine باید ثبت شوند. کلید در secret manager یا محیط مجاز، نه مخزن یا کامنت.

۳. آماده‌سازی بدون پرداخت:

```bash
python3 scripts/verify_docs.py
python3 scripts/verify_architecture.py
python3 scripts/verify_s2_panel.py
python3 -B -m unittest discover -s evaluation -p 'test_*.py'
python3 evaluation/prepare_s3.py /tmp/whitehat-s3-inputs
```

این فرمان‌ها benchmark مدل واقعی نیستند. بعد از احراز مدل/بودجه و محیط، G001 را برای هر دو baseline/candidate با runner موجود اجرا کن؛ ابتدا هزینه/receipt/latency و صحت ورودی/پاسخ را بررسی، سپس suite شصت‌تایی و داوری کور پانزده‌بُعدی را کامل کن. معیارهای C3 را پایین نیاور. fixture و خودارزیابی مدل جای داور انسانی نیستند.

۴. برای مذاکره، درخواست زیر را از مالک/Chief Elyora پیگیری کن: **بستهٔ خام transport/session صوت** با repo/commit/hash و مجوز reuse؛ event contract شامل turn-final/VAD/barge-in/pause/resume/reconnect/cancel، شناسهٔ turn و sequence، quota/receipt و خطا؛ dependency lock، env template بدون secret، فرمان اجرا/تست و fixture مجاز و شواهد device/latency. این بسته هنوز به White Hat متصل نشده است.

فقط transport قابل reuse است. هفت Mode و reasoning، ownership، مجوز، بودجه، receipt/ledger و state تصمیم در Kotlin واحد White Hat می‌مانند. قواعد آموزشی و اطلاعات دانشجوی Elyora وارد نشوند. تا source pin و آزمون سازگاری نباشد، Voice فقط DESIGN/NOT RUN است؛ engine موازی ساخته نشود.

۵. [طرح تجاری](../commerce/COMMERCIAL-PLANS-FA.md) را بر مبنای هزینه/کیفیت واقعی بعد از C3 تنظیم کن. کاتالوگ Solo/Team/Business/Enterprise پیشنهاد است؛ `plan-preview` صورتحساب یا entitlement فعال نمی‌کند. ledger، webhook معتبر، idempotency، refund و sandbox پرداخت مرحلهٔ مستقل S5 هستند. هیچ مصرف نامحدود، نرخ واقعی یا درآمد تأییدنشده فرض نشود.

۶. پیش از ورود مشتری یا production: دامنه/IdP/secret manager و نسخهٔ production-safe سرویس‌ها، retention/deletion/consent و backup واقعی، RPO/RTO تحت workload، آزمون انسانی assistive technology و security/load/pilot پذیرش مستقل می‌خواهند. سیاست disposable reference را برای دادهٔ مشتری استفاده نکن. native buildها موفق بوده‌اند، اما install/upgrade/signing/notarization هنوز پذیرش جدا دارند.

## قواعد کیفیت و بازیابی هر مرحله

Kotlin اولویت ادامه است؛ DOM/BFF transport و KMP client قرارداد/نمایش‌اند. مالک نوشتن تصمیم و مجوز فقط سرور است. دستورهای CI فعلی روی SHA تازه اجرا شوند؛ موتور کیفیت پین‌شده `67f3693c5ca3593a1a1eb52f3de54aac40be1cb2` و hard gateهای Kotlin architecture و panel-source اجباری‌اند.

هر نقص: requirement → بازتولید → root cause → رگرسیون → اصلاح سیستم موجود → آزمون اثر متقابل و کیفیت. پس از تغییر UI، source pin باید در یک بازبینی صریح به‌روز شود؛ checksum یا معیار برای مخفی‌کردن failure عوض نشود. در آزمون مرجع قبلی کنتراست logout شکست خورد و با shared action styling رفع شد؛ آن گیت پابرجاست.

هر checkpoint در Issue #9 و PR مربوط: `ID | UTC | owner | branch/full SHA/tree | changed paths | acceptance IDs | command/toolchain/exit | fixture/real | PASS/FAIL/BLOCKED/NOT RUN | raw artifacts/hash | risks | rollback | next numbered instruction`.

بازیابی سورس در worktree تمیز:

```bash
git fetch origin
git worktree add --detach ../whitehat-s2-accepted b6206383e6af9c39e4ae8c61ec7c6f9970acecc4
git -C ../whitehat-s2-accepted rev-parse 'HEAD^{tree}'
```

این backup پایگاه داده نیست. بعد از write واقعی، stop writes، snapshot و replay/forward recovery طبق ADR-010 لازم است؛ صرف revert کد کافی نیست. main merge و deployment دو اقدام مستقل‌اند و در این تحویل انجام نشده‌اند.

## نام و پرامپت چت بعدی

**White Hat | S3 — ارزیابی واقعی تصمیم‌یار و آماده‌سازی مذاکره**

«از checkpoint WH-S2-CLOSURE-20261117-07 و فایل SALEHE-S3-START-20261117-FA.md در آخرین HEAD PR #3 ادامه بده. S2 مهندسی reference پذیرفته شده و کد b6206383e6af9c39e4ae8c61ec7c6f9970acecc4 با recovery/wh-s2-accepted-reference-20261117 مرجع است. source import/auth/Kotlin/backend/panel pin/Keycloak TLS/failover/کیفیت را دوباره نساز. ابتدا HEAD تازه و رسیدها را بخوان، سپس S3/C3 را با runner موجود، مدل/بودجه/داور مشخص، G001 و suite60/rubric15 پیش ببر. برای Voice بستهٔ خام نسخه‌دار Elyora را با قرارداد روشن درخواست کن؛ reasoning و دادهٔ White Hat در Kotlin واحد بمانند. commercial preview را paid plan فرض نکن. تولید/دادهٔ مشتری و human AT و native signing/billing هنوز گیت مستقل‌اند. هر مرحله کیفیت، رگرسیون، ref بازیابی و دستور بعدی داشته باشد.»
