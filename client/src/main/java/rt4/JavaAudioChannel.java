package rt4;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;
import java.awt.Component;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@OriginalClass("client!qa")
public final class JavaAudioChannel extends AudioChannel {

	@OriginalMember(owner = "client!qa", name = "L", descriptor = "I")
	private int bufferSize;

	@OriginalMember(owner = "client!qa", name = "M", descriptor = "Ljavax/sound/sampled/SourceDataLine;")
	private SourceDataLine sourceDataLine;

	@OriginalMember(owner = "client!qa", name = "O", descriptor = "Ljavax/sound/sampled/AudioFormat;")
	private AudioFormat audioFormat;

	@OriginalMember(owner = "client!qa", name = "P", descriptor = "[B")
	private byte[] audioData;

	@OriginalMember(owner = "client!qa", name = "N", descriptor = "Z")
	private boolean isSoundMax = false;

	private long audioDevice;
	private long audioContext;
	private int source;

	// Initialization (done once outside write())
	int[] buffers = new int[2];
	int nextBuffer = 0;

	@OriginalMember(owner = "client!qa", name = "d", descriptor = "()V")
	@Override
	protected final void flush() {
		if (this.sourceDataLine != null) {
			this.sourceDataLine.close();
			this.sourceDataLine = null;
		}
	}

	public void initOpenAL(){
		// Get the device
		String defaultDeviceName = ALC10.alcGetString(0, ALC11.ALC_DEFAULT_DEVICE_SPECIFIER);
		long audioDevice = ALC10.alcOpenDevice(defaultDeviceName);

		// Get the capabilities
		ALCCapabilities deviceCaps = ALC.createCapabilities(audioDevice);

		if (deviceCaps.OpenALC10) {
			System.out.println("OpenALC10: Available");
		}

		if (deviceCaps.OpenALC11) {
			System.out.println("OpenALC11: Available");
		}

		if (deviceCaps.ALC_EXT_EFX) {
			System.out.println("ALC_EXT_EFX: Available");
		}

		// Create a context and make it current
		IntBuffer contextAttribList = BufferUtils.createIntBuffer(16);
		contextAttribList.put(ALC10.ALC_REFRESH);
		contextAttribList.put(60);

		contextAttribList.put(ALC10.ALC_SYNC);
		contextAttribList.put(ALC10.ALC_FALSE);

		contextAttribList.put(0);
		contextAttribList.flip();

		audioContext = ALC10.alcCreateContext(audioDevice, contextAttribList);

		if (!ALC10.alcMakeContextCurrent(audioContext)) {
			throw new RuntimeException("Failed to make context current");
		}

		AL.createCapabilities(deviceCaps);
		source = AL10.alGenSources();
		for (int i = 0; i < 2; i++) {
			buffers[i] = AL10.alGenBuffers();
		}
	}

	@OriginalMember(owner = "client!qa", name = "a", descriptor = "(Ljava/awt/Component;)V")
	@Override
	public final void init(@OriginalArg(0) Component arg0) {
		initOpenAL();
		@Pc(1) Info[] mixers = AudioSystem.getMixerInfo();
		if (mixers != null) {
			for (Info mixerInfo : mixers) {
				if (mixerInfo != null) {
					@Pc(28) String mixerName = mixerInfo.getName();
					if (mixerName != null && mixerName.toLowerCase().contains("soundmax")) {
						this.isSoundMax = true;
					}
				}
			}
		}
		this.audioFormat = new AudioFormat((float) AudioChannel.sampleRate, 16, AudioChannel.stereo ? 2 : 1, true, false);
		this.audioData = new byte[0x100 << (AudioChannel.stereo ? 2 : 1)];
	}

	@OriginalMember(owner = "client!qa", name = "a", descriptor = "(I)V")
	@Override
	public final void open(@OriginalArg(0) int size) throws LineUnavailableException {
		try {
			@Pc(20) javax.sound.sampled.DataLine.Info lineInfo = new javax.sound.sampled.DataLine.Info(SourceDataLine.class, this.audioFormat, size << (AudioChannel.stereo ? 2 : 1));
			this.sourceDataLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
			this.sourceDataLine.open();
			this.sourceDataLine.start();
			this.bufferSize = size;
		} catch (@Pc(36) LineUnavailableException e) {
			if (IntUtils.bitCountFast(size) == 1) {
				this.sourceDataLine = null;
				throw e;
			} else {
				this.open(IntUtils.clp2(size));
			}
		}
	}

	@OriginalMember(owner = "client!qa", name = "b", descriptor = "()V")
	@Override
	protected final void close() throws LineUnavailableException {
		this.sourceDataLine.flush();
		if (!this.isSoundMax) {
			return;
		}
		this.sourceDataLine.close();
		this.sourceDataLine = null;
		@Pc(34) javax.sound.sampled.DataLine.Info lineInfo = new javax.sound.sampled.DataLine.Info(SourceDataLine.class, this.audioFormat, this.bufferSize << (AudioChannel.stereo ? 2 : 1));
		this.sourceDataLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
		this.sourceDataLine.open();
		this.sourceDataLine.start();
	}

	@OriginalMember(owner = "client!qa", name = "c", descriptor = "()I")
	@Override
	protected final int getBufferSize() {
		return this.bufferSize - (this.sourceDataLine.available() >> (AudioChannel.stereo ? 2 : 1));
	}

	protected final void write() {
		int error;
		short sampleBatchSize = 256;
		if (AudioChannel.stereo) {
			sampleBatchSize = 512;
		}
		ByteBuffer bufferData = BufferUtils.createByteBuffer(sampleBatchSize * 2);
		for (int i = 0; i < sampleBatchSize; i++) {
			int sampleData = this.samples[i];
			if ((sampleData + 8388608 & 0xFF000000) != 0) {
				sampleData = sampleData >> 31 ^ 0x7FFFFF;
			}
			byte byte1 = (byte) (sampleData >> 8);
			byte byte2 = (byte) (sampleData >> 16);

			bufferData.put(byte1);
			bufferData.put(byte2);
		}
		bufferData.flip();

		int buffer = AL10.alGenBuffers();

		error = AL10.alGetError();
		if (error != AL10.AL_NO_ERROR) {
			System.out.println("OpenAL (alGenBufferes) error detected! Error code: " + error);
		}

		AL10.alBufferData(buffer, AudioChannel.stereo ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16, bufferData, AudioChannel.sampleRate);
		error = AL10.alGetError();
		if (error != AL10.AL_NO_ERROR) {
			System.out.println("OpenAL (alBufferData) error detected! Error code: " + error);
		}

		// Stop the source before attaching a buffer to it
		if (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
			AL10.alSourceStop(source);
		}
		error = AL10.alGetError();
		if (error != AL10.AL_NO_ERROR) {
			System.out.println("OpenAL (alSourceStop) error detected! Error code: " + error);
		}

		AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
		error = AL10.alGetError();
		if (error != AL10.AL_NO_ERROR) {
			System.out.println("OpenAL (alSourcei) error detected! Error code: " + error);
		}

		AL10.alSourcePlay(source);
		error = AL10.alGetError();
		if (error != AL10.AL_NO_ERROR) {
			System.out.println("OpenAL (alSourcePlay) error detected! Error code: " + error);
		}
	}
}
