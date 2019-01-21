package uk.co.omgdrv.simplevgm.model;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public interface VgmFmProvider {

    int reset();

    int init(int clock, int rate);

    void update(int[] buf_lr, int offset, int end);

    void write0(int addr, int data);

    void write1(int addr, int data);

    VgmFmProvider NO_SOUND = new VgmFmProvider() {
        @Override
        public int reset() {
            return 0;
        }

        @Override
        public int init(int Clock, int Rate) {
            return 0;
        }

        @Override
        public void update(int[] buf_lr, int offset, int end) {

        }

        @Override
        public void write0(int addr, int data) {

        }

        @Override
        public void write1(int addr, int data) {

        }
    };
}
