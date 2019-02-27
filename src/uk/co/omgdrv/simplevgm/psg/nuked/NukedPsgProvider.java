package uk.co.omgdrv.simplevgm.psg.nuked;

import uk.co.omgdrv.simplevgm.VgmEmu;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.util.BlipBuffer;

import java.util.stream.IntStream;

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

    public static final int PSG_MAX_VOLUME = 0x80;
    public static int CLOCK_HZ = 3579545;
    public static int NUKED_PSG_SAMPLING_HZ = CLOCK_HZ / 16;

    private static final double NANOS_TO_SEC = 1_000_000_000;
    private static final double NANOS_PER_SAMPLE = NANOS_TO_SEC / NUKED_PSG_SAMPLING_HZ;
    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / CLOCK_HZ;

    private PsgYm7101 psg;
    private PsgYm7101.PsgContext context;

    private double[] rawBuffer = new double[NUKED_PSG_SAMPLING_HZ];
    private double[] hpfBuffer = new double[VgmEmu.VGM_SAMPLE_RATE_HZ];
    public byte[] nukedBuffer = new byte[VgmEmu.VGM_SAMPLE_RATE_HZ];

    private double nanosToNextSample = NANOS_PER_SAMPLE;
    private int currentCycle;
    private int sampleCounter = 0;
    public int secondsElapsed = 0;

    protected PsgCompare psgCompare;

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
        int delayCycles = toPsgClockCycles(vgmDelayCycles);
        runUntil(delayCycles);
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
        long delayCycles = toPsgClockCycles(vgmDelayCycles);
        if (delayCycles > currentCycle) {
            runUntil(vgmDelayCycles);
        }
        currentCycle -= delayCycles;
    }

    private static int toPsgClockCycles(long vgmDelayCycles) {
        return (int) ((vgmDelayCycles * 1.0 / VgmEmu.VGM_SAMPLE_RATE_HZ) * CLOCK_HZ);
    }

    private void runUntil(int delayCycles) {
        if (delayCycles > currentCycle) {
            long count = delayCycles;
            while (count-- > 0) {
                psg.PSG_Cycle(context);
                updateSampleBuffer();
            }
            currentCycle = delayCycles;
        }
    }

    protected double rawSample;

    protected boolean updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        boolean hasSample = false;
        if (nanosToNextSample < 0) {
            hasSample = true;
            nanosToNextSample += NANOS_PER_SAMPLE;
            rawSample = psg.PSG_GetSample(context);
            rawBuffer[sampleCounter] = rawSample;
            sampleCounter++;
            if (sampleCounter == NUKED_PSG_SAMPLING_HZ) {
                hpf(rawBuffer);
                sampleCounter = 0;
                if (psgCompare != null) {
                    psgCompare.pushData(PsgCompare.PsgType.NUKED, nukedBuffer);
                }
                secondsElapsed++;
            }
        }
        return hasSample;
    }

    private static int SAMPLE_RATIO = NUKED_PSG_SAMPLING_HZ / VgmEmu.VGM_SAMPLE_RATE_HZ;
    private static double TOTAL_SAMPLES_WITH_RATIO = NUKED_PSG_SAMPLING_HZ / SAMPLE_RATIO;
    private static int EXTRA_SAMPLE_POS = (int) Math.ceil(1 / ((TOTAL_SAMPLES_WITH_RATIO / VgmEmu.VGM_SAMPLE_RATE_HZ) - 1));

    private void hpf(double[] rawBuffer) {
        int k = 0, l = 0;
        for (int i = SAMPLE_RATIO; i < rawBuffer.length - 1; i += SAMPLE_RATIO) {
            if (l % EXTRA_SAMPLE_POS == 0) {
                l++;
                continue;
            }
            hpfBuffer[k] = rawBuffer[i + 3] - rawBuffer[i - 2];
            nukedBuffer[k++] = scaleSample(hpfBuffer[k]);
            l++;
//            System.out.println(nukedBuffer[k - 1]);
        }
        byte lastVal = nukedBuffer[k - 1];
        IntStream.range(k, nukedBuffer.length).forEach(i -> nukedBuffer[i] = lastVal);
    }

    public static byte scaleSample(double val) {
        int res = (int) Math.round(val * PSG_MAX_VOLUME);
        return res > Byte.MAX_VALUE ? Byte.MAX_VALUE : (byte) (res < Byte.MIN_VALUE ? Byte.MIN_VALUE : res);
    }
}
