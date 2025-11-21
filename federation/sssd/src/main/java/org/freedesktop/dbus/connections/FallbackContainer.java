package org.freedesktop.dbus.connections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.utils.LoggingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FallbackContainer {

    private final Logger                        logger    = LoggerFactory.getLogger(getClass());
    private final Map<String[], ExportedObject> fallbacks = new HashMap<>();

    FallbackContainer() {
    }

    public synchronized void add(String _path, ExportedObject _eo) {
        logger.debug("Adding fallback on {} of {}", _path, _eo);
        fallbacks.put(_path.split("/"), _eo);
    }

    public synchronized void remove(String _path) {
        logger.debug("Removing fallback on {}", _path);
        fallbacks.remove(_path.split("/"));
    }

    public synchronized ExportedObject get(String _path) {
        int best = 0;
        ExportedObject bestobject = null;
        String[] pathel = _path.split("/");
        for (Map.Entry<String[], ExportedObject> entry : fallbacks.entrySet()) {
            String[] fbpath = entry.getKey();

            LoggingHelper.logIf(logger.isTraceEnabled(), () ->
                logger.trace("Trying fallback path {} to match {}",
                        Arrays.deepToString(fbpath),
                        Arrays.deepToString(pathel))
            );

            int i;
            for (i = 0; i < pathel.length && i < fbpath.length; i++) {
                if (!pathel[i].equals(fbpath[i])) {
                    break;
                }
            }
            if (i > 0 && i == fbpath.length && i > best) {
                bestobject = entry.getValue();
            }
            logger.trace("Matches {} bestobject now {}", i, bestobject);
        }

        logger.debug("Found fallback for {} of {}", _path, bestobject);
        return bestobject;
    }
}
