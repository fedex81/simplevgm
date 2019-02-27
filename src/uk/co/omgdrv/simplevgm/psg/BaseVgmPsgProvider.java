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

    protected int currentVgmDelayCycle;

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        runUntil(vgmDelayCycles);
    }

    //TODO implement??
    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {

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
