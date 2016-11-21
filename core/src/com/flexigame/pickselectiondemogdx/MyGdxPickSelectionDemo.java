package com.flexigame.pickselectiondemogdx;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.*;
import com.flexigame.fg.gfx.*;

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
            pinchZoomFactor = 48.0f;
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
    Camera camera;

    //-------------------------------------------------------------------------

    String crystalModelPath = "Crystal1/Crystal1.g3db";
    String suitcaseModelPath = "suitcase/suitcase.g3db";
    //String rocksModelPath = "rocks_02/rocks_02.g3db";
    String gasTankModelPath = "gaz_tank/gaz_tank.g3db";
    String bombModelPath = "bomb/bomb.g3db";
    String clockBombModelPath = "clock_bomb/clock_bomb.g3db";
    //String clockBombModelPath = "plastic_barrel/plastic_barrel.g3db";
    String ammoBoxModelPath = "ammo_box/ammo_box.g3db";
    String weaponBoxModelPath = "weapon_box_2/weapon_box.g3db";

    //-------------------------------------------------------------------------

    Material boxMaterial;
    Model boxModel;
    Model crystalModel;
    Model suitcaseModel;
    //Model rocksModel;
    Model gasTankModel;
    Model bombModel;
    Model clockBombModel;
    Model ammoBoxModel;
    Model weaponBoxModel;

    //-------------------------------------------------------------------------

    InputMultiplexer inputMultiplexer;
    MyCameraInputController cameraInputController;

    //-------------------------------------------------------------------------

    PickSelection pickSelection;
    PickSelectionRenderer pickSelectionRenderer;
    PickSelectionFrameBuffer pickSelectionFBO;
    NinePatch selectionBoxNinePatch;

    Array<Color> diffuseColors;

    //-------------------------------------------------------------------------

    @Override
    public void create() {
        // adjust properly pick selection buffer
        assetManager = new AssetManager();
        Texture.setAssetManager(assetManager);

        diffuseColors = new Array<Color>();
        spriteBatch = new SpriteBatch();

        TextureLoader.TextureParameter params = new TextureLoader.TextureParameter();
        params.genMipMaps = false;
        params.magFilter = Texture.TextureFilter.Linear;
        params.minFilter = Texture.TextureFilter.Linear;
        params.wrapU = Texture.TextureWrap.MirroredRepeat;
        params.wrapV = Texture.TextureWrap.MirroredRepeat;

        assetManager.load("white.tga", Texture.class); // just white pixels (easier to use)
        assetManager.load("black.tga", Texture.class); // just black pixels

        assetManager.load(crystalModelPath, Model.class);
        assetManager.load(suitcaseModelPath, Model.class);
        //assetManager.load(rocksModelPath, Model.class);
        assetManager.load(gasTankModelPath, Model.class);
        assetManager.load(bombModelPath, Model.class);
        assetManager.load(clockBombModelPath, Model.class);
        assetManager.load(ammoBoxModelPath, Model.class);
        assetManager.load(weaponBoxModelPath, Model.class);
        assetManager.load("ship.g3db", Model.class);

        loadTextures(params);
        selectionBoxNinePatch = new NinePatch(assetManager.get("selectionBox.png", Texture.class), 2, 2, 2, 2);

        whiteTexture = assetManager.get("white.tga", Texture.class);
        blackTexture = assetManager.get("black.tga", Texture.class);

        sceneManager = new SimpleSceneManager();

        boxMaterial = new Material(
                TextureAttribute.createDiffuse(assetManager.get("box1/diffuse.tga", Texture.class)),
                //TextureAttribute.createDiffuse(whiteTexture),
                TextureAttribute.createNormal(assetManager.get("box1/normal.tga", Texture.class)),
                //TextureAttribute.createSpecular(assetManager.get("box1/specular.tga", Texture.class)),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createAmbient(Color.SKY),
                ColorAttribute.createSpecular(Color.WHITE),
                //new BlendingAttribute(true, 1.0f),
                //new IntAttribute(IntAttribute.CullFace, GL20.NONE),
                new FloatAttribute(FloatAttribute.Shininess, 15.0f)
        );


        camera = sceneManager.getCamera();
        camera.position.set(-92.0f, 64.0f, 36.0f);
        camera.far = 1024.0f;
        //camera.lookAt(0.0f, 0.0f, 16.0f);
        camera.lookAt(10.0f, -8.0f, -48.0f);
        camera.update();

        sceneManager.setScreenWidth(getWidth());
        sceneManager.setScreenHeight(getHeight());
        sceneManager.setFrustumCheckSphere(true);
        pickSelection = new PickSelection();
        pickSelection.setCamera(sceneManager.getCamera());
        pickSelection.setSpatialObjects(sceneManager.getSpatialObjects());
        pickSelection.usePickingBox();
        pickSelection.setGroupSelectionMode();
        //pickSelection.setToggleSelectionMode();

        pickSelection.addOnSelectionListener(new PickSelection.OnSelectionListener() {
            @Override
            public void selectionChanged(SpatialObject spatialObject,
                                         PickSelection.PickingInfo pickingInfo,
                                         boolean selected) {
                String message = "";
                if (pickingInfo.selected) {
                    message = "[" + spatialObject.getSpatialObjectID() + "] SELECTED (" + selected + ")";
                } else {
                    message = "[" + spatialObject.getSpatialObjectID() + "] UNSELECTED (" + selected + ")";
                }
                GameObject gameObject = sceneManager.get(spatialObject.getSpatialObjectID());
                if (gameObject != null) {
                    System.out.println(message + ": " + gameObject.getName());
                }
            } // void selectionChanged(...)
        });

        ModelBuilder modelBuilder = sceneManager.getModelBuilder();
        long attributes = VertexAttributes.Usage.Position;
        attributes |= VertexAttributes.Usage.TextureCoordinates;
        attributes |= VertexAttributes.Usage.Normal;
        attributes |= VertexAttributes.Usage.Tangent;
        attributes |= VertexAttributes.Usage.BiNormal;

        boxModel = modelBuilder.createBox(16.0f, 16.0f, 16.0f, boxMaterial, attributes);
        crystalModel = assetManager.get(crystalModelPath, Model.class);
        suitcaseModel = assetManager.get(suitcaseModelPath, Model.class);
        //rocksModel = assetManager.get(rocksModelPath, Model.class);
        gasTankModel = assetManager.get(gasTankModelPath, Model.class);
        bombModel = assetManager.get(bombModelPath, Model.class);
        clockBombModel = assetManager.get(clockBombModelPath, Model.class);
        ammoBoxModel = assetManager.get(ammoBoxModelPath, Model.class);
        weaponBoxModel = assetManager.get(weaponBoxModelPath, Model.class);

        //crystalModel = assetManager.get("ship.g3db", Model.class);
        sceneManager.add(boxModel, "BOX1").setPosition(-25.0f, -5.0f, 5.0f); // 0
        sceneManager.get("BOX1").setScale(1.25f, 1.25f, 1.25f);

        sceneManager.add(boxModel, "BOX2").setPosition(35.0f, 0.0f, -10.0f); // 1

        sceneManager.add(ammoBoxModel, "AMMOBOX1").setPosition(10.0f, -5.0f, -40.0f); // 2
        sceneManager.get("AMMOBOX1").setScale(0.25f);

        sceneManager.add(crystalModel, "CRYSTAL1").setPosition(10.0f, 15.0f, -100.0f); // 3
        sceneManager.get("CRYSTAL1").setScale(2.0f);

        sceneManager.add(crystalModel, "CRYSTAL2").setPosition(-50.0f, 40.0f, -90.0f); // 4
        sceneManager.get("CRYSTAL2").setScale(2.5f);

        sceneManager.add(suitcaseModel, "SUITCASE1").setPosition(5.0f, 10.0f, 30.0f); // 5
        sceneManager.get("SUITCASE1").setScale(0.2f);

        {
            Node node1 = sceneManager.get("SUITCASE1").nodes.get(0).getChild(1);
            BoundingBox boundingBox = new BoundingBox();
            node1.calculateBoundingBox(boundingBox);
            node1.rotation.setFromAxis(1.0f, 0.0f, 0.0f, -45.0f);
            node1.translation.set(0.0f, boundingBox.getDepth() / 4.0f * (float) Math.sqrt(2.0f), 0.0f);
            node1.calculateTransforms(true);
            sceneManager.get("SUITCASE1").refreshOriginalBoundingBox();
        }

        //sceneManager.add(rocksModel, "ROCKS1").setPosition(45.0f, 25.0f, 0.0f);
        //sceneManager.get("ROCKS1").setScale(0.2f);

        sceneManager.add(clockBombModel, "CLOCKBOMB1").setPosition(45.0f, 25.0f, 0.0f); // 6
        sceneManager.get("CLOCKBOMB1").setScale(0.25f);

        sceneManager.add(gasTankModel, "GASTANK1").setPosition(50.0f, 20.0f, -50.0f); // 7
        sceneManager.get("GASTANK1").setScale(0.2f);

        sceneManager.add(weaponBoxModel, "WEAPONBOX1").setPosition(-55.0f, 0.0f, -35.0f); // 8
        sceneManager.get("WEAPONBOX1").setScale(0.2f);

        sceneManager.add(bombModel, "BOMB1").setPosition(25.0f, 25.0f, -70.0f); // 9
        sceneManager.get("BOMB1").setScale(0.15f);

        cameraInputController = new MyCameraInputController(camera);
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(this);
        inputMultiplexer.addProcessor(cameraInputController);
        Gdx.input.setCatchBackKey(true);
        Gdx.input.setInputProcessor(inputMultiplexer);
        assetManager.finishLoading();

        pickSelection.setScreenDimensions(getWidth(), getHeight());

        pickSelectionFBO = new PickSelectionFrameBuffer(getWidth(), getHeight());
        Gdx.app.debug(APP_NAME_ID, "Finished initializing framebuffer!");
        pickSelectionRenderer = new PickSelectionRenderer(sceneManager,
                pickSelection,
                pickSelectionFBO);
        Gdx.app.debug(APP_NAME_ID, "Finished initializing pick selection renderer.");

        pickSelection.setCheckFBOPixels(true);

        diffuseColors.ensureCapacity(sceneManager.count() + 1);
        for (int i = 0; i < sceneManager.count(); i++) {
            GameObject gameObject = sceneManager.get(i);
            Color color = new Color(((ColorAttribute) gameObject.materials.get(0).get(ColorAttribute.Diffuse)).color);
            diffuseColors.add(color);
        } // for each scene manager object

    } // void create()

    public void loadTextures(TextureLoader.TextureParameter params) {
        assetManager.load("box1/normal.tga", Texture.class, params);
        //assetManager.load("box1/specular.tga", Texture.class, params);
        assetManager.load("box1/diffuse.tga", Texture.class, params);

        assetManager.load("selectionBox.png", Texture.class, params);

        assetManager.finishLoading(); // this will block the screen!
        Gdx.app.debug(APP_NAME_ID, "Finished loading all assets!");
    } // void loadTextures(...)

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
            Color color = diffuseColors.get(i); // should match object index in scene manager
            ((ColorAttribute) gameObject.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
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

    float[][] rotationSpeeds = {
            {0.50f, 1.0f, 0.75f, 32.0f}, // BOX1
            {0.00f, 1.0f, 0.00f, 24.0f}, // BOX2
            {0.25f, 1.0f, 0.25f, -15.0f}, // AMMOBOX1
            {0.25f, 1.0f, 0.10f, 20.0f}, // CRYSTAL1
            {0.50f, 1.0f, 0.15f, -20.0f}, // CRYSTAL2
            {0.00f, 1.0f, 0.00f, 15.0f}, // SUITCASE1
            {0.25f, 1.0f, 0.25f, 25.0f}, // CLOCKBOMB1
            {0.15f, 1.0f, 0.15f, -15.0f}, // GASTANK1
            {0.15f, 1.0f, 0.15f, 15.0f}, // WEAPONBOX1
            {0.25f, 1.0f, 0.50f, -25.0f} // BOMB1
    };

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.7f, 0.7f, 0.7f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        cameraInputController.update();
        final float delta = Gdx.app.getGraphics().getDeltaTime();

        int nObjects = sceneManager.count();
        for (int i = 0; i < rotationSpeeds.length && i < nObjects; i++) {
            float[] rot = rotationSpeeds[i];
            sceneManager.get(i).rotate(rot[0], rot[1], rot[2], rot[3] * delta);
        } // for each object and rotation speed data

        sceneManager.render();
        boolean showFrameBufferTexture = false;
        if (isKeyPressed(Input.Keys.F)) {
            showFrameBufferTexture = true;
        }
        if (Gdx.app.getType() == Application.ApplicationType.Android &&
                (isKeyPressed(Input.Keys.MENU) || isKeyPressed(Input.Keys.BACK))) {
            showFrameBufferTexture = true;
        }
        // Draw framebuffer texture (full screen)
        if (showFrameBufferTexture) {
            spriteBatch.begin();
            spriteBatch.draw(pickSelectionFBO.getTexture(), 0, getHeight(), getWidth(), -getHeight());
            spriteBatch.end();
        }

        // Draw intersection of the picking box with the on-screen box of the selected object
        if (isKeyPressed(Input.Keys.I) && pickSelection.hasPicked() && pickSelection.isPickerActive()) {
            spriteBatch.begin();
            Rectangle tmpRectangle = new Rectangle();
            Rectangle pickBox = pickSelection.getPickBox();
            TextureRegion region = new TextureRegion(pickSelectionFBO.getTexture(), 0.0f, 0.0f, 1.0f, 1.0f);
            int nSelected = pickSelection.count();
            for (int sidx = 0; sidx < nSelected; sidx++) {
                PickSelection.PickingInfo pickingInfo = pickSelection.getSelectedObjectPickingInfo(sidx);
                if (!pickingInfo.pickBoxOverlaps && !pickingInfo.pickBoxContains)
                    continue; // ignore
                if (!pickBox.overlaps(pickingInfo.onScreen))
                    continue; // ignore again (pick selection is after this code)
                GameObject gameObject = (GameObject) pickingInfo.spatialObject;
                PickSelection.rectangleIntersection(tmpRectangle,
                        pickBox,
                        pickingInfo.onScreen);

                region.setRegion(
                        (int) pickSelectionFBO.computePositionX(tmpRectangle.x),
                        (int) pickSelectionFBO.computePositionY(tmpRectangle.y),
                        (int) pickSelectionFBO.computePositionX(tmpRectangle.width),
                        (int) pickSelectionFBO.computePositionY(tmpRectangle.height));

                spriteBatch.draw(region,
                        tmpRectangle.x,
                        tmpRectangle.y + tmpRectangle.height,
                        tmpRectangle.width,
                        -tmpRectangle.height);
            }
            spriteBatch.end();
        } // draw intersection pick boxes

        // Draw current pick selection box (and on-screen boxes for all selected objects)
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
                    //GameObject gameObject = sceneManager.get(gameObjectIndex);
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
        } // draw pick box

        // Update pick selection buffer + traverse spatial objects
        if (pickSelection.isPickerActive()) {
            pickSelection.refreshPickBoxDimensions();
            pickSelectionRenderer.renderToFrameBuffer();
            pickSelectionRenderer.refreshPixelBuffer();
            pickSelection.traverse(true);
            updateObjectsColors(pickSelection.getSelectedObjects());
        }
        //sleep(30); // 30 fps forced
    } // void render()

    //-------------------------------------------------------------------------

    @Override
    public void dispose() {
        Gdx.app.debug(APP_NAME_ID, "dispose() {...}");
        spriteBatch.dispose();
        boxModel.dispose();

        assetManager.dispose();
        sceneManager.dispose();

        pickSelectionRenderer.dispose();
    } // void dispose()

    @Override
    public void resize(int width, int height) {
        Gdx.app.debug(APP_NAME_ID, "resize(width=" + width + ", height=" + height + ") {...}");
        super.resize(width, height);
        if (sceneManager != null) {
            sceneManager.setScreenWidth(width);
            sceneManager.setScreenHeight(height);
        }
        if (pickSelection != null)
            pickSelection.setScreenDimensions(width, height);
    } // void resize(...)

    //-------------------------------------------------------------------------

    int debugDrawCode = 0;

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.L) {
            dumpSelection();
        }
        if (keycode == Input.Keys.V) {
            Array<GameObject> visibleObjects = sceneManager.getVisibleObjects();
            System.out.println("There are currently '" + visibleObjects.size + "' visible objects");
            for (int i = 0; i < visibleObjects.size; i++) {
                System.out.println("Visible object [" + i + "]: '" + visibleObjects.get(i).getName() + "'");
            } // for each visible object
        }
        boolean noModKeys = true;
        if (isKeyPressed(Input.Keys.ALT_LEFT) ||
                isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                isKeyPressed(Input.Keys.CONTROL_LEFT))
            noModKeys = false;
        if (keycode == Input.Keys.B && noModKeys) {
            // Bounding sphere / bounding box
            // 0 - nothing
            // 1 - boxes only
            // 2 - spheres only
            // 3 - both
            debugDrawCode++;
            if (debugDrawCode > 3)
                debugDrawCode = 0;
            switch (debugDrawCode) {
                case 0:
                    sceneManager.setShowBoundingBoxes(false);
                    sceneManager.setShowBoundingSpheres(false);
                    break;
                case 1:
                    sceneManager.setShowBoundingBoxes(true);
                    sceneManager.setShowBoundingSpheres(false);
                    break;
                case 2:
                    sceneManager.setShowBoundingBoxes(false);
                    sceneManager.setShowBoundingSpheres(true);
                    break;
                case 3:
                    sceneManager.setShowBoundingBoxes(true);
                    sceneManager.setShowBoundingSpheres(true);
                    break;
            }
        }

        if (keycode == Input.Keys.SHIFT_LEFT) {
            pickSelection.setToggleSelectionMode(true);
        }

        /*if(keycode == Input.Keys.C) {
            System.out.println("camera eye {"+camera.position.x+"; "+camera.position.y+"; "+camera.position.z+"}");
            System.out.println("camera center {"+(camera.position.x + camera.direction.x)
                    +"; "+(camera.position.y + camera.direction.y)
                    +"; "+(camera.position.z + camera.direction.z)+"}");
        }*/
        return false;
    } // boolean keyDown(...)

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.SHIFT_LEFT) {
            pickSelection.setToggleSelectionMode(false);
        }
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
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == 0) {
            pickSelection.setPickerCoord(screenX, screenY);
            pickSelection.unclick();
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pickSelection.isPickerActive()) {
            pickSelection.setPickerCoord(screenX, screenY);
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

    private long _t_diff = 0, _t_start = 0;

    public void sleep(int fps) {
        if (fps > 0) {
            _t_diff = TimeUtils.millis() - _t_start;
            long targetDelay = 1000 / fps;
            if (_t_diff < targetDelay) {
                try {
                    Thread.sleep(targetDelay - _t_diff);
                } catch (InterruptedException e) {
                }
            }
            _t_start = TimeUtils.millis();
        }
    } // void sleep(int fps)

    //-------------------------------------------------------------------------
} // class MyGdxPickSelectionDemo
