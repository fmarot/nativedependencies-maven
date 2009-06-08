package com.googlecode.mavennatives.nativedependencies;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Echos an object string to the output screen.
 * 
 * @goal copy
 * @requiresProject true
 * @requiresDependencyResolution
 */
public class CopyNativesMojo extends AbstractMojo
{
	/**
	 * POM
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 * @required
	 */
	private MavenProject project;

	/**
	 * 
	 * @parameter expression="${nativesTargetDir}" default-value="${basedir}/target/natives"
	 */
	private File nativesTargetDir;

	public void execute() throws MojoExecutionException, MojoFailureException
	{
		try
		{
			getLog().info("Saving natives in " + nativesTargetDir);
			Set<Artifact> artifacts = project.getArtifacts();
			nativesTargetDir.mkdirs();
			for (Artifact artifact : artifacts)
			{
				String classifier = artifact.getClassifier();
				if (classifier != null && classifier.startsWith("natives-"))
				{
					getLog().info(String.format("G:%s - A:%s - C:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier()));
					copyJarContent(artifact.getFile(), nativesTargetDir);
				}

			}
		}
		catch (Exception e)
		{
			throw new MojoFailureException("Unable to copy natives",e);
		}
	}

	private void copyJarContent(File jarPath, File targetDir) throws IOException
	{
		getLog().info("Copying natives from " + jarPath.getName());
		JarFile jar = new JarFile(jarPath);

		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements())
		{
			JarEntry file = entries.nextElement();
			getLog().info("Copying native - " + file.getName());
			File f = new File(targetDir, file.getName());
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
				while (is.available() > 0)
				{ // write contents of 'is' to 'fos'
					fos.write(is.read());
				}
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
