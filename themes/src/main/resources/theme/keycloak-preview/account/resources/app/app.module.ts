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

import {ResponsivenessService} from './responsiveness-service/responsiveness.service'

import { AccountServiceClient } from './account-service/account.service';
import {TranslateUtil} from './ngx-translate/translate.util';

import { DeclaredVarTranslateLoader } from './ngx-translate/declared.var.translate.loader';
import { AppComponent } from './app.component';
import { TopNavComponent } from './top-nav/top-nav.component';
import { NotificationComponent } from './top-nav/notification.component';
import { ToastNotifier } from './top-nav/toast.notifier';
import { SideNavComponent } from './side-nav/side-nav.component';
import { AccountPageComponent } from './content/account-page/account-page.component';
import { PasswordPageComponent } from './content/password-page/password-page.component';
import { PageNotFoundComponent } from './content/page-not-found/page-not-found.component';

import { AuthenticatorPageComponent } from './content/authenticator-page/authenticator-page.component';

import { SessionsPageComponent } from './content/sessions-page/sessions-page.component';
import { LargeSessionCardComponent } from './content/sessions-page/large-session-card.component';
import { SmallSessionCardComponent } from './content/sessions-page/small-session-card.component';

import { ApplicationsPageComponent } from './content/applications-page/applications-page.component';
import { LargeAppCardComponent } from './content/applications-page/large-app-card.component';
import { SmallAppCardComponent } from './content/applications-page/small-app-card.component';
import { RowAppCardComponent } from './content/applications-page/row-app-card.component';

import { ToolbarComponent } from './content/widgets/toolbar.component';

import {OrderbyPipe} from './pipes/orderby.pipe';
import {FilterbyPipe} from './pipes/filterby.pipe';

const routes: Routes = [
    { path: 'account', component: AccountPageComponent },
    { path: 'password', component: PasswordPageComponent },
    { path: 'authenticator', component: AuthenticatorPageComponent },
    { path: 'sessions', component: SessionsPageComponent },
    { path: 'applications', component: ApplicationsPageComponent },
    { path: '', redirectTo: '/account', pathMatch: 'full' },
    { path: '**', component: PageNotFoundComponent}
];

const decs = [
    AppComponent,
    TopNavComponent,
    NotificationComponent,
    SideNavComponent,
    AccountPageComponent,
    PasswordPageComponent,
    PageNotFoundComponent,
    AuthenticatorPageComponent,
    SessionsPageComponent,
    LargeSessionCardComponent,
    SmallSessionCardComponent,
    ApplicationsPageComponent,
    LargeAppCardComponent,
    SmallAppCardComponent,
    RowAppCardComponent,
    ToolbarComponent,
    OrderbyPipe,
    FilterbyPipe
];

export const ORIGINAL_INCOMING_URL: Location = window.location;

@NgModule({
  declarations: decs,
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    TranslateModule.forRoot({
        loader: {provide: TranslateLoader, useClass: DeclaredVarTranslateLoader}
    }),
    RouterModule.forRoot(routes)
  ],
  providers: [
    KeycloakService,
    KEYCLOAK_HTTP_PROVIDER,
    ResponsivenessService,
    AccountServiceClient,
    TranslateUtil,
    ToastNotifier,
    { provide: LocationStrategy, useClass: HashLocationStrategy }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
