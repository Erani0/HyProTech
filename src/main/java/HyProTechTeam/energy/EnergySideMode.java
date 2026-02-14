package HyProTechTeam.energy;

public enum EnergySideMode {
    DISABLED("Off"),
    INPUT("In"),
    OUTPUT("Out"),
    BOTH("In/Out");

    private final String label;

    EnergySideMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public EnergySideMode next() {
        switch (this) {
            case DISABLED:
                return INPUT;
            case INPUT:
                return OUTPUT;
            case OUTPUT:
                return BOTH;
            case BOTH:
            default:
                return DISABLED;
        }
    }

    public static EnergySideMode fromFlags(boolean input, boolean output) {
        if (input && output) {
            return BOTH;
        }
        if (input) {
            return INPUT;
        }
        if (output) {
            return OUTPUT;
        }
        return DISABLED;
    }
}
