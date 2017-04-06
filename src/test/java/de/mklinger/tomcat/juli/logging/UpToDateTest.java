package de.mklinger.tomcat.juli.logging;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class UpToDateTest {
	private static final String TOMCAT_VERSION_PROPERTY = "tomcat.version";

	static {
		System.setProperty("java.net.useSystemProxies", "true");
	}

	@Test
	public void test() throws MalformedURLException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		String referenceVersionString = System.getProperty(TOMCAT_VERSION_PROPERTY);
		if (referenceVersionString == null || referenceVersionString.isEmpty()) {
			throw new IllegalArgumentException("No reference version given. Use system property '" + TOMCAT_VERSION_PROPERTY + "'");
		}

		Version referenceVersion = new Version(referenceVersionString);

		List<Version> versions = loadVersions();
		if (versions.isEmpty()) {
			throw new IllegalStateException("No versions found");
		}

		System.out.println("Reference version: " + referenceVersion);
		List<Version> laterVersions = new ArrayList<>();
		for (Version version : versions) {
			if (version.equalsSeries(referenceVersion) && !version.hasSuffix() && version.isLaterThan(referenceVersion)) {
				laterVersions.add(version);
				System.out.println("Later version available: " + version);
			}
		}
		if (laterVersions.isEmpty()) {
			System.out.println("Up to date.");
			return;
		} else {
			System.out.println("Not up to date.");
			throw new AssertionError("Not up to date. Reference version: " + referenceVersion + " - Later versions: " + laterVersions);
		}
	}

	private static List<Version> loadVersions() throws SAXException, IOException, ParserConfigurationException, MalformedURLException, XPathExpressionException {
		Document doc;
		try (InputStream in = URI.create("https://repo1.maven.org/maven2/org/apache/tomcat/tomcat-juli/maven-metadata.xml").toURL().openStream()) {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		}

		XPathExpression xpe = XPathFactory.newInstance().newXPath().compile("/metadata/versioning/versions/version");

		NodeList nl = (NodeList)xpe.evaluate(doc, XPathConstants.NODESET);
		List<Version> versions = new ArrayList<>(nl.getLength());
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			String version = n.getTextContent();
			versions.add(new Version(version));
		}
		return versions;
	}

	private static class Version implements Comparable<Version> {
		private final String versionString;
		private final int major;
		private final int minor;
		private final int fix;
		private final String suffix;

		private Version(final String versionString) {
			this.versionString = versionString;
			StringTokenizer st = new StringTokenizer(versionString, ".-");
			if (st.countTokens() < 3) {
				throw new IllegalArgumentException("Unparseable version string: " + versionString);
			}
			this.major = Integer.parseInt(st.nextToken());
			this.minor = Integer.parseInt(st.nextToken());
			this.fix = Integer.parseInt(st.nextToken());

			StringBuilder sb = new StringBuilder();
			while (st.hasMoreTokens()) {
				if (sb.length() > 0) {
					sb.append('.');
				}
				sb.append(st.nextToken());
			}
			this.suffix = sb.toString();
		}

		public boolean equalsSeries(final Version other) {
			return other.major == this.major && other.minor == this.minor;
		}

		public boolean hasSuffix() {
			return !this.suffix.isEmpty();
		}

		public boolean isLaterThan(final Version version) {
			return compareTo(version) > 0;
		}

		@Override
		public int compareTo(final Version o) {
			int n = Integer.compare(major, o.major);
			if (n != 0) {
				return n;
			}
			n = Integer.compare(minor, o.minor);
			if (n != 0) {
				return n;
			}
			n = Integer.compare(fix, o.fix);
			if (n != 0) {
				return n;
			}
			return suffix.compareTo(o.suffix);
		}

		@Override
		public String toString() {
			return versionString;
		}
	}
}
