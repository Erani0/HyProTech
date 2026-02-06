package com.example.plugin.config;

public final class ConfigArrays {
    private ConfigArrays() {
    }

    public static String[] mergeStringArray(String[] value, String[] defaults, int expectedLength) {
        String[] result = new String[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            String fallback = defaults != null && i < defaults.length ? defaults[i] : "";
            result[i] = fallback == null ? "" : fallback;
        }
        if (value != null) {
            int limit = Math.min(value.length, expectedLength);
            for (int i = 0; i < limit; i++) {
                String item = value[i];
                if (item == null) {
                    continue;
                }
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    result[i] = trimmed;
                }
            }
        }
        return result;
    }

    public static int[] mergeIntArray(int[] value, int[] defaults, int expectedLength, int minValue) {
        int[] result = new int[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            int fallback = defaults != null && i < defaults.length ? defaults[i] : minValue;
            result[i] = Math.max(minValue, fallback);
        }
        if (value != null) {
            int limit = Math.min(value.length, expectedLength);
            for (int i = 0; i < limit; i++) {
                int v = value[i];
                result[i] = v < minValue ? minValue : v;
            }
        }
        return result;
    }

    public static double[] mergeDoubleArray(double[] value, double[] defaults, int expectedLength, double minValue) {
        double[] result = new double[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            double fallback = defaults != null && i < defaults.length ? defaults[i] : minValue;
            if (!Double.isFinite(fallback) || fallback < minValue) {
                fallback = minValue;
            }
            result[i] = fallback;
        }
        if (value != null) {
            int limit = Math.min(value.length, expectedLength);
            for (int i = 0; i < limit; i++) {
                double v = value[i];
                if (!Double.isFinite(v) || v < minValue) {
                    v = minValue;
                }
                result[i] = v;
            }
        }
        return result;
    }
}
