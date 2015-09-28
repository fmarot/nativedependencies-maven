package com.teamtter.mavennatives.nativedependencies;

import org.codehaus.plexus.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
public class OsFilter {
	static final String OS = System.getProperty("os.name").toLowerCase();
	static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

	private String osName;
	/** Optional, WARNING: for 32bit, should be "x86", not "32" !!! */
	private String osArch;
	private String suffix;

	public OsFilter() {
	}

	public boolean accepts(String effectiveSuffix) {

		effectiveSuffix = effectiveSuffix.toLowerCase();
		osName = osName != null ? osName.toLowerCase() : "";
		osArch = osArch != null ? osArch.toLowerCase() : "";
		suffix = suffix != null ? suffix.toLowerCase() : "";

		boolean filterMatches = false;
		if (StringUtils.isNotBlank(osName) && OS.contains(osName)) {
			if (StringUtils.isBlank(osArch) || filterToOsArchMatches()) {
				filterMatches = effectiveSuffix.contains(this.suffix);
			}
		}

		log.debug(this.toString() + " accepts suffix '" + effectiveSuffix + "': " + filterMatches);
		return filterMatches;
	}

	private boolean filterToOsArchMatches() {
		return StringUtils.isNotBlank(osArch) && OS_ARCH.contains(osArch);
	}

}
