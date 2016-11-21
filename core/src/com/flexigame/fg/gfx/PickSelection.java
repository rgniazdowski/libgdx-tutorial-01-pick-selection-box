package com.flexigame.fg.gfx;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.TimeUtils;
import com.flexigame.fg.utils.AbstractFlags;
import com.flexigame.fg.utils.Vector2i;

/**
 *
 */
public class PickSelection {

    public interface PixelChecker {
        boolean isColorInPixels(int colorValue, Rectangle area, boolean dump);
    } // interface PixelChecker

    public enum Result {
        NOT_PICKED,
        PICKED_SPHERE,
        PICKED_AABB,
        PICKED_PIXEL;

        public boolean isPicked() {
            return (this.ordinal() != NOT_PICKED.ordinal());
        }

        public boolean isSphere() {
            return (this.ordinal() == PICKED_SPHERE.ordinal());
        }

        public boolean isAABB() {
            return (this.ordinal() == PICKED_AABB.ordinal());
        }

        public boolean isPixel() {
            return (this.ordinal() == PICKED_PIXEL.ordinal());
        }
    } // enum Result

    //-------------------------------------------------------------------------

    public static final class StateFlags extends AbstractFlags {
        static final int NO_FLAGS = 0;
        /* Selection works only when picker is active */
        static final int SELECTION_ON_CLICK = 1;
        /* Selection is active even without toggled click */
        static final int SELECTION_ON_HOVER = 2;
        /* Whether or not to check aabb triangles - may be more accurate
         * than picking sphere; AABB is transformed and projected to screen space
         * when using picking box */
        static final int CHECK_AABB_TRIANGLES = 4;
        /* Whether or not to check frame buffer object pixel data (provided externally) */
        static final int CHECK_FBO_PIXELS = 8;
        /* Whether or not the picker is active */
        static final int PICKER_ACTIVE = 16;
        /* Group selection mode - basically it means multi-selection mode */
        static final int GROUP_SELECTION_MODE = 32;
        /* Toggle selection mode - second click deselects;
         * Deselection only occurs on spatial object(!) */
        static final int TOGGLE_SELECTION_MODE = 64;
        /* Whether or not to use the onscreen picking box */
        static final int USE_PICKING_BOX = 128;
        /* Internal flag marking begin() function execution - set to false on end() */
        static final int INTERNAL_BEGIN = 256;
        /* Whether or not should unselect objects (when picking ray does not collide) */
        static final int INTERNAL_SHOULD_UNSELECT = 512;
        /* Internal flag - should continue traversing? */
        static final int INTERNAL_SHOULD_CONTINUE = 1024;

        public static final int[] values = {NO_FLAGS, // 0
                SELECTION_ON_CLICK,
                SELECTION_ON_HOVER,
                CHECK_AABB_TRIANGLES,
                CHECK_FBO_PIXELS,
                PICKER_ACTIVE,
                GROUP_SELECTION_MODE,
                TOGGLE_SELECTION_MODE,
                USE_PICKING_BOX};

        public StateFlags() {
            super();
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

    public interface OnSelectionListener {
        void selectionChanged(SpatialObject spatialObject, PickingInfo pickingInfo, boolean selected);
    }

    Array<OnSelectionListener> onSelectionListenersArray = new Array<OnSelectionListener>();

    public boolean addOnSelectionListener(OnSelectionListener onSelectionListener) {
        if (onSelectionListener == null)
            throw new NullPointerException("onSelectionListener cannot be null");
        if (onSelectionListenersArray.contains(onSelectionListener, true)) {
            return false;
        }
        onSelectionListenersArray.add(onSelectionListener);
        return true;
    }

    public boolean removeOnSelectionListener(OnSelectionListener onSelectionListener) {
        if (onSelectionListener == null)
            throw new NullPointerException("onSelectionListener cannot be null");

        return onSelectionListenersArray.removeValue(onSelectionListener, true);
    }

    public Array<OnSelectionListener> getOnSelectionListenersArray() {
        return onSelectionListenersArray;
    }

    protected void callOnSelectionListeners(SpatialObject spatialObject, PickingInfo pickingInfo, boolean selected) {
        if (spatialObject == null)
            throw new NullPointerException("spatialObject cannot be null");
        for (int i = 0; i < onSelectionListenersArray.size; i++) {
            OnSelectionListener onSelectionListener = onSelectionListenersArray.get(i);
            onSelectionListener.selectionChanged(spatialObject, pickingInfo, selected); // call!
        }
    }

    //-------------------------------------------------------------------------

    static public final class PickingInfo implements Pool.Poolable {
        /* Spatial object being check */
        public SpatialObject spatialObject = null;
        /* Current pick selection result for given object */
        public Result result = Result.NOT_PICKED;
        /* Intersection point (ray with 3D sphere) */
        public Vector3 intersection = new Vector3();
        /* On screen rectangle occupied by the object */
        public Rectangle onScreen = new Rectangle();
        /* radius of the 2D onscreen circle */
        public int radius = 0;
        /* Center of the bound 2d circle */
        public Vector2i center = new Vector2i();
        /* timestamp in milliseconds - when the object was selected */
        public float timeStamp = 0;
        /* whether or not is currently selected */
        public boolean selected = false;
        /* whether or not the picking box contains the on-screen box of this object */
        public boolean pickBoxContains = false;
        /* whether or not the picking box overlaps with the on-screen box */
        public boolean pickBoxOverlaps = false;

        @Override
        public void reset() {
            pickBoxContains = false;
            pickBoxOverlaps = false;
            selected = false;
            timeStamp = 0.0f;
            center.x = 0;
            center.y = 0;
            radius = 0;
            onScreen.set(0, 0, 0, 0);
            result = Result.NOT_PICKED;
            intersection.set(0.0f, 0.0f, 0.0f);
            spatialObject = null;
        } // void reset()
    } // static final class PickingInfo

    private final Pool<PickingInfo> pickingInfoPool = new Pool<PickingInfo>() {
        @Override
        protected PickingInfo newObject() {
            return new PickingInfo();
        }
    };

    //-------------------------------------------------------------------------

    /* External array with spatial objects (that need checking) - can be null */
    protected Array<SpatialObject> spatialObjects = null;
    /* Array with currently selected objects */
    protected final Array<SpatialObject> selectedObjects = new Array<SpatialObject>();
    /* Special map for mapping object scene index to PickingInfo structure */
    protected final ObjectMap<Integer, PickingInfo> pickingInfoMap = new ObjectMap<Integer, PickingInfo>(64);
    /* Current state flags (on/off options) */
    protected final StateFlags stateFlags = new StateFlags(StateFlags.SELECTION_ON_CLICK);
    /* External camera (required) */
    protected Camera camera = null;
    /* Current picking ray */
    protected Ray ray = null;
    /* Currently reported pick selection position */
    protected Vector2i pickPos = new Vector2i();
    /* The start position of the pick selection (when picker was reported active) */
    protected Vector2i pickPosBegin = new Vector2i();
    /* Current timestamp in milliseconds */
    protected float pickTimeStampBegin = 0;
    /* Proper position and size of the pick selection box */
    protected Rectangle pickBox = new Rectangle();
    /* What is good pick result? */
    protected Result goodPickResult = Result.NOT_PICKED;
    /* Timestamp in milliseconds marking PickSelection initialization */
    protected long initTimeStamp = 0;
    /* Helper Vector3 array with aabb points */
    protected Vector3[] aabbPoints = new Vector3[8];
    /* Internal bounding box - used for picking box additional detection */
    protected BoundingBox internalAABB = new BoundingBox();
    /* Temporary helper vector */
    protected Vector3 tmpVec = new Vector3();
    protected Rectangle tmpRectangle = new Rectangle();
    protected int screenWidth = 0;
    protected int screenHeight = 0;
    /* External interface for checking fbo pixels (for objects ids) */
    PixelChecker fboPixelChecker;

    //-------------------------------------------------------------------------

    public PickSelection() {
        initTimeStamp = TimeUtils.millis();
        for (int i = 0; i < 8; i++) {
            aabbPoints[i] = new Vector3();
        }
    }

    //-------------------------------------------------------------------------

    public void resetFlags() {
        this.stateFlags.reset();
    }

    public boolean hasFlag(int flag) {
        return this.stateFlags.isToggled(flag);
    }

    //-------------------------------------------------------------------------

    public void setPixelChecker(PixelChecker pixelChecker) {
        this.fboPixelChecker = pixelChecker;
    }

    //-------------------------------------------------------------------------

    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void setScreenWidth(int width) {
        this.screenWidth = width;
    }

    public void setScreenHeight(int height) {
        this.screenHeight = height;
    }

    //-------------------------------------------------------------------------

    public void setOnClick() {
        this.setOnClick(true);
    }

    public void setOnClick(boolean toggle) {
        stateFlags.set(StateFlags.SELECTION_ON_CLICK, toggle);
        if (toggle)
            stateFlags.set(StateFlags.SELECTION_ON_HOVER, false); // set onHover mode to false
    }

    public boolean isOnClick() {
        return stateFlags.isToggled(StateFlags.SELECTION_ON_CLICK);
    }

    public void setOnHover() {
        this.setOnHover(true);
    }

    public void setOnHover(boolean toggle) {
        stateFlags.set(StateFlags.SELECTION_ON_HOVER, toggle);
        if (toggle)
            stateFlags.set(StateFlags.SELECTION_ON_CLICK, false);
    }

    public boolean isOnHover() {
        return stateFlags.isToggled(StateFlags.SELECTION_ON_HOVER);
    }

    public void setCheckAABBTriangles() {
        this.setCheckAABBTriangles(true);
    }

    public void setCheckAABBTriangles(boolean toggle) {
        stateFlags.set(StateFlags.CHECK_AABB_TRIANGLES, toggle);
    }

    public boolean isCheckAABBTriangles() {
        return stateFlags.isToggled(StateFlags.CHECK_AABB_TRIANGLES);
    }

    public void setCheckFBOPixels(boolean toggle) {
        stateFlags.set(StateFlags.CHECK_FBO_PIXELS, toggle);
    }

    public boolean isCheckFBOPixels() {
        return stateFlags.isToggled(StateFlags.CHECK_FBO_PIXELS);
    }

    public void setGroupSelectionMode() {
        this.setGroupSelectionMode(true);
    }

    public void setGroupSelectionMode(boolean toggle) {
        stateFlags.set(StateFlags.GROUP_SELECTION_MODE, toggle);
    }

    public boolean isGroupSelectionMode() {
        return stateFlags.isToggled(StateFlags.GROUP_SELECTION_MODE);
    }

    public void setToggleSelectionMode() {
        this.setToggleSelectionMode(true);
    }

    public void setToggleSelectionMode(boolean toggle) {
        stateFlags.set(StateFlags.TOGGLE_SELECTION_MODE, toggle);
    }

    public boolean isToggleSelectionMode() {
        return stateFlags.isToggled(StateFlags.TOGGLE_SELECTION_MODE);
    }

    public boolean isPickerActive() {
        return stateFlags.isToggled(StateFlags.PICKER_ACTIVE);
    }

    public void usePickingBox() {
        usePickingBox(true);
    }

    public void usePickingBox(boolean toggle) {
        stateFlags.set(StateFlags.USE_PICKING_BOX, toggle);
    }

    public boolean isUsePickingBox() {
        return stateFlags.isToggled(StateFlags.USE_PICKING_BOX);
    }

    //-------------------------------------------------------------------------

    public void setPickerActive(boolean state) {
        if (state && !isPickerActive())
            pickBox.set(0, 0, 0, 0);
        stateFlags.set(StateFlags.PICKER_ACTIVE, state);

        // if the mode is on click + toggle - do nothing?
        // if the mode is on click + toggle off - clear selection
        // if the mode is on click + group selection + toggle off- clear selection
        // if the mode is on click + group selection + toggle - do nothing
        if (isOnClick() && state) {
            pickPosBegin.set(pickPos);
            // pickTimeBegin = timesys::exact();
            //pickTimeStampBegin = TimeUtils.millis();
            pickTimeStampBegin = ((float) TimeUtils.timeSinceMillis(initTimeStamp)) / 1000.0f;
            if (!isToggleSelectionMode())
                clear(); // this clears selection
        }
    }

    public void setPickerCoord(int x, int y) {
        pickPos.set(x, screenHeight - y);
    }

    public void setPickerCoord(Vector2i coord) {
        pickPos.set(coord.x, screenHeight - coord.y);
    }

    public void setPickerCoord(Vector2 coord) {
        pickPos.set((int) coord.x, screenHeight - (int) coord.y);
    }

    public void click() {
        setPickerActive(true);
    }

    public void unclick() {
        setPickerActive(false);
    }

    public void clear() {
        ObjectMap.Values<PickingInfo> values = this.pickingInfoMap.values();
        pickingInfoPool.freeAll(values.toArray());
        while (values.hasNext()) {
            PickingInfo pickingInfo = values.next();
            pickingInfo.reset();
        } // for each entry
        //pickingInfoMap.clear();
        selectedObjects.clear();
    } // void clear()

    //-------------------------------------------------------------------------

    public Array<SpatialObject> getSpatialObjects() {
        return spatialObjects;
    }

    public Array<SpatialObject> getSelectedObjects() {
        return selectedObjects;
    }

    public SpatialObject getSelectedObject() {
        if (selectedObjects.size == 0)
            return null;
        return selectedObjects.get(0);
    }

    public PickingInfo getSelectedObjectPickingInfo() {
        if (selectedObjects.size == 0)
            return null;
        return pickingInfoMap.get(selectedObjects.get(0).getSpatialObjectID());
    }

    public PickingInfo getSelectedObjectPickingInfo(int index) {
        if (selectedObjects.size == 0)
            return null;
        if (index >= selectedObjects.size)
            return null;
        return pickingInfoMap.get(selectedObjects.get(index).getSpatialObjectID());
    }

    public ObjectMap<Integer, PickingInfo> getPickingInfoMap() {
        return pickingInfoMap;
    }

    public Vector3 getIntersection() {
        SpatialObject spatialObject = getSelectedObject();
        if (spatialObject == null)
            return null;
        int index = spatialObject.getSpatialObjectID();
        PickingInfo pickingInfo = pickingInfoMap.get(Integer.valueOf(index));
        if (pickingInfo != null) {
            return pickingInfo.intersection;
        }
        return null;
    }

    public Vector3 getIntersection(Vector3 intersection) {
        if (intersection == null)
            throw new IllegalArgumentException("intersection cannot be null");
        Vector3 value = getIntersection();
        if (value != null) {
            intersection.set(value);
        } else {
            intersection.set(0.0f, 0.0f, 0.0f);
        }
        return intersection;
    }

    //-------------------------------------------------------------------------

    public boolean hasPicked() {
        return (selectedObjects.size > 0);
    }

    public int count() {
        return selectedObjects.size;
    }

    //-------------------------------------------------------------------------

    public Ray getRay() {
        return ray;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        if (camera == null)
            this.ray = null;
    }

    public void setSpatialObjects(Array<SpatialObject> spatialObjects) {
        this.spatialObjects = spatialObjects;
    }

    public Camera getCamera() {
        return camera;
    }

    public Rectangle getPickBox() {
        return pickBox;
    }

    public Vector2i getPickPosition() {
        return pickPos;
    }

    //-------------------------------------------------------------------------

    protected Result internal_isPicked(PickingInfo pickingInfo) {
        if (pickingInfo == null)
            throw new IllegalArgumentException("pickingInfo cannot be null");
        SpatialObject spatialObject = pickingInfo.spatialObject;
        if (spatialObject == null)
            throw new IllegalArgumentException("spatialObject cannot be null");
        pickingInfo.result = Result.NOT_PICKED;

        boolean status = Intersector.intersectRaySphere(this.ray,
                spatialObject.getPosition(),
                spatialObject.getRadius(),
                pickingInfo.intersection);

        if (status && !isCheckAABBTriangles())
            pickingInfo.result = Result.PICKED_SPHERE;

        if (status && isCheckAABBTriangles()) {
            // FIXME - need to implement checking picking with AABB triangles!
            pickingInfo.result = Result.PICKED_AABB;
        }

        if (isOnClick() && isUsePickingBox() && this.camera != null) {
            BoundingBox boundingBox =
                    pickingInfo.spatialObject.getOriginalBoundingBox();
            boundingBox.getCorner000(aabbPoints[0]);
            boundingBox.getCorner001(aabbPoints[1]);
            boundingBox.getCorner010(aabbPoints[2]);
            boundingBox.getCorner011(aabbPoints[3]);
            boundingBox.getCorner100(aabbPoints[4]);
            boundingBox.getCorner101(aabbPoints[5]);
            boundingBox.getCorner110(aabbPoints[6]);
            boundingBox.getCorner111(aabbPoints[7]);
            internalAABB.inf();
            for (int i = 0; i < 8; i++) {
                aabbPoints[i].mul(pickingInfo.spatialObject.getTransform());
                camera.project(aabbPoints[i]);
                internalAABB.ext(aabbPoints[i].x,
                        aabbPoints[i].y,
                        aabbPoints[i].z);
            }
            internalAABB.getCenter(tmpVec);
            pickingInfo.center.x = (int) tmpVec.x;
            pickingInfo.center.y = (int) tmpVec.y;

            pickingInfo.onScreen.x = internalAABB.min.x;
            pickingInfo.onScreen.y = internalAABB.min.y;
            pickingInfo.onScreen.width = internalAABB.getWidth();
            pickingInfo.onScreen.height = internalAABB.getHeight();
            pickingInfo.pickBoxOverlaps = pickBox.overlaps(pickingInfo.onScreen);
            pickingInfo.pickBoxContains = pickBox.contains(pickingInfo.onScreen);
            boolean boxStatus = pickingInfo.pickBoxOverlaps || pickingInfo.pickBoxContains;
            if (boxStatus && !isCheckFBOPixels()) {
                pickingInfo.result = goodPickResult; // force proper selection result
            } else if (boxStatus && fboPixelChecker != null) {
                int colorValue = pickingInfo.spatialObject.getSpatialObjectID();
                PickSelection.rectangleIntersection(tmpRectangle,
                        pickBox,
                        pickingInfo.onScreen);
                //tmpRectangle.set(pickBox);
                // This function gets now intersection rectangle (needs to detect it)
                if (fboPixelChecker.isColorInPixels(colorValue, tmpRectangle, false)) {
                    pickingInfo.result = goodPickResult;
                }
            } // has fbo pixel checker?
        } else if (!isUsePickingBox() && isCheckFBOPixels() && fboPixelChecker != null) {
            tmpRectangle.x = pickPos.x;
            tmpRectangle.y = pickPos.y;
            tmpRectangle.width = 1.0f;
            tmpRectangle.height = 1.0f;
            int colorValue = pickingInfo.spatialObject.getSpatialObjectID();
            if (fboPixelChecker.isColorInPixels(colorValue, tmpRectangle, false)) {
                pickingInfo.result = goodPickResult;
            }
        }
        return pickingInfo.result;
    } // Result internal_isPicked(...)

    //-------------------------------------------------------------------------

    public PickingInfo performFullCheck(SpatialObject spatialObject) {
        if (spatialObject == null)
            throw new IllegalArgumentException("spatialObject cannot be null");

        final Integer key = Integer.valueOf(spatialObject.getSpatialObjectID());
        PickingInfo pickingInfo = pickingInfoMap.get(key);
        if (pickingInfo == null) {
            //pickingInfo = new PickingInfo();
            pickingInfo = pickingInfoPool.obtain();
            pickingInfo.spatialObject = spatialObject;
            pickingInfoMap.put(key, pickingInfo);
        } else if (pickingInfo.spatialObject == null) {
            pickingInfo.spatialObject = spatialObject;
        }
        pickingInfo.result = this.internal_isPicked(pickingInfo);
        boolean shouldRemove = false;
        boolean shouldAdd = false;
        final int index = selectedObjects.indexOf(spatialObject, true); // obj1 == obj2
        final float ts = pickingInfo.timeStamp;
        final float exact = ((float) TimeUtils.timeSinceMillis(initTimeStamp)) / 1000.0f;
        if (pickingInfo.result == goodPickResult) {
            if (isToggleSelectionMode() && isPickerActive() && !isOnHover() && ts < pickTimeStampBegin) {
                pickingInfo.selected = !pickingInfo.selected;
                if (!pickingInfo.selected) {
                    shouldRemove = true;
                } else {
                    shouldAdd = true;
                }
            } else if (pickingInfo.selected) {
                shouldAdd = false;
                // no toggle mode - already selected, no event
            } else if (!isToggleSelectionMode() || isOnHover()) {
                // not toggle selection mode
                // not checking for picker status
                pickingInfo.selected = true;
                shouldAdd = true;
            }

            if (!isGroupSelectionMode()) {
                // first selected wins! ignore the rest
                // this works only with traverse() functions
                shouldContinue(false);
            }

            if ((isOnHover() || !isGroupSelectionMode()) && shouldAdd) {
                // no grouping
                for (int i = 0; i < selectedObjects.size; i++) {
                    SpatialObject selectedSpatialObject = selectedObjects.get(i);
                    int selectedSpatialObjectID = selectedSpatialObject.getSpatialObjectID();
                    PickingInfo selectedPickingInfo = pickingInfoMap.get(Integer.valueOf(selectedSpatialObjectID));
                    if (selectedPickingInfo != null)
                        selectedPickingInfo.selected = false;
                }
                selectedObjects.clear();
                selectedObjects.add(spatialObject);
                pickingInfo.timeStamp = exact;
                //////DEBUG////System.out.println(spatialObject.getSpatialObjectID() + " selected[" + pickingInfo.selected + "]: ts [" + ts + "]<[" + pickTimeStampBegin + "] pickTimeStamp | shouldRemove: " + shouldRemove);
            }

            if (isGroupSelectionMode() && !isOnHover() && shouldAdd) {
                if (!selectedObjects.contains(spatialObject, true)) {
                    selectedObjects.add(spatialObject);
                    //////DEBUG////System.out.println(spatialObject.getSpatialObjectID() + " added to group, selected[" + pickingInfo.selected + "]: ts [" + ts + "]<[" + pickTimeStampBegin + "] pickTimeStamp | shouldRemove: " + shouldRemove);
                    pickingInfo.timeStamp = exact;
                }
            }
        } else if (shouldUnselect()) {
            shouldRemove = true;
            pickingInfo.selected = false;
        }
        if (shouldRemove && selectedObjects.size > 0 && index >= 0) {
            //////DEBUG////System.out.println(spatialObject.getSpatialObjectID() + " removing from internal obj list [idx:" + index + "]");
            selectedObjects.removeIndex(index);
            pickingInfo.selected = false;
            pickingInfo.timeStamp = exact;
        }
        return pickingInfo;
    } // Result performFullCheck(...)

    //-------------------------------------------------------------------------

    public boolean shouldContinue() {
        return stateFlags.isToggled(StateFlags.INTERNAL_SHOULD_CONTINUE);
    }

    protected void shouldContinue(boolean toggle) {
        stateFlags.set(StateFlags.INTERNAL_SHOULD_CONTINUE, toggle);
    }

    protected boolean shouldUnselect() {
        return stateFlags.isToggled(StateFlags.INTERNAL_SHOULD_UNSELECT);
    }

    protected void shouldUnselect(boolean toggle) {
        stateFlags.set(StateFlags.INTERNAL_SHOULD_UNSELECT, toggle);
    }

    //-------------------------------------------------------------------------

    public void updateRay() {
        if (camera == null)
            return; // can't do
        // The screen coordinates origin is assumed to be in the top left corner
        ray = camera.getPickRay(this.pickPos.x, this.camera.viewportHeight - this.pickPos.y);
    }

    public void refreshPickBoxDimensions() {
        if (pickPos.x < 0)
            pickPos.x = 0;
        if (pickPos.y < 0)
            pickPos.y = 0;
        float sizeX, sizeY, posX, posY;
        posX = pickPosBegin.x;
        posY = pickPosBegin.y;
        sizeX = pickPos.x - pickPosBegin.x;
        sizeY = pickPos.y - pickPosBegin.y;
        if (sizeX < 0) {
            posX = posX + sizeX;
            sizeX *= -1.0f;
        }
        if (sizeY < 0) {
            posY = posY + sizeY;
            sizeY *= -1.0f;
        }
        pickBox.setPosition(posX, posY);
        if (posX + sizeX > screenWidth) {
            sizeX = screenWidth - posX;
        }
        if (posY + sizeY > screenHeight) {
            sizeY = screenHeight - posY;
        }
        pickBox.setSize(sizeX, sizeY);
    }

    public boolean begin() {
        if (stateFlags.isToggled(StateFlags.INTERNAL_BEGIN))
            return false; // IGNORE
        stateFlags.set(StateFlags.INTERNAL_BEGIN, true);
        shouldContinue(false); // shouldCheck // internal ? ? ?
        // isToggle = false;
        // isGroup = false;
        shouldUnselect(false);
        // Rectangle used for drawing (Rectangle class) uses bottom left
        // corner as origin
        // Also drawing methods use this point as origin (BL corner)
        // However event reporting uses... Top Left corner
        // Coordinates are converted in picker position reporting functions
        goodPickResult = Result.NOT_PICKED;
        refreshPickBoxDimensions();
        if (isOnClick()) {
            shouldContinue(isPickerActive());
            // isToggle
            // isGroup
            // checkBox
            if ((!isToggleSelectionMode() && !isGroupSelectionMode()) || (isUsePickingBox() && !isToggleSelectionMode()))
                shouldUnselect(true);
        } else if (isOnHover()) {
            shouldContinue(true);
            // isGroup?
            shouldUnselect(true);
        }

        if (shouldContinue()) {
            updateRay();
            goodPickResult = Result.PICKED_SPHERE;
            if (isCheckAABBTriangles())
                goodPickResult = Result.PICKED_AABB;
            if (isCheckFBOPixels())
                goodPickResult = Result.PICKED_PIXEL;
        }
        return true;
    } // boolean begin()

    public boolean end() {
        if (!stateFlags.isToggled(StateFlags.INTERNAL_BEGIN))
            return false; // IGNORE
        stateFlags.set(StateFlags.INTERNAL_BEGIN, false);
        if (isOnClick()) {
            // lastSelectedNode.reset()
            // pLastNode = NULL;
            if (!isPickerActive())
                pickTimeStampBegin = -1;
        } else if (isOnHover()) {

        }
        return true;
    } // boolean end()

    //-------------------------------------------------------------------------

    public boolean traverse(boolean shouldCallListeners) {
        return traverse(this.spatialObjects, shouldCallListeners);
    }

    public boolean traverse(Array<SpatialObject> spatialObjectsArray) {
        return traverse(spatialObjectsArray, false);
    }

    public boolean traverse(Array<SpatialObject> spatialObjectsArray,
                            boolean shouldCallListeners) {
        if (spatialObjectsArray == null)
            throw new IllegalArgumentException("spatialObjectsArray cannot be null");
        if (!begin())
            return false;
        if (!shouldContinue()) {
            end();
            return false;
        }

        final int numObjects = spatialObjectsArray.size;
        SpatialObject spatialObject;
        boolean wasSelectedBefore = false;
        for (int i = 0; i < numObjects; i++) {
            wasSelectedBefore = false;
            spatialObject = spatialObjectsArray.get(i);
            if (!spatialObject.isVisible())
                continue;
            int objectID = spatialObject.getSpatialObjectID();
            PickingInfo pickingInfo = null;
            if (pickingInfoMap.containsKey(objectID)) {
                pickingInfo = pickingInfoMap.get(objectID);
                wasSelectedBefore = pickingInfo.selected;
            }
            pickingInfo = performFullCheck(spatialObject);

            if (pickingInfo.selected && shouldCallListeners && !wasSelectedBefore) {
                // SELECTED! NEW!
                callOnSelectionListeners(spatialObject, pickingInfo, true);
            } else if (!pickingInfo.selected && shouldCallListeners && wasSelectedBefore) {
                // UNSELECTED!
                callOnSelectionListeners(spatialObject, pickingInfo, false);
            }
            if (!shouldContinue()) {
                // already selected something - no multiple selection is allowed
                break; // !
            }
        } // for each objects

        return end(); // should return true
    } // boolean traverse(...)

    //-------------------------------------------------------------------------

    public static void rectangleIntersection(Rectangle result, Rectangle r1, Rectangle r2) {
        int tx1 = (int) r1.x;
        int ty1 = (int) r1.y;
        int rx1 = (int) r2.x;
        int ry1 = (int) r2.y;
        long tx2 = tx1;
        tx2 += (int) r1.width;
        long ty2 = ty1;
        ty2 += (int) r1.height;
        long rx2 = rx1;
        rx2 += (int) r2.width;
        long ry2 = ry1;
        ry2 += (int) r2.height;
        if (tx1 < rx1) tx1 = rx1;
        if (ty1 < ry1) ty1 = ry1;
        if (tx2 > rx2) tx2 = rx2;
        if (ty2 > ry2) ty2 = ry2;
        tx2 -= tx1;
        ty2 -= ty1;
        // tx2,ty2 will never overflow (they will never be
        // larger than the smallest of the two source w,h)
        // they might underflow, though...
        if (tx2 < Integer.MIN_VALUE) tx2 = Integer.MIN_VALUE;
        if (ty2 < Integer.MIN_VALUE) ty2 = Integer.MIN_VALUE;
        result.x = tx1;
        result.y = ty1;
        result.width = (int) tx2;
        result.height = (int) ty2;
    } // rectangleIntersection(...)

    //-------------------------------------------------------------------------
} // class PickSelection
