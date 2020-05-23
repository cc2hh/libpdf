# libpdf

[![](https://jitpack.io/v/cc2hh/libpdf.svg)](https://jitpack.io/#cc2hh/libpdf)


How to
To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

gradle
maven
sbt
leiningen
Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.cc2hh:libpdf:1.6.6'
	}
