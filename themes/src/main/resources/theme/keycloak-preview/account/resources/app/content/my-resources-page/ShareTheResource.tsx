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
    ChipGroupToolbarItem,
    Form, 
    FormGroup, 
    Gallery,
    GalleryItem,
    Modal, 
    Stack,
    StackItem,
    TextInput
} from '@patternfly/react-core';

import { ShareAltIcon } from '@patternfly/react-icons';

import {AccountServiceClient} from '../../account-service/account.service';
import { Resource, Permission, Scope } from './MyResourcesPage';
import { Msg } from '../../widgets/Msg';
import {ContentAlert} from '../ContentAlert';

interface ShareTheResourceProps {
    resource: Resource;
    permissions: Permission[];
    sharedWithUsersMsg: React.ReactNode;
    onClose: (resource: Resource, row: number) => void;
    row: number;
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
    protected static defaultProps = {permissions: [], row: 0};

    public constructor(props: ShareTheResourceProps) {
        super(props);

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

        AccountServiceClient.Instance.doPut('/resources/' + rscId + '/permissions', {data: permissions})
            .then(() => {
                ContentAlert.success(Msg.localize('shareSuccess'));
                this.props.onClose(this.props.resource, this.props.row);
            })
    };

    private handleToggleDialog = () => {
       if (this.state.isOpen) {
           this.setState({isOpen: false});
       } else {
           this.clearState();
           this.setState({isOpen: true});
       }
    };

    private handleUsernameChange = (username: string) => {
        this.setState({usernameInput: username});
    }

    private handleAddUsername = () => {
        if ((this.state.usernameInput !== '') && (!this.state.usernames.includes(this.state.usernameInput))) {
            this.state.usernames.push(this.state.usernameInput);
            this.setState({usernameInput: '', usernames: this.state.usernames});
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

    private handleSelectPermission = (selectedPermission: Scope) => {
        let newPermissionsSelected: Scope[] = this.state.permissionsSelected;
        let newPermissionsUnSelected: Scope[] = this.state.permissionsUnSelected;

        if (newPermissionsSelected.includes(selectedPermission)) {
            newPermissionsSelected = newPermissionsSelected.filter(permission => permission !== selectedPermission);
            newPermissionsUnSelected.push(selectedPermission);
        } else {
            newPermissionsUnSelected = newPermissionsUnSelected.filter(permission => permission !== selectedPermission);
            newPermissionsSelected.push(selectedPermission);
        }

        this.setState({permissionsSelected: newPermissionsSelected, permissionsUnSelected: newPermissionsUnSelected});
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
                <Button variant="link" onClick={this.handleToggleDialog}>
                    <ShareAltIcon/> Share
                </Button>

                <Modal
                title={'Share the resource - ' + this.props.resource.name}
                isLarge={true}
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
                    <Stack gutter='md'>
                        <StackItem isFilled>
                        <Form>
                            <FormGroup
                                label="Add users to share your resource with"
                                type="string"
                                helperTextInvalid={Msg.localize('resourceAlreadyShared')}
                                fieldId="username"
                                isRequired
                                isValid={!this.isAlreadyShared()}
                                >
                                    <Gallery gutter='sm'>
                                        <GalleryItem>
                                            <TextInput
                                                value={this.state.usernameInput}
                                                isValid={!this.isAlreadyShared()}
                                                id="username"
                                                aria-describedby="username-helper"
                                                placeholder="username or email"
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
                                <ChipGroup>
                                    <ChipGroupToolbarItem key='users-selected' categoryName='Share with '>
                                    {this.state.usernames.map((currentChip: string) => (
                                        <Chip key={currentChip} onClick={() => this.handleDeleteUsername(currentChip)}>
                                            {currentChip}
                                        </Chip>
                                    ))}
                                    </ChipGroupToolbarItem>
                                </ChipGroup>
                            </FormGroup>
                            <FormGroup
                                label=""
                                fieldId="permissions-selected"
                            >
                                {this.state.permissionsSelected.length < 1 && <strong>Select permissions below:</strong>}
                                <ChipGroup>
                                    <ChipGroupToolbarItem key='permissions-selected' categoryName='Grant Permissions '>
                                    {this.state.permissionsSelected.map((currentChip: Scope) => (
                                        <Chip key={currentChip.toString()} onClick={() => this.handleSelectPermission(currentChip)}>
                                            {currentChip.toString()}
                                        </Chip>
                                    ))}
                                    </ChipGroupToolbarItem>
                                </ChipGroup>
                            </FormGroup>
                            <FormGroup
                                label=""
                                fieldId="permissions-not-selected"
                            >
                                <ChipGroup>
                                    <ChipGroupToolbarItem key='permissions-unselected' categoryName='Not Selected '>
                                    {this.state.permissionsUnSelected.map((currentChip: Scope) => (
                                        <Chip key={currentChip.toString()} onClick={() => this.handleSelectPermission(currentChip)}>
                                            {currentChip.toString()}
                                        </Chip>
                                    ))}
                                    </ChipGroupToolbarItem>
                                </ChipGroup>
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