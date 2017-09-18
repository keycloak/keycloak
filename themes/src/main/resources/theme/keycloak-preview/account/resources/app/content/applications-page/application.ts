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
 
 import {TranslateUtil} from '../../ngx-translate/translate.util';
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
export class Application {

    constructor(private app: any, private translateUtil: TranslateUtil ) {
        this.setIcon();
    }
    
    private setIcon(): void {
        this.app.icon = "pficon-key";
        
        if (!this.app.hasOwnProperty('description')) {
            return;
        }
        
        let desc: string = this.app.description;
        const iconIndex: number = desc.indexOf("//icon=");
        if (iconIndex > -1) {
            this.app.icon = desc.substring(iconIndex + 7, desc.length);
        }
    }
    
    public get clientId():string {
        return this.app.name;
    }
    
    public get name():string {
        if (this.app.hasOwnProperty('name')) {
            return this.translateUtil.translate(this.app.name);
        }
        
        return this.app.clientId;
    }
    
    public get description(): string {
        if (!this.app.hasOwnProperty('description')) return null;
        
        let desc: string = this.app.description;
        
        if (desc.indexOf("//icon") > -1) {
            desc = desc.substring(0, desc.indexOf("//icon"));
        }
        
        return desc;
    }
    
    public get icon(): string {
        return this.app.icon;
    }
    
    public get effectiveUrl(): string {
        return this.app.effectiveUrl;
    }
}


