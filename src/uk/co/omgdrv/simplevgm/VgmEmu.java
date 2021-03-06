package uk.co.omgdrv.simplevgm;

// Sega Master System, BBC Micro VGM music file emulator
// http://www.slack.net/~ant/

/* Copyright (C) 2003-2007 Shay Green. This module is free software; you
can redistribute it and/or modify it under the terms of the GNU Lesser
General Public License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version. This
module is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
details. You should have received a copy of the GNU Lesser General Public
License along with this module; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA */

import uk.co.omgdrv.simplevgm.fm.MdFmProvider;
import uk.co.omgdrv.simplevgm.fm.nukeykt.Ym2612Nuke;
import uk.co.omgdrv.simplevgm.fm.ym2413.Ym2413Provider;
import uk.co.omgdrv.simplevgm.model.VgmFmProvider;
import uk.co.omgdrv.simplevgm.model.VgmHeader;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.green.SmsApu;
import uk.co.omgdrv.simplevgm.util.Util;

import static uk.co.omgdrv.simplevgm.model.VgmDataFormat.*;

public final class VgmEmu extends ClassicEmu {

    public static final int VGM_SAMPLE_RATE_HZ = 44100;
    public static final int FADE_LENGTH_SEC = 5;

    public static VgmEmu createInstance(VgmPsgProvider apu, VgmFmProvider fm) {
        VgmEmu emu = new VgmEmu();
        if(apu != null) {
            emu.psg = apu;
        }
        if (fm != null) {
            emu.fm = fm;
        }
        return emu;
    }

    // TODO: use custom noise taps if present
    protected int parseHeader(byte[] data)
    {
        vgmHeader = VgmHeader.loadHeader(data);
        if(!VgmHeader.VGM_MAGIC_WORD.equals(vgmHeader.getIdent())) {
            throw new IllegalArgumentException("Unexpected magic word: " + vgmHeader.getIdent());
        }
        if(vgmHeader.getVersion() > VgmHeader.VGM_VERSION){
            System.out.println("VGM version "+vgmHeader.getVersionString()+" ( > 1.50) not supported, " +
                    "cant guarantee correct playback");
        }

        // Data and loop
        this.data = data;
        if (data[data.length - 1] != CMD_END) {
            data = Util.resize(data, data.length + 1);
            data[data.length - 1] = CMD_END;
        }

        // PSG clock rate
        int clockRate = vgmHeader.getSn76489Clk();
        //this needs to be set even if there is no psg
        clockRate = clockRate > 0 ? clockRate : 3579545;
        psg = new SmsApu(); //this needs to be created even if there is no psg
        psgFactor = (int) ((float) psgTimeUnit / vgmRate * clockRate + 0.5);

        // FM clock rate
        fm_clock_rate = vgmHeader.getYm2612Clk();
        if (fm_clock_rate > 0)
        {
            fm = fm == VgmFmProvider.NO_SOUND ? new Ym2612Nuke() : fm;
            buf.setVolume(0.7);
            fm.init(fm_clock_rate, sampleRate());
        }
        else
        {
            fm_clock_rate = vgmHeader.getYm2413Clk();
            if (fm_clock_rate > 0) {
                fm = new Ym2413Provider();
                fm.init(fm_clock_rate, sampleRate());
            }
            buf.setVolume(1.0);
        }

        setClockRate(clockRate);
        psg.setOutput(buf.center(), buf.left(), buf.right());
        pos = vgmHeader.getDataOffset();

        System.out.println(vgmHeader.toString());
        return 1;
    }


// private

    static final int vgmRate = VGM_SAMPLE_RATE_HZ;
    static final double vgmSamplesPerMs = vgmRate/1000d;
    static final int psgTimeBits = 12;
    static final int psgTimeUnit = 1 << psgTimeBits;


    VgmPsgProvider psg = VgmPsgProvider.NO_SOUND;
    VgmFmProvider fm = VgmFmProvider.NO_SOUND;
    VgmHeader vgmHeader;
    int fm_clock_rate;
    int pos;
    byte[] data;
    int delay;
    static int psgFactor;
    final int[] fm_buf_lr = new int[48000 / 10 * 2];
    int fm_pos;
    int dac_disabled; // -1 if disabled
    int pcm_data;
    int pcm_pos;
    int dac_amp;
    boolean loopFlag;

