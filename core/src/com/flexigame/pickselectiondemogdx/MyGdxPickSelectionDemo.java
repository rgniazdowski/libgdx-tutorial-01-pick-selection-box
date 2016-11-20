package com.flexigame.pickselectiondemogdx;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.flexigame.fg.gfx.GameObject;
import com.flexigame.fg.gfx.PickSelection;
import com.flexigame.fg.gfx.SimpleSceneManager;
import com.flexigame.fg.gfx.SpatialObject;

public class MyGdxPickSelectionDemo extends ApplicationAdapter implements InputProcessor {

    //-------------------------------------------------------------------------

    /**
     * This is an enhanced version of camera input controller. Additional keys
     * are set for better camera movement - also the way camera is being rotated
     * is managed differently.
     */
    protected static class MyCameraInputController extends CameraInputController {

        public int leftKey = Input.Keys.A;
        protected boolean leftPressed = false;
        public int rightKey = Input.Keys.D;
        protected boolean rightPressed = false;
        public int upKey = Input.Keys.SPACE;
        protected boolean upPresed = false;
        public int downKey = Input.Keys.CONTROL_LEFT;
        protected boolean downPressed = false;

        protected final Vector3 rightVec = new Vector3();
        private final Vector3 tmpV1 = new Vector3();
        private final Vector3 tmpV2 = new Vector3();

        public MyCameraInputController(final Camera camera) {
            super(camera);
            rotateLeftKey = Input.Keys.Q;
            rotateRightKey = Input.Keys.E;


            translateTarget = true;
            forwardTarget = true;
            scrollTarget = true;

            autoUpdate = true;

            rotateButton = Input.Buttons.RIGHT;
            translateButton = Input.Buttons.MIDDLE;
            forwardButton = Input.Buttons.FORWARD;

            translateUnits = 96.0f;
            rotateAngle = 180.0f;
        } // MyCameraInputController

        //---------------------------------------------------------------------

        @Override
        public void update() {
            final float delta = Gdx.graphics.getDeltaTime();
            rightVec.set(camera.direction);
            rightVec.crs(camera.up);

            if (leftPressed) {
                camera.translate(tmpV1.set(rightVec).scl(-delta * translateUnits));
                if (translateTarget)
                    target.add(tmpV1);
            }

            if (rightPressed) {
                camera.translate(tmpV1.set(rightVec).scl(delta * translateUnits));
                if (translateTarget)
                    target.add(tmpV1);
            }

            if (upPresed) {
                camera.translate(tmpV1.set(camera.up).scl(delta * translateUnits));
                if (translateTarget)
                    target.add(tmpV1);
            }

            if (downPressed) {
                camera.translate(tmpV1.set(camera.up).scl(-delta * translateUnits));
                if (translateTarget)
                    target.add(tmpV1);
            }

            super.update();

            if (upPresed || downPressed || leftPressed || rightPressed) {
                if (autoUpdate)
                    camera.update();
            }
        } // void update()

        @Override
        protected boolean process(float deltaX, float deltaY, int button) {
            if (button == rotateButton) {
                tmpV1.set(camera.direction).crs(camera.up).y = 0f;
                camera.rotate(tmpV1.nor(), -deltaY * rotateAngle);
                //camera.rotate(camera.up, -deltaX * rotateAngle);
                camera.rotate(Vector3.Y, deltaX * rotateAngle);
                //camera.rotateAround(target, tmpV1.nor(), deltaY * rotateAngle);
                //camera.rotateAround(target, Vector3.Y, deltaX * -rotateAngle);
            } else if (button == translateButton) {
                camera.translate(tmpV1.set(camera.direction).crs(camera.up).nor().scl(-deltaX * translateUnits));
                camera.translate(tmpV2.set(camera.up).scl(-deltaY * translateUnits));
                if (translateTarget) target.add(tmpV1).add(tmpV2);
            } else if (button == forwardButton) {
                camera.translate(tmpV1.set(camera.direction).scl(deltaY * translateUnits));
                if (forwardTarget) target.add(tmpV1);
            }
            if (autoUpdate) camera.update();
            return true;
            //return super.process(deltaX, deltaY, button);
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == leftKey)
                leftPressed = true;
            if (keycode == rightKey)
                rightPressed = true;
            if (keycode == upKey)
                upPresed = true;
            if (keycode == downKey)
                downPressed = true;
            return super.keyDown(keycode);
        }

