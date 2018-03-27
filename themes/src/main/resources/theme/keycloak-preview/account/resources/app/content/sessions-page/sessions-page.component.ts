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
import {Component, OnInit} from '@angular/core';
import {Response} from '@angular/http';

import {AccountServiceClient} from '../../account-service/account.service';
import {TranslateUtil} from '../../ngx-translate/translate.util';

import {View} from '../widgets/toolbar.component';
import {PropertyLabel} from '../widgets/property.label';
import {ActionButton} from '../widgets/action.button';
import {RefreshButton, Refreshable} from '../widgets/refresh.button';

import {Session} from './session';

@Component({
    selector: 'app-sessions-page',
    templateUrl: './sessions-page.component.html',
    styleUrls: ['./sessions-page.component.css']
})
export class SessionsPageComponent implements Refreshable, OnInit {
    private filterLabels: PropertyLabel[] = [];
    private sortLabels: PropertyLabel[] = [];

    private response: any[] = [];
    private sessions: Session[] = [];
    
    private actionButtons: ActionButton[] = [];
    
    constructor(private accountSvc: AccountServiceClient, private translateUtil: TranslateUtil ) {
        this.initPropLabels();
        this.actionButtons.push(new LogoutAllButton(accountSvc, translateUtil));
        this.actionButtons.push(new RefreshButton(accountSvc,"/sessions", this));
        accountSvc.doGetRequest("/sessions", (res: Response) => this.refresh(res));
    }
    
    private initPropLabels(): void {
        this.filterLabels.push({prop: "ipAddress", label: "IP"});
        
        this.sortLabels.push({prop: "ipAddress", label: "IP"});
        this.sortLabels.push({prop: "started", label: "Started"});
        this.sortLabels.push({prop: "lastAccess", label: "Last Access"});
        this.sortLabels.push({prop: "expires", label: "Expires"});
    }

    public refresh(res: Response) {
      console.log('**** response from account REST API ***');
      console.log(JSON.stringify(res));
      console.log('*** res.json() ***');
      console.log(JSON.stringify(res.json()));
      console.log('***************************************');
      this.response = res.json();
      
      const newSessions: Session[] = [];
      for (let session of res.json()) {
          newSessions.push(new Session(session));
      }
      
      // reference must change to trigger pipes
      this.sessions = newSessions;
    }
    
    private logoutAllSessions() {
        this.accountSvc.doDelete("/sessions", 
                                (res: Response) => this.handleLogoutResponse(res), 
                                {params: {current: true}},
                                "Logging out all sessions.");
    }
    
    private handleLogoutResponse(res: Response) {
      console.log('**** response from account DELETE ***');
      console.log(JSON.stringify(res));
      console.log('***************************************');
    }
    
    ngOnInit() {
    }

}

class LogoutAllButton implements ActionButton {
    public readonly label: string = "Logout All"; //TODO: localize in constructor
    public readonly tooltip: string;
    
    constructor(private accountSvc: AccountServiceClient, translateUtil: TranslateUtil ) {
        this.tooltip = translateUtil.translate('doLogOutAllSessions');
    }
        
    performAction(): void {
        this.accountSvc.doDelete("/sessions", 
                                (res: Response) => this.handleLogoutResponse(res), 
                                {params: {current: true}},
                                "Logging out all sessions.");
    }
    
    private handleLogoutResponse(res: Response) {
      console.log('**** response from account DELETE ***');
      console.log(JSON.stringify(res));
      console.log('***************************************');
    }
}
