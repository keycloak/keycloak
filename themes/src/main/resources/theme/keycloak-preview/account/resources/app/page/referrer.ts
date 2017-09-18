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
import {TranslateUtil} from '../ngx-translate/translate.util';

declare const referrer: string;
declare const referrer_uri: string;

 /**
 * Encapsulate referrer logic.
 * 
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
export class Referrer {

    constructor(private translateUtil: TranslateUtil) {}
    
    public exists(): boolean {
        return typeof referrer !== "undefined";
    }
    
    // return a value suitable for parameterized use with ngx-translate
    // example {{'backTo' | translate:referrer.getName()}}
    public getName(): { param_0: string } {
        return {param_0: this.translateUtil.translate(referrer) };
    }
    
    public getUri(): string {
        return referrer_uri;
    }
}


