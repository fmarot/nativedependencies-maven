package com.teamtter.mavennatives.m2eclipse.natives;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/** The activator class controls the plug-in life cycle */
public class Activator extends Plugin {

	public static final String PLUGIN_ID = "eclipsenatives";

	/** The shared instance */
	private static Activator plugin;

	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/** @return the shared instance */
	public static Activator getDefault() {
		return plugin;
	}

}
