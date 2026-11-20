package org.infotoast.lorax.customobject.datatype;

/** Export representation only. Runtime checks belong to the common placement engine. */
public abstract class CustomObjectCheck implements CustomObjectItem {
    protected final ObjectLocation location;
    public CustomObjectCheck(ObjectLocation location) { this.location = location; }
    @Override public ObjectLocation getLocation() { return location; }
}
