/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.client.registration.cli.util;

import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.registration.cli.aesh.Globals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static org.keycloak.client.registration.cli.util.OsUtil.OS_ARCH;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class IoUtil {

    public static String readFileOrStdin(String file) {
        String content;
        if ("-".equals(file)) {
            content = readFully(System.in);
        } else {
            try (InputStream is = new FileInputStream(file)) {
                content = readFully(is);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("File not found: " + file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + file, e);
            }
        }
        return content;
    }

    public static void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted");
        }
    }

    public static String readSecret(String prompt, CommandInvocation invocation) {

        // TODO Windows hack - masking not working on Windows
        char maskChar = OS_ARCH.isWindows() ? 0 : '*';
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(invocation.getShell())
                .prompt(new Prompt(prompt, maskChar))
                .create();
        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .create();

        consoleBuffer.displayPrompt();

        // activate stdin
        Globals.stdin.setInputStream(System.in);

        String result;
        try {
            do {
                result = inputProcessor.parseOperation(invocation.getInput());
            } while (result == null);
        } catch (Exception e) {
            throw new RuntimeException("^C", e);
        }
        /*
        if (!Globals.stdin.isStdinAvailable()) {
            try {
                return readLine(new InputStreamReader(System.in));
            } catch (IOException e) {
                throw new RuntimeException("Standard input not available");
            }
        }
         */
        // Windows hack - get rid of any \n
        result = result.replaceAll("\\n", "");
        return result;
    }

    public static String readFully(InputStream is) {
        Charset charset = Charset.forName("utf-8");
        StringBuilder out = new StringBuilder();
        byte [] buf = new byte[8192];

        int rc;
        try {
            while ((rc = is.read(buf)) != -1) {
                out.append(new String(buf, 0, rc, charset));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read stream", e);
        }
        return out.toString();
    }

    public static void ensureFile(Path path) throws IOException {

        FileSystem fs = FileSystems.getDefault();
        Set<String> supportedViews = fs.supportedFileAttributeViews();
        Path parent = path.getParent();

        if (!isDirectory(parent)) {
            createDirectories(parent);
            // make sure only owner can read/write it
            if (supportedViews.contains("posix")) {
                setUnixPermissions(parent);
            } else if (supportedViews.contains("acl")) {
                setWindowsPermissions(parent);
            } else {
                warnErr("Failed to restrict access permissions on .keycloak directory: " + parent);
            }
        }
        if (!isRegularFile(path)) {
            createFile(path);
            // make sure only owner can read/write it
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                setUnixPermissions(path);
            } else if (supportedViews.contains("acl")) {
                setWindowsPermissions(path);
            } else {
                warnErr("Failed to restrict access permissions on config file: " + path);
            }
        }
    }

    private static void setUnixPermissions(Path path) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        if (isDirectory(path)) {
            perms.add(PosixFilePermission.OWNER_EXECUTE);
        }
        Files.setPosixFilePermissions(path, perms);
    }

    private static void setWindowsPermissions(Path path) throws IOException {
        AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
        UserPrincipal owner = view.getOwner();
        List<AclEntry> acl = view.getAcl();
        ListIterator<AclEntry> it = acl.listIterator();
        while (it.hasNext()) {
            AclEntry entry = it.next();
            if ("BUILTIN\\Administrators".equals(entry.principal().getName()) || "NT AUTHORITY\\SYSTEM".equals(entry.principal().getName())) {
                continue;
            }
            it.remove();
        }
        AclEntry entry = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(owner)
                .setPermissions(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA,
                        AclEntryPermission.APPEND_DATA, AclEntryPermission.READ_NAMED_ATTRS,
                        AclEntryPermission.WRITE_NAMED_ATTRS, AclEntryPermission.EXECUTE,
                        AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.WRITE_ATTRIBUTES,
                        AclEntryPermission.DELETE, AclEntryPermission.READ_ACL, AclEntryPermission.SYNCHRONIZE)
                .build();
        acl.add(entry);
        view.setAcl(acl);
    }

    public static void printOut(String msg) {
        System.out.println(msg);
    }

    public static void printErr(String msg) {
        System.err.println(msg);
    }

    public static void printfOut(String format, String ... params) {
        System.out.println(new Formatter().format("WARN: " + format, params));
    }

    public static void warnOut(String msg) {
        System.out.println("WARN: " + msg);
    }

    public static void warnErr(String msg) {
        System.err.println("WARN: " + msg);
    }

    public static void warnfOut(String format, String ... params) {
        System.out.println(new Formatter().format("WARN: " + format, params));
    }

    public static void warnfErr(String format, String ... params) {
        System.err.println(new Formatter().format("WARN: " + format, params));
    }

    public static void logOut(String msg) {
        System.out.println("LOG: " + msg);
    }
}
