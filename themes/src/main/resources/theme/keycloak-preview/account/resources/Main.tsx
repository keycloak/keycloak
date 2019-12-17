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
import * as ReactDOM from 'react-dom';
import {System} from 'systemjs';

import {HashRouter} from 'react-router-dom';

import {App} from './app/App';
import {ContentItem, ModulePageDef, flattenContent, initGroupAndItemIds, isExpansion, isModulePageDef} from './app/ContentPages';

declare let isReactLoading: boolean;
declare function toggleReact(): void;

export interface MainProps {}
export class Main extends React.Component<MainProps> {
    
    public constructor(props: MainProps) {
        super(props);
    }

    public componentDidMount(): void {
        isReactLoading = false;
        toggleReact();
    }

    public render(): React.ReactNode {
        return (
            <HashRouter>
                <App/>
            </HashRouter>
        );
    }
};

declare const resourceUrl: string;
declare let content: ContentItem[];
const e = React.createElement;

function removeHidden(items: ContentItem[]): ContentItem[] {
    const visible: ContentItem[] = [];

    for (let item of items) {
        if (item.hidden) continue;
        
        if (isExpansion(item)) {
            visible.push(item);
            item.content = removeHidden(item.content);
            if (item.content.length === 0) {
                visible.pop(); // remove empty expansion
            }
        } else {
            visible.push(item);
        }
    }

    return visible;
}

content = removeHidden(content);
initGroupAndItemIds();

function loadModule(modulePage: ModulePageDef): Promise<ModulePageDef> {
    return new Promise ((resolve, reject) => {
        System.import(resourceUrl + modulePage.modulePath).then( (module: React.Component) => {
            modulePage.module = module;
            resolve(modulePage);
        }).catch((error: Error) => {
            console.warn('Unable to load ' + modulePage.label + ' because ' + error.message);
            reject(modulePage);
        });
    });
};

const moduleLoaders: Promise<ModulePageDef>[] = [];
flattenContent(content).forEach((item: ContentItem) => {
    if (isModulePageDef(item)) {
        moduleLoaders.push(loadModule(item));
    }
});

// load content modules and start
Promise.all(moduleLoaders).then(() => {
    const domContainer = document.querySelector('#main_react_container');
    ReactDOM.render(e(Main), domContainer);
});