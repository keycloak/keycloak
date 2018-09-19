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
import {Component, OnInit, ViewChild, Renderer2} from '@angular/core';
import {Response} from '@angular/http';
import {FormGroup} from '@angular/forms';

import {NotificationType} from 'patternfly-ng/notification';

import {AccountServiceClient} from '../../account-service/account.service';
import {KeycloakNotificationService} from '../../notification/keycloak-notification.service';

@Component({
    selector: 'app-password-page',
    templateUrl: './password-page.component.html',
    styleUrls: ['./password-page.component.css']
})
export class PasswordPageComponent implements OnInit {

    @ViewChild('formGroup') private formGroup: FormGroup;
    private lastPasswordUpdate: number;
    
    constructor(private accountSvc: AccountServiceClient, 
                private renderer: Renderer2,
                protected kcNotifySvc: KeycloakNotificationService,) {
        this.accountSvc.doGetRequest("/credentials/password", (res: Response) => this.handleGetResponse(res));
    }
    
    public changePassword() {
        console.log("posting: " + JSON.stringify(this.formGroup.value));
        if (!this.confirmationMatches()) return;
        this.accountSvc.doPostRequest("/credentials/password", (res: Response) => this.handlePostResponse(res), this.formGroup.value);
        this.renderer.selectRootElement('#password').focus();
    }
    
    private confirmationMatches(): boolean {
        const newPassword: string = this.formGroup.value['newPassword'];
        const confirmation: string = this.formGroup.value['confirmation'];
        
        const matches: boolean = newPassword === confirmation;
        
        if (!matches) {
            this.kcNotifySvc.notify('notMatchPasswordMessage', NotificationType.DANGER)
        }
        
        return matches;
    }
    
    protected handlePostResponse(res: Response) {
      console.log('**** response from password POST ***');
      console.log(JSON.stringify(res));
      console.log('***************************************');
      this.formGroup.reset();
      this.accountSvc.doGetRequest("/credentials/password", (res: Response) => this.handleGetResponse(res));
    }
    
    protected handleGetResponse(res: Response) {
        console.log('**** response from password GET ***');
        console.log(JSON.stringify(res));
        console.log('***************************************');
        this.lastPasswordUpdate = res.json()['lastUpdate'];
    }

    ngOnInit() {
    }
}
