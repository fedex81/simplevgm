package uk.co.omgdrv.simplevgm;// Common aspects of emulators which use BlipBuffer for sound output
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

import uk.co.omgdrv.simplevgm.util.StereoBuffer;

public abstract class ClassicEmu extends MusicEmu {

    public static final int bufLength = 32;
    protected StereoBuffer buf = new StereoBuffer("SmsApu");

    // derived class can override and mix its own samples here
    protected abstract void mixSamples(byte[] out, int offset, int count);

    // Subclass can also get number of msec to run, and return number of clocks emulated
    protected abstract int runMsec(int msec);

    protected int setSampleRate_(int rate) {
        buf.setSampleRate(rate, 1000 / bufLength);
        return rate;
    }

    public void startTrack(int track) {
        super.startTrack(track);
        buf.clear();
    }

    protected int play_(byte[] out, int count) {
        int pos = 0;
        while (true) {
            int n = buf.readSamples(out, pos, count);
            mixSamples(out, pos, n);

            pos += n;
            count -= n;
            if (count <= 0)
                break;

            if (trackEnded_) {
                java.util.Arrays.fill(out, pos, pos + count, (byte) 0);
                break;
            }

            int clocks = runMsec(bufLength);
            buf.endFrame(clocks);
        }
        return pos;
    }

    protected final int countSamples(int time) {
        return buf.countSamples(time);
    }

    protected void setClockRate(int rate) {
        buf.setClockRate(rate);
    }
}
