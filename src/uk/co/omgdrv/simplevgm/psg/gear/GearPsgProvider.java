package uk.co.omgdrv.simplevgm.psg.gear;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class GearPsgProvider extends BaseVgmPsgProvider {

    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / PsgProvider.GEAR_CLOCK_HZ / 2; // div2 //TODO Why

    protected PsgProvider psg;
    private double nanosToNextSample = NANOS_PER_SAMPLE;
    public int sampleCounter = 0;
    public byte[] gearBuffer = new byte[VGM_SAMPLE_RATE_HZ];
    protected PsgCompare psgCompare;
    protected PsgCompare.PsgType type = PsgCompare.PsgType.GEAR;

    public static GearPsgProvider createInstance() {
        return createInstance(null);
    }

    public static GearPsgProvider createInstance(PsgCompare compare) {
        GearPsgProvider g = new GearPsgProvider();
        g.psg = PsgProvider.createInstance(VGM_SAMPLE_RATE_HZ);
        g.psgCompare = compare;
        return g;
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        super.writeData(vgmDelayCycles, data);
        psg.write(data);
    }

    @Override
    protected long toPsgCycles(long vgmDelayCycles) {
        return (long) ((vgmDelayCycles * 1.0 / VGM_SAMPLE_RATE_HZ) * PsgProvider.GEAR_CLOCK_HZ);
    }

    @Override
    protected void updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        if (nanosToNextSample < 0) {
            nanosToNextSample += NANOS_PER_SAMPLE;
            psg.output(gearBuffer, sampleCounter, sampleCounter + 1);
            sampleCounter++;
            if (sampleCounter == VGM_SAMPLE_RATE_HZ) {
                sampleCounter = 0;
                if (psgCompare != null) {
                    psgCompare.pushData(type, gearBuffer);
                }
            }
        }
    }
}
