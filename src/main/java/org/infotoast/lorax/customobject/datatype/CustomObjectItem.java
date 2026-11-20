package org.infotoast.lorax.customobject.datatype;

/** Serialization model for optional WorldEdit export; contains no world access. */
public interface CustomObjectItem {
    String getName();
    ObjectLocation getLocation();
    String toString();
}
