package uk.co.omgdrv.simplevgm.psg;

import uk.co.omgdrv.simplevgm.util.BlipBuffer;

public class SmsOsc
{
    static final int masterVolume = (int) (0.40 * 65536 / 128);

    BlipBuffer output;
    int outputSelect;
    final BlipBuffer[] outputs = new BlipBuffer[4];
    int delay;
    int lastAmp;
    int volume;

    void reset()
    {
        delay = 0;
        lastAmp = 0;
        volume = 0;
        outputSelect = 3;
        output = outputs[outputSelect];
    }
}
