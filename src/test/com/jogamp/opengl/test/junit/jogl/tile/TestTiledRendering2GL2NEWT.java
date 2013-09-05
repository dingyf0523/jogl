/**
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.test.junit.jogl.tile;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.TileRendererBase;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.File;
import java.io.IOException;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLOffscreenAutoDrawable;
import javax.media.opengl.GLRunnable;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Demos offscreen {@link GLAutoDrawable} being used for
 * {@link TileRenderer} rendering to produce a PNG file.
 * <p>
 * {@link TileRenderer} is being kicked off from the main thread.
 * </p>
 * <p>
 * {@link TileRenderer} buffer allocation is performed
 * within the pre {@link GLEventListener} 
 * set via {@link TileRendererBase#setGLEventListener(GLEventListener, GLEventListener)}
 * on the main thread. 
 * </p>
  * <p>
 * At tile rendering finish, the viewport and
 * and the original {@link GLEventListener}'s PMV matrix as well.
 * The latter is done by calling it's {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape} method. 
 * </p>
*/
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTiledRendering2GL2NEWT extends UITestCase {
    static long duration = 500; // ms

    @Test
    public void test01() throws IOException {
        doTest();
    }

    void doTest() throws IOException {      
        GLCapabilities caps = new GLCapabilities(null);
        caps.setDoubleBuffered(false);

        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLOffscreenAutoDrawable glad = factory.createOffscreenAutoDrawable(null, caps, null, 256, 256, null);

        final Gears gears = new Gears();
        gears.setDoRotation(false);
        glad.addGLEventListener( gears );

        // Fix the image size for now
        final int imageWidth = glad.getWidth() * 6;
        final int imageHeight = glad.getHeight() * 4;

        final String filename = this.getSnapshotFilename(0, "-tile", glad.getChosenGLCapabilities(), imageWidth, imageHeight, false, TextureIO.PNG, null);
        final File file = new File(filename);

        // Initialize the tile rendering library
        final TileRenderer renderer = new TileRenderer();
        gears.setTileRenderer(renderer);
        renderer.attachToAutoDrawable(glad);
        renderer.setImageSize(imageWidth, imageHeight);

        final GLPixelBuffer.GLPixelBufferProvider pixelBufferProvider = GLPixelBuffer.defaultProviderWithRowStride;
        final boolean[] flipVertically = { false };

        final GLEventListener preTileGLEL = new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                final GL gl = drawable.getGL();
                GLPixelAttributes pixelAttribs = pixelBufferProvider.getAttributes(gl, 3);
                GLPixelBuffer pixelBuffer = pixelBufferProvider.allocate(gl, pixelAttribs, imageWidth, imageHeight, 1, true, 0);
                renderer.setImageBuffer(pixelBuffer);
                if( drawable.isGLOriented() ) {
                    flipVertically[0] = false;
                } else {
                    flipVertically[0] = true;
                }
            }
            @Override
            public void dispose(GLAutoDrawable drawable) {}
            @Override
            public void display(GLAutoDrawable drawable) {}
            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
        };
        renderer.setGLEventListener(preTileGLEL, null);

        do {
            renderer.display();
        } while ( !renderer.eot() );

        renderer.detachFromAutoDrawable();
        gears.setTileRenderer(null);
        
        // Restore viewport and Gear's PMV matrix
        // .. even though we close the demo, this is for documentation!
        glad.invoke(true, new GLRunnable() {
            @Override
            public boolean run(GLAutoDrawable drawable) {
                drawable.getGL().glViewport(0, 0, drawable.getWidth(), drawable.getHeight());
                gears.reshape(drawable, 0, 0, drawable.getWidth(), drawable.getHeight());
                return false;
            }            
        });

        glad.destroy();

        final GLPixelBuffer imageBuffer = renderer.getImageBuffer();
        final TextureData textureData = new TextureData(
                caps.getGLProfile(),
                0 /* internalFormat */,
                imageWidth, imageHeight,
                0, 
                imageBuffer.pixelAttributes,
                false, false, 
                flipVertically[0],
                imageBuffer.buffer,
                null /* Flusher */);

        TextureIO.write(textureData, file);
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestTiledRendering2GL2NEWT.class.getName());
    }    
}