package com.teamtter.mavennatives.nativedependencies;

import lombok.Data;

@Data
public class OsFilter {
	String osName;
	String osArch;
	String suffix;
	
	static final String OS = System.getProperty("os.name");
	
	// TODO: get inspiration from https://github.com/trustin/os-maven-plugin
	
	public boolean accepts(String suffix) {
		OsMatches
		// TODO use org.apache.commons.lang - Class StringUtils
		return false;
	}

}
