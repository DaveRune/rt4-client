package rt4;

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

	@OriginalMember(owner = "client!qa", name = "d", descriptor = "()V")
	@Override
	protected final void flush() {
		if (this.sourceDataLine != null) {
			this.sourceDataLine.close();
			this.sourceDataLine = null;
		}
	}

	@OriginalMember(owner = "client!qa", name = "a", descriptor = "(Ljava/awt/Component;)V")
	@Override
	public final void init(@OriginalArg(0) Component arg0) {
		@Pc(1) Info[] mixers = AudioSystem.getMixerInfo();
		if (mixers != null) {
			for (@Pc(9) int mixerIndex = 0; mixerIndex < mixers.length; mixerIndex++) {
				@Pc(21) Info mixerInfo = mixers[mixerIndex];
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

	@OriginalMember(owner = "client!qa", name = "a", descriptor = "()V")
	@Override
	protected final void write() {
		@Pc(1) short sampleBatchSize = 256;
		if (AudioChannel.stereo) {
			sampleBatchSize = 512;
		}
		for (@Pc(9) int i = 0; i < sampleBatchSize; i++) {
			@Pc(17) int sampleData = this.samples[i];
			if ((sampleData + 8388608 & 0xFF000000) != 0) {
				sampleData = sampleData >> 31 ^ 0x7FFFFF;
			}
			this.audioData[i * 2] = (byte) (sampleData >> 8);
			this.audioData[i * 2 + 1] = (byte) (sampleData >> 16);
		}
		this.sourceDataLine.write(this.audioData, 0, sampleBatchSize << 1);
	}
}
