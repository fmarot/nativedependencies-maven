package com.googlecode.nativedependencies.example;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;


public class App 
{
    public static void main( String[] args )
    {
    	int targetWidth = 640;
    	int targetHeight = 480;
    	 
    	
    	
    	try {
    		DisplayMode chosenMode = new DisplayMode(targetWidth,targetHeight);
    		
    	    Display.setDisplayMode(chosenMode);
    	    Display.setTitle("Example Maven Natives");
    	    Display.setFullscreen(false);
    	    Display.create();
    	} catch (LWJGLException e) {
    	    Sys.alert("Error","Unable to create display.");
    	    System.exit(0);
    	}
    	 
    	GL11.glClearColor(0,0,0,0);
    	
    	boolean gameRunning = true;
    	float pos = 0;
    	 
    	int FRAMERATE = 60;

    	while (gameRunning) {
    		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

    	 
    	     GL11.glRotatef(0.6f, 0, 0, 1);
    	     GL11.glBegin(GL11.GL_TRIANGLES);
    	          GL11.glVertex3f(-0.5f,-0.5f,0);
    	          GL11.glVertex3f(0.5f,-0.5f,0);
    	          GL11.glVertex3f(0,0.5f,0);
    	       
    	     GL11.glEnd();
    	 
    	     Display.update();
    	     Display.sync(FRAMERATE);

    	     if (Display.isCloseRequested()) {
    	           gameRunning = false;
    	           Display.destroy();
    	           System.exit(0);
    	     }
    	}
    }
}
