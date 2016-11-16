package com.flexigame.fg.utils;

/**
 *
 */
public class Vector2i {
    public int x;
    public int y;

    public Vector2i() {
        x = 0;
        y = 0;
    }

    public Vector2i(Vector2i vector) {
        this.x = vector.x;
        this.y = vector.y;
    }

    public Vector2i(int _x, int _y) {
        this.x = _x;
        this.y = _y;
    }

    //-------------------------------------------------------------------------

    public Vector2i set(int _x, int _y) {
        this.x = _x;
        this.y = _y;
        return this;
    }

    public Vector2i set(Vector2i vector) {
        this.x = vector.x;
        this.y = vector.y;
        return this;
    }

    public Vector2i zero() {
        this.x = 0;
        this.y = 0;
        return this;
    }

    //-------------------------------------------------------------------------

    public boolean isZero() {
        return (this.x == 0 && this.y == 0);
    }

    public boolean isNegative() {
        return (this.x < 0 && this.y < 0);
    }

    public boolean isPositive() {
        return (this.x > 0 && this.y > 0);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Vector2i other = (Vector2i)obj;
        if(this.x != other.x)
            return false;
        if(this.y != other.y)
            return false;
        return true;
    }

    //-------------------------------------------------------------------------

    public int length() {
        return (int)Math.sqrt(x * x + y * y);
    }

    public int length2() {
        return (x * x + y * y);
    }

    public int area() {
        return x * y;
    }

    //-------------------------------------------------------------------------

    public Vector2i sub(Vector2i v) {
        x -= v.x;
        y -= v.y;
        return this;
    }

    public Vector2i sub(int value) {
        x -= value;
        y -= value;
        return this;
    }

    public Vector2i sub(int x, int y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    //-------------------------------------------------------------------------

    public Vector2i add(Vector2i v) {
        x += v.x;
        y += v.y;
        return this;
    }

    public Vector2i add(int value) {
        x += value;
        y += value;
        return this;
    }

    public Vector2i add(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }

    //-------------------------------------------------------------------------

    public Vector2i abs() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        return this;
    }

    public Vector2i abs(Vector2i v) {
        this.x = Math.abs(v.x);
        this.y = Math.abs(v.y);
        return this;
    }

    public Vector2i abs(int value) {
        this.x = Math.abs(value);
        this.y = this.x;
        return this;
    }

    public Vector2i abs(int x, int y) {
        this.x = Math.abs(x);
        this.y = Math.abs(y);
        return this;
    }

    //-------------------------------------------------------------------------

    public int dot (Vector2i v) {
        return (x * v.x + y * v.y);
    }

    public int dot (int ox, int oy) {
        return (x * ox + y * oy);
    }

    //-------------------------------------------------------------------------

    @Override
    public String toString() {
        return x + "x" + y;
    }


    //-------------------------------------------------------------------------

} // class Vector2i
