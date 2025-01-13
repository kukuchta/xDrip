package com.eveningoutpost.dexdrip.cgm.sharefollow;

public enum TREND_ARROW_VALUES {
    NONE(0),
    DOUBLE_UP(1, "\u21C8", "DoubleUp", 40d),
    SINGLE_UP(2, "\u2191", "SingleUp", 3.5d),
    UP_45(3, "\u2197", "FortyFiveUp", 2d),
    FLAT(4, "\u2192", "Flat", 1d),
    DOWN_45(5, "\u2198", "FortyFiveDown", -1d),
    SINGLE_DOWN(6, "\u2193", "SingleDown", -2d),
    DOUBLE_DOWN(7, "\u21CA", "DoubleDown", -3.5d),
    NOT_COMPUTABLE(8, "", "NotComputable"),
    OUT_OF_RANGE(9, "", "RateOutOfRange");

    private String arrowSymbol;
    private String trendName;
    private int myID;
    private Double threshold;

    TREND_ARROW_VALUES(int id, String symbol, String name) {
        this.myID = id;
        this.arrowSymbol = symbol;
        this.trendName = name;
    }

    TREND_ARROW_VALUES(int id, String symbol, String name, Double threshold) {
        this.myID = id;
        this.arrowSymbol = symbol;
        this.trendName = name;
        this.threshold = threshold;
    }

    TREND_ARROW_VALUES(int id) {
        this(id, null, null, null);
    }

    public String Symbol() {
        if (arrowSymbol == null) {
            return "\u2194\uFE0E";
        } else {
            return arrowSymbol + "\uFE0E";
        }
    }

    public String trendName() {
        return this.trendName;
    }

    public String friendlyTrendName() {
        if (trendName == null) {
            return name().replace("_", " ");
        } else {
            return trendName;
        }
    }

    public int getID() {
        return myID;
    }

    public static TREND_ARROW_VALUES getTrend(double value) {
        TREND_ARROW_VALUES finalTrend = NONE;
        for (TREND_ARROW_VALUES trend : values()) {
            if (trend.threshold == null)
                continue;

            if (value > trend.threshold)
                return finalTrend;
            else
                finalTrend = trend;
        }
        return finalTrend;
    }

    public static double getSlope(String value) {
        for (TREND_ARROW_VALUES trend : values())
            if (trend.trendName.equalsIgnoreCase(value))
                return trend.threshold;
        throw new IllegalArgumentException();
    }

    public static TREND_ARROW_VALUES getEnum(int id) {
        for (TREND_ARROW_VALUES t : values()) {
            if (t.myID == id)
                return t;
        }
        return OUT_OF_RANGE;
    }

    public static TREND_ARROW_VALUES getEnum(String value) {
        try {
            int val = Integer.parseInt(value);
            return getEnum(val);
        } catch (NumberFormatException e) {
            for (TREND_ARROW_VALUES trend : values())
                if (trend.friendlyTrendName().equalsIgnoreCase(value) || trend.name().equalsIgnoreCase(value))
                    return trend;
        }
        return OUT_OF_RANGE;
    }

}
