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
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
export class Session {

    constructor(private session: any) {}
    
    get ipAddress(): string {
        return this.session.ipAddress;
    }
    
    get started(): number {
        return this.session.started;
    }
    
    get lastAccess(): number {
        return this.session.lastAccess;
    }
    
    get expires(): number {
        return this.session.expires;
    }
    
    get clients(): string[] {
        return this.session.clients;
    }
}


