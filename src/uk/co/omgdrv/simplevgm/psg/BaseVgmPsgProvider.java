package uk.co.omgdrv.simplevgm.psg;

import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.util.BlipBuffer;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public abstract class BaseVgmPsgProvider implements VgmPsgProvider {


    public static final double NANOS_TO_SEC = 1_000_000_000;
    public static final int VGM_SAMPLE_RATE_HZ = 44100;
    public static final double NANOS_PER_SAMPLE = NANOS_TO_SEC / VGM_SAMPLE_RATE_HZ;
    public static int CLOCK_HZ = 3579545;

    protected int currentVgmDelayCycle;

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        runUntil(vgmDelayCycles);
    }

    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void reset() {
        currentVgmDelayCycle = 0;
    }

    //TODO implement
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

    protected void runUntil(int vgmDelayCycles) {
        if (vgmDelayCycles > currentVgmDelayCycle) {
            long delayCycles = toPsgCycles(vgmDelayCycles);
            while (delayCycles-- > 0) {
                updateSampleBuffer();
            }
            currentVgmDelayCycle = vgmDelayCycles;
        }
    }

    protected abstract void updateSampleBuffer();

    protected abstract long toPsgCycles(long vgmDelayCycles);
}
