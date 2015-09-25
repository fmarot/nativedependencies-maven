package com.teamtter.mavennatives.nativedependencies;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AcceptEverythingOsFilter extends OsFilter {

	@Override
	public boolean accepts(String suffix) {
		log.debug("AcceptEverythingOsFilter accepts '" + suffix + "'");
		return true;
	}
}
