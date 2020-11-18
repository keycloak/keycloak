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
    Form,
    FormGroup,
    TextInput,
    InputGroup
} from '@patternfly/react-core';
import { OkIcon } from '@patternfly/react-icons';

import { Resource, Permission, Scope } from './resource-model';
import { Msg } from '../../widgets/Msg';
import { AccountServiceContext } from '../../account-service/AccountServiceContext';
import { ContentAlert } from '../ContentAlert';
import { PermissionSelect } from './PermissionSelect';

interface EditTheResourceProps {
    resource: Resource;
    permissions: Permission[];
    onClose: () => void;
    children: (toggle: () => void) => void;
}

interface EditTheResourceState {
    changed: boolean[];
    isOpen: boolean;
}

export class EditTheResource extends React.Component<EditTheResourceProps, EditTheResourceState> {
    protected static defaultProps = { permissions: [] };
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(props: EditTheResourceProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;

        this.state = {
            changed: [],
            isOpen: false,
        };
    }

    private clearState(): void {
        this.setState({});
    }

    private handleToggleDialog = () => {
        if (this.state.isOpen) {
            this.setState({ isOpen: false });
            this.props.onClose();
        } else {
            this.clearState();
            this.setState({ isOpen: true });
        }
    };

    private updateChanged = (row: number) => {
        const changed = this.state.changed;
        changed[row] = !changed[row];
        this.setState({ changed });
    }

    async savePermission(permission: Permission): Promise<void> {
        await this.context!.doPut(`/resources/${this.props.resource._id}/permissions`, [permission]);
        ContentAlert.success(Msg.localize('updateSuccess'));
    }

    public render(): React.ReactNode {
        return (
            <React.Fragment>
                {this.props.children(this.handleToggleDialog)}

                <Modal
                    title={'Edit the resource - ' + this.props.resource.name}
                    isLarge
                    isOpen={this.state.isOpen}
                    onClose={this.handleToggleDialog}
                    actions={[
                        <Button key="done" variant="link" id="done" onClick={this.handleToggleDialog}>
                            <Msg msgKey='done' />
                        </Button>,
                    ]}
                >
                    <Form isHorizontal>
                        {this.props.permissions.map((p, row) => (
                            <React.Fragment>
                                <FormGroup
                                    fieldId={`username-${row}`}
                                    label={Msg.localize('User')}
                                >
                                    <TextInput id={`username-${row}`} type="text" value={p.username} isDisabled />

                                </FormGroup>
                                <FormGroup
                                    fieldId={`permissions-${row}`}
                                    label={Msg.localize('permissions')}
                                    isRequired
                                >
                                    <InputGroup>
                                        <PermissionSelect
                                            scopes={this.props.resource.scopes}
                                            selected={(p.scopes as string[]).map(s => new Scope(s))}
                                            direction={row === this.props.permissions.length - 1 ? "up" : "down"}
                                            onSelect={selection => {
                                                p.scopes = selection.map(s => s.name);
                                                this.updateChanged(row);
                                            }}
                                        />
                                        <Button
                                            id={`save-${row}`}
                                            isDisabled={!this.state.changed[row]}
                                            onClick={() => this.savePermission(p)}
                                        >
                                            <OkIcon />
                                        </Button>
                                    </InputGroup>
                                </FormGroup>
                                <hr />
                            </React.Fragment>
                        ))}
                    </Form>
                </Modal>
            </React.Fragment>
        );
    }
}