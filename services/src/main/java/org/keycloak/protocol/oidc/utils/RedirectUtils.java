/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oidc.utils;

import org.jboss.logging.Logger;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RedirectUtils {

    private static final Logger logger = Logger.getLogger(RedirectUtils.class);

    public static String verifyRealmRedirectUri(UriInfo uriInfo, String redirectUri, RealmModel realm) {
        Set<String> validRedirects = getValidateRedirectUris(uriInfo, realm);
        return verifyRedirectUri(uriInfo, null, redirectUri, realm, validRedirects, true);
    }

    public static String verifyRedirectUri(UriInfo uriInfo, String redirectUri, RealmModel realm, ClientModel client) {
        return verifyRedirectUri(uriInfo, redirectUri, realm, client, true);
    }

    public static String verifyRedirectUri(UriInfo uriInfo, String redirectUri, RealmModel realm, ClientModel client, boolean requireRedirectUri) {
        if (client != null)
            return verifyRedirectUri(uriInfo, client.getRootUrl(), redirectUri, realm, client.getRedirectUris(), requireRedirectUri);
        return null;
    }

    public static Set<String> resolveValidRedirects(UriInfo uriInfo, String rootUrl, Set<String> validRedirects) {
        // If the valid redirect URI is relative (no scheme, host, port) then use the request's scheme, host, and port
        Set<String> resolveValidRedirects = new HashSet<String>();
        for (String validRedirect : validRedirects) {
            resolveValidRedirects.add(validRedirect); // add even relative urls.
            if (validRedirect.startsWith("/")) {
                validRedirect = relativeToAbsoluteURI(uriInfo, rootUrl, validRedirect);
                logger.debugv("replacing relative valid redirect with: {0}", validRedirect);
                resolveValidRedirects.add(validRedirect);
            }
        }
        return resolveValidRedirects;
    }

    private static Set<String> getValidateRedirectUris(UriInfo uriInfo, RealmModel realm) {
        Set<String> redirects = new HashSet<>();
        for (ClientModel client : realm.getClients()) {
            redirects.addAll(resolveValidRedirects(uriInfo, client.getRootUrl(), client.getRedirectUris()));
        }
        return redirects;
    }

    private static String verifyRedirectUri(UriInfo uriInfo, String rootUrl, String redirectUri, RealmModel realm, Set<String> validRedirects, boolean requireRedirectUri) {
        if (redirectUri == null) {
            if (!requireRedirectUri) {
                redirectUri = getSingleValidRedirectUri(validRedirects);
            }

            if (redirectUri == null) {
                logger.debug("No Redirect URI parameter specified");
                return null;
            }
        } else if (validRedirects.isEmpty()) {
            logger.debug("No Redirect URIs supplied");
            redirectUri = null;
        } else {
            redirectUri = lowerCaseHostname(redirectUri);

            String r = redirectUri;
            Set<String> resolveValidRedirects = resolveValidRedirects(uriInfo, rootUrl, validRedirects);

            boolean valid = matchesRedirects(resolveValidRedirects, r);

            if (!valid && r.startsWith(Constants.INSTALLED_APP_URL) && r.indexOf(':', Constants.INSTALLED_APP_URL.length()) >= 0) {
                int i = r.indexOf(':', Constants.INSTALLED_APP_URL.length());

                StringBuilder sb = new StringBuilder();
                sb.append(r.substring(0, i));

                i = r.indexOf('/', i);
                if (i >= 0) {
                    sb.append(r.substring(i));
                }

                r = sb.toString();

                valid = matchesRedirects(resolveValidRedirects, r);
            }
            if (valid && redirectUri.startsWith("/")) {
                redirectUri = relativeToAbsoluteURI(uriInfo, rootUrl, redirectUri);
            }
            redirectUri = valid ? redirectUri : null;
        }

        if (Constants.INSTALLED_APP_URN.equals(redirectUri)) {
            return Urls.realmInstalledAppUrnCallback(uriInfo.getBaseUri(), realm.getName()).toString();
        } else {
            return redirectUri;
        }
    }

    private static String lowerCaseHostname(String redirectUri) {
        int n = redirectUri.indexOf('/', 7);
        if (n == -1) {
            return redirectUri.toLowerCase();
        } else {
            return redirectUri.substring(0, n).toLowerCase() + redirectUri.substring(n);
        }
    }

    private static String relativeToAbsoluteURI(UriInfo uriInfo, String rootUrl, String relative) {
        if (rootUrl == null || rootUrl.isEmpty()) {
            rootUrl = UriUtils.getOrigin(uriInfo.getBaseUri());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(rootUrl);
        sb.append(relative);
        return sb.toString();
    }

    private static boolean matchesRedirects(Set<String> validRedirects, String redirect) {
        for (String validRedirect : validRedirects) {
            if (validRedirect.endsWith("*") && !validRedirect.contains("?")) {
                // strip off the query component - we don't check them when wildcards are effective
                String r = redirect.contains("?") ? redirect.substring(0, redirect.indexOf("?")) : redirect;
                // strip off *
                int length = validRedirect.length() - 1;
                validRedirect = validRedirect.substring(0, length);
                if (r.startsWith(validRedirect)) return true;
                // strip off trailing '/'
                if (length - 1 > 0 && validRedirect.charAt(length - 1) == '/') length--;
                validRedirect = validRedirect.substring(0, length);
                if (validRedirect.equals(r)) return true;
                if (validRedirect.contains("*") && !validRedirect.equals("*")) {
                    String vr = replaceWildCards(r, validRedirect);                   
                    if(r.equals(vr) || r.startsWith(vr)) return true;                   
                }
            } else if (validRedirect.equals(redirect)) return true;
        }
        return false;
    }

    private static String getSingleValidRedirectUri(Collection<String> validRedirects) {
        if (validRedirects.size() != 1) return null;
        String validRedirect = validRedirects.iterator().next();
        int idx = validRedirect.indexOf("/*");
        if (idx > -1) {
            validRedirect = validRedirect.substring(0, idx);
        }
        return validRedirect;
    }
    
    private static String replaceWildCards(String r, String vr) {
        /*
         * Keep replacing the "*" in valid redirect with the sub strings in redirect uri
         * and finally check if it matches with redirect uri or if redirect uri
         * starts with that string.
         */
        try {
            //Loop till all "*" is replaced 
            while(vr.contains("*")) {
                //Split for first two occurences of "*"
                String[] splitVR = vr.split("\\*", 3);
                //If redirect uri contains the firt 2 values of split array, continue wiht logic
                if (r.contains(splitVR[0]) && r.contains(splitVR[1])) {
                    String wildCardRep = null;
                    //If first part of split array is empty, replace * with
                    //sub string of redirect uri from index 0 to index of second part
                    //of split array
                    if (splitVR[0].isEmpty()) {
                        wildCardRep = r.substring(0, r.indexOf(splitVR[1])); 
                    } else if (splitVR[1].isEmpty()) {
                        //If second part of split array is empty, repalce *
                        // with sub string of redirect uri from index of
                        //second part of split array to length of redirect uri
                        wildCardRep = r.substring(splitVR[0].length(), r.length()); 
                        
                    } else {
                        //repalce * with sub string of redirect uri from index of
                        //first part of split array to index of second part
                        //of split array
                        wildCardRep = r.substring(splitVR[0].length(), r.indexOf(splitVR[1])); 
                    }
                    if (wildCardRep != null) {
                        //Repalce the * with the sub string
                        vr = vr.replaceFirst("\\*", wildCardRep);
                    }
                } else {
                    break;
                }               
           }
        } catch(Exception e) {
            logger.error("Error while comparing with wildcard redirect uri", e);
        }
        
        return vr;
    }
}
