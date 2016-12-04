package com.flexigame.fg.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.flexigame.fg.utils.AbstractFlags;

/**
 *
 */
public class SimpleSceneManager implements Disposable {

    private Environment environment;
    private DirectionalLight directionalLight;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private ShapeRenderer shapeRenderer;
    private int screenWidth;
    private int screenHeight;
    private String rootName;
    private Vector3 tmpVec;

    public static final class StateFlags extends AbstractFlags {
        static final int NO_FLAGS = 0;
        static final int LINEAR_TRAVERSE = 1;
        static final int IGNORE_COLLISIONS = 2;
        static final int HIDE_NODES = 4;
        static final int HIDE_SKYBOX = 8;
        static final int HIDE_SHADOWS = 16;
        static final int HIDE_ALL = HIDE_NODES + HIDE_SHADOWS + HIDE_SKYBOX;
        static final int FRUSTUM_CHECK_BOX = 32;
        static final int FRUSTUM_CHECK_SPHERE = 64;
        static final int OCCLUSION_CHECK = 128;
        static final int SHOW_GROUND_GRID = 256;
        static final int SKYBOX_FOLLOWS_CAMERA = 512;
        static final int SHOW_BOUNDING_SPHERES = 1024;
        static final int SHOW_BOUNDING_BOXES = 2048;

        public static final int[] values = {NO_FLAGS,
                LINEAR_TRAVERSE,
                IGNORE_COLLISIONS,
                HIDE_NODES,
                HIDE_SKYBOX,
                HIDE_SHADOWS,
                HIDE_ALL,
                FRUSTUM_CHECK_BOX,
                FRUSTUM_CHECK_SPHERE,
                OCCLUSION_CHECK,
                SHOW_GROUND_GRID,
                SKYBOX_FOLLOWS_CAMERA,
                SHOW_BOUNDING_SPHERES,
                SHOW_BOUNDING_BOXES};

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
    } // enum StateFlags

    private final StateFlags stateFlags = new StateFlags(StateFlags.LINEAR_TRAVERSE);
    /** **/
    private Array<GameObject> gameObjects;
    private Array<SpatialObject> spatialObjects;
    /** **/
    private Array<GameObject> visibleObjects;

    /**
     *
     */
    public SimpleSceneManager() {
        this.screenWidth = Gdx.app.getGraphics().getWidth();
        this.screenHeight = Gdx.app.getGraphics().getHeight();
        this.environment = new Environment();

        this.environment.set(new ColorAttribute(ColorAttribute.AmbientLight,
                0.75f, 0.75f, 0.8f, 1.0f));

        this.directionalLight = new DirectionalLight().
                set(0.8f, 0.8f, 0.9f, -0.75f, -0.45f, -0.1f);

        this.environment.add(directionalLight);

        this.camera = new PerspectiveCamera(70, screenWidth, screenHeight);
        this.camera.position.set(0.0f, 0.0f, 0.0f);
        this.camera.lookAt(0.0f, 0.0f, 0.0f);
        this.camera.near = 1.0f;
        this.camera.far = 512.0f;
        this.camera.update();

        this.shapeRenderer = new ShapeRenderer();

        this.modelBuilder = new ModelBuilder();
        this.modelBatch = new ModelBatch();

        this.gameObjects = new Array<GameObject>();
        this.gameObjects.ensureCapacity(16);
        this.spatialObjects = new Array<SpatialObject>();
        this.spatialObjects.ensureCapacity(16);
        this.visibleObjects = new Array<GameObject>();
        this.visibleObjects.ensureCapacity(16);

        this.tmpVec = new Vector3();
    }

    public void deleteAll() {
        this.gameObjects.clear();
        this.spatialObjects.clear();
    }

    @Override
    public void dispose() {
        this.deleteAll();
        this.shapeRenderer.dispose();
        this.modelBatch.dispose();
        // dispose manually
        // Model
        // ModelBatch
    }
    //-------------------------------------------------------------------------

    public void resetFlags() {
        this.stateFlags.reset();
    }

    //-------------------------------------------------------------------------

    public boolean isLinearTraverse() {
        return stateFlags.isToggled(StateFlags.LINEAR_TRAVERSE);
    }

    public boolean isIgnoreCollisions() {
        return stateFlags.isToggled(StateFlags.IGNORE_COLLISIONS);
    }

