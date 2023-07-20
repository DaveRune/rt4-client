package rt4;

import javax.sound.sampled.*;
import java.awt.Component;

public final class JavaAudioChannel extends AudioChannel {

	private int bufferSize;
	private SourceDataLine sourceDataLine;
	private AudioFormat audioFormat;
	private byte[] audioData;
	private boolean isSoundMaxMixer; // SoundMAX is a suite of audio processing algorithms (has nothing to do with volume)
	private final int stereoMultiplier = AudioChannel.stereo ? 2 : 1; // constant variable for stereo check result

	@Override
	public void init(Component component) {
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		if (mixers != null) {
			for (Mixer.Info mixerInfo : mixers) {
				if (mixerInfo == null) { continue; }
				String mixerName = mixerInfo.getName();
				if (mixerName != null && mixerName.toLowerCase().contains("soundmax")) {
					isSoundMaxMixer = true;
					break;
				}
			}
		}
		audioFormat = new AudioFormat((float) AudioChannel.sampleRate, 16, AudioChannel.stereo ? 2 : 1, true, false);
		audioData = new byte[0x100 << stereoMultiplier];
	}

	@Override
	public void open(int size) throws LineUnavailableException {
		try {
			sourceDataLine = getAudioLine(size << stereoMultiplier);
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

	private SourceDataLine getAudioLine(int lineSize) throws LineUnavailableException {
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, lineSize);
		return (SourceDataLine) AudioSystem.getLine(lineInfo);
	}

	@Override
	protected void close() throws LineUnavailableException {
		sourceDataLine.flush();
		if (isSoundMaxMixer) {
			sourceDataLine.close();
			sourceDataLine = getAudioLine(bufferSize << stereoMultiplier);
			sourceDataLine.open();
			sourceDataLine.start();
		}
	}
	@Override
	protected int getBufferSize() {
		return bufferSize - (sourceDataLine.available() >> stereoMultiplier);
	}

	protected void write() {
		int audioSamplesPerBatch = AudioChannel.stereo ? 512 : 256;
		for (int i = 0; i < audioSamplesPerBatch; i++) {
			int sampleData = samples[i];
			if ((sampleData + 8388608 & 0xFF000000) != 0) {
				sampleData = sampleData >> 31 ^ 0x7FFFFF;
			}
			audioData[i * 2] = (byte) (sampleData >> 8);
			audioData[i * 2 + 1] = (byte) (sampleData >> 16);
		}
		sourceDataLine.write(audioData, 0, audioSamplesPerBatch << 1);
	}
}