package com.teamtter.mavennatives.nativedependencies;

public class AcceptEverythingOsFilter extends OsFilter {

	@Override
	public boolean accepts(String suffix) {
		return true;
	}
}
