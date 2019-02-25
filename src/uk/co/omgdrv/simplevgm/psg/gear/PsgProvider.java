package uk.co.omgdrv.simplevgm.psg.gear;

import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2018
 */
public interface PsgProvider {

    int GEAR_CLOCK_HZ = NukedPsgProvider.CLOCK_HZ / 32;

    int PSG_OUTPUT_SAMPLE_SIZE = 8;
    int PSG_OUTPUT_CHANNELS = 1;

    static PsgProvider createInstance(int sampleRate) {
        PsgProvider psgProvider = new SN76496(GEAR_CLOCK_HZ, sampleRate);
        psgProvider.init();
        return psgProvider;
    }

    void init();

    void write(int data);

    void output(byte[] output);

    void output(byte[] output, int offset, int end);

    void reset();

    PsgProvider NO_SOUND = new PsgProvider() {

        @Override
        public void init() {

        }

        @Override
        public void write(int data) {

        }

        @Override
        public void output(byte[] ouput) {

        }

        @Override
        public void output(byte[] output, int offset, int end) {

        }

        @Override
        public void reset() {

        }
    };
}
