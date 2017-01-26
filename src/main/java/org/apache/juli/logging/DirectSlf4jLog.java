package org.apache.juli.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de - klingerm
 */
public class DirectSlf4jLog implements Log {
	private Logger delegate;

	public static Log getInstance(String name) {
		try {
			return new DirectSlf4jLog(LoggerFactory.getLogger(name));
		} catch (Throwable e) {
			throw new LogConfigurationException("Error creating SLF4J logger", e);
		}
	}

	public static void release() {
	}

	public DirectSlf4jLog(Logger delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean isDebugEnabled() {
		return delegate.isDebugEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return delegate.isErrorEnabled();
	}

	@Override
	public boolean isFatalEnabled() {
		return delegate.isErrorEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return delegate.isInfoEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return delegate.isTraceEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return delegate.isWarnEnabled();
	}

	@Override
	public void trace(Object message) {
		delegate.trace(String.valueOf(message));
	}

	@Override
	public void trace(Object message, Throwable t) {
		delegate.trace(String.valueOf(message), t);
	}

	@Override
	public void debug(Object message) {
		delegate.debug(String.valueOf(message));
	}

	@Override
	public void debug(Object message, Throwable t) {
		delegate.debug(String.valueOf(message), t);
	}

	@Override
	public void info(Object message) {
		delegate.info(String.valueOf(message));
	}

	@Override
	public void info(Object message, Throwable t) {
		delegate.info(String.valueOf(message), t);
	}

	@Override
	public void warn(Object message) {
		delegate.warn(String.valueOf(message));
	}

	@Override
	public void warn(Object message, Throwable t) {
		delegate.warn(String.valueOf(message), t);
	}

	@Override
	public void error(Object message) {
		delegate.error(String.valueOf(message));
	}

	@Override
	public void error(Object message, Throwable t) {
		delegate.error(String.valueOf(message), t);
	}

	@Override
	public void fatal(Object message) {
		delegate.error(String.valueOf(message));
	}

	@Override
	public void fatal(Object message, Throwable t) {
		delegate.error(String.valueOf(message), t);
	}
}
