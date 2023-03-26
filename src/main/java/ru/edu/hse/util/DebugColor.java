package ru.edu.hse.util;

public enum DebugColor {
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    PURPLE(35),
    CYAN(36);

    private final int value;

    DebugColor(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

}
