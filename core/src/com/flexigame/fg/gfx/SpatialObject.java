package com.flexigame.fg.gfx;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

/**
 *
 */
public interface SpatialObject {

    int getSpatialObjectID();

    BoundingBox getBoundingBox();
    BoundingBox getOriginalBoundingBox();

    Vector3 getExtent();
    Vector3 getCenter();
    float getRadius();

    Matrix4 getTransform();

    void setPosition(Vector3 position);
    void setPosition(float _x, float _y, float _z);
    Vector3 getPosition();

    void setScale(float _x, float _y, float _z);
    Vector3 getScale();
} // interface SpatialObject
