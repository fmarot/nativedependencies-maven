package com.teamtter.mavennatives.nativedependencies;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.annotations.Component;

import lombok.extern.slf4j.Slf4j;

/** Note: in a multi-module Maven project, this @Component is a singleton: different instances of mojo
 * are being injected with the SAME instance of {@link ArtifactHandler} */
@Component(role = IArtifactHandler.class)
@Slf4j // Starting with Maven 3.1.0, SLF4J Logger can be used directly too, without Plexus
public class ArtifactHandler implements IArtifactHandler {

	private static final RaceConditionPreventer raceConditionPreventer = new RaceConditionPreventer();

	private static List<String> zipLikeExtensions = Arrays.asList("jar", "zip", "gz");

	private static List<String> tarGzExensions = Arrays.asList("tar.gz", "tgz");

	private static List<String> _7zExtensions = Arrays.asList("7z", "7zip");

	/**
	 * Wraps any Exception encountered into an ArtifactUnpackingException which is a
	 * RUNTIME Exception
	 */
	@Override
	public void moveOrUnpackArtifactTo(File unpackingDir, Artifact artifact) {
		File artifactFile = artifact.getFile();

		// we do not want to unzip the same artifact in the same directory multiple times at once (in the
		// case of multi-module Maven projects) nor do we want one execution to try to delete the uncompressed Tar
		// file while another may be extracting it so we try to protect the execution and be thread safe.
		raceConditionPreventer.preventRaceCondition(unpackingDir,
				artifactFile,
				() -> moveOrUnpackFileTo(unpackingDir, artifactFile));
	}

	public static void moveOrUnpackFileTo(File unpackingDir, File artifactFile) {
		String fileName = artifactFile.getName();
		String extension = FilenameUtils.getExtension(fileName);

		try {
			if (fileName.contains(".tar.") || tarGzExensions.contains(extension)) {
				log.info("Artifact {} will be uncompressed as tar-gz-like to {}", artifactFile, unpackingDir);
				String basename = FilenameUtils.getBaseName(artifactFile.getName());
				File uncompressedTarFile = new File(unpackingDir, basename);
				uncompressAFile(artifactFile, unpackingDir);
				uncompressAnArchive(uncompressedTarFile, unpackingDir);
				uncompressedTarFile.delete();
			} else if (zipLikeExtensions.contains(extension)) {
				log.info("Artifact {} will be uncompressed as zip-like to {}", artifactFile, unpackingDir);
				uncompressAnArchive(artifactFile, unpackingDir);
			} else if (_7zExtensions.contains(extension)) {
				log.info("Artifact {} will be uncompressed as 7z to {}", artifactFile, unpackingDir);
				uncompress7zArchive(artifactFile, unpackingDir);
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

	private static void uncompress7zArchive(File artifactFile, File unpackingDir) {
		try {
			SevenZFile sevenZFile = new SevenZFile(artifactFile);
			SevenZArchiveEntry entry = sevenZFile.getNextEntry();
			
			while (entry != null) {
				File entryFile = new File(unpackingDir, entry.getName());
				if (entry.isDirectory()) {
					entryFile.mkdirs();
				} else {
					FileOutputStream out = new FileOutputStream(entryFile);
					byte[] content = new byte[(int) entry.getSize()];
					sevenZFile.read(content, 0, content.length);
					out.write(content);
					out.close();
				}
				entry = sevenZFile.getNextEntry();
			}
			sevenZFile.close();
		} catch (Exception e) {
			log.error("Unable to fully uncompress {} to {}", artifactFile, unpackingDir);
			throw new ArtifactUnpackingException(e);
		}
	}

	private static void uncompressAnArchive(File fileIn, File dirOut) {
		try {
			FileInputStream fin = new FileInputStream(fileIn);
			BufferedInputStream bis = new BufferedInputStream(fin);

			try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(bis)) {

				ArchiveEntry entry = null;

				List<TarArchiveEntry> tarEntriesSymlinks = new ArrayList<>();
				while ((entry = ais.getNextEntry()) != null) {
					if (entry.isDirectory()) {
						// nothing, will be created if it contains files
					} else {
						File outFile = new File(dirOut, entry.getName());
						outFile.getParentFile().mkdirs();

						boolean isSpecialCase = false;
						if (entry instanceof TarArchiveEntry) {
							TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
							if (tarEntry.isSymbolicLink()) {
								isSpecialCase = true;
								log.debug("File {} is a symlink", outFile);
								tarEntriesSymlinks.add(tarEntry);
							}
						}

						if (!isSpecialCase) { // special cases are already handled
							log.debug("File {} is not special", outFile);
							try (OutputStream out = new FileOutputStream(outFile)) {
								IOUtils.copy(ais, out);
							}
						}
					}
				}

				// Treat symlinks
				for (TarArchiveEntry tarEntry : tarEntriesSymlinks) {
					File outFile = new File(dirOut, tarEntry.getName());
					Path linkTarget = new File(outFile.getParent(), tarEntry.getLinkName()).toPath();
					try {
						Files.createSymbolicLink(outFile.toPath(), linkTarget);
					} catch (Exception e) {
						log.warn("Unable to create symlink {} -> {} (maybe OS/filesystem does not support it ?)",
								outFile.toPath(), linkTarget);
					}
				}

			}
		} catch (Exception e) {
			log.error("Unable to fully uncompress {} to {}", fileIn, dirOut);
			throw new ArtifactUnpackingException(e);
		}
	}

	private static void uncompressAFile(File fileIn, File dirOut) {
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

	public static void main(String[] args) {
		String destDir = "/media/vg1-data/Downloads/mvntest/out/";
		String sourceFile = "/media/vg1-data/Downloads/mvntest/archive.tar.gz";
		moveOrUnpackFileTo(new File(destDir), new File(sourceFile));
	}
}
