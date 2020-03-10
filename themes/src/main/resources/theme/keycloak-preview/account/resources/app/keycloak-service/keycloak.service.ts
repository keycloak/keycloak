/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import {KeycloakLoginOptions} from './keycloak.d';

// keycloak.js downloaded in index.ftl
declare function Keycloak(config?: string|{}): Keycloak.KeycloakInstance;

export type KeycloakClient = Keycloak.KeycloakInstance;
type InitOptions = Keycloak.KeycloakInitOptions;

declare const keycloak: KeycloakClient;

class KeycloakService {

    public authenticated(): boolean {
        return keycloak.authenticated ? keycloak.authenticated : false;
    }

    public login(options?: KeycloakLoginOptions): void {
        keycloak.login(options);
    }

    public logout(redirectUri?: string): void {
        keycloak.logout({redirectUri: redirectUri});
    }

    public account(): void {
        keycloak.accountManagement();
    }
    
    public authServerUrl(): string | undefined {
        const authServerUrl = keycloak.authServerUrl;
        return authServerUrl!.charAt(authServerUrl!.length - 1) === '/' ? authServerUrl : authServerUrl + '/';
    }
    
    public realm(): string | undefined {
        return keycloak.realm;
    }

    public get token(): Promise<string> {
        return new Promise<string>((resolve, reject) => {
            if (keycloak.token) {
                keycloak
                    .updateToken(5)
                    .success(() => {
                        resolve(keycloak.token as string);
                    })
                    .error(() => {
                        reject('Failed to refresh token');
                    });
            } else {
                reject('Not logged in');
            }
        });
    }
}

export default new KeycloakService();