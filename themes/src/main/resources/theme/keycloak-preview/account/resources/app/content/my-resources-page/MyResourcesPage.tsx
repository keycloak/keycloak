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
import {Tab, Tabs, TextInput, Modal, Button } from '@patternfly/react-core';
import {DataTable} from './DataTable';
import {ContentPage} from '../ContentPage';
import {Msg} from '../../widgets/Msg';

export interface MyResourcesPageProps {
}

export interface MyResourcesPageState {
    activeTabKey: number;
    isModalOpen: boolean;
}

export class MyResourcesPage extends React.Component<MyResourcesPageProps, MyResourcesPageState> {

    constructor(props: MyResourcesPageProps) {
        super(props);
        this.state = {
            activeTabKey: 0,
            isModalOpen: false
        };
    }

    render(): React.ReactNode {
        return (
            <ContentPage title="resources">
                <Tabs isFilled activeKey={this.state.activeTabKey} onSelect={this.handleTabClick}>
                    <Tab eventKey={0} title={Msg.localize('myResources')}>
                        <TextInput type="text" placeholder={Msg.localize('filterByName')} style={styles.filters}/>
                        <DataTable data={this.fetchResources()} headers={["Resource", "Application", "Date", "Shared With"]}/>
                    </Tab>
                    <Tab eventKey={1} title={Msg.localize('sharedwithMe')}>
                        <TextInput type="text" placeholder={Msg.localize('filterByName')} style={styles.filters}/>
                        <DataTable data={this.fetchResourcesSharedWithMe()} headers={["Resource", "Application", "Date", "Shared With"]}/>
                    </Tab>
                </Tabs>
            </ContentPage>
        );
    }

    private fetchResources() {
        let resources = [];

        for (let i = 0; i < 5; i++) {
            resources.push(["Alice Family " + i, "photoz-restful-api", "12:00 AM", <span><i className="fas fa-share-alt"></i> +5</span>]);
        }

        return resources;
    }

    private fetchResourcesSharedWithMe() {
        let resources = [];

        for (let i = 0; i < 5; i++) {
            resources.push(["Alice Family " + i, "John", "photoz-restful-api", "Read, Write, Execute"]);
        }

        return resources;
    }

    handleTabClick = (event: any, tabIndex: number) => {
        this.setState({
            activeTabKey: tabIndex
        });
    };
};

const styles: any = {
    filters: {
        width: 200,
        fontSize: 13
    }
}
