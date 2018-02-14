/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
import { NgModule }             from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import {KeycloakGuard} from './keycloak-service/keycloak.guard';

import { HomePageComponent } from './content/home-page/home-page.component';

declare const resourceUrl: string;

export const routes: Routes = [
        {path: '', canActivateChild:[KeycloakGuard], children: [
            { path: 'account', loadChildren: resourceUrl + '/app/content/account-page/account.module.js#AccountModule' },
            { path: 'password', loadChildren: resourceUrl + '/app/content/password-page/password.module.js#PasswordModule' },
            { path: 'authenticator', loadChildren: resourceUrl + '/app/content/authenticator-page/authenticator.module.js#AuthenticatorModule' },
            { path: 'device-activity', loadChildren: resourceUrl + '/app/content/device-activity-page/device-activity.module.js#DeviceActivityModule' },
            { path: 'sessions', loadChildren: resourceUrl + '/app/content/sessions-page/sessions.module.js#SessionsModule' },
            { path: 'applications', loadChildren: resourceUrl + '/app/content/applications-page/applications.module.js#ApplicationsModule' },
            { path: 'linked-accounts', loadChildren: resourceUrl + '/app/content/linked-accounts-page/linked-accounts.module.js#LinkedAccountsModule' },
            { path: 'my-resources', loadChildren: resourceUrl + '/app/content/my-resources-page/my-resources.module.js#MyResourcesModule' },
            { path: ':**', loadChildren: resourceUrl + '/app/content/page-not-found/page-not-found.module.js#PageNotFoundModule' },
            ]
        }
    ];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
  declarations: [HomePageComponent]
})
export class AppRoutingModule {}

