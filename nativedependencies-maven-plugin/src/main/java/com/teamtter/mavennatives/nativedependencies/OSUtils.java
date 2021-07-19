package com.teamtter.mavennatives.nativedependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OSUtils {
	public static final List<String> WINDOWS_LIBS_EXTENSIONS = Arrays.asList("dll");

	public static final List<String> LINUX_LIBS_EXTENSIONS = Arrays.asList("so");

	public static final List<String> MAC_LIBS_EXTENSIONS = Arrays.asList("jnilib", "dylib");

	// double-brace initialization trick
	public static final List<String> ALL_OS_EXTENSIONS = new ArrayList<>() {
		{
			addAll(WINDOWS_LIBS_EXTENSIONS);
			addAll(LINUX_LIBS_EXTENSIONS);
			addAll(MAC_LIBS_EXTENSIONS);
		}
	};
}
