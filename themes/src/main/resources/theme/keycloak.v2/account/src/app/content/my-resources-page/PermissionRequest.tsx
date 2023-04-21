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
    Text,
    Badge,
    DataListItem,
    DataList,
    TextVariants,
    DataListItemRow,
    DataListItemCells,
    DataListCell,
    Chip,
    Split,
    SplitItem,
    ModalVariant
} from '@patternfly/react-core';
import { UserCheckIcon } from '@patternfly/react-icons';

import { HttpResponse } from '../../account-service/account.service';
import { AccountServiceContext } from '../../account-service/AccountServiceContext';
import { Msg } from '../../widgets/Msg';
import { ContentAlert } from '../ContentAlert';
import { Resource, Scope, Permission, Permissions } from './resource-model';


interface PermissionRequestProps {
    resource: Resource;
    onClose: () => void;
}

interface PermissionRequestState {
    isOpen: boolean;
}

export class PermissionRequest extends React.Component<PermissionRequestProps, PermissionRequestState> {
    protected static defaultProps:Permissions = { permissions: [], row: 0 };
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(props: PermissionRequestProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;
    
        this.state = {
            isOpen: false,
        };
    }

    private handleApprove = async (shareRequest: Permission, index: number) => {
        this.handle(shareRequest.username, shareRequest.scopes as Scope[], true);
        this.props.resource.shareRequests.splice(index, 1);
    };

    private handleDeny = async (shareRequest: Permission, index: number) => {
        this.handle(shareRequest.username, shareRequest.scopes as Scope[]);
        this.props.resource.shareRequests.splice(index, 1)
    };

    private handle = async (username: string, scopes: Scope[], approve: boolean = false) => {
        const id = this.props.resource._id
        this.handleToggleDialog();

        const permissionsRequest: HttpResponse<Permission[]> = await this.context!.doGet(`/resources/${encodeURIComponent(id)}/permissions`);
        const permissions = permissionsRequest.data || [];
        const foundPermission = permissions.find(p => p.username === username);
        const userScopes = foundPermission ? (foundPermission.scopes as Scope[]): [];
        if (approve) {
            userScopes.push(...scopes);
        }
        try {
            await this.context!.doPut(`/resources/${encodeURIComponent(id)}/permissions`, [{ username: username, scopes: userScopes }] )
            ContentAlert.success(Msg.localize('shareSuccess'));
            this.props.onClose();
        } catch (e) {
            console.error('Could not update permissions', e.error);
        }
    };

    private handleToggleDialog = () => {
        this.setState({ isOpen: !this.state.isOpen });
    };

    public render(): React.ReactNode {
        const id = `shareRequest-${this.props.resource.name.replace(/\s/, '-')}`;
        return (
            <React.Fragment>
                <Button id={id} variant="link" onClick={this.handleToggleDialog}>
                    <UserCheckIcon size="lg" />
                    <Badge>{this.props.resource.shareRequests.length}</Badge>
                </Button>

                <Modal
                    id={`modal-${id}`}
                    title={Msg.localize('permissionRequests') + ' - ' + this.props.resource.name}
                    variant={ModalVariant.large}
                    isOpen={this.state.isOpen}
                    onClose={this.handleToggleDialog}
                    actions={[
                        <Button id={`close-${id}`} key="close" variant="link" onClick={this.handleToggleDialog}>
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
                                        <strong><Msg msgKey='permissionRequests' /></strong>
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
                                            <DataListCell id={`requestor${i}`} key={`requestor${i}`}>
                                                <span>
                                                    {shareRequest.firstName} {shareRequest.lastName} {shareRequest.lastName ? '' : shareRequest.username}
                                                </span><br />
                                                <Text component={TextVariants.small}>{shareRequest.email}</Text>
                                            </DataListCell>,
                                            <DataListCell id={`permissions${i}`} key={`permissions${i}`}>
                                                {(shareRequest.scopes as Scope[]).map((scope, j) => <Chip key={j} isReadOnly>{scope}</Chip>)}
                                            </DataListCell>,
                                            <DataListCell key={`actions${i}`}>
                                                <Split hasGutter>
                                                    <SplitItem>
                                                        <Button
                                                            id={`accept-${i}-${id}`}
                                                            onClick={() => this.handleApprove(shareRequest, i)}
                                                        >
                                                            Accept
                                                        </Button>
                                                    </SplitItem>
                                                    <SplitItem>
                                                        <Button
                                                            id={`deny-${i}-${id}`}
                                                            variant="danger"
                                                            onClick={() => this.handleDeny(shareRequest, i)}
                                                        >
                                                            Deny
                                                        </Button>
                                                    </SplitItem>
                                                </Split>
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
