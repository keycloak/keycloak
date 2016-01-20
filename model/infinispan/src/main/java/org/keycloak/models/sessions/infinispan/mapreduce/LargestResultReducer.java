package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Reducer;

import java.util.Iterator;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LargestResultReducer implements Reducer<String, Integer> {

    @Override
    public Integer reduce(String reducedKey, Iterator<Integer> itr) {
        Integer largest = itr.next();
        while (itr.hasNext()) {
            Integer next = itr.next();
            if (next > largest) {
                largest = next;
            }
        }
        return largest;
    }

}
