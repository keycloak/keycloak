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

import {Msg} from '../widgets/Msg';
 
declare const referrerName: string;
declare const referrerUri: string;

export interface ReferrerProps {
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class Referrer extends React.Component<ReferrerProps> {
    
    public constructor(props: ReferrerProps) {
        super(props);
    }

    public render(): React.ReactNode {
        if (typeof referrerName === "undefined") return null;
        
        return (
            <a className="nav-item-iconic" href={referrerUri}>
               <span className="pficon-arrow"></span>
               <Msg msgKey="backTo" params={[referrerName]}/>
            </a>
        );
    }
};