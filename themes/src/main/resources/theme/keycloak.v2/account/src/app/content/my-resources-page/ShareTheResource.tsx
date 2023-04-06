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
    Chip,
    ChipGroup,
    Form,
    FormGroup,
    Gallery,
    GalleryItem,
    Modal,
    Stack,
    StackItem,
    TextInput,
    ModalVariant
} from '@patternfly/react-core';

import { AccountServiceContext } from '../../account-service/AccountServiceContext';
import { Resource, Permission, Scope } from './resource-model';
import { Msg } from '../../widgets/Msg';
import {ContentAlert} from '../ContentAlert';
import { PermissionSelect } from './PermissionSelect';

interface ShareTheResourceProps {
    resource: Resource;
    permissions: Permission[];
    sharedWithUsersMsg: React.ReactNode;
    onClose: () => void;
    children: (toggle: () => void) => void;
}

interface ShareTheResourceState {
    isOpen: boolean;
    permissionsSelected: Scope[];
    permissionsUnSelected: Scope[];
    usernames: string[];
    usernameInput: string;
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 */
export class ShareTheResource extends React.Component<ShareTheResourceProps, ShareTheResourceState> {
    protected static defaultProps:any = {permissions: []};
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(props: ShareTheResourceProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;
    
        this.state = {
            isOpen: false,
            permissionsSelected: [],
            permissionsUnSelected: this.props.resource.scopes,
            usernames: [],
            usernameInput: ''
        };
    }

    private clearState(): void {
        this.setState({
            permissionsSelected: [],
            permissionsUnSelected: this.props.resource.scopes,
            usernames: [],
            usernameInput: ''
        });
    }

    private handleAddPermission = () => {
        const rscId: string = this.props.resource._id;
        const newPermissions: string[] = [];

        for (const permission of this.state.permissionsSelected) {
            newPermissions.push(permission.name);
        }

        const permissions = [];

        for (const username of this.state.usernames) {
            permissions.push({username: username, scopes: newPermissions});
        }

        this.handleToggleDialog();

        this.context!.doPut(`/resources/${encodeURIComponent(rscId)}/permissions`, permissions)
            .then(() => {
                ContentAlert.success('shareSuccess');
                this.props.onClose();
            })
    };

    private handleToggleDialog = () => {
       if (this.state.isOpen) {
           this.setState({isOpen: false});
           this.props.onClose();
       } else {
           this.clearState();
           this.setState({isOpen: true});
       }
    };

    private handleUsernameChange = (username: string) => {
        this.setState({usernameInput: username});
    }

    private handleAddUsername = async () => {
        if ((this.state.usernameInput !== '') && (!this.state.usernames.includes(this.state.usernameInput))) {
            const response = await this.context!.doGet<{username: string}>(`/resources/${encodeURIComponent(this.props.resource._id)}/user`, { params: { value: this.state.usernameInput } });
            if (response.data && response.data.username) {
                this.setState({ usernameInput: '', usernames: [...this.state.usernames, this.state.usernameInput] });
            } else {
                ContentAlert.info('userNotFound', [this.state.usernameInput]);
            }
        }
    }

    private handleEnterKeyInAddField = (event: React.KeyboardEvent) => {
        if (event.key === "Enter") {
            event.preventDefault();
            this.handleAddUsername();
        }
    }

    private handleDeleteUsername = (username: string) => {
        const newUsernames: string[] = this.state.usernames.filter(user => user !== username);
        this.setState({usernames: newUsernames});
    }

    private isAddDisabled(): boolean {
        return this.state.usernameInput === '' || this.isAlreadyShared();
    }

    private isAlreadyShared(): boolean {
        for (let permission of this.props.permissions) {
            if (permission.username === this.state.usernameInput) return true;
        }

        return false;
    }

    private isFormInvalid(): boolean {
        return (this.state.usernames.length === 0) || (this.state.permissionsSelected.length === 0);
    }

    public render(): React.ReactNode {
        return (
            <React.Fragment>
                {this.props.children(this.handleToggleDialog)}

                <Modal
                title={'Share the resource - ' + this.props.resource.name}
                variant={ModalVariant.large}
                isOpen={this.state.isOpen}
                onClose={this.handleToggleDialog}
                actions={[
                    <Button key="cancel" variant="link" onClick={this.handleToggleDialog}>
                        <Msg msgKey='cancel'/>
                    </Button>,
                    <Button key="confirm" variant="primary" id="done" onClick={this.handleAddPermission} isDisabled={this.isFormInvalid()}>
                        <Msg msgKey='done'/>
                    </Button>
                ]}
                >
                    <Stack hasGutter>
                        <StackItem isFilled>
                        <Form>
                            <FormGroup
                                label="Add users to share your resource with"
                                type="string"
                                helperTextInvalid={Msg.localize('resourceAlreadyShared')}
                                fieldId="username"
                                isRequired
                                >
                                    <Gallery hasGutter>
                                        <GalleryItem>
                                            <TextInput
                                                value={this.state.usernameInput}
                                                id="username"
                                                aria-describedby="username-helper"
                                                placeholder="Username or email"
                                                onChange={this.handleUsernameChange}
                                                onKeyPress={this.handleEnterKeyInAddField}
                                            />
                                        </GalleryItem>
                                        <GalleryItem>
                                            <Button key="add-user" variant="primary" id="add" onClick={this.handleAddUsername} isDisabled={this.isAddDisabled()}>
                                                <Msg msgKey="add"/>
                                            </Button>
                                        </GalleryItem>

                                </Gallery>
                                <ChipGroup categoryName={Msg.localize('shareWith')}>
                                    {this.state.usernames.map((currentChip: string) => (
                                        <Chip key={currentChip} onClick={() => this.handleDeleteUsername(currentChip)}>
                                            {currentChip}
                                        </Chip>
                                    ))}
                                </ChipGroup>
                            </FormGroup>
                            <FormGroup
                                label=""
                                fieldId="permissions-selected"
                            >
                                <PermissionSelect
                                    scopes={this.state.permissionsUnSelected}
                                    onSelect={selection => this.setState({ permissionsSelected: selection })}
                                    direction="up"
                                />
                            </FormGroup>
                        </Form>
                    </StackItem>
                    <StackItem isFilled><br/></StackItem>
                    <StackItem isFilled>
                        {this.props.sharedWithUsersMsg}
                    </StackItem>

                    </Stack>
                </Modal>
            </React.Fragment>
        );
    }
}
