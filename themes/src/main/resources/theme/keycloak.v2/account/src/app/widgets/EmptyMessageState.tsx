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
import {
    EmptyState,
    EmptyStateVariant,
    Title,
    EmptyStateIcon,
    TitleLevel,
    EmptyStateBody,
    IconProps,
} from '@patternfly/react-core'

import { Msg } from './Msg';

export interface EmptyMessageStateProps {
    icon: React.FunctionComponent<IconProps>;
    messageKey: string;
}

export default class EmptyMessageState extends React.Component<EmptyMessageStateProps, {}> {
    constructor(props: EmptyMessageStateProps) {
        super(props);
    }

    render() {
        return (
            <EmptyState variant={EmptyStateVariant.full}>
                <EmptyStateIcon icon={this.props.icon} />
                <Title headingLevel={TitleLevel.h5} size="lg">
                    <Msg msgKey={this.props.messageKey} />
                </Title>
                <EmptyStateBody>
                    {this.props.children}
                </EmptyStateBody>
            </EmptyState>
        );
    }
}