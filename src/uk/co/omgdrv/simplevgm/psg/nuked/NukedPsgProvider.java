package uk.co.omgdrv.simplevgm.psg.nuked;

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
 * <p>
 * https://forums.nesdev.com/viewtopic.php?f=23&t=15562
 */
public class NukedPsgProvider implements VgmPsgProvider {
    public static final int PSG_MAX_VOLUME = 0x40;
    public static int CLOCK_HZ = 3579545;
    public static int NUKED_CLOCK_HZ = CLOCK_HZ / 16;

    private static final double NANOS_TO_SEC = 1_000_000_000;
    private static final double NANOS_PER_SAMPLE = NANOS_TO_SEC / VgmEmu.VGM_SAMPLE_RATE_HZ; //22675 ns
    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / NUKED_CLOCK_HZ / 2; // div2 //TODO Why

    private PsgYm7101 psg;
    private PsgYm7101.PsgContext context;

    private double[] nukedBufferRaw = new double[VgmEmu.VGM_SAMPLE_RATE_HZ];
    private double[] nukedBufferDiff = new double[VgmEmu.VGM_SAMPLE_RATE_HZ];
    public byte[] nukedBuffer = new byte[VgmEmu.VGM_SAMPLE_RATE_HZ];

    private double nanosToNextSample = NANOS_PER_SAMPLE;
    private int currentVgmDelayCycle;
    private int sampleCounter = 0;
    public int secondsElapsed = 0;

    private PsgCompare psgCompare;

    public static NukedPsgProvider createInstance() {
        return createInstance(null);
    }

    public static NukedPsgProvider createInstance(PsgCompare psgCompare) {
        NukedPsgProvider n = new NukedPsgProvider();
        n.psgCompare = psgCompare;
        return n;
    }

    public NukedPsgProvider() {
        psg = new PsgYm7101Impl();
        context = new PsgYm7101.PsgContext();
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        runUntil(vgmDelayCycles);
        psg.PSG_Write(context, data);
    }

    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {
    }

    @Override
    public void reset() {
        psg.PSG_Reset(context);
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

    private static long toPsgCycles(long vgmDelayCycles) {
        return (long) ((vgmDelayCycles * 1.0 / VgmEmu.VGM_SAMPLE_RATE_HZ) * NUKED_CLOCK_HZ);
    }

    private void runUntil(int vgmDelayCycles) {
        if (vgmDelayCycles > currentVgmDelayCycle) {
            long delayCycles = toPsgCycles(vgmDelayCycles);
            while (delayCycles-- > 0) {
                psg.PSG_Cycle(context);
                updateSampleBuffer();
            }
            currentVgmDelayCycle = vgmDelayCycles;
        }
    }

    private void updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        if (nanosToNextSample < 0) {
            nanosToNextSample += NANOS_PER_SAMPLE;
            nukedBufferRaw[sampleCounter] = psg.PSG_GetSample(context); //sample [0;4]
            sampleCounter++;
            if (sampleCounter == VgmEmu.VGM_SAMPLE_RATE_HZ) {
                highPassFilter(nukedBufferRaw, nukedBufferDiff);
                scaleFilter(nukedBufferDiff, nukedBuffer);
                sampleCounter = 0;
                if (psgCompare != null) {
                    psgCompare.pushData(PsgCompare.PsgType.NUKED, nukedBuffer);
                }
                secondsElapsed++;
            }
        }
    }

    private void highPassFilter(double[] input, double[] output) {
        output[0] = input[0];
        for (int i = 1; i < input.length - 1; i++) {
            double change = (input[i + 1] - input[i]);
            if (Math.abs(change) < 0.05) {
                output[i] = output[i - 1];
            } else {
                output[i] = change > 1 ? change : (change < -1 ? -1 : change);
            }
        }
        output[nukedBufferRaw.length - 1] = output[nukedBufferRaw.length - 2];
    }

    private void scaleFilter(double[] input, byte[] output) {
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) Math.round(input[i] * PSG_MAX_VOLUME);
        }
    }
}
