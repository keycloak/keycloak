/*
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import {KeycloakService} from '../keycloak-service/keycloak.service';
import {ContentAlert} from '../content/ContentAlert';

declare const baseUrl: string;

type ConfigResolve = (config: RequestInit) => void;

export interface HttpResponse<T = unknown> extends Response {
    data?: T;
}

export interface RequestInitWithParams extends RequestInit {
    params?: {[name: string]: string | number};
}

export class AccountServiceError extends Error {
    constructor(public response: HttpResponse) {
        super(response.statusText);
    }
}

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class AccountServiceClient {
    private kcSvc: KeycloakService;
    private accountUrl: string;

    public constructor(keycloakService: KeycloakService) {
        this.kcSvc = keycloakService;
        this.accountUrl = this.kcSvc.authServerUrl() + 'realms/' + this.kcSvc.realm() + '/account';
    }

    public async doGet<T>(endpoint: string,
                          config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        return this.doRequest(endpoint, {...config, method: 'get'});
    }

    public async doDelete<T>(endpoint: string,
                            config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        return this.doRequest(endpoint, {...config, method: 'delete'});
    }

    public async doPost<T>(endpoint: string,
                          body: string | {},
                          config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        return this.doRequest(endpoint, {...config, body: JSON.stringify(body), method: 'post'});
    }

    public async doPut<T>(endpoint: string,
                         body: string | {},
                         config?: RequestInitWithParams): Promise<HttpResponse<T>> {
        return this.doRequest(endpoint, {...config, body: JSON.stringify(body), method: 'put'});
    }

    public async doRequest<T>(endpoint: string,
                              config?: RequestInitWithParams): Promise<HttpResponse<T>> {

        const response: HttpResponse<T> = await fetch(this.makeUrl(endpoint, config).toString(),
                                                      await this.makeConfig(config));

        try {
            response.data = await response.json();
        } catch (e) {} // ignore.  Might be empty

        if (!response.ok) {
            this.handleError(response);
            throw new AccountServiceError(response);
        }

        return response;
    }

    private handleError(response: HttpResponse): void {
        if (response !== null && response.status === 401) {
            if (this.kcSvc.authenticated() && !this.kcSvc.audiencePresent()) {
                // authenticated and the audience is not present => not allowed
                window.location.href = baseUrl + '#/forbidden';
            } else {
                // session timed out?
                this.kcSvc.login();
            }
        }

        if (response !== null && response.status === 403) {
            window.location.href = baseUrl + '#/forbidden';
        }

        if (response !== null && response.data != null) {
            if (response.data['errors'] != null) {
                for(let err of response.data['errors'])
                    ContentAlert.danger(err['errorMessage'], err['params']);
            } else {
                ContentAlert.danger(
                `${response.statusText}: ${response.data['errorMessage'] ? response.data['errorMessage'] : ''} ${response.data['error'] ? response.data['error'] : ''}`);
            };
        } else {
            ContentAlert.danger(response.statusText);
        }
    }

    private makeUrl(endpoint: string, config?: RequestInitWithParams): URL {
        if (endpoint.startsWith('http')) return new URL(endpoint);
        const url = new URL(this.accountUrl + endpoint);

        // add request params
        if (config && config.hasOwnProperty('params')) {
            const params: {[name: string]: string} = config.params as {} || {};
            Object.keys(params).forEach(key => url.searchParams.append(key, params[key]))
        }

        return url;
    }

    private makeConfig(config: RequestInit = {}): Promise<RequestInit> {
        return new Promise( (resolve: ConfigResolve) => {
            this.kcSvc.getToken()
                .then( (token: string) => {
                    resolve( {
                        ...config,
                        headers: {'Content-Type': 'application/json',
                                 ...config.headers,
                                  Authorization: 'Bearer ' + token}
                    });
                }).catch(() => {
                    this.kcSvc.login();
                });
        });
    }

}

window.addEventListener("unhandledrejection", (event: PromiseRejectionEvent) => {
    event.promise.catch(error => {
        if (error instanceof AccountServiceError) {
            // We already handled the error. Ignore unhandled rejection.
            event.preventDefault();
        }
    });
});
