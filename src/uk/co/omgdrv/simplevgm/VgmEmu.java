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

import uk.co.omgdrv.simplevgm.fm.YM2612;
import uk.co.omgdrv.simplevgm.model.FmProvider;
import uk.co.omgdrv.simplevgm.model.PsgProvider;
import uk.co.omgdrv.simplevgm.model.VgmHeader;
import uk.co.omgdrv.simplevgm.psg.SmsApu;
import uk.co.omgdrv.simplevgm.util.Util;

import static uk.co.omgdrv.simplevgm.model.VgmDataFormat.*;

public final class VgmEmu extends ClassicEmu {


    public static VgmEmu createInstance(PsgProvider apu){
        VgmEmu emu = new VgmEmu();
        if(apu != null) {
            emu.psg = apu;
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
        //TODO should disable PSG?
        if (clockRate == 0) {
            clockRate = 3579545;
        }
        psgFactor = (int) ((float) psgTimeUnit / vgmRate * clockRate + 0.5);

        // FM clock rate
        fm_clock_rate = vgmHeader.getYm2612Clk();
        fm = null;
        if (fm_clock_rate != 0)
        {
            fm = new YM2612();
            buf.setVolume(0.7);
            fm.init(fm_clock_rate, sampleRate());
        }
        else
        {
            buf.setVolume(1.0);
        }

        setClockRate(clockRate);
        psg.setOutput(buf.center(), buf.left(), buf.right());
        pos = vgmHeader.getDataOffset();

        return 1;
    }

// private

    static final int vgmRate = 44100;
    static final int psgTimeBits = 12;
    static final int psgTimeUnit = 1 << psgTimeBits;


    PsgProvider psg = new SmsApu();
    FmProvider fm;
    VgmHeader vgmHeader;
    int fm_clock_rate;
    int pos;
    byte[] data;
    int delay;
    int psgFactor;
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

        delay = 0;
        pcm_data = pos;
        pcm_pos = pos;
        dac_amp = -1;
        loopFlag = false;

        psg.reset();
        if (fm != null)
            fm.reset();
    }

    private int toPSGTime(int vgmTime)
    {
        return (vgmTime * psgFactor + psgTimeUnit / 2) >> psgTimeBits;
    }

    private int toFMTime(int vgmTime)
    {
        return countSamples(toPSGTime(vgmTime));
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

    protected int runMsec(int msec)
    {
        final int duration = vgmRate / 100 * msec / 10;

        {
            int sampleCount = toFMTime(duration);
            java.util.Arrays.fill(fm_buf_lr, 0, sampleCount * 2, 0);
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
                    endOfStream = true;
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

                case CMD_YM2612_PORT0:
                    if (fm != null)
                    {
                        int port = data[pos++] & 0xFF;
                        int val = data[pos++] & 0xFF;
                        if (port == YM2612_DAC_PORT)
                        {
                            write_pcm(time, val);
                        }
                        else
                        {
                            if (port == 0x2B)
                            {
                                dac_disabled = (val >> 7 & 1) - 1;
                                dac_amp |= dac_disabled;
                            }
                            runFM(time);
                            fm.write0(port, val);
                        }
                    }
                    break;

                case CMD_YM2612_PORT1:
                    if (fm != null)
                    {
                        runFM(time);
                        int port = data[pos++] & 0xFF;
                        fm.write1(port, data[pos++] & 0xFF);
                    }
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
                    switch (cmd & 0xF0)
                    {
                        case CMD_PCM_DELAY:
                            write_pcm(time, data[pcm_pos++] & 0xFF);
                            time += cmd & 0x0F;
                            break;

                        case CMD_SHORT_DELAY:
                            time += (cmd & 0x0F) + 1;
                            break;

                        //unsupported two operands
                        case 0x30:
                        case 0x40:
                            pos += 1;
                            System.out.println(vgmHeader.getIdent() + vgmHeader.getVersionString() +", unsupported command: " + Integer.toHexString(cmd));
                            break;

                        //unsupported two operands
                        case 0x50:
                        case 0xA0:
                        case 0xB0:
                            System.out.println(vgmHeader.getIdent() + vgmHeader.getVersionString() +", unsupported command: " + Integer.toHexString(cmd));
                            pos += 2;
                            break;
                        //unsupported three operands
                        case 0xC0:
                        case 0xD0:
                            System.out.println(vgmHeader.getIdent() + vgmHeader.getVersionString() +", unsupported command: " + Integer.toHexString(cmd));
                            pos += 3;
                            break;
                        //unsupported four operands
                        case 0xE0:
                        case 0xF0:
                            System.out.println(vgmHeader.getIdent() + vgmHeader.getVersionString() +", unsupported command: " + Integer.toHexString(cmd));
                            pos += 4;
                            break;
                        default:
                            System.out.println(String.format("Unexpected command: %s, at position: %s", Integer.toHexString(cmd), Integer.toHexString(pos)));
                            logError();
                            break;
                    }
            }
        }

        if (fm != null)
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

    protected void mixSamples(byte[] out, int out_off, int count)
    {
        if (fm == null)
            return;

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
