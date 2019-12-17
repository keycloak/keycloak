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

import {
    Badge,
    DataList,
    DataListItem,
    DataListItemRow,
    DataListCell,
    DataListToggle,
    DataListContent,
    DataListItemCells,
    Level,
    LevelItem,
    Stack,
    StackItem
  } from '@patternfly/react-core';

import { EditAltIcon, Remove2Icon, UserCheckIcon } from '@patternfly/react-icons';

import {AccountServiceClient} from '../../account-service/account.service';
import {ShareTheResource} from "./ShareTheResource";
import {Client, PaginatedResources, Permission, Resource} from "./MyResourcesPage";
import { Msg } from '../../widgets/Msg';

export interface ResourcesTableState {
    isRowOpen: boolean[];
    permissions: Map<number, Permission[]>;
}

export interface ResourcesTableProps {
    resources: PaginatedResources;
    noResourcesMessage: string;
}

export class ResourcesTable extends React.Component<ResourcesTableProps, ResourcesTableState> {

    public constructor(props: ResourcesTableProps) {
        super(props);
        this.state = {
            isRowOpen: new Array<boolean>(props.resources.data.length).fill(false),
            permissions: new Map()
        }
    }

    private onToggle = (row: number): void => {
        const newIsRowOpen: boolean[] = this.state.isRowOpen;
        newIsRowOpen[row] = !newIsRowOpen[row];
        if (newIsRowOpen[row]) this.fetchPermissions(this.props.resources.data[row], row);
        this.setState({isRowOpen: newIsRowOpen});
    };

    private fetchPermissions(resource: Resource, row: number): void {
        console.log('**** fetchPermissions');
        AccountServiceClient.Instance.doGet('resources/' + resource._id + '/permissions')
          .then((response: AxiosResponse<Permission[]>) => {
            console.log('Fetching Permissions row: ' + row);
            console.log({response});
            const newPermissions: Map<number, Permission[]> = new Map(this.state.permissions);
            newPermissions.set(row, response.data);
            this.setState({permissions: newPermissions});
          }
        );
    }

    private hasPermissions(row: number): boolean {
        return (this.state.permissions.has(row)) && (this.state.permissions.get(row)!.length > 0);
    }

    private firstUser(row: number): string {
        if (!this.hasPermissions(row)) return 'ERROR!!!!'; // should never happen

        return this.state.permissions.get(row)![0].username;
    }

    private numOthers(row: number): number {
        if (!this.hasPermissions(row)) return -1; // should never happen

        return this.state.permissions.get(row)!.length - 1;
    }

    public sharedWithUsersMessage(row: number): React.ReactNode {
        if (!this.hasPermissions(row)) return (<React.Fragment><Msg msgKey='resourceNotShared'/></React.Fragment>);

        // TODO: Not using a parameterized message because I want to use <strong> tag.  Need to figure out a good solution to this.
        if (this.numOthers(row) > 0) {
            return (<React.Fragment><Msg msgKey='resourceSharedWith'/> <strong>{this.firstUser(row)}</strong> <Msg msgKey='and'/> <strong>{this.numOthers(row)}</strong> <Msg msgKey='otherUsers'/>.</React.Fragment>)
        } else {
            return (<React.Fragment><Msg msgKey='resourceSharedWith'/> <strong>{this.firstUser(row)}</strong>.</React.Fragment>)
        }
    }

    public render(): React.ReactNode {
        return (
            <DataList aria-label={Msg.localize('resources')}>
                <DataListItem key='resource-header' aria-labelledby='resource-header'>
                    <DataListItemRow>
                        // invisible toggle allows headings to line up properly
                        <span style={{visibility: 'hidden'}}>
                            <DataListToggle
                                isExpanded={false}
                                id='resource-header-invisible-toggle'
                                aria-controls="ex-expand1"
                            />
                        </span>
                        <DataListItemCells
                            dataListCells={[
                                <DataListCell key='resource-name-header' width={5}>
                                    <strong><Msg msgKey='resourceName'/></strong>
                                </DataListCell>,
                                <DataListCell key='application-name-header' width={5}>
                                    <strong><Msg msgKey='application'/></strong>
                                </DataListCell>,
                                <DataListCell key='permission-request-header' width={5}>
                                    <strong><Msg msgKey='permissionRequests'/></strong>
                                </DataListCell>,
                            ]}
                        />
                    </DataListItemRow>
                </DataListItem>
                {(this.props.resources.data.length === 0) && <Msg msgKey={this.props.noResourcesMessage}/>}
                {this.props.resources.data.map((resource: Resource, row: number) => {
                    return (
                    <DataListItem key={'resource-' + row} aria-labelledby={resource.name} isExpanded={this.state.isRowOpen[row]}>
                        <DataListItemRow>
                            <DataListToggle
                                onClick={()=>this.onToggle(row)}
                                isExpanded={this.state.isRowOpen[row]}
                                id={'resourceToggle-' + row}
                                aria-controls="ex-expand1"
                            />
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell key={'resourceName-' + row} width={5}>
                                        <Msg msgKey={resource.name}/>
                                    </DataListCell>,
                                    <DataListCell key={'resourceClient-' + row} width={5}>
                                        <a href={resource.client.baseUrl}>{this.getClientName(resource.client)}</a>
                                    </DataListCell>,
                                    <DataListCell key={'permissionRequests-' + row} width={5}>
                                    {resource.shareRequests.length > 0 && <a href={resource.client.baseUrl}><UserCheckIcon size='lg'/><Badge>{resource.shareRequests.length}</Badge></a>}
                                    </DataListCell>
                                ]}
                            />
                        </DataListItemRow>
                        <DataListContent
                            noPadding={false}
                            aria-label="Session Details"
                            id="ex-expand1"
                            isHidden={!this.state.isRowOpen[row]}
                        >
                            <Stack gutter='md'>
                                <StackItem isFilled>
                                    <Level gutter='md'>
                                        <LevelItem><span/></LevelItem>
                                        <LevelItem>{this.sharedWithUsersMessage(row)}</LevelItem>
                                        <LevelItem><span/></LevelItem>
                                    </Level>
                                </StackItem>
                                <StackItem isFilled>
                                    <Level gutter='md'>
                                        <LevelItem><span/></LevelItem>
                                        <LevelItem>
                                            <ShareTheResource resource={resource} 
                                                              permissions={this.state.permissions.get(row)!} 
                                                              sharedWithUsersMsg={this.sharedWithUsersMessage(row)}
                                                              onClose={this.fetchPermissions.bind(this)}
                                                              row={row}/>
                                        </LevelItem>
                                        <LevelItem><EditAltIcon/> Edit</LevelItem>
                                        <LevelItem><Remove2Icon/> Remove</LevelItem>
                                        <LevelItem><span/></LevelItem>
                                    </Level>
                                </StackItem>
                            </Stack>
                        </DataListContent>
                    </DataListItem>
                )})}
                            
            </DataList>
        );
    }

    private getClientName(client: Client): string {
        if (client.hasOwnProperty('name') && client.name !== null && client.name !== '') {
            return Msg.localize(client.name!);
        } else {
            return client.clientId;
        }
    }
}