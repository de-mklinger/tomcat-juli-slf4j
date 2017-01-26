package de.mklinger.tomcat.juli.logging;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.juli.logging.Log;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de - klingerm
 */
public class ApiTest {
	@Test
	public void testLogInterface() throws IOException {
		try (ZipInputStream zin = new ZipInputStream(getClass().getClassLoader().getResourceAsStream("tomcat-juli.jar"))) {
			ZipEntry entry;
			while ((entry = zin.getNextEntry()) != null) {
				if ("org/apache/juli/logging/Log.class".equals(entry.getName())) {
					Class<?> logClass = Log.class;
					check(zin, logClass);
				}
			}
		}
	}

	private void check(InputStream tomcatClassIn, Class<?> localClass) throws IOException {
		ClassReader classReader = new ClassReader(tomcatClassIn);
		classReader.accept(new ClassVisitor(Opcodes.ASM5) {
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				// TODO check visible fields
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// TODO only if visible
				Class<?>[] parameterTypes = getParameterTypes(desc);
				Class<?> returnType = getReturnType(desc);
				try {
					Method method = localClass.getDeclaredMethod(name, parameterTypes);
					// found.
					Assert.assertEquals("Method in local class has wrong return type", returnType, method.getReturnType());
				} catch (NoSuchMethodException | SecurityException e) {
					// not found
					Assert.fail("Method not found in local class " + localClass + ": " + name + " " + desc);
				}
				return null;
			}

			private Class<?> getReturnType(String desc) {
				return findLocalClass(Type.getReturnType(desc));
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	private static Class<?>[] getParameterTypes(String desc) {
		Type[] argumentTypes = Type.getArgumentTypes(desc);
		Class<?>[] parameterTypes = new Class<?>[argumentTypes.length];
		for (int i = 0; i < argumentTypes.length; i++) {
			parameterTypes[i] = findLocalClass(argumentTypes[i]);
		}
		return parameterTypes;
	}

	private static Class<?> findLocalClass(Type type) {
		if ("void".equals(type.getClassName())) {
			return Void.TYPE;
		}
		if ("boolean".equals(type.getClassName())) {
			return Boolean.TYPE;
		}
		if ("long".equals(type.getClassName())) {
			return Long.TYPE;
		}
		if ("int".equals(type.getClassName())) {
			return Integer.TYPE;
		}
		// TODO other primitives
		try {
			return Class.forName(type.getClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Type referenced by tomcat class not found: " + type.getClassName(), e);
		}
	}
}
