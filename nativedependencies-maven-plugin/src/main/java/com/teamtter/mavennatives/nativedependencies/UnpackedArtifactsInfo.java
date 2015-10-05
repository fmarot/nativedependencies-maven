package com.teamtter.mavennatives.nativedependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class UnpackedArtifactsInfo {

	private Map<String, Long> pathToLastModified = new HashMap<>();

	public void flagAsUnpacked(File file) {
		Path normalizedPath = file.toPath().normalize();
		String pathAsString = normalizedPath.toString();
		long lastModified = file.lastModified();
		pathToLastModified.put(pathAsString, lastModified);
	}

	public boolean containsExactly(File currentArtifactFile) {
		boolean contains = false;
		String normalizedPath = currentArtifactFile.toPath().normalize().toString();
		Long lastModifiedDate = pathToLastModified.get(normalizedPath);
		if (lastModifiedDate != null) {
			long currentArtifactLastModifiedDate = currentArtifactFile.lastModified();
			if (currentArtifactLastModifiedDate == lastModifiedDate) {
				contains = true;
			}
		}
		return contains;
	}
}
