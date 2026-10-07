package org.keycloak.misc.vulns;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class YamlUtil {

    private static final YAMLMapper ym = YAMLMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build();

    public static <T> T read(File src, Class<T> valueType) throws IOException {
        return ym.readValue(src, valueType);
    }

    public static <T> T read(InputStream src, Class<T> valueType) throws IOException {
        return ym.readValue(src, valueType);
    }

}
