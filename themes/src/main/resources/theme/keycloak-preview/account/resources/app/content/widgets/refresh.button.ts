/*
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import {Response} from '@angular/http';

import {ActionButton} from './action.button';
 
import {Icon} from '../../page/icon';

import {AccountServiceClient} from '../../account-service/account.service';

 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
export class RefreshButton implements ActionButton {

    readonly label:Icon = new Icon('fa', 'refresh');
    readonly tooltip:string = 'Refresh';  //TODO: localize in constructor
    
    constructor(private accountSvc: AccountServiceClient, 
                private request: string, 
                private refreshable:Refreshable) {}
    
    public performAction(): void {
        this.accountSvc.doGetRequest(this.request, (res: Response) => {
            this.refreshable.refresh(res);
        });
    }
}

export interface Refreshable {
    refresh(response:Response): void;
}


