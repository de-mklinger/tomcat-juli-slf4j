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

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de - klingerm
 */
public class TomcatIT {
	@Test
	public void test() throws IOException, CommandLineException {
		File catalinaHome = getCatalinaHome();
		checkCatalinaHome(catalinaHome);

		int controlPort = getFreePort();
		int httpPort = getFreePort();
		int ajpPort = getFreePort();

		File confDir = new File(catalinaHome, "conf");
		File serverXml = new File(confDir, "server.xml");
		Path bak = Files.createTempFile("server", ".xml");
		Files.copy(serverXml.toPath(), bak, StandardCopyOption.REPLACE_EXISTING);
		try {
			Files.write(serverXml.toPath(),
					Files.readAllLines(serverXml.toPath(), StandardCharsets.UTF_8).stream()
					.map(line -> line.replace("8005", String.valueOf(controlPort)))
					.map(line -> line.replace("8080", String.valueOf(httpPort)))
					.map(line -> line.replace("8009", String.valueOf(ajpPort)))
					.collect(Collectors.toList()));

			ByteArrayOutputStream stderr = new ByteArrayOutputStream();

			Pattern p = Pattern.compile(Pattern.quote("[main] INFO org.apache.catalina.startup.Catalina - Server startup in ") + "\\d+" + Pattern.quote(" ms"));
			File binDir = new File(catalinaHome, "bin");
			Cmd cmd = new CmdBuilder(new File(binDir, "catalina.sh"))
					.arg("run")
					.stderr(stderr)
					.destroyForcibly(true)
					.destroyOnError(true)
					.ping(() -> {
						Matcher m = p.matcher(stderr.toString());
						if (m.find()) {
							throw new TestSuccessException();
						}
					})
					.timeout(10, TimeUnit.SECONDS)
					.toCmd();

			try {
				cmd.execute();
			} catch (CommandLineException e) {
				Throwable cause = e.getCause();
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

	private static int getFreePort() throws IOException {
		try (ServerSocket ss = new ServerSocket()) {
			ss.bind(null);
			return ss.getLocalPort();
		}
	}

	private static class TestSuccessException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	private void checkCatalinaHome(File catalinaHome) {
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
		String catalinaHomeProp = System.getProperty("catalina.home");
		if (catalinaHomeProp != null && !catalinaHomeProp.isEmpty()) {
			return new File(catalinaHomeProp);
		} else {
			File itestDir = new File("target/itest");
			if (!itestDir.isDirectory()) {
				throw new FileNotFoundException("ITest directory not found");
			}
			File[] files = itestDir.listFiles();
			if (files == null || files.length != 1) {
				throw new FileNotFoundException("ITest directory not found");
			}
			return files[0];
		}
	}
}
