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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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
		, threadSafe = true /** the RaceConditionPreventer should prevent many problems. All problems ??? => To be verified */
, defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES, requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true)
@Slf4j
public class CopyNativesMojo extends AbstractMojo {

	private static final String ALREADY_UNPACKED_ARTIFACTS_INFO_FILE = "alreadyUnpackedArtifactsInfo.json";

	public static final String NATIVES_PREFIX = "natives-";

	@Parameter(defaultValue = "${project}", readonly = true)
	@Setter
	private MavenProject mavenProject;

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
	
	/**
	 * If set to true, this parameter will override any value set for {@link #nativesTargetDir}.
	 * In this case (true), the plugin will try to find the directory of the upper parent pom ('upperPomDir')
	 * by going up in the filesystem.
	 * Once this 'upperPomDir' is found, then the $upperPomDir/target/natives directory will be used
	 * instead of whatever nativesTargetDir was previously set.
	 * This should simplify a LOT the builds in multi-module projects to unpack everything
	 * in the same place, whether you are executing mvn from the $nativesTargetDir or directly inside
	 * a child module.
	 */
	@Parameter(property = "autoDetectDirUpInFilesystem", defaultValue = "false")
	@Setter
	private boolean autoDetectDirUpInFilesystem;

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
		if (skip) {
			log.info("Skipping execution due to 'skip' == true");
		} else {
			initOsFiltersIfNeeded();
			
			overrideNativesTargetDirIfNeeded();
			mavenProject.getProperties().put("nativesTargetDir", nativesTargetDir.toString());
			log.info("Natives will be saved in {} " + (separateDirs ? " and separated dirs according to classifier" : ""), nativesTargetDir );
		
			copyNativeDependencies();
		}
	}

	private void overrideNativesTargetDirIfNeeded() {
		if (autoDetectDirUpInFilesystem) {
			nativesTargetDir = new File(lookupUpperParentPom().getParentFile(), "/target/natives/");
			log.debug("nativesTargetDir overriden with {}", nativesTargetDir);
		}
	}

	private File lookupUpperParentPom() {
		File currentPom = new File(mavenProject.getBasedir(), "pom.xml");
		try {
	        boolean upperPomFound = false;
	        while (!upperPomFound) {
				MavenXpp3Reader currentReader = new MavenXpp3Reader();
	        	Model currentModel = currentReader.read(new FileReader(currentPom));
	        	Parent parent = currentModel.getParent();
	        	if (parent == null) {
	        		log.trace("current pom has no parent and is THE upper pom: {}", currentPom);
	        		upperPomFound = true;	// upper Pom is 'currentPom'
	        	} else {
					String parentGroupId = parent.getGroupId();
		            String parentArtifactId = parent.getArtifactId();
		        	File possibleParentPom = new File(currentPom.getParentFile().getParentFile(), "pom.xml");
			        if (possibleParentPom.exists()) {
						MavenXpp3Reader possibleParentReader = new MavenXpp3Reader();
				        Model possibleParentModel = possibleParentReader.read(new FileReader(possibleParentPom));
				        String possibleParentGroupId = possibleParentModel.getGroupId();
				        String possibleParentArtifactId = possibleParentModel.getArtifactId();
				        
				        boolean artifactIdOK = parentArtifactId.equals(possibleParentArtifactId);
				        boolean groupIdOK = possibleParentGroupId == null || parentGroupId.equals(possibleParentGroupId);	// may be null because inherited from parent pom
				        
						if (artifactIdOK && groupIdOK ) {
				        	currentPom = possibleParentPom;
				        } else {
				        	log.info("{} is not parent of {}", possibleParentPom, currentPom);
				        	upperPomFound = true;	// upper Pom is 'currentPom'
				        }
			        } else {
			        	upperPomFound = true;	// upper Pom is 'currentPom'
			        }
	        	}
	        }
		} catch (Exception e) {
			log.warn("Exception looking for parent pom.", e);
		}
        log.debug("found upper pom: {} ", currentPom);
		
		return currentPom;
	}

	private void initOsFiltersIfNeeded() {
		if (autoDetectOSNatives) {
			if (osFilters.size() != 0) {
				log.warn("Some OS filters have been set but will be overriden by auto-OS-detection. Please define filters OR set autoDetectOSNatives = true");
			}
			OsFilter thisComputer = new OsFilter(OsFilter.OS, null, getbasicOsTrigramm(OsFilter.OS));
			osFilters.add(thisComputer);
			log.debug("autoDetectOSNatives = true");
		} else if (osFilters.size() == 0) {
			log.debug("AcceptEverythingOsFilter will be used");
			osFilters.add(new AcceptEverythingOsFilter()); // we will handle ALL native deps
		} else {
			log.info("{} OS filters have been defined", osFilters.size());
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
			Set<Artifact> artifacts = mavenProject.getArtifacts(); // warning: depending on the phase, come may be missing, see MavenProject javadoc
			for (Artifact artifact : artifacts) {
				String classifier = artifact.getClassifier();
				if (artifactAlreadyUnpacked(unpackedArtifactsInfo, artifact)) {
					log.debug("{} is already unpacked", artifactToString(artifact));
				} else if (classifierMatchesConfig(classifier)) {
					log.debug("{} => ok", artifactToString(artifact));
					int i = 0;
					handleDependancyCopyingOrUnpacking(artifact, classifier, unpackedArtifactsInfo);
					atLeastOneartifactCopied = true;
					i++;
					
				} else {
					log.debug("{} => ko, native will be filtered out", artifactToString(artifact));
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

	/** Warning: this method prevents unpacking artifacts resulting from a previous execution of Maven
	 * or another module in the same execution if sequential, but when Maven parralelism is used (-T 1C for example)
	 * then another layer of protection is used: the RaceConditionPreventer.
	 * This is because 2 modules could read the same version of the json file missing an artifact and both
	 * try to unzip it. */
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
		log.debug("prefixMatches={}", prefixMatches);
		String suffix = classifier.replace(NATIVES_PREFIX, "");
		log.debug("suffix = {}", suffix);
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
