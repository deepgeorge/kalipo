package org.kalipo.service.util;

/**
 * Created by damoeb on 10.12.14.
 */
public final class NumUtils {
    public static Integer nullToZero(Integer num) {
        return num == null ? 0 : num;
    }

    public static double nullToZero(Double num) {
        return num == null ? 0 : num;
    }
}