        @Override
        public boolean keyUp(int keycode) {
            if (keycode == leftKey)
                leftPressed = false;
            if (keycode == rightKey)
                rightPressed = false;
            if (keycode == upKey)
                upPresed = false;
            if (keycode == downKey)
                downPressed = false;
            return super.keyUp(keycode);
        }

        //---------------------------------------------------------------------

    } // static class MyCameraInputController

    //-------------------------------------------------------------------------

    protected static final String APP_NAME_ID = "PickSelectionDemo";
    protected static final String APP_TITLE = "Pick Selection Demo X";
    public static final int BASE_SCREEN_WIDTH = 720;
    public static final int BASE_SCREEN_HEIGHT = 1280;

    public int getBaseScreenWidth() {
        return BASE_SCREEN_WIDTH;
    }

    public int getBaseScreenHeight() {
        return BASE_SCREEN_HEIGHT;
    }

    public float getScreenScaleX() {
        return ((float) this.getWidth()) / ((float) BASE_SCREEN_WIDTH);
    }

    public float getScreenScaleY() {
        return ((float) this.getHeight()) / ((float) BASE_SCREEN_HEIGHT);
    }

    //-------------------------------------------------------------------------

    public int getWidth() {
        return Gdx.app.getGraphics().getWidth();
    }

    public int getHeight() {
        return Gdx.app.getGraphics().getHeight();
    }

    //-------------------------------------------------------------------------

    Texture blackTexture;
    Texture whiteTexture;
    SpriteBatch spriteBatch;
    AssetManager assetManager;
    SimpleSceneManager sceneManager;
    PickSelection pickSelection;
    NinePatch selectionBoxNinePatch;
    Material box1Material;
    Model box1Model;
    Camera camera;
    Vector3[] aabbPoints;

    //-------------------------------------------------------------------------

    @Override
    public void create() {
        assetManager = new AssetManager();
        Texture.setAssetManager(assetManager);

        aabbPoints = new Vector3[8];
        for (int i = 0; i < 8; i++) {
            aabbPoints[i] = new Vector3();
        }

        spriteBatch = new SpriteBatch();

        TextureLoader.TextureParameter params = new TextureLoader.TextureParameter();
        params.genMipMaps = false;
        params.magFilter = Texture.TextureFilter.Linear;
        params.minFilter = Texture.TextureFilter.Linear;
        params.wrapU = Texture.TextureWrap.MirroredRepeat;
        params.wrapV = Texture.TextureWrap.MirroredRepeat;

        assetManager.load("white.tga", Texture.class); // just white pixels (easier to use)
        assetManager.load("black.tga", Texture.class); // just black pixels

        loadTextures(params);
        selectionBoxNinePatch = new NinePatch(assetManager.get("selectionBox.png", Texture.class), 2, 2, 2, 2);

        whiteTexture = assetManager.get("white.tga", Texture.class);
        blackTexture = assetManager.get("black.tga", Texture.class);

        box1Material = new Material(
                TextureAttribute.createDiffuse(assetManager.get("box1/diffuse.tga", Texture.class)),
                TextureAttribute.createNormal(assetManager.get("box1/normal.tga", Texture.class)),
                TextureAttribute.createSpecular(assetManager.get("box1/specular.tga", Texture.class)),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createAmbient(Color.WHITE),
                ColorAttribute.createSpecular(Color.CLEAR)
        );

        sceneManager = new SimpleSceneManager();
        camera = sceneManager.getCamera();
        camera.position.set(0.0f, 0.0f, 64.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        camera.update();

        sceneManager.setScreenWidth(getWidth());
        sceneManager.setScreenHeight(getHeight());
        pickSelection = new PickSelection();
        pickSelection.setCamera(sceneManager.getCamera());
        pickSelection.setSpatialObjects(sceneManager.getSpatialObjects());
        pickSelection.usePickingBox();
        //pickSelection.setGroupSelectionMode();
        //pickSelection.setToggleSelectionMode();

        pickSelection.addOnSelectionListener(new PickSelection.OnSelectionListener() {
            @Override
            public void selectionChanged(SpatialObject spatialObject,
                                         PickSelection.PickingInfo pickingInfo,
                                         boolean selected) {
                String message = "";
                if(pickingInfo.selected) {
                    message = "SELECTED ("+selected+")";
                } else {
                    message = "UNSELECTED ("+selected+")";
                }
                GameObject gameObject = sceneManager.get(spatialObject.getSpatialObjectID());
                if(gameObject != null) {
                    System.out.println(message+": "+gameObject.getName());
                }
            } // void selectionChanged(...)
        });

        ModelBuilder modelBuilder = sceneManager.getModelBuilder();

        long attributes = VertexAttributes.Usage.Position;
        attributes |= VertexAttributes.Usage.TextureCoordinates;
        attributes |= VertexAttributes.Usage.Normal;
        attributes |= VertexAttributes.Usage.Tangent;
        attributes |= VertexAttributes.Usage.BiNormal;
        box1Model = modelBuilder.createBox(16.0f, 16.0f, 16.0f, box1Material, attributes);

        sceneManager.add(box1Model, "BOX1").setPosition(-16.0f, 0.0f, 5.0f);

        sceneManager.add(box1Model, "BOX2").setPosition(32.0f, 0.0f, -10.0f);

        Gdx.input.setCatchBackKey(true);
        Gdx.input.setInputProcessor(this);
    } // void create()

    public void loadTextures(TextureLoader.TextureParameter params) {
        assetManager.load("box1/normal.tga", Texture.class, params);
        assetManager.load("box1/specular.tga", Texture.class, params);
        assetManager.load("box1/diffuse.tga", Texture.class, params);

        assetManager.load("selectionBox.png", Texture.class, params);

        assetManager.finishLoading(); // this will block the screen!
        Gdx.app.debug(APP_NAME_ID, "Finished loading all assets!");
    }

    //-------------------------------------------------------------------------

    public void dumpSelection() {
        Array<SpatialObject> selectedObjects = pickSelection.getSelectedObjects();
        int cnt = 0;
        for (int i = 0; i < selectedObjects.size; i++) {
            SpatialObject spatialObject = selectedObjects.get(i);
            int id = spatialObject.getSpatialObjectID();
            if (id >= 0) {
                GameObject gameObject = sceneManager.get(id);
                System.out.println("[" + i + "] Selected object: " + gameObject.getName());
                cnt++;
            }
        }
        if (cnt > 0)
            System.out.println();
    } // void dumpSelection()

    public void updateObjectsColors(Array<SpatialObject> selectedObjects) {
        Array<GameObject> gameObjects = sceneManager.getGameObjects();
        for (int i = 0; i < gameObjects.size; i++) {
            GameObject gameObject = gameObjects.get(i);
            ((ColorAttribute) gameObject.materials.get(0).get(ColorAttribute.Diffuse)).color.set(Color.WHITE);
        } // for each game object

        if (selectedObjects == null)
            return;

        for (int i = 0; i < selectedObjects.size; i++) {
            SpatialObject spatialObject = selectedObjects.get(i);
            int id = spatialObject.getSpatialObjectID();
            if (id >= 0) {
                GameObject gameObject = sceneManager.get(id);
                ((ColorAttribute) gameObject.materials.get(0).get(ColorAttribute.Diffuse)).color.set(Color.RED);
            }
        } // for each selected object
    } // void updateObjectsColors(...)

    //-------------------------------------------------------------------------

    Vector3 cameraRightVec = new Vector3();
    BoundingBox internalAABB = new BoundingBox();

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.7f, 0.7f, 0.7f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        final float delta = Gdx.app.getGraphics().getDeltaTime();

        GameObject obj0 = sceneManager.get(0);
        obj0.rotate(0.5f, 1.0f, 0.75f, 32.0f * delta);

        GameObject obj1 = sceneManager.get(1);
        obj1.rotate(0.0f, 1.0f, 0.0f, 24.0f * delta);

        sceneManager.render();

        if (pickSelection.isUsePickingBox() && pickSelection.isPickerActive()) {
            Rectangle pickBox = pickSelection.getPickBox();
            spriteBatch.begin();
            spriteBatch.enableBlending();
            selectionBoxNinePatch.draw(spriteBatch, pickBox.getX(), pickBox.getY(), pickBox.getWidth(), pickBox.getHeight());

            ObjectMap<Integer, PickSelection.PickingInfo> pickingInfoMap = pickSelection.getPickingInfoMap();
            Array<SpatialObject> selectedObjects = pickSelection.getSelectedObjects();
            for (int sid = 0; sid < selectedObjects.size; sid++) {
                SpatialObject spatialObject = selectedObjects.get(sid);
                int gameObjectIndex = spatialObject.getSpatialObjectID();
                if (pickingInfoMap.containsKey(gameObjectIndex)) {
                    GameObject gameObject = sceneManager.get(gameObjectIndex);
                    PickSelection.PickingInfo pickingInfo = pickingInfoMap.get(gameObjectIndex);
                    if (!pickingInfo.selected)
                        continue;
                    selectionBoxNinePatch.draw(spriteBatch,
                     pickingInfo.onScreen.getX(), pickingInfo.onScreen.getY(),
                     pickingInfo.onScreen.getWidth(), pickingInfo.onScreen.getHeight());
                }
            } // for each selected object
            spriteBatch.disableBlending();
            spriteBatch.end();
        }

        if (isKeyPressed(Input.Keys.W)) {
            camera.position.mulAdd(camera.direction, 64.0f * delta);
            camera.update();
        }
        if (isKeyPressed(Input.Keys.S)) {
            //camera.translate(0.0f, 0.0f, 64.0f * delta);
            camera.position.mulAdd(camera.direction, -64.0f * delta);
            camera.update();
        }
        if (isKeyPressed(Input.Keys.A)) {
            cameraRightVec.set(camera.direction);
            cameraRightVec.crs(0.0f, 1.0f, 0.0f);
            camera.position.mulAdd(cameraRightVec, -64.0f * delta);
            camera.update();
        }
        if (isKeyPressed(Input.Keys.D)) {
            cameraRightVec.set(camera.direction);
            cameraRightVec.crs(0.0f, 1.0f, 0.0f);
            camera.position.mulAdd(cameraRightVec, 64.0f * delta);
            camera.update();
        }
        if (isKeyPressed(Input.Keys.SPACE)) {
            camera.translate(0.0f, 64.0f * delta, 0.0f);
            camera.update();
        }
        if (isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            camera.translate(0.0f, -64.0f * delta, 0.0f);
            camera.update();
        }

        if (isKeyPressed(Input.Keys.LEFT)) {

        }
        if (isKeyPressed(Input.Keys.RIGHT)) {

        }
        if (isKeyPressed(Input.Keys.UP)) {

        }
        if (isKeyPressed(Input.Keys.DOWN)) {

        }
    } // void render()

    @Override
    public void dispose() {
        spriteBatch.dispose();
        box1Model.dispose();

        assetManager.dispose();

        sceneManager.dispose();
    } // void dispose()

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sceneManager.setScreenWidth(width);
        sceneManager.setScreenHeight(height);
    }

    //-------------------------------------------------------------------------

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.L) {
            dumpSelection();
        }
        return false;
    } // boolean keyDown(...)

    @Override
    public boolean keyUp(int keycode) {
        return false;
    } // boolean keyUp(...)

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    //-------------------------------------------------------------------------

    public boolean isKeyPressed(int keycode) {
        return Gdx.input.isKeyPressed(keycode);
    }

    public boolean isKeyPressed(String keyname) {
        final int keycode = Input.Keys.valueOf(keyname);
        return Gdx.input.isKeyPressed(keycode);
    }

    public boolean isKeyJustPressed(int keycode) {
        return Gdx.input.isKeyJustPressed(keycode);
    }

    public boolean isKeyJustPressed(String keyname) {
        final int keycode = Input.Keys.valueOf(keyname);
        return Gdx.input.isKeyJustPressed(keycode);
    }

    //-------------------------------------------------------------------------

    boolean wasDragged = false;

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        wasDragged = false;
        if (button == 0) {
            pickSelection.setPickerCoord(screenX, screenY);
            pickSelection.click();
            pickSelection.traverse(true);
            updateObjectsColors(pickSelection.getSelectedObjects());
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == 0) {
            pickSelection.setPickerCoord(screenX, screenY);
            pickSelection.unclick();
            pickSelection.traverse(true);
            updateObjectsColors(pickSelection.getSelectedObjects());
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pickSelection.isPickerActive()) {
            pickSelection.setPickerCoord(screenX, screenY);
            pickSelection.traverse(true);
            updateObjectsColors(pickSelection.getSelectedObjects());
        } else {
            int diffX = Gdx.input.getDeltaX();
            int diffY = Gdx.input.getDeltaY();

            float maxDiff = (float) Math.max(Math.abs(diffX), Math.abs(diffY));

            float axisY = 0.0f;
            if (maxDiff != 0.0f)
                axisY = (float) diffX / maxDiff;
            float axisX = 0.0f;
            if (maxDiff != 0.0f)
                axisX = (float) diffY / maxDiff;

            //System.out.println("X: " + axisX + " | Y: " + axisY);
            if(maxDiff != 0.0f) {
                camera.rotate(0.75f, axisX, axisY, 0.0f);
                camera.update();
            }
        }
        wasDragged = true;
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    //-------------------------------------------------------------------------
} // class MyGdxPickSelectionDemo
