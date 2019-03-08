package uk.co.omgdrv.simplevgm.psg.green;

import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.psg.gear.PsgProvider;
import uk.co.omgdrv.simplevgm.util.StereoBuffer;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class GreenPsgProvider extends BaseVgmPsgProvider {

    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / PsgProvider.GEAR_CLOCK_HZ / 2;
    static final int psgTimeBits = 12;
    static final int psgTimeUnit = 1 << psgTimeBits;
    static final int psgFactor = (int) (1.0 * psgTimeUnit / VGM_SAMPLE_RATE_HZ * CLOCK_HZ + 0.5);


    private VgmPsgProvider psg;
    private double nanosToNextSample = NANOS_PER_SAMPLE;
    public int sampleCounter = 0;

    public byte[] greenBuffer = new byte[VGM_SAMPLE_RATE_HZ];

    protected PsgCompare psgCompare;
    protected PsgCompare.PsgType type = PsgCompare.PsgType.GREEN;
    protected StereoBuffer stereoBuffer;

    public static GreenPsgProvider createInstance(PsgCompare compare) {
        GreenPsgProvider g = new GreenPsgProvider();
        g.psg = new SmsApu();
        g.psgCompare = compare;
        g.stereoBuffer = new StereoBuffer("GreenPsg");
        g.init();
        return g;
    }

    private void init() {
        stereoBuffer.setSampleRate(VGM_SAMPLE_RATE_HZ, 1000);
        stereoBuffer.setClockRate(CLOCK_HZ);
        psg.setOutput(stereoBuffer.center(), stereoBuffer.left(), stereoBuffer.right());
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        super.writeData(vgmDelayCycles, data);
        psg.writeData((int) toPsgCycles(vgmDelayCycles), data);
    }

    private void endFrameInternal(int vgmDelayCycles) {
        int time = (int) toPsgCycles(vgmDelayCycles);
        psg.endFrame(time);
        stereoBuffer.endFrame(vgmDelayCycles);
    }

    @Override
    protected void updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        if (nanosToNextSample < 0) {
            nanosToNextSample += NANOS_PER_CYCLE;
            sampleCounter++;
            if (sampleCounter == VGM_SAMPLE_RATE_HZ) {
                endFrameInternal(VGM_SAMPLE_RATE_HZ);
                int read = stereoBuffer.readSamples(greenBuffer, 0, greenBuffer.length);
                sampleCounter = 0;
                if (psgCompare != null) {
                    psgCompare.pushData(type, greenBuffer);
                }
            }
        }
    }

    @Override
    protected long toPsgCycles(long vgmDelayCycles) {
        return (vgmDelayCycles * psgFactor + psgTimeUnit / 2) >> psgTimeBits;
    }
}
