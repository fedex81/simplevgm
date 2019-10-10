package uk.co.omgdrv.simplevgm.fm.ym2413;

import uk.co.omgdrv.simplevgm.model.VgmFmProvider;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 *
 * F = (49716 * Fnum) / (2^19 - (octave-1)) ??
 *
 */
public class Ym2413Provider implements VgmFmProvider {

    private OPLL opll;

    // Input clock
    private static final int CLOCK_HZ = 3579545;
    public static final double FM_RATE = 49716.0;

    static double ymRatePerMs = FM_RATE / 1000.0;
    final static double rateRatio = FM_CALCS_PER_MS / ymRatePerMs;
    double rateRatioAcc = 0;
    double sampleRateCalcAcc = 0;

    public enum FmReg {ADDR_LATCH_REG, DATA_REG}

    @Override
    public void reset() {
        for (int i = 0x10; i < 0x40; i++) {
            Emu2413.OPLL_writeIO(opll, 0, i);
            Emu2413.OPLL_writeIO(opll, 1, 0);
        }
        Emu2413.OPLL_reset_patch(opll);
        Emu2413.OPLL_reset(opll);
    }

    @Override
    public void init(int clock, int rate) {
        Emu2413.OPLL_init();
        opll = Emu2413.OPLL_new();
    }


    static int AUDIO_SCALE_BITS = 3;

    @Override
    public void update(int[] buf_lr, int offset, int samples441) {
        offset <<= 1; //stereo
        sampleRateCalcAcc += samples441 / rateRatio;
        int total = (int) sampleRateCalcAcc + 1; //needed to match the offsets
        for (int i = 0; i < total; i++) {
            int res = Emu2413.OPLL_calc(opll) << AUDIO_SCALE_BITS;
            rateRatioAcc += rateRatio;
            if (rateRatioAcc > 1) {
                buf_lr[offset++] = res;
                buf_lr[offset++] = res;
                rateRatioAcc--;
            }
        }
        sampleRateCalcAcc -= total;
    }

    @Override
    public void write(int addr, int data) {
        switch (FmReg.values()[addr]) {
            case ADDR_LATCH_REG:
                Emu2413.OPLL_writeIO(opll, 0, data);
                break;
            case DATA_REG:
                Emu2413.OPLL_writeIO(opll, 1, data);
                break;
        }
    }
}
