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

import { Dropdown, KebabToggle, PageHeaderTools, PageHeaderToolsGroup, PageHeaderToolsItem } from '@patternfly/react-core';

import { ReferrerDropdownItem } from './widgets/ReferrerDropdownItem';
import { ReferrerLink } from './widgets/ReferrerLink';
import { Features } from './widgets/features';
import { LogoutButton, LogoutDropdownItem } from './widgets/Logout';

declare const referrerName: string;
declare const features: Features;

interface PageToolbarProps { 
    avatar: React.ReactNode;
}
interface PageToolbarState { isKebabDropdownOpen: boolean }
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
                <ReferrerDropdownItem key='referrerDropdownItem' />
            )
        }

        kebabDropdownItems.push(<LogoutDropdownItem key='LogoutDropdownItem' />);

        return (
            <PageHeaderTools>
                <PageHeaderToolsGroup visibility={{ default: 'hidden', lg: 'visible' }}>
                    {this.hasReferrer &&
                        <PageHeaderToolsItem>
                            <ReferrerLink />
                        </PageHeaderToolsItem>
                    }
                </PageHeaderToolsGroup>
                <PageHeaderToolsGroup visibility={{ default: 'hidden', lg: 'visible' }}>
                    <PageHeaderToolsItem>
                        <LogoutButton />
                    </PageHeaderToolsItem>
                    <PageHeaderToolsItem>
                        {this.props.avatar}
                    </PageHeaderToolsItem>
                </PageHeaderToolsGroup>


                <PageHeaderToolsGroup key='secondGroup' visibility={{ lg: 'hidden' }}>
                    <PageHeaderToolsItem key='kebab' className="pf-m-mobile">
                        <Dropdown
                            isPlain
                            position="right"
                            toggle={<KebabToggle id="mobileKebab" onToggle={this.onKebabDropdownToggle} />}
                            isOpen={this.state.isKebabDropdownOpen}
                            dropdownItems={kebabDropdownItems}
                        />
                    </PageHeaderToolsItem>
                    <PageHeaderToolsItem>
                        {this.props.avatar}
                    </PageHeaderToolsItem>
                </PageHeaderToolsGroup>
            </PageHeaderTools>
        );
    }
}
