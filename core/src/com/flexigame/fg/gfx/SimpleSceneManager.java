package com.flexigame.fg.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.EnumSet;

/**
 *
 */
public class SimpleSceneManager implements Disposable {

    private Environment environment;
    private DirectionalLight directionalLight;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private int screenWidth;
    private int screenHeight;
    private String rootName;

    public enum StateFlags {
        NONE,
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
        SKYBOX_FOLLOWS_CAMERA
    } // enum StateFlags

    private EnumSet<StateFlags> stateFlags;
    /** **/
    private Array<GameObject> gameObjects;
    private Array<SpatialObject> spatialObjects;

    /**
     *
     */
    public SimpleSceneManager() {
        this.screenWidth = Gdx.app.getGraphics().getWidth();
        this.screenHeight = Gdx.app.getGraphics().getHeight();
        this.environment = new Environment();

        this.environment.set(new ColorAttribute(ColorAttribute.AmbientLight,
                0.75f, 0.75f, 0.8f, 1.0f));

        this.directionalLight = new DirectionalLight().set(0.8f, 0.8f, 0.9f, -0.75f, -0.45f, -0.1f);
        this.environment.add(directionalLight);

        this.camera = new PerspectiveCamera(67, screenWidth, screenHeight);
        this.camera.position.set(0.0f, 0.0f, 0.0f);
        this.camera.lookAt(0.0f, 0.0f, 0.0f);
        this.camera.near = 1.0f;
        this.camera.far = 512.0f;
        this.camera.update();

        this.modelBuilder = new ModelBuilder();
        this.modelBatch = new ModelBatch();

        this.gameObjects = new Array<GameObject>();
        this.spatialObjects = new Array<SpatialObject>();

        this.stateFlags = EnumSet.of(StateFlags.LINEAR_TRAVERSE);
    }

    public void deleteAll() {
        this.gameObjects.clear();
        this.spatialObjects.clear();
    }

    @Override
    public void dispose() {
        this.deleteAll();
        this.modelBatch.dispose();
        // dispose manually
        // Model
        // ModelBatch
    }
    //-------------------------------------------------------------------------

    protected void setFlag(StateFlags flag, boolean toggle) {
        if (flag != StateFlags.NONE && toggle == false)
            this.stateFlags.remove(flag);
        else if (toggle && !this.stateFlags.contains(flag))
            this.stateFlags.add(flag);
        else if (flag == StateFlags.NONE)
            this.stateFlags = EnumSet.of(StateFlags.NONE);
    }

    public boolean isFlagActive(StateFlags flag) {
        return this.stateFlags.contains(flag);
    }

    public boolean isLinearTraverse() {
        return isFlagActive(StateFlags.LINEAR_TRAVERSE);
    }

    public boolean isIgnoreCollisions() {
        return isFlagActive(StateFlags.IGNORE_COLLISIONS);
    }

    public boolean isHideNodes() {
        return isFlagActive(StateFlags.HIDE_NODES);
    }

    public boolean isHideSkybox() {
        return isFlagActive(StateFlags.HIDE_SKYBOX);
    }

    public boolean isHideShadows() {
        return isFlagActive(StateFlags.HIDE_SHADOWS);
    }

    public boolean isHideAll() {
        boolean hideRest = (isFlagActive(StateFlags.HIDE_NODES) ||
                isFlagActive(StateFlags.HIDE_SHADOWS) ||
                isFlagActive(StateFlags.HIDE_SKYBOX));
        return (isFlagActive(StateFlags.HIDE_ALL) || hideRest);
    }

    public boolean isFrustumCheckBox() {
        return isFlagActive(StateFlags.FRUSTUM_CHECK_BOX);
    }

    public boolean isFrustumCheckSphere() {
        return isFlagActive(StateFlags.FRUSTUM_CHECK_SPHERE);
    }

    public boolean isFrustumCheck() {
        return (isFlagActive(StateFlags.FRUSTUM_CHECK_BOX) ||
                isFlagActive(StateFlags.FRUSTUM_CHECK_SPHERE));
    }

