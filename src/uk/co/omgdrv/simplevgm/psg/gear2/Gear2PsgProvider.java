package uk.co.omgdrv.simplevgm.psg.gear2;

import uk.co.omgdrv.simplevgm.VgmEmu;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.psg.gear.GearPsgProvider;
import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class Gear2PsgProvider extends GearPsgProvider {

    public static Gear2PsgProvider createInstance() {
        return createInstance(null);
    }

    public static Gear2PsgProvider createInstance(PsgCompare compare) {
        Gear2PsgProvider g = new Gear2PsgProvider();
        g.psg = SN76489Psg.createInstance(NukedPsgProvider.CLOCK_HZ, VgmEmu.VGM_SAMPLE_RATE_HZ);
        g.psgCompare = compare;
        g.type = PsgCompare.PsgType.GEAR2;
        return g;
    }
}