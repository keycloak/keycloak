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
import {withRouter, RouteComponentProps} from 'react-router-dom';
import {AxiosResponse} from 'axios';

import {
    Badge,
    Button,
    DataList, 
    DataListAction,
    DataListItemCells, 
    DataListCell,
    DataListItemRow, 
    Stack,
    StackItem,
    Title, 
    TitleLevel,
    DataListItem,
} from '@patternfly/react-core';

import {
    BitbucketIcon,
    CubeIcon,
    FacebookIcon,
    GithubIcon, 
    GitlabIcon,
    GoogleIcon,
    InstagramIcon,
    LinkIcon,
    LinkedinIcon,
    MicrosoftIcon,
    OpenshiftIcon,
    PaypalIcon,
    StackOverflowIcon,
    TwitterIcon,
    UnlinkIcon
} from '@patternfly/react-icons';

import {AccountServiceClient} from '../../account-service/account.service';
import {Msg} from '../../widgets/Msg';
import {ContentPage} from '../ContentPage';
import {createRedirect} from '../../util/RedirectUri';

interface LinkedAccount {
    connected: boolean;
    social: boolean;
    providerAlias: string;
    providerName: string;
    displayName: string;
    linkedUsername: string;
}

interface LinkedAccountsPageProps extends RouteComponentProps {
}

interface LinkedAccountsPageState {
    linkedAccounts: LinkedAccount[];
    unLinkedAccounts: LinkedAccount[];
}

/**
 * @author Stan Silvert
 */
class LinkedAccountsPage extends React.Component<LinkedAccountsPageProps, LinkedAccountsPageState> {
    
    public constructor(props: LinkedAccountsPageProps) {
        super(props);
        this.state = {
            linkedAccounts: [],
            unLinkedAccounts: []
        }

        this.getLinkedAccounts();
    }

    private getLinkedAccounts(): void {
        AccountServiceClient.Instance.doGet("/linked-accounts")
            .then((response: AxiosResponse<LinkedAccount[]>) => {
                console.log({response});
                const linkedAccounts = response.data.filter((account) => account.connected);
                const unLinkedAccounts = response.data.filter((account) => !account.connected);
                this.setState({linkedAccounts: linkedAccounts, unLinkedAccounts: unLinkedAccounts});
            });
    }

    private unLinkAccount(account: LinkedAccount): void {
        const url = '/linked-accounts/' + account.providerName;

        AccountServiceClient.Instance.doDelete(url)
            .then((response: AxiosResponse) => {
                console.log({response});
                this.getLinkedAccounts();
            });
    }

    private linkAccount(account: LinkedAccount): void {
        const url = '/linked-accounts/' + account.providerName;

        const redirectUri: string = createRedirect(this.props.location.pathname);

        AccountServiceClient.Instance.doGet(url, { params: {providerId: account.providerName, redirectUri}})
            .then((response: AxiosResponse<{accountLinkUri: string}>) => {
                console.log({response});
                window.location.href = response.data.accountLinkUri;
            });
    }

    public render(): React.ReactNode {

        return (
            <ContentPage title={Msg.localize('linkedAccountsTitle')} introMessage={Msg.localize('linkedAccountsIntroMessage')}>
                <Stack gutter='md'>
                    <StackItem isFilled>
                        <Title headingLevel={TitleLevel.h2} size='2xl'>
                            <Msg msgKey='linkedLoginProviders'/>
                        </Title>
                        <DataList aria-label='foo'>
                            {this.makeRows(this.state.linkedAccounts, true)}
                        </DataList>
                    </StackItem>
                    <StackItem isFilled/>
                    <StackItem isFilled>
                        <Title headingLevel={TitleLevel.h2} size='2xl'>
                            <Msg msgKey='unlinkedLoginProviders'/>
                        </Title>
                        <DataList aria-label='foo'>
                            {this.makeRows(this.state.unLinkedAccounts, false)}
                        </DataList>
                    </StackItem>                    
                </Stack>
            </ContentPage>
        );
    }

    private emptyRow(isLinked: boolean): React.ReactNode {
        let isEmptyMessage = '';
        if (isLinked) {
            isEmptyMessage = Msg.localize('linkedEmpty');
        } else {
            isEmptyMessage = Msg.localize('unlinkedEmpty');
        }

        return (
            <DataListItem key='emptyItem' aria-labelledby="empty-item">
                <DataListItemRow key='emptyRow'>
                    <DataListItemCells dataListCells={[
                        <DataListCell key='empty'><strong>{isEmptyMessage}</strong></DataListCell>
                    ]}/>
                </DataListItemRow>
            </DataListItem>
        )
    }

    private makeRows(accounts: LinkedAccount[], isLinked: boolean): React.ReactNode {
        if (accounts.length === 0) {
            return this.emptyRow(isLinked);
        }

        return (
            <> {
        
                accounts.map( (account: LinkedAccount) => (
                    <DataListItem key={account.providerName} aria-labelledby="simple-item1">
                        <DataListItemRow key={account.providerName}>
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell key='idp'><Stack><StackItem isFilled>{this.findIcon(account)}</StackItem><StackItem isFilled><h2><strong>{account.displayName}</strong></h2></StackItem></Stack></DataListCell>,
                                    <DataListCell key='badge'><Stack><StackItem isFilled/><StackItem isFilled>{this.badge(account)}</StackItem></Stack></DataListCell>,
                                    <DataListCell key='username'><Stack><StackItem isFilled/><StackItem isFilled>{account.linkedUsername}</StackItem></Stack></DataListCell>,
                                ]}/>
                            <DataListAction aria-labelledby='foo' aria-label='foo action' id='setPasswordAction'>
                                {isLinked && <Button variant='link' onClick={() => this.unLinkAccount(account)}><UnlinkIcon size='sm'/> <Msg msgKey='unLink'/></Button>}
                                {!isLinked && <Button variant='link' onClick={() => this.linkAccount(account)}><LinkIcon size='sm'/> <Msg msgKey='link'/></Button>}
                            </DataListAction>
                        </DataListItemRow>
                    </DataListItem>
                ))
        
            } </>
        
        )
    }

    private badge(account: LinkedAccount): React.ReactNode {
        if (account.social) {
            return (<Badge><Msg msgKey='socialLogin'/></Badge>);
        }
    
        return (<Badge style={{backgroundColor: "green"}} ><Msg msgKey='systemDefined'/></Badge>);
    }

    private findIcon(account: LinkedAccount): React.ReactNode {
        if (account.providerName.toLowerCase().includes('github')) return (<GithubIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('linkedin')) return (<LinkedinIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('facebook')) return (<FacebookIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('google')) return (<GoogleIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('instagram')) return (<InstagramIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('microsoft')) return (<MicrosoftIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('bitbucket')) return (<BitbucketIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('twitter')) return (<TwitterIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('openshift')) return (<OpenshiftIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('gitlab')) return (<GitlabIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('paypal')) return (<PaypalIcon size='xl'/>);
        if (account.providerName.toLowerCase().includes('stackoverflow')) return (<StackOverflowIcon size='xl'/>);

        return (<CubeIcon size='xl'/>);
    }

};

const LinkedAccountsPagewithRouter = withRouter(LinkedAccountsPage);
export {LinkedAccountsPagewithRouter as LinkedAccountsPage};