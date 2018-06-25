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

import { AuthenticatorPageComponent } from './authenticator-page.component';
import { MobileAuthenticatorSetupPageComponent } from './mobile-authenticator-setup-page.component';
import { BackupCodeSetupPageComponent } from './backup-code-setup-page.component';
import { SMSCodeSetupPageComponent } from './sms-code-setup-page.component';

const routes: Routes = [
    { path: 'authenticator', component: AuthenticatorPageComponent },
    { path: 'mobile-authenticator-setup', component: MobileAuthenticatorSetupPageComponent },
    { path: 'backup-code-setup', component: BackupCodeSetupPageComponent },
    { path: 'sms-code-setup', component: SMSCodeSetupPageComponent },
    { path: '**', component: AuthenticatorPageComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AuthenticatorRoutingModule {}

