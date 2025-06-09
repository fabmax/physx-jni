package de.fabmax.physxjni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import physx.common.PxErrorCodeEnum;
import physx.extensions.PxD6AxisEnum;

import java.util.Arrays;
import java.util.Comparator;

public class EnumLookupTest {
    @Test
    public void enumLookupIndexed() {
        var values = PxD6AxisEnum.values();
        var min = Arrays.stream(values).min(Comparator.comparing(it -> it.value)).orElseThrow().value;
        var max = Arrays.stream(values).max(Comparator.comparing(it -> it.value)).orElseThrow().value;
        Assertions.assertTrue(min == 0 && max < 256);
        for (PxD6AxisEnum value : values) {
            Assertions.assertSame(value, PxD6AxisEnum.forValue(value.value));
        }
    }

    @Test
    public void enumLookupNonIndexed() {
        var values = PxErrorCodeEnum.values();
        var min = Arrays.stream(values).min(Comparator.comparing(it -> it.value)).orElseThrow().value;
        var max = Arrays.stream(values).max(Comparator.comparing(it -> it.value)).orElseThrow().value;
        Assertions.assertTrue(min < 0 || max >= 256);
        for (PxErrorCodeEnum value : values) {
            Assertions.assertSame(value, PxErrorCodeEnum.forValue(value.value));
        }
    }
}
