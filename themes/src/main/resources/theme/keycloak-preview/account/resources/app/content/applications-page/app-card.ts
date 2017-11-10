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
 
import {Application} from './application';
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
export abstract class AppCard {
    
    abstract app: Application;
    abstract sessions: any[];
    
/*    
    protected getName(): string {
        if (this.app.hasOwnProperty('name')) {
            return this.translateUtil.translate(this.app.name);
        }
        
        return this.app.clientId;
    }
    
    protected getDescription (): string {
        if (!this.app.hasOwnProperty('description')) return null;
        
        let desc: string = this.app.description;
        
        if (desc.indexOf("//icon") > -1) {
            desc = desc.substring(0, desc.indexOf("//icon"));
        }
        
        return desc;
    }*/
    
    protected isSessionActive() : boolean {
        for (let session of this.sessions) {
            for (let client of session.clients) {
                if (this.app.clientId === client.clientId) return true;
            }
        }
        return false;
    }
    
}


