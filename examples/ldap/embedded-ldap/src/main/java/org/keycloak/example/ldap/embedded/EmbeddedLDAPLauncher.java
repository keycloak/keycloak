package org.keycloak.example.ldap.embedded;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows to run embedded ApacheDS LDAP or Kerberos server
 *
 * It is supposed to be executed from JAR file. For example:
 * java -jar target/embedded-ldap.jar ldap
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EmbeddedLDAPLauncher {

    public static void main(String[] args) throws Exception {
        String arg = args.length == 0 ? null : args[0];
        if (arg == null) {
            System.err.println("Missing argument: either 'kerberos', 'ldap' or 'keytabCreator' must be passed as argument");
            System.exit(1);
        }

        String clazz = null;
        File home = getHome();
        Properties defaultProperties = new Properties();
        if (arg.equalsIgnoreCase("ldap")) {

            clazz = "org.keycloak.util.ldap.LDAPEmbeddedServer";
            File ldapLdif = file(home, "..", "ldap-app", "users.ldif");
            defaultProperties.put("ldap.ldif", ldapLdif.getAbsolutePath());
        } else if (arg.equalsIgnoreCase("kerberos")) {

            clazz = "org.keycloak.util.ldap.KerberosEmbeddedServer";
            File kerberosLdif = file(home, "..", "..", "kerberos", "users.ldif");
            defaultProperties.put("ldap.ldif", kerberosLdif.getAbsolutePath());
        } else if (arg.equalsIgnoreCase("keytabCreator")) {

            clazz = "org.keycloak.util.ldap.KerberosKeytabCreator";
        } else {

            System.err.println("Invalid argument: '" + arg + "' . Either 'kerberos', 'ldap' or 'keytabCreator' must be passed as argument");
            System.exit(1);
        }

        // Remove first argument
        String[] newArgs = new String[args.length - 1];
        for (int i=0 ; i<(args.length - 1) ; i++) {
            newArgs[i] = args[i + 1];
        }

        System.out.println("Executing " + clazz);
        runClass(clazz, newArgs, defaultProperties);
    }


    private static void runClass(String className, String[] args, Properties defaultProperties) throws Exception {
        File home = getHome();
        File lib = file(home, "target", "embedded-ldap");

        if (!lib.exists()) {
            System.err.println("Could not find lib directory: " + lib.toString());
            System.exit(1);
        } else {
            System.out.println("Found directory to load jars: " + lib.getAbsolutePath());
        }

        List<URL> jars = new ArrayList<URL>();
        for (File file : lib.listFiles()) {
            jars.add(file.toURI().toURL());
        }
        URL[] urls = jars.toArray(new URL[jars.size()]);
        URLClassLoader loader = new URLClassLoader(urls, EmbeddedLDAPLauncher.class.getClassLoader());

        Class mainClass = loader.loadClass(className);
        Method executeMethod = null;
        for (Method m : mainClass.getMethods()) if (m.getName().equals("execute")) { executeMethod = m; break; }
        Object obj = args;
        executeMethod.invoke(null, obj, defaultProperties);
    }


    private static File getHome() {
        String launcherPath = EmbeddedLDAPLauncher.class.getName().replace('.', '/') + ".class";
        URL jarfile = EmbeddedLDAPLauncher.class.getClassLoader().getResource(launcherPath);
        if (jarfile != null) {
            Matcher m = Pattern.compile("jar:(file:.*)!/" + launcherPath).matcher(jarfile.toString());
            if (m.matches()) {
                try {
                    File jarPath = new File(new URI(m.group(1)));
                    File libPath = jarPath.getParentFile().getParentFile();
                    System.out.println("Home directory: " + libPath.toString());
                    if (!libPath.exists()) {
                        System.exit(1);

                    }
                    return libPath;
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.err.println("jar file null: " + launcherPath);
        }
        return null;
    }

    private static File file(File home, String... pathItems) {
        File current = home;

        for (String item : pathItems) {
            if (item.equals("..")) {
                current = current.getParentFile();
            } else {
                current = new File(current, item);
            }
        }
        return current;
    }
}
