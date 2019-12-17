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
import {AxiosResponse} from 'axios';

import * as parse from 'parse-link-header';

import { Button, Level, LevelItem, Stack, StackItem, Tab, Tabs, TextInput } from '@patternfly/react-core';

import {AccountServiceClient} from '../../account-service/account.service';

import {ResourcesTable} from './ResourcesTable';
import {ContentPage} from '../ContentPage';
import {Msg} from '../../widgets/Msg';

export interface MyResourcesPageProps {
}

export interface MyResourcesPageState {
    activeTabKey: number;
    isModalOpen: boolean;
    nameFilter: string;
    myResources: PaginatedResources;
    sharedWithMe: PaginatedResources;
}

export interface Resource {
    _id: string;
    name: string;
    client: Client;
    scopes: Scope[];
    uris: string[];
    shareRequests: Permission[];
}

export interface Client {
    baseUrl: string;
    clientId: string;
    name?: string;
}

export class Scope {
    public constructor(public name: string, public displayName?: string) {}

    public toString(): string {
        if (this.hasOwnProperty('displayName') && (this.displayName)) {
            return this.displayName;
        } else {
            return this.name;
        }
    }
}

export interface PaginatedResources {
    nextUrl: string;
    prevUrl: string;
    data: Resource[];
}

export interface Permission {
    email?: string;
    firstName?: string;
    lastName?: string;
    scopes: Scope[];  // this should be Scope[] - fix API
    username: string;
}

const MY_RESOURCES_TAB = 0;
const SHARED_WITH_ME_TAB = 1;

export class MyResourcesPage extends React.Component<MyResourcesPageProps, MyResourcesPageState> {
    private first = 0;
    private max = 5;

    public constructor(props: MyResourcesPageProps) {
        super(props);
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
        console.log('prev url=' + this.state.myResources.prevUrl);
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
        AccountServiceClient.Instance.doGet(url, 
                                            {params: extraParams}
                                            )
            .then((response: AxiosResponse<Resource[]>) => {
                const resources: Resource[] = response.data;
                resources.forEach((resource: Resource) => resource.shareRequests = []);

                // serialize the Scope objects from JSON so that toString() will work.
                resources.forEach((resource: Resource) => resource.scopes = resource.scopes.map(this.makeScopeObj));

                if (this.isSharedWithMeTab()) {
                    console.log('Shared With Me Resources: ');
                    this.setState({sharedWithMe: this.parseResourceResponse(response)});
                } else {
                    console.log('MyResources: ');
                    this.setState({myResources: this.parseResourceResponse(response)}, this.fetchPermissionRequests);
                }

                console.log({response});
            })
    }

    private makeScopeObj = (scope: Scope): Scope => {
        return new Scope(scope.name, scope.displayName);
    }

    private fetchPermissionRequests = () => {
        console.log('fetch permission requests');
        this.state.myResources.data.forEach((resource: Resource) => {
            this.fetchShareRequests(resource);
        });
    }

    private fetchShareRequests(resource: Resource): void {
        AccountServiceClient.Instance.doGet('/resources/' + resource._id + '/permissions/requests')
            .then((response: AxiosResponse<Permission[]>) => {
                //console.log('Share requests for ' + resource.name);
                //console.log({response});
                resource.shareRequests = response.data;
                if (resource.shareRequests.length > 0) {
                    //console.log('forcing update');
                    this.forceUpdate();
                }
            });
    }

    private parseResourceResponse(response: AxiosResponse<Resource[]>): PaginatedResources {
        const links: string = response.headers.link;
        const parsed: (parse.Links | null) = parse(links);

        let next = '';
        let prev = '';

        if (parsed !== null) {
            if (parsed.hasOwnProperty('next')) next = parsed.next.url;
            if (parsed.hasOwnProperty('prev')) prev = parsed.prev.url;
        }

        const resources = response.data;

        return {nextUrl: next, prevUrl: prev, data: resources};
    }

    private makeTab(eventKey: number, title: string, resources: PaginatedResources, noResourcesMessage: string): React.ReactNode {
        return (
            <Tab eventKey={eventKey} title={Msg.localize(title)}>
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
                        <ResourcesTable resources={resources} noResourcesMessage={noResourcesMessage}/>
                    </StackItem>
                </Stack>
            </Tab>
        )
    }

    public render(): React.ReactNode {
        return (
            <ContentPage title="resources" onRefresh={this.fetchInitialResources.bind(this)}>
                <Tabs isFilled activeKey={this.state.activeTabKey} onSelect={this.handleTabClick}>
                    {this.makeTab(0, 'myResources', this.state.myResources, 'notHaveAnyResource')}
                    {this.makeTab(1, 'sharedwithMe', this.state.sharedWithMe, 'noResourcesSharedWithYou')}
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
