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
    Flex,
    FlexItem,
    Pagination,
    PaginationVariant,
    TextInput
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
    UnlinkIcon,
    SearchIcon
} from '@patternfly/react-icons';

import {HttpResponse} from '../../account-service/account.service';
import {AccountServiceContext} from '../../account-service/AccountServiceContext';
import {Msg} from '../../widgets/Msg';
import {ContentPage} from '../ContentPage';
import {createRedirect} from '../../util/RedirectUri';


interface ResultsResponse {
    results: LinkedAccount[],
    totalHits: number
}

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
    linkedAccountsKeyword: string,
    linkedAccountsPage: number,
    linkedAccountsPerPage: number,
    linkedAccountsHits: number,
    linkedAccounts: LinkedAccount[];
    unLinkedAccountsKeyword: string,
    unLinkedAccountsPage: number,
    unLinkedAccountsPerPage: number,
    unLinkedAccountsHits: number,
    unLinkedAccounts: LinkedAccount[];
}

/**
 * @author Stan Silvert
 */
class LinkedAccountsPage extends React.Component<LinkedAccountsPageProps, LinkedAccountsPageState> {
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(props: LinkedAccountsPageProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;

        if(this.state == null) {
            this.state = {
                linkedAccountsKeyword: "",
                linkedAccountsPage: 1,
                linkedAccountsPerPage: 10,
                linkedAccountsHits: 0,
                linkedAccounts: [],
                unLinkedAccountsKeyword: "",
                unLinkedAccountsPage: 1,
                unLinkedAccountsPerPage: 10,
                unLinkedAccountsHits: 0,
                unLinkedAccounts: []
            }
            this.getLinkedAccounts(this.state.linkedAccountsKeyword, 0, 10);
            this.getUnlinkedAccounts(this.state.unLinkedAccountsKeyword, 0, 10);
        }


    }

    private getLinkedAccounts(keyword: string, first: number, max: number): void {
        this.context!.doGet<ResultsResponse>("/linked-accounts", { params: {linked: "true", keyword: keyword, first: first, max: max}} )
            .then((response: HttpResponse<ResultsResponse>) => {
                console.log({response});
                const linkedAccounts = response.data!.results;
                const totalHits = response.data!.totalHits;
                this.setState({
                    linkedAccounts: linkedAccounts,
                    linkedAccountsHits: totalHits
                });
            });
    }

    private getUnlinkedAccounts(keyword: string, first: number, max: number): void {
        this.context!.doGet<ResultsResponse>("/linked-accounts", { params: {linked: "false", keyword: keyword, first: first, max: max}} )
            .then((response: HttpResponse<ResultsResponse>) => {
                console.log({response});
                const unLinkedAccounts = response.data!.results;
                const totalHits = response.data!.totalHits;
                this.setState({
                    unLinkedAccounts: unLinkedAccounts,
                    unLinkedAccountsHits: totalHits
                });
            });
    }

    private unLinkAccount(account: LinkedAccount): void {
        const url = '/linked-accounts/' + account.providerName;

        this.context!.doDelete<void>(url)
            .then((response: HttpResponse<void>) => {
                console.log({response});
                this.getLinkedAccounts(this.state.linkedAccountsKeyword, (this.state.linkedAccountsPage-1) * this.state.linkedAccountsPerPage, this.state.linkedAccountsPerPage);
                this.getUnlinkedAccounts(this.state.unLinkedAccountsKeyword, (this.state.unLinkedAccountsPage-1) * this.state.unLinkedAccountsPerPage, this.state.unLinkedAccountsPerPage);
            });
    }

    private linkAccount(account: LinkedAccount): void {
        const url = '/linked-accounts/' + account.providerName;

        const redirectUri: string = createRedirect(this.props.location.pathname);

        this.context!.doGet<{accountLinkUri: string}>(url, { params: {providerId: account.providerName, redirectUri}})
            .then((response: HttpResponse<{accountLinkUri: string}>) => {
                console.log({response});
                window.location.href = response.data!.accountLinkUri;
            });
    }

