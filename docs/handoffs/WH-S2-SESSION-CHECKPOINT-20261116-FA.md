# White Hat — نقطهٔ اتمام وظیفه و ادامهٔ صالحه

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
