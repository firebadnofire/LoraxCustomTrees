package org.infotoast.lorax.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Catalog {
    private Catalog() { }
    public static Map<String, Structure> load(ClassLoader loader) throws IOException {
        Map<String, Structure> catalog = new LinkedHashMap<String, Structure>();
        for (String[] species : Generation.TREES) for (String name : species) {
            InputStream input = loader.getResourceAsStream(name + ".bo5");
            if (input == null) throw new IOException("Missing bundled structure: " + name + ".bo5");
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                if (catalog.put(name, StructureReader.read(name + ".bo5", reader)) != null) throw new IOException("Duplicate tree " + name);
            }
        }
        return Collections.unmodifiableMap(catalog);
    }
}
