package com.teamtter.mavennatives.nativedependencies;

import java.io.File;

import org.apache.maven.artifact.Artifact;

public interface IArtifactHandler {

	public void moveOrUnpackTo(File unpackingDir, Artifact artifact);

}
