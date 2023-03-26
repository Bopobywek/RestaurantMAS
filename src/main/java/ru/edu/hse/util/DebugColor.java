package ru.edu.hse.util;

public enum DebugColor {
    GREEN(40),
    YELLOW(184), // 226
    BLUE(33),
    PURPLE(135),
    CYAN(49),
    BLOOD_COLOR(196), // 196
    ORANGE(208),
    PINK(201),
    SAND(179);


    private final int value;

    DebugColor(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

}
