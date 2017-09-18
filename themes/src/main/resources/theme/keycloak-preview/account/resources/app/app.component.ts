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
import {Component, HostListener} from '@angular/core';

import {TranslateService} from '@ngx-translate/core';

import {ResponsivenessService, ContentWidthClass, MenuClickListener} from "./responsiveness-service/responsiveness.service";

declare const locale: string;

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent implements MenuClickListener {
    
    private contentWidthClass: ContentWidthClass = this.respSvc.calcSideContentWidthClass();
    
    constructor(translate: TranslateService, private respSvc: ResponsivenessService) {
        // this language will be used as a fallback when a translation isn't found in the current language
        translate.setDefaultLang('en');

        // the lang to use, if the lang isn't available, it will use the current loader to get them
        translate.use(locale);
        
        this.respSvc.addMenuClickListener(this);
    }
    
    public menuClicked() : void {
        this.contentWidthClass = this.respSvc.calcSideContentWidthClass();
    }
    
    @HostListener('window:resize', ['$event'])
    private onResize(event: any) {
        this.contentWidthClass = this.respSvc.calcSideContentWidthClass();
    }
    
}