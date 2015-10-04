package com.teamtter.mavennatives.nativedependencies;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.annotations.Component;

import lombok.extern.slf4j.Slf4j;

@Component(role = IArtifactHandler.class)
@Slf4j // Starting with Maven 3.1.0, SLF4J Logger can be used directly too, without Plexus
public class ArtifactHandler implements IArtifactHandler {

	private static List<String> zipLikeExtensions = Arrays.asList("jar", "zip", "gz", ".7z", ".7zip");

	private static List<String> tarGzExensions = Arrays.asList("tar.gz", "tgz");

	/** Wraps any Exception encountered into an ArtifactUnpackingException which is a RUNTIME Exception */
	@Override
	public void moveOrUnpackTo(File unpackingDir, Artifact artifact) {
		File artifactFile = artifact.getFile();
		String fileName = artifactFile.getName();
		String extension = FilenameUtils.getExtension(fileName);

		try {
			if (fileName.contains(".tar.") || tarGzExensions.contains(extension)) {
				log.info("Artifact {} will be uncompressed as tar-gz-like to {}", artifactFile, unpackingDir);
				String basename = FilenameUtils.getBaseName(artifactFile.getName());
				File uncompressedTarFile = new File(unpackingDir, basename);
				uncompressAFile(artifactFile, uncompressedTarFile);
				uncompressAnArchive(uncompressedTarFile, unpackingDir);
				uncompressedTarFile.delete();
			} else if (zipLikeExtensions.contains(extension)) {
				log.info("Artifact {} will be uncompressed as zip-like to {}", artifactFile, unpackingDir);
				uncompressAnArchive(artifactFile, unpackingDir);
			} else {
				log.info("Artifact {} can not be unpacked, will be moved as is to {}", artifactFile, unpackingDir);
				File targetFile = new File(unpackingDir, fileName);
				FileUtils.copyFile(artifactFile, targetFile);
			}
		} catch (Exception e) {
			log.error("Error unpacking or moving artifact {}", artifactFile);
			throw new ArtifactUnpackingException(e);
		}
	}

	public static void uncompressAnArchive(File fileIn, File dirOut) {
		try {
			FileInputStream fin = new FileInputStream(fileIn);
			BufferedInputStream bis = new BufferedInputStream(fin);

			try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(bis)) {
				ArchiveEntry entry = null;

				while ((entry = ais.getNextEntry()) != null) {
					if (entry.isDirectory()) {
						// nothing, will be created if it contains files
					} else {
						File outFile = new File(dirOut, entry.getName());
						outFile.getParentFile().mkdirs();
						try (OutputStream out = new FileOutputStream(outFile)) {
							IOUtils.copy(ais, out);
						}
					}
				}
			}
		} catch (IOException | ArchiveException e) {
			log.error("Unable to fully uncompress {} to {}", fileIn, dirOut);
			throw new ArtifactUnpackingException(e);
		}
	}

	public static void uncompressAFile(File fileIn, File dirOut) {
		dirOut.mkdirs();
		try {
			FileInputStream fin = new FileInputStream(fileIn);
			BufferedInputStream bis = new BufferedInputStream(fin);
			try (CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(bis)) {
				String targetFileName = FilenameUtils.getBaseName(fileIn.getName());
				File outFile = new File(dirOut, targetFileName);
				try (OutputStream out = new FileOutputStream(outFile)) {
					IOUtils.copy(cis, out);
				}
			}
		} catch (IOException | CompressorException e) {
			log.error("Unable to fully uncompress {} to {}", fileIn, dirOut);
			throw new ArtifactUnpackingException(e);
		}
	}

}
