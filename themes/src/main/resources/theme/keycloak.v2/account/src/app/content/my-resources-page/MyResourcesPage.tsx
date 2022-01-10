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

import parse from '../../util/ParseLink';

import { Button, Level, LevelItem, Stack, StackItem, Tab, Tabs, TextInput } from '@patternfly/react-core';

import {HttpResponse} from '../../account-service/account.service';
import {AccountServiceContext} from '../../account-service/AccountServiceContext';

import { PaginatedResources, Resource, Scope, Permission } from './resource-model';
import {ResourcesTable} from './ResourcesTable';
import {ContentPage} from '../ContentPage';
import {Msg} from '../../widgets/Msg';
import { SharedResourcesTable } from './SharedResourcesTable';

export interface MyResourcesPageProps {
}

export interface MyResourcesPageState {
    activeTabKey: number;
    isModalOpen: boolean;
    nameFilter: string;
    myResources: PaginatedResources;
    sharedWithMe: PaginatedResources;
}

const MY_RESOURCES_TAB = 0;
const SHARED_WITH_ME_TAB = 1;

export class MyResourcesPage extends React.Component<MyResourcesPageProps, MyResourcesPageState> {
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;
    private first = 0;
    private max = 5;

    public constructor(props: MyResourcesPageProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;

        this.state = {
            activeTabKey: MY_RESOURCES_TAB,
            nameFilter: '',
            isModalOpen: false,
            myResources: {nextUrl: '', prevUrl: '', data: []},
            sharedWithMe: {nextUrl: '', prevUrl: '', data: []}
        };

        this.fetchInitialResources();
    }

    private isSharedWithMeTab(): boolean {
        return this.state.activeTabKey === SHARED_WITH_ME_TAB;
    }

    private hasNext(): boolean {
        if (this.isSharedWithMeTab()) {
            return (this.state.sharedWithMe.nextUrl !== null) && (this.state.sharedWithMe.nextUrl !== '');
        } else {
            return (this.state.myResources.nextUrl !== null) && (this.state.myResources.nextUrl !== '');
        }
    }

    private hasPrevious(): boolean {
        if (this.isSharedWithMeTab()) {
            return (this.state.sharedWithMe.prevUrl !== null) && (this.state.sharedWithMe.prevUrl !== '');
        } else {
            return (this.state.myResources.prevUrl !== null) && (this.state.myResources.prevUrl !== '');
        }
    }

    private fetchInitialResources(): void {
        if (this.isSharedWithMeTab()) {
            this.fetchResources("/resources/shared-with-me");
        } else {
            this.fetchResources("/resources", {first: this.first, max: this.max});
        }
    }

    private fetchFilteredResources(params: Record<string, string|number>): void {
        if (this.isSharedWithMeTab()) {
            this.fetchResources("/resources/shared-with-me", params);
        } else {
            this.fetchResources("/resources", {...params, first: this.first, max: this.max});
        }
    }

    private fetchResources(url: string, extraParams?: Record<string, string|number>): void {
        this.context!.doGet<Resource[]>(url, {params: extraParams})
            .then((response: HttpResponse<Resource[]>) => {
                const resources: Resource[] = response.data || [];
                resources.forEach((resource: Resource) => resource.shareRequests = []);

                // serialize the Scope objects from JSON so that toString() will work.
                resources.forEach((resource: Resource) => resource.scopes = resource.scopes.map(this.makeScopeObj));

                if (this.isSharedWithMeTab()) {
                    this.setState({sharedWithMe: this.parseResourceResponse(response)}, this.fetchPending);
                } else {
                    this.setState({myResources: this.parseResourceResponse(response)}, this.fetchPermissionRequests);
                }
            });
    }

    private makeScopeObj = (scope: Scope): Scope => {
        return new Scope(scope.name, scope.displayName);
    }

    private fetchPermissionRequests = () => {
        this.state.myResources.data.forEach((resource: Resource) => {
            this.fetchShareRequests(resource);
        });
    }

    private fetchShareRequests(resource: Resource): void {
        this.context!.doGet('/resources/' + resource._id + '/permissions/requests')
            .then((response: HttpResponse<Permission[]>) => {
                resource.shareRequests = response.data || [];
                if (resource.shareRequests.length > 0) {
                    this.forceUpdate();
                }
            });
    }

