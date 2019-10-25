/*
 * Ym2612Nuke
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 07/04/19 16:01
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.co.omgdrv.simplevgm.fm.nukeykt;
/*
 * Ym2612Nuke
 * Copyright (c) 2018-2019 Federico Berti
 * Last modified: 07/04/19 16:01
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import uk.co.omgdrv.simplevgm.fm.MdFmProvider;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TODO check this:
 * https://github.com/nukeykt/Nuked-OPN2/issues/4
 */
public class Ym2612Nuke implements MdFmProvider {
    private IYm3438 ym3438;
    private IYm3438.IYm3438_Type chip;

    private int[][] ym3438_accm = new int[24][2];
    int ym3438_cycles = 0;
    int[] ym3438_sample = new int[2];

    double rateRatioAcc = 0;
    double sampleRateCalcAcc = 0;

    private Queue<Integer> commandQueue = new ConcurrentLinkedQueue<>();
    private AtomicLong queueSize = new AtomicLong();

    private static final int MASTER_CLOCK_HZ = 7_670_442; //MD_NTSC FM CLOCK
    private static final int CLOCK_HZ = MASTER_CLOCK_HZ / 6;
    private static double CYCLE_PER_MS = CLOCK_HZ / 1000.0;
    private final static double rateRatio = FM_CALCS_PER_MS / CYCLE_PER_MS;

    public Ym2612Nuke() {
        this(new IYm3438.IYm3438_Type());
//        this(new Ym3438Jna());
    }

    public Ym2612Nuke(IYm3438.IYm3438_Type chip) {
        this.ym3438 = new Ym3438();
        this.chip = chip;
        this.ym3438.OPN2_SetChipType(IYm3438.ym3438_mode_readmode);
    }

    public Ym2612Nuke(IYm3438 impl) {
        this.ym3438 = impl;
        this.ym3438.OPN2_SetChipType(IYm3438.ym3438_mode_readmode);
    }

    @Override
    public void reset() {
        ym3438.OPN2_Reset(chip);
    }

    @Override
    public int readRegister(int type, int regNumber) {
        return 0;
    }


    @Override
    public int read() {
        return ym3438.OPN2_Read(chip, 0x4000);
    }

    @Override
    public void init(int clock, int rate) {

    }

    @Override
    public void writePort(int addr, int data) {
        commandQueue.offer(addr);
        queueSize.addAndGet(1);
        commandQueue.offer(data);
        queueSize.addAndGet(1);
    }

    private boolean isReadyWrite() {
        boolean isBusyState = (ym3438.OPN2_Read(chip, 0) & FM_STATUS_BUSY_BIT_MASK) > 0;
        boolean isWriteInProgress = ym3438.isWriteAddrEn(chip) || ym3438.isWriteDataEn(chip);
        return !isBusyState && !isWriteInProgress;
    }

    private void spinOnce() {
        if (queueSize.get() > 1 && isReadyWrite()) {
            ym3438.OPN2_Write(chip, commandQueue.poll(), commandQueue.poll());
            queueSize.addAndGet(-2);
        }
        ym3438.OPN2_Clock(chip, ym3438_accm[ym3438_cycles]);
        ym3438_cycles = (ym3438_cycles + 1) % 24;
        if (ym3438_cycles == 0) {
            ym3438_sample[0] = 0;
            ym3438_sample[1] = 0;
            for (int j = 0; j < 24; j++) {
                ym3438_sample[0] += ym3438_accm[j][0];
                ym3438_sample[1] += ym3438_accm[j][1];
            }
            lastL = (lastL + ym3438_sample[0]) >> 1;
            lastR = (lastR + ym3438_sample[1]) >> 1;
        }
    }

    private int lastL = 0;
    private int lastR = 0;

    @Override
    public void update(int[] buf_lr, int offset, int samples441) {
        offset <<= 1;
        sampleRateCalcAcc += samples441 / rateRatio;
        int total = (int) (sampleRateCalcAcc + 1);
        for (int i = 0; i < total; i++) {
            spinOnce();
            rateRatioAcc += rateRatio;
            if (rateRatioAcc > 1) {
                buf_lr[offset++] = lastL << 4;
                buf_lr[offset++] = lastR << 4;
                rateRatioAcc--;
            }
        }
        sampleRateCalcAcc -= total;
    }
}
