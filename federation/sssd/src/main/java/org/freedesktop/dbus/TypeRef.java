package org.freedesktop.dbus;

/**
 * Interface for retrieving generic type information with reflection.
 * <p>
 * Examples:
 * <pre>{@code
 * public interface ListOfStrings extends TypeRef<List<String>> {
 * }
 * } </pre>
 *
 * @param <T> generic type
 */
public interface TypeRef<T> {
}
