package org.keycloak.services.models.nosql.impl;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Utils {

    private Utils() {};

    /**
     * Add item to the end of array
     *
     * @param inputArray could be null. In this case, method will return array of length 1 with added item
     * @param item must be not-null
     * @return array with added item to the end
     */
    public static <T> T[] addItemToArray(T[] inputArray, T item) {
        if (item == null) {
            throw new IllegalArgumentException("item must be non-null");
        }

        T[] outputArray;
        if (inputArray == null) {
            outputArray = (T[])Array.newInstance(item.getClass(), 1);
        } else {
            outputArray = Arrays.copyOf(inputArray, inputArray.length + 1);
        }
        outputArray[outputArray.length - 1] = item;
        return outputArray;
    }

    /**
     * Return true if array contains specified item
     * @param array could be null (In this case method always return false)
     * @param item can't be null
     * @return
     */
    public static boolean contains(Object[] array, Object item) {
        if (item == null) {
            throw new IllegalArgumentException("item must be non-null");
        }

        if (array != null) {
            for (Object current : array) {
                if (item.equals(current)) {
                    return true;
                }
            }
        }
        return false;
    }

}
