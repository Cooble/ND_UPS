package cz.cooble.ndc.core;

import cz.cooble.ndc.core.NBT;

/**
 * foo
 */
public interface NBTSaveable {

    void readFromNBT(NBT nbt);

    void writeToNBT(NBT nbt);

    boolean isDirty();

}
