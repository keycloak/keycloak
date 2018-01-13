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
import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { platformBrowser } from '@angular/platform-browser';

import { AppModule } from './app/app.module';
//import { environment } from './environments/environment';

import { KeycloakService } from './app/keycloak-service/keycloak.service';

//if (environment.production) {
//  enableProdMode();
//}

declare const authUrl: string;
declare const resourceUrl: string;
declare const realm: string;

const noLogin: boolean = false; // convenient for development
if (noLogin) {
    platformBrowserDynamic().bootstrapModule(AppModule);
} else {
    KeycloakService.init(authUrl + '/realms/' + realm + '/account/keycloak.json',
                         {onLoad: 'login-required'})
        .then(() => {
            platformBrowserDynamic().bootstrapModule(AppModule);
        })
        .catch((e: any) => {
            console.log('Error in bootstrap: ' + JSON.stringify(e));
        });
}
