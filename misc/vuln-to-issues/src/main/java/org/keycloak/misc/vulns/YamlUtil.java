package org.keycloak.misc.vulns;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;

public class YamlUtil {

    private static final YAMLMapper ym = new YAMLMapper();

    public static <T> T read(File src, Class<T> valueType) throws IOException {
        return ym.readValue(src, valueType);
    }

}
