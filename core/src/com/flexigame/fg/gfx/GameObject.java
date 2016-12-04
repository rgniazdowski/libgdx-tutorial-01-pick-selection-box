package com.flexigame.fg.gfx;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.flexigame.fg.utils.AbstractFlags;

/**
 *
 */
public class GameObject extends ModelInstance implements SpatialObject {

    public static final class StateFlags extends AbstractFlags {
        public static final int NO_FLAGS = 0;
        public static final int VISIBLE = 1;
        public static final int ACTIVE = 2;
        public static final int SELECTED = 4;

        public static final int[] values = {NO_FLAGS, VISIBLE, ACTIVE, SELECTED};

        public StateFlags() {
        }

        public StateFlags(int value) {
            super(value);
        }

        @Override
        public int count() {
            return values.length;
        }

        @Override
        public int[] getValues() {
            return values;
        }

    } // static final class StateFlags

    //-------------------------------------------------------------------------

    protected BoundingBox boundingBox = new BoundingBox();
    protected BoundingBox originalBoundingBox = new BoundingBox();
    protected Vector3 center = new Vector3();
    protected Vector3 extent = new Vector3();
    protected Vector3 dimensions = new Vector3();
    protected Vector3 scale = new Vector3(1.0f, 1.0f, 1.0f);
    protected Vector3 tmpVec = new Vector3();
    protected Quaternion tmpQuat = new Quaternion();
    protected float radius = 0.0f;
    private int selfID = 0;
    private int parentID = 0;
    private String name = "";
    private StateFlags stateFlags = new StateFlags();
    protected boolean isTransformed = false;

    //-------------------------------------------------------------------------

    public GameObject(Model model) {
        super(model);
        refreshOriginalBoundingBox();
    }

    public GameObject(String name, Model model) {
        this(model);
        this.setName(name);
    }

    public GameObject(Model model, String rootNode, boolean mergeTransform) {
        super(model, rootNode, mergeTransform);
        refreshOriginalBoundingBox();
    }

    public GameObject(String name, Model model, String rootNode, boolean mergeTransform) {
        this(model, rootNode, mergeTransform);
        this.setName(name);
    }

    //-------------------------------------------------------------------------

    public void refreshOriginalBoundingBox() {
        // this bounding box can be invalid if nodes are transformed
        boundingBox.inf();
        final int n = nodes.size;
        for (int i = 0; i < n; i++)
            nodes.get(i).extendBoundingBox(boundingBox, false);
        originalBoundingBox.set(boundingBox);
        boundingBox.getCenter(center);
        boundingBox.getDimensions(dimensions);
        extent.set(dimensions);
        extent.scl(0.5f);

        tmpVec.x = scale.x * extent.x;
        tmpVec.y = scale.y * extent.y;
        tmpVec.z = scale.z * extent.z;

        radius = tmpVec.len();
    } // void refreshOriginalBoundingBox()

    //-------------------------------------------------------------------------

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    @Override
    public BoundingBox getOriginalBoundingBox() {
        return originalBoundingBox;
    }

    @Override
    public Vector3 getCenter() {
        return center;
    }

    @Override
    public Vector3 getExtent() {
        return extent;
    }

    @Override
    public Vector3 getDimensions() { return dimensions; }

    @Override
    public float getRadius() {
        return radius;
    }

    //-------------------------------------------------------------------------

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setID(int id) {
        this.selfID = id;
    }

    public int getID() {
        return selfID;
    }

    @Override
    public int getSpatialObjectID() {
        return selfID;
    }

    public void setParentID(int id) {
        this.parentID = id;
    }

    public int getParentID() {
        return parentID;
    }

    //-------------------------------------------------------------------------

    public void setFlag(int flag, boolean toggle) {
        this.stateFlags.set(flag, toggle);
    }

    public void resetFlags() {
        this.stateFlags.reset();
    }

    public StateFlags getStateFlags() {
        return this.stateFlags;
    }

    public boolean hasFlag(int flag) {
        return this.stateFlags.isToggled(flag);
    }

    //-------------------------------------------------------------------------

    public void setVisible(boolean toggle) {
        this.stateFlags.set(StateFlags.VISIBLE, toggle);
    }

    @Override
    public boolean isVisible() {
        return this.stateFlags.isToggled(StateFlags.VISIBLE);
    }

    public void setActive(boolean toggle) {
        this.stateFlags.set(StateFlags.ACTIVE, toggle);
    }

