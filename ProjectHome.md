# Maven Native Dependencies #

This project contains a series of plugins and tools to make using native dependencies in maven easier.

## maven-nativedependencies-plugin ##

This plugin unpacks every dependency with a classifier beginning with "natives-".

Adding this config to your pom will bind the plugin to the package phase and unpack the natives in the "target/natives" directory.

```
<plugin>
	<groupId>com.googlecode.mavennatives</groupId>
	<artifactId>maven-nativedependencies-plugin</artifactId>
	<version>0.0.7</version>
	<executions>
		<execution>
			<id>unpacknatives</id>
			<goals>
				<goal>copy</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

There are some configuration options for the plugin:

```
<configuration>
    <nativesTargetDir>target/natives</nativesTargetDir>
    <separateDirs>false</separateDirs>
</configuration>
```

This are the default values, when enabling **separateDirs** the plugin will unpack each native dependency to a subdir of the **nativesTargetDir** named like its **classifier** (for example: **natives-windows** will go to **target/natives/windows**)



Mavennatives is now hosted on maven central repository so you don't need to do anything extra to use it, other than adding the plugin in your pom.

The development version is 0.0.8-SNAPSHOT

### Example Project ###

There is a maven example project that shows a really simple application using this plugin to handle its native dependencies.

The application displays a triangle rotating in a window it uses [lwjgl](http://www.lwjgl.org/) as the way to use OpenGL.

The pom.xml of the project defines the repo where it can find the lwjgl java and native dependencies.

You can obtain this example from svn using

```
svn checkout http://mavennatives.googlecode.com/svn/trunk/maven-nativedependencies-example/ natives-example
```

after checking out the project use

```
mvn package
```

to produce a zip of the executable project, it contains

  * The application jar
  * The native libraries for windows (32 and 64 bits), linux (32 and 64 bits) and macOSX
  * The lwjgl jar
  * Scripts to run the application (.bat and .sh)


## Eclipse Plugin ##

Since version 0.0.7 of the maven-nativedependencies-plugin if you have m2eclipse installed and the nativedependencies plugin configured the unpacking of natives will run automatically, you don't need the eclipse plugin to unpack them. However in order to setup the java.library.path environment variable in eclipse you will have to do it either manually or automatically using the eclipse plugin.

This Eclipse plugin is an extension to m2eclipse, it detects if you have the maven plugin configured, and if you do it executes the unpacking of natives, and configures the Native Library Location.

If you import a maven project that has the mavennatives plugin configured, and you have the m2eclipse integration plugin, on import the natives will be extracted, also when performing a clean from eclipse the natives will be extracted.

So, if you use both these tools, using native dependencies requires no manual configuration, other than whats in the pom, just run your app and it works.

To install this plugin just add this [update-site](http://mavennatives.googlecode.com/svn/eclipse-update/) to eclipse.

If you use Eclipse m2e 1.0+ just install the latest version from the update site

If you use m2eclipse 0.12.**from sonatype you need to uncheck the option to "only show the latest version" from the eclipse install screen and select the 0.0.1 version of our eclipse plugin.**








