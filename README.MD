# mavennatives

Automatically exported from code.google.com/p/mavennatives which was at version 0.0.7.
This maven plugin has then been renamed to "nativedependencies-maven-plugin" (to follow Apache Maven conventions) and groupid changed to com.teamtter.mavennatives.

# TODO

* publish on Maven Central
* transparently handle misc compression format (zip, tar, tgz, 7zip...) and single file not compressed deps (.dll, .so, .dylib...)
* offer possibility to extract native files only once in a multi-module project at the root
* keep a cache of the signature (md5, sha1, date ???) for each compressed file to avoid uncompressing it again if it has not changed
* option to avoid overwriting deps if newer already present
* update Eclipse plugin to
** match new names and grouId
** match current Eclipse V4 standards
** m2e connector ???
* make Eclipse plugin automatically add -Djava.library.path (to run targets, junit tests, etc)
* make Eclipse plugin automatically add LD_LIBRARY_PATH entries (to run targets, junit tests, etc)
* optionnally enable other locations to be added in both above

# Done

* use real Java 5 annotations
* parameter to be able to skip the plugin execution (overridable through a variable)
* add parameters to auto-detect platform and get only platform specific libs


## Problematic that this plugin will try to solve

* enable a native developer to compile it's native lib (we don't care how) and place them in a folder where they won't be overwriten on each mvn text byt the ones in .m2 or Archiva
** use another folder and play on the order of folder declaration in the -Djava.library.path ???
** do not overwrite if newer
** another solution ?
* streamline command-line VS Eclipse use => if it works on command-line, it should work in Eclipse without touching anything !


# Reasons for forking original project

* add finer grain control over what natives dependencies will be unpacked.
* familiarize myself with the dev of Maven plugins.
* improve eclipse plugin
* finally find a way to prevent each and every project using native libs to have to manually (god I have this word!) configure the -Djava.library.path and LD_LIBRARY_PATH

License kept to : Apache License 2.0 