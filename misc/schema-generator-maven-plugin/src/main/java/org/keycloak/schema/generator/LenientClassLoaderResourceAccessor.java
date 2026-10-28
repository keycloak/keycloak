package org.keycloak.schema.generator;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * A {@link ClassLoaderResourceAccessor} whose internal {@link ClassLoader} silently substitutes a
 * synthesised stub class for any class that cannot be found on the classpath.
 *
 * <p>The schema-generator only needs to <em>parse</em> Liquibase changelogs and generate SQL; it never
 * executes custom changes. When a changelog references a {@code <customChange>} implementation that is
 * not present on the generator classpath, Liquibase calls
 * {@code Scope.getCurrentScope().getClassLoader().loadClass(className)} and immediately casts the result
 * to {@code CustomChange}. A stub that extends {@link DummyCustomChange} (which implements both
 * {@code CustomSqlChange} and {@code CustomTaskChange}) satisfies that cast.
 *
 * <p>The stub bytecode must carry the <em>exact binary name</em> Liquibase requested; otherwise
 * {@code Class.forName} rejects it with a {@code ClassNotFoundException} even though {@code loadClass}
 * returned successfully. The minimal bytecode generated here is a public class that extends
 * {@link DummyCustomChange} with only a no-arg constructor — identical in behaviour to its parent.
 */
class LenientClassLoaderResourceAccessor extends ClassLoaderResourceAccessor {

    /**
     * Holder trick: {@code super(…)} must be the first statement, so we stash the loader in a
     * {@code ThreadLocal} before calling the parent constructor and retrieve it afterwards.
     */
    private static final ThreadLocal<ClassLoader> INIT_LOADER = new ThreadLocal<>();

    private final ClassLoader lenientLoader;

    LenientClassLoaderResourceAccessor(ClassLoader parent) {
        super(initLoader(parent));
        this.lenientLoader = INIT_LOADER.get();
        INIT_LOADER.remove();
    }

    /**
     * Returns the lenient {@link ClassLoader} that must also be registered in the Liquibase
     * {@code Scope} so that {@code Class.forName(name, true, scope.getClassLoader())} reaches our
     * stub-generating logic.
     */
    ClassLoader getClassLoader() {
        return lenientLoader;
    }

    // -------------------------------------------------------------------------

    private static ClassLoader initLoader(ClassLoader parent) {
        ClassLoader loader = new StubClassLoader(parent);
        INIT_LOADER.set(loader);
        return loader;
    }

    // -------------------------------------------------------------------------

    /**
     * A {@link ClassLoader} that synthesises a minimal stub class extending {@link DummyCustomChange}
     * for any name that cannot be resolved through the normal delegation chain.
     */
    private static final class StubClassLoader extends ClassLoader {

        StubClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                byte[] bytecode = generateStubBytecode(name);
                return defineClass(name, bytecode, 0, bytecode.length);
            }
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                Class<?> clazz = findClass(name);
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
        }

        /**
         * Emits the minimal valid bytecode for a public class with binary name {@code className} that
         * extends {@link DummyCustomChange} and has only a default no-arg constructor.
         */
        private static byte[] generateStubBytecode(String className) {
            String internalName = className.replace('.', '/');
            String superName = "org/keycloak/schema/generator/DummyCustomChange";

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                // Magic number
                dos.writeInt(0xCAFEBABE);
                // Minor & Major version (Java 17 = 61)
                dos.writeShort(0);
                dos.writeShort(61);

                // Constant pool (10 entries, indexed 1–9)
                dos.writeShort(10);

                // 1: Utf8  internalName
                dos.writeByte(1); dos.writeUTF(internalName);
                // 2: Class → 1
                dos.writeByte(7); dos.writeShort(1);
                // 3: Utf8  superName
                dos.writeByte(1); dos.writeUTF(superName);
                // 4: Class → 3
                dos.writeByte(7); dos.writeShort(3);
                // 5: Utf8  "<init>"
                dos.writeByte(1); dos.writeUTF("<init>");
                // 6: Utf8  "()V"
                dos.writeByte(1); dos.writeUTF("()V");
                // 7: Utf8  "Code"
                dos.writeByte(1); dos.writeUTF("Code");
                // 8: NameAndType → 5, 6
                dos.writeByte(12); dos.writeShort(5); dos.writeShort(6);
                // 9: Methodref → Class 4, NameAndType 8
                dos.writeByte(10); dos.writeShort(4); dos.writeShort(8);

                // Access flags: ACC_PUBLIC | ACC_SUPER
                dos.writeShort(0x0021);
                // this class, super class
                dos.writeShort(2);
                dos.writeShort(4);
                // interfaces, fields: none
                dos.writeShort(0);
                dos.writeShort(0);

                // Methods: one <init>
                dos.writeShort(1);
                dos.writeShort(0x0001); // ACC_PUBLIC
                dos.writeShort(5);      // name index: "<init>"
                dos.writeShort(6);      // descriptor index: "()V"
                dos.writeShort(1);      // attributes count

                // Code attribute for <init>: aload_0 + invokespecial super.<init> + return
                dos.writeShort(7);   // attribute name index: "Code"
                dos.writeInt(17);    // attribute length
                dos.writeShort(1);   // max_stack
                dos.writeShort(1);   // max_locals
                dos.writeInt(5);     // code length
                dos.writeByte(0x2A); // aload_0
                dos.writeByte(0xB7); // invokespecial
                dos.writeShort(9);   // methodref 9
                dos.writeByte(0xB1); // return
                dos.writeShort(0);   // exception table length
                dos.writeShort(0);   // code attributes count

                // Class attributes: none
                dos.writeShort(0);

                dos.flush();
                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate stub class for " + className, e);
            }
        }
    }
}
