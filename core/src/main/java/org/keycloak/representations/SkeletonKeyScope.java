package org.keycloak.representations;


import org.keycloak.util.MultivaluedHashMap;

/**
 * Key is resource desired.  Values are roles desired for that resource
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeyScope extends MultivaluedHashMap<String, String> {
}
