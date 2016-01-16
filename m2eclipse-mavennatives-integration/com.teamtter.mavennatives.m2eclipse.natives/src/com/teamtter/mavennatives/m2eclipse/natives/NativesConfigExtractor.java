package com.teamtter.mavennatives.m2eclipse.natives;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativesConfigExtractor {
	static Logger log = LoggerFactory.getLogger(NativesConfigExtractor.class);

	static final String groupId = "com.teamtter.mavennatives";
	static final String artifactId = "nativedependencies-maven-plugin";
	static final String nativesPathAttribute = "nativesTargetDir";
	static final String defaultNativesPath = "target/natives";
	static final String nativeDependenciesGoal = "copy";

	public static boolean isMavenNativesProject(MavenProject mavenProject) {
		Plugin nativesMavenPlugin = getNativesPlugin(mavenProject);
		boolean isMavenNativesProject = nativesMavenPlugin != null;
		log.info("isMavenNativesProject = {}", isMavenNativesProject);
		return isMavenNativesProject;
	}

	private static Plugin getNativesPlugin(MavenProject mavenProject) {
		Plugin nativesMavenPlugin = mavenProject.getPlugin(groupId + ":" + artifactId);
		return nativesMavenPlugin;
	}

	public static String getNativesPath(MavenProject mavenProject) {
		Object configuration = getNativesPlugin(mavenProject).getConfiguration();
		String nativesPath = defaultNativesPath;
		if (configuration instanceof Xpp3Dom) {
			Xpp3Dom confDom = (Xpp3Dom) configuration;
			Xpp3Dom nativesPathConfig = confDom.getChild(nativesPathAttribute);
			if (nativesPathConfig != null) {
				String nativesPathConfigValue = nativesPathConfig.getValue();
				if (nativesPathConfigValue != null && !nativesPathConfigValue.equals("")) {
					nativesPath = nativesPathConfigValue;
				}
			}
		}
		log.info("nativesPath = {}", nativesPath);
		return nativesPath;
	}

	public static String getNativeDependenciesGoal() {
		return groupId + ":" + artifactId + ":" + nativeDependenciesGoal;
	}
}
