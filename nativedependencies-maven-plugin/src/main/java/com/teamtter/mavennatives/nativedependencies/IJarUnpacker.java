package com.teamtter.mavennatives.nativedependencies;

import java.io.File;
import java.io.IOException;

public interface IJarUnpacker {

	public void copyJarContent(File jarPath, File targetDir) throws IOException;
}
