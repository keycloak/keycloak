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
import {Router, NavigationEnd} from '@angular/router';

import {KeycloakNotificationService} from '../notification/keycloak-notification.service';

@Component({
    selector: 'inline-notification',
    templateUrl: './inline-notification.component.html',
    styleUrls: ['./inline-notification.component.css']
})
export class InlineNotification implements OnInit {
    dismissable: boolean = true;
    message: string = '';
    type: string = 'success';
    hidden: boolean = true;
    
    constructor(private notificationSvc: KeycloakNotificationService,
                private router: Router) {
        this.router.events.subscribe(value => {
            if (value instanceof NavigationEnd) {
                this.hidden = true;
            }
        });
    }

    ngOnInit(): void {
      // Track Notifications
      this.notificationSvc.getNotificationsObserver
        .subscribe((notification) => {
            console.log('>> notification=' + JSON.stringify(notification));
            if (notification.length === 0) {
                this.hidden = true;
                return;
            }
            console.log('>> updating message...');
            // always display the latest message
            this.message = notification[notification.length-1].message;
            this.type = notification[notification.length-1].type;
            this.hidden = false;
        });
    }

}


