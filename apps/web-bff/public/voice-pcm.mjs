class WhiteHatPcm extends AudioWorkletProcessor {
  constructor() {
    super();
    this.samples = new Int16Array(4096);
    this.offset = 0;
    this.stopped = false;
    this.port.onmessage = () => {
      this.stopped = true;
      this.flush();
      this.port.postMessage(null);
    };
  }

  flush() {
    if (!this.offset) return;
    const pcm = new ArrayBuffer(this.offset * 2);
    const target = new DataView(pcm);
    for (let i = 0; i < this.offset; i++) target.setInt16(i * 2, this.samples[i], true);
    this.port.postMessage(pcm, [pcm]);
    this.offset = 0;
  }

  process(inputs) {
    if (this.stopped) return false;
    for (const sample of inputs[0]?.[0] ?? []) {
      const value = Math.max(-1, Math.min(sample, 1));
      this.samples[this.offset++] = Math.round(value * 32767);
      if (this.offset === this.samples.length) this.flush();
    }
    return true;
  }
}
registerProcessor('whitehat-pcm', WhiteHatPcm);
