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
 
 import {Injectable, EventEmitter} from '@angular/core';
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
@Injectable()
export class ToastNotifier extends EventEmitter<ToastNotification> {
    constructor() {
        super();
    }
}

type ToastIcon = "pficon-ok" | 
                 "pficon-info" |
                 "pficon-warning-triangle-o" |
                 "pficon-error-circle-o";
                 
type ToastAlertType = "alert-success" |
                      "alert-info" |
                      "alert-warning" |
                      "alert-danger";
                 
export type MessageType = "success" |
                          "info" |
                          "warning" |
                          "error";

export class ToastNotification {
    public alertType: ToastAlertType = "alert-success";
    public icon: ToastIcon = "pficon-ok";
    
    constructor(public message: string, messageType?: MessageType) {
        switch (messageType) {
            case "info": {
                this.alertType = "alert-info";
                this.icon = "pficon-info";
                break;
            }
            case "warning": {
                this.alertType = "alert-warning";
                this.icon = "pficon-warning-triangle-o";
                break;
            }
            case "error": {
                this.alertType = "alert-danger";
                this.icon = "pficon-error-circle-o";
                break;
            }
            default: {
                this.alertType = "alert-success";
                this.icon = "pficon-ok";
            }
        }
    }
}


