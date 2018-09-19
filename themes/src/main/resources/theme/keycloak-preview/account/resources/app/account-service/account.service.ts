/*
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
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
 
import {Injectable} from '@angular/core';
import {Http, Response, RequestOptionsArgs} from '@angular/http';

import {KeycloakNotificationService} from '../notification/keycloak-notification.service';
import {KeycloakService} from '../keycloak-service/keycloak.service';

import {NotificationType} from 'patternfly-ng/notification';
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
@Injectable()
export class AccountServiceClient {

    private accountUrl: string;

    constructor(protected http: Http,
                protected kcSvc: KeycloakService,
                protected kcNotifySvc: KeycloakNotificationService) {
        this.accountUrl = kcSvc.authServerUrl() + 'realms/' + kcSvc.realm() + '/account';
    }
    
    public doGetRequest(endpoint: string, 
                        responseHandler: Function, 
                        options?: RequestOptionsArgs) {
        this.http.get(this.accountUrl + endpoint, options)
            .subscribe((res: Response) => responseHandler(res),
                       (error: Response) => this.handleServiceError(error));
    }
    
    public doPostRequest(endpoint: string,
                         responseHandler: Function,
                         options?: RequestOptionsArgs,
                         successMessage?: string) {
        this.http.post(this.accountUrl + endpoint, options)
            .subscribe((res: Response) => this.handleAccountUpdated(responseHandler, res, successMessage),
                       (error: Response) => this.handleServiceError(error));
    }
    
    private handleAccountUpdated(responseHandler: Function, res: Response, successMessage?: string) {
        let message: string = "Your account has been updated.";
        if (successMessage) message = successMessage;
        this.kcNotifySvc.notify(message, NotificationType.SUCCESS);
        responseHandler(res);
    } 
    
    public doDelete(endpoint: string,
                    responseHandler: Function,
                    options?: RequestOptionsArgs,
                    successMessage?: string) {
        this.http.delete(this.accountUrl + endpoint, options)
            .subscribe((res: Response) => this.handleAccountUpdated(responseHandler, res, successMessage),
                       (error: Response) => this.handleServiceError(error));
    }
    
    private handleServiceError(response: Response): void {
        console.log('**** ERROR!!!! ***');
        console.log(JSON.stringify(response));
        console.log("response.status=" + response.status);
        console.log('***************************************')
        
        if ((response.status === undefined) || (response.status === 401)) {
            this.kcSvc.logout();
            return;
        }

        if (response.status === 403) {
            // TODO: go to a forbidden page?
        }

        if (response.status === 404) {
            // TODO: route to PageNotFoundComponent
        }

        let message: string = response.status + " " + response.statusText;

        const not500Error: boolean = response.status !== 500;
        console.log('not500Error=' + not500Error);
        
        // Unfortunately, errors can be sent back in the response body as
        // 'errorMessage' or 'error_description'
        if (not500Error && response.json().hasOwnProperty('errorMessage')) {
            message = response.json().errorMessage;
        }

        if (not500Error && response.json().hasOwnProperty('error_description')) {
            message = response.json().error_description;
        }
        
        this.kcNotifySvc.notify(message, NotificationType.DANGER, response.json().params);
    }
}


