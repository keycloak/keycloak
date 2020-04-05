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

//import {KeycloakNotificationService} from '../notification/keycloak-notification.service';
import {KeycloakService} from '../keycloak-service/keycloak.service';
import Axios, {AxiosRequestConfig, AxiosResponse, AxiosError} from 'axios';
import {ContentAlert} from '../content/ContentAlert';

//import {NotificationType} from 'patternfly-ng/notification';*/

type AxiosResolve = (response: AxiosResponse) => void;
type ConfigResolve = (config: AxiosRequestConfig) => void;
type ErrorReject = (error: Error) => void;

 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class AccountServiceClient {
    private static instance: AccountServiceClient = new AccountServiceClient();

    private kcSvc: KeycloakService = KeycloakService.Instance;
    private accountUrl: string = this.kcSvc.authServerUrl() + 'realms/' + this.kcSvc.realm() + '/account';

    private constructor() {}

    public static get Instance(): AccountServiceClient  {
        return AccountServiceClient.instance;
    }

    public doGet(endpoint: string,
                config?: AxiosRequestConfig): Promise<AxiosResponse> {
        return this.doRequest(endpoint, {...config, method: 'get'});
    }

    public doDelete(endpoint: string,
            config?: AxiosRequestConfig): Promise<AxiosResponse> {
        return this.doRequest(endpoint, {...config, method: 'delete'});
    }

    public doPut(endpoint: string,
                config?: AxiosRequestConfig): Promise<AxiosResponse> {
        return this.doRequest(endpoint, {...config,
                                         method: 'put',
                                         headers: {'Content-Type': 'application/json'}
                                        }
        );
    }

    public doPost(endpoint: string,
                config?: AxiosRequestConfig): Promise<AxiosResponse> {
        return this.doRequest(endpoint, {...config, method: 'post'});
    }

    public doRequest(endpoint: string,
                     config?: AxiosRequestConfig): Promise<AxiosResponse> {

        return new Promise((resolve: AxiosResolve, reject: ErrorReject) => {
            this.makeConfig(endpoint, config)
                .then((config: AxiosRequestConfig) => {
                    console.log({config});
                    this.axiosRequest(config, resolve, reject);
                }).catch( (error: AxiosError) => {
                    this.handleError(error);
                    reject(error);
                });
        });
    }

    private axiosRequest(config: AxiosRequestConfig,
                         resolve: AxiosResolve,
                         reject: ErrorReject): void {
        Axios.request(config)
            .then((response: AxiosResponse) => {
                 resolve(response);
            })
            .catch((error: AxiosError) => {
                this.handleError(error);
                reject(error);
            });
    }

    private handleError(error: AxiosError): void {
        if (error != null && error.response != null && error.response.status === 401) {
            // session timed out?
            this.kcSvc.login();
        }
        console.log(error);

        if (error != null && error.response != null && error.response.data != null && error.response.data.errorMessage) {
            ContentAlert.danger(error.response.data.errorMessage);
        } else {
            ContentAlert.danger(error.name + ': ' + error.message);
        }
    }

    private makeConfig(endpoint: string, config: AxiosRequestConfig = {}): Promise<AxiosRequestConfig> {
        return new Promise( (resolve: ConfigResolve) => {
            this.kcSvc.getToken()
                .then( (token: string) => {
                    resolve( {
                        ...config,
                        baseURL: this.accountUrl,
                        url: endpoint,
                        headers: {...config.headers, Authorization: 'Bearer ' + token}
                    });
                }).catch(() => {
                    this.kcSvc.login();
                });
        });
    }
}