    private onLinkedFilterChange = (value: string, event: any) => {
        this.setState({linkedAccountsKeyword: value});
        debugger;
    }

    private filterLinkedButton = () => {
        const pageNumber = 1;
        this.setState({
            linkedAccountsPage: pageNumber
        });
        this.getLinkedAccounts(this.state.linkedAccountsKeyword, (pageNumber-1) * this.state.linkedAccountsPerPage, this.state.linkedAccountsPerPage);
    }

    private filterLinkedKeyPress = (event: any) => {
        if(event.key === 'Enter'){
            this.filterLinkedButton();
        }
    }

    private onLinkedSetPage = (_event : any, pageNumber : number): void => {
        this.setState({
            linkedAccountsPage: pageNumber
        });
        this.getLinkedAccounts(this.state.linkedAccountsKeyword, (pageNumber-1) * this.state.linkedAccountsPerPage, this.state.linkedAccountsPerPage);
    }

    private onLinkedPerPageSelect = (_event : any, perPage : number): void => {
        this.setState({
            linkedAccountsPerPage: perPage
        });
        this.getLinkedAccounts(this.state.linkedAccountsKeyword, 0, perPage);
    }

    private onUnlinkedFilterChange = (value: string, event: any) => {
        this.setState({unLinkedAccountsKeyword: value});
    }

    private filterUnlinkedButton = () => {
        const pageNumber = 1;
        this.setState({
            unLinkedAccountsPage: pageNumber
        });
        this.getUnlinkedAccounts(this.state.unLinkedAccountsKeyword, (pageNumber-1) * this.state.unLinkedAccountsPerPage, this.state.unLinkedAccountsPerPage);
    }

    private filterUnlinkedKeyPress = (event: any) => {
        if(event.key === 'Enter'){
            this.filterUnlinkedButton();
        }
    }

    private onUnlinkedSetPage = (_event : any, pageNumber : number): void => {
        this.setState({
            unLinkedAccountsPage: pageNumber
        });
        this.getUnlinkedAccounts(this.state.unLinkedAccountsKeyword, (pageNumber-1) * this.state.unLinkedAccountsPerPage, this.state.unLinkedAccountsPerPage);
    }

    private onUnlinkedPerPageSelect = (_event : any, perPage : number): void => {
        this.setState({
            unLinkedAccountsPerPage: perPage
        });
        this.getUnlinkedAccounts(this.state.unLinkedAccountsKeyword, 0, perPage);
    }

