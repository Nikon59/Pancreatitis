package com.pancreatitis.core.model.ml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScalingParamsTest {

    @Test
    @DisplayName("Normalize value with mean and standard deviation")
    void testNormalize() {
        ScalingParams scaling = new ScalingParams(10.0f, 2.0f);
        assertEquals(10.0f, scaling.getMean());
        assertEquals(2.0f, scaling.getStd());

        // (14.0 - 10.0) / 2.0 = 2.0
        assertEquals(2.0f, scaling.normalize(14.0f));
        // (10.0 - 10.0) / 2.0 = 0.0
        assertEquals(0.0f, scaling.normalize(10.0f));
        // (8.0 - 10.0) / 2.0 = -1.0
        assertEquals(-1.0f, scaling.normalize(8.0f));
    }

    @Test
    @DisplayName("Default std to 1.0 when std is 0.0")
    void testZeroStd() {
        ScalingParams scaling = new ScalingParams(5.0f, 0.0f);
        assertEquals(1.0f, scaling.getStd());
        assertEquals(0.0f, scaling.normalize(5.0f));
    }
}
