/* 
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
import {Route} from 'react-router-dom';
import {NavItem} from '@patternfly/react-core';

export interface PageDef {
    path: string;
    label: string;
    component: React.ComponentType;
}

declare const extensionPages: PageDef[];

export class ExtensionPages { // extends React.Component<ExtensionPagesProps> {
    
    public static get Links(): React.ReactNode {
        if (typeof extensionPages === 'undefined') return (<span/>);
        
        const links: React.ReactElement[] = extensionPages.map((page: PageDef, index: number) => 
            <NavItem key={page.path} to={'#/app/' + page.path} itemId={'ext-' + index} type="button">{page.label}</NavItem>
        );
        return (<React.Fragment>{links}</React.Fragment>);
    }
    
    public static get Routes(): React.ReactNode {
        if (typeof extensionPages === 'undefined') return (<span/>);
        
        const routes: React.ReactElement<Route>[] = extensionPages.map((page) => 
            <Route key={page.path} path={'/app/' + page.path} component={page.component}/>
        );
        return (<React.Fragment>{routes}</React.Fragment>);
    }

};