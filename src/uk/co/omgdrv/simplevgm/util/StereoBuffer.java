package uk.co.omgdrv.simplevgm.util;

public final class StereoBuffer
{
    private BlipBuffer[] bufs = new BlipBuffer[3];

    // Same behavior as in BlipBuffer unless noted

    public StereoBuffer()
    {
        for (int i = bufs.length; --i >= 0; )
        {
            bufs[i] = new BlipBuffer();
        }
    }

    public void setSampleRate(int rate, int msec)
    {
        for (int i = bufs.length; --i >= 0; )
        {
            bufs[i].setSampleRate(rate, msec);
        }
    }

    public void setClockRate(int rate)
    {
        for (int i = bufs.length; --i >= 0; )
        {
            bufs[i].setClockRate(rate);
        }
    }

    public int clockRate()
    {
        return bufs[0].clockRate();
    }

    public int countSamples(int time)
    {
        return bufs[0].countSamples(time);
    }

    public void clear()
    {
        for (int i = bufs.length; --i >= 0; )
        {
            bufs[i].clear();
        }
    }

    public void setVolume(double v)
    {
        for (int i = bufs.length; --i >= 0; )
        {
            bufs[i].setVolume(v);
        }
    }

    // The three channels that are mixed together
    // left output  = left  + center
    // right output = right + center
    public BlipBuffer center()
    {
        return bufs[2];
    }

    public BlipBuffer left()
    {
        return bufs[0];
    }

    public BlipBuffer right()
    {
        return bufs[1];
    }

    public void endFrame(int time)
    {
        for (int i = bufs.length; --i >= 0; )
        {
            bufs[i].endFrame(time);
        }
    }

    public int samplesAvail()
    {
        return bufs[2].samplesAvail() << 1;
    }

    // Output is in stereo, so count must always be a multiple of 2
    public int readSamples(byte[] out, int start, int count)
    {
        assert (count & 1) == 0;

        final int avail = samplesAvail();
        if (count > avail)
            count = avail;

        if ((count >>= 1) > 0)
        {
            // TODO: optimize for mono case

            // calculate center in place
            final int[] mono = bufs[2].buf;
            {
                int accum = bufs[2].accum;
                int i = 0;
                do
                {
                    mono[i] = (accum += mono[i] - (accum >> 9));
                }
                while (++i < count);
                bufs[2].accum = accum;
            }

            // calculate left and right
            for (int ch = 2; --ch >= 0; )
            {
                // add right and output
                final int[] buf = bufs[ch].buf;
                int accum = bufs[ch].accum;
                int pos = (start + ch) << 1;
                int i = 0;
                do
                {
                    int s = ((accum += buf[i] - (accum >> 9)) + mono[i]) >> 15;

                    // clamp to 16 bits
                    if ((short) s != s)
                        s = (s >> 24) ^ 0x7FFF;

                    // write as big endian
                    out[pos] = (byte) (s >> 8);
                    out[pos + 1] = (byte) s;
                    pos += 4;
                }
                while (++i < count);
                bufs[ch].accum = accum;
            }

            for (int i = bufs.length; --i >= 0; )
            {
                bufs[i].removeSamples(count);
            }
        }
        return count << 1;
    }
}
