package uk.co.omgdrv.simplevgm.psg;

import uk.co.omgdrv.simplevgm.util.BlipBuffer;

public final class SmsNoise extends SmsOsc
{
    int shifter;
    int feedback;
    int select;

    void reset()
    {
        select = 0;
        shifter = 0x8000;
        feedback = 0x9000;
        super.reset();
    }

    void run(int time, int endTime, int period)
    {
        // TODO: probably also not zero-centered
        final BlipBuffer output = this.output;

        int amp = volume;
        if ((shifter & 1) != 0)
            amp = -amp;

        {
            int delta = amp - lastAmp;
            if (delta != 0)
            {
                lastAmp = amp;
                output.addDelta(time, delta * masterVolume);
            }
        }

        time += delay;
        if (volume == 0)
            time = endTime;

        if (time < endTime)
        {
            final int feedback = this.feedback;
            int shifter = this.shifter;
            int delta = amp * (2 * masterVolume);
            if ((period *= 2) == 0)
                period = 16;

            do
            {
                int changed = shifter + 1;
                shifter = (feedback & -(shifter & 1)) ^ (shifter >> 1);
                if ((changed & 2) != 0) // true if bits 0 and 1 differ
                    output.addDelta(time, delta = -delta);
            }
            while ((time += period) < endTime);

            this.shifter = shifter;
            lastAmp = (delta < 0 ? -volume : volume);
        }
        delay = time - endTime;
    }
}
