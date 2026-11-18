# White Hat — پایان وظیفهٔ Kotlin و ادامهٔ صالحه

شناسه: `WH-S2-KOTLIN-20261117-06` · تاریخ: ۱۷ نوامبر ۲۰۲۶ · مخزن: `hypermindedd/white-hat` · PR #3.

## نقطهٔ ادامه و بازیابی

| مرجع | مقدار |
|---|---|
| شاخهٔ توسعهٔ موجود | `review/salehe-stage-c-verified-20261116` |
| کد آزموده‌شده | `cc101d00dbd63df0a4e7586e01a6c9413d953e71` |
| درخت کد | `08d517b1fc813a3da40446ea2641426639bc9568` |
| checkout واقعی CI | `6f0258a99e622345d9120995abff850bc9278dea`؛ درخت برابر با کد بالا |
| بازیابی این وظیفه | `recovery/wh-s2-kotlin-commercial-20261117` روی همان کد |
| مبنای پیش از این وظیفه | `14f5a411e7902ce51544dbcd01ade057106e9382` |
| بازیابی auth قبلی | `recovery/wh-s2-auth-fixes-20261116` روی `8cf12de7b9146189c58ada3dc42d7d0323283df4` |

کامیت پس از کد بالا، فقط همین اسناد و رسید را تکمیل می‌کند؛ با skip-ci ثبت می‌شود و CI کد به آن کامیت مستندات نسبت داده نمی‌شود. از آخرین HEAD بخوان و تغییرهای بعدی را با کد آزموده‌شده مقایسه کن.

## کار اجراشده

- Kotlin/PostgreSQL: pool و زمان اتصال قابل‌پیکربندی و محدود؛ statement timeout و lock timeout در scope تراکنش؛ بازگردانی write شکست‌خورده؛ حفظ خطای اصلی در شکست rollback؛ کدهای redacted `DATABASE_BUSY` و `DATABASE_UNAVAILABLE` با 503. retry خودکار mutation یا provider اضافه نشده است.
- `GET /api/v1/ready` وضعیت آمادگی دیتابیس را از liveness جدا می‌کند. اطلاعات SQL/اتصال/کلید را افشا نمی‌کند. آزمون outage واقعیِ مرز DataSource و آزمون‌های lock و statement با PostgreSQL اجرا شده‌اند.
- کاتالوگ چهار پلن و اقتصاد واحد با BigInteger در Kotlin؛ API workspace-scoped فقط برای OWNER/ADMIN؛ بدون billing یا تغییر entitlement. [سند تجاری و پیشنهادها](../commerce/COMMERCIAL-PLANS-FA.md).
- قرارداد `contracts/architecture/module-boundaries.json` و کنترل source/build graph؛ گیت اجباری `ARCHITECTURE` در موتور کیفیت موجود افزوده شد. تست‌ها تزریق وابستگی ممنوع، چرخه، writer دوم در BFF و ماژول ثبت‌نشده را رد می‌کنند. این کنترل static است و ادعای اثبات همهٔ رفتارهای reflection/dynamic SQL ندارد.
- OpenAPI، ده پاسخ واقعی نمونه و harness مرورگر با همان API هماهنگ شدند. هفت Mode، ownership تصمیم، migration، صف و هزینهٔ نامطمئن موجود حفظ شدند؛ هیچ backend یا موتور دوم ساخته نشد.

## شواهد مستقل، نه صرفاً وضعیت سبز

[رسید JSON](../../evidence/stage-c-review/kotlin-commercial-20261117.json) شامل هش بایت ZIP، هش هر JUnit، شمارش suiteها، پاسخ‌های بررسی‌شده و علت skipهای کیفیت است.

| اجرا | نتیجه و مرز |
|---|---|
| Kotlin/مرورگر/کلاینت/بازیابی — 43952680368 | SUCCESS؛ ZIP واقعاً دریافت و hash شد؛ ۹ JUnit نهایی: **۶۳ آزمون، ۰ شکست، ۰ خطا، ۰ SKIP** |
| وب/BFF/نشست PostgreSQL — 43952680348 | **۱۹ PASS، ۰ FAIL، ۱ SKIP**؛ مورد انتقال در تمرین مستقل زیر پوشش دارد |
| تمرین مستقل cutover در workflow Kotlin | **۳ PASS، ۰ SKIP**؛ restore پس از committed write و RLS |
| موتور کیفیت — 43952680333 | ZIP مستقل بررسی شد: **۱۴ PASS، ۱۰ SKIPPED**؛ GO برای دامنهٔ پیکربندی‌شده |
| پاسخ‌های قرارداد | ۷ Mode + pilot usage + catalog + estimate، مجموعاً ۱۰ پاسخ واقعی با OpenAPI بررسی شد |
| مرورگر واقعی | ۵ رکورد موفق خام، از جمله پیش‌نمایش تجاری غیرفعال، boot recovery، logout failure، نقش‌ها/private isolation، RTL و کیبورد |
| Android — 43952680355؛ iOS — 43952680387؛ Desktop — 43952680322 | SUCCESS ساخت/بسته؛ نصب، ارتقا، امضای توزیع و پذیرش دستگاه نیست |

Kotlin artifact `14268921831`: `38bb1189e04763503462001436091ba753ec31eb3fb41556897437d82696d1e6`.

Quality artifact `14266694325`: `b5ee38d0cd0ca709e063d18a9d5f1709f6794ca4b328acd87485d2aa248d26f2`.

هویت، provider و داده‌ها مصنوعی‌اند؛ Chromium/Ktor/PostgreSQL فرایند واقعی آزمایشی‌اند. نتیجهٔ ۶۳ آزمون به XMLهای نهایی تعلق دارد؛ سطرهای تکراری log دوباره شمرده نشده‌اند. lifecycle کلاینت در اجرای پیش‌فرض SKIP و سپس در اجرای مستقل PASS شده و XML نهایی جایگزین شده است. صفر SKIP کل پروژه ادعا نشده است.

ده skip موتور: mirrors، pytest، Python dependency audit، Semgrep، API، web، UI، Android، iOS و traceability داخلی موتور. تست‌های مستقل مرورگر/native جای این skipها را در گزارش موتور «PASS» نمی‌کنند. کنترل built-in Python همچنان صفر rule دارد؛ قرارداد Kotlin از command اجباری جدا اجرا می‌شود.

## تصمیم پایان وظیفه و وضعیت S2

**این وظیفهٔ فنی تمام و شواهدش بررسی شد. S2 کامل همچنان BLOCKED است.** مصوبات یا محیط واقعیِ ناموجود، با تست مصنوعی جایگزین نمی‌شوند. محل فعلی هر مانع در [S2-BLOCKERS-FA.md](../stages/S2-BLOCKERS-FA.md) است.

صالحه در Issue #9 دریافت checkpoint قبلی را با حساب `saleheebrahimi2029-hub` تأیید کرده و لاگ همین کد را در Issue #4 مستقل بازبینی کرده است. بررسی ZIP معوق قبلی اکنون در این checkpoint انجام شد؛ دریافت checkpoint 06 هنوز باید جدا ACK شود. دسترسی Drive او قبلاً مسدود گزارش شده است؛ این دستور و شواهد GitHub خودبسنده‌اند و ادامهٔ توسعهٔ کد نباید منتظر دسترسی Drive بماند. دسترسی نوشتن او تأیید نشده؛ در صورت read از fork+PR همین مبنا ادامه دهد.

## دستورهای بعدی صالحه — به ترتیب وابستگی

۱. از همین SHA و ref بازیابی شروع کن؛ source import، اصلاح auth و تغییرات Kotlin بالا را دوباره نساز. Issue #9 را با ACK این checkpoint، SHA و مسیر write/fork به‌روز کن. رسید JSON و raw artifacts روی دو شناسهٔ بالا مرجع‌اند؛ اگر دوباره بازبینی کردی با شمارش نهایی، نه تعداد سطرهای تکراری log، گزارش بده.

۲. **P0 / Issue #5:** هرمتیکو منبع آخرین پنل جامع White Hat را با repo/path/commit/blob hash و مجوز استفاده مشخص کند؛ صالحه آن را با adapter موجود و role/feature/API mapping تطبیق دهد. تا منبع احراز نشده، پنل جدید حدسی یا پنل Elyora جای آن قرار نگیرد.

۳. **P0 / Issue #6:** از Chief/مالک، محیط TLS و IdP مصوب و حساب‌های آزمایشی مجاز و مسیر secret manager را دریافت کن؛ secret در Issue/Drive عمومی نوشته نشود. سپس refresh/JWKS/replay/CSRF، active revoke و multi-replica/failover، خطاهای 401–429/503، screen reader/RTL/keyboard و reconnect را روی همان نسخهٔ مقصد اجرا و evidence ثبت کن. timeoutهای جدید را با بار و connection budget متناسب همان محیط تنظیم کن؛ ادعای ظرفیت تولید بدون اندازه‌گیری نکن.

۴. **P0 / Issue #6 و #7:** نگاشت رسمی داده، quarantine، retention/deletion/consent، دسترسی audit/مالی، RPO/RTO و حفظ writeهای بعد از cutover را تصویب و روی کپی مجاز تمرین کن. فقط یک writer کسب‌وکار فعال باشد. سپس Chief پذیرش مستقل C1/C2 را روی SHA و محیط مشخص ثبت کند. پیش از این تصمیم، S2-CLOSURE تاریخی مجوز پذیرش جدید نیست.

