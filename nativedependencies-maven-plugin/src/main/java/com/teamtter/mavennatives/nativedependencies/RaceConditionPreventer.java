package com.teamtter.mavennatives.nativedependencies;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/** this class helps to prevent race conditions on some files to be extracted*/
@Slf4j
public class RaceConditionPreventer {

	/** List of artifacts unpacked during this Maven execution */
	private ConcurrentHashMap<FileAndDir, MyLock> unpackedAtRuntime = new ConcurrentHashMap<>();

	@Value
	static class FileAndDir {

		public FileAndDir(File artifactFile, File unpackingDir) {
			try {
				this.artifactFile = artifactFile.getCanonicalFile();
				this.unpackingDir = unpackingDir.getCanonicalFile();
			} catch (Exception e) {
				throw new RuntimeException("unable to init FileAndDir " + artifactFile + " - " + unpackingDir);
			}
		}

		private File unpackingDir;
		private File artifactFile;
	}

	class MyLock extends Object {
	}

	public void preventRaceCondition(File unpackingDir, File artifactFile, Runnable runnable) {
		FileAndDir unpackInfo = new FileAndDir(artifactFile, unpackingDir);

		MyLock myLock = new MyLock();
		unpackedAtRuntime.putIfAbsent(unpackInfo, myLock);
		MyLock storedLock = unpackedAtRuntime.get(unpackInfo);
		synchronized (storedLock) {
			if (myLock.equals(storedLock)) {
				// nobody has already unpacked this artifact => unpack it !
				runnable.run();
			} else {
				log.info("{} has already been unpacked in {} during this Maven execution => will not do it again", artifactFile, unpackingDir);
			}
		}
	}

}
