package com.flexigame.fg.utils;

import com.badlogic.gdx.utils.Array;

/**
 *
 */
public abstract class AbstractFlags {

    protected int value = 0;

    //-------------------------------------------------------------------------

    public AbstractFlags() {
    }

    public AbstractFlags(int value) {
        this.value = value;
    }

    //-------------------------------------------------------------------------

    public int getValue() {
        return this.value;
    }

    public void set(int _id, boolean toggle) {
        this.value |= _id;
        if (!toggle) {
            this.value ^= _id;
        }
    }

    public void unset(int _id) {
        this.value |= _id;
        this.value ^= _id;
    }

    public void reset() {
        value = 0;
    }

    public boolean isToggled(int _id) {
        return ((this.value & _id) == _id);
    }

    //-------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != this.getClass())
            return false;
        AbstractFlags other = (AbstractFlags) obj;
        if (other.getValue() != this.getValue())
            return false;
        return true;
    }

    //-------------------------------------------------------------------------

    public abstract int count();

    public abstract int[] getValues();

    //-------------------------------------------------------------------------

    public void toArray(Array<Integer> result) {
        result.clear();
        final int length = this.count();
        int[] values = this.getValues();
        for (int i = 0; i < length; i++) {
            if (this.isToggled(values[i]))
                result.add(Integer.valueOf(values[i]));
        }
    }

    //-------------------------------------------------------------------------
} // abstract class AbstractFlags
