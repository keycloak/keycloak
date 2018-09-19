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
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { RouterModule, Routes } from '@angular/router';
import { LocationStrategy, HashLocationStrategy } from '@angular/common';

import { TranslateLoader } from '@ngx-translate/core';
import { TranslateModule } from '@ngx-translate/core';

import { KeycloakService } from './keycloak-service/keycloak.service';
import { KEYCLOAK_HTTP_PROVIDER } from './keycloak-service/keycloak.http';
import {KeycloakGuard} from './keycloak-service/keycloak.guard';

import {ResponsivenessService} from './responsiveness-service/responsiveness.service';
import {KeycloakNotificationService} from './notification/keycloak-notification.service';

import { AccountServiceClient } from './account-service/account.service';
import {TranslateUtil} from './ngx-translate/translate.util';

import { DeclaredVarTranslateLoader } from './ngx-translate/declared.var.translate.loader';
import { AppComponent } from './app.component';
import {VerticalNavComponent} from './vertical-nav/vertical-nav.component';
import {InlineNotification} from './notification/inline-notification-component';

import { VerticalNavigationModule } from 'patternfly-ng/navigation';
import {InlineNotificationModule} from 'patternfly-ng/notification/inline-notification';


/* Routing Module */
import { AppRoutingModule } from './app-routing.module';

const decs = [
    AppComponent,
    VerticalNavComponent,
    InlineNotification,
];

export const ORIGINAL_INCOMING_URL: Location = window.location;

@NgModule({
  declarations: decs,
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    VerticalNavigationModule,
    InlineNotificationModule,
    TranslateModule.forRoot({
        loader: {provide: TranslateLoader, useClass: DeclaredVarTranslateLoader}
    }),
    AppRoutingModule,
  ],
  providers: [
    KeycloakService,
    KeycloakGuard,
    KEYCLOAK_HTTP_PROVIDER,
    ResponsivenessService,
    KeycloakNotificationService,
    AccountServiceClient,
    TranslateUtil,
    { provide: LocationStrategy, useClass: HashLocationStrategy }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
