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
import {KeycloakLoginOptions, KeycloakError} from "../../../../../../../../../../adapters/oidc/js/src/main/resources/keycloak";

// keycloak.js downloaded in index.ftl
declare function Keycloak(config?: string|{}): Keycloak.KeycloakInstance;

export type KeycloakClient = Keycloak.KeycloakInstance;
type InitOptions = Keycloak.KeycloakInitOptions;

declare const keycloak: KeycloakClient;

export class KeycloakService {
    private static keycloakAuth: KeycloakClient = keycloak;
    private static instance: KeycloakService = new KeycloakService();

    private constructor() {
        
    }
    
    public static get Instance(): KeycloakService  {
        return this.instance;
    }
    
    /**
     * Configure and initialize the Keycloak adapter.
     *
     * @param configOptions Optionally, a path to keycloak.json, or an object containing
     *                      url, realm, and clientId.
     * @param adapterOptions Optional initiaization options.  See javascript adapter docs
     *                       for details.
     * @returns {Promise<T>}
     */
    public static init(configOptions?: string|{}, initOptions: InitOptions = {}): Promise<void> {
        KeycloakService.keycloakAuth = Keycloak(configOptions);

        return new Promise((resolve, reject) => {
            KeycloakService.keycloakAuth.init(initOptions)
                .success(() => {
                    resolve();
                })
                .error((errorData: KeycloakError) => {
                    reject(errorData);
                });
        });
    }
    
    public authenticated(): boolean {
        return KeycloakService.keycloakAuth.authenticated ? KeycloakService.keycloakAuth.authenticated : false;
    }

    public login(options?: KeycloakLoginOptions): void {
        KeycloakService.keycloakAuth.login(options);
    }

    public logout(redirectUri?: string): void {
        KeycloakService.keycloakAuth.logout({redirectUri: redirectUri});
    }

    public account(): void {
        KeycloakService.keycloakAuth.accountManagement();
    }
    
    public authServerUrl(): string | undefined {
        const authServerUrl = KeycloakService.keycloakAuth.authServerUrl;
        return authServerUrl!.charAt(authServerUrl!.length - 1) === '/' ?  authServerUrl : authServerUrl + '/';
    }
    
    public realm(): string | undefined {
        return KeycloakService.keycloakAuth.realm;
    }

    public getToken(): Promise<string> {
        return new Promise<string>((resolve, reject) => {
            if (KeycloakService.keycloakAuth.token) {
                KeycloakService.keycloakAuth
                    .updateToken(5)
                    .success(() => {
                        resolve(KeycloakService.keycloakAuth.token as string);
                    })
                    .error(() => {
                        reject('Failed to refresh token');
                    });
            } else {
                reject('Not loggen in');
            }
        });
    }
}
