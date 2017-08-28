package de.mklinger.tomcat.juli.logging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import de.mklinger.commons.exec.Cmd;
import de.mklinger.commons.exec.CmdBuilder;
import de.mklinger.commons.exec.CommandLineException;
import de.mklinger.commons.exec.CommandLineUtil;
import de.mklinger.commons.exec.JavaHome;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de - klingerm
 */
public class TomcatIT {
	@Test
	public void test() throws IOException, CommandLineException {
		final File catalinaHome = getCatalinaHome();
		checkCatalinaHome(catalinaHome);

		final int controlPort = getFreePort();
		final int httpPort = getFreePort();
		final int ajpPort = getFreePort();

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

			final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

			final Pattern p = Pattern.compile(Pattern.quote("[main] INFO org.apache.catalina.startup.Catalina - Server startup in ") + "\\d+" + Pattern.quote(" ms"));
			final File binDir = new File(catalinaHome, "bin");
			final String executableName;
			if (CommandLineUtil.isWindows()) {
				executableName = "catalina.bat";
			} else {
				executableName = "catalina.sh";
			}
			final File executable = new File(binDir, executableName);
			final Cmd cmd = new CmdBuilder(executable)
					.arg("run")
					.environment("CATALINA_HOME", catalinaHome.getAbsolutePath())
					.environment("JAVA_HOME", JavaHome.getByRuntime().getJavaHome().getAbsolutePath())
					.redirectErrorStream(true)
					.stdout(stderr)
					.destroyForcibly(true)
					.destroyOnError(true)
					.ping(() -> {
						final Matcher m = p.matcher(stderr.toString());
						if (m.find()) {
							if (CommandLineUtil.isWindows()) {
								stopTomcat(catalinaHome, executable);
							}
							throw new TestSuccessException();
						}
					})
					.timeout(10, TimeUnit.SECONDS)
					.toCmd();

			try {
				cmd.execute();
				throw new CommandLineException("Execute returned without matching log");
			} catch (final CommandLineException e) {
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

	private void stopTomcat(final File catalinaHome, final File executable) {
		try {
			new CmdBuilder(executable)
			.arg("stop")
			.environment("CATALINA_HOME", catalinaHome.getAbsolutePath())
			.environment("JAVA_HOME", JavaHome.getByRuntime().getJavaHome().getAbsolutePath())
			.toCmd()
			.execute();
		} catch (final CommandLineException e) {
			throw new RuntimeException("Stop failed");
		}
	}

	private static int getFreePort() throws IOException {
		try (ServerSocket ss = new ServerSocket()) {
			ss.bind(null);
			return ss.getLocalPort();
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
