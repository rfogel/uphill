package com.uphill.common;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

public class TestCommonUtil {

    public static EasyRandom generator() {
        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.stringLengthRange(2, 10);
        parameters.collectionSizeRange(2, 10);
        return new EasyRandom(parameters);
    }
}
