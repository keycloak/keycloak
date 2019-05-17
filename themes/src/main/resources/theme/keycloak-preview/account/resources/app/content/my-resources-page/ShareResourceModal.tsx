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
import {Button, Form, FormGroup, Modal, TextInput} from '@patternfly/react-core';
import {Msg} from "../../widgets/Msg";

export interface ShareResourceModalState {
    isModalOpen: boolean
}

export interface ShareResourceModalProps {
    resource: any;
}

export class ShareResourceModal extends React.Component<ShareResourceModalProps, ShareResourceModalState> {

    constructor(props: ShareResourceModalProps) {
        super(props);
        this.state = {isModalOpen: false}
    }

    render(): React.ReactNode {
        return (
            <React.Fragment>
                <button className="pf-c-button pf-m-tertiary" onClick={this.handleModalToggle}>
                    Share
                </button>
                <Modal
                    isSmall
                    title={"Share the resource " + this.props.resource[0]}
                    isOpen={this.state.isModalOpen}
                    actions={[
                        <Button key="cancel" variant="secondary" onClick={this.handleModalToggle}>
                            <Msg msgKey="doCancel"/>
                        </Button>,
                        <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
                            <Msg msgKey="share"/>
                        </Button>
                    ]}>
                    <section className="pf-c-page__main-section">
                        <Form>
                            <FormGroup
                                label="Add people to share your resource with"
                                isRequired
                                fieldId="simple-form-name"
                            >
                                <TextInput
                                    isRequired
                                    type="text"
                                    id="simple-form-name"
                                    name="simple-form-name"
                                    aria-describedby="simple-form-name-helper"
                                />
                            </FormGroup>
                        </Form>
                    </section>
                </Modal>
            </React.Fragment>
        );
    }

    handleModalToggle = () => {
        this.setState(({isModalOpen}) => ({
            isModalOpen: !isModalOpen
        }));
    };
}
