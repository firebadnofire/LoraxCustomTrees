package org.infotoast.lorax.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Symbolic state: no platform classes, IDs or mutable runtime block objects. */
public final class BlockState {
    public final String name;
    public final Map<String, String> properties;
    private static final String[] DIRECTIONS = {"north", "east", "south", "west"};

    public BlockState(String name, Map<String, String> properties) {
        this.name = name;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<String, String>(properties));
    }

    public static BlockState parse(String text) {
        text = text.trim().toLowerCase(Locale.ROOT);
        int bracket = text.indexOf('[');
        String name = bracket < 0 ? text : text.substring(0, bracket);
        if (!name.contains(":")) name = "minecraft:" + name;
        if (!name.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) throw new IllegalArgumentException("Invalid block name: " + text);
        Map<String, String> properties = new LinkedHashMap<String, String>();
        if (bracket >= 0) {
            if (!text.endsWith("]")) throw new IllegalArgumentException("Unclosed block properties: " + text);
            String body = text.substring(bracket + 1, text.length() - 1);
            if (!body.isEmpty()) for (String entry : body.split(",")) {
                String[] pair = entry.split("=", -1);
                if (pair.length != 2 || !pair[0].matches("[a-z0-9_]+") || !pair[1].matches("[a-z0-9_]+")
                        || properties.put(pair[0], pair[1]) != null) {
                    throw new IllegalArgumentException("Invalid or duplicate block property: " + entry);
                }
            }
        }
        return new BlockState(name, properties);
    }

    private static String direction(String value, int turns) {
        for (int i = 0; i < DIRECTIONS.length; i++) if (DIRECTIONS[i].equals(value)) return DIRECTIONS[(i + turns) % 4];
        return value;
    }

    public BlockState rotate(int turns) {
        if (turns < 0 || turns > 3) throw new IllegalArgumentException("Rotation must be between 0 and 3");
        Map<String, String> rotated = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            String key = direction(property.getKey(), turns);
            String value = property.getValue();
            if ("facing".equals(key)) value = direction(value, turns);
            if ("axis".equals(key) && turns % 2 != 0) {
                if ("x".equals(value)) value = "z";
                else if ("z".equals(value)) value = "x";
            }
            if ("rotation".equals(key)) value = Integer.toString((Integer.parseInt(value) + turns * 4) % 16);
            rotated.put(key, value);
        }
        return new BlockState(name, rotated);
    }

    @Override public String toString() {
        if (properties.isEmpty()) return name;
        StringBuilder text = new StringBuilder(name).append('[');
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (text.charAt(text.length() - 1) != '[') text.append(',');
            text.append(property.getKey()).append('=').append(property.getValue());
        }
        return text.append(']').toString();
    }
}
