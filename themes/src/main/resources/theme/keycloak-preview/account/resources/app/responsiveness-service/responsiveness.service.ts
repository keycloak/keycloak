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
 import { Injectable } from "@angular/core";
 
 import { Media } from "./media";
 
 export type SideNavClasses = "" | "collapsed" | "hidden" | "hidden show-mobile-nav";
 export type ContentWidthClass = "" | "collapsed-nav" | "hidden-nav";
 
 export interface MenuClickListener {
     menuClicked(): void;
 }
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
@Injectable()
export class ResponsivenessService {

    private menuOn: boolean = false;
    private menuListeners: MenuClickListener[] = Array<MenuClickListener>();
    
    public addMenuClickListener(listener: MenuClickListener) {
        this.menuListeners.push(listener);
    }
    
    public menuClicked() : void {
        this.menuOn = !this.menuOn;
        
        for (let listener of this.menuListeners) {
            listener.menuClicked();
        }
    }
    
    public calcSideNavWidthClasses() : SideNavClasses {
        const media: Media = new Media();
        
        if (media.isLarge() && !this.menuOn) {
            return "";
        }
        
        if (media.isLarge() && this.menuOn) {
            return "collapsed";
        }
        
        if (media.isMedium() && !this.menuOn) {
            return "collapsed";
        }
        
        if (media.isMedium() && this.menuOn) {
            return "";
        }
        
        // media must be small
        if (!this.menuOn) {
            return "hidden"
        }
        
        return "hidden show-mobile-nav";
    }
    
    public calcSideContentWidthClass() : ContentWidthClass {
        const media: Media = new Media();
        
        if (media.isLarge() && !this.menuOn) {
            return "";
        }
        
        if (media.isLarge() && this.menuOn) {
            return "collapsed-nav";
        }
        
        if (media.isMedium() && !this.menuOn) {
            return "collapsed-nav";
        }
        
        if (media.isMedium() && this.menuOn) {
            return "";
        }
        
        // media must be small
        if (!this.menuOn) {
            return "hidden-nav"
        }
        
        return "hidden-nav";
    }
}


