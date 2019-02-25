package uk.co.omgdrv.simplevgm.psg.nuked;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public interface PsgYm7101 {

    class PsgContext {
        int latch;
        int[] volume = new int[4];
        int[] output = new int[4];
        int[] freq = new int[4];
        int[] counter = new int[4];
        int sign;
        int noise_data;
        int noise_reset;
        int noise_update;
        int noise_type;
        int noise;
        int inverse;
        int cycle;
        int debug;
    }

    void PSG_Reset(PsgContext context);

    void PSG_Write(PsgContext context, int data);

    int PSG_Read(PsgContext context);

    void PSG_SetDebugBits(PsgContext context, int data);

    double PSG_GetSample(PsgContext context);

    void PSG_Cycle(PsgContext context);
}
