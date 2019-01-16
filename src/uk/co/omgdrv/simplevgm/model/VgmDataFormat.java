package uk.co.omgdrv.simplevgm.model;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class VgmDataFormat {
    public static final int CMD_GG_STEREO = 0x4F;
    public static final int CMD_PSG = 0x50;
    public static final int CMD_YM2612_PORT0 = 0x52;
    public static final int CMD_YM2612_PORT1 = 0x53;
    public static final int CMD_DELAY = 0x61;
    public static final int CMD_DELAY_735 = 0x62;
    public static final int CMD_DELAY_882 = 0x63;
    public static final int CMD_END = 0x66;
    public static final int CMD_DATA_BLOCK = 0x67;
    public static final int CMD_SHORT_DELAY = 0x70;
    public static final int CMD_PCM_DELAY = 0x80;
    public static final int CMD_PCM_SEEK = 0xE0;
    public static final int YM2612_DAC_PORT = 0x2A;
    public static final int PCM_BLOCK_TYPE = 0x00;
}
