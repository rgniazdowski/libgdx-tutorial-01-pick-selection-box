package com.flexigame.fg.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 *
 */
public class PickSelectionFrameBuffer {

    public static final int FBO_DEFAULT_WIDTH = 256;
    public static final int FBO_DEFAULT_HEIGHT = 256;

    /* byte buffer holder - used for acquiring data from the frame buffer */
    ByteBuffer byteBuffer = null;
    /* readable array of byte pixels */
    byte[] bytePixels = null;
    /* GL handle id for the frame buffer object */
    int frameBufferObject = 0;
    /* GL handler id for the render buffer object */
    int renderBufferObject = 0;
    Texture texture = null;

    /* width of the frame buffer object - const */
    final int width = FBO_DEFAULT_WIDTH;
    /* height of the frame buffer object */
    int height = FBO_DEFAULT_HEIGHT;
    /* Current width of the screen in pixels */
    int screenWidth = 0;
    /* Current height of the screen in pixels */
    int screenHeight = 0;

    /* Is this supported on wide range of devices? Should use RGB565? How? */
    private Pixmap.Format format = Pixmap.Format.RGBA8888;
    /* Size of the single pixel in bytes - used for allocation */
    private int pixelSize = 4;

    int glSupportedReadFormat = GL20.GL_RGBA;
    int glSupportedReadType = GL20.GL_UNSIGNED_BYTE;
    int currentReadFormat = glSupportedReadFormat;
    int currentReadType = glSupportedReadType;

    private boolean valid = false;

    //-------------------------------------------------------------------------

    public PickSelectionFrameBuffer(int screenWidth, int screenHeight) {
        IntBuffer intBuffer = BufferUtils.newIntBuffer(16);
        Gdx.gl20.glGetIntegerv(GL20.GL_IMPLEMENTATION_COLOR_READ_TYPE, intBuffer);
        glSupportedReadType = intBuffer.get(0);
        intBuffer.position(0);
        Gdx.gl20.glGetIntegerv(GL20.GL_IMPLEMENTATION_COLOR_READ_FORMAT, intBuffer);
        glSupportedReadFormat = intBuffer.get(0);

        /*System.out.println("glSupportedReadType = "+glSupportedReadType+";");
        System.out.println("GL20.GL_UNSIGNED_BYTE = "+GL20.GL_UNSIGNED_BYTE+";");
        System.out.println("GL20.GL_UNSIGNED_SHORT_5_6_5 = "+GL20.GL_UNSIGNED_SHORT_5_6_5+";");
        System.out.println("glSupportedReadFormat = "+glSupportedReadFormat+";");
        System.out.println("GL20.GL_RGBA = "+GL20.GL_RGBA+";");
        System.out.println("GL20.GL_RGB = "+GL20.GL_RGB+";");*/
        initialize(screenWidth, screenHeight);
    } // PickSelectionFrameBuffer()

    //-------------------------------------------------------------------------

    public void initialize(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        // need to use proportion
        height = (int) ((float) screenHeight * (((float) width) / ((float) screenWidth)));

        // should reinitialize texture with new size?
        // this can be really inefficient if the screen size changes often, like
        // when resizing using mouse pointer. Use delay?
        frameBufferObject = Gdx.gl20.glGenFramebuffer();
        Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, frameBufferObject);

        {
            texture = new Texture(width, height, format);
            texture.setFilter(Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest);
            texture.setWrap(Texture.TextureWrap.ClampToEdge,
                    Texture.TextureWrap.ClampToEdge);
            texture.bind();
        } // pick selection texture preparation

        renderBufferObject = Gdx.gl20.glGenRenderbuffer();
        Gdx.gl20.glBindRenderbuffer(GL20.GL_RENDERBUFFER, renderBufferObject);
        Gdx.gl20.glRenderbufferStorage(GL20.GL_RENDERBUFFER,
                GL20.GL_DEPTH_COMPONENT16, width, height);
        Gdx.gl20.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);

        // attach the texture and the render buffer to the frame buffer
        Gdx.gl20.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER,
                GL20.GL_COLOR_ATTACHMENT0,
                GL20.GL_TEXTURE_2D,
                texture.getTextureObjectHandle(), 0);
        Gdx.gl20.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER,
                GL20.GL_DEPTH_ATTACHMENT,
                GL20.GL_RENDERBUFFER,
                renderBufferObject);

        if (Gdx.gl20.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER) != GL20.GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("Framebuffer for pick selection is not complete!");
            valid = false;
        } else {
            valid = true;
        }

        Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);
        {
            byteBuffer = BufferUtils.newByteBuffer(width * height * pixelSize);
            bytePixels = new byte[width * height * pixelSize];
        }
    } // void initialize(...)

    //-------------------------------------------------------------------------

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public byte[] getBytePixels() {
        return bytePixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Texture getTexture() {
        return texture;
    }

    public Pixmap.Format getFormat() {
        return format;
    }

    public int getPixelSize() {
        return pixelSize;
    }

    public boolean isValid() {
        return valid;
    }

    //-------------------------------------------------------------------------

    public void bind() {
        Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, frameBufferObject);
    }

    public void unbind() {
        Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);
    }

    public int computePositionX(int position) {
        return (int) (position / (float) screenWidth * (float) width);
    }

    public int computePositionY(int position) {
        return (int) (position / (float) screenHeight * (float) height);
    }

    public float computePositionX(float position) {
        return (position / (float) screenWidth * (float) width);
    }

    public float computePositionY(float position) {
        return (position / (float) screenHeight * (float) height);
    }

    //-------------------------------------------------------------------------

    public void refreshPixelBuffer(int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w < 0 || h < 0) {
            throw new IllegalArgumentException("position and size parameters cannot be less than 0");
        }
        Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, frameBufferObject);
        Gdx.gl20.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 4);
        if (x + w > width)
            w = width - x;
        if (y + h > height)
            h = height - y;
        int length = w * h * pixelSize;
        if (length > bytePixels.length)
            length = bytePixels.length; // protect against overflow (throw?)
        byteBuffer.position(0);
        Gdx.gl.glReadPixels(x, y, w, h,
                currentReadFormat, currentReadType, byteBuffer);
        byteBuffer.position(0);
        byteBuffer.get(bytePixels, 0, length);
        Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);
    } // void refreshPixelBuffer(...)

    //-------------------------------------------------------------------------

} // class PickSelectionFrameBuffer