    public boolean isHideNodes() {
        return stateFlags.isToggled(StateFlags.HIDE_NODES);
    }

    public boolean isHideSkybox() {
        return stateFlags.isToggled(StateFlags.HIDE_SKYBOX);
    }

    public boolean isHideShadows() {
        return stateFlags.isToggled(StateFlags.HIDE_SHADOWS);
    }

    public boolean isHideAll() {
        return stateFlags.isToggled(StateFlags.HIDE_ALL);
    }

    public boolean isFrustumCheckBox() {
        return stateFlags.isToggled(StateFlags.FRUSTUM_CHECK_BOX);
    }

    public boolean isFrustumCheckSphere() {
        return stateFlags.isToggled(StateFlags.FRUSTUM_CHECK_SPHERE);
    }

    public boolean isFrustumCheck() {
        return (stateFlags.isToggled(StateFlags.FRUSTUM_CHECK_BOX) ||
                stateFlags.isToggled(StateFlags.FRUSTUM_CHECK_SPHERE));
    }

    public boolean isOcclusionCheck() {
        return stateFlags.isToggled(StateFlags.OCCLUSION_CHECK);
    }

    public boolean isShowGroundGrid() {
        return stateFlags.isToggled(StateFlags.SHOW_GROUND_GRID);
    }

    public boolean isSkyboxFollowsCamera() {
        return stateFlags.isToggled(StateFlags.SKYBOX_FOLLOWS_CAMERA);
    }

    public boolean isShowBoundingSpheres() {
        return stateFlags.isToggled(StateFlags.SHOW_BOUNDING_SPHERES);
    }

    public boolean isShowBoundingBoxes() {
        return stateFlags.isToggled(StateFlags.SHOW_BOUNDING_BOXES);
    }

    //-------------------------------------------------------------------------

    public void disableDirectionalLighting() {
        if (this.environment.has(DirectionalLightsAttribute.Type)) {
            this.environment.remove(directionalLight);
            Attribute directionalLightsAttribute = this.environment.get(DirectionalLightsAttribute.Type);
            if (directionalLightsAttribute != null) {
                ((DirectionalLightsAttribute) directionalLightsAttribute).lights.removeValue(directionalLight, true);
            }
        }
    } // void disableDirectionalLighting()

    public void enableDirectionLighting() {
        if (directionalLight == null)
            return;
        if (!this.environment.has(DirectionalLightsAttribute.Type)) {
            this.environment.add(directionalLight);
        }
    } // void enableDirectionLighting()

    //-------------------------------------------------------------------------

    public void setLinearTraverse(boolean toggle) {
        stateFlags.set(StateFlags.LINEAR_TRAVERSE, toggle);
    }

    public void setIgnoreCollisions(boolean toggle) {
        stateFlags.set(StateFlags.IGNORE_COLLISIONS, toggle);
    }

    public void setHideNodes(boolean toggle) {
        stateFlags.set(StateFlags.HIDE_NODES, toggle);
    }

    public void setHideSkybox(boolean toggle) {
        stateFlags.set(StateFlags.LINEAR_TRAVERSE, toggle);
    }

    public void setHideShadows(boolean toggle) {
        stateFlags.set(StateFlags.HIDE_SHADOWS, toggle);
    }

    public void setHideAll(boolean toggle) {
        stateFlags.set(StateFlags.HIDE_ALL, toggle);
    }

    public void setFrustumCheckBox(boolean toggle) {
        stateFlags.set(StateFlags.FRUSTUM_CHECK_BOX, toggle);
        if (toggle)
            stateFlags.set(StateFlags.FRUSTUM_CHECK_SPHERE, false);
    }

    public void setFrustumCheckSphere(boolean toggle) {
        stateFlags.set(StateFlags.FRUSTUM_CHECK_SPHERE, toggle);
        if (toggle)
            stateFlags.set(StateFlags.FRUSTUM_CHECK_BOX, false);
    }

    public void setOcclusionCheck(boolean toggle) {
        stateFlags.set(StateFlags.OCCLUSION_CHECK, toggle);
    }

    public void setShowGroundGrid(boolean toggle) {
        stateFlags.set(StateFlags.SHOW_GROUND_GRID, toggle);
    }

    public void setSkyboxFollowsCamera(boolean toggle) {
        stateFlags.set(StateFlags.SKYBOX_FOLLOWS_CAMERA, toggle);
    }

