package com.googlecode.mavennatives.m2eclipse.natives;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class NativesConfigExtractor
{
	static final String groupId = "com.googlecode.mavennatives";
	static final String artifactId = "maven-nativedependencies-plugin";
	static final String nativesPathAttribute = "nativesTargetDir";
	static final String defaultNativesPath = "target/natives";
	static final String nativeDependenciesGoal = "copy";

	public boolean isMavenNativesProject(MavenProject mavenProject)
	{
		Plugin nativesMavenPlugin = getNativesPlugin(mavenProject);
		return nativesMavenPlugin!=null;
	}


	private Plugin getNativesPlugin(MavenProject mavenProject)
	{
		Plugin nativesMavenPlugin = mavenProject.getPlugin(groupId + ":" + artifactId);
		return nativesMavenPlugin;
	}
	
	
	public String getNativesPath(MavenProject mavenProject)
	{
		Object configuration = getNativesPlugin(mavenProject).getConfiguration();
		if (configuration instanceof Xpp3Dom)
		{
			Xpp3Dom confDom = (Xpp3Dom) configuration;
			Xpp3Dom nativesPathConfig = confDom.getChild(nativesPathAttribute);
			if(nativesPathConfig!=null)
			{
				String nativesPathConfigValue = nativesPathConfig.getValue();
				if(nativesPathConfigValue!=null && !nativesPathConfigValue.equals(""))
				{
					return nativesPathConfigValue;
				}
			}
		}
		return defaultNativesPath;
	}
	
	public String getNativeDependenciesGoal()
	{
		return groupId+":"+artifactId +":" + nativeDependenciesGoal; 
	}
}
