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

    public static final short[] ym2413_inst = {
            /* MULT  MULT modTL DcDmFb AR/DR AR/DR SL/RR SL/RR */
            /*   0     1     2     3     4     5     6    7    */
            /* These YM2413(OPLL) patch dumps are done via audio analysis (and a/b testing?) from Jarek and are known to be inaccurate */
            0x49, 0x4c, 0x4c, 0x12, 0x00, 0x00, 0x00, 0x00,  //0
            0x61, 0x61, 0x1e, 0x17, 0xf0, 0x78, 0x00, 0x17,  //1
            0x13, 0x41, 0x1e, 0x0d, 0xd7, 0xf7, 0x13, 0x13,  //2
            0x13, 0x01, 0x99, 0x04, 0xf2, 0xf4, 0x11, 0x23,  //3
            0x21, 0x61, 0x1b, 0x07, 0xaf, 0x64, 0x40, 0x27,  //4
            0x22, 0x21, 0x1e, 0x06, 0xf0, 0x75, 0x08, 0x18,  //5
            0x31, 0x22, 0x16, 0x05, 0x90, 0x71, 0x00, 0x13,  //6
            0x21, 0x61, 0x1d, 0x07, 0x82, 0x80, 0x10, 0x17,  //7
            0x23, 0x21, 0x2d, 0x16, 0xc0, 0x70, 0x07, 0x07,  //8
            0x61, 0x61, 0x1b, 0x06, 0x64, 0x65, 0x10, 0x17,  //9
            0x61, 0x61, 0x0c, 0x18, 0x85, 0xf0, 0x70, 0x07,  //A
            0x23, 0x01, 0x07, 0x11, 0xf0, 0xa4, 0x00, 0x22,  //B
            0x97, 0xc1, 0x24, 0x07, 0xff, 0xf8, 0x22, 0x12,  //C
            0x61, 0x10, 0x0c, 0x05, 0xf2, 0xf4, 0x40, 0x44,  //D
            0x01, 0x01, 0x55, 0x03, 0xf3, 0x92, 0xf3, 0xf3,  //E
            0x61, 0x41, 0x89, 0x03, 0xf1, 0xf4, 0xf0, 0x13,  //F

            /* drum instruments definitions */
            /* MULTI MULTI modTL  xxx  AR/DR AR/DR SL/RR SL/RR */
            /*   0     1     2     3     4     5     6    7    */
            /* Drums dumped from the VRC7 using debug mode, these are likely also correct for ym2413(OPLL) but need verification */
            0x01, 0x01, 0x18, 0x0f, 0xdf, 0xf8, 0x6a, 0x6d,/* BD */
            0x01, 0x01, 0x00, 0x00, 0xc8, 0xd8, 0xa7, 0x68,/* HH, SD */
            0x05, 0x01, 0x00, 0x00, 0xf8, 0xaa, 0x59, 0x55  /* TOM, TOP CYM */
    };

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
        Emu2413.default_inst = ym2413_inst;
        Emu2413.OPLL_init();
        opll = Emu2413.OPLL_new();
    }


    @Override
    public void update(int[] buf_lr, int offset, int samples441) {
        offset <<= 1; //stereo
        sampleRateCalcAcc += samples441 / rateRatio;
        int total = (int) sampleRateCalcAcc + 1; //needed to match the offsets
        for (int i = 0; i < total; i++) {
            int res = Emu2413.OPLL_calc(opll);
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
