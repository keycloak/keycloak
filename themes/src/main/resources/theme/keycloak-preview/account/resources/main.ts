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

import { KeycloakService, KeycloakClient } from './app/keycloak-service/keycloak.service';


//if (environment.production) {
//  enableProdMode();
//}

declare const authUrl: string;
declare const resourceUrl: string;
declare const realm: string;
declare const keycloak: KeycloakClient;

KeycloakService.setKeycloakAuth(keycloak);

loadCss('/styles.css');
platformBrowserDynamic().bootstrapModule(AppModule);

function loadCss(url:string) {
    const link = document.createElement("link");
    link.href = resourceUrl + url;
    link.rel = "stylesheet";
    link.media = "screen, print";
    document.head.appendChild(link);
}
