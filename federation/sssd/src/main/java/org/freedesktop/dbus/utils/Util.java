package org.freedesktop.dbus.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing helper methods for handling strings, files and so on.
 *
 * @author hypfvieh
 * @since v3.2.5 - 2020-12-28
 */
public final class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    /** Characters used for random strings */
    private static final char[] SYMBOLS;

    static {
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch) {
          tmp.append(ch);
        }
        for (char ch = 'a'; ch <= 'z'; ++ch) {
          tmp.append(ch);
        }
        for (char ch = 'A'; ch <= 'Z'; ++ch) {
            tmp.append(ch);
        }
        SYMBOLS = tmp.toString().toCharArray();
    }

    private Util() {}

    /**
     * Trys to read a properties file.
     * Returns null if properties file could not be loaded
     * @param _file property file to read
     * @return Properties Object or null
     */
    public static Properties readProperties(File _file) {
        if (_file.exists()) {
            try {
                return readProperties(new FileInputStream(_file));
            } catch (FileNotFoundException _ex) {
                LOGGER.info("Could not load properties file: " + _file, _ex);
            }
        }
        return null;
    }

    /**
     * Tries to read a properties file from an inputstream.
     * @param _stream input stream providing property file content
     * @return properties object/null
     */
    public static Properties readProperties(InputStream _stream) {
        Properties props = new Properties();
        if (_stream == null) {
            return null;
        }

        try {
            props.load(_stream);
            return props;
        } catch (IOException | NumberFormatException _ex) {
            LOGGER.warn("Could not properties: ", _ex);
        }
        return null;
    }

    /**
     * Checks if the given String is either null or blank.
     * Blank means:<br>
     * <pre>
     * " " - true
     * "" - true
     * null - true
     * " xx" - false
     * </pre>
     * @param _str string to test
     * @return true if string is blank or null, false otherwise
     */
    public static boolean isBlank(String _str) {
        if (_str == null) {
            return true;
        }

        return _str.isBlank();
    }

    /**
     * Null-safe equals for two strings.
     *
     * @param _str1 first string
     * @param _str2 second string
     * @return true if both are equal (also true if both are null)
     */
    public static boolean strEquals(String _str1, String _str2) {
        if (_str1 == _str2) {
            return true;
        } else if (_str1 == null || _str2 == null) {
            return false;
        } else if (_str1.length() != _str2.length()) {
            return false;
        }
        return _str1.equals(_str2);
    }

    /**
     * Checks if the given String is either null or empty.
     * Blank means:<br>
     * <pre>
     * " " - false
     * "" - true
     * null - true
     * " xx" - false
     * </pre>
     * @param _str string to test
     * @return true if string is empty or null, false otherwise
     */
    public static boolean isEmpty(String _str) {
        if (_str == null) {
            return true;
        }

        return _str.isEmpty();
    }

    /**
     * Generate a simple (cryptographic insecure) random string.
     * @param _length length of random string
     * @return random string or empty string if _length &lt;= 0
     */
    public static String randomString(int _length) {
        if (_length <= 0) {
            return "";
        }
        Random random = new Random();
        char[] buf = new char[_length];
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        }
        return new String(buf);
    }

    /**
     * Upper case the first letter of the given string.
     *
     * @param _str string
     * @return uppercased string
     */
    public static String upperCaseFirstChar(String _str) {
        if (_str == null) {
            return null;
        }
        if (_str.isEmpty()) {
            return _str;
        }
        return _str.substring(0, 1).toUpperCase() + _str.substring(1);
    }

    /**
     * Converts a snake-case-string to camel case string.
     * <br>
     * Eg. this_is_snake_case &rarr; thisIsSnakeCase
     * @param _input string
     * @return camel case string or input if nothing todo. Returns null if input was null.
     */
    public static String snakeToCamelCase(String _input) {
        if (isBlank(_input)) {
            return _input;
        }

        Pattern compile = Pattern.compile("_[a-zA-Z]");
        Matcher matcher = compile.matcher(_input);

        String result = _input;

        while (matcher.find()) {
            String match = matcher.group();
            String replacement = match.replace("_", "");
            replacement = replacement.toUpperCase();

            result = result.replaceFirst(match, replacement);

        }

        return result;
    }

    /**
     * Abbreviates a String using ellipses.
     *
     * @param _str string to abbrivate
     * @param _length max length
     * @return abbreviated string, original string if string length is lower or equal then desired length or null if input was null
     */
    public static String abbreviate(String _str, int _length) {
        if (_str == null) {
            return null;
        }
        if (_str.length() <= _length) {
            return _str;
        }

        String abbr = _str.substring(0, _length - 3) + "...";

        return abbr;
    }

    /**
     * Check if the given value is a valid network port (1 - 65535).
     * @param _port 'port' to check
     * @param _allowWellKnown allow ports below 1024 (aka reserved well known ports)
     * @return true if int is a valid network port, false otherwise
     */
    public static boolean isValidNetworkPort(int _port, boolean _allowWellKnown) {
        if (_allowWellKnown) {
            return _port > 0 && _port < 65536;
        }

        return _port > 1024 && _port < 65536;
    }

    /**
     * @see #isValidNetworkPort(int, boolean)
     * @param _str string to check
     * @param _allowWellKnown allow well known port
     * @return true if valid port, false otherwise
     */
    public static boolean isValidNetworkPort(String _str, boolean _allowWellKnown) {
        if (isInteger(_str, false)) {
            return isValidNetworkPort(Integer.parseInt(_str), _allowWellKnown);
        }
        return false;
    }

    /**
     * Check if string is an either positive or negative integer.
     *
     * @param _str string to validate
     * @param _allowNegative negative integer allowed
     * @return true if integer, false otherwise
     */
    public static boolean isInteger(String _str, boolean _allowNegative) {
        if (_str == null) {
            return false;
        }

        String regex = "[0-9]+$";
        if (_allowNegative) {
            regex = "^-?" + regex;
        } else {
            regex = "^" + regex;
        }
        return _str.matches(regex);
    }

    /**
     * Reads a file to a List&lt;String&gt; (each line is one entry in list).
     * Line endings (line feed/carriage return) are NOT removed!
     *
     * @param _fileName file to read
     * @return list containing text
     */
    public static List<String> readFileToList(String _fileName) {
        List<String> localText = getTextfileFromUrl(_fileName, Charset.defaultCharset(), false);
        return localText;
    }

    /**
     * Reads a file to a String.
     * Line endings (line feed/carriage return) are NOT removed!
     *
     * @param _file file to read
     * @return String containing content, maybe null
     */
    public static String readFileToString(File _file) {
        return String.join(System.lineSeparator(), readFileToList(_file.getAbsolutePath()));
    }

    /**
     * Reads a text file from the given URL using the provided charset.
     * Using the _silent argument optionally disables all error logging.
     *
     * @param _url url providing the file to read
     * @param _charset charset to use
     * @param _silent true to not log exceptions, false otherwise
     * @return list of string or null on error
     */
    public static List<String> getTextfileFromUrl(String _url, Charset _charset, boolean _silent) {
        if (_url == null) {
            return null;
        }
        String fileUrl = _url;
        if (!fileUrl.contains("://")) {
            fileUrl = "file://" + fileUrl;
        }

        try {
            URL dlUrl;
            if (fileUrl.startsWith("file:/")) {
                dlUrl = new URL("file", "", fileUrl.replaceFirst("file:\\/{1,2}", ""));
            } else {
                dlUrl = new URL(fileUrl);
            }
            URLConnection urlConn = dlUrl.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);

            return readTextFileFromStream(urlConn.getInputStream(), _charset, _silent);

        } catch (IOException _ex) {
            if (!_silent) {
                LOGGER.warn("Error while reading file:", _ex);
            }
        }

        return null;
    }

    /**
     * Reads a text file from given {@link InputStream} using the given {@link Charset}.
     * @param _input stream to read
     * @param _charset charset to use
     * @param _silent true to disable exception logging, false otherwise
     * @return List of string or null on error
     */
    public static List<String> readTextFileFromStream(InputStream _input, Charset _charset, boolean _silent) {
        if (_input == null) {
            return null;
        }
        try {
            List<String> fileContent;
            try (BufferedReader dis = new BufferedReader(new InputStreamReader(_input, _charset))) {
                String s;
                fileContent = new ArrayList<>();
                while ((s = dis.readLine()) != null) {
                    fileContent.add(s);
                }
            }

            return !fileContent.isEmpty() ? fileContent : null;
        } catch (IOException _ex) {
            if (!_silent) {
                LOGGER.warn("Error while reading file:", _ex);
            }
        }

        return null;
    }

    /**
     * Write String to file with the given charset.
     * Optionally appends the data to the file.
     *
     * @param _fileName the file to write
     * @param _fileContent the content to write
     * @param _charset the charset to use
     * @param _append append content to file, if false file will be overwritten if existing
     *
     * @return true on successful write, false otherwise
     */
    public static boolean writeTextFile(String _fileName, String _fileContent, Charset _charset, boolean _append) {
        if (isBlank(_fileName)) {
            return false;
        }
        String allText = "";
        if (_append) {
            File file = new File(_fileName);
            if (file.exists()) {
                allText = readFileToString(file);
            }
        }
        allText += _fileContent;

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(_fileName), _charset)) {
            writer.write(allText);
        } catch (IOException _ex) {
            LOGGER.error("Could not write file to '" + _fileName + "'", _ex);
            return false;
        }

        return true;
    }

    /**
     * Gets the host name of the local machine.
     * @return host name
     */
    public static String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException _ex) {
            return null;
        }
    }

    /**
     * Checks if any of the 'needle' values are found in 'haystack'.
     *
     * @param <T> type
     * @param _haystack collection to check
     * @param _needles values to find
     *
     * @return true if any value found, false if any parameter null or no matching value found
     */
    public static <T> boolean collectionContainsAny(Collection<T> _haystack, Collection<T> _needles) {
        if (_haystack == null || _needles == null) {
            return false;
        }

        for (T t : _needles) {
            if (_haystack.contains(t)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines the current logged on user.
     * @return logged on user
     */
    public static String getCurrentUser() {
        String[] sysPropParms = new String[] {"user.name", "USER", "USERNAME"};
        for (int i = 0; i < sysPropParms.length; i++) {
            String val = System.getProperty(sysPropParms[i]);
            if (!isEmpty(val)) {
                return val;
            }
        }
        return null;
    }

    /**
     * Checks if the running OS is a MacOS/MacOS X.
     * @return true if MacOS (or MacOS X), false otherwise
     */
    public static boolean isMacOs() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.US).startsWith("mac");
    }

    public static boolean isFreeBsd() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.US).startsWith("freebsd");
    }

    /**
     * Checks if the running OS is a MS Windows OS.
     * @return true if Windows, false otherwise
     */
    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.US).startsWith("windows");
    }

    /**
     * Get the java version of the current running JRE.
     *
     * @return java major
     */
    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf('.');
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    /**
     * Create a random GUID (used for connection addresses).
     *
     * @return String
     */
    public static String genGUID() {
        Random r = new Random();
        byte[] buf = new byte[16];
        r.nextBytes(buf);
        return Hexdump.toHex(buf, false);
    }

    /**
     * Creates a unix socket address using.
     *
     * @param _listeningSocket true if the address should be used for a listing socket
     * @param _abstract true to create an abstract socket
     *
     * @return address String
     */
    public static String createDynamicSessionAddress(boolean _listeningSocket, boolean _abstract) {
        String address = "unix:";
        String path = new File(System.getProperty("java.io.tmpdir"), "dbus-XXXXXXXXXX").getAbsolutePath();
        Random r = new Random();

        do {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 10; i++) {
                sb.append((char) (Math.abs(r.nextInt()) % 26) + 65);
            }
            path = path.replaceAll("..........$", sb.toString());
            LoggerFactory.getLogger(Util.class).trace("Trying path {}", path);
        } while (new File(path).exists());

        if (_abstract) {
            address += "abstract=" + path;
        } else {
            address += "path=" + path;
        }

        if (_listeningSocket) {
            address += ",listen=true";
        }

        address += ",guid=" + Util.genGUID();

        LoggerFactory.getLogger(Util.class).debug("Created Session address: {}", address);

        return address;
    }

    /**
     * Checks if given value is greater or equal to the given minimum and less or equal to the given maximum.
     *
     * @param _check value to check
     * @param _min minimum allowed value (including)
     * @param _max maximum allowed value (including)
     *
     * @return given value if in range
     * @throws IllegalArgumentException when given value is out of range
     *
     * @since 4.2.0 - 2022-07-13
     */
    public static int checkIntInRange(int _check, int _min, int _max) {
        if (_check >= _min && _check <= _max) {
            return _check;
        }
        throw new IllegalArgumentException("Value " + _check + " is out ouf range (< " + _min + " && > " + _max + ")");
    }

    /**
     * Setup the unix socket file permissions.
     * User and group can always be set, file permissions are only set on non-windows OSes.
     *
     * @param _path path to file which where permissions should be set
     * @param _fileOwner new owner for the file
     * @param _fileGroup new group for the file
     * @param _fileUnixPermissions unix permissions to set on file
     */
    public static void setFilePermissions(Path _path, String _fileOwner, String _fileGroup, Set<PosixFilePermission> _fileUnixPermissions) {
        Objects.requireNonNull(_path, "Path required");
        UserPrincipalLookupService userPrincipalLookupService = _path.getFileSystem().getUserPrincipalLookupService();

        if (userPrincipalLookupService == null) {
            LOGGER.error("Unable to set user/group permissions on {}", _path);
        }

        if (!Util.isBlank(_fileOwner)) {
            try {
                UserPrincipal userPrincipal = userPrincipalLookupService.lookupPrincipalByName(_fileOwner);
                if (userPrincipal != null) {
                    Files.getFileAttributeView(_path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setOwner(userPrincipal);
                }
            } catch (IOException _ex) {
                LOGGER.error("Could not change owner of {} to {}", _path, _fileOwner, _ex);
            }
        }

        if (!Util.isBlank(_fileGroup)) {
            try {
                GroupPrincipal groupPrincipal = userPrincipalLookupService.lookupPrincipalByGroupName(_fileGroup);
                if (groupPrincipal != null) {
                    Files.getFileAttributeView(_path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(groupPrincipal);
                }
            } catch (IOException _ex) {
                LOGGER.error("Could not change group of {} to {}", _path, _fileGroup, _ex);
            }
        }

        if (!Util.isWindows() && _fileUnixPermissions != null) {
            try {
                Files.setPosixFilePermissions(_path, _fileUnixPermissions);
            } catch (Exception _ex) {
                LOGGER.error("Could not set file permissions of {} to {}", _path, _fileUnixPermissions, _ex);
            }
        }
    }

    /**
     * Waits for the provided supplier to return true or throws an exception.
     * <p>
     * This method will call the provided supplier every _sleepTime milliseconds to check
     * if the supplier returns true.<br>
     * If supplier returns true, method will return.
     * If no value is present after the defined _timeoutMs a {@link IllegalStateException} is thrown.
     *
     * @param _lockName name for the lock (used in exception text and logging)
     * @param _wait supplier to wait for
     * @param _timeoutMs timeout in milliseconds when wait will fail
     * @param _sleepTime sleep time between each retries
     *
     * @param <T> exception type which might be thrown
     *
     * @throws T when timeout is reached
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void waitFor(String _lockName, IThrowingSupplier<Boolean, T> _wait, long _timeoutMs, long _sleepTime) throws T {
        long waited = 0;

        boolean wait = false;
        T lastEx = null;
        do {

            try {
                lastEx = null;
                // supplier may throw, if so assume that we should still wait and retry
                wait = _wait.get();
            } catch (Throwable _ex) {
                lastEx = (T) _ex;
                wait = false;
            }

            if (waited >= _timeoutMs) {
                if (lastEx != null) { // we have a timeout and last retry has thrown a exception
                    throw lastEx;
                }
                // we have a timeout and no exception happened before
                throw new IllegalStateException(_lockName + " not available in the specified time of " + _timeoutMs + " ms");
            }
            try {
                Thread.sleep(_sleepTime);
            } catch (InterruptedException _ex) {
                LOGGER.debug("Interrupted while waiting for {}", _lockName);
                break;
            }
            waited += _sleepTime;
            LOGGER.debug("Waiting for {} to be available: {} of {} ms waited", _lockName, waited, _timeoutMs);
        } while (!wait);
    }
}
