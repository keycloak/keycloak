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
import { Pipe, PipeTransform } from '@angular/core';
 
 /**
 * Case insensitive filtering. 
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
@Pipe({name: 'filterby'})
export class FilterbyPipe implements PipeTransform {
    transform(objects: any[], property: string, text: string) {
        if (!property) return objects;
        if (!text) return objects;
        
        const transformed: any[] = [];
        for (let obj of objects) {
            let propVal:any = obj[property];
            if (!this.isString(propVal)) {
                console.error("Can't filter property " + property + ". Its value is not a string.");
                break;
            }
            
            let strPropVal:string = propVal as string;
            if (strPropVal.toLowerCase().indexOf(text.toLowerCase()) != -1) {
                transformed.push(obj);
            }
        }
        
        return transformed;
    }
    
    private isString(value: any): boolean {
        return (typeof value == 'string') || (value instanceof String);
    }
}


