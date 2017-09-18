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
 * For this application, we are responsive to three sizes: large, medium, and
 * small.  Note that these do not perfectly correspond to bootstrap device
 * sizes but are more in line with patternfly.
 * 
 * When making decisions based on screen size, you should create a single
 * instance of this class and then test for each size.  Do not use several
 * instances because the screen size could change at any time and you may
 * get inconsistent results.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
export class Media {

    private screenWidth: number = window.innerWidth;
    
    public isLarge(): boolean {
        return this.screenWidth > 1023;
    }
    
    public isMedium(): boolean {
        return (this.screenWidth < 1023) && (this.screenWidth > 768);
    }
    
    public isSmall(): boolean {
        return this.screenWidth < 769;
    }
}


