package de.mklinger.tomcat.juli.logging;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogConfigurationException;
import org.apache.juli.logging.LogFactory;
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
				if (getResourceName(Log.class).equals(entry.getName())) {
					check(zin, Log.class);
				} else if (getResourceName(LogConfigurationException.class).equals(entry.getName())) {
					check(zin, LogConfigurationException.class);
				} else if (getResourceName(LogFactory.class).equals(entry.getName())) {
					check(zin, LogFactory.class);
				}
			}
		}
	}

	private static String getResourceName(Class<?> clazz) {
		return clazz.getName().replace('.', '/') + ".class";
	}

	private void check(InputStream tomcatClassIn, Class<?> localClass) throws IOException {
		ClassReader classReader = new ClassReader(tomcatClassIn);
		classReader.accept(new ClassVisitor(Opcodes.ASM5) {
			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				if ((Opcodes.ACC_PRIVATE & access) != 0) {
					return null;
				}

				Class<?> type = findLocalClass(Type.getType(desc));

				try {
					Field field = localClass.getDeclaredField(name);
					Assert.assertEquals("Field in local class has wrong type", type, field.getType());

					// TODO check same visibility and other modifiers (static)

				} catch (NoSuchFieldException | SecurityException e) {
					// not found
					Assert.fail("Field not found in local class " + localClass + ": " + name + " " + desc);
				}

				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if ("<clinit>".equals(name)) {
					// ignore static blocks
					return null;
				}
				if ((Opcodes.ACC_PRIVATE & access) != 0) {
					return null;
				}

				Class<?>[] parameterTypes = getParameterTypes(desc);
				Class<?> returnType = getReturnType(desc);
				try {
					if ("<init>".equals(name)) {
						localClass.getDeclaredConstructor(parameterTypes);
						// TODO check same visibility and other modifiers (static)
					} else {
						Method method = localClass.getDeclaredMethod(name, parameterTypes);
						Assert.assertEquals("Method in local class has wrong return type", returnType, method.getReturnType());
						// TODO check same visibility and other modifiers (static)
					}
				} catch (NoSuchMethodException | SecurityException e) {
					// not found
					Assert.fail("Method not found in local class " + localClass + ": " + name + " " + desc);
				}
				return null;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	private static Class<?> getReturnType(String desc) {
		return findLocalClass(Type.getReturnType(desc));
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
		if ("short".equals(type.getClassName())) {
			return Short.TYPE;
		}
		if ("char".equals(type.getClassName())) {
			return Character.TYPE;
		}
		if ("byte".equals(type.getClassName())) {
			return Byte.TYPE;
		}
		if ("flloat".equals(type.getClassName())) {
			return Byte.TYPE;
		}
		if ("double".equals(type.getClassName())) {
			return Byte.TYPE;
		}

		String name;
		if (type.getSort() == Type.ARRAY) {
			name = type.getDescriptor().replace('/', '.');
		} else {
			name = type.getClassName();
		}

		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Type referenced by tomcat class not found: " + type.getClassName(), e);
		}
	}
}
