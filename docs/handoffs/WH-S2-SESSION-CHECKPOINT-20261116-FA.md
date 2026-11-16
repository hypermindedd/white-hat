# White Hat — نقطهٔ اتمام وظیفه و ادامهٔ صالحه

## نقطهٔ ادامهٔ جاری — WH-S2-AUTH-20261116-05

**نتیجهٔ نهایی این وظیفه:** وب SUCCESS (۱۹ PASS/۰ FAIL/۱ SKIP)، Kotlin/مرورگر واقعی/کلاینت/cutover SUCCESS، موتور کیفیت SUCCESS با GO فقط برای دامنهٔ فعال. log نهایی مستقلاً خوانده شد: `stageCBrowserWithRealKtorAndPostgres PASSED`؛ تست lifecycle کلاینت ابتدا در مجموعهٔ پیش‌فرض SKIP و سپس در اجرای مستقل همان workflow PASSED است؛ cutover سه PASS و صفر SKIP داشت. هیچ ادعای صفر SKIP برای همهٔ workflowها یا پذیرش محصول صادر نشده است.

رسید نسخه‌دار: `evidence/stage-c-review/auth-recovery-20261116.json`. Artifact کاتلین `14266600975` با SHA256 `1c6acc20049c506f589d6e499a9d5713e3e64bbdb528dafc9783ee246aac76cf` و quality `14266295698` با SHA256 `7fdf496c1aa312c69a696450b878a7d4629a103ac249d683d353db7f87cc9749` ثبت شدند. metadata و log بررسی شدند؛ محتوای ZIP/JUnit در این نوبت مستقلاً استخراج نشده و باید در بازبینی صالحه کنترل شود.

این بخش جایگزین دستور ادامهٔ checkpoint 04 در پایین است؛ بخش پایین سابقهٔ ورود بسته است. صالحه از **کد اصلاح‌شدهٔ `8cf12de7b9146189c58ada3dc42d7d0323283df4`** ادامه بدهد، نه از کاندید صرفاً واردشدهٔ d0c5ab9.

درخت نسخهٔ اصلاح‌شده: `7553c7819b161813b4a3ce46915348fc1740065d`.

نقطهٔ بازیابی جدید: `recovery/wh-s2-auth-fixes-20261116` روی همین SHA. نقطهٔ قبلی `recovery/wh-s2-session-20261116` برای بررسی پیش از اصلاح runtime حفظ شده است. شاخهٔ ادامه همان `review/salehe-stage-c-verified-20261116` و PR #3 است.

### ایرادهای رفع‌شده

| ایراد واقعی | علت | اصلاح و شاهد |
|---|---|---|
| رد ورود معتبر پس از چرخش کلید IdP | JWKS پنج دقیقه cache می‌شد و kid تازه هیچ بازخوانی ایجاد نمی‌کرد | refresh محدود برای kid ناشناخته، تجمیع درخواست درحال اجرا، فاصلهٔ ۳۰ ثانیه برای refresh اجباری؛ کلید ناشناخته همچنان رد می‌شود |
| خروج اجباری پس از اختلال یا 429 هنگام تمدید | catch تمام خطاها نشست را حذف می‌کرد | خطای موقت با 503 و حفظ نشست؛ هیچ توکن منقضی به API نمی‌رود؛ رد قطعی refresh و امضای نامعتبر نشست را باطل می‌کنند |
| نمایش صفحهٔ ورود پس از اختلال موقت بارگذاری | boot هر خطایی را خروج از حساب فرض می‌کرد | وضعیت خطای قابل‌دسترسی با دکمهٔ تلاش دوباره؛ فقط 401 به ورود می‌رود |
| نمایش موفقیت خروج با وجود شکست درخواست logout | پاک‌کردن state داخل finally بود | فقط بعد از موفقیت پاسخ سرور، خروج نمایش داده می‌شود؛ شکست امکان تلاش دوباره دارد |

مرز اصلاح: `apps/web-bff/server.mjs`، `public/panel.js`، `test/integration.test.mjs` و `test/e2e/stage-c-browser.mjs`. قرارداد Kotlin/API، هفت Mode، دیتابیس تصمیم، migration و آلفای تاریخی تغییر نکرده‌اند. سیاست retry خودکار عملیات تصمیم یا مدل هزینه‌زا اضافه نشده است.

