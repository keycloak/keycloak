package org.keycloak.ssf.transmitter.subject;

public enum SubjectManagementResult {
    OK,
    STREAM_NOT_FOUND,
    FORMAT_UNSUPPORTED,
    SUBJECT_NOT_FOUND,
    /**
     * The resolved subject is backed by a read-only user store
     * (e.g. an LDAP federation with edit mode {@code READ_ONLY}, or
     * with import disabled) so the {@code ssf.notify.<clientId>}
     * subscription state cannot be persisted. Surfaces as a clean
     * transmitter limitation rather than an unhandled 500. Persisting
     * per-user subscription state requires writable user storage
     * (e.g. LDAP edit mode {@code UNSYNCED} with import enabled,
     * or a LDAP attribute mapper that returns a proper
     * {@code ssf.notify.<client_id>=true} attribute).
     */
    SUBJECT_READ_ONLY
}
