package uk.co.omgdrv.simplevgm.model;

import uk.co.omgdrv.simplevgm.util.Util;

import java.util.Objects;

import static uk.co.omgdrv.simplevgm.model.VgmHeader.Field.*;

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
    public static final int DEFAULT_DATA_OFFSET = 0x40;

    enum Field {
        IDENT(0),
        EOF_OFFSET(4),
        VERSION(8),
        SN76489_CLK(12),
        YM2413_CLK(16),
        GD3_OFFSET(20),
        NUM_SAMPLES(24),
        LOOP_OFFSET(28),
        LOOP_SAMPLES(32),
        RATE(36),
        SN76489_FB(40, 2),
        SN76489_SHIFT(42, 1),
        SN76489_FLAGS(43, 1),
        YM2612_CLK(44),
        DATA_OFFSET(52),
        ;

        private int position;
        private int size;

        Field(int pos){
            this(pos, 4); //4bytes
        }

        Field(int pos, int size){
            this.position = pos;
            this.size = size;
        }

        public int getPosition() {
            return position;
        }

        public int getSize() {
            return size;
        }
    }

    private String ident;
    private String versionString;

    private int eofOffset;
    private int version;
    private int dataOffset;

    private int loopOffset;
    private int loopSamples;

    private int sn76489Clk;
    private int ym2612Clk;
    private int ym2413Clk;

    private int rate;

    private int gd3Offset;
    private int numSamples;

    private int sn76489Fb;
    private int sn76489Shift;
    private int sn76489Flags;

    private Gd3Tag gd3 = Gd3Tag.NO_TAG;


    private VgmHeader() {
    }

    public static VgmHeader loadHeader(byte[] data) {
        VgmHeader v = new VgmHeader();
        v.ident = new String(data, IDENT.getPosition(), IDENT.getSize());
        v.eofOffset = getIntValue(data, EOF_OFFSET);
        v.version = getIntValue(data, VERSION);
        v.versionString = toVersionString(v.version);
        v.sn76489Clk = getIntValue(data, SN76489_CLK);
        v.ym2413Clk = getIntValue(data, YM2413_CLK);
        v.gd3Offset =  getIntValue(data, GD3_OFFSET);
        v.numSamples=  getIntValue(data, NUM_SAMPLES);
        v.loopOffset=  getIntValue(data, LOOP_OFFSET);
        v.loopOffset = v.loopOffset == 0 ? 0 : v.loopOffset + LOOP_OFFSET.getPosition();
        v.loopSamples=  getIntValue(data, LOOP_SAMPLES);
        v.rate=  getIntValue(data, RATE);
        v.sn76489Fb = Util.getUInt32LE(data[SN76489_FB.getPosition()], data[SN76489_FB.getPosition() + 1]);
        v.sn76489Shift = Util.getUInt32LE(data[SN76489_SHIFT.getPosition()]);
        v.sn76489Flags = Util.getUInt32LE(data[SN76489_FLAGS.getPosition()]);
        v.ym2612Clk=  getIntValue(data, YM2612_CLK);
        if(v.ym2612Clk == 0 && v.version <= 0x101){
//            v.ym2612Clk = v.ym2413Clk;
        }
        v.dataOffset=  getIntValue(data, DATA_OFFSET);
        v.dataOffset = v.dataOffset == 0 ? DEFAULT_DATA_OFFSET : v.dataOffset + DATA_OFFSET.getPosition();
        if (v.gd3Offset > 0) {
            v.gd3 = Gd3Tag.parseTag(data, v.gd3Offset + 0x14);
        }
        return v;
    }

    public static String toVersionString(int version) {
        String major = Integer.toString((version >> 8) & 0xFF, 16);
        int minorVal = version & 0xFF;
        String minor = (minorVal < 10 ? "0" : "" ) + Integer.toString(minorVal, 16);
        return major + "." + minor;
    }

    private static int getIntValue(byte[] data, Field field){
        if(field.size == 4) {
            return  Util.getUInt32LE(data, field.getPosition());
        }
        return -1;
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

    public int getYm2413Clk() {
        return ym2413Clk;
    }

    public int getRate() {
        return rate;
    }

    public int getLoopSamples() {
        return loopSamples;
    }

    public int getGd3Offset() {
        return gd3Offset;
    }


    public int getNumSamples() {
        return numSamples;
    }

    public int getSn76489Fb() {
        return sn76489Fb;
    }

    public int getSn76489Flags() {
        return sn76489Flags;
    }

    public int getSn76489Shift() {
        return sn76489Shift;
    }

    @Override
    public String toString() {
        String str = "VgmHeader{" +
                "ident='" + ident + '\'' +
                ", eofOffset=" + eofOffset +
                ", version=" + versionString +
                ", dataOffset=" + dataOffset +
                ", loopOffset=" + loopOffset +
                ", loopSamples=" + loopSamples +
                ", sn76489Clk=" + sn76489Clk +
                ", ym2612Clk=" + ym2612Clk +
                ", rate=" + rate +
                ", gd3Offset=" + gd3Offset +
                ", numSamples=" + numSamples +
                ", sn76489Fb=" + sn76489Fb +
                ", sn76489Shift=" + sn76489Shift +
                ", sn76489Flags=" + sn76489Flags +
                '}';
        str += Gd3Tag.NO_TAG != gd3 ? gd3.toDataString() : "";
        return str;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VgmHeader vgmHeader = (VgmHeader) o;
        return eofOffset == vgmHeader.eofOffset &&
                version == vgmHeader.version &&
                dataOffset == vgmHeader.dataOffset &&
                loopOffset == vgmHeader.loopOffset &&
                loopSamples == vgmHeader.loopSamples &&
                sn76489Clk == vgmHeader.sn76489Clk &&
                ym2612Clk == vgmHeader.ym2612Clk &&
                rate == vgmHeader.rate &&
                gd3Offset == vgmHeader.gd3Offset &&
                numSamples == vgmHeader.numSamples &&
                sn76489Fb == vgmHeader.sn76489Fb &&
                sn76489Shift == vgmHeader.sn76489Shift &&
                sn76489Flags == vgmHeader.sn76489Flags &&
                Objects.equals(ident, vgmHeader.ident) &&
                Objects.equals(versionString, vgmHeader.versionString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ident, versionString, eofOffset, version, dataOffset, loopOffset, loopSamples, sn76489Clk, ym2612Clk, rate, gd3Offset, numSamples, sn76489Fb, sn76489Shift, sn76489Flags);
    }
}
