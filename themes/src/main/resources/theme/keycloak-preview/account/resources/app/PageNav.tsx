/* 
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

import {Nav, NavExpandable, NavList, NavItem} from '@patternfly/react-core';

import {Msg} from './widgets/Msg';
import {ExtensionPages} from './content/extensions/ExtensionPages';
 
export interface PageNavProps {
}
 
interface PageNavState {
    activeGroup: string | number;
    activeItem: string | number;
}

export class PageNav extends React.Component<PageNavProps, PageNavState> {
    
    public constructor(props: PageNavProps) {
        super(props);
        this.state = {
            activeGroup: '',
            activeItem: 'grp-0_itm-0'
        };
    }

    private onNavSelect = (groupId: number, itemId: number): void => {
        this.setState({
            activeItem: itemId,
            activeGroup: groupId
        });
    };
    
    public render(): React.ReactNode {
        return (
            <Nav onSelect={this.onNavSelect} aria-label="Nav">
                <NavList>
                    <NavItem to="#/app/account" itemId="grp-0_itm-0" isActive={this.state.activeItem === 'grp-0_itm-0'}>
                        {Msg.localize("account")}
                    </NavItem>
                    <NavExpandable title="Account Security" groupId="grp-1" isActive={this.state.activeGroup === 'grp-1'}>
                        <NavItem to="#/app/password" groupId="grp-1" itemId="grp-1_itm-1" isActive={this.state.activeItem === 'grp-1_itm-1'}>
                            {Msg.localize("password")}
                        </NavItem>
                        <NavItem to="#/app/authenticator" groupId="grp-1" itemId="grp-1_itm-2" isActive={this.state.activeItem === 'grp-1_itm-2'}>
                            {Msg.localize("authenticator")}
                        </NavItem>
                        <NavItem to="#/app/device-activity" groupId="grp-1" itemId="grp-1_itm-3" isActive={this.state.activeItem === 'grp-1_itm-3'}>
                            {Msg.localize("device-activity")}
                        </NavItem>
                        <NavItem to="#/app/linked-accounts" groupId="grp-1" itemId="grp-1_itm-4" isActive={this.state.activeItem === 'grp-1_itm-4'}>
                            {Msg.localize("linkedAccountsHtmlTitle")}
                        </NavItem>
                    </NavExpandable>
                    <NavItem to="#/app/applications" itemId="grp-2_itm-0" isActive={this.state.activeItem === 'grp-2_itm-0'}>
                        {Msg.localize("applications")}
                    </NavItem>
                    <NavItem to="#/app/my-resources" itemId="grp-3_itm-0" isActive={this.state.activeItem === 'grp-3_itm-0'}>
                        {Msg.localize("myResources")}
                    </NavItem>
                    {ExtensionPages.Links}
                </NavList>
            </Nav>
        );
    }
}
