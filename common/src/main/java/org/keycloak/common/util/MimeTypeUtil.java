package org.keycloak.common.util;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MimeTypeUtil {

    private static MimetypesFileTypeMap map = new MimetypesFileTypeMap();
    static {
        map.addMimeTypes("text/css css CSS");
        map.addMimeTypes("text/javascript js JS");
        map.addMimeTypes("text/javascript js JS");
        map.addMimeTypes("image/png png PNG");
        map.addMimeTypes("image/svg+xml svg SVG");
        map.addMimeTypes("text/html html htm HTML HTM");
    }

    public static String getContentType(File file) {
        return map.getContentType(file);
    }

    public static String getContentType(String path) {
        return map.getContentType(path);
    }

}
