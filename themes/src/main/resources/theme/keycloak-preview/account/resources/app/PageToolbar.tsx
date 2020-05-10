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
 
import {Dropdown, KebabToggle, Toolbar, ToolbarGroup, ToolbarItem} from '@patternfly/react-core';

import {ReferrerDropdownItem} from './widgets/ReferrerDropdownItem';
import {ReferrerLink} from './widgets/ReferrerLink';
import {Features} from './widgets/features';
import {LogoutButton,LogoutDropdownItem} from './widgets/Logout';

declare const referrerName: string;
declare const features: Features;

interface PageToolbarProps {}
interface PageToolbarState {isKebabDropdownOpen: boolean}
export class PageToolbar extends React.Component<PageToolbarProps, PageToolbarState> {
    private hasReferrer: boolean = typeof referrerName !== 'undefined';

    public constructor(props: PageToolbarProps) {
        super(props);
        
        this.state = {
            isKebabDropdownOpen: false,
        };
    }
    
    private onKebabDropdownToggle = (isKebabDropdownOpen: boolean) => {
        this.setState({
            isKebabDropdownOpen
        });
    };
    
    public render(): React.ReactNode {
        const kebabDropdownItems = [];
        if (this.hasReferrer) {
            kebabDropdownItems.push(
                <ReferrerDropdownItem key='referrerDropdownItem'/>
            )
        }
        
        kebabDropdownItems.push(<LogoutDropdownItem key='LogoutDropdownItem'/>);
        
        return (
            <Toolbar>
                {this.hasReferrer &&
                    <ToolbarGroup key='referrerGroup'>
                        <ToolbarItem className="pf-m-icons" key='referrer'>
                            <ReferrerLink/>
                        </ToolbarItem>
                    </ToolbarGroup>
                }
                    
                <ToolbarGroup key='secondGroup'>
                    <ToolbarItem className="pf-m-icons" key='logout'>
                        <LogoutButton/>
                    </ToolbarItem>
                    
                    <ToolbarItem key='kebab' className="pf-m-mobile">
                        <Dropdown
                            isPlain
                            position="right"
                            toggle={<KebabToggle id="mobileKebab" onToggle={this.onKebabDropdownToggle} />}
                            isOpen={this.state.isKebabDropdownOpen}
                            dropdownItems={kebabDropdownItems}
                        />
                    </ToolbarItem>
                </ToolbarGroup>
            </Toolbar>
        );
    }
}
