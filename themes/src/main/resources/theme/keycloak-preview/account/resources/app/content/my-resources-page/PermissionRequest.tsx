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
import { Button, Modal, Text, Badge, DataListItem, DataList, TextVariants, DataListItemRow, DataListItemCells, DataListCell, Chip } from '@patternfly/react-core';
import { UserCheckIcon } from '@patternfly/react-icons';

import { AccountServiceClient } from '../../account-service/account.service';
import { Msg } from '../../widgets/Msg';
import { ContentAlert } from '../ContentAlert';
import { Resource, Scope, Permission } from './MyResourcesPage';


interface PermissionRequestProps {
    resource: Resource;
    onClose: () => void;
}

interface PermissionRequestState {
    isOpen: boolean;
}

export class PermissionRequest extends React.Component<PermissionRequestProps, PermissionRequestState> {
    protected static defaultProps = { permissions: [], row: 0 };

    public constructor(props: PermissionRequestProps) {
        super(props);

        this.state = {
            isOpen: false,
        };
    }

    private handleApprove = async (username: string, scopes: Scope[]) => {
        this.handle(username, scopes, true);
    };

    private handleDeny = async (username: string, scopes: Scope[]) => {
        this.handle(username, scopes);
    }

    private handle = async (username: string, scopes: Scope[], approve: boolean = false) => {
        const id = this.props.resource._id
        this.handleToggleDialog();

        const permissionsRequest = await AccountServiceClient.Instance.doGet(`/resources/${id}/permissions`);
        const userScopes = permissionsRequest.data.find((p: Permission) => p.username === username).scopes;
        if (approve) {
            userScopes.push(...scopes);
        }
        try {
            await AccountServiceClient.Instance.doPut(`/resources/${id}/permissions`, { data: [{ username: username, scopes: userScopes }] })
            ContentAlert.success(Msg.localize('shareSuccess'));
            this.props.onClose();
        } catch (e) {
            console.error('Could not update permissions', e.error);
        }
    }

    private handleToggleDialog = () => {
        if (this.state.isOpen) {
            this.setState({ isOpen: false });
        } else {
            this.setState({ isOpen: true });
        }
    };

    public render(): React.ReactNode {
        return (
            <React.Fragment>
                <Button variant="link" onClick={this.handleToggleDialog}>
                    <UserCheckIcon size="lg" />
                    <Badge>{this.props.resource.shareRequests.length}</Badge>
                </Button>

                <Modal
                    title={'Permission requests - ' + this.props.resource.name}
                    isLarge={true}
                    isOpen={this.state.isOpen}
                    onClose={this.handleToggleDialog}
                    actions={[
                        <Button key="close" variant="link" onClick={this.handleToggleDialog}>
                            <Msg msgKey="close" />
                        </Button>,
                    ]}
                >
                    <DataList aria-label={Msg.localize('permissionRequests')}>
                        <DataListItemRow>
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell key='permissions-name-header' width={5}>
                                        <strong>Requestor</strong>
                                    </DataListCell>,
                                    <DataListCell key='permissions-requested-header' width={5}>
                                        <strong><Msg msgKey='permissions' /> requested</strong>
                                    </DataListCell>,
                                    <DataListCell key='permission-request-header' width={5}>
                                    </DataListCell>
                                ]}
                            />
                        </DataListItemRow>
                        {this.props.resource.shareRequests.map((shareRequest, i) =>
                            <DataListItem key={i} aria-labelledby="requestor">
                                <DataListItemRow>
                                    <DataListItemCells
                                        dataListCells={[
                                            <DataListCell key={`requestor${i}`}>
                                                <span>{shareRequest.firstName} {shareRequest.lastName}</span><br />
                                                <Text component={TextVariants.small}>{shareRequest.email}</Text>
                                            </DataListCell>,
                                            <DataListCell key={`permissions${i}`}>
                                                {shareRequest.scopes.map((scope, j) => <Chip key={j} isReadOnly>{scope}</Chip>)}
                                            </DataListCell>,
                                            <DataListCell key={`actions${i}`}>
                                                <Button onClick={() => this.handleApprove(shareRequest.username, shareRequest.scopes)}>
                                                    Accept
                                                </Button>
                                                <Button variant="danger" onClick={() => this.handleDeny(shareRequest.username, shareRequest.scopes)}>
                                                    Deny
                                                </Button>
                                            </DataListCell>
                                        ]}
                                    />
                                </DataListItemRow>
                            </DataListItem>
                        )}
                    </DataList>
                </Modal>
            </React.Fragment>
        );
    }
}