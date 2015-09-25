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
import java.util.ArrayList;
import java.util.List;
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
import lombok.extern.slf4j.Slf4j;

/** Unpacks native dependencies */
@Mojo(name = "copy" /** the goal */
, threadSafe = false /** until proven otherwise, false */
, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true)
@Slf4j
public class CopyNativesMojo extends AbstractMojo {

	public static final String NATIVES_PREFIX = "natives-";

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

	// @formatter:off
	@Parameter
	@Setter
	private List<OsFilter> osFilters = new ArrayList() {{
		add(new AcceptEverythingOsFilter()); // unless configured otherwise, we will handle ALL native deps (no filter)
	}};
	// @formatter:on

	@Component
	@Setter
	private IArtifactHandler artifactHandler;

	@Component
	@Setter
	private BuildContext buildContext;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			log.info("Skipping execution due to 'skip' == true");
		} else {
			copyNativeDependencies();
		}
	}

	private void copyNativeDependencies() throws MojoFailureException {
		try {
			log.info("Saving natives in " + nativesTargetDir + (separateDirs ? "separated dirs according to classifier" : ""));

			Set<Artifact> artifacts = mavenProject.getArtifacts();
			boolean atLeastOneartifactCopied = false;
			for (Artifact artifact : artifacts) {
				log.info("Testing: " + artifactToString(artifact));
				String classifier = artifact.getClassifier();
				if (classifierMatchesConfig(classifier)) {
					handleDependancyCopyingOrUnpacking(artifact, classifier);
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

	/** Copy the native dep into 'unpackingDir' or unzip, depending on the file type */
	private void handleDependancyCopyingOrUnpacking(Artifact artifact, String classifier) throws IOException {
		log.info("Will unpack: " + artifactToString(artifact));
		File unpackingDir = computeUnpackingDir(classifier);
		artifactHandler.moveOrUnpackTo(unpackingDir, artifact);
	}

	private String artifactToString(Artifact artifact) {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String classifier = artifact.getClassifier();
		return String.format("G:%s - A:%s - C:%s", groupId, artifactId, classifier);
	}

	private boolean classifierMatchesConfig(String classifier) {
		if (classifier == null) {
			return false;
		}

		boolean prefixMatches = classifier != null && classifier.startsWith(NATIVES_PREFIX);
		String suffix = classifier.replace(NATIVES_PREFIX, "");
		boolean suffixMatchesCurrentOs = false;
		for (OsFilter filter : osFilters) {
			// if at least one filter matches the current os/arch then handle this artifact
			if (filter.accepts(suffix)) {
				suffixMatchesCurrentOs = true;
				break;
			}
		}
		boolean matches = prefixMatches && suffixMatchesCurrentOs;
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
