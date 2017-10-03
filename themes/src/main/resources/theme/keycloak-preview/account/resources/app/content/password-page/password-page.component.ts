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
import {Component, OnInit, ViewChild} from '@angular/core';
import {Response} from '@angular/http';
import {FormGroup} from '@angular/forms';

import {AccountServiceClient} from '../../account-service/account.service';

@Component({
    selector: 'app-password-page',
    templateUrl: './password-page.component.html',
    styleUrls: ['./password-page.component.css']
})
export class PasswordPageComponent implements OnInit {

    @ViewChild('formGroup') private formGroup: FormGroup;
    
    constructor(private accountSvc: AccountServiceClient) {
    }
    
    public changePassword() {
        console.log("posting: " + JSON.stringify(this.formGroup.value));
        this.accountSvc.doPostRequest("/credentials", (res: Response) => this.handlePostResponse(res), this.formGroup.value);
    }
    
    protected handlePostResponse(res: Response) {
      console.log('**** response from account POST ***');
      console.log(JSON.stringify(res));
      console.log('***************************************');
    }

    ngOnInit() {
    }
}
