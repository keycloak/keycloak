package org.keycloak.storage.user;

import java.util.Optional;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * This is an optional capability interface intended to be implemented by any
 * {@link org.keycloak.storage.UserStorageProviderFactory UserStorageProviderFactory} that supports
 * testing connection to the external user storage.
 * You must implement this interface if you want to be able to test the connection within the Admin console.
 *
 * @author <a href="mailto:m.neuhaus@smf.de">Marco Neuhaus</a>
 * @version $Revision: 1 $
 */
public interface UserStorageConnectionTest {

    /**
     * Tests the connection to the external user storage using the provided configuration
     *
     * @param session Keycloak session
     * @param realm Realm model
     * @param model Component configuration model
     * @return Result object indicating success or failure of the connection test
     * @throws Exception if an error occurs during the connection test
     */
    Result testConnection(KeycloakSession session, RealmModel realm, ComponentModel model) throws Exception;

    /**
     * Result class that encapsulates the outcome of a connection test.
     * Contains information about whether the test was successful and any relevant message.
     */
    public class Result {
        private final boolean success;
        private final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        /**
         * Creates a successful test result with no message.
         *
         * @return Result indicating successful connection test
         */
        public static Result success() {
            return new Result(true, null);
        }

        /**
         * Creates a failed test result with an error message.
         *
         * @param message Description of why the connection test failed
         * @return Result indicating failed connection test
         */
        public static Result failure(String message) {
            return new Result(false, message);
        }

        /**
         * @return true if the connection test was successful, false otherwise
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * @return Optional containing an error message in case of failure, empty if test was successful
         */
        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }
    }
}
