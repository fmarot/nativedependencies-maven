package com.googlecode.mavennatives.m2eclipse.natives;

import java.util.List;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativesConfigurator extends AbstractJavaProjectConfigurator {

	static Logger logger = LoggerFactory.getLogger(NativesConfigurator.class);

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor progressMonitor) throws CoreException {

		logger.info("Configuring mvn natives !");

		MavenProject mavenProject = request.getMavenProject();

		if (NativesConfigExtractor.isMavenNativesProject(mavenProject)) {

			String relativeNativesPath = NativesConfigExtractor.getNativesPath(mavenProject);

			IProject project = request.getProject();
			IPath relativePath = project.getFullPath().makeRelative();
			IPath nativesPath = relativePath.append(relativeNativesPath);

			logger.info("MavenNatives - Setting nativesPath: " + nativesPath.toString());

			executeNativeDependenciesCopy(request, progressMonitor);

			project.getFolder(relativeNativesPath).refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);

			logger.info("MavenNatives - Done");

		} else {
			logger.info("MavenNatives - Is not a MavenNatives project");
		}

	}

	private void executeNativeDependenciesCopy(ProjectConfigurationRequest request, IProgressMonitor progressMonitor) throws CoreException {
		List<MojoExecution> executions = getMojoExecutions(request, progressMonitor);

		if (executions.size() != 1) {
			throw new IllegalArgumentException();
		}

		MojoExecution execution = executions.get(0);

		maven.execute(request.getMavenSession(), execution, progressMonitor);
	}

	private void addNativesPathToMavenContainer(List<IClasspathEntryDescriptor> entrydescriptors, String nativesPath) {
		for (int i = 0; i < entrydescriptors.size(); i++) {
			IClasspathEntryDescriptor entry = entrydescriptors.get(i);
			if (isMaven2ClasspathContainer(entry.getPath())) {
				IClasspathAttribute nativeAttr = JavaRuntime.newLibraryPathsAttribute(new String[] { nativesPath });
				entry.setClasspathAttribute(nativeAttr.getName(), nativeAttr.getValue());
			}
		}
	}

	public static boolean isMaven2ClasspathContainer(IPath containerPath) {
		return containerPath != null && containerPath.segmentCount() > 0 && IClasspathManager.CONTAINER_ID.equals(containerPath.segment(0));
	}

	@Override
	public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
		logger.info("Configuring Raw Classpath");
		MavenProject mavenProject = request.getMavenProject();
		String relativeNativesPath = NativesConfigExtractor.getNativesPath(mavenProject);

		String nativesPath = request.getProject().getFullPath().makeRelative().append(relativeNativesPath).toOSString();

		addNativesPathToMavenContainer(classpath.getEntryDescriptors(), nativesPath);
	}
}
