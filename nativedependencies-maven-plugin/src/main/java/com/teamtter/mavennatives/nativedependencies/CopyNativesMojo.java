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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/** Unpacks native dependencies */
@Mojo(name = "copy" /** the goal */
, threadSafe = false /** until proven otherwise, false */
, defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true)
@Slf4j
public class CopyNativesMojo extends AbstractMojo {

	private static final String ALREADY_UNPACKED_ARTIFACTS_INFO_FILE = "alreadyUnpackedArtifactsInfo.json";

	public static final String NATIVES_PREFIX = "natives-";

	@Parameter(defaultValue = "${project}", readonly = true)
	@Setter
	private MavenProject mavenProject;

	// @Parameter(property = "nativesTargetDir", defaultValue = "${project.build.directory}/natives")
	/**
	 * by default, in case of a multi module project, we will unpack ALL NATIVES to
	 * the same dir, thus saving space and unzip time while allowing all interdependent
	 * projects to benefit from the presence of native libs
	 */
	@Parameter(property = "nativesTargetDir", defaultValue = "${session.executionRootDirectory}/target/natives")
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

	@Parameter
	@Setter
	private List<OsFilter> osFilters = new ArrayList<>();

	@Component
	@Setter
	private IArtifactHandler artifactHandler;

	@Component
	@Setter
	private BuildContext buildContext;

	private static ObjectMapper jsonMapper;

	static {
		jsonMapper = new ObjectMapper();
		jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
		jsonMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		initOsFiltersIfNeeded();
		if (skip) {
			log.info("Skipping execution due to 'skip' == true");
		} else {
			copyNativeDependencies();
		}
	}

	private void initOsFiltersIfNeeded() {
		if (autoDetectOSNatives) {
			if (osFilters.size() != 0) {
				log.warn("Some OS filters have been set but will be overriden by auto-OS-detection. Please define filters OR set autoDetectOSNatives = true");
			}
			OsFilter thisComputer = new OsFilter(OsFilter.OS, null, getbasicOsTrigramm(OsFilter.OS));
			osFilters.add(thisComputer);
		} else if (osFilters.size() == 0) {
			osFilters.add(new AcceptEverythingOsFilter()); // we will handle ALL native deps
		} else {
			log.debug("{} OS filters have been defined", osFilters.size());
		}
	}

	private String getbasicOsTrigramm(String os) {
		if (os.contains("win")) {
			return "win";
		} else if (os.contains("lin")) {
			return "lin";
		} else if (os.contains("mac")) {
			return "mac";
		}
		log.warn("unable to auto-detect OS...");
		return "";
	}

	private void copyNativeDependencies() throws MojoFailureException {
		boolean atLeastOneartifactCopied = false;
		UnpackedArtifactsInfo unpackedArtifactsInfo = loadAlreadyUnpackedArtifactsInfo();
		try {
			log.info("Saving natives in " + nativesTargetDir + (separateDirs ? "separated dirs according to classifier" : ""));

			Set<Artifact> artifacts = mavenProject.getArtifacts(); // warning: depending on the phase, come may be missing, see MavenProject javadoc
			for (Artifact artifact : artifacts) {
				String classifier = artifact.getClassifier();
				if (classifierMatchesConfig(classifier) && !artifactAlreadyUnpacked(unpackedArtifactsInfo, artifact)) {
					log.info("{} => ok", artifactToString(artifact));
					handleDependancyCopyingOrUnpacking(artifact, classifier, unpackedArtifactsInfo);
					atLeastOneartifactCopied = true;
				} else {
					log.info("{} => ko, native will be filtered out", artifactToString(artifact));
				}
			}

		} catch (Exception e) {
			if (atLeastOneartifactCopied) {
				buildContext.refresh(nativesTargetDir);
			}
			throw new MojoFailureException("Unable to copy natives", e);
		} finally {
			if (atLeastOneartifactCopied) {
				writeAlreadyUnpackedArtifactsInfo(unpackedArtifactsInfo);
			}
		}
	}

	private UnpackedArtifactsInfo loadAlreadyUnpackedArtifactsInfo() {
		File file = new File(nativesTargetDir, ALREADY_UNPACKED_ARTIFACTS_INFO_FILE);
		UnpackedArtifactsInfo info = new UnpackedArtifactsInfo();
		if (file.exists()) {
			try {
				info = jsonMapper.readValue(file, UnpackedArtifactsInfo.class);
			} catch (IOException e) {
				log.error("Unable to read file {}", file, e);
				// just log, only problem may be slower execution because of no reusing existing file
			}
		}
		return info;
	}

	private void writeAlreadyUnpackedArtifactsInfo(UnpackedArtifactsInfo unpackedArtifactsInfo) {
		File file = new File(nativesTargetDir, ALREADY_UNPACKED_ARTIFACTS_INFO_FILE);
		try {
			jsonMapper.writeValue(file, unpackedArtifactsInfo);
		} catch (IOException e) {
			log.error("Unable to write to file {}", file, e);
			// just log, only problem may be slower execution because of no reusing existing file
		}
	}

	/** Copy the native dep into 'unpackingDir' or unzip, depending on the file type */
	private void handleDependancyCopyingOrUnpacking(Artifact artifact, String classifier, UnpackedArtifactsInfo unpackedArtifactsInfo) {
		log.info("Will unpack: " + artifactToString(artifact));
		File unpackingDir = computeUnpackingDir(classifier);
		artifactHandler.moveOrUnpackArtifactTo(unpackingDir, artifact);
		unpackedArtifactsInfo.flagAsUnpacked(artifact.getFile());
	}

	private boolean artifactAlreadyUnpacked(UnpackedArtifactsInfo unpackedArtifactsInfo, Artifact artifact) {
		File currentArtifactFile = artifact.getFile();
		boolean contains = false;
		if (unpackedArtifactsInfo.containsExactly(currentArtifactFile)) {
			contains = true;
			log.debug("Artifact {} already unpacked", artifact);
		}
		return contains;
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
		log.info("prefixMatches={}", prefixMatches);
		String suffix = classifier.replace(NATIVES_PREFIX, "");
		log.info("suffix = {}", suffix);
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
