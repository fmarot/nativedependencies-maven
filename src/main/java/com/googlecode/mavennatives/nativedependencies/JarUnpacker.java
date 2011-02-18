package com.googlecode.mavennatives.nativedependencies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = IJarUnpacker.class)
public class JarUnpacker implements IJarUnpacker
{
	private Log log = new SystemStreamLog();

	public void copyJarContent(File jarPath, File targetDir) throws IOException
	{
		log.info("Copying natives from " + jarPath.getName());
		JarFile jar = new JarFile(jarPath);

		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements())
		{
			JarEntry file = entries.nextElement();

			File f = new File(targetDir, file.getName());

			log.info("Copying native - " + file.getName());
			
			File parentFile = f.getParentFile();
			parentFile.mkdirs();
			
			if (file.isDirectory())
			{ // if its a directory, create it
				f.mkdir();
				continue;
			}

			InputStream is = null;
			FileOutputStream fos = null;

			try
			{
				is = jar.getInputStream(file); // get the input stream
				fos = new FileOutputStream(f);
				IOUtils.copy(is, fos);
			}
			finally
			{
				if (fos != null)
					fos.close();
				if (is != null)
					is.close();
			}

		}

	}

}
