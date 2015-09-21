package com.teamtter.mavennatives.nativedependencies;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import lombok.Setter;

/** Unpacks native dependencies */
@Mojo(name = "copy" /** the goal */
, threadSafe = false /** until proven otherwise, false */
, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true)
public class CopyNativesMojo extends AbstractMojo {

	private static final String NATIVES_PREFIX = "natives-";

	@Parameter(defaultValue = "${project}", readonly = true)
	@Setter
	private MavenProject mavenProject;

	@Parameter(property = "nativesTargetDir", defaultValue = "${project.build.directory}/natives")
	@Setter
	private File nativesTargetDir;

	@Parameter(property = "separateDirs", defaultValue = "false")
	@Setter
	private boolean separateDirs;

	@Parameter(property = "autoDetectOSNatives", defaultValue = "false")
	@Setter
	private boolean autoDetectOSNatives;

	@Parameter(property = "skip", defaultValue = "false")
	@Setter
	private boolean skip;

	@Component
	@Setter
	private IJarUnpacker jarUnpacker;

	@Component
	@Setter
	private BuildContext buildContext;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skipping execution due to 'skip' == true");
		} else {
			copyNativeDependencies();
		}
	}

	private void copyNativeDependencies() throws MojoFailureException {
		try {
			getLog().info("Saving natives in " + nativesTargetDir + (separateDirs ? "separated dirs according to classifier" : ""));

			Set<Artifact> artifacts = mavenProject.getArtifacts();
			boolean atLeastOneartifactCopied = false;
			for (Artifact artifact : artifacts) {
				String classifier = artifact.getClassifier();
				if (classifierMatchesConfig(classifier)) {
					unpackArtifact(artifact, classifier);
					atLeastOneartifactCopied = true;
				}
			}
			
			if (atLeastOneartifactCopied) {
				buildContext.refresh(nativesTargetDir);
			}
		} catch (Exception e) {
			throw new MojoFailureException("Unable to copy natives", e);
		}
	}

	private void unpackArtifact(Artifact artifact, String classifier) throws IOException {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		getLog().info(String.format("G:%s - A:%s - C:%s", groupId, artifactId, classifier));
		File unpackingDir = computeUnpackingDir(classifier);
		jarUnpacker.copyJarContent(artifact.getFile(), unpackingDir);
	}

	private boolean classifierMatchesConfig(String classifier) {
		boolean matches = classifier != null && classifier.startsWith(NATIVES_PREFIX);
		// TODO: add test for autoDetectOSNatives
		return matches;
	}

	private File computeUnpackingDir(String classifier) {
		File artifactDir;
		if (separateDirs) {
			String suffix = classifier.substring(NATIVES_PREFIX.length());
			artifactDir = new File(nativesTargetDir, suffix);
		} else {
			artifactDir = nativesTargetDir;
		}
		artifactDir.mkdirs();
		return artifactDir;
	}

}
