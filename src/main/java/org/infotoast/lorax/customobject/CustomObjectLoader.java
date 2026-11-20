package org.infotoast.lorax.customobject;

import org.infotoast.lorax.customobject.exception.CustomObjectLoadingFailedException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.infotoast.lorax.common.Structure;

public class CustomObjectLoader {
    private final Map<String, CustomObject> customObjects = new HashMap<>();
    private final Map<String, Structure> structures = new LinkedHashMap<>();

    public CustomObjectLoader() {
    }

    public void loadCustomObject(String name, String fileName) throws CustomObjectLoadingFailedException {
        try {
            ObjectFile customObjectFile = new ObjectFile(name, fileName);
            CustomObject object = CustomObjectReader.read(customObjectFile);
            customObjects.put(name, object);
            structures.put(name, object.getDefinition());
        } catch (URISyntaxException | RuntimeException e) {
            throw new CustomObjectLoadingFailedException("Failed to load " + fileName + ": " + e.getMessage());
        }
    }

    public CustomObject getCustomObject(String name) {
        return customObjects.get(name);
    }

    public Map<String, Structure> getStructures() {
        return Collections.unmodifiableMap(structures);
    }

    public void loadObjects() throws CustomObjectLoadingFailedException {
        customObjects.clear();
        structures.clear();
        for (String[] species : org.infotoast.lorax.common.Generation.TREES) {
            for (String name : species) loadCustomObject(name, name + ".bo5");
        }
    }
}
