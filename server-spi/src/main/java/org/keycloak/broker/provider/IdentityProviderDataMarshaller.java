package org.keycloak.broker.provider;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface IdentityProviderDataMarshaller {

    String serialize(Object obj);
    <T> T deserialize(String serialized, Class<T> clazz);

}
