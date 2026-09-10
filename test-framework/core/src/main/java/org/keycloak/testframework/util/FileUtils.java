package org.keycloak.testframework.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    public static long readLongFromFile(File file) {
        return Long.parseLong(readStringFromFile(file));
    }

    public static String readStringFromFile(File file) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeToFile(File file, long value) {
        writeToFile(file, Long.toString(value));
    }

    public static void writeToFile(File file, String value) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(file, value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(File file) {
        if (file.isFile()) {
            if (!file.delete()) {
                throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
            }
        } else if (file.isDirectory()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete directory: " + e);
            }
        }

    }

}