    public void setShowBoundingSpheres(boolean toggle) {
        stateFlags.set(StateFlags.SHOW_BOUNDING_SPHERES, toggle);
    }

    public void setShowBoundingBoxes(boolean toggle) {
        stateFlags.set(StateFlags.SHOW_BOUNDING_BOXES, toggle);
    }
    //-------------------------------------------------------------------------

    public Environment getEnvironment() {
        return environment;
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public ModelBatch getModelBatch() {
        return modelBatch;
    }

    public ModelBuilder getModelBuilder() {
        return modelBuilder;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public String getRootName() {
        return rootName;
    }

    public Array<GameObject> getGameObjects() {
        return gameObjects;
    }

    public Array<GameObject> getVisibleObjects() {
        return visibleObjects;
    }

    public Array<SpatialObject> getSpatialObjects() {
        return spatialObjects;
    }

    public int count() {
        return gameObjects.size;
    }

    public boolean isEmpty() {
        return (this.count() == 0);
    }

    public boolean isVisible(GameObject gameObject) {
        if (gameObject == null)
            return false;
        return visibleObjects.contains(gameObject, true);
    }

    //-------------------------------------------------------------------------

    public SimpleSceneManager setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
        this.camera.viewportWidth = screenWidth;
        this.camera.update();
        return this;
    }

    public SimpleSceneManager setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
        this.camera.viewportHeight = screenHeight;
        this.camera.update();
        return this;
    }

    public void refreshScreenDimensions() {
        this.screenWidth = Gdx.app.getGraphics().getWidth();
        this.screenHeight = Gdx.app.getGraphics().getHeight();
        this.camera.viewportWidth = this.screenWidth;
        this.camera.viewportHeight = this.screenHeight;
        this.camera.update();
    }

