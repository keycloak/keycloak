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
import {Button, Grid, GridItem, Text, Title, Tooltip, Card, CardBody, Stack, StackItem, PageSection, TextContent, PageSectionVariants, SplitItem, Split} from '@patternfly/react-core';
import {RedoIcon, SyncAltIcon} from '@patternfly/react-icons';

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
            <ContentAlert />

            <PageSection variant={PageSectionVariants.light} className="pf-u-pb-xs">
              <Split>
                <SplitItem isFilled>
                  <TextContent>
                    <Title headingLevel="h1" size="2xl" className="pf-u-mb-xl">
                      <Msg msgKey={this.props.title} />
                    </Title>
                    {this.props.introMessage && (
                      <Text component="p">
                        <Msg msgKey={this.props.introMessage} />
                      </Text>
                    )}
                  </TextContent>
                </SplitItem>
                {this.props.onRefresh && (
                  <SplitItem>
                    <Tooltip content={<Msg msgKey="refreshPage" />}>
                      <Button
                        aria-label={Msg.localize('refreshPage')}
                        id="refresh-page"
                        variant="link"
                        onClick={this.props.onRefresh}
                        icon={<SyncAltIcon />}
                      >
                        <Msg msgKey="refresh" />
                      </Button>
                    </Tooltip>
                  </SplitItem>
                )}
              </Split>
            </PageSection>
            {this.props.children}
          </React.Fragment>
        );
    }
};