چهار آزمون هدفمند پیش از اصلاح: سه شکست و یک موفقیت؛ شکست‌های functional شامل چرخش کلید و حفظ نشست در اختلال بودند. پس از اصلاح، هر چهار موفق شدند. کل وب محلی: ۱۲ PASS و دو SKIP ناشی از نبود fixture DB. CI وب روی نسخهٔ جدید و PostgreSQL: **۱۹ PASS، صفر FAIL، یک SKIP** مربوط به تمرین مستقل انتقال؛ log واقعاً خوانده شد. این یک SKIP باید با گردش‌کار Kotlin/cutover پوشش داده شود.

CI وب: https://github.com/hypermindedd/white-hat/actions/runs/43948199800

checkout واقعی PR: `9ecf2f9172cbd6a705a1125b82143ad3481e9206`؛ درخت آن مستقل خوانده شد و با درخت نسخهٔ اصلاح‌شده برابر است. کد پیش از این اصلاح هم CI سبز داشت؛ نتیجهٔ قبلی به این نسخه تعمیم داده نشده است.

رسیدهای نسخهٔ جدید برای تکمیل بازبینی:

- Kotlin، مرورگر واقعی، native client و cutover: https://github.com/hypermindedd/white-hat/actions/runs/43948200345
- موتور کیفیت پین‌شده: https://github.com/hypermindedd/white-hat/actions/runs/43948199765
- Android: https://github.com/hypermindedd/white-hat/actions/runs/43948199754
- iOS: https://github.com/hypermindedd/white-hat/actions/runs/43948199835
- Desktop: https://github.com/hypermindedd/white-hat/actions/runs/43948199827

آزمون مرورگر دو boot failure در `/bff/session` و `/me` و بازیابی با کیبورد، سپس شکست logout و باقی‌ماندن نشست را به harness واقعی Ktor/PostgreSQL اضافه می‌کند. نتیجهٔ نهایی این workflow و کیفیت در کامنت نسخه‌دار checkpoint همین PR/Issue #9 ثبت می‌شود؛ وجود تست معادل اجرای موفق آن نیست. فایل‌های مستندات بعدی با skip-ci ثبت می‌شوند؛ CI به SHA کد بالا تعلق دارد و ادعای CI روی کامیت صرفاً مستندات نیست.

### دستور اجرایی تازه به صالحه

۱. checkpoint 05 و آخرین کامنت PR #3/Issue #9 را بخوان و ACK بده. import بسته و اصلاحات بالا انجام شده‌اند؛ تکرارشان نکن. آخرین head را با `8cf12de...` مقایسه کن؛ تغییر جدید را مستقل بازبینی و تست کن.

۲. نتیجهٔ نهایی CIهای همین نسخه و JUnit/skipها و checksum artifact را ثبت کن. نقص تازهٔ اثبات‌شده را با رگرسیون و اصلاح روی همین معماری برطرف کن؛ گزارش‌کردنِ صرف پایان تسک فنی نیست.

۳. باقی اولویت‌ها: source pin معتبر پنل (Issue #5)، staging TLS/IdP واقعی، session/failover/a11y کامل و سیاست داده/backup/restore با RPO/RTO (Issue #6)، سپس بستهٔ تصمیم Chief (Issue #7). source pin و مصوبات ناموجود را حدس نزن. S2 تا پذیرش C1/C2 همچنان BLOCKED است؛ voice/real-model/device/billing گیت‌های مستقل‌اند.

۴. در پایان هر وظیفهٔ اجرایی، کد و تست و raw evidence، SHA، ref بازیابی و دستور بعدی را در Issue #9 و PR #3 ثبت کن. این سند اتمام بخش اصلاحات احراز هویت را ثبت می‌کند؛ ACK یا اجرای صالحه و پذیرش کل محصول را اثبات نمی‌کند.

بازیابی امن سورس: `git worktree add --detach ../whitehat-auth-recovery 8cf12de7b9146189c58ada3dc42d7d0323283df4` پس از fetch در محیط تمیز. rollback پیش از write واقعی با revert بازبینی‌شدهٔ دو کامیت اصلاح auth/web ممکن است؛ پس از write واقعی همان قواعد ADR-010 لازم است.

پرامپت ادامه: «White Hat را از checkpoint WH-S2-AUTH-20261116-05 و PR #3 ادامه بده. کد اصلاح‌شده 8cf12de7b9146189c58ada3dc42d7d0323283df4 و ref recovery/wh-s2-auth-fixes-20261116 مرجع‌اند. آخرین HEAD و رسیدها را بخوان؛ import و رفع rotation/outage/logout انجام شده‌اند. ایراد جدید را بازتولید، رفع و تست کن؛ سپس source pin پنل، staging و سیاست/restore داده را تکمیل کن. معماری Kotlin/PostgreSQL/BFF واحد بماند. پایان وظیفه با checkpoint و بازیابی و دستور بعدی؛ S2 پیش از sign-off پذیرفته اعلام نشود.»

## سابقهٔ checkpoint 04 — ورود بسته و آزمون نشست

شناسه: `WH-S2-SESSION-20261116-04` · تاریخ: ۱۶ نوامبر ۲۰۲۶ · مسئول ادامه: صالحه، `saleheebrahimi2029-hub` طبق Issue #9.

## مرز اتمام این وظیفه

بازیابی و واردکردن کاندید نشست از درایو به **همان شاخهٔ PR #3** انجام شد. این سند پایان وظیفهٔ انتقال و تثبیت آزمون است؛ پایان S2 یا اجازهٔ انتشار نیست. دستورهای قدیمی «کاندید هنوز در GitHub نیست» از این نقطه تاریخی‌اند. کاندید را دوباره import یا cherry-pick نکن.

| مرجع | شناسهٔ دقیق |
|---|---|
| مخزن | `hypermindedd/white-hat` |
| شاخهٔ ادامه | `review/salehe-stage-c-verified-20261116` |
| PR | https://github.com/hypermindedd/white-hat/pull/3 |
| آخرین کد واردشده | `d0c5ab948dd1fdbd8f74192c25764b5bef422aa0` |
| درخت کد واردشده | `48eb5ae70ec26acf1c108f27f2cd7dff2acc0241` |
| مبنای قبل از این وظیفه | `6cb299bd0ce2feda8b57d9bc4379872625eac7ab` |
| نقطهٔ بازیابی کد | `recovery/wh-s2-session-20261116` روی `d0c5ab948dd1fdbd8f74192c25764b5bef422aa0` |
| کاندید تاریخی داخل بسته | `6b5bf0e137cecc55f3fce96d67484848338a020c`؛ هویت کامیت واردشده متفاوت، درخت برابر است |
| بستهٔ منبع | https://drive.google.com/file/d/1FJZbpTBVijQqxqcXUlxhGLcELY4q_po4/view |
| SHA256 بسته | `5f1c6bb223fc03ada0b362a44eb329f7b9b82c7eecb35340e8175c53305db7c4` |

این ref نقطهٔ بازیابی **سورس** است؛ snapshot پایگاه داده یا نسخهٔ تولید پذیرفته‌شده نیست. فایل‌های راهنمای این تحویل ممکن است در کامیت مستندات بعدی قرار بگیرند؛ نتیجهٔ تست به کد و درخت بالا تعلق دارد.

## کار اجراشده و معماری

- ZIP و سورس تو‌در‌تو از نظر مسیر ناامن و symlink بررسی شدند؛ ۲۷ مدخل SHA256SUMS موفق بود؛ patch روی مبنای واقعی اعمال و درخت Git دوباره محاسبه شد.
- پنج مسیر وارد شدند: `.github/workflows/web-bff.yml`، `KOTLIN-START-HERE-FA.md`، `apps/web-bff/README-FA.md`، `apps/web-bff/test/session-store.test.mjs` و `server/README-FA.md`.
- سرویس PostgreSQL 16 آزمایشی به CI وب افزوده شد. آزمون‌ها مصرف یک‌بارهٔ state، انقضا، هم‌زمانی، ماندگاری، revoke، rotation، تغییر/جابه‌جایی ciphertext، rollback تراکنش و rate limit مشترک را پوشش می‌دهند.
- سه راهنما با کد موجود هماهنگ شدند. کد runtime، قرارداد API، migration، هفت Mode و آلفای بازیابی‌شده در این وظیفه تغییر نکردند.
- authority تصمیم، نقش، workspace و مصرف در Kotlin/PostgreSQL است؛ DB نشست BFF فقط transport احراز هویت را نگه می‌دارد. ADR-010 و ADR-011 مرجع‌اند؛ معماری یا writer جدیدی ایجاد نشده است.

## شواهد و حدود پذیرش

نتایج CI و رسیدهای خام این checkpoint در کامنت نسخه‌دار Issue #9 و PR #3 ثبت می‌شوند. برای وضعیت جاری همان رسید را بخوان، نه وضعیت تاریخی دفتر درایو.

| بررسی مستقل این نوبت | نتیجه |
|---|---|
| source integrity / tree equality | PASS |
| `python3 scripts/verify_docs.py` | PASS؛ ۱۷ منبع و هش |
| سه generator قرارداد و policy | PASS؛ ۴۲ parity fixture، بدون مدل واقعی |
| آلفای Node24، `npm test` | ۲۳ PASS، صفر fail/skip |
| `python3 -B -m unittest discover -s evaluation -p 'test_*.py'` | ۴ PASS |
| `python3 -B -m unittest discover -s scripts -p 'test_*.py'` | ۵ PASS |
| BFF محلی بدون PostgreSQL | ۸ PASS، ۲ SKIP؛ نتیجهٔ DB محسوب نمی‌شود |
| BFF در GitHub با PostgreSQL | ۱۵ PASS، صفر FAIL، یک SKIP انتقال داده؛ log مستقل خوانده شد |
| Gradle محلی | BLOCKED؛ دریافت distribution با خطای Network is unreachable، پیش از اجرای Kotlin |

Node محلی `24.22.1`، JDK `21.0.13`؛ Node آزمون وب CI `24.22.2`؛ داده‌ها و هویت/ارائه‌دهنده مصنوعی‌اند. مرورگر و DB واقعی آزمایشی، معادل IdP واقعی یا کیفیت مدل نیستند.

وب CI: https://github.com/hypermindedd/white-hat/actions/runs/43946849191

شناسهٔ checkout آزمون PR برابر `dac16d40be60b2e77bc93a410a397fec40c3c50c` است؛ درخت آن مستقل خوانده شد و دقیقاً `48eb5ae70ec26acf1c108f27f2cd7dff2acc0241` است. تفاوت merge ref با head پنهان نشده است.

سایر رسیدهای همین کد:

- Kotlin / Chromium / native client / cutover: https://github.com/hypermindedd/white-hat/actions/runs/43946849131
- pinned quality: https://github.com/hypermindedd/white-hat/actions/runs/43946849661
- Android: https://github.com/hypermindedd/white-hat/actions/runs/43946849307
- iOS: https://github.com/hypermindedd/white-hat/actions/runs/43946849172
- Desktop: https://github.com/hypermindedd/white-hat/actions/runs/43946849410

سبزشدن workflow به‌تنهایی گیت S2 را نمی‌بندد؛ SKIP انتقال داده باید با تمرین مستقل Kotlin/cutover پوشش داده شود. آزمون‌های کنارگذاشتهٔ موتور کیفیت، نبود قواعد مرزبندی معماری و نصب/امضای اپ‌ها همچنان جداگانه گزارش شوند.

## دستور بعدی به صالحه — به ترتیب وابستگی

۱. در Issue #9 دریافت این checkpoint را تأیید کن. آخرین head را دوباره بخوان؛ source import بالا تمام شده است. درخت و ancestry تغییرات جدید را با این مرجع مقایسه کن. برای تغییر مستقیم write لازم است؛ اگر دسترسی read باقی است از fork+PR با همین مبنا استفاده کن.

۲. رسید نهایی شش workflow بالا را بخوان و artifactهای Kotlin و quality را تا JUnit، fail/error/skip و SHA256 بررسی کن. CIهای این checkpoint روی درخت مشخص بالا هستند؛ به تغییر بعدی تعمیم نده. اگر FAIL بود: acceptance ID → علت ریشه‌ای → آزمون رگرسیون → اصلاح → CI همان SHA. گیت را ضعیف نکن. نتیجهٔ ۱۲ PASS/۱ FAIL داخل ZIP تاریخی است و با نتیجهٔ تازه جایگزین می‌شود، حذف نمی‌شود.

۳. Issue #5: منبع جامع پنل White Hat را از هرمتیکو با repo/path/commit/hash بگیر و ماتریس feature/role/API و قبل/بعد RTL/a11y ثبت کن. تا source pin معتبر نیست، همین adapter را «آخرین پنل جامع» اعلام نکن و پنل جدید حدسی نساز.

۴. Issue #6: روی staging مصوب، TLS/IdP/JWKS و refresh/replay/CSRF، چند replica و failure/reconnect، RBAC/RLS و private isolation، خطاهای 401/403/404/409/429، keyboard/screen-reader و حالت‌های loading/error را با مرورگر واقعی کامل کن. هم‌زمان مصوبهٔ retention/deletion/consent، نگاشت داده، RPO/RTO و تمرین backup/restore با حفظ write جدید را بگیر. credential و دادهٔ خصوصی وارد مخزن عمومی نشوند.

۵. Issue #7: شواهد C1/C2 و موانع را به Chief برای تصمیم مستقل بده. تا source pin، امنیت/staging و سیاست/بازیابی داده پذیرفته نشده، `S2-BLOCKERS-FA.md` مرجع است؛ S2 closure و S3 acceptance صادر نکن.

۶. Issue #8: native install/upgrade/signing، real-model با بودجه/داوری، donor صوت و billing sandbox را به‌عنوان گیت‌های مستقل دنبال کن. Build با install، fixture با benchmark و برنامه با اجرا برابر نیست.

قالب هر پاسخ: `checkpoint | UTC | owner | branch/full SHA/tree | paths | acceptance IDs | command/exit/toolchain | fixture/real | PASS/FAIL/BLOCKED/NOT RUN | raw evidence/CI/JUnit/hash | root cause | rollback | next instruction`.

## بازیابی و پایان

برای بررسی امن نقطهٔ بازیابی، در worktree تمیز اجرا کن:

```bash
git fetch origin
git worktree add --detach ../whitehat-recovery-wh-s2 d0c5ab948dd1fdbd8f74192c25764b5bef422aa0
git -C ../whitehat-recovery-wh-s2 rev-parse HEAD
git -C ../whitehat-recovery-wh-s2 rev-parse 'HEAD^{tree}'
```

بازگردانی این پنج مسیر پیش از هر write واقعی با revert کنترل‌شده و review ممکن است؛ force-push یا reset شاخهٔ مشترک انجام نشود. پس از write واقعی، stop writes و backup و replay/inverse migration مطابق ADR-010 لازم است؛ بازگردانی کد جای بازگردانی داده نیست.

**پایان این وظیفه:** کاندید ثبت‌شده، ref بازیابی، CI واقعی آغازشده/رسیدها و دستور اجرای نسخه‌دار. **وضعیت محصول: S2 BLOCKED**. ACK صالحه هنوز احراز نشده است.

نام ادامه: **White Hat | S2 — پذیرش پنل، هویت و بازیابی داده**

پرامپت: «از PR #3 و checkpoint WH-S2-SESSION-20261116-04 در docs/handoffs ادامه بده. source import نشست تمام شده؛ دوباره وارد نکن. آخرین head و رسید CI را بخوان، سپس Issue #9 و #5/#6/#7 را به ترتیب وابستگی اجرا کن. Kotlin/PostgreSQL/BFF و هفت Mode موجود مرجع‌اند. source pin پنل، staging identity/a11y/security و سیاست/restore داده را با شاهد دقیق تکمیل کن. پایان هر وظیفه ref بازیابی، checkpoint، raw evidence و دستور بعدی ثبت کن؛ S2 را پیش از پذیرش مستقل نبند.»
