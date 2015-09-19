# mavennatives

Automatically exported from code.google.com/p/mavennatives which was at version 0.0.7.
This maven plugin has then been renamed to "nativedependencies-maven-plugin" (to follow Apache Maven conventions) and groupid changed to com.teamtter.mavennatives.

# TODO

* use latest dep versions
* use real Java 5 annotations
* add parameters to auto-detect platform and get only platform specific libs
* make Eclipse plugin automatically add -Djava.library.path (to run targets, junit tests, etc)
* make Eclipse plugin automatically add LD_LIBRARY_PATH entries (to run targets, junit tests, etc)
* optionnally enable other locations to be added in both above

# Reasons for forking original project

* add finer grain control over what natives dependencies will be unpacked.
* familiarize myself with the dev of Maven plugins.
* improve eclipse plugin
* finally find a way to prevent each and every project using native libs to have to manually (god I have this word!) configure the -Djava.library.path and LD_LIBRARY_PATH

License kept to : Apache License 2.0 