    private fetchPending = async () => {
        const response: HttpResponse<Resource[]> = await this.context!.doGet(`/resources/pending-requests`);
        const resources: Resource[] = response.data || [];
        resources.forEach((pendingRequest: Resource) => {
            this.state.sharedWithMe.data.forEach(resource => {
                if (resource._id === pendingRequest._id) {
                    resource.shareRequests = [{username: 'me', scopes: pendingRequest.scopes}]
                    this.forceUpdate();
                }
            });
        });
    }

    private parseResourceResponse(response: HttpResponse<Resource[]>): PaginatedResources {
        const links: string | undefined = response.headers.get('link') || undefined;
        const parsed = parse(links);

        let next = '';
        let prev = '';

        if (parsed !== null) {
            if (parsed.next) next = parsed.next;
            if (parsed.prev) prev = parsed.prev;
        }

        const resources: Resource[] = response.data || [];

        return {nextUrl: next, prevUrl: prev, data: resources};
    }

    private makeTab(eventKey: number, title: string, resources: PaginatedResources, sharedResourcesTab: boolean): React.ReactNode {
        return (
            <Tab id={title} eventKey={eventKey} title={Msg.localize(title)}>
                <Stack gutter="md">
                    <StackItem isFilled><span/></StackItem>
                    <StackItem isFilled>
                        <Level gutter='md'>
                            <LevelItem>
                                <TextInput value={this.state.nameFilter} onChange={this.handleFilterRequest} id={'filter-' + title} type="text" placeholder={Msg.localize('filterByName')} />
                            </LevelItem>
                        </Level>
                    </StackItem>
                    <StackItem isFilled>
                        {!sharedResourcesTab && <ResourcesTable resources={resources}/>}
                        {sharedResourcesTab && <SharedResourcesTable resources={resources}/>}
                    </StackItem>
                </Stack>
            </Tab>
        )
    }

    public render(): React.ReactNode {
        return (
            <ContentPage title="resources" onRefresh={this.fetchInitialResources.bind(this)}>
                <Tabs isFilled activeKey={this.state.activeTabKey} onSelect={this.handleTabClick}>
                    {this.makeTab(0, 'myResources', this.state.myResources, false)}
                    {this.makeTab(1, 'sharedwithMe', this.state.sharedWithMe, true)}
                </Tabs>

                <Level gutter='md'>
                    <LevelItem>
                        {this.hasPrevious() && <Button onClick={this.handlePreviousClick}>&lt;<Msg msgKey='previousPage'/></Button>}
                    </LevelItem>

                    <LevelItem>
                        {this.hasPrevious() && <Button onClick={this.handleFirstPageClick}><Msg msgKey='firstPage'/></Button>}
                    </LevelItem>

                    <LevelItem>
                        {this.hasNext() && <Button onClick={this.handleNextClick}><Msg msgKey='nextPage'/>&gt;</Button>}
                    </LevelItem>
                </Level>
            </ContentPage>
        );
    }

    private handleFilterRequest = (value: string) => {
        this.setState({nameFilter: value});
        this.fetchFilteredResources({name: value});
    }

    private clearNextPrev(): void {
        const newMyResources: PaginatedResources = this.state.myResources;
        newMyResources.nextUrl = '';
        newMyResources.prevUrl = '';
        this.setState({myResources: newMyResources});
    }

    private handleFirstPageClick = () => {
        this.fetchInitialResources();
    }

    private handleNextClick = () => {
        if (this.isSharedWithMeTab()) {
            this.fetchResources(this.state.sharedWithMe.nextUrl);
        } else {
            this.fetchResources(this.state.myResources.nextUrl);
        }
    }

    private handlePreviousClick = () => {
        if (this.isSharedWithMeTab()) {
            this.fetchResources(this.state.sharedWithMe.prevUrl);
        } else {
            this.fetchResources(this.state.myResources.prevUrl);
        }
    }

    private handleTabClick = (event: React.MouseEvent<HTMLInputElement>, tabIndex: number) => {
        if (this.state.activeTabKey === tabIndex) return;

        this.setState({
            nameFilter: '',
            activeTabKey: tabIndex
        }, () => {this.fetchInitialResources()});
    };
};
