package uk.co.omgdrv.simplevgm.util;

import uk.me.berndporr.iirj.*;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class FilterHelper {

    public static double CHEBY_RIPPLE_DB = 1.0;
    public static int FILTER_ORDER = 2;

    public enum FilterType {
        BESSEL, BUTTERWORTH, CHEBYSHEV1, CHEBYSHEV2
    }

    public static Cascade setupBandPass(FilterType type, int order, double sampleRate, double centerFrequency,
                                        double widthFrequency) {
        switch (type) {
            case BESSEL:
                Bessel b = new Bessel();
                b.bandPass(order, sampleRate, centerFrequency, widthFrequency);
                return b;
            case BUTTERWORTH:
                Butterworth bu = new Butterworth();
                bu.bandPass(order, sampleRate, centerFrequency, widthFrequency);
                return bu;
            case CHEBYSHEV1:
                ChebyshevI c1 = new ChebyshevI();
                c1.bandPass(order, sampleRate, centerFrequency, widthFrequency, CHEBY_RIPPLE_DB);
                return c1;
            case CHEBYSHEV2:
                ChebyshevII c2 = new ChebyshevII();
                c2.bandPass(order, sampleRate, centerFrequency, widthFrequency, CHEBY_RIPPLE_DB);
                return c2;
            default:
                break;
        }
        return null;
    }

    public static Cascade setupHighPass(FilterType type, int order, double sampleRate, double cutoffFrequency) {
        switch (type) {
            case BESSEL:
                Bessel b = new Bessel();
                b.highPass(order, sampleRate, cutoffFrequency);
                return b;
            case BUTTERWORTH:
                Butterworth bu = new Butterworth();
                bu.highPass(order, sampleRate, cutoffFrequency);
                return bu;
            case CHEBYSHEV1:
                ChebyshevI c1 = new ChebyshevI();
                c1.highPass(order, sampleRate, cutoffFrequency, CHEBY_RIPPLE_DB);
                return c1;
            case CHEBYSHEV2:
                ChebyshevII c2 = new ChebyshevII();
                c2.highPass(order, sampleRate, cutoffFrequency, CHEBY_RIPPLE_DB);
                return c2;
            default:
                break;
        }
        return null;
    }

    public static Cascade setupLowPass(FilterType type, int order, double sampleRate, double cutoffFrequency) {
        switch (type) {
            case BESSEL:
                Bessel b = new Bessel();
                b.lowPass(order, sampleRate, cutoffFrequency);
                return b;
            case BUTTERWORTH:
                Butterworth bu = new Butterworth();
                bu.lowPass(order, sampleRate, cutoffFrequency);
                return bu;
            case CHEBYSHEV1:
                ChebyshevI c1 = new ChebyshevI();
                c1.lowPass(order, sampleRate, cutoffFrequency, CHEBY_RIPPLE_DB);
                return c1;
            case CHEBYSHEV2:
                ChebyshevII c2 = new ChebyshevII();
                c2.lowPass(order, sampleRate, cutoffFrequency, CHEBY_RIPPLE_DB);
                return c2;
            default:
                break;
        }
        return null;
    }
}
