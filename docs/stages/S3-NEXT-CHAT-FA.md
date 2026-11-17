# White Hat | ادامهٔ S3

از checkpoint 07 و [دستور صالحه](../handoffs/SALEHE-S3-START-20261117-FA.md) در آخرین HEAD PR #3 شروع کن. [S2 reference](S2-CLOSURE-FA.md) پذیرفته شده؛ کد b6206383e6af9c39e4ae8c61ec7c6f9970acecc4 و recovery/wh-s2-accepted-reference-20261117 مرجع‌اند. تولید/دادهٔ مشتری و مدل واقعی هنوز پذیرفته نیستند.

1. برای C3، baseline و candidate را با نسخهٔ کد، نام و نسخهٔ مدل، workspace مجزا، سقف هزینه، بازبین و مسیر ثبت رسید پین کن.
2. از `evaluation/README-C3.md`، `G001` را در هر دو محیط اجرا کن. خروجی خام، شناسهٔ run، latency، مصرف و هزینهٔ قابل‌تأیید را بررسی کن. سپس اجرای کامل ۶۰ مورد را ثبت کن. پوشش تصویر G056 و شواهد بومی حمل سند یا retrieval در G026/G057 و G032/G033 جدا ردیابی شوند.
3. بستهٔ داوری کور را پس از تطبیق workspace، تصمیم، revision و hash هر رسید تولید کن. خودهویتی مدل و ریسک شکستن کورسازی را به بازبین گزارش کن. با ثبت ۱۵ بُعد، دلیل، شدت و نتیجهٔ injection، scorer نسخه‌دار را اجرا و نتیجهٔ آستانه‌های SM-EVAL-001 را ضمیمه کن.
4. برای C4 ابتدا PR #1، SHA سورس Elyora و آزمون سازگاری transport را ثبت کن. پس از تأیید سازگاری مسیر VoiceTransport، نشست، بودجهٔ مصرف، transcript، barge-in، reconnect و validator را با feature flag و آزمون دستگاه پیش ببر.
5. C5 و C6 مسیرهای پذیرش S4 و S5 هستند. تست واقعی دستگاه، signing/store، checkout و webhook، سیاست مالی، امنیت محیط و GO/NO-GO در گزارش مستقل بمانند.
