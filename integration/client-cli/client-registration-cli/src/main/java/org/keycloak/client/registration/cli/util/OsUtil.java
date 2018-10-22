package org.keycloak.client.registration.cli.util;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class OsUtil {

    public static final OsArch OS_ARCH = determineOSAndArch();

    public static final String CMD = OS_ARCH.isWindows() ? "kcreg.bat" : "kcreg.sh";

    public static final String PROMPT = OS_ARCH.isWindows() ? "c:\\>" : "$";

    public static final String EOL = OS_ARCH.isWindows() ? "\r\n" : "\n";


    public static OsArch determineOSAndArch() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");

        if (arch.equals("amd64")) {
            arch = "x86_64";
        }

        if (os.startsWith("linux")) {
            if (arch.equals("x86") || arch.equals("i386") || arch.equals("i586")) {
                arch = "i686";
            }
            return new OsArch("linux", arch);
        } else if (os.startsWith("windows")) {
            if (arch.equals("x86")) {
                arch = "i386";
            }
            if (os.indexOf("2008") != -1 || os.indexOf("2003") != -1 || os.indexOf("vista") != -1) {
                return new OsArch("win32", arch, true);
            } else {
                return new OsArch("win32", arch);
            }
        } else if (os.startsWith("sunos")) {
            return new OsArch("sunos5", "x86_64");
        } else if (os.startsWith("mac os x")) {
            return new OsArch("osx", "x86_64");
        } else if (os.startsWith("freebsd")) {
            return new OsArch("freebsd", arch);
        }

        // unsupported platform
        throw new RuntimeException("Could not determine OS and architecture for this operating system: " + os);
    }
}
