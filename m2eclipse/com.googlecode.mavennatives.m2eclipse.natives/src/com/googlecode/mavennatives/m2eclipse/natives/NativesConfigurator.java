package com.googlecode.mavennatives.m2eclipse.natives;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IJavaProjectConfigurator;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

public class NativesConfigurator extends AbstractProjectConfigurator implements IJavaProjectConfigurator {

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor progressMonitor) throws CoreException {

		System.out.println();
		MavenLogger.log("Configuring mvn natives");

		MavenProject mavenProject = request.getMavenProject();

		if (NativesConfigExtractor.isMavenNativesProject(mavenProject)) {

			String relativeNativesPath = NativesConfigExtractor.getNativesPath(mavenProject);

			IPath nativesPath = request.getProject().getFullPath().makeRelative().append(relativeNativesPath);

			MavenLogger.log("MavenNatives - Setting nativesPath: " + nativesPath.toString());

			IProject project = request.getProject().getProject();
			IJavaProject javaProject = JavaCore.create(project);

			IClasspathEntry[] entries = javaProject.getRawClasspath();
			addNativesPathToMavenContainer(entries, nativesPath.toString());
			javaProject.setRawClasspath(entries, progressMonitor);
			MavenLogger.log("MavenNatives - Configured");

			IFile pom = request.getPom();
			executeNativeDependenciesCopy(progressMonitor, pom);

			request.getProject().getFolder(relativeNativesPath).refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);

			MavenLogger.log("MavenNatives - Done");

		} else {
			MavenLogger.log("MavenNatives - Is not a MavenNatives project");
		}

	}

	private static void executeNativeDependenciesCopy(IProgressMonitor progressMonitor, IFile pom) throws CoreException {
		MavenPlugin plugin = MavenPlugin.getDefault();
		MavenProjectManager projectManager = plugin.getMavenProjectManager();

		IMavenProjectFacade projectFacade = projectManager.create(pom, false, progressMonitor);

		ResolverConfiguration resolverConfiguration = projectFacade.getResolverConfiguration();
		MavenExecutionRequest mavenrequest = projectManager.createExecutionRequest(pom, resolverConfiguration, progressMonitor);

		List<String> goals = new ArrayList<String>();
		goals.add(NativesConfigExtractor.getNativeDependenciesGoal());
		mavenrequest.setGoals(goals);
		IMaven maven = plugin.getMaven();
		MavenExecutionResult executionResult = maven.execute(mavenrequest, progressMonitor);

		if (executionResult.hasExceptions()) {
			List<Throwable> exceptions = executionResult.getExceptions();
			for (Throwable throwable : exceptions) {
				// TODO report failed build
				throwable.printStackTrace();
			}
			Exception executionException = (Exception) executionResult.getExceptions().get(0);
			throw new RuntimeException("Unable to execute " + NativesConfigExtractor.nativeDependenciesGoal + " goal: " + executionException.getMessage(), executionException);
		} else {
			MavenLogger.log("Native dependencies extracted fine");
		}
	}

	private void addNativesPathToMavenContainer(IClasspathEntry[] classpathEntries, String nativesPath) {
		for (int i = 0; i < classpathEntries.length; i++) {
			IClasspathEntry entry = classpathEntries[i];
			if (BuildPathManager.isMaven2ClasspathContainer(entry.getPath())) {
				classpathEntries[i] = addNativesPathToMavenContainer(entry, nativesPath);
			}
		}
	}

	private IClasspathEntry addNativesPathToMavenContainer(IClasspathEntry entry, String nativesPath) {
		IClasspathAttribute nativeAttr = JavaCore.newClasspathAttribute(JavaRuntime.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY, nativesPath);
		entry = JavaCore.newContainerEntry(entry.getPath(), entry.getAccessRules(), new IClasspathAttribute[] { nativeAttr }, entry.isExported());
		return entry;
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
		System.out.println("mavenProjectChanged");
		// event.getMavenProject().getProject().getProject()
	}

	public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
		MavenLogger.log("configureClassPathCalled");
		System.out.println("configureClassPathCalled");

	}

	public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
		MavenLogger.log("configureRawClassPathCalled");
		System.out.println("configureClassPathCalled");

	}

	@Override
	public AbstractBuildParticipant getBuildParticipant(MojoExecution execution) {
		MavenLogger.log("getBuildParticipant " + execution.toString());
		System.out.println("getBuildParticipant " + execution.toString());

		if (execution.getGroupId().equalsIgnoreCase(NativesConfigExtractor.groupId) && execution.getArtifactId().equalsIgnoreCase(NativesConfigExtractor.artifactId) && execution.getGoal().equalsIgnoreCase(NativesConfigExtractor.nativeDependenciesGoal)) {

			return new AbstractBuildParticipant() {

				public Set<IProject> build(int kind, IProgressMonitor progressMonitor) throws Exception {
					return null;
				}

				@Override
				public void clean(IProgressMonitor progressMonitor) throws CoreException {
					MavenLogger.log("AfterClean mavennatives - " + this.toString());
					IMavenProjectFacade projectFacade = getMavenProjectFacade();
					IFile pom = projectFacade.getPom();
					executeNativeDependenciesCopy(progressMonitor, pom);
				}
			};
		} else {
			return super.getBuildParticipant(execution);
		}
	}

}
