/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
    Card,
    CardBody,
    EmptyState,
    EmptyStateBody,
    EmptyStateVariant,
    Grid,
    GridItem,
    Title
} from '@patternfly/react-core';

declare const resourceUrl: string;

export class KeycloakManLovesJsx extends React.Component {

    public render(): React.ReactNode {

        return (
            <Card>
                <CardBody>
                    <EmptyState variant={EmptyStateVariant.small}>
                        <Title headingLevel="h4" size="lg">
                        Keycloak Man Loves JSX, React, and PatternFly
                        </Title>
                        <EmptyStateBody>
                            <Grid gutter='sm'>
                                <GridItem span={12}><img src={resourceUrl + '/public/keycloak-man-95x95.jpg'}/></GridItem>
                                <GridItem span={12}><img src={resourceUrl + '/public/heart-95x95.png'}/></GridItem>
                                <GridItem span={12}>
                                    <img src={resourceUrl + '/public/jsx-95x95.png'}/>
                                    <img src={resourceUrl + '/public/react-95x95.png'}/>
                                    <img src={resourceUrl + '/public/patternfly-95x95.png'}/>
                                </GridItem>
                            </Grid>
                        </EmptyStateBody>
                        <Title headingLevel="h4" size="lg">
                        But you can use whatever you want as long as you wrap it in a React Component.
                        </Title>
                    </EmptyState>
                </CardBody>
            </Card>
        );
    }
};