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
 
 import {Injectable} from '@angular/core';
 import {TranslateService} from '@ngx-translate/core';
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
@Injectable()
export class TranslateUtil {
    constructor(private translator: TranslateService) {
    }
    
    public translate(key: string, params?: Array<any>): string {
        // remove Freemarker syntax
        if (key.startsWith('${') && key.endsWith('}')) {
            key = key.substring(2, key.length - 1);
        }
        
        const ngTranslateParams = {};
        for (let i in params) {
            let paramName: string = 'param_' + i;
            ngTranslateParams[paramName] = params[i];
        }
        
        return this.translator.instant(key, ngTranslateParams);
    }
}


