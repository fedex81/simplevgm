package uk.co.omgdrv.simplevgm.psg.gear;

import uk.co.omgdrv.simplevgm.VgmEmu;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.util.BlipBuffer;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class GearPsgProvider implements VgmPsgProvider {

    private static final double NANOS_TO_SEC = 1_000_000_000;
    private static final double NANOS_PER_SAMPLE = NANOS_TO_SEC / VgmEmu.VGM_SAMPLE_RATE_HZ; //22675 ns
    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / PsgProvider.GEAR_CLOCK_HZ / 2; // div2 //TODO Why

    private PsgProvider psg;
    private double nanosToNextSample = NANOS_PER_SAMPLE;
    private int currentVgmDelayCycle;
    public int sampleCounter = 0;
    public byte[] gearBuffer = new byte[VgmEmu.VGM_SAMPLE_RATE_HZ];
    private PsgCompare psgCompare;

    public static GearPsgProvider createInstance() {
        return createInstance(null);
    }

    public static GearPsgProvider createInstance(PsgCompare compare) {
        GearPsgProvider g = new GearPsgProvider();
        g.psg = PsgProvider.createInstance(VgmEmu.VGM_SAMPLE_RATE_HZ);
        g.psgCompare = compare;
        return g;
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        runUntil(vgmDelayCycles);
        psg.write(data);
    }

    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {

    }

    @Override
    public void reset() {

    }

    @Override
    public void writeGG(int time, int data) {

    }

    @Override
    public void endFrame(int vgmDelayCycles) {
        if (vgmDelayCycles > currentVgmDelayCycle) {
            runUntil(vgmDelayCycles);
        }
        currentVgmDelayCycle -= vgmDelayCycles;
    }

    private void runUntil(int vgmDelayCycles) {
        if (vgmDelayCycles > currentVgmDelayCycle) {
            long delayCycles = toPsgCycles(vgmDelayCycles);
            while (delayCycles-- > 0) {
                updateSampleBuffer();
            }
            currentVgmDelayCycle = vgmDelayCycles;
        }
    }

    private static long toPsgCycles(long vgmDelayCycles) {
        return (long) ((vgmDelayCycles * 1.0 / VgmEmu.VGM_SAMPLE_RATE_HZ) * PsgProvider.GEAR_CLOCK_HZ);
    }

    private void updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        if (nanosToNextSample < 0) {
            nanosToNextSample += NANOS_PER_SAMPLE;
            psg.output(gearBuffer, sampleCounter, sampleCounter + 1);
            sampleCounter++;
            if (sampleCounter == VgmEmu.VGM_SAMPLE_RATE_HZ) {
                sampleCounter = 0;
                if (psgCompare != null) {
                    psgCompare.pushData(PsgCompare.PsgType.GEAR, gearBuffer);
                }
            }
        }
    }
}