    public boolean isOcclusionCheck() {
        return isFlagActive(StateFlags.OCCLUSION_CHECK);
    }

    public boolean isShowGroundGrid() {
        return isFlagActive(StateFlags.SHOW_GROUND_GRID);
    }

    public boolean isSkyboxFollowsCamera() {
        return isFlagActive(StateFlags.SKYBOX_FOLLOWS_CAMERA);
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
        this.setFlag(StateFlags.LINEAR_TRAVERSE, toggle);
    }

    public void setIgnoreCollisions(boolean toggle) {
        this.setFlag(StateFlags.IGNORE_COLLISIONS, toggle);
    }

    public void setHideNodes(boolean toggle) {
        this.setFlag(StateFlags.LINEAR_TRAVERSE, toggle);
        if (!toggle)
            this.setFlag(StateFlags.HIDE_ALL, false);
    }

    public void setHideSkybox(boolean toggle) {
        this.setFlag(StateFlags.LINEAR_TRAVERSE, toggle);
        if (!toggle)
            this.setFlag(StateFlags.HIDE_ALL, false);
    }

    public void setHideShadows(boolean toggle) {
        this.setFlag(StateFlags.HIDE_SHADOWS, toggle);
        if (!toggle)
            this.setFlag(StateFlags.HIDE_ALL, false);
    }

    public void setHideAll(boolean toggle) {
        this.setFlag(StateFlags.HIDE_ALL, toggle);
        this.setFlag(StateFlags.HIDE_NODES, toggle);
        this.setFlag(StateFlags.HIDE_SHADOWS, toggle);
        this.setFlag(StateFlags.HIDE_SKYBOX, toggle);
    }

    public void setFrustumCheckBox(boolean toggle) {
        this.setFlag(StateFlags.FRUSTUM_CHECK_BOX, toggle);
        if (toggle)
            this.setFlag(StateFlags.FRUSTUM_CHECK_SPHERE, false);
    }

    public void setFrustumCheckSphere(boolean toggle) {
        this.setFlag(StateFlags.FRUSTUM_CHECK_SPHERE, toggle);
        if (toggle)
            this.setFlag(StateFlags.FRUSTUM_CHECK_BOX, false);
    }

    public void setOcclusionCheck(boolean toggle) {
        this.setFlag(StateFlags.OCCLUSION_CHECK, toggle);
    }

    public void setShowGroundGrid(boolean toggle) {
        this.setFlag(StateFlags.SHOW_GROUND_GRID, toggle);
    }

    public void setSkyboxFollowsCamera(boolean toggle) {
        this.setFlag(StateFlags.SKYBOX_FOLLOWS_CAMERA, toggle);
    }
    //-------------------------------------------------------------------------

    public Environment getEnvironment() {
        return environment;
    }

    public PerspectiveCamera getCamera() {
        return camera;
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

    public EnumSet<StateFlags> getStateFlags() {
        return stateFlags;
    }

    public Array<GameObject> getGameObjects() {
        return gameObjects;
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
        linearTraverse(true);
    }

    public void render() {
        //Gdx.gl.glCullFace(GL20.GL_FRONT);
        this.modelBatch.begin(this.camera);
        if (this.isLinearTraverse())
            linearTraverse(false);
        this.modelBatch.end();
        //Gdx.gl.glCullFace(GL20.GL_BACK);
    }

    protected void linearTraverse(boolean updateOnly) {
        final int numObjects = this.gameObjects.size;
        for (int i = 0; i < numObjects; i++) {
            GameObject gameObject = this.gameObjects.get(i);
            gameObject.update();
            if (!updateOnly && gameObject.isVisible())
                this.modelBatch.render(this.gameObjects.get(i), this.environment);
        } // for each game object in scene
    } // void linearTraverse(...)

    //-------------------------------------------------------------------------

} // class SimpleSceneManager
