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

import {
    Button,
    Modal,
    DataList,
    DataListItemRow,
    DataListItemCells,
    DataListCell,
    DataListItem,
    ChipGroup,
    ChipGroupToolbarItem,
    Chip
} from '@patternfly/react-core';

import { EditAltIcon } from '@patternfly/react-icons';

import { Resource, Permission, Scope } from './MyResourcesPage';
import { Msg } from '../../widgets/Msg';
import { AccountServiceClient } from '../../account-service/account.service';
import { ContentAlert } from '../ContentAlert';

interface EditTheResourceProps {
    resource: Resource;
    permissions: Permission[];
    onClose: (resource: Resource, row: number) => void;
    row: number;
}

interface EditTheResourceState {
    isOpen: boolean;
}

export class EditTheResource extends React.Component<EditTheResourceProps, EditTheResourceState> {
    protected static defaultProps = { permissions: [], row: 0 };

    public constructor(props: EditTheResourceProps) {
        super(props);

        this.state = {
            isOpen: false,
        };
    }

    private clearState(): void {
        this.setState({
        });
    }

    private handleToggleDialog = () => {
        if (this.state.isOpen) {
            this.setState({ isOpen: false });
        } else {
            this.clearState();
            this.setState({ isOpen: true });
        }
    };

    async deletePermission(permission: Permission, scope: Scope): Promise<void> {
        permission.scopes.splice(permission.scopes.indexOf(scope), 1);
        await AccountServiceClient.Instance.doPut(`/resources/${this.props.resource._id}/permissions`, {data: [permission]});
        ContentAlert.success(Msg.localize('shareSuccess'));
        this.props.onClose(this.props.resource, this.props.row);
    }

    public render(): React.ReactNode {
        return (
            <React.Fragment>
                <Button variant="link" onClick={this.handleToggleDialog}>
                    <EditAltIcon /> Edit
                </Button>

                <Modal
                    title={'Edit the resource - ' + this.props.resource.name}
                    isLarge={true}
                    isOpen={this.state.isOpen}
                    onClose={this.handleToggleDialog}
                    actions={[
                        <Button key="done" variant="link" id="done" onClick={this.handleToggleDialog}>
                            <Msg msgKey='done' />
                        </Button>,
                    ]}
                >
                    <DataList aria-label={Msg.localize('resources')}>
                        <DataListItemRow>
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell key='resource-name-header' width={3}>
                                        <strong><Msg msgKey='User' /></strong>
                                    </DataListCell>,
                                    <DataListCell key='permissions-header' width={5}>
                                        <strong><Msg msgKey='permissions' /></strong>
                                    </DataListCell>,
                                ]}
                            />
                        </DataListItemRow>
                        {this.props.permissions.map((p, row) => {
                            return (
                                <DataListItem key={'resource-' + row} aria-labelledby={p.username}>
                                    <DataListItemRow>
                                        <DataListItemCells
                                            dataListCells={[
                                                <DataListCell key={'userName-' + row} width={5}>
                                                    {p.username}
                                                </DataListCell>,
                                                <DataListCell key={'permission-' + row} width={5}>
                                                    <ChipGroup>
                                                        <ChipGroupToolbarItem key='permissions' categoryName={Msg.localize('permissions')}>
                                                            {
                                                                p.scopes.length > 0 && p.scopes.map(scope => (
                                                                    <Chip key={scope.toString()} onClick={() => this.deletePermission(p, scope)}>
                                                                        {scope.displayName || scope}
                                                                    </Chip>
                                                                ))
                                                            }
                                                        </ChipGroupToolbarItem>
                                                    </ChipGroup>
                                                </DataListCell>
                                            ]}
                                        />
                                    </DataListItemRow>
                                </DataListItem>
                            );
                        })}
                    </DataList>
                </Modal>
            </React.Fragment>
        );
    }
}