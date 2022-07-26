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
 * Main class for pick selection - supports checking ray intersections with
 * bounding spheres, axis-aligned boxes, oriented boxes and querying special
 * framebuffer texture. Rendering to the framebuffer must be done separately.
 */
public class PickSelection {

    public interface PixelChecker {
        boolean isColorInPixels(int colorValue, Rectangle area, boolean dump);
    } // interface PixelChecker

    public enum Result {
        NOT_PICKED,
        PICKED_SPHERE,
        PICKED_AABB,
        PICKED_ON_SCREEN_BOX,
        PICKED_OBB_TRIANGLES,
        PICKED_PIXEL; // framebuffer / color texture

        public boolean isPicked() {
            return (this.ordinal() != NOT_PICKED.ordinal());
        }

        public boolean isSphere() {
            return (this.ordinal() == PICKED_SPHERE.ordinal());
        }

        public boolean isAABB() {
            return (this.ordinal() == PICKED_AABB.ordinal());
        }

        public boolean isOnScreenBox() {
            return (this.ordinal() == PICKED_ON_SCREEN_BOX.ordinal());
        }

        public boolean isOBBTriangles() {
            return (this.ordinal() == PICKED_OBB_TRIANGLES.ordinal());
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
        /* Whether or not to check Axis-Aligned Bounding Boxes */
        static final int CHECK_AABBS = 4;
        /* Whether or not to check on screen boxes */
        static final int CHECK_ON_SCREEN_BOXES = 8;
        /* Whether or not to check obb triangles - may be more accurate
         * than picking sphere; AABB is transformed and projected to screen space
         * when using picking box */
        static final int CHECK_OBB_TRIANGLES = 16;
        /* Whether or not to check frame buffer object pixel data (provided externally) */
        static final int CHECK_FBO_PIXELS = 32;
        /* Whether or not the picker is active */
        static final int PICKER_ACTIVE = 64;
        /* Group selection mode - basically it means multi-selection mode */
        static final int GROUP_SELECTION_MODE = 128;
        /* Toggle selection mode - second click deselects;
         * Deselection only occurs on spatial object(!) */
        static final int TOGGLE_SELECTION_MODE = 256;
        /* Whether or not to use the onscreen picking box */
        static final int USE_PICKING_BOX = 512;
        /* Internal flag marking begin() function execution - set to false on end() */
        static final int INTERNAL_BEGIN = 1024;
        /* Whether or not should unselect objects (when picking ray does not collide) */
        static final int INTERNAL_SHOULD_UNSELECT = 2048;
        /* Internal flag - should continue traversing? */
        static final int INTERNAL_SHOULD_CONTINUE = 4096;

        public static final int[] values = {NO_FLAGS, // 0
                SELECTION_ON_CLICK,
                SELECTION_ON_HOVER,
                CHECK_AABBS,
                CHECK_ON_SCREEN_BOXES,
                CHECK_OBB_TRIANGLES,
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
    } // interface OnSelectionListener

    Array<OnSelectionListener> onSelectionListenersArray = new Array<OnSelectionListener>();

    public boolean addOnSelectionListener(OnSelectionListener onSelectionListener) {
        if (onSelectionListener == null)
            throw new NullPointerException("onSelectionListener cannot be null");
        if (onSelectionListenersArray.contains(onSelectionListener, true)) {
            return false;
        }
        onSelectionListenersArray.add(onSelectionListener);
        return true;
    } // boolean addOnSelectionListener(...)

    public boolean removeOnSelectionListener(OnSelectionListener onSelectionListener) {
        if (onSelectionListener == null)
            throw new NullPointerException("onSelectionListener cannot be null");

        return onSelectionListenersArray.removeValue(onSelectionListener, true);
    }

    public Array<OnSelectionListener> getOnSelectionListenersArray() {
        return onSelectionListenersArray;
    }

    protected void callOnSelectionListeners(SpatialObject spatialObject,
                                            PickingInfo pickingInfo,
                                            boolean selected) {
        if (spatialObject == null)
            throw new NullPointerException("spatialObject cannot be null");
        for (int i = 0; i < onSelectionListenersArray.size; i++) {
            OnSelectionListener onSelectionListener = onSelectionListenersArray.get(i);
            onSelectionListener.selectionChanged(spatialObject,
                    pickingInfo,
                    selected); // call!
        } // for each on selection listener
    } // void callOnSelectionListeners(...)

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
            spatialObject = null;
            result = Result.NOT_PICKED;
            intersection.set(0.0f, 0.0f, 0.0f);
            onScreen.set(0, 0, 0, 0);
            radius = 0;
            center.set(0, 0);
            timeStamp = 0.0f;
            selected = false;
            pickBoxContains = false;
            pickBoxOverlaps = false;
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

    public void setOnClick(boolean toggle) {
        stateFlags.set(StateFlags.SELECTION_ON_CLICK, toggle);
        if (toggle)
            stateFlags.set(StateFlags.SELECTION_ON_HOVER, false); // set onHover mode to false
    }

    public boolean isOnClick() {
        return stateFlags.isToggled(StateFlags.SELECTION_ON_CLICK);
    }

    public void setOnHover(boolean toggle) {
        stateFlags.set(StateFlags.SELECTION_ON_HOVER, toggle);
        if (toggle)
            stateFlags.set(StateFlags.SELECTION_ON_CLICK, false);
    }

    public boolean isOnHover() {
        return stateFlags.isToggled(StateFlags.SELECTION_ON_HOVER);
    }

    public void setCheckAABBs(boolean toggle) {
        stateFlags.set(StateFlags.CHECK_AABBS, toggle);
    }

    public boolean isCheckAABBs() {
        return stateFlags.isToggled(StateFlags.CHECK_AABBS);
    }

    public void setCheckOBBTriangles(boolean toggle) {
        stateFlags.set(StateFlags.CHECK_OBB_TRIANGLES, toggle);
    }

    public boolean isCheckOBBTriangles() {
        return stateFlags.isToggled(StateFlags.CHECK_OBB_TRIANGLES);
    }

    public void setCheckOnScreenBoxes(boolean toggle) {
        stateFlags.set(StateFlags.CHECK_ON_SCREEN_BOXES, toggle);
    }

    public boolean isCheckOnScreenBoxes() {
        return stateFlags.isToggled(StateFlags.CHECK_ON_SCREEN_BOXES);
    }

    public void setCheckFBOPixels(boolean toggle) {
        stateFlags.set(StateFlags.CHECK_FBO_PIXELS, toggle);
    }

    public boolean isCheckFBOPixels() {
        return stateFlags.isToggled(StateFlags.CHECK_FBO_PIXELS);
    }

    public void setGroupSelectionMode(boolean toggle) {
        stateFlags.set(StateFlags.GROUP_SELECTION_MODE, toggle);
    }

    public boolean isGroupSelectionMode() {
        return stateFlags.isToggled(StateFlags.GROUP_SELECTION_MODE);
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

    public void setUsePickingBox(boolean toggle) {
        stateFlags.set(StateFlags.USE_PICKING_BOX, toggle);
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
        //pickingInfoPool.freeAll(values.toArray());
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

    public final short[][] aabbTrisIdx = {
            {
                    1, 5, 7
            }, // 0: front 157
            {
                    1, 7, 3
            }, // 1: front 173
            {
                    6, 2, 4
            }, // 2: back 624
            {
                    6, 4, 8
            }, // 3: back 648
            {
                    2, 1, 3
            }, // 4: left 213
            {
                    2, 3, 4
            }, // 5: left 234
            {
                    5, 6, 8
            }, // 6: right 568
            {
                    5, 8, 7
            }, // 7: right 587
            {
                    7, 8, 4
            }, // 8: top 784
            {
                    7, 4, 3
            }, // 9: top 743
            {
                    1, 2, 6
            }, // 10: bottom 126
            {
                    1, 6, 5
            } //  11: bottom 165
    };

    protected Result internal_isPicked(PickingInfo pickingInfo) {
        if (pickingInfo == null)
            throw new IllegalArgumentException("pickingInfo cannot be null");
        SpatialObject spatialObject = pickingInfo.spatialObject;
        if (spatialObject == null)
            throw new IllegalArgumentException("spatialObject cannot be null");
        pickingInfo.result = Result.NOT_PICKED;
        boolean status = Intersector.intersectRaySphere(this.ray,
                spatialObject.getCenter(),
                spatialObject.getRadius(),
                pickingInfo.intersection), triangleStatus, hasCornerPoints = false;

        if (status && !isCheckOBBTriangles())
            pickingInfo.result = Result.PICKED_SPHERE;

        if(isCheckAABBs()) {
            if(Intersector.intersectRayBoundsFast(this.ray,
                    spatialObject.getCenter(),
                    spatialObject.getDimensions())) {
                // intersected with AABB (not OBB)
                pickingInfo.result = Result.PICKED_AABB;
            }
        }

        if (status && isCheckOBBTriangles()) {
            // get bounding box of the model in model space!
            BoundingBox boundingBox = spatialObject.getOriginalBoundingBox();
            boundingBox.getCorner000(aabbPoints[0]);
            boundingBox.getCorner001(aabbPoints[1]);
            boundingBox.getCorner010(aabbPoints[2]);
            boundingBox.getCorner011(aabbPoints[3]);
            boundingBox.getCorner100(aabbPoints[4]);
            boundingBox.getCorner101(aabbPoints[5]);
            boundingBox.getCorner110(aabbPoints[6]);
            boundingBox.getCorner111(aabbPoints[7]);
            for (int i = 0; i < 8; i++) {
                aabbPoints[i].mul(spatialObject.getTransform());
            } // for each aabb point
            hasCornerPoints = true; // for further use if needed
            // 12 triangles of the aabb
            for (int i = 0; i < 12; i++) {
                triangleStatus = Intersector.intersectRayTriangle(this.ray,
                        aabbPoints[aabbTrisIdx[i][0] - 1],
                        aabbPoints[aabbTrisIdx[i][1] - 1],
                        aabbPoints[aabbTrisIdx[i][2] - 1],
                        tmpVec);
                if (triangleStatus) {
                    pickingInfo.result = Result.PICKED_OBB_TRIANGLES;
                    pickingInfo.intersection.set(tmpVec);
                    break;
                }
            } // for each triangle in the aabb
        } // isCheckOBBTriangles()

        tmpRectangle.x = pickPos.x;
        tmpRectangle.y = pickPos.y;
        tmpRectangle.width = 1.0f;
        tmpRectangle.height = 1.0f;

        if (isOnClick() && isUsePickingBox() || isCheckOnScreenBoxes()) {
            internalAABB.inf();
            BoundingBox boundingBox = spatialObject.getOriginalBoundingBox();
            if(!hasCornerPoints) {
                boundingBox.getCorner000(aabbPoints[0]);
                boundingBox.getCorner001(aabbPoints[1]);
                boundingBox.getCorner010(aabbPoints[2]);
                boundingBox.getCorner011(aabbPoints[3]);
                boundingBox.getCorner100(aabbPoints[4]);
                boundingBox.getCorner101(aabbPoints[5]);
                boundingBox.getCorner110(aabbPoints[6]);
                boundingBox.getCorner111(aabbPoints[7]);
                for (int i = 0; i < 8; i++) {
                    aabbPoints[i].mul(spatialObject.getTransform());
                    camera.project(aabbPoints[i]);
                    internalAABB.ext(aabbPoints[i].x,
                            aabbPoints[i].y,
                            aabbPoints[i].z);
                } // for each aabb point
            } else {
                // already have updated aabbPoints - check aabb triangles is true
                for (int i = 0; i < 8; i++) {
                    camera.project(aabbPoints[i]);
                    internalAABB.ext(aabbPoints[i].x,
                            aabbPoints[i].y,
                            aabbPoints[i].z);
                } // for each aabb point
            }
            internalAABB.getCenter(tmpVec);
            pickingInfo.center.x = (int) tmpVec.x;
            pickingInfo.center.y = (int) tmpVec.y;

            pickingInfo.onScreen.x = internalAABB.min.x;
            pickingInfo.onScreen.y = internalAABB.min.y;
            pickingInfo.onScreen.width = internalAABB.getWidth();
            pickingInfo.onScreen.height = internalAABB.getHeight();
            if(isUsePickingBox()) {
                pickingInfo.pickBoxOverlaps = pickBox.overlaps(pickingInfo.onScreen);
                pickingInfo.pickBoxContains = pickBox.contains(pickingInfo.onScreen);
                boolean boxStatus = pickingInfo.pickBoxOverlaps || pickingInfo.pickBoxContains;
                if (boxStatus && !isCheckFBOPixels()) {
                    pickingInfo.result = goodPickResult; // force proper selection result
                } else if (boxStatus && fboPixelChecker != null) {
                    int colorValue = pickingInfo.spatialObject.getSpatialObjectID();
                    Intersector.intersectRectangles(pickBox,
                            pickingInfo.onScreen,
                            tmpRectangle);
                    // This function gets now intersection rectangle (needs to detect it)
                    if (fboPixelChecker.isColorInPixels(colorValue, tmpRectangle, false)) {
                        pickingInfo.result = goodPickResult;
                    }
                } // has fbo pixel checker?
            } else {
                // check on screen boxes
                // tmp rectangle is one pixel in size
                pickingInfo.pickBoxContains = false;
                pickingInfo.pickBoxOverlaps = false;
                boolean boxStatus = pickingInfo.onScreen.overlaps(tmpRectangle) || pickingInfo.onScreen.contains(tmpRectangle);
                if(boxStatus && !isCheckFBOPixels()) {
                    pickingInfo.result = Result.PICKED_ON_SCREEN_BOX;
                } else if(boxStatus && fboPixelChecker != null) {
                    int colorValue = pickingInfo.spatialObject.getSpatialObjectID();
                    if (fboPixelChecker.isColorInPixels(colorValue, tmpRectangle, false)) {
                        pickingInfo.result = goodPickResult;
                    }
                }
            }
        } else if (!isUsePickingBox() && isCheckFBOPixels() && fboPixelChecker != null) {
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

        final Integer key = spatialObject.getSpatialObjectID();
        PickingInfo pickingInfo = pickingInfoMap.get(key);
        if (pickingInfo == null) {
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
                pickingInfo.selected = true;
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
            //System.out.println(spatialObject.getSpatialObjectID() + " should unselect true, selected[" + pickingInfo.selected + "]: ts [" + ts + "]<[" + pickTimeStampBegin + "] pickTimeStamp | shouldRemove: " + shouldRemove);
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
        camera.update();
        // The screen coordinates origin is assumed to be in the top left corner
        ray = camera.getPickRay(this.pickPos.x, this.camera.viewportHeight - this.pickPos.y);
    }

    public void refreshPickBoxDimensions() {
        if (pickPos.x < 0)
            pickPos.x = 0;
        if (pickPos.y < 0)
            pickPos.y = 0;
        if (!isUsePickingBox()) {
            pickBox.set(pickPos.x, pickPos.y, 1.0f, 1.0f);
            return;
        }
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
            return false; // ignore it!
        stateFlags.set(StateFlags.INTERNAL_BEGIN, true);
        shouldContinue(false);
        shouldUnselect(false);
        // Rectangle used for drawing uses bottom left corner as origin,
        // Drawing methods also use this point as origin (BL corner).
        // However input event reporting uses... Top Left corner!
        // Coordinates are converted in picker position reporting functions.
        goodPickResult = Result.NOT_PICKED;
        refreshPickBoxDimensions();
        if (isOnClick()) {
            shouldContinue(isPickerActive());
            if ((!isToggleSelectionMode() && !isGroupSelectionMode()) ||
                    (isUsePickingBox() && !isToggleSelectionMode())) {
                shouldUnselect(true);
            }
        } else if (isOnHover()) {
            shouldContinue(true);
            shouldUnselect(true);
        }

        if (shouldContinue()) {
            updateRay();
            goodPickResult = Result.PICKED_SPHERE;
            if(isCheckAABBs())
                goodPickResult = Result.PICKED_AABB;
            if(isCheckOnScreenBoxes())
                goodPickResult = Result.PICKED_ON_SCREEN_BOX;
            if (isCheckOBBTriangles())
                goodPickResult = Result.PICKED_OBB_TRIANGLES;
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
            if (!isPickerActive())
                pickTimeStampBegin = -1;
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
        boolean wasSelectedBefore;
        for (int i = 0; i < numObjects; i++) {
            wasSelectedBefore = false;
            spatialObject = spatialObjectsArray.get(i);
            if (!spatialObject.isVisible())
                continue;
            final int objectID = spatialObject.getSpatialObjectID();
            PickingInfo pickingInfo;
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

} // class PickSelection
