package rt4;

import javax.sound.sampled.*;
import java.awt.Component;

public final class JavaAudioChannel extends AudioChannel {

	private int bufferSize;
	private SourceDataLine sourceDataLine;
	private AudioFormat audioFormat;
	private byte[] audioData;
	private boolean isSoundMax;

	@Override
	public void init(Component component) {
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		if (mixers != null) {
			for (Mixer.Info mixerInfo : mixers) {
				if (mixerInfo != null) {
					String mixerName = mixerInfo.getName();
					if (mixerName != null && mixerName.toLowerCase().contains("soundmax")) {
						isSoundMax = true;
					}
				}
			}
		}
		audioFormat = new AudioFormat((float) AudioChannel.sampleRate, 16, AudioChannel.stereo ? 2 : 1, true, false);
		audioData = new byte[0x100 << (AudioChannel.stereo ? 2 : 1)];
	}

	@Override
	public void open(int size) throws LineUnavailableException {
		try {
			DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, size << (AudioChannel.stereo ? 2 : 1));
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
			sourceDataLine.open();
			sourceDataLine.start();
			bufferSize = size;
		} catch (LineUnavailableException e) {
			if (Integer.bitCount(size) == 1) {
				sourceDataLine = null;
				throw e;
			} else {
				open(Integer.highestOneBit(size));
			}
		}
	}

	@Override
	protected void close() throws LineUnavailableException {
		sourceDataLine.flush();
		if (!isSoundMax) {
			return;
		}
		sourceDataLine.close();
		sourceDataLine = null;
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, bufferSize << (AudioChannel.stereo ? 2 : 1));
		sourceDataLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
		sourceDataLine.open();
		sourceDataLine.start();
	}

	@Override
	protected int getBufferSize() {
		return bufferSize - (sourceDataLine.available() >> (AudioChannel.stereo ? 2 : 1));
	}
	protected void write() {
		int sampleBatchSize = AudioChannel.stereo ? 512 : 256;
		for (int i = 0; i < sampleBatchSize; i++) {
			int sampleData = samples[i];
			if ((sampleData + 8388608 & 0xFF000000) != 0) {
				sampleData = sampleData >> 31 ^ 0x7FFFFF;
			}
			audioData[i * 2] = (byte) (sampleData >> 8);
			audioData[i * 2 + 1] = (byte) (sampleData >> 16);
		}
		sourceDataLine.write(audioData, 0, sampleBatchSize << 1);
	}
}