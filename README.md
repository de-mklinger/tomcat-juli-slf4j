Tomcat logging directly to SLFJ4
================================

[![Maven Central](https://img.shields.io/maven-central/v/de.mklinger.tomcat/tomcat-juli-slf4j/9.0.svg)](https://search.maven.org/search?q=g:de.mklinger.tomcat%20AND%20a:tomcat-juli-slf4j%20AND%20v:9.0.*)
[![Maven Central](https://img.shields.io/maven-central/v/de.mklinger.tomcat/tomcat-juli-slf4j/8.5.svg)](https://search.maven.org/search?q=g:de.mklinger.tomcat%20AND%20a:tomcat-juli-slf4j%20AND%20v:8.5.*)
[![Maven Central](https://img.shields.io/maven-central/v/de.mklinger.tomcat/tomcat-juli-slf4j/8.0.svg)](https://search.maven.org/search?q=g:de.mklinger.tomcat%20AND%20a:tomcat-juli-slf4j%20AND%20v:8.0.*)

This is a drop-in replacement for tomcat-juli.jar. It requires slf4j-api
being available in Tomcat's boot classpath and a logging backend for SLF4J
being available, also in Tomcat's boot classpath.
