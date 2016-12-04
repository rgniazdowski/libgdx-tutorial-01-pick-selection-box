package com.flexigame.pickselectiondemogdx.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.flexigame.pickselectiondemogdx.MyGdxPickSelectionDemo;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1024;
		config.height = 600;
		config.resizable = false;
		config.allowSoftwareMode = false;
		config.forceExit = true;
		config.backgroundFPS = 24;
		config.fullscreen = false;
		config.title = "Pick Selection Box Demo - flexigame.com Technical Blog";
		new LwjglApplication(new MyGdxPickSelectionDemo(), config);
	}
} // class DesktopLauncher
