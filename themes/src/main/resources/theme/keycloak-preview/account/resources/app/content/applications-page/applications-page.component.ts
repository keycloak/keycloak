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

import {TranslateUtil} from '../../ngx-translate/translate.util';
import {AccountServiceClient} from '../../account-service/account.service';

import {Application} from './application';

import {View} from '../widgets/toolbar.component';
import {PropertyLabel} from '../widgets/property.label';
import {ActionButton} from '../widgets/action.button';
import {RefreshButton, Refreshable} from '../widgets/refresh.button';

declare const resourceUrl: string;

type SelectableProperty = "name" | "description";

@Component({
    moduleId: module.id, // need this for styleUrls path to work properly with Systemjs
    selector: 'app-applications-page',
    templateUrl: 'applications-page.component.html',
    styleUrls: ['applications-page.component.css']
})
export class ApplicationsPageComponent implements Refreshable, OnInit {
    private activeView: View = "LargeCards";

    private resourceUrl: string = resourceUrl;
    private applications: Application[] = [];
    private isSortAscending: boolean = true;
    private sortBy: SelectableProperty = "name";
    private filterBy: SelectableProperty = "name";
    private filterText: string = "";
    
    private propLabels: PropertyLabel[] = [];
    private actionButtons: ActionButton[] = [];
    
    private sessions: any[] = [];

    constructor(accountSvc: AccountServiceClient, private translateUtil: TranslateUtil) {
        this.initPropLabels();
        this.actionButtons.push(new RefreshButton(accountSvc,"/applications", this));
        accountSvc.doGetRequest("/applications", (res: Response) => this.refresh(res));
        accountSvc.doGetRequest("/sessions", (res: Response) => this.handleGetSessionsResponse(res));
    }
    
    private initPropLabels(): void {
        this.propLabels.push({prop: "name", label: "Name"});
        this.propLabels.push({prop: "description", label: "Description"});
    }

    public refresh(res: Response) {
        console.log('**** response from apps REST API ***');
        console.log(JSON.stringify(res));
        console.log('*** apps res.json() ***');
        console.log(JSON.stringify(res.json().applications));
        console.log('*************************************');

        const newApps: Application[] = [];
        for (let app of res.json().applications) {
            newApps.push(new Application(app, this.translateUtil));
        }
        
        // reference must change to trigger pipes
        this.applications = newApps;
    }

    private handleGetSessionsResponse(res: Response) {
        console.log('**** response from sessions REST API ***');
        console.log(JSON.stringify(res));
        console.log('*** sessions res.json() ***');
        console.log(JSON.stringify(res.json()));
        console.log('***************************************');
        this.sessions = res.json();
    }

    ngOnInit() {
    }

}