    public void startTrack(int track)
    {
        super.startTrack(track);
        setFade();
        delay = 0;
        pcm_data = pos;
        pcm_pos = pos;
        dac_amp = -1;
        loopFlag = false;

        psg.reset();
        fm.reset();
    }

    private void setFade() {
        int totalSamples = vgmHeader.getNumSamples() + vgmHeader.getLoopSamples();
        int lengthSec = totalSamples / vgmRate;
        setFade(lengthSec - FADE_LENGTH_SEC, FADE_LENGTH_SEC);
    }

    private int toPSGTime(int vgmTime)
    {
        if (psg instanceof SmsApu) {
            return toPSGTimeGreen(vgmTime);
        }
        return vgmTime;
    }


    public static int toPSGTimeGreen(int vgmTime) {
        return (vgmTime * psgFactor + psgTimeUnit / 2) >> psgTimeBits;
    }

    private int toFMTime(int vgmTime)
    {
        return countSamples(toPSGTimeGreen(vgmTime));
    }

    private void runFM(int vgmTime)
    {
        int count = toFMTime(vgmTime) - fm_pos;
        if (count > 0)
        {
            fm.update(fm_buf_lr, fm_pos, count);
            fm_pos += count;
        }
    }

    private void write_pcm(int vgmTime, int amp)
    {
        int blip_time = toPSGTime(vgmTime);
        int old = dac_amp;
        int delta = amp - old;
        dac_amp = amp;
        if (old >= 0) // first write is ignored, to avoid click
            buf.center().addDelta(blip_time, delta * 300);
        else
            dac_amp |= dac_disabled;
    }

    private static boolean endlessLoopFlag = true;
    private long sampleCounter = 0;