    public boolean isActive() {
        return this.stateFlags.isToggled(StateFlags.ACTIVE);
    }

    public void setSelected(boolean toggle) {
        this.stateFlags.set(StateFlags.SELECTED, toggle);
    }

    public boolean isSelected() {
        return this.stateFlags.isToggled(StateFlags.SELECTED);
    }

    //-------------------------------------------------------------------------

    @Override
    public Matrix4 getTransform() {
        return this.transform;
    }

    //-------------------------------------------------------------------------

    @Override
    public void setPosition(Vector3 position) {
        isTransformed = true;
        this.transform.setTranslation(position);
    }

    @Override
    public void setPosition(float x, float y, float z) {
        isTransformed = true;
        this.transform.setTranslation(x, y, z);
    }

    @Override
    public Vector3 getPosition() {
        return this.transform.getTranslation(tmpVec);
    }

    public void getPositionExt(Vector3 position) {
        this.transform.getTranslation(position);
    }

    //-------------------------------------------------------------------------

    public void getScaleExt(Vector3 scale) {
        this.transform.getScale(scale);
    }

    @Override
    public Vector3 getScale() {
        return this.scale;
    }

    @Override
    public void setScale(float _scale) {
        if (_scale < 0.0f)
            _scale *= -1.0f;
        this.setScale(_scale, _scale, _scale);
    }

    @Override
    public void setScale(float _x, float _y, float _z) {
        isTransformed = true;
        this.transform.getTranslation(tmpVec);
        this.transform.getRotation(tmpQuat);
        tmpQuat.nor(); // normalize quaternion
        // reset the transformation, scaling first
        this.transform.setToScaling(_x, _y, _z);
        this.transform.setTranslation(tmpVec);
        this.transform.rotate(tmpQuat);

        scale.x = _x;
        scale.y = _y;
        scale.z = _z;

        tmpVec.x = _x * this.extent.x;
        tmpVec.y = _y * this.extent.y;
        tmpVec.z = _z * this.extent.z;

        radius = tmpVec.len();
    }

    //-------------------------------------------------------------------------

    public Matrix4 translate(float x, float y, float z) {
        isTransformed = true;
        return this.transform.translate(x, y, z);
    }

    public Matrix4 translate(Vector3 translation) {
        isTransformed = true;
        return this.transform.translate(translation);
    }

    //-------------------------------------------------------------------------

    public Matrix4 rotate(float axisX, float axisY, float axisZ, float degrees) {
        isTransformed = true;
        return this.transform.rotate(axisX, axisY, axisZ, degrees);
    }

    public Matrix4 rotate(Quaternion quaternion) {
        isTransformed = true;
        return this.transform.rotate(quaternion);
    }

    public Matrix4 rotate(final Vector3 v1, final Vector3 v2) {
        isTransformed = true;
        return this.transform.rotate(v1, v2);
    }

    //-------------------------------------------------------------------------

    public void updateBoundingBox() {
        updateBoundingBox(false);
    }

    public void updateBoundingBox(boolean force) {
        if (isTransformed || force) {
            boundingBox.set(originalBoundingBox);
            boundingBox.mul(transform);
            boundingBox.getCenter(center);
            boundingBox.getDimensions(dimensions);
            extent.set(dimensions);
            extent.scl(0.5f);

            isTransformed = false;
        }
    }

    public void update() {
        updateBoundingBox();
    }

    //-------------------------------------------------------------------------

    public Matrix4 setRotation(float axisX, float axisY, float axisZ, float degrees) {
        isTransformed = true;
        transform.getTranslation(tmpVec);
        transform.setToScaling(scale.x, scale.y, scale.z);
        transform.setTranslation(tmpVec);
        return this.transform.rotate(axisX, axisY, axisZ, degrees);
    }

    public Matrix4 setRotation(Quaternion quaternion) {
        isTransformed = true;
        transform.getTranslation(tmpVec);
        transform.setToScaling(scale.x, scale.y, scale.z);
        transform.setTranslation(tmpVec);
        return this.transform.rotate(quaternion);
    }

    public Matrix4 setRotation(final Vector3 v1, final Vector3 v2) {
        isTransformed = true;
        transform.getTranslation(tmpVec);
        transform.setToScaling(scale.x, scale.y, scale.z);
        transform.setTranslation(tmpVec);
        return transform.rotate(v1, v2);
    }

    //-------------------------------------------------------------------------

} // class GameObject
