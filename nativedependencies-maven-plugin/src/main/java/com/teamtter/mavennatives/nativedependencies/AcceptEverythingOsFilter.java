package com.teamtter.mavennatives.nativedependencies;

import org.apache.maven.plugin.logging.Log;

public class AcceptEverythingOsFilter extends OsFilter {

	@Override
	public boolean accepts(String suffix, Log log) {
		log.debug("AcceptEverythingOsFilter accepts '" + suffix + "'");
		return true;
	}
}