    public void refreshScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.camera.viewportWidth = width;
        this.camera.viewportHeight = height;
        this.camera.update();
    }

    //-------------------------------------------------------------------------

    public boolean add(GameObject gameObject) {
        if (gameObject == null)
            return false;
        boolean contains = this.gameObjects.contains(gameObject, true);
        if (contains)
            return false; // cannot add - already exitsts
        this.gameObjects.add(gameObject);
        this.spatialObjects.add(gameObject);
        int index = indexOf(gameObject);
        gameObject.setID(index);
        gameObject.setActive(true); // active as default
        gameObject.setVisible(true); // visible as default
        return true;
    }

    public GameObject add(Model model, String objectName) {
        if (model == null)
            return null;
        GameObject gameObject = new GameObject(model);
        gameObject.setName(objectName);
        boolean status = add(gameObject);
        if (!status)
            gameObject = null; // nullify?
        return gameObject;
    }

    public GameObject add(Model model, String rootNode, boolean mergeTransform, String objectName) {
        if (model == null)
            return null;
        GameObject gameObject = new GameObject(model, rootNode, mergeTransform);
        gameObject.setName(objectName);
        boolean status = add(gameObject);
        if (!status)
            gameObject = null; // nullify?
        return gameObject;
    }

    /*public boolean add(ModelInstance modelInstance, String objectName) {
        if(modelInstance == null)
            return false;
        return status;
    }*/

    //-------------------------------------------------------------------------

    public int indexOf(GameObject gameObject) {
        if (gameObject == null)
            return -1;
        return this.gameObjects.indexOf(gameObject, true);
    }

    public boolean contains(GameObject gameObject) {
        if (gameObject == null)
            return false;
        return this.gameObjects.contains(gameObject, true);
    }

    //-------------------------------------------------------------------------

    public GameObject get(int index) {
        GameObject gameObject = this.gameObjects.get(index);
        return gameObject;
    }

    public GameObject get(String objectName) {
        final int size = this.gameObjects.size;
        int index = -1;
        for (int i = 0, n = size; i < n; i++) {
            if (this.gameObjects.get(i).getName().equals(objectName)) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            return this.gameObjects.get(index);
        }
        return null;
    }

    //-------------------------------------------------------------------------

    public GameObject remove(String objectName) {
        final int size = this.gameObjects.size;
        int index = -1;
        for (int i = 0, n = size; i < n; i++) {
            if (this.gameObjects.get(i).getName().equals(objectName)) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            return this.remove(index);
        }
        return null;
    }

    public GameObject remove(int index) {
        if (index >= gameObjects.size || index < 0)
            return null;
        GameObject gameObject = gameObjects.get(index);
        gameObject.setID(-1); // no longer managed!
        gameObjects.removeIndex(index);
        spatialObjects.removeIndex(index);
        for (int i = index; i < this.gameObjects.size; i++) {
            if (gameObjects.get(i) != null) {
                gameObjects.get(i).setID(i); // reset remaining IDs!
            }
        } // for each next game object!
        return gameObject;
    }

    public GameObject remove(GameObject gameObject) {
        if (gameObject == null)
            return null;
        int index = this.indexOf(gameObject);
        if (index < 0)
            return null; // Scene manager does not contain this game object
        return remove(index);
    }

    //-------------------------------------------------------------------------

    public void update() {
        linearTraverse();
    }

    public void render() {
        //Gdx.gl.glCullFace(GL20.GL_FRONT);
        this.modelBatch.begin(this.camera);
        if (this.isLinearTraverse())
            linearTraverse();
        this.modelBatch.end();
        //Gdx.gl.glCullFace(GL20.GL_BACK);
        if (!isShowBoundingBoxes() && !isShowBoundingSpheres())
            return;
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.identity();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Vector3 center;
        for (int i = 0; i < visibleObjects.size; i++) {
            GameObject gameObject = visibleObjects.get(i);
            center = gameObject.center;
            if (isShowBoundingBoxes()) {
                shapeRenderer.setColor(Color.FIREBRICK);
                shapeRenderer.box(
                        center.x - gameObject.extent.x,
                        center.y - gameObject.extent.y,
                        center.z + gameObject.extent.z,
                        gameObject.dimensions.x,
                        gameObject.dimensions.y,
                        gameObject.dimensions.z
                );
            }
            if (isShowBoundingSpheres()) {
                shapeRenderer.setColor(Color.SCARLET);
                shapeRenderer.translate(center.x, center.y, center.z);
                int segments = Math.max(6, (int) (10 * (float) Math.cbrt(gameObject.radius)));
                shapeRenderer.circle(0.0f, 0.0f, gameObject.radius, segments);
                shapeRenderer.rotate(0.0f, 1.0f, 0.0f, 45.0f);
                shapeRenderer.circle(0.0f, 0.0f, gameObject.radius, segments);
                shapeRenderer.rotate(0.0f, 1.0f, 0.0f, 45.0f);
                shapeRenderer.circle(0.0f, 0.0f, gameObject.radius, segments);
                shapeRenderer.rotate(0.0f, 1.0f, 0.0f, 45.0f);
                shapeRenderer.circle(0.0f, 0.0f, gameObject.radius, segments);
                shapeRenderer.rotate(0.0f, 1.0f, 0.0f, -135.0f);
                shapeRenderer.rotate(1.0f, 0.0f, 0.0f, 90.0f);
                shapeRenderer.circle(0.0f, 0.0f, gameObject.radius, segments);
                shapeRenderer.rotate(1.0f, 0.0f, 0.0f, -90.0f);
                shapeRenderer.translate(-center.x, -center.y, -center.z);
            }
        } // for each visible game object
        shapeRenderer.end();
    } // void render()

    public boolean checkVisibilitySphere(GameObject gameObject) {
        if (gameObject == null)
            throw new NullPointerException("gameObject cannot be null");
        return this.camera.frustum.sphereInFrustum(gameObject.center, gameObject.radius);
    } // boolean checkVisibilitySphere(...)

    public boolean checkVisibilityBox(GameObject gameObject) {
        return this.camera.frustum.boundsInFrustum(gameObject.center, gameObject.dimensions);
    } // boolean checkVisibilityBox(...)

    protected void linearTraverse() {
        final int numObjects = gameObjects.size;
        visibleObjects.clear();
        for (int i = 0; i < numObjects; i++) {
            GameObject gameObject = gameObjects.get(i);
            gameObject.update();

            if (isFrustumCheckBox())
                gameObject.setVisible(checkVisibilityBox(gameObject));
            else if (isFrustumCheckSphere())
                gameObject.setVisible(checkVisibilitySphere(gameObject));

            if (gameObject.isVisible()) {
                visibleObjects.add(gameObject);
                modelBatch.render(gameObject, environment);
            }
        } // for each game object in scene
    } // void linearTraverse(...)

    //-------------------------------------------------------------------------

} // class SimpleSceneManager
