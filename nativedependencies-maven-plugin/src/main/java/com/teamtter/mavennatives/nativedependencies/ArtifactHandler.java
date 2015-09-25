package com.teamtter.mavennatives.nativedependencies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.annotations.Component;

import lombok.extern.slf4j.Slf4j;

@Component(role = IArtifactHandler.class)
@Slf4j // Starting with Maven 3.1.0, SLF4J Logger can be used directly too, without Plexus
public class ArtifactHandler implements IArtifactHandler {

	private static List<String> jarExtensions = Arrays.asList("jar", "zip");

	private static List<String> tarExensions = Arrays.asList("tar", "tgz", "gz");

	@Override
	public void moveOrUnpackTo(File unpackingDir, Artifact artifact) {
		File artifactFile = artifact.getFile();
		String fileName = artifactFile.getName();
		String extension = FilenameUtils.getExtension(fileName);

		try {
			if (jarExtensions.contains(extension)) {
				unpackJarOrZip(artifactFile, unpackingDir);
			} else if (tarExensions.contains(extension)) {
				throw new UnsupportedOperationException("extension not handled yet");
			} else {
				log.debug("Artifact {} can not be unpacked, will be moved as is to {}", fileName, unpackingDir);
				File targetFile = new File(unpackingDir, fileName);
				FileUtils.copyFile(artifactFile, targetFile);
			}
		} catch (Exception e) {
			log.error("Error unpacking or moving artifact {}", artifactFile);
		}
	}

	private void unpackJarOrZip(File jarPath, File targetDir) throws IOException {
		log.info("unpackJarOrZip natives from " + jarPath.getName());
		JarFile jar = new JarFile(jarPath);

		for (JarEntry jarEntry : Collections.list(jar.entries())) {
			File f = new File(targetDir, jarEntry.getName());
			log.info("Copying native - " + jarEntry.getName());
			File parentFile = f.getParentFile();
			parentFile.mkdirs();

			if (jarEntry.isDirectory()) {
				f.mkdir();
			} else {
				try (InputStream is = jar.getInputStream(jarEntry); FileOutputStream fos = new FileOutputStream(f)) {
					IOUtils.copy(is, fos);
				}
			}
		}
	}
}