    protected int runMsec(int msec)
    {

        final int duration = (int) (vgmSamplesPerMs * msec);

        {
            int sampleCount = toFMTime(duration);
            java.util.Arrays.fill(fm_buf_lr, 0, sampleCount * 2, 0);
            sampleCounter += sampleCount;
        }
        fm_pos = 0;

        int time = delay;
        boolean endOfStream = false;
        while (time < duration && !endOfStream)
        {
            int cmd = CMD_END;
            if (pos < data.length)
                cmd = data[pos++] & 0xFF;
            switch (cmd)
            {
                case CMD_END:
                    //TODO fix sample counting
//                    System.out.println("End command after samples: " + sampleCounter);
                    boolean loopDone = sampleCounter >= vgmHeader.getNumSamples() + vgmHeader.getLoopSamples();
                    endOfStream = !endlessLoopFlag && loopDone;
                    if (vgmHeader.getLoopSamples() == 0 && sampleCounter < vgmHeader.getNumSamples()) {
                        pos = data.length;
                    } else {
                        pos = loopDone ? vgmHeader.getDataOffset() : vgmHeader.getLoopOffset();
                    }
                    break;
                case CMD_DELAY_735:
                    time += 735;
                    break;

                case CMD_DELAY_882:
                    time += 882;
                    break;

                case CMD_GG_STEREO:
                    psg.writeGG(toPSGTime(time), data[pos++] & 0xFF);
                    break;

                case CMD_PSG:
                    psg.writeData(toPSGTime(time), data[pos++] & 0xFF);
                    break;
//                0x51	aa dd	YM2413, write value dd to register aa
                case CMD_YM2413_PORT:
                    runFM(time);
                    int reg1 = data[pos++] & 0xFF;
                    int val1 = data[pos++] & 0xFF;
                    fm.write(Ym2413Provider.FmReg.ADDR_LATCH_REG.ordinal(), reg1);
                    fm.write(Ym2413Provider.FmReg.DATA_REG.ordinal(), val1);
                    break;
                case CMD_YM2612_PORT0:
                    int port = data[pos++] & 0xFF;
                    int val = data[pos++] & 0xFF;
                    if (port == YM2612_DAC_PORT) {
                        write_pcm(time, val);
                    } else {
                        if (port == 0x2B) {
                            dac_disabled = (val >> 7 & 1) - 1;
                            dac_amp |= dac_disabled;
                        }
                        runFM(time);
                        fm.writePort(MdFmProvider.FM_ADDRESS_PORT0, port);
                        fm.writePort(MdFmProvider.FM_DATA_PORT0, val);
                    }
                    break;

                case CMD_YM2612_PORT1:
                    runFM(time);
                    int fmPort = data[pos++] & 0xFF;
                    int fmVal = data[pos++] & 0xFF;
                    fm.writePort(MdFmProvider.FM_ADDRESS_PORT1, fmPort);
                    fm.writePort(MdFmProvider.FM_DATA_PORT1, fmVal);
                    break;

                case CMD_DELAY:
                    time += (data[pos + 1] & 0xFF) * 0x100 + (data[pos] & 0xFF);
                    pos += 2;
                    break;
                case CMD_DATA_BLOCK:
                    if (data[pos++] != CMD_END)
                        logError();
                    int type = data[pos++];
                    long size = Util.getUInt32LE(data, pos);
                    pos += 4;
                    if (type == PCM_BLOCK_TYPE)
                        pcm_data = pos;
                    pos += size;
                    break;

                case CMD_PCM_SEEK:
                    pcm_pos = pcm_data + Util.getUInt32LE(data, pos);
                    pos += 4;
                    break;

                default:
                    switch (cmd & 0xF0) {
                        case CMD_PCM_DELAY:
                            write_pcm(time, data[pcm_pos++] & 0xFF);
                            time += cmd & 0x0F;
                            break;

                        case CMD_SHORT_DELAY:
                            time += (cmd & 0x0F) + 1;
                            break;
                        default:
                            handleUnsupportedCommand(cmd);
                            break;
                    }
            }
        }
        runFM(duration);

        int endTime = toPSGTime(duration);
        delay = time - duration;
        psg.endFrame(endTime);
        if (pos >= data.length || endOfStream)
        {
            setTrackEnded();
            if (pos > data.length)
            {
                pos = data.length;
                logError(); // went past end
            }
        }

        fm_pos = 0;

        return endTime;
    }

    private void handleUnsupportedCommand(int cmd) {
        System.out.println(vgmHeader.getIdent() + vgmHeader.getVersionString() + ", unsupported command: " + Integer.toHexString(cmd));
        switch (cmd & 0xF0) {
            //unsupported one operand
            case 0x30:
            case 0x40:
                pos += 1;
                break;
            //unsupported two operands
            case 0x50:
            case 0xA0:
            case 0xB0:
                pos += 2;
                break;
            //unsupported three operands
            case 0xC0:
            case 0xD0:
                pos += 3;
                break;
            //unsupported four operands
            case 0xE0:
            case 0xF0:
                pos += 4;
                break;
            case 0x90:  //vgm 1.60, dac stream control 0x90 - 0x95
                int subCmd = cmd & 0x7;
                int diff = subCmd < 2 || subCmd == 5 ? 4 : 5;
                diff = subCmd == 3 ? 10 : diff;
                diff = subCmd == 4 ? 1 : diff;
                pos += diff;
                break;
            default:
                System.out.println(String.format("Unexpected command: %s, at position: %s",
                        Integer.toHexString(cmd), Integer.toHexString(pos)));
                logError();
                break;
        }
    }


    protected void mixSamples(byte[] out, int out_off, int count)
    {
        out_off *= 2;
        int in_off = fm_pos;

        while (--count >= 0)
        {
            int s = (out[out_off] << 8) + (out[out_off + 1] & 0xFF);
            s = (s >> 2) + fm_buf_lr[in_off];
            in_off++;
            if ((short) s != s)
                s = (s >> 31) ^ 0x7FFF;
            out[out_off] = (byte) (s >> 8);
            out_off++;
            out[out_off] = (byte) s;
            out_off++;
        }

        fm_pos = in_off;
    }
}