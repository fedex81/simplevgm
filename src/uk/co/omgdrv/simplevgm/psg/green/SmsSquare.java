package uk.co.omgdrv.simplevgm.psg.green;

import uk.co.omgdrv.simplevgm.util.BlipBuffer;

public final class SmsSquare extends SmsOsc
{
    int period;
    int phase;

    void reset()
    {
        period = 0;
        phase = 0;
        super.reset();
    }

    void run(int time, int endTime)
    {
        final int period = this.period;

        int amp = volume;
        if (period > 128)
            amp = (amp * 2) & -phase;

        {
            int delta = amp - lastAmp;
            if (delta != 0)
            {
                lastAmp = amp;
                output.addDelta(time, delta * masterVolume);
            }
        }

        time += delay;
        delay = 0;
        if (period != 0)
        {
            if (time < endTime)
            {
                if (volume == 0 || period <= 128) // ignore 16kHz and higher
                {
                    // keep calculating phase
                    int count = (endTime - time + period - 1) / period;
                    phase = (phase + count) & 1;
                    time += count * period;
                }
                else
                {
                    final BlipBuffer output = this.output;
                    int delta = (amp - volume) * (2 * masterVolume);
                    do
                    {
                        output.addDelta(time, delta = -delta);
                    }
                    while ((time += period) < endTime);

                    phase = (delta >= 0 ? 1 : 0);
                    lastAmp = volume * (phase << 1);
                }
            }
            delay = time - endTime;
        }
    }
}