    public render(): React.ReactNode {

        return (
            <ContentPage title={Msg.localize('linkedAccountsTitle')} introMessage={Msg.localize('linkedAccountsIntroMessage')}>
                <Stack gutter='md'>
                    <StackItem isFilled>
                        <Title headingLevel={TitleLevel.h2} size='2xl'>
                            <Msg msgKey='linkedLoginProviders'/>
                        </Title>
                        <DataList id="linked-idps" aria-label='foo' isCompact>
                            {this.makeRows(this.state.linkedAccounts, true)}
                        </DataList>
                    </StackItem>
                    <Flex>
                        <FlexItem>
                            <TextInput
                                type="search"
                                value={this.state.linkedAccountsKeyword}
                                id="linked-search"
                                name="linked-search"
                                onChange={this.onLinkedFilterChange}
                                aria-label="Search linked"
                                placeholder="Filter"
                                onKeyDown={this.filterLinkedKeyPress}
                            />
                        </FlexItem>
                        <FlexItem>
                            <Button variant="control" aria-label="filter-linked" onClick={this.filterLinkedButton}>
                                <SearchIcon />
                            </Button>
                        </FlexItem>
                        <FlexItem>
                            <Pagination
                                itemCount={this.state.linkedAccountsHits}
                                widgetId="pagination-linked"
                                perPage={this.state.linkedAccountsPerPage}
                                page={this.state.linkedAccountsPage}
                                variant={PaginationVariant.bottom}
                                onSetPage={this.onLinkedSetPage}
                                onPerPageSelect={this.onLinkedPerPageSelect}
                            />
                        </FlexItem>
                    </Flex>
                    <StackItem isFilled/>
                    <StackItem isFilled>
                        <Title headingLevel={TitleLevel.h2} size='2xl'>
                            <Msg msgKey='unlinkedLoginProviders'/>
                        </Title>
                        <DataList id="unlinked-idps" aria-label='foo' isCompact>
                            {this.makeRows(this.state.unLinkedAccounts, false)}
                        </DataList>
                    </StackItem>
                    <Flex>
                        <FlexItem>
                            <TextInput
                                type="search"
                                value={this.state.unLinkedAccountsKeyword}
                                id="unlinked-search"
                                name="unlinked-search"
                                onChange={this.onUnlinkedFilterChange}
                                aria-label="Search unlinked"
                                placeholder="Filter"
                                onKeyDown={this.filterUnlinkedKeyPress}
                            />
                        </FlexItem>
                        <FlexItem>
                            <Button variant="control" aria-label="filter-unlinked" onClick={this.filterUnlinkedButton}>
                                <SearchIcon />
                            </Button>
                        </FlexItem>
                        <FlexItem>
                            <Pagination
                                itemCount={this.state.unLinkedAccountsHits}
                                widgetId="pagination-unlinked"
                                perPage={this.state.unLinkedAccountsPerPage}
                                page={this.state.unLinkedAccountsPage}
                                variant={PaginationVariant.bottom}
                                onSetPage={this.onUnlinkedSetPage}
                                onPerPageSelect={this.onUnlinkedPerPageSelect}
                            />
                        </FlexItem>
                    </Flex>
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
                    <DataListItem id={`${account.providerAlias}-idp`} key={account.providerName} aria-labelledby="simple-item1">
                        <DataListItemRow key={account.providerName}>
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell key='idp'><Flex><FlexItem>{this.findIcon(account)}</FlexItem><FlexItem id={`${account.providerAlias}-idp-name`}><h2><strong>{account.displayName}</strong></h2></FlexItem></Flex></DataListCell>,
                                    <DataListCell key='badge'><Flex><FlexItem/><FlexItem id={`${account.providerAlias}-idp-badge`}>{this.badge(account)}</FlexItem></Flex></DataListCell>,
                                    <DataListCell key='username'><Flex><FlexItem/><FlexItem id={`${account.providerAlias}-idp-username`}>{account.linkedUsername}</FlexItem></Flex></DataListCell>,
                                ]}/>
                            <DataListAction aria-labelledby='foo' aria-label='foo action' id='setPasswordAction'>
                                {isLinked && <Button id={`${account.providerAlias}-idp-unlink`} variant='link' onClick={() => this.unLinkAccount(account)}><UnlinkIcon size='sm'/> <Msg msgKey='unLink'/></Button>}
                                {!isLinked && <Button id={`${account.providerAlias}-idp-link`} variant='link' onClick={() => this.linkAccount(account)}><LinkIcon size='sm'/> <Msg msgKey='link'/></Button>}
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
        const socialIconId = `${account.providerAlias}-idp-icon-social`;
        if (account.providerName.toLowerCase().includes('github')) return (<GithubIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('linkedin')) return (<LinkedinIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('facebook')) return (<FacebookIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('google')) return (<GoogleIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('instagram')) return (<InstagramIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('microsoft')) return (<MicrosoftIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('bitbucket')) return (<BitbucketIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('twitter')) return (<TwitterIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('openshift')) return (<OpenshiftIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('gitlab')) return (<GitlabIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('paypal')) return (<PaypalIcon id={socialIconId} size='lg'/>);
        if (account.providerName.toLowerCase().includes('stackoverflow')) return (<StackOverflowIcon id={socialIconId} size='lg'/>);

        return (<CubeIcon id={`${account.providerAlias}-idp-icon-default`} size='lg'/>);
    }

};

const LinkedAccountsPagewithRouter = withRouter(LinkedAccountsPage);
export {LinkedAccountsPagewithRouter as LinkedAccountsPage};