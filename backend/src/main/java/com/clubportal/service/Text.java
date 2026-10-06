package com.clubportal.service;

final class Text {

    private Text() {
    }

    /** Trims a string; blank becomes null. */
    static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
