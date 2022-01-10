/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

declare const baseUrl: string;
declare const referrer: string;
declare const referrerUri: string;

/**
 * Create a redirect uri that can return to this application with referrer and referrer_uri intact.
 * 
 * @param currentLocation The ReactRouter location to return to.
 *  
 * @author Stan Silvert
 */
export const createRedirect = (currentLocation: string): string => {
    let redirectUri: string = baseUrl;
    
    if (typeof referrer !== 'undefined') {
        // '_hash_' is a workaround for when uri encoding is not
        // sufficient to escape the # character properly.
        // The problem is that both the redirect and the application URL contain a hash.
        // The browser will consider anything after the first hash to be client-side.  So
        // it sees the hash in the redirect param and stops.
        redirectUri += "?referrer=" + referrer + "&referrer_uri=" + referrerUri.replace('#', '_hash_');
    }

    return encodeURIComponent(redirectUri) + encodeURIComponent("/#" + currentLocation);    
}