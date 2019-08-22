package com.takusemba.rtmppublisher.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Environment;
import android.util.Size;
import android.util.TypedValue;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;


/**
 * Created by srikaram on 11-Sep-16.
 */
public class RenderUtil {

    private static final Paint EMPTY_PAINT = new Paint();
    private static final String VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  gl_Position = a_position;\n" +
            "  v_texcoord = a_texcoord;\n" +
            "}\n";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "uniform sampler2D tex_sampler;\n" +
                    "varying vec2 v_texcoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(tex_sampler, v_texcoord);\n" +
                    "}\n";
    private static final float[] TEX_VERTICES = {
            0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
    };
    private static final float[] POS_VERTICES = {
            -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f
    };

    public static String getOurFolder() {
        File root = Environment.getExternalStorageDirectory();
        File savePath = new File(root.getAbsolutePath() + "/Pic2Fro");
        savePath.mkdir();
        return savePath.getAbsolutePath();
    }

    public static float dpTopx(float dp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static Bitmap getResizedBitmap(Bitmap src, int desWidth, int desHeight, Bitmap reUseBitmap) {
        Bitmap dest;
        if (reUseBitmap == null) {
            dest = Bitmap.createBitmap(desWidth, desHeight, Bitmap.Config.ARGB_8888);
        } else {
            dest = reUseBitmap;
        }
        int diffHeight = desHeight - src.getHeight();
        Canvas canvas = new Canvas(dest);
        canvas.save();
        canvas.drawColor(Color.GRAY);
        RectF destRect = new RectF(0f, diffHeight / 3, desWidth, desHeight - (diffHeight / 3));
        canvas.drawBitmap(src, null, destRect, EMPTY_PAINT);
        canvas.restore();
        return dest;
    }

    public static void renderTextures(List<CustomDecorator> decors){//[] textures, int viewWidth, int viewHeight) {
        RenderContext context = createProgram();

        if (context == null) {
            return;
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        for(CustomDecorator decor: decors){

            GLES20.glUseProgram(context.shaderProgram);


            GLES20.glViewport(decor.getPosition().x, decor.getPosition().y, decor.getSize().getWidth(), decor.getSize().getHeight());

            // Set the vertex attributes
            GLES20.glVertexAttribPointer(
                    context.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, context.texVertices);
            GLES20.glEnableVertexAttribArray(context.texCoordHandle);
            GLES20.glVertexAttribPointer(
                    context.posCoordHandle, 2, GLES20.GL_FLOAT, false, 0, context.posVertices);
            GLES20.glEnableVertexAttribArray(context.posCoordHandle);
            // Set the input texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, decor.getTextureId());

            GLES20.glUniform1i(context.texSamplerHandle, 0);
            // Draw!
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        }

        GLES20.glFlush();
        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
    }


    public static RenderContext createProgram() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        if (vertexShader == 0) {
            return null;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (pixelShader == 0) {
            return null;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                String info = GLES20.glGetProgramInfoLog(program);
                GLES20.glDeleteProgram(program);
                program = 0;
                throw new RuntimeException("Could not link program: " + info);
            }
        }
        // Bind attributes and uniforms
        RenderContext context = new RenderContext();
        context.texSamplerHandle = GLES20.glGetUniformLocation(program, "tex_sampler");
        context.texCoordHandle = GLES20.glGetAttribLocation(program, "a_texcoord");
        context.posCoordHandle = GLES20.glGetAttribLocation(program, "a_position");
        context.texVertices = createVerticesBuffer(TEX_VERTICES);
        context.posVertices = createVerticesBuffer(POS_VERTICES);
        context.shaderProgram = program;
        return context;
    }

    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                String info = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                shader = 0;
                throw new RuntimeException("Could not compile shader " + shaderType + ":" + info);
            }
        }
        return shader;
    }

    private static FloatBuffer createVerticesBuffer(float[] vertices) {
        if (vertices.length != 8) {
            throw new RuntimeException("Number of vertices should be four.");
        }
        FloatBuffer buffer = ByteBuffer.allocateDirect(
                vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(vertices).position(0);
        return buffer;
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            // Do nothing
        }
    }

    public static class RenderContext {
        private int shaderProgram;
        private int texSamplerHandle;
        private int texCoordHandle;
        private int posCoordHandle;
        private FloatBuffer texVertices;
        private FloatBuffer posVertices;
    }

    public static int createTexture(Bitmap bitmap) {

        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int[] textures = new int[1];

        GLES20.glGenTextures(textures.length, textures, 0);

        int texture = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return texture;
    }

    public static class CustomDecorator{
        int mTextureId = -1;
        Bitmap mBitmap = null;
        Size mSize;
        Point mPosition;

        public CustomDecorator(Bitmap bitmap, Size size, Point position) {
            mSize = size;
            mPosition = position;

            if(bitmap != null) {
                mBitmap = createScaledBitmap(bitmap, size);
                mTextureId = RenderUtil.createTexture(mBitmap);
                mSize = new Size(size.getWidth(), size.getWidth()*bitmap.getHeight()/bitmap.getWidth());
            }
        }

        public int getTextureId() {
            return mTextureId;
        }

        public void setTextureId(int textureId) {
            mTextureId = textureId;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public Size getSize() {
            return mSize;
        }

        public void setSize(Size size) {
            mSize = size;
        }

        public Point getPosition() {
            return mPosition;
        }

        public void setPosition(Point position) {
            mPosition = position;
        }

        private Bitmap createScaledBitmap(Bitmap bm, Size newSize) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            float scaleWidth = ((float) newSize.getWidth()) / width;
            float scaleHeight = ((float) newSize.getHeight()) / height;
            // CREATE A MATRIX FOR THE MANIPULATION
            Matrix matrix = new Matrix();
            // RESIZE THE BIT MAP
            matrix.postScale(scaleWidth, scaleHeight);

            // "RECREATE" THE NEW BITMAP
            Bitmap resizedBitmap = Bitmap.createBitmap(
                    bm, 0, 0, width, height, matrix, false);
            bm.recycle();
            return resizedBitmap;
        }

        public void updateTexId() {
            if(mBitmap !=null)
                mTextureId = RenderUtil.createTexture(mBitmap);
        }
    }
}
