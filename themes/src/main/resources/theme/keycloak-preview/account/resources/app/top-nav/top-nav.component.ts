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
import {Component, OnInit, Input} from '@angular/core';
import {TranslateUtil} from '../ngx-translate/translate.util';
import {KeycloakService} from '../keycloak-service/keycloak.service';
import {ResponsivenessService} from "../responsiveness-service/responsiveness.service";
import {Referrer} from "../page/referrer";

declare const resourceUrl: string;
declare const baseUrl: string;
declare const referrer: string;
declare const referrer_uri: string;
declare const isInternationalizationEnabled: boolean;
declare const availableLocales: Array<Object>;

@Component({
    selector: 'app-top-nav',
    templateUrl: './top-nav.component.html',
    styleUrls: ['./top-nav.component.css']
})
export class TopNavComponent implements OnInit {
    @Input() showSideNav: String;

    public resourceUrl: string = resourceUrl;
    public availableLocales: Array<Object> = availableLocales;
    
    private referrer: Referrer;
    
    constructor(private keycloakService: KeycloakService, 
                translateUtil: TranslateUtil, 
                private respSvc: ResponsivenessService) {
        this.referrer = new Referrer(translateUtil);
    }
    
    private menuClicked(): void {
        this.respSvc.menuClicked();
    }
    
    ngOnInit() {
    }

    private logout() {
        this.keycloakService.logout(baseUrl);
    }
    
    private showLocales(): boolean {
        return isInternationalizationEnabled && (this.availableLocales.length > 1); 
    }
    
    private changeLocale(newLocale: string) {
        this.keycloakService.login({kcLocale: newLocale });
    }

}
