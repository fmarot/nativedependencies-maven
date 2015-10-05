// package com.teamtter.mavennatives.nativedependencies;
//
// import java.io.File;
// import java.io.IOException;
// import java.util.HashSet;
// import java.util.Set;
//
// import org.apache.maven.artifact.Artifact;
// import org.apache.maven.plugin.MojoExecutionException;
// import org.apache.maven.plugin.MojoFailureException;
// import org.apache.maven.plugin.testing.ArtifactStubFactory;
// import org.apache.maven.project.MavenProject;
// import org.jmock.Expectations;
// import org.jmock.Mockery;
// import org.jmock.integration.junit4.JMock;
// import org.jmock.lib.legacy.ClassImposteriser;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.sonatype.plexus.build.incremental.DefaultBuildContext;
//
// @RunWith(JMock.class)
// public class CopyNativesMojoTest {
//
// Mockery context = new Mockery() {
//
// {
// setImposteriser(ClassImposteriser.INSTANCE);
// }
// };
// private CopyNativesMojo mojo;
// private IArtifactHandler jarUnpacker;
// private MavenProject mavenProject;
// private File nativesTargetDir;
// private ArtifactStubFactory artifactFactory;
//
// @Before
// public void setUp() {
// mojo = new CopyNativesMojo();
// mavenProject = context.mock(MavenProject.class);
// mojo.setMavenProject(mavenProject);
// jarUnpacker = context.mock(IArtifactHandler.class);
// mojo.setArtifactHandler(jarUnpacker);
// nativesTargetDir = context.mock(File.class);
// mojo.setNativesTargetDir(nativesTargetDir);
// artifactFactory = new ArtifactStubFactory();
// mojo.setBuildContext(new DefaultBuildContext());
// }
//
// @Test
// public void execute_Without_Dependencies() throws MojoExecutionException, MojoFailureException {
// final Set<Artifact> artifacts = new HashSet<Artifact>();
//
// context.checking(new Expectations() {
//
// {
// oneOf(mavenProject).getArtifacts();
// will(returnValue(artifacts));
// }
// });
//
// mojo.execute();
// }
//
// @Test
// public void execute_Without_Native_Dependencies() throws MojoExecutionException, MojoFailureException, IOException {
// final Set<Artifact> artifacts = new HashSet<Artifact>();
//
// artifacts.add(artifactFactory.createArtifact("groupid1", "artifactid1", "1.0"));
// artifacts.add(artifactFactory.createArtifact("groupid2", "artifactid2", "2.0"));
// artifacts.add(artifactFactory.createArtifact("groupid3", "artifactid3", "3.0"));
//
// context.checking(new Expectations() {
//
// {
// oneOf(mavenProject).getArtifacts();
// will(returnValue(artifacts));
// }
// });
//
// mojo.execute();
// }
//
// @Test
// public void execute_With_One_Native_Dependency_Calls_The_Unpacker() throws MojoExecutionException, MojoFailureException, IOException {
// final Set<Artifact> artifacts = new HashSet<Artifact>();
//
// artifacts.add(artifactFactory.createArtifact("groupid1", "artifactid1", "1.0"));
// final Artifact nativeArtifact = artifactFactory.createArtifact("groupid2", "artifactid2", "2.0", "compile", "jar", CopyNativesMojo.NATIVES_PREFIX + "windows");
// final File nativeFile = new File("test1");
// nativeArtifact.setFile(nativeFile);
//
// artifacts.add(nativeArtifact);
// artifacts.add(artifactFactory.createArtifact("groupid3", "artifactid3", "3.0"));
//
// context.checking(new Expectations() {
//
// {
// oneOf(nativesTargetDir).mkdirs();
// oneOf(mavenProject).getArtifacts();
// will(returnValue(artifacts));
// oneOf(jarUnpacker).moveOrUnpackTo(nativesTargetDir, nativeArtifact);
// }
// });
//
// mojo.execute();
// }
//
// }
