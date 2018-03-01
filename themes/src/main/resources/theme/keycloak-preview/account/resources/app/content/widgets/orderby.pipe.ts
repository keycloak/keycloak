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
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
@Pipe({name: 'orderby'})
export class OrderbyPipe implements PipeTransform {
    transform(objects: any[], property: string, descending: boolean = true) {
        if (!property) return objects;
        
        let sorted: any[] = objects.sort((obj1, obj2) => {
            if (obj1[property] > obj2[property]) return 1;
            if (obj1[property] < obj2[property]) return -1;
            return 0;
        })
        
        if (descending) {
            return sorted;
        } else {
            return sorted.reverse();
        }
    }
}


