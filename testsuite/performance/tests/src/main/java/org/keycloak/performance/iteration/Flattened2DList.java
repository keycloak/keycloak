package org.keycloak.performance.iteration;

import java.util.AbstractList;
import java.util.List;

/**
 * 2D list of lists of the same size represented as a single list.
 * Useful for proxying lists of nested entities for example client roles of clients.
 * 
 * @author tkyjovsk
 * @param <XT> type of X-list items
 * @param <YT> type of Y-list items
 */
public abstract class Flattened2DList<XT, YT> extends AbstractList<YT> {

    public abstract List<XT> getXList();

    @Override
    public int size() {
        return getXList().size() * getYListSize();
    }

    @Override
    public YT get(int index) {
        int x = index % getXList().size();
        int y = index / getXList().size();
        return getYList(getXList().get(x)).get(y);
    }

    public abstract List<YT> getYList(XT xList);

    public abstract int getYListSize();

}
