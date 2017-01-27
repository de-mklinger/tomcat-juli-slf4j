package de.mklinger.tomcat.juli.logging;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogConfigurationException;
import org.apache.juli.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de - klingerm
 */
@RunWith(Parameterized.class)
public class ApiTest {
	@Parameters(name= "{index}: {0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ Log.class },
			{ LogConfigurationException.class },
			{ LogFactory.class }
		});
	}

	private Class<?> localClass;

	public ApiTest(Class<?> localClass) {
		this.localClass = localClass;
	}

	@Test
	public void testApi() throws IOException {
		String resourceName = getResourceName(localClass);
		try (ZipInputStream zin = new ZipInputStream(getClass().getClassLoader().getResourceAsStream("tomcat-juli.jar"))) {
			ZipEntry entry;
			while ((entry = zin.getNextEntry()) != null) {
				if (resourceName.equals(entry.getName())) {
					check(zin);
					return;
				}
			}
			Assert.fail("Test class resource not found in jar: " + resourceName);
		}
	}

	private static String getResourceName(Class<?> clazz) {
		return clazz.getName().replace('.', '/') + ".class";
	}

	private void check(InputStream tomcatClassIn) throws IOException {
		ClassReader classReader = new ClassReader(tomcatClassIn);
		classReader.accept(new ClassVisitor(Opcodes.ASM5) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				checkClass(superName, interfaces);
			}

			@Override
			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				checkField(access, name, desc);
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				checkMethod(access, name, desc);
				return null;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	private void checkClass(String superName, String[] interfaces) {
		Class<?> tomcatSuperClass = Object.class;
		if (superName != null) {
			tomcatSuperClass = findLocalClass(superName.replace('/', '.'));
		}
		Class<?> superClass = localClass.getSuperclass();
		if (superClass == null) {
			superClass = Object.class;
		}

		Assert.assertEquals("Local class extends wrong super class - ", tomcatSuperClass, superClass);

		Set<Class<?>> tomcatInterfaces = new HashSet<>();
		for (String tomcatInterfaceName : interfaces) {
			Class<?> tomcatInterface = findLocalClass(tomcatInterfaceName.replace('/', '.'));
			tomcatInterfaces.add(tomcatInterface);
		}

		Set<Class<?>> localInterfaces = new HashSet<>();
		Collections.addAll(localInterfaces, localClass.getInterfaces());

		Assert.assertEquals("Local class does not implement same interfaces - ", tomcatInterfaces, localInterfaces);
	}

	private void checkField(int access, String name, String desc) {
		if ((Opcodes.ACC_PRIVATE & access) != 0) {
			return;
		}

		Class<?> type = findLocalClass(Type.getType(desc));

		Field field;
		try {
			field = localClass.getDeclaredField(name);
		} catch (NoSuchFieldException | SecurityException e) {
			// not found
			Assert.fail("Field not found in local class " + localClass + ": " + name + " " + desc);
			return; // make compiler happy
		}

		Assert.assertEquals(prefix(field, "Wrong type: "), type, field.getType());

		Assert.assertEquals(prefix(field, "abstract: "), (Opcodes.ACC_ABSTRACT & access) != 0, Modifier.isAbstract(field.getModifiers()));
		Assert.assertEquals(prefix(field, "final: "), (Opcodes.ACC_FINAL & access) != 0, Modifier.isFinal(field.getModifiers()));
		Assert.assertEquals(prefix(field, "private: "), (Opcodes.ACC_PRIVATE & access) != 0, Modifier.isPrivate(field.getModifiers()));
		Assert.assertEquals(prefix(field, "protected: "), (Opcodes.ACC_PROTECTED & access) != 0, Modifier.isProtected(field.getModifiers()));
		Assert.assertEquals(prefix(field, "public: "), (Opcodes.ACC_PUBLIC & access) != 0, Modifier.isPublic(field.getModifiers()));
		Assert.assertEquals(prefix(field, "static: "), (Opcodes.ACC_STATIC & access) != 0, Modifier.isStatic(field.getModifiers()));
	}

	private void checkMethod(int access, String name, String desc) {
		if ("<clinit>".equals(name)) {
			// ignore static blocks
			return;
		}
		if ((Opcodes.ACC_PRIVATE & access) != 0) {
			return;
		}

		Class<?>[] parameterTypes = getParameterTypes(desc);
		Class<?> returnType = getReturnType(desc);
		try {
			if ("<init>".equals(name)) {
				Constructor<?> constructor = localClass.getDeclaredConstructor(parameterTypes);
				Assert.assertEquals(prefix(constructor, "private: "), (Opcodes.ACC_PRIVATE & access) != 0, Modifier.isPrivate(constructor.getModifiers()));
				Assert.assertEquals(prefix(constructor, "protected: "), (Opcodes.ACC_PROTECTED & access) != 0, Modifier.isProtected(constructor.getModifiers()));
				Assert.assertEquals(prefix(constructor, "public: "), (Opcodes.ACC_PUBLIC & access) != 0, Modifier.isPublic(constructor.getModifiers()));
			} else {
				Method method = localClass.getDeclaredMethod(name, parameterTypes);
				Assert.assertEquals(prefix(method, "Wrong return type: "), returnType, method.getReturnType());
				Assert.assertEquals(prefix(method, "abstract: "), (Opcodes.ACC_ABSTRACT & access) != 0, Modifier.isAbstract(method.getModifiers()));
				Assert.assertEquals(prefix(method, "final: "), (Opcodes.ACC_FINAL & access) != 0, Modifier.isFinal(method.getModifiers()));
				Assert.assertEquals(prefix(method, "private: "), (Opcodes.ACC_PRIVATE & access) != 0, Modifier.isPrivate(method.getModifiers()));
				Assert.assertEquals(prefix(method, "protected: "), (Opcodes.ACC_PROTECTED & access) != 0, Modifier.isProtected(method.getModifiers()));
				Assert.assertEquals(prefix(method, "public: "), (Opcodes.ACC_PUBLIC & access) != 0, Modifier.isPublic(method.getModifiers()));
				Assert.assertEquals(prefix(method, "static: "), (Opcodes.ACC_STATIC & access) != 0, Modifier.isStatic(method.getModifiers()));
			}
		} catch (NoSuchMethodException | SecurityException e) {
			// not found
			Assert.fail("Method not found in local class " + localClass + ": " + name + " " + desc);
		}
	}

	public static String prefix(Member e, String msg) {
		return e.getClass().getSimpleName() + " '" + e.getName() + "': " + msg;
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

		return findLocalClass(name);
	}

	private static Class<?> findLocalClass(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Type referenced by tomcat class not found as local class: " + name, e);
		}
	}
}