۵. **تجاری / Issue #8:** پس از پذیرش S2، هزینه/کیفیت واقعی و سیاست قیمت و ارز را تثبیت کن؛ سپس paid entitlement و payment sandbox با audit/idempotency/refund. طرح‌های پیشنهادی همین کد را به اشتراک فروخته‌شده تبدیل نکن. پیشنهادهای IDEA-01 تا IDEA-05 در سند تجاری فقط backlog هستند.

۶. هر نقص جدید: requirement → بازتولید → root cause → رگرسیون → اصلاح همان سیستم → موتور کیفیت پین‌شده + CI و آزمون تأثیر بر بخش‌های مرتبط. گیت‌ها را برای سبزشدن پایین نیاور. پایان هر وظیفه باید SHA، ref بازیابی، raw evidence، blocker/owner و دستور بعدی داشته باشد.

## درخواست شفاف انجین خام مذاکرهٔ هم‌زمان Elyora

**این انجین هنوز دریافت یا متصل نشده و مانع بستن S2 نیست.** برای مرحلهٔ مستقل مذاکره، از مالک/Chief Elyora این بسته درخواست می‌شود:

- سورس خام transport/session صوت، adapter ارائه‌دهنده و قرارداد event، با repo/commit/hash و محدودهٔ مجاز reuse؛ source minified یا تصویر رابط کافی نیست.
- قراردادهای VAD/turn-final/interruption/barge-in/pause/resume/reconnect/cancel، ترتیب و شناسهٔ turn، خطا، quota و receipt مصرف؛ وضعیت usage نامعلوم باید قابل‌نمایش باشد.
- entry point و dependency lock، `.env.example` بدون secret، فرمان اجرا و تست، fixture صوت/متن با رضایت و مجوز، و روش kill-switch و پاک‌سازی session.
- شواهد latency روی دستگاه/میکروفون و محدودیت‌های شناخته‌شده؛ هیچ قیمت یا performance فرضی به White Hat منتقل نشود.

فقط transport قابل reuse است. هفت Mode، reasoning، مجوز، state تصمیم، بودجه و ledger White Hat در Kotlin واحد باقی می‌مانند. شخصیت، قواعد آموزشی و دادهٔ دانشجوی Elyora وارد محصول نمی‌شوند. سازگاری protocol، امنیت و real-provider/device acceptance باید در گیت مستقل آزموده شود.

## بازیابی و rollback

```bash
git fetch origin
git worktree add --detach ../whitehat-cp06 cc101d00dbd63df0a4e7586e01a6c9413d953e71
git -C ../whitehat-cp06 rev-parse 'HEAD^{tree}'
```

درخت باید `08d517b1fc813a3da40446ea2641426639bc9568` باشد. این ref بازیابی سورس است، نه backup DB. این وظیفه migration جدید ندارد. پیش از write واقعی، rollback کد با revert بازبینی‌شدهٔ cc101d0 و بررسی قرارداد/consumer ممکن است؛ force-push شاخهٔ مشترک ممنوع. پس از write واقعی، توقف writer، snapshot و replay/forward restore طبق ADR-010 لازم است.

## نام و پرامپت ادامه

نام: **White Hat | S2 — پنل معتبر و پذیرش محیط واقعی**

پرامپت: «از checkpoint WH-S2-KOTLIN-20261117-06 و PR #3 ادامه بده. کد آزموده‌شده cc101d00dbd63df0a4e7586e01a6c9413d953e71 و recovery/wh-s2-kotlin-commercial-20261117 مرجع‌اند. آخرین HEAD را دوباره بخوان؛ import/auth/Kotlin timeout/plan preview/architecture gate اجرا شده‌اند. ZIPهای CI مستقل تأیید شده‌اند: ۶۳ JUnit با صفر خطا/شکست/skip و quality ۱۴ PASS/۱۰ SKIP. source pin پنل در #5، staging و سیاست/restore داده در #6 و sign-off Chief در #7 باقی‌اند. آن‌ها را با شواهد واقعی ببند؛ اگر پیش‌نیاز نیست blocker مشخص کن. Kotlin و PostgreSQL authority واحد بمانند؛ گیت کیفیت و تست تأثیر متقابل هر مرحله لازم‌اند. دستورهای تجاری و درخواست خام صوت در همین تحویل هستند. پایان وظیفه با checkpoint/ref بازیابی/دستور بعدی؛ S2 بدون پذیرش مستقل بسته اعلام نشود.»
