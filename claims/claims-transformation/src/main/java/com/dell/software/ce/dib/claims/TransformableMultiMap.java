package com.dell.software.ce.dib.claims;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.map.MultiValueMap;
import org.keycloak.util.MultivaluedHashMap;

import java.util.List;
import java.util.Map;

/**
 * Creates a map that can contain multiple values per key and transform both keys and/or values.
 * @param <K>  the key type
 * @param <V>  the value type
 */
public class TransformableMultiMap<K extends Comparable, V> extends MultiValueMap<K, V> {
    /** The transformer to use for the key */
    protected final Transformer<? super K, ? extends K> keyTransformer;
    /** The transformer to use for the value */
    protected final Transformer<? super V, ? extends V> valueTransformer;

    /**
     * Specifies the transforms to use to transform specified keys. Does not transform values.
     * @param transforms Map of key transforms to use. The key in the map is used to specify the original key to be transformed. The value in the map is what to transform it to.
     */
    public TransformableMultiMap(final Map<K, K> transforms) {
        this(new KeyTransformer(transforms), null);
    }

    public TransformableMultiMap(final List<ClaimMapping<K>> transforms) {
        this(new KeyTransformer(transforms), null);
    }

    /**
     * Specifies the key and value transformers to use.
     * @param keyTransformer
     * @param valueTransformer
     */
    public TransformableMultiMap(final Transformer<? super K, ? extends K> keyTransformer, final Transformer<? super V, ? extends V> valueTransformer) {
        this.keyTransformer = keyTransformer;
        this.valueTransformer = valueTransformer;
    }

    public void transformMap() {
        transformMap(false);
    }

    /**
     * Transforms the current values using the transformers specified at creation time.
     * @param appendResults If true the original key/values will still exist after a key transformation transformation. Value transformations are not appended currently because our value part of the Map doesn't require
     *                      that it implements comparable.
     */
    public void transformMap(final boolean appendResults) {
        if (this.isEmpty()) {
            return;
        }

        final MultiValueMap<K, V> result = new MultiValueMap<>();

        for (final Map.Entry<K, Object> entry : this.entrySet()) {
            List<V> values = (List<V>)entry.getValue();
            K transformedKey = transformKey(entry.getKey());

            for(V item : values) {
                V transformedValue = transformValue(item);
                result.put(transformedKey, transformedValue);

                //If we want the original results as well than we need to add the original key and items
                //we only want to add it if the transformedKey and un-transformed key are different
                if(transformedKey.compareTo(entry.getKey()) != 0 && appendResults) {
                    result.put(entry.getKey(), item);
                }
            }
        }

        this.clear();
        this.putAll(result);
    }

    /**
     * Merges a second MultiMap with this map. Generic types must be the same.
     * @param map MultiMap to merge.
     */
    public void mergeMap(final MultiMap<K, V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        for (final Map.Entry<K, Object> entry : map.entrySet()) {
            List<V> values = (List<V>)entry.getValue();

            for(V item : values) {
                this.put(entry.getKey(), item);
            }
        }
    }

    public void mergeMap(final MultivaluedHashMap<K, V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        for (final Map.Entry<K, List<V>> entry : map.entrySet()) {
            List<V> values = entry.getValue();

            for(V item : values) {
                this.put(entry.getKey(), item);
            }
        }
    }
    /**
     * Transforms a key.
     * <p>
     * The transformer itself may throw an exception if necessary.
     *
     * @param object  the object to transform
     * @return the transformed object
     */
    protected K transformKey(final K object) {
        if (keyTransformer == null) {
            return object;
        }
        return keyTransformer.transform(object);
    }

    /**
     * Transforms a value.
     * <p>
     * The transformer itself may throw an exception if necessary.
     *
     * @param object  the object to transform
     * @return the transformed object
     */
    protected V transformValue(final V object) {
        if (valueTransformer == null) {
            return object;
        }
        return valueTransformer.transform(object);
    }
}