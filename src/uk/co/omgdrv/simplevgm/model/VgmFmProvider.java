package uk.co.omgdrv.simplevgm.model;

import uk.co.omgdrv.simplevgm.VgmEmu;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public interface VgmFmProvider {

    double FM_CALCS_PER_MS = VgmEmu.VGM_SAMPLE_RATE_HZ / 1000.0;

    void reset();

    void init(int clock, int rate);

    void update(int[] buf_lr, int offset, int end);

    default int readRegister(int type, int regNumber) {
        throw new RuntimeException("Invalid");
    }

    default void writePort(int addr, int data) {
        throw new RuntimeException("Invalid");
    }

    //single port
    default void write(int addr, int data) {
        throw new RuntimeException("Invalid");
    }

    default int read() {
        throw new RuntimeException("Invalid");
    }

    VgmFmProvider NO_SOUND = new VgmFmProvider() {
        @Override
        public void reset() {

        }

        @Override
        public void init(int Clock, int Rate) {

        }

        @Override
        public void update(int[] buf_lr, int offset, int end) {

        }

        @Override
        public void writePort(int addr, int data) {

        }

        @Override
        public void write(int addr, int data) {

        }
    };
}
