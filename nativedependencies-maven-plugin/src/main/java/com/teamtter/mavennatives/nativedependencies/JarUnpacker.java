package com.teamtter.mavennatives.nativedependencies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;

import lombok.extern.slf4j.Slf4j;

@Component(role = IJarUnpacker.class)
@Slf4j	// Starting with Maven 3.1.0, SLF4J Logger can be used directly too, without Plexus
public class JarUnpacker implements IJarUnpacker {

	@Override
	public void copyJarContent(File jarPath, File targetDir) throws IOException {
		log.info("Copying natives from " + jarPath.getName());
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
