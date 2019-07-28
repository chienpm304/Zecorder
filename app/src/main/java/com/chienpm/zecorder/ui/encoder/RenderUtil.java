package com.chienpm.zecorder.ui.encoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.TypedValue;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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

//    public static String buildVideoName(String fileName) {
//        return R.getOurFolder() + "/" + fileName + ".mp4";
//    }

    public static void renderTexture(int texture, int viewWidth, int viewHeight) {
        RenderContext context = createProgram();
        if (context == null) {
            return;
        }
        // Use our shader program
        GLES20.glUseProgram(context.shaderProgram);
        // Set viewport
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        // Set the vertex attributes
        GLES20.glVertexAttribPointer(
                context.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, context.texVertices);
        GLES20.glEnableVertexAttribArray(context.texCoordHandle);
        GLES20.glVertexAttribPointer(
                context.posCoordHandle, 2, GLES20.GL_FLOAT, false, 0, context.posVertices);
        GLES20.glEnableVertexAttribArray(context.posCoordHandle);
        // Set the input texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(context.texSamplerHandle, 0);
        // Draw!
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
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
}
