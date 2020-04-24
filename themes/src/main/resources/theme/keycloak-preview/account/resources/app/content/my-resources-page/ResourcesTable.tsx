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
    StackItem,
    Button
  } from '@patternfly/react-core';

import { Remove2Icon } from '@patternfly/react-icons';

import {AccountServiceClient} from '../../account-service/account.service';
import {PermissionRequest} from "./PermissionRequest";
import {ShareTheResource} from "./ShareTheResource";
import {Permission, Resource} from "./MyResourcesPage";
import { Msg } from '../../widgets/Msg';
import { ResourcesTableState, ResourcesTableProps, AbstractResourcesTable } from './AbstractResourceTable';
import { EditTheResource } from './EditTheResource';
import { ContentAlert } from '../ContentAlert';

export interface CollapsibleResourcesTableState extends ResourcesTableState {
    isRowOpen: boolean[];
}

export class ResourcesTable extends AbstractResourcesTable<CollapsibleResourcesTableState> {

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
        AccountServiceClient.Instance.doGet('resources/' + resource._id + '/permissions')
          .then((response: AxiosResponse<Permission[]>) => {
            const newPermissions: Map<number, Permission[]> = new Map(this.state.permissions);
            newPermissions.set(row, response.data);
            this.setState({permissions: newPermissions});
          }
        );
    }

    private removeShare(resource: Resource, row: number): void {
        const permissions = this.state.permissions.get(row)!.map(a => ({ username: a.username, scopes: [] }));
        AccountServiceClient.Instance.doPut(`/resources/${resource._id}/permissions`, { data: permissions })
            .then(() => {
                ContentAlert.success(Msg.localize('shareSuccess'));
                this.onToggle(row);
            });
    }

    public render(): React.ReactNode {
        return (
            <DataList aria-label={Msg.localize('resources')} id="resourcesList">
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
                {(this.props.resources.data.length === 0) && <Msg msgKey="notHaveAnyResource"/>}
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
                                        {resource.shareRequests.length > 0 &&
                                            <PermissionRequest
                                                resource={resource}
                                                onClose={() => this.fetchPermissions(resource, row)}
                                            ></PermissionRequest>
                                        }
                                    </DataListCell>
                                ]}
                            />
                        </DataListItemRow>
                        <DataListContent
                            noPadding={false}
                            aria-label="Session Details"
                            id={'ex-expand' + row}
                            isHidden={!this.state.isRowOpen[row]}
                        >
                            <Stack gutter='md'>
                                <StackItem isFilled>
                                    <Level gutter='md'>
                                        <LevelItem><span/></LevelItem>
                                        <LevelItem id={'shared-with-user-message-' + row}>{this.sharedWithUsersMessage(row)}</LevelItem>
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
                                        <LevelItem>
                                            <EditTheResource resource={resource} permissions={this.state.permissions.get(row)!} row={row} onClose={this.fetchPermissions.bind(this)}/>
                                        </LevelItem>
                                        <LevelItem>
                                            <Button
                                                isDisabled={this.numOthers(row) < 0}
                                                variant="link"
                                                onClick={() => this.removeShare(resource, row)}
                                            >
                                                <Remove2Icon/> Remove
                                            </Button>
                                        </LevelItem>
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
}