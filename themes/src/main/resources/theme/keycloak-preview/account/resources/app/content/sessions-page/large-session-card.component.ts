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
 import {Component, Input} from '@angular/core';
 import {Session} from './session';
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
 @Component({
    moduleId: module.id, // need this for styleUrls path to work properly with Systemjs
    selector: 'large-session-card',
    templateUrl: 'large-session-card.component.html',
    styleUrls: ['large-session-card.component.css']
})
export class LargeSessionCardComponent {
    @Input() session: Session;
}


