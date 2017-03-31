Tomcat logging directly to SLFJ4
================================

[![Maven Central](https://img.shields.io/maven-central/v/de.mklinger.tomcat/tomcat-juli-slf4j.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22de.mklinger.tomcat%22%20AND%20a%3A%22tomcat-juli-slf4j%22)

This is a drop-in replacement for tomcat-juli.jar. It requires slf4j-api
being available in Tomcat's boot classpath and a logging backend for SLF4J
being available, also in Tomcat's boot classpath.
