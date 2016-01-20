package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Reducer;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FirstResultReducer implements Reducer<Object, Object>, Serializable {

    @Override
    public Object reduce(Object reducedKey, Iterator<Object> itr) {
        return itr.next();
    }

}
