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

import {Msg} from './Msg';

import {ArrowIcon} from '@patternfly/react-icons';
 
declare const referrerName: string;
declare const referrerUri: string;

export interface ReferrerLinkProps {
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
export class ReferrerLink extends React.Component<ReferrerLinkProps> {

    public constructor(props: ReferrerLinkProps) {
        super(props);
    }

    public render(): React.ReactNode {
        return (
            // '_hash_' is a workaround for when uri encoding is not
            // sufficient to escape the # character properly.
            // See AppInitiatedActionPage for more details.
            <a id="referrerLink" href={referrerUri.replace('_hash_', '#')}>
               <ArrowIcon/> <Msg msgKey="backTo" params={[referrerName]}/>
            </a>
        );
    }
};