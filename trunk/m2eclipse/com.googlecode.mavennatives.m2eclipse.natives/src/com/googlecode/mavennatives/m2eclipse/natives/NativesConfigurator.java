package com.googlecode.mavennatives.m2eclipse.natives;

import java.util.List;

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
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
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

		System.out.println();
		logger.info("Configuring mvn natives");

		MavenProject mavenProject = request.getMavenProject();

		if (NativesConfigExtractor.isMavenNativesProject(mavenProject)) {

			String relativeNativesPath = NativesConfigExtractor.getNativesPath(mavenProject);

			IPath nativesPath = request.getProject().getFullPath().makeRelative().append(relativeNativesPath);

			logger.info("MavenNatives - Setting nativesPath: " + nativesPath.toString());

			IFile pom = request.getPom();
			executeNativeDependenciesCopy(request, progressMonitor, pom);

			request.getProject().getFolder(relativeNativesPath).refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);

			logger.info("MavenNatives - Done");

		} else {
			logger.info("MavenNatives - Is not a MavenNatives project");
		}

	}

	private void executeNativeDependenciesCopy(ProjectConfigurationRequest request, IProgressMonitor progressMonitor, IFile pom) throws CoreException {
		List<MojoExecution> executions = getMojoExecutions(request, progressMonitor);

		if (executions.size() != 1) {
			throw new IllegalArgumentException();
		}

		MojoExecution execution = executions.get(0);

		maven.execute(request.getMavenSession(), execution, progressMonitor);

		// String nativesPath = NativesConfigExtractor.getNativesPath(request.getMavenProject());

		// IPath nativesDir = request.getMavenProjectFacade().getFullPath(new File(nativesPath));

		// request.getProject().getFolder(nativesDir).refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);

		//
		// MavenExecutionRequest mavenrequest = MavenPlugin.getMaven().createExecutionRequest(progressMonitor);
		//
		// List<String> goals = new ArrayList<String>();
		// goals.add(NativesConfigExtractor.getNativeDependenciesGoal());
		// mavenrequest.setGoals(goals);
		// IMaven maven = MavenPlugin.getMaven();
		// MavenExecutionResult executionResult = maven.execute(mavenrequest, progressMonitor);
		//
		// if (executionResult.hasExceptions()) {
		// List<Throwable> exceptions = executionResult.getExceptions();
		// for (Throwable throwable : exceptions) {
		// // TODO report failed build
		// throwable.printStackTrace();
		// }
		// Exception executionException = (Exception) executionResult.getExceptions().get(0);
		// throw new RuntimeException("Unable to execute " + NativesConfigExtractor.nativeDependenciesGoal + " goal: " + executionException.getMessage(), executionException);
		// } else {
		// logger.info("Native dependencies extracted fine");
		// }
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

	private IClasspathEntry addNativesPathToMavenContainer(IClasspathEntry entry, String nativesPath) {
		IClasspathAttribute nativeAttr = JavaRuntime.newLibraryPathsAttribute(new String[] { nativesPath });
		entry = JavaCore.newContainerEntry(entry.getPath(), entry.getAccessRules(), new IClasspathAttribute[] { nativeAttr }, entry.isExported());
		return entry;
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
		System.out.println("mavenProjectChanged");
		// event.getMavenProject().getProject().getProject()
	}

	public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
		logger.info("configureClassPathCalled");
		System.out.println("configureClassPathCalled");
//		MavenProject mavenProject = facade.getMavenProject();
//		String relativeNativesPath = NativesConfigExtractor.getNativesPath(mavenProject);
//		
//
//		IPath nativesPath = facade.getProject().getFullPath().makeRelative().append(relativeNativesPath);
//		
//		addNativesPathToMavenContainer(classpath.getEntries(),nativesPath.toOSString());
	}

	public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
		logger.info("configureRawClassPathCalled");
		System.out.println("configureRawClassPathCalled");
		MavenProject mavenProject = request.getMavenProject();
		String relativeNativesPath = NativesConfigExtractor.getNativesPath(mavenProject);
		

		IPath nativesPath = request.getProject().getFullPath().makeRelative().append(relativeNativesPath);
		
		addNativesPathToMavenContainer(classpath.getEntryDescriptors(),nativesPath.toOSString());
	}
}
