package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import java.awt.Component;

@OriginalClass("client!vh")
public class AudioChannel {

	@OriginalMember(owner = "client!na", name = "w", descriptor = "Z")
	public static boolean stereo;
	@OriginalMember(owner = "client!va", name = "O", descriptor = "I")
	public static int threadPriority;
	@OriginalMember(owner = "client!em", name = "x", descriptor = "Lclient!cj;")
	public static AudioThread audioThread;
	@OriginalMember(owner = "client!dh", name = "h", descriptor = "I")
	public static int sampleRate;

	@OriginalMember(owner = "client!vh", name = "h", descriptor = "Lclient!qb;")
	private PcmStream audioStream;

	@OriginalMember(owner = "client!vh", name = "n", descriptor = "[I")
	public int[] samples;

	@OriginalMember(owner = "client!vh", name = "D", descriptor = "I")
	private int bufferSizeAdjustment;

	@OriginalMember(owner = "client!vh", name = "H", descriptor = "I")
	public int sampleRate2;

	@OriginalMember(owner = "client!vh", name = "K", descriptor = "I")
	public int bufferCapacity;

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "I")
	private final int constant = 32;

	@OriginalMember(owner = "client!vh", name = "f", descriptor = "J")
	private long currentClockTime = MonotonicClock.currentTimeMillis();

	@OriginalMember(owner = "client!vh", name = "w", descriptor = "[Lclient!qb;")
	private final PcmStream[] pcmStreamsArrayOne = new PcmStream[8];

	@OriginalMember(owner = "client!vh", name = "x", descriptor = "I")
	private int consumedSamples = 0;

	@OriginalMember(owner = "client!vh", name = "v", descriptor = "J")
	private long calculateConsumptionAt = 0L;

	@OriginalMember(owner = "client!vh", name = "E", descriptor = "I")
	private int bufferPosition = 0;

	@OriginalMember(owner = "client!vh", name = "A", descriptor = "Z")
	private boolean skipConsumptionCheck = true;

	@OriginalMember(owner = "client!vh", name = "z", descriptor = "[Lclient!qb;")
	private final PcmStream[] pcmStreamsArrayTwo = new PcmStream[8];

	@OriginalMember(owner = "client!vh", name = "y", descriptor = "J")
	private long closeUntil = 0L;

	@OriginalMember(owner = "client!vh", name = "G", descriptor = "I")
	private int prevConsumedSamples = 0;

	@OriginalMember(owner = "client!vh", name = "C", descriptor = "I")
	private int prevBufferSize = 0;

	@OriginalMember(owner = "client!dc", name = "a", descriptor = "(IIIZ)V")
	public static void init(@OriginalArg(3) boolean stereo) {
		threadPriority = 2;
		AudioChannel.stereo = stereo;
		sampleRate = GlobalConfig.AUDIO_SAMPLE_RATE;
	}

	@OriginalMember(owner = "client!id", name = "a", descriptor = "(ILsignlink!ll;Ljava/awt/Component;II)Lclient!vh;")
	public static AudioChannel create(@OriginalArg(0) int sampleRate, @OriginalArg(1) SignLink signLink, @OriginalArg(2) Component component, @OriginalArg(3) int channelIndex) {
		if (AudioChannel.sampleRate == 0) {
			throw new IllegalStateException();
		}
		try {
			@Pc(33) AudioChannel audioChannel = new JavaAudioChannel();
			audioChannel.sampleRate2 = sampleRate;
			audioChannel.samples = new int[(stereo ? 2 : 1) * 256];
			audioChannel.init(component);
			audioChannel.bufferCapacity = (sampleRate & -1024) + 1024;
			if (audioChannel.bufferCapacity > 16384) {
				audioChannel.bufferCapacity = 16384;
			}
			audioChannel.open(audioChannel.bufferCapacity);
			if (threadPriority > 0 && audioThread == null) {
				audioThread = new AudioThread();
				audioThread.signLink = signLink;
				signLink.startThread(threadPriority, audioThread);
			}
			if (audioThread != null) {
				if (audioThread.channels[channelIndex] != null) {
					throw new IllegalArgumentException();
				}
				audioThread.channels[channelIndex] = audioChannel;
			}
			return audioChannel;
		} catch (@Pc(109) Throwable ex1) {
			ex1.printStackTrace();
			try {
				@Pc(120) SignLinkAudioChannel signLinkAudioChannel = new SignLinkAudioChannel(signLink, channelIndex);
				signLinkAudioChannel.samples = new int[(stereo ? 2 : 1) * 256];
				signLinkAudioChannel.sampleRate2 = sampleRate;
				signLinkAudioChannel.init(component);
				signLinkAudioChannel.bufferCapacity = 16384;
				signLinkAudioChannel.open(signLinkAudioChannel.bufferCapacity);
				if (threadPriority > 0 && audioThread == null) {
					audioThread = new AudioThread();
					audioThread.signLink = signLink;
					signLink.startThread(threadPriority, audioThread);
				}
				if (audioThread != null) {
					if (audioThread.channels[channelIndex] != null) {
						throw new IllegalArgumentException();
					}
					audioThread.channels[channelIndex] = signLinkAudioChannel;
				}
				return signLinkAudioChannel;
			} catch (@Pc(186) Throwable ex2) {
				ex2.printStackTrace();
				return new AudioChannel();
			}
		}
	}

	@OriginalMember(owner = "client!nd", name = "a", descriptor = "(ZLclient!qb;)V")
	public static void setInactive(@OriginalArg(1) PcmStream pcmStream) {
		if (pcmStream.sound != null) {
			pcmStream.sound.position = 0;
		}
		pcmStream.active = false;
		for (@Pc(14) PcmStream subStream = pcmStream.firstSubStream(); subStream != null; subStream = pcmStream.nextSubStream()) {
			setInactive(subStream);
		}
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "()V")
	protected void write() throws Exception {
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "(I)V")
	public void open(@OriginalArg(0) int arg0) throws Exception {
	}

	@OriginalMember(owner = "client!vh", name = "b", descriptor = "()V")
	protected void close() throws Exception {
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "([II)V")
	private void readAudioData(@OriginalArg(0) int[] audioBuffer) {
		int dataToProcess = stereo ? 512 : 256;
		ArrayUtils.clear(audioBuffer, 0, dataToProcess);
		this.bufferPosition -= 256;
		if (this.audioStream != null && this.bufferPosition <= 0) {
			this.bufferPosition += sampleRate >> 4;
			setInactive(this.audioStream);
			this.updatePcmStreamArray(this.audioStream, this.audioStream.getSomeCalculationResult());
			@Pc(45) int sumProcessed = 0;
			@Pc(47) int bitMask = 255;
			@Pc(49) int streamIndex = 7;
			label106:
			while (bitMask != 0) {
				@Pc(57) int bitIndex;
				@Pc(62) int offset;
				if (streamIndex < 0) {
					bitIndex = streamIndex & 0x3;
					offset = -(streamIndex >> 2);
				} else {
					bitIndex = streamIndex;
					offset = 0;
				}
				for (@Pc(73) int mask = bitMask >>> bitIndex & 0x11111111; mask != 0; mask >>>= 0x4) {
					if ((mask & 0x1) != 0) {
						bitMask &= ~(0x1 << bitIndex);
						@Pc(91) PcmStream lastActiveStream = null;
						@Pc(96) PcmStream currentStream = this.pcmStreamsArrayOne[bitIndex];
						label100:
						while (true) {
							while (true) {
								if (currentStream == null) {
									break label100;
								}
								@Pc(101) Sound currentSound = currentStream.sound;
								if (currentSound == null || currentSound.position <= offset) {
									currentStream.active = true;
									@Pc(125) int processed = currentStream.calculateSomething();
									sumProcessed += processed;
									if (currentSound != null) {
										currentSound.position += processed;
									}
									if (sumProcessed >= this.constant) {
										break label106;
									}
									@Pc(145) PcmStream subStream = currentStream.firstSubStream();
									if (subStream != null) {
										@Pc(150) int position = currentStream.index;
										while (subStream != null) {
											this.updatePcmStreamArray(subStream, position * subStream.getSomeCalculationResult() >> 8);
											subStream = currentStream.nextSubStream();
										}
									}
									@Pc(169) PcmStream nextStream = currentStream.nextPcmStream;
									currentStream.nextPcmStream = null;
									if (lastActiveStream == null) {
										this.pcmStreamsArrayOne[bitIndex] = nextStream;
									} else {
										lastActiveStream.nextPcmStream = nextStream;
									}
									if (nextStream == null) {
										this.pcmStreamsArrayTwo[bitIndex] = lastActiveStream;
									}
									currentStream = nextStream;
								} else {
									bitMask |= 0x1 << bitIndex;
									lastActiveStream = currentStream;
									currentStream = currentStream.nextPcmStream;
								}
							}
						}
					}
					bitIndex += 4;
					offset++;
				}
				streamIndex--;
			}
			for (streamIndex = 0; streamIndex < 8; streamIndex++) {
				@Pc(212) PcmStream pcmStream = this.pcmStreamsArrayOne[streamIndex];
				this.pcmStreamsArrayOne[streamIndex] = this.pcmStreamsArrayTwo[streamIndex] = null;
				while (pcmStream != null) {
					@Pc(227) PcmStream nextPcmStream = pcmStream.nextPcmStream;
					pcmStream.nextPcmStream = null;
					pcmStream = nextPcmStream;
				}
			}
		}
		if (this.bufferPosition < 0) {
			this.bufferPosition = 0;
		}
		if (this.audioStream != null) {
			this.audioStream.read(audioBuffer, 0, 256);
		}
		this.currentClockTime = MonotonicClock.currentTimeMillis();
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "(B)V")
	public final synchronized void loop() {
		if (this.samples == null) {
			return;
		}
		@Pc(14) long currentTime = MonotonicClock.currentTimeMillis();
		try {
			if (this.closeUntil != 0L) {
				if (currentTime < this.closeUntil) {
					return;
				}
				this.open(this.bufferCapacity);
				this.skipConsumptionCheck = true;
				this.closeUntil = 0L;
			}
			@Pc(38) int currentBufferSize = this.getBufferSize();
			if (this.consumedSamples < this.prevBufferSize - currentBufferSize) {
				this.consumedSamples = this.prevBufferSize - currentBufferSize;
			}
			@Pc(65) int desiredBufferSize = this.sampleRate2 + this.bufferSizeAdjustment;
			if (desiredBufferSize + 256 > 16384) {
				desiredBufferSize = 16128;
			}
			if (this.bufferCapacity < desiredBufferSize + 256) {
				this.bufferCapacity += 1024;
				if (this.bufferCapacity > 16384) {
					this.bufferCapacity = 16384;
				}
				this.flush();
				currentBufferSize = 0;
				this.open(this.bufferCapacity);
				if (this.bufferCapacity < desiredBufferSize + 256) {
					desiredBufferSize = this.bufferCapacity - 256;
					this.bufferSizeAdjustment = desiredBufferSize - this.sampleRate2;
				}
				this.skipConsumptionCheck = true;
			}
			while (desiredBufferSize > currentBufferSize) {
				currentBufferSize += 256;
				this.readAudioData(this.samples);
				this.write();
			}
			if (currentTime > this.calculateConsumptionAt) {
				if (this.skipConsumptionCheck) {
					this.skipConsumptionCheck = false;
				} else if (this.consumedSamples == 0 && this.prevConsumedSamples == 0) {
					this.flush();
					this.closeUntil = currentTime + 2000L;
					return;
				} else {
					this.bufferSizeAdjustment = Math.min(this.prevConsumedSamples, this.consumedSamples);
					this.prevConsumedSamples = this.consumedSamples;
				}
				this.calculateConsumptionAt = currentTime + 2000L;
				this.consumedSamples = 0;
			}
			this.prevBufferSize = currentBufferSize;
		} catch (@Pc(202) Exception ex) {
			ex.printStackTrace();
			this.flush();
			this.closeUntil = currentTime + 2000L;
		}
		try {
			if (currentTime > this.currentClockTime + 500000L) {
				currentTime = this.currentClockTime;
			}
			while (currentTime > this.currentClockTime + 5000L) {
				this.skip();
				this.currentClockTime += 256000 / sampleRate;
			}
		} catch (@Pc(247) Exception ex) {
			ex.printStackTrace();
			this.currentClockTime = currentTime;
		}
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "(ILclient!qb;)V")
	public final synchronized void setAudioStream(@OriginalArg(1) PcmStream pcmStream) {
		this.audioStream = pcmStream;
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "(Lclient!qb;IB)V")
	private void updatePcmStreamArray(@OriginalArg(0) PcmStream pcmStream, @OriginalArg(1) int index) {
		@Pc(16) int adjustedIndex = index >> 5;
		@Pc(21) PcmStream existingPcmStream = this.pcmStreamsArrayTwo[adjustedIndex];
		if (existingPcmStream == null) {
			this.pcmStreamsArrayOne[adjustedIndex] = pcmStream;
		} else {
			existingPcmStream.nextPcmStream = pcmStream;
		}
		this.pcmStreamsArrayTwo[adjustedIndex] = pcmStream;
		pcmStream.index = index;
	}

	@OriginalMember(owner = "client!vh", name = "c", descriptor = "()I")
	protected int getBufferSize() throws Exception {
		return this.bufferCapacity;
	}

	@OriginalMember(owner = "client!vh", name = "b", descriptor = "(B)V")
	public final synchronized void stopAudio() {
		this.skipConsumptionCheck = true;
		try {
			this.close();
		} catch (@Pc(10) Exception ex) {
			ex.printStackTrace();
			this.flush();
			this.closeUntil = MonotonicClock.currentTimeMillis() + 2000L;
		}
	}

	@OriginalMember(owner = "client!vh", name = "b", descriptor = "(I)V")
	public final void method3571() {
		this.skipConsumptionCheck = true;
	}

	@OriginalMember(owner = "client!vh", name = "d", descriptor = "()V")
	protected void flush() {
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "(II)V")
	private void skip() {
		this.bufferPosition -= 256;
		if (this.bufferPosition < 0) {
			this.bufferPosition = 0;
		}
		if (this.audioStream != null) {
			this.audioStream.skip(256);
		}
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "(Z)V")
	public final synchronized void quit() {
		if (audioThread != null) {
			@Pc(6) boolean isCurrentChannel = true;
			for (@Pc(8) int channelIndex = 0; channelIndex < 2; channelIndex++) {
				if (audioThread.channels[channelIndex] == this) {
					audioThread.channels[channelIndex] = null;
				}
				if (audioThread.channels[channelIndex] != null) {
					isCurrentChannel = false;
				}
			}
			if (isCurrentChannel) {
				audioThread.stop = true;
				while (audioThread.running) {
					ThreadUtils.sleep(50L);
				}
				audioThread = null;
			}
		}
		this.flush();
		this.samples = null;
	}

	@OriginalMember(owner = "client!vh", name = "a", descriptor = "(Ljava/awt/Component;)V")
	public void init(@OriginalArg(0) Component arg0) throws Exception {
	}
}
