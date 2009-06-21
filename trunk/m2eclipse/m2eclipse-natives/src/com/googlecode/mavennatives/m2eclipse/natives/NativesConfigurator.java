package com.googlecode.mavennatives.m2eclipse.natives;

import java.util.Arrays;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

public class NativesConfigurator extends AbstractProjectConfigurator
{

	NativesConfigExtractor configExtractor = new NativesConfigExtractor();

	@Override
	public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor progressMonitor) throws CoreException
	{
		embedder.getLogger().info("MavenNatives - Begin");

		MavenProject mavenProject = request.getMavenProject();

		if (configExtractor.isMavenNativesProject(mavenProject))
		{
			IPath relpath = request.getProject().getProjectRelativePath();
			
			
			
			String relativeNativesPath = configExtractor.getNativesPath(mavenProject);
			
			
			
			IPath nativesPath = request.getProject().getFullPath().makeRelative().append(relativeNativesPath);
			
			
			embedder.getLogger().info("MavenNatives - Setting nativesPath: " + nativesPath.toString());

			IJavaProject javaProject = JavaCore.create(request.getProject().getProject());

			IClasspathEntry[] entries = javaProject.getRawClasspath();
			addNativesPathToMavenContainer(entries, nativesPath.toString());
			javaProject.setRawClasspath(entries, progressMonitor);
			embedder.getLogger().info("MavenNatives - Configured");

			
			MavenExecutionRequest requestExecution = new DefaultMavenExecutionRequest()
				.setBaseDirectory(mavenProject.getFile().getParentFile())
				.setGoals(Arrays.asList(new String[] { NativesConfigExtractor.nativeDependenciesGoal}));

			MavenExecutionResult result = embedder.execute(requestExecution);
			

			if (result.hasExceptions())
			{
				Exception executionException = (Exception) result.getExceptions().get(0);
				throw new RuntimeException("Unable to execute " + NativesConfigExtractor.nativeDependenciesGoal + " goal: " + executionException.getMessage(),executionException);
			}
			
			request.getProject().getFolder(relativeNativesPath).refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);
			
			embedder.getLogger().info("MavenNatives - Done");

		}
		else
		{
			embedder.getLogger().info("MavenNatives - Is not a MavenNatives project");
		}

	}

	private void addNativesPathToMavenContainer(IClasspathEntry[] classpathEntries, String nativesPath)
	{
		for (int i = 0; i < classpathEntries.length; i++)
		{
			IClasspathEntry entry = classpathEntries[i];
			if (BuildPathManager.isMaven2ClasspathContainer(entry.getPath()))
			{
				classpathEntries[i] = addNativesPathToMavenContainer(entry, nativesPath);
			}
		}
	}

	private IClasspathEntry addNativesPathToMavenContainer(IClasspathEntry entry, String nativesPath)
	{
		IClasspathAttribute nativeAttr = JavaCore.newClasspathAttribute(JavaRuntime.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY, nativesPath);
		entry = JavaCore.newContainerEntry(entry.getPath(), entry.getAccessRules(), new IClasspathAttribute[] { nativeAttr }, entry.isExported());
		return entry;
	}

	@Override
	protected void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException
	{
		System.out.println("evento Changed");
		// event.getMavenProject().getProject().getProject()
	}
}
