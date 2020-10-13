package de.mklinger.tomcat.juli.logging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import de.mklinger.commons.exec.Cmd;
import de.mklinger.commons.exec.CmdBuilder;
import de.mklinger.commons.exec.CmdException;
import de.mklinger.commons.exec.CmdSettings;
import de.mklinger.commons.exec.CmdUtil;
import de.mklinger.commons.exec.JavaHome;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de - klingerm
 */
public class TomcatIT {
	@Test
	public void test() throws IOException, CmdException {
		final File catalinaHome = getCatalinaHome();
		checkCatalinaHome(catalinaHome);

		final int controlPort = getFreePort();
		final int httpPort = getFreePort();
		final int ajpPort = getFreePort();

		//System.out.println("Using ports:\n\tcontrol port: " + controlPort + "\n\thttp port: " + httpPort + "\n\tajp port: " + ajpPort);

		final File confDir = new File(catalinaHome, "conf");
		final File serverXml = new File(confDir, "server.xml");
		final Path bak = Files.createTempFile("server", ".xml");
		Files.copy(serverXml.toPath(), bak, StandardCopyOption.REPLACE_EXISTING);
		try {
			Files.write(serverXml.toPath(),
					Files.readAllLines(serverXml.toPath(), StandardCharsets.UTF_8).stream()
					.map(line -> line.replace("8005", String.valueOf(controlPort)))
					.map(line -> line.replace("8080", String.valueOf(httpPort)))
					.map(line -> line.replace("8009", String.valueOf(ajpPort)))
					.collect(Collectors.toList()));

			//System.out.println("server.xml:\n\n" + new String(Files.readAllBytes(serverXml.toPath()), "UTF-8") + "\n");

			final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

			final Pattern p = Pattern.compile(Pattern.quote("[main] INFO org.apache.catalina.startup.Catalina - Server startup in [") + "\\d+" + Pattern.quote("] milliseconds"));
			final File binDir = new File(catalinaHome, "bin");
			final String executableName;
			if (CmdUtil.isWindows()) {
				executableName = "catalina.bat";
			} else {
				executableName = "catalina.sh";
			}
			final File executable = new File(binDir, executableName);
			final CmdBuilder cmdb = new CmdBuilder(executable)
					.arg("run")
					.directory(binDir)
					.redirectErrorStream(true)
					.stdout(stderr)
					.destroyForcibly(true)
					.destroyOnError(true)
					.ping(() -> {
						final Matcher m = p.matcher(stderr.toString());
						if (m.find()) {
							if (CmdUtil.isWindows()) {
								stopTomcat(catalinaHome, executable);
							}
							throw new TestSuccessException();
						}
					})
					.timeout(10, TimeUnit.SECONDS);

			final CmdSettings cmdSettings = cmdb.toCmdSettings();
			setEnvironment(cmdSettings, catalinaHome);
			cmdSettings.freeze();

			final Cmd cmd = new Cmd(cmdSettings);
			try {
				cmd.execute();
				throw new CmdException("Execute returned without matching log");
			} catch (final CmdException e) {
				final Throwable cause = e.getCause();
				if (cause == null || !(cause instanceof TestSuccessException)) {
					System.err.println("Tomcat stderr:\n" + stderr.toString());
					throw e;
				}
			} finally {
				cmd.destroyForcibly();
			}

		} finally {
			Files.move(bak, serverXml.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void setEnvironment(final CmdSettings cmdSettings, final File catalinaHome) {
		final Map<String, String> environment = new HashMap<>(System.getenv());

		for (final Iterator<Map.Entry<String, String>> iterator = environment.entrySet().iterator(); iterator.hasNext();) {
			final Map.Entry<String, String> e = iterator.next();
			if (e.getKey().contains("CATALINA") || e.getKey().contains("TOMCAT") || e.getKey().contains("JAVA")) {
				System.out.println("Removing from environment: " + e.getKey() + " -> " + e.getValue());
				iterator.remove();
			}
		}

		environment.put("CATALINA_HOME", catalinaHome.getAbsolutePath());
		environment.put("JAVA_HOME", JavaHome.getByRuntime().getJavaHome().getAbsolutePath());

		cmdSettings.setEnvironment(environment);
	}

	private void stopTomcat(final File catalinaHome, final File executable) {
		try {
			new CmdBuilder(executable)
			.arg("stop")
			.environment("CATALINA_HOME", catalinaHome.getAbsolutePath())
			.environment("JAVA_HOME", JavaHome.getByRuntime().getJavaHome().getAbsolutePath())
			.toCmd()
			.execute();
		} catch (final CmdException e) {
			throw new RuntimeException("Stop failed");
		}
	}

	private static int getFreePort() throws IOException {
		while (true) {
			try (ServerSocket ss = new ServerSocket()) {
				ss.bind(new InetSocketAddress((int) Math.round(Math.random() * 15_000.0 + 50_000.0)));
				return ss.getLocalPort();
			} catch (final IOException e) {
				// ignore
			}
		}
	}

	private static class TestSuccessException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	private void checkCatalinaHome(final File catalinaHome) {
		if (!new File(catalinaHome, "bin").isDirectory()) {
			throw new IllegalStateException("Invalid catalina home: " + catalinaHome);
		}
		if (!new File(catalinaHome, "logs").isDirectory()) {
			throw new IllegalStateException("Invalid catalina home: " + catalinaHome);
		}
		if (!new File(catalinaHome, "conf").isDirectory()) {
			throw new IllegalStateException("Invalid catalina home: " + catalinaHome);
		}
	}

	private File getCatalinaHome() throws FileNotFoundException {
		final String catalinaHomeProp = System.getProperty("catalina.home");
		if (catalinaHomeProp != null && !catalinaHomeProp.isEmpty()) {
			return new File(catalinaHomeProp);
		} else {
			final File itestDir = new File("target/itest");
			if (!itestDir.isDirectory()) {
				throw new FileNotFoundException("ITest directory not found");
			}
			final File[] files = itestDir.listFiles();
			if (files == null || files.length != 1) {
				throw new FileNotFoundException("ITest directory not found");
			}
			return files[0];
		}
	}
}
