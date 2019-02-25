package uk.co.omgdrv.simplevgm.psg.green;// Sega Master System SN76489 PSG sound chip emulator
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

import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.util.BlipBuffer;

public final class SmsApu implements VgmPsgProvider
{
    int lastTime;
    int latch;
    int noiseFeedback;
    int loopedFeedback;

    static final int oscCount = 4;
    final SmsSquare[] squares = new SmsSquare[3];
    final SmsNoise noise = new SmsNoise();
    final SmsOsc[] oscs = new SmsOsc[oscCount];

    static final int[] noisePeriods = {0x100, 0x200, 0x400};

    private void runUntil(int endTime)
    {
        if (endTime > lastTime)
        {
            // run oscillators
            for (int i = oscCount; --i >= 0; )
            {
                SmsOsc osc = oscs[i];
                if (osc.output != null)
                {
                    if (i < 3)
                    {
                        squares[i].run(lastTime, endTime);
                    }
                    else
                    {
                        int period = squares[2].period;
                        if (noise.select < 3)
                            period = noisePeriods[noise.select];
                        noise.run(lastTime, endTime, period);
                    }
                }
            }

            lastTime = endTime;
        }
    }

    public SmsApu()
    {
        for (int i = 0; i < 3; i++)
        {
            oscs[i] = squares[i] = new SmsSquare();
        }
        oscs[3] = noise;
    }

    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right)
    {
        for (int i = 0; i < oscCount; i++)
        {
            SmsOsc osc = oscs[i];
            osc.outputs[1] = right;
            osc.outputs[2] = left;
            osc.outputs[3] = center;
            osc.output = osc.outputs[osc.outputSelect];
        }
    }

    public void reset(int feedback, int noiseWidth)
    {
        lastTime = 0;
        latch = 0;

        // convert to "Galios configuration"
        loopedFeedback = 1 << (noiseWidth - 1);
        noiseFeedback = 0;
        while (--noiseWidth >= 0)
        {
            noiseFeedback = (noiseFeedback << 1) | (feedback & 1);
            feedback >>= 1;
        }

        squares[0].reset();
        squares[1].reset();
        squares[2].reset();
        noise.reset();
    }

    public void reset()
    {
        reset(0x0009, 16);
    }

    public void writeGG(int time, int data)
    {
        runUntil(time);

        for (int i = 0; i < oscCount; i++)
        {
            SmsOsc osc = oscs[i];
            int flags = data >> i;
            BlipBuffer oldOutput = osc.output;
            osc.outputSelect = (flags >> 3 & 2) | (flags & 1);
            osc.output = osc.outputs[osc.outputSelect];
            if (osc.output != oldOutput && osc.lastAmp != 0)
            {
                if (oldOutput != null)
                    oldOutput.addDelta(time, -osc.lastAmp * SmsOsc.masterVolume);
                osc.lastAmp = 0;
            }
        }
    }

    static final int[] volumes = {
            64, 50, 39, 31, 24, 19, 15, 12, 9, 7, 5, 4, 3, 2, 1, 0
    };

    public void writeData(int time, int data)
    {
        runUntil(time);

        if ((data & 0x80) != 0)
            latch = data;

        int index = (latch >> 5) & 3;
        if ((latch & 0x10) != 0)
        {
            oscs[index].volume = volumes[data & 15];
        }
        else if (index < 3)
        {
            SmsSquare sq = squares[index];
            if ((data & 0x80) != 0)
                sq.period = (sq.period & 0xFF00) | (data << 4 & 0x00FF);
            else
                sq.period = (sq.period & 0x00FF) | (data << 8 & 0x3F00);
        }
        else
        {
            noise.select = data & 3;
            noise.feedback = ((data & 0x04) != 0) ? noiseFeedback : loopedFeedback;
            noise.shifter = 0x8000;
        }
    }

    public void endFrame(int endTime)
    {
        if (endTime > lastTime)
            runUntil(endTime);

        lastTime -= endTime;
    }
}
