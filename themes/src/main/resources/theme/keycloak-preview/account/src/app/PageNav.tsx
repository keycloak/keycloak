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
import {Nav, NavList} from '@patternfly/react-core';

import {makeNavItems, flattenContent, ContentItem, PageDef} from './ContentPages';

declare const content: ContentItem[];

export interface PageNavProps extends RouteComponentProps {}

export interface PageNavState {}

class PageNavigation extends React.Component<PageNavProps, PageNavState> {

    public constructor(props: PageNavProps) {
        super(props);
    }

    private findActiveItem(): PageDef {
        const currentPath: string = this.props.location.pathname;
        const items: PageDef[] = flattenContent(content);
        const firstItem = items[0];
        for (let item of items) {
            const itemPath: string = '/' + item.path;
            if (itemPath === currentPath) {
                return item;
            }
        };

        return firstItem;
    }

    public render(): React.ReactNode {
        const activeItem: PageDef = this.findActiveItem();
        return (
            <Nav>
                <NavList>
                    {makeNavItems(activeItem)}
                </NavList>
            </Nav>
        );
    }
}

export const PageNav = withRouter(PageNavigation);
