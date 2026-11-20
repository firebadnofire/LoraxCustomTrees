package org.infotoast.lorax.customobject.datatype;


public class CustomObjectGroundCheck extends CustomObjectCheck {
    public CustomObjectGroundCheck(ObjectLocation location) {
        super(location);
    }


    @Override
    public String getName() {
        return "GroundCheck";
    }

    public static String[] getKeywords() {
        return new String[]{"GroundCheck", "GC"};
    }

    public static String getRESyntax() {
        return "G.*\\(-?[0-9]+,-?[0-9]+,-?[0-9]+\\)";
    }

    @Override
    public String toString() {
        return "GC(" + location + ")";
    }
}
