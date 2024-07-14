package org.keycloak.test.framework.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class FileConfigSource implements ConfigSource {

    private static final Map<String, String> configuration = new HashMap<>();

    static {
        Properties p = new Properties();
        String configFilePath = System.getProperty("kc.test.config");
        File fileConfig = new File(configFilePath);

        try(FileInputStream inputStream = new FileInputStream(fileConfig)) {
            p.load(inputStream);
            p.forEach((k, v) -> configuration.put(k.toString(), v.toString()));
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrdinal() {
        return 500;
    }

    @Override
    public Map<String, String> getProperties() {
        return configuration;
    }

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(String property) {
        return configuration.get(property);
    }

    @Override
    public String getName() {
        return FileConfigSource.class.getSimpleName();
    }

}
