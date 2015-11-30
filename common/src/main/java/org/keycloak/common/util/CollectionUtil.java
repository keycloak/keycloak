package org.keycloak.common.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author <a href="mailto:jeroen.rosenberg@gmail.com">Jeroen Rosenberg</a>
 */
public class CollectionUtil {

    public static String join(Collection<String> strings) {
        return join(strings, ", ");
    }

    public static String join(Collection<String> strings, String separator) {
        Iterator<String> iter = strings.iterator();
        StringBuilder sb = new StringBuilder();
        if(iter.hasNext()){
            sb.append(iter.next());
            while(iter.hasNext()){
                sb.append(separator).append(iter.next());
            }
        }
        return sb.toString();
    }

    // Return true if all items from col1 are in col2 and viceversa. Order is not taken into account
    public static <T> boolean collectionEquals(Collection<T> col1, Collection<T> col2) {
        if (col1.size() != col2.size()) {
            return false;
        }

        for (T item : col1) {
            if (!col2.contains(item)) {
                return false;
            }
        }

        return true;
    }
}
