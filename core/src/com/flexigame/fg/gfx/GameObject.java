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

        //---------------------------------------------------------------------

        public StateFlags() {
        }

        public StateFlags(int value) {
            super(value);
        }

        //---------------------------------------------------------------------

        @Override
        public int count() {
            return values.length;
        }

        @Override
        public int[] getValues() {
            return values;
        }

        //---------------------------------------------------------------------
    } // static final class StateFlags

    protected BoundingBox boundingBox = new BoundingBox();
    protected BoundingBox originalBoundingBox = new BoundingBox();
    protected Vector3 center = new Vector3();
    protected Vector3 extent = new Vector3();
    protected Vector3 scale = new Vector3(1.0f, 1.0f, 1.0f);
    protected Vector3 tmpVec = new Vector3();
    protected float radius = 0.0f;
    private int selfID = 0;
    private int parentID = 0;
    private String name = "";
    private StateFlags stateFlags = new StateFlags();
    protected boolean isTransformed = false;
    public Object userObject = null;

    //-------------------------------------------------------------------------

    public GameObject(Model model) {
        super(model);
        calculateBoundingBox(boundingBox);
        originalBoundingBox.set(boundingBox);
        boundingBox.getCenter(center);
        boundingBox.getDimensions(extent);
        extent.scl(0.5f);
    }

    public GameObject(String name, Model model) {
        this(model);
        this.setName(name);
    }

    public GameObject(Model model, String rootNode, boolean mergeTransform) {
        super(model, rootNode, mergeTransform);
        calculateBoundingBox(boundingBox);
        boundingBox.getCenter(center);
        boundingBox.getDimensions(extent);
        extent.scl(0.5f);
    }

    public GameObject(String name, Model model, String rootNode, boolean mergeTransform) {
        this(model, rootNode, mergeTransform);
        this.setName(name);
    }

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

    ;

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
    public void setScale(float _x, float _y, float _z) {
        isTransformed = true;
        this.transform.getTranslation(tmpVec);
        Quaternion rotation = new Quaternion();
        this.transform.getRotation(rotation);
        rotation.nor(); // normalize quaternion
        // reset the transformation, scaling first
        this.transform.setToScaling(_x, _y, _z);
        this.transform.setTranslation(tmpVec);
        this.transform.rotate(rotation);

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
            this.boundingBox.set(this.originalBoundingBox);
            this.boundingBox.mul(this.transform);
            //GameObject.transformBoundingBox(this.boundingBox, this.transform);
            isTransformed = false;
        }
    }

    private static void transformBoundingBox(BoundingBox box, Matrix4 transform) {
        Vector3 translation = new Vector3();
        transform.getTranslation(translation);

        float[] values = transform.getValues();
        float[][] m = {
                {0.0f, 0.0f, 0.0f, 0.0f},
                {0.0f, 0.0f, 0.0f, 0.0f},
                {0.0f, 0.0f, 0.0f, 0.0f},
                {0.0f, 0.0f, 0.0f, 0.0f}
        };

        m[0][0] = values[Matrix4.M00];
        m[0][1] = values[Matrix4.M01];
        m[0][2] = values[Matrix4.M02];
        m[0][3] = values[Matrix4.M03];

        m[1][0] = values[Matrix4.M10];
        m[1][1] = values[Matrix4.M11];
        m[1][2] = values[Matrix4.M12];
        m[1][3] = values[Matrix4.M13];

        m[2][0] = values[Matrix4.M20];
        m[2][1] = values[Matrix4.M21];
        m[2][2] = values[Matrix4.M22];
        m[2][3] = values[Matrix4.M23];

        m[3][0] = values[Matrix4.M30];
        m[3][1] = values[Matrix4.M31];
        m[3][2] = values[Matrix4.M32];
        m[3][3] = values[Matrix4.M33];

        float a, b;
        int i, j;
        float[] oldmin = {box.min.x, box.min.y, box.min.z};
        float[] oldmax = {box.max.x, box.max.y, box.max.z};

        float[] min = {translation.x, translation.y, translation.z};
        float[] max = {translation.x, translation.y, translation.z};

        for (j = 0; j < 3; j++) {
            for (i = 0; i < 3; i++) {
                a = m[i][j] * oldmin[i];
                b = m[i][j] * oldmax[i];
                if (a < b) {
                    min[j] += a;
                    max[j] += b;
                } else {
                    min[j] += b;
                    max[j] += a;
                }
            }
        }

        box.min.x = min[0];
        box.min.y = min[1];
        box.min.z = min[2];

        box.max.x = max[0];
        box.max.y = max[1];
        box.max.z = max[2];
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
        Vector3 translation = new Vector3();
        transform.getTranslation(translation);
        transform.setToScaling(scale.x, scale.y, scale.z);
        transform.setTranslation(translation);
        return transform.rotate(v1, v2);
    }

    //-------------------------------------------------------------------------

} // class GameObject
