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
import {Button, Grid, GridItem, Title, Tooltip} from '@patternfly/react-core';
import {RedoIcon} from '@patternfly/react-icons';

import {Msg} from '../widgets/Msg';
import {ContentAlert} from './ContentAlert';

interface ContentPageProps {
    title: string; // Literal title or key into message bundle
    introMessage?: string; // Literal message or key into message bundle
    onRefresh?: () => void;
    children: React.ReactNode;
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 */
export class ContentPage extends React.Component<ContentPageProps> {

    public constructor(props: ContentPageProps) {
        super(props);
    }

    public render(): React.ReactNode {
        return (
            <React.Fragment>
                <ContentAlert/>
                <section id="page-heading" className="pf-c-page__main-section pf-m-light">
                    <Grid>
                        <GridItem span={11}><Title headingLevel='h1' size='3xl'><strong><Msg msgKey={this.props.title}/></strong></Title></GridItem>
                        {this.props.onRefresh &&
                            <GridItem span={1}>
                                <Tooltip content={<Msg msgKey='refreshPage'/>}><Button id='refresh-page' variant='plain' onClick={this.props.onRefresh}><RedoIcon size='sm'/></Button></Tooltip>
                            </GridItem>
                        }
                        {this.props.introMessage && <GridItem span={12}> <Msg msgKey={this.props.introMessage}/></GridItem>}
                    </Grid>
                </section>

                <section className="pf-c-page__main-section pf-m-no-padding-mobile">
                    {this.props.children}
                </section>
            </React.Fragment>
        );
    }
};