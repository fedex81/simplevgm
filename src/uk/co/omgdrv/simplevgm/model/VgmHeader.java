package uk.co.omgdrv.simplevgm.model;

import uk.co.omgdrv.simplevgm.util.Util;

import java.util.Arrays;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class VgmHeader {

    public static final String VGM_MAGIC_WORD = "Vgm ";
    public static final int VGM_VERSION = 0x150;

    String ident;
    int eofOffset;
    int version;
    int dataOffset;
    int loopOffset;

    int sn76489Clk;
    int ym2612Clk;

    int rate;

    byte[] ym2413_clk = new byte[4];
    byte[] gd3_offset = new byte[4];
    byte[] num_samples = new byte[4];

    byte[] loop_samples = new byte[4];

    byte[] sn76489_fb = new byte[2];
    byte[] sn76489_shift = new byte[1];
    byte[] sn76489_flags = new byte[1];

    byte[] ym2151_clk = new byte[4];
    byte[] sega_pcm_clk = new byte[4];
    byte[] sega_pcm_reg = new byte[4];

    private VgmHeader() {
    }

    public static VgmHeader loadHeader(byte[] data) {
        VgmHeader v = new VgmHeader();
        int index = 0;
        v.ident = new String(data, index, 4);
        index += 4;
        v.eofOffset = Util.getUInt32LE(data[index], data[index + 1], data[index + 2], data[index + 3]);
        index += 4;
        v.version = Util.getUInt32LE(data[index], data[index + 1], data[index + 2], data[index + 3]);
        index += 4;

        v.sn76489Clk = Util.getUInt32LE(data[index], data[index + 1], data[index + 2], data[index + 3]);
        index += 4;

        v.ym2413_clk = Arrays.copyOfRange(data, index, index + v.ym2413_clk.length);
        index += v.ym2413_clk.length;

        v.gd3_offset = Arrays.copyOfRange(data, index, index + v.gd3_offset.length);
        index += v.gd3_offset.length;

        v.num_samples = Arrays.copyOfRange(data, index, index + v.num_samples.length);
        index += v.num_samples.length;

        v.loopOffset = Util.getUInt32LE(data[index], data[index + 1], data[index + 2], data[index + 3]);
        v.loopOffset = v.loopOffset == 0 ? 0 : v.loopOffset + 0x1C;
        index += 4;

        v.loop_samples = Arrays.copyOfRange(data, index, index + v.loop_samples.length);
        index += v.loop_samples.length;
        v.rate = Util.getUInt32LE(data[index], data[index + 1], data[index + 2], data[index + 3]);
        index += 4;
        v.sn76489_fb = Arrays.copyOfRange(data, index, index + v.sn76489_fb.length);
        index += v.sn76489_fb.length;
        v.sn76489_shift = Arrays.copyOfRange(data, index, index + v.sn76489_shift.length);
        index += v.sn76489_shift.length;
        v.sn76489_flags = Arrays.copyOfRange(data, index, index + v.sn76489_flags.length);
        index += v.sn76489_flags.length;

        v.ym2612Clk = Util.getUInt32LE(data[index], data[index + 1], data[index + 2], data[index + 3]);
        index += 4;

        v.ym2151_clk = Arrays.copyOfRange(data, index, index + v.ym2151_clk.length);
        index += v.ym2151_clk.length;

        v.dataOffset = Util.getUInt32LE(data[index], data[index + 1], data[index + 2], data[index + 3]);
        v.dataOffset = v.dataOffset == 0 ? 0x40 : v.dataOffset + 0x34;
        index += 4;

        v.sega_pcm_clk = Arrays.copyOfRange(data, index, index + v.sega_pcm_clk.length);
        index += v.sega_pcm_clk.length;
        v.sega_pcm_reg = Arrays.copyOfRange(data, index, index + v.sega_pcm_reg.length);
        return v;
    }

    public String getVersionString() {
        return Integer.toString((version >> 8) & 0xFF, 16) + "." + Integer.toString(version & 0xFF, 16);
    }

    public int getVersion() {
        return version;
    }

    public String getIdent() {
        return ident;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public int getEofOffset() {
        return eofOffset;
    }

    public int getLoopOffset() {
        return loopOffset;
    }

    public int getSn76489Clk() {
        return sn76489Clk;
    }

    public int getYm2612Clk() {
        return ym2612Clk;
    }

    public int getRate() {
        return rate;
    }
}
