package com.flexigame.fg.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FlushablePool;
import com.flexigame.fg.utils.Vector2i;

/**
 *
 */
public class PickSelectionRenderer implements PickSelection.PixelChecker, Disposable {
    protected static class RenderablePool extends FlushablePool<Renderable> {
        @Override
        protected Renderable newObject() {
            return new Renderable();
        }

        @Override
        public Renderable obtain() {
            Renderable renderable = super.obtain();
            renderable.environment = null;
            renderable.material = null;
            renderable.meshPart.set("", null, 0, 0, 0);
            renderable.shader = null;
            return renderable;
        }
    }

    Array<Renderable> renderableArray;
    RenderablePool renderablesPool;
    SimpleSceneManager sceneManager;
    PickSelectionFrameBuffer frameBuffer;
    PickSelection pickSelection;
    DefaultShader pickShader;
    String fragmentShaderText = "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "uniform vec4 u_baseColor;\n" +
            "void main()                                  \n" +
            "{                                            \n" +
            "    gl_FragColor = u_baseColor;\n" +
            "}";

    Color pickingColor = new Color();

    //-------------------------------------------------------------------------

    public PickSelectionRenderer(SimpleSceneManager sceneManager,
                                 PickSelection pickSelection,
                                 PickSelectionFrameBuffer frameBuffer) {
        renderableArray = new Array<Renderable>();
        renderablesPool = new RenderablePool();

        Renderable renderable = new Renderable();
        renderable.environment = sceneManager.getEnvironment();
        renderable.material = new Material();
        renderable.meshPart.mesh = new Mesh(true, 4, 4, VertexAttribute.Position());

        pickShader = new DefaultShader(renderable,
                new DefaultShader.Config() {
                    {
                        this.fragmentShader = fragmentShaderText;
                    }
                });

        pickShader.init();

        this.sceneManager = sceneManager;
        this.pickSelection = pickSelection;
        this.frameBuffer = frameBuffer;

        this.pickSelection.setPixelChecker(this);
    } // PickSelectionRenderer()

    //-------------------------------------------------------------------------

    @Override
    public void dispose() {
        renderableArray.clear();
        renderablesPool.clear();
        pickShader.dispose();
        pickSelection.setPixelChecker(null);
    } // void dispose()

    //-------------------------------------------------------------------------

    @Override
    public boolean isColorInPixels(int colorValue, Rectangle area, boolean dump) {
        if (!frameBuffer.isValid())
            throw new RuntimeException("Pick selection buffer is not valid");
        boolean status = false;
        final Rectangle pickBox = pickSelection.getPickBox();
        int area_x = Math.max(frameBuffer.computePositionX((int) area.x - (int) pickBox.x), 0);
        int area_y = Math.max(frameBuffer.computePositionY((int) area.y - (int) pickBox.y), 0);
        int area_width = frameBuffer.computePositionX((int) area.width);
        int area_height = frameBuffer.computePositionY((int) area.height);
        int line_width = frameBuffer.computePositionX((int) pickBox.width);
        if (area_width == 0)
            area_width = 1;
        if (line_width == 0)
            line_width = 1;
        if (area_height == 0)
            area_height = 1;

        int red, green, blue, alpha;
        int gameObjectIndex = 0;
        int finalOffset = 0;
        int pixelSize = frameBuffer.getPixelSize();
        int offset = area_x * pixelSize + area_y * pixelSize * line_width;
        for (int y = 0; y < area_height && !status; y++) {
            for (int x = 0; x < area_width && !status; x++) {
                finalOffset = offset + x * pixelSize + y * pixelSize * line_width;

                red = frameBuffer.bytePixels[finalOffset + 0];
                green = frameBuffer.bytePixels[finalOffset + 1];
                blue = frameBuffer.bytePixels[finalOffset + 2];
                alpha = frameBuffer.bytePixels[finalOffset + 3];
                gameObjectIndex = Color.toIntBits(alpha, blue, green, red);
                gameObjectIndex--; // down one
                if (gameObjectIndex == colorValue) {
                    status = true;
                    break;
                }
            } // for each row in data
        } // for each line in data
        return status;
    } // boolean isColorInPixels(...)

    //-------------------------------------------------------------------------

    public void refreshPixelBuffer() {
        // Need to read just the selection box (or single pixel)
        Rectangle pickBox = pickSelection.getPickBox();
        Vector2i pickPos = pickSelection.getPickPosition();
        int x = pickPos.x, y = pickPos.y, w = 1, h = 1;
        if (pickSelection.isOnClick() && pickSelection.isUsePickingBox()) {
            x = (int) pickBox.x;
            y = (int) pickBox.y;
            w = (int) pickBox.width;
            h = (int) pickBox.height;

            w = frameBuffer.computePositionX(w);
            h = frameBuffer.computePositionX(h);
        }
        x = frameBuffer.computePositionX(x);
        y = frameBuffer.computePositionY(y);
        if (w == 0)
            w = 1;
        if (h == 0)
            h = 1;

        frameBuffer.refreshPixelBuffer(x, y, w, h);
    } // void refreshPixelBuffer()

    //-------------------------------------------------------------------------

    public void renderToFrameBuffer() {
        frameBuffer.bind();
        Gdx.gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glViewport(0, 0, frameBuffer.width, frameBuffer.height);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Array<GameObject> gameObjects = sceneManager.getGameObjects();
        pickShader.begin(sceneManager.getCamera(), sceneManager.getModelBatch().getRenderContext());

        // the rendering loop is based on the code from ModelBatch class
        for (int objectIndex = 0; objectIndex < gameObjects.size; objectIndex++) {
            final GameObject gameObject = gameObjects.get(objectIndex);
            if (!gameObject.isVisible())
                continue; // ignore not visible game objects
            int gameObjectIndex = 1 + gameObject.getID(); // up one!
            Color.rgba8888ToColor(pickingColor, gameObjectIndex);
            pickShader.program.setUniformf("u_baseColor",
                    pickingColor.r,
                    pickingColor.g,
                    pickingColor.b,
                    pickingColor.a);
            final int offset = renderableArray.size;
            gameObject.getRenderables(renderableArray, renderablesPool);
            for (int i = offset; i < renderableArray.size; i++) {
                Renderable renderable = renderableArray.get(i);
                renderable.environment = sceneManager.getEnvironment();
                renderable.shader = pickShader;
                pickShader.render(renderable);
            }
        } // for each game object

        pickShader.end();
        renderablesPool.flush();
        renderableArray.clear();
        frameBuffer.unbind();

        Gdx.gl.glViewport(0, 0,
                sceneManager.getScreenWidth(),
                sceneManager.getScreenHeight());
    } // void renderToFrameBuffer()

    //-------------------------------------------------------------------------

} // class PickSelectionRenderer
