package com.jogamp.opengl.test.junit.graph.demos;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.Region;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;

public class GPUUISceneNewtDemo {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static int SceneMSAASamples = 0;
    static boolean GraphVBAAMode = false;
    static boolean GraphMSAAMode = false;
    static float GraphAutoMode = GPUUISceneGLListener0A.DefaultNoAADPIThreshold;

    public static void main(String[] args) {
        int width = 800, height = 400;
        int x = 10, y = 10;
        if( 0 != args.length ) {
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-smsaa")) {
                    i++;
                    SceneMSAASamples = MiscUtils.atoi(args[i], SceneMSAASamples);
                    GraphMSAAMode = false;
                    GraphVBAAMode = false;
                    GraphAutoMode = 0f;
                } else if(args[i].equals("-gmsaa")) {
                    GraphMSAAMode = true;
                    GraphVBAAMode = false;
                    GraphAutoMode = 0f;
                } else if(args[i].equals("-gvbaa")) {
                    GraphMSAAMode = false;
                    GraphVBAAMode = true;
                    GraphAutoMode = 0f;
                } else if(args[i].equals("-gauto")) {
                    GraphMSAAMode = false;
                    GraphVBAAMode = true;
                    i++;
                    GraphAutoMode = MiscUtils.atof(args[i], GraphAutoMode);
                } else if(args[i].equals("-width")) {
                    i++;
                    width = MiscUtils.atoi(args[i], width);
                } else if(args[i].equals("-height")) {
                    i++;
                    height = MiscUtils.atoi(args[i], height);
                } else if(args[i].equals("-x")) {
                    i++;
                    x = MiscUtils.atoi(args[i], x);
                } else if(args[i].equals("-y")) {
                    i++;
                    y = MiscUtils.atoi(args[i], y);
                }
            }
        }
        System.err.println("Desired win size "+width+"x"+height);
        System.err.println("Desired win pos  "+x+"/"+y);
        System.err.println("Scene MSAA Samples "+SceneMSAASamples);
        System.err.println("Graph MSAA Mode "+GraphMSAAMode);
        System.err.println("Graph VBAA Mode "+GraphVBAAMode);
        System.err.println("Graph Auto Mode "+GraphAutoMode+" no-AA dpi threshold");

        GLProfile glp = GLProfile.getGL2ES2();
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        if( SceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(SceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final int rmode;
        if( 0 < GraphAutoMode ) {
            rmode = -1;
        } else if( GraphVBAAMode ) {
            rmode = Region.VBAA_RENDERING_BIT;
        } else if( GraphMSAAMode ) {
            rmode = Region.MSAA_RENDERING_BIT;
        } else {
            rmode = 0;
        }

        final GLWindow window = GLWindow.create(caps);
        window.setPosition(x, y);
        window.setSize(width, height);
        window.setTitle("GraphUI Newt Demo: graph["+Region.getRenderModeString(rmode)+"], msaa "+SceneMSAASamples);

        GPUUISceneGLListener0A sceneGLListener = 0 < GraphAutoMode ? new GPUUISceneGLListener0A(GraphAutoMode, DEBUG, TRACE) :
                                                                     new GPUUISceneGLListener0A(rmode, DEBUG, TRACE);

        window.addGLEventListener(sceneGLListener);
        sceneGLListener.attachInputListenerTo(window);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(60, System.err);
        animator.add(window);

        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyed(WindowEvent e) {
                animator.stop();
            }
        });

        window.setVisible(true);
        animator.start();
    }
}