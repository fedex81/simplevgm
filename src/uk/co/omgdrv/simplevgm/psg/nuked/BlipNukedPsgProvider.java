package uk.co.omgdrv.simplevgm.psg.nuked;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.util.BlipBuffer;
import uk.co.omgdrv.simplevgm.util.DspUtil;

import java.util.Arrays;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 * <p>
 * https://forums.nesdev.com/viewtopic.php?f=23&t=15562
 */
public class BlipNukedPsgProvider extends NukedPsgProvider {


    private static int BLIP_BUFFER_SAMPLES_MS = 100;
    private static int BLIP_FACTOR = CLOCK_HZ / NUKED_PSG_SAMPLING_HZ;
    private static int BLIP_BUFFER_SAMPLES_CLOCKS = (int) (CLOCK_HZ * (BLIP_BUFFER_SAMPLES_MS / 1000d));
    private static int BLIP_BUFFER_SAMPLES_END_FRAME = BLIP_BUFFER_SAMPLES_CLOCKS / BLIP_FACTOR;
    private static int BLIP_BUFFER_SIZE = BaseVgmPsgProvider.VGM_SAMPLE_RATE_HZ / (1000 / BLIP_BUFFER_SAMPLES_MS);


    private BlipBuffer blipBuffer;
    private int blipSampleCounter;
    private double lastSample;
    private byte[] bufferSamples = new byte[BLIP_BUFFER_SIZE];
    private byte[] bufferSamples16 = new byte[BLIP_BUFFER_SIZE * 2];

    protected PsgCompare psgCompare;

    public static BlipNukedPsgProvider createInstance() {
        return createInstance(null);
    }

    public static BlipNukedPsgProvider createInstance(PsgCompare psgCompare) {
        BlipNukedPsgProvider n = new BlipNukedPsgProvider();
        n.psgCompare = psgCompare;
        return n;
    }

    BlipNukedPsgProvider() {
        super();
        blipBuffer = new BlipBuffer();
        blipBuffer.setSampleRate(BaseVgmPsgProvider.VGM_SAMPLE_RATE_HZ, BLIP_BUFFER_SAMPLES_MS);
        blipBuffer.setClockRate(CLOCK_HZ);
    }

    @Override
    public void reset() {
        super.reset();
        blipBuffer.clear();
    }

    @Override
    protected boolean updateSampleBuffer() {
//        //TODO wrong??
//        boolean res = super.updateSampleBuffer();
//        if (res) {
//            updateBlipSampleBuffer(rawSample);
//        }
//        return res;
        return false;
    }


    private void updateBlipSampleBuffer(double sample) {
        byte scaledDelta = DspUtil.scaleClamp8bit(sample - lastSample, DspUtil.PSG_MAX_VOLUME_8_BIT);
        int clockRateTime = BLIP_FACTOR * blipSampleCounter;
//        System.out.println(clockRateTime + "," + (scaledSample -lastScaledSample));
        blipBuffer.addDelta(clockRateTime, scaledDelta);
        lastSample = sample;
        blipSampleCounter++;
        if (blipSampleCounter == BLIP_BUFFER_SAMPLES_END_FRAME) {
            blipBuffer.endFrame(BLIP_BUFFER_SAMPLES_CLOCKS);
            int read = blipBuffer.readSamples8bit(bufferSamples, 0, BLIP_BUFFER_SIZE);
            blipSampleCounter = 0;
            if (psgCompare != null) {
                byte[] res = Arrays.copyOf(bufferSamples, read); //TODO
                psgCompare.pushData(PsgCompare.PsgType.NUKED_BLIP, res);
            }
        }
    }
}
