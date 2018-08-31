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
import {Component, OnInit} from '@angular/core';

import {AccountServiceClient} from '../../account-service/account.service';

import {LinkedAccounts} from './linked-accounts-bean.interface';

@Component({
    selector: 'app-linked-accounts-page',
    templateUrl: './linked-accounts-page.component.html',
    styleUrls: ['./linked-accounts-page.component.css']
})
export class LinkedAccountsPageComponent implements OnInit {
    
    private linkedAccounts: LinkedAccounts;
    private redirectUri: string = "http://localhost:8080/auth/realms/master/account/%23/linked-accounts";//resourceUrl + "#/linked-accounts";

    constructor(private accountSvc: AccountServiceClient) {
    }
    
    private linkAccount(providerId: string):void {
        console.log('>>>> redirectUri=' + this.redirectUri)
        this.accountSvc.doGetRequest("/linked-accounts/" + providerId + "?redirectUri=" + this.redirectUri, (res: Response) => this.handleAccountLinkUriRequest(res));
    }
    
    private doGet():void {
        this.accountSvc.doGetRequest("/linked-accounts", (res: Response) => this.handleGetResponse(res));
        //this.accountSvc.doGetNonAccount("http://localhost:8080/auth/admin/realms/master/users?first=0&max=20", (res: Response) => this.handleGetResponse(res));
        //this.accountSvc.doGetNonAccount("http://localhost:8080/auth/admin/master/console/config", (res: Response) => this.handleGetResponse(res));
    }
    
    private doRemove(providerId: string):void {
        this.accountSvc.doDelete("/linked-accounts/" + providerId, (res: Response) => this.handleDeleteResponse(res));
    }
    
    protected handleGetResponse(res: Response) {
        console.log('**** response from Linked Accounts GET ***');
        console.log(JSON.stringify(res));
        console.log(res.json())
        console.log('***************************************');
    }
    
    protected handleAccountLinkUriRequest(res: Response) {
        console.log('**** response from Linked Accounts GET Account Link URI Request ***');
        console.log(JSON.stringify(res));
        console.log('accountLinkUri=' + res.json()['accountLinkUri']);
        console.log('***************************************');
        window.location.replace(res.json()['accountLinkUri']); 
    }
    
    protected handleDeleteResponse(res: Response) {
        console.log('**** response from Linked Accounts DELETE ***');
        console.log(JSON.stringify(res));
        console.log('***************************************');
    }
    
    ngOnInit() {
    }
}
