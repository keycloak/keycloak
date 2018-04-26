package org.keycloak.performance.iteration;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.Validate;
import org.keycloak.performance.util.Loggable;

/**
 *
 * @author tkyjovsk
 */
public class ListOfLists<E> extends AbstractList<E> implements Loggable {

    private final List<List<E>> listOfLists = new LinkedList<>();

    public ListOfLists(List<List<E>> listOfLists) {
        this.listOfLists.addAll(listOfLists);
    }

    public ListOfLists(List<E>... lists) {
        this(Arrays.asList(lists));
    }

    @Override
    public E get(int index) {
        E e = null;
        int rIndex = index;
        for (List<E> l : listOfLists) {
            int s = l.size();
            if (s > rIndex) {
                e = l.get(rIndex);
                break;
            } else {
                rIndex -= s;
            }
        }
        Validate.notNull(e);
        return e;
    }

    @Override
    public int size() {
        return listOfLists.stream().mapToInt(List::size).sum();
    }

}
