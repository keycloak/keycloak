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
 import {Component, Input, OnInit} from '@angular/core';
 
 import {PropertyLabel} from './property.label';
 import {ActionButton} from './action.button';
 import {Icon} from '../../page/icon';
 
 export type View = "LargeCards" | "SmallCards" | "List";
 
 /**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2017 Red Hat Inc.
 */
 @Component({
    moduleId: module.id, // need this for styleUrls path to work properly with Systemjs
    selector: 'toolbar',
    templateUrl: 'toolbar.html',
    styleUrls: ['toolbar.css']
})
export class ToolbarComponent implements OnInit {
    @Input() filterProps: PropertyLabel[];
    @Input() sortProps: PropertyLabel[];
    @Input() actionButtons: ActionButton[];
    
    // TODO: localize in constructor
    readonly sortByTooltip: string = "Sort by...";
    readonly sortAscendingTooltip: string = "Sort Ascending";
    readonly sortDescendingTooltip: string = "Sort Descending";
    
    private isSortAscending: boolean = true;
    private sortBy: PropertyLabel;
    private filterBy: PropertyLabel;
    private filterText: string = "";
    
    public activeView: View = "LargeCards";
    
    ngOnInit() {
        if (this.filterProps && this.filterProps.length > 0) {
            this.filterBy = this.filterProps[0];
        }
        
        if (this.sortProps && this.sortProps.length > 0) {
            this.sortBy = this.sortProps[0];
        }
    }
    
    private changeView(activeView: View) {
        this.activeView = activeView;
    }
    
    private toggleSort() {
        this.isSortAscending = !this.isSortAscending;
    }
    
    private changeSortByProp(prop: PropertyLabel) {
        this.sortBy = prop;
    }
    
    private changeFilterByProp(prop: PropertyLabel) {
        this.filterBy = prop;
        this.filterText = "";
    }
    
    private selectedFilterClass(prop: PropertyLabel): string {
        if (this.filterBy === prop) {
            return "selected";
        } else {
            return "";
        }
    }
    
    private selectedSortByClass(prop: PropertyLabel): string {
        if (this.sortBy === prop) {
            return "selected";
        } else {
            return "";
        }
    }
    
    isIconButton(button: ActionButton): boolean {
        return button.label instanceof Icon;
    }
}


