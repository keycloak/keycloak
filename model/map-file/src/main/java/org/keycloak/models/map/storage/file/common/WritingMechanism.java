package org.keycloak.models.map.storage.file.common;

/**
 * Class implementing this interface defines mechanism for writing basic structures: primitive types,
 * sequences and maps.
 */
public interface WritingMechanism {

    /**
     * Writes a value of a primitive type ({@code null}, boolean, number, String).
     * @param value
     * @return
     */
    WritingMechanism writeObject(Object value);

    /**
     * Writes a sequence, items of which are written using this mechanism in the {@code task}.
     * @param task
     * @return
     */
    WritingMechanism writeSequence(Runnable task);

    /**
     * Writes a mapping, items of which are written using this mechanism in the {@code task}.
     * @param task
     * @return
     */
    WritingMechanism writeMapping(Runnable task);

    /**
     * Writes a mapping key/value pair, items of which are written using this mechanism in the {@code task}.
     * @param valueTask
     * @return
     */
    WritingMechanism writePair(String key, Runnable valueTask);


}
