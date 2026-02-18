package HyProTechTeam.item;

public enum ItemTarget {
    AUTO("Auto"),
    INPUT("Input"),
    FUEL("Fuel"),
    OUTPUT("Output");

    private final String label;

    ItemTarget(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ItemTarget next() {
        switch (this) {
            case AUTO:
                return INPUT;
            case INPUT:
                return FUEL;
            case FUEL:
                return OUTPUT;
            case OUTPUT:
            default:
                return AUTO;
        }
    }
}
