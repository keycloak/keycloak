package org.keycloak.client.registration.cli.config;

import org.keycloak.client.registration.cli.util.IoUtil;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.keycloak.client.registration.cli.util.IoUtil.printErr;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FileConfigHandler implements ConfigHandler {

    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static String configFile;

    public static void setConfigFile(String filename) {
        configFile = filename;
    }

    public static String getConfigFile() {
        return configFile;
    }

    public ConfigData loadConfig() {
        // for now just dumb impl ignoring file locks for read
        File file = new File(configFile);
        if (!file.isFile() || file.length() == 0) {
            return new ConfigData();
        }

        try {
            try (FileInputStream is = new FileInputStream(configFile)) {
                return JsonSerialization.readValue(is, ConfigData.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + configFile, e);
        }
    }

    public static void ensureFile() {
        Path path = null;
        try {
            path = Paths.get(new File(configFile).getAbsolutePath());
            IoUtil.ensureFile(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create config file: " + path, e);
        }
    }

    public void saveMergeConfig(ConfigUpdateOperation op) {
        try {
            ensureFile();

            try (RandomAccessFile file = new RandomAccessFile(new File(configFile), "rw")) {
                FileChannel fileChannel = file.getChannel();

                FileLock fileLock = null;

                // lock file for write
                int tryCount = 0;
                do try {
                    fileLock = fileChannel.tryLock();
                    break;
                } catch (OverlappingFileLockException e) {
                    // sleep a little, and try again
                    try {
                        Thread.sleep(100);
                        continue;
                    } catch (InterruptedException e1) {
                        throw new RuntimeException("Interrupted");
                    }
                } while (tryCount++ < 10);

                if (fileLock != null) {
                    try {
                        // load config from file
                        ConfigData config = new ConfigData();
                        long size = file.length();
                        if (size > MAX_SIZE) {
                            printErr("Config file " + configFile + " is too big. It will be overwritten.");
                            file.setLength(0);
                        } else if (size > 0){
                            byte[] buf = new byte[(int) size];
                            file.readFully(buf);
                            config = JsonSerialization.readValue(new ByteArrayInputStream(buf), ConfigData.class);
                        }

                        // update loaded config
                        op.update(config);

                        // save config to file
                        byte [] content = JsonSerialization.writeValueAsPrettyString(config).getBytes("utf-8");
                        file.seek(0);
                        file.write(content);
                        file.setLength(content.length);

                    } finally {
                        fileLock.release();
                    }
                } else {
                    throw new RuntimeException("Failed to get lock on " + configFile);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + configFile, e);
        }
    }
}
