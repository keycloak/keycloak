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
import {withRouter, RouteComponentProps} from 'react-router-dom';

import {Msg} from './Msg';

import {
    Dropdown,
    DropdownItem,
    DropdownToggle,
    Nav,
    NavExpandable,
    NavItem,
    NavList
} from '@patternfly/react-core';

declare const locale: string;
declare const baseUrl: string;
declare const referrer: string;
declare const referrerUri: string;

interface AvailableLocale {
    locale: string; 
    label: string;
};
declare const availableLocales: [AvailableLocale];
// remove entry for current locale
const availLocales = availableLocales.filter((availableLocale: AvailableLocale) => availableLocale.locale !== locale);

let referrerFragment = '';
if ((typeof referrer !== 'undefined') && 
    (typeof referrerUri !== 'undefined')) {
    referrerFragment = '&referrer=' + referrer + '&referrer_uri=' + encodeURIComponent(referrerUri);
}
    
interface LocaleKebabItemProps extends RouteComponentProps {}
interface LocaleKebabItemState {activeGroup: string; activeItem: string}
class LocaleKebabItem extends React.Component<LocaleKebabItemProps, LocaleKebabItemState> {
    public constructor(props: LocaleKebabItemProps) {
        super(props);
        this.state = {
            activeGroup: 'locale-group',
            activeItem: ''
        };
    }
     
    public render(): React.ReactNode {        
        const appPath = this.props.location.pathname;
        const localeNavItems = availLocales.map((availableLocale: AvailableLocale) => {
            const url = baseUrl + '?kc_locale=' + availableLocale.locale + referrerFragment + '#' + appPath;
            return (<NavItem
                        id={`mobile-locale-${availableLocale.locale}`}
                        key={availableLocale.locale}
                        to={url}>
                            {availableLocale.label}
                    </NavItem> );
        });
        
        return (
            <Nav>
                <NavList>
                    <NavExpandable id="mobile-locale" title={Msg.localize('locale_' + locale)} isActive={false} groupId="locale-group">
                    {localeNavItems}
                  </NavExpandable>
                </NavList>
            </Nav>
        );
    }
};

interface LocaleDropdownComponentProps extends RouteComponentProps {}
interface LocaleDropdownComponentState {isDropdownOpen: boolean}
class LocaleDropdownComponent extends React.Component<LocaleDropdownComponentProps, LocaleDropdownComponentState> {
    public constructor(props: LocaleDropdownComponentProps) {
        super(props);
        this.state = {isDropdownOpen: false};
    }
     
    private onDropdownToggle = (isDropdownOpen: boolean) => {
        this.setState({
            isDropdownOpen
        });
    };

    private onDropdownSelect = () => {
        this.setState({
            isDropdownOpen: !this.state.isDropdownOpen
        });
    };

    public render(): React.ReactNode {
        const appPath = this.props.location.pathname;
        const localeDropdownItems = availLocales.map((availableLocale: AvailableLocale) => {
            const url = baseUrl + '?kc_locale=' + availableLocale.locale + referrerFragment + '#' + appPath;
            return (<DropdownItem
                        id={`locale-${availableLocale.locale}`}
                        key={availableLocale.locale}
                        href={url}>
                            {availableLocale.label}
                    </DropdownItem> );
        });

        if (localeDropdownItems.length < 2) return (<React.Fragment/>);
        
        return (
            <Dropdown
                isPlain
                position="right"
                onSelect={this.onDropdownSelect}
                isOpen={this.state.isDropdownOpen}
                toggle={<DropdownToggle id="locale-dropdown-toggle" onToggle={this.onDropdownToggle}><Msg msgKey={'locale_' + locale}/></DropdownToggle>}
                dropdownItems={localeDropdownItems}
            />
        );
    }
};

export const LocaleDropdown = withRouter(LocaleDropdownComponent);
export const LocaleNav = withRouter(LocaleKebabItem);