package com.foxyclient.mixin_interface;

public interface IEntityRenderState {
    void setEntityId(int id);
    int getEntityId();
    void foxyclient$setSelf(boolean isSelf);
    boolean foxyclient$isSelf();
    void foxyclient$setUuid(java.util.UUID uuid);
    java.util.UUID foxyclient$getUuid();
}
