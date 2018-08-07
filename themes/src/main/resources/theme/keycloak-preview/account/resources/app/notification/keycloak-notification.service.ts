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
import {Injectable} from '@angular/core';
import {Router, NavigationEnd} from '@angular/router';
 
import {NotificationService} from 'patternfly-ng/notification/notification-service';
import {TranslateUtil} from '../ngx-translate/translate.util';

@Injectable()
export class KeycloakNotificationService extends NotificationService {
    
    constructor(router: Router,
                private translateUtil: TranslateUtil) {
        super();
        router.events.subscribe(value => {
            if (value instanceof NavigationEnd) {
                this.removeAll();
            };
        });
    }
    
    public notify(message: string, notificationType: string, params?: Array<any>) : void {
        let translatedMessage: string = this.translateUtil.translate(message, params);
        translatedMessage = translatedMessage.replace("%27", "'");
        this.message(
            notificationType, // type
            null, // header
            translatedMessage, // message
            false, // isPersistent
            null, // Action primaryAction
            null // Action[] more actions
        );
        this.setVerbose(true);
    }
    
    private removeAll(): void {
        for (let notification of this.getNotifications()) {
            this.remove(notification);
        }
    }
}


