const start = document.querySelector('#voice-start');
const stop = document.querySelector('#voice-stop');
const cancel = document.querySelector('#voice-cancel');
const status = document.querySelector('#voice-status');
const transcript = document.querySelector('#voice-text');
const confirm = document.querySelector('#voice-confirm');
let previewPath;

const errors = {
  AUTH_REQUIRED: 'اول وارد حساب آزمایشی شو و دوباره این صفحه رو باز کن.',
  VOICE_PILOT_UNAVAILABLE: 'پایلوت برای هیچ فضای کاریِ متعلق به این حساب فعال نیست.',
  VOICE_PILOT_EXHAUSTED: 'سهمیهٔ سه تلاش این فضای آزمایشی تمام شده.',
  VOICE_PREVIEW_UNAVAILABLE: 'مهلت تأیید متن تمام شد یا این ضبط قابل تأیید نیست.',
  VOICE_AUDIO_TOO_SHORT: 'زمان ضبط خیلی کوتاه بود.',
  VOICE_BACKPRESSURE: 'سرعت اتصال برای ارسال صوت کافی نبود.',
  NotAllowedError: 'مرورگر اجازهٔ استفاده از میکروفن رو نداده.',
  NotFoundError: 'میکروفن پیدا نشد.',
};

let csrf;
let path;
let active;

async function api(route, body) {
  const options = body === undefined ? {} : {
    method: 'POST',
    headers: {'Content-Type': 'application/json', 'X-CSRF-Token': csrf},
    body: JSON.stringify(body),
  };
  const response = await fetch(route, {...options, signal: AbortSignal.timeout(17000)});
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || 'VOICE_FAILED');
  return data;
}

function changes(busy, recording = false) {
  start.disabled = busy;
  stop.disabled = !recording;
  cancel.disabled = !recording;
}

function message(error) {
  return errors[error.message] ?? errors[error.name] ?? 'ضبط کامل نشد. پیام خطا و دسترسی پایلوت رو بررسی کن.';
}

function stopMicrophone(session) {
  session.capture?.disconnect();
  session.source?.disconnect();
  session.stream?.getTracks().forEach(track => track.stop());
  void session.context?.close().catch(() => {});
}

async function end(abort, failure) {
  const session = active;
  if (!session || session.ending) return;
  session.ending = true;
  changes(true);
  clearTimeout(session.timer);
  status.textContent = abort ? 'لغو ضبط…' : 'دریافت متن نهایی…';
  try {
    if (!abort) {
      session.capture.port.postMessage('flush');
      await Promise.race([
        session.flushed,
        new Promise((_, reject) => setTimeout(() => reject(new Error('VOICE_FLUSH_TIMEOUT')), 800)),
      ]);
    }
    stopMicrophone(session);
    if (abort) {
      await api(session.path + '/cancel', {});
      status.textContent = failure ? message(failure) : 'ضبط لغو شد.';
    } else {
      await session.queue;
      transcript.value = (await api(session.path + '/finish', {})).transcript;
      previewPath = session.path + '/decision';
      confirm.disabled = false;
      status.textContent = 'متن آمادهٔ بازبینی است.';
    }
  } catch (error) {
    stopMicrophone(session);
    void api(session.path + '/cancel', {}).catch(() => {});
    status.textContent = message(failure ?? error);
  } finally {
    active = undefined;
    changes(false);
  }
}

start.onclick = async () => {
  changes(true);
  transcript.value = '';
  previewPath = undefined;
  confirm.disabled = true;
  status.textContent = 'در حال فعال‌کردن میکروفن…';
  const session = {queue: Promise.resolve(), queued: 0};
  try {
    session.context = new AudioContext({sampleRate: 24000});
    if (session.context.sampleRate !== 24000) throw new Error('VOICE_SAMPLE_RATE_UNAVAILABLE');
    await session.context.resume();
    session.stream = await navigator.mediaDevices.getUserMedia({audio: {channelCount: 1, echoCancellation: true}});
    await session.context.audioWorklet.addModule('/voice-pcm.mjs');
    session.source = session.context.createMediaStreamSource(session.stream);
    session.capture = new AudioWorkletNode(session.context, 'whitehat-pcm', {channelCount: 1});
    const lease = await api(path, {});
    session.path = path + '/' + lease.lease_id;
    let resolveFlush;
    session.flushed = new Promise(resolve => { resolveFlush = resolve; });
    session.capture.port.onmessage = ({data}) => {
      if (data === null) { resolveFlush(); return; }
      if (++session.queued > 5) {
        void end(true, new Error('VOICE_BACKPRESSURE'));
        return;
      }
      const pcm = btoa(String.fromCharCode(...new Uint8Array(data)));
      session.queue = session.queue.then(() => api(session.path + '/chunk', {pcm}))
        .finally(() => { session.queued--; });
      void session.queue.catch(error => end(true, error));
    };
    active = session;
    session.source.connect(session.capture);
    session.capture.connect(session.context.destination);
    session.timer = setTimeout(() => { void end(false); }, 14000);
    status.textContent = 'در حال ارسال زندهٔ صوت…';
    changes(true, true);
  } catch (error) {
    stopMicrophone(session);
    if (session.path) void api(session.path + '/cancel', {}).catch(() => {});
    active = undefined;
    status.textContent = message(error);
    changes(false);
  }
};
confirm.onclick = async () => {
  if (!previewPath) return;
  confirm.disabled = true;
  changes(true);
  try {
    await api(previewPath, {});
    previewPath = undefined;
    status.textContent = 'تصمیم در آرشیو ثبت شد.';
  } catch (error) {
    status.textContent = message(error);
    confirm.disabled = error.message === 'VOICE_PREVIEW_UNAVAILABLE';
  } finally {
    changes(false);
  }
};
stop.onclick = () => { void end(false); };
cancel.onclick = () => { void end(true); };
window.addEventListener('pagehide', () => { if (active) stopMicrophone(active); });

try {
  csrf = (await api('/bff/session')).csrf;
  const me = await api('/bff/api/v1/me');
  for (const workspace of me.workspaces.filter(row => row.role === 'OWNER')) {
    const candidate = '/bff/api/v1/workspaces/' + workspace.id + '/voice-pilot';
    try {
      await api(candidate);
      path = candidate;
      break;
    } catch (error) {
      if (!['VOICE_PILOT_UNAVAILABLE', 'NOT_FOUND'].includes(error.message)) throw error;
    }
  }
  if (!path) throw new Error('VOICE_PILOT_UNAVAILABLE');
  status.textContent = 'برای ضبط آماده است.';
  changes(false);
} catch (error) {
  status.textContent = message(error);
}
