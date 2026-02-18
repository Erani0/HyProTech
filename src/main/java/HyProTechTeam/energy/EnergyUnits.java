package HyProTechTeam.energy;

import java.util.Locale;

public final class EnergyUnits {
    public static final double JOULES_PER_WH = 3600.0;

    private EnergyUnits() {
    }

    public static double toWh(int joules) {
        return joules / JOULES_PER_WH;
    }

    public static String formatWh(int joules) {
        return String.format(Locale.US, "%.2f", toWh(joules));
    }

    public static String formatEnergy(int joules) {
        return formatJoules(joules);
    }

    public static String formatEnergyWithCapacity(int energy, int capacity) {
        return formatJoules(energy) + " / " + formatJoules(capacity);
    }

    public static String formatWatts(int watts) {
        return watts + " W";
    }

    public static String formatJoulesPerTick(int joules) {
        if (joules >= 1_000_000) {
            double mj = joules / 1_000_000.0;
            return String.format(Locale.US, "%.2f MJ/t", mj);
        }
        if (joules >= 1000) {
            double kj = joules / 1000.0;
            return String.format(Locale.US, "%.1f kJ/t", kj);
        }
        return joules + " J/t";
    }

    public static String formatJoulesPerSecond(int joules) {
        if (joules >= 1_000_000) {
            double mj = joules / 1_000_000.0;
            return String.format(Locale.US, "%.2f MJ/s", mj);
        }
        if (joules >= 1000) {
            double kj = joules / 1000.0;
            return String.format(Locale.US, "%.1f kJ/s", kj);
        }
        return joules + " J/s";
    }

    public static String formatJoules(int joules) {
        long abs = Math.abs((long) joules);
        if (abs >= 1_000_000) {
            double mj = joules / 1_000_000.0;
            return String.format(Locale.US, "%.2f MJ", mj);
        }
        if (abs >= 1000) {
            double kj = joules / 1000.0;
            return String.format(Locale.US, "%.1f kJ", kj);
        }
        return joules + " J";
    }
}
