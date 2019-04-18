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

import {HashRouter} from 'react-router-dom';

import {App} from './app/App';

const e = React.createElement;

export interface MainProps {}
export class Main extends React.Component<MainProps> {
    
    public constructor(props: MainProps) {
        super(props);
    }

    public render(): React.ReactNode {
        return (
            <HashRouter>
                <App/>
            </HashRouter>
        );
    }
};

const domContainer = document.querySelector('#main_react_container');
ReactDOM.render(e(Main), domContainer);