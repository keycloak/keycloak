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
    Button,
    DataList,
    DataListAction,
    DataListItemCells,
    DataListCell,
    DataListItemRow,
    Label,
    PageSection,
    PageSectionVariants,
    Split,
    SplitItem,
    Stack,
    StackItem,
    Title,
    DataListItem,
} from '@patternfly/react-core';

import {
    BitbucketIcon,
    CubeIcon,
    GitlabIcon,
    LinkIcon,
    OpenshiftIcon,
    PaypalIcon,
    UnlinkIcon,
    FacebookIcon,
    GoogleIcon,
    InstagramIcon,
    MicrosoftIcon,
    TwitterIcon,
    StackOverflowIcon,
    LinkedinIcon,
    GithubIcon
} from '@patternfly/react-icons';

import {HttpResponse} from '../../account-service/account.service';
import {AccountServiceContext} from '../../account-service/AccountServiceContext';
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
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(props: LinkedAccountsPageProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;

        this.state = {
            linkedAccounts: [],
            unLinkedAccounts: []
        }

        this.getLinkedAccounts();
    }

    private getLinkedAccounts(): void {
        this.context!.doGet<LinkedAccount[]>("/linked-accounts")
            .then((response: HttpResponse<LinkedAccount[]>) => {
                console.log({response});
                const linkedAccounts = response.data!.filter((account) => account.connected);
                const unLinkedAccounts = response.data!.filter((account) => !account.connected);
                this.setState({linkedAccounts: linkedAccounts, unLinkedAccounts: unLinkedAccounts});
            });
    }

    private unLinkAccount(account: LinkedAccount): void {
        const url = '/linked-accounts/' + account.providerName;

        this.context!.doDelete<void>(url)
            .then((response: HttpResponse<void>) => {
                console.log({response});
                this.getLinkedAccounts();
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

    public render(): React.ReactNode {

        return (
            <ContentPage title={Msg.localize('linkedAccountsTitle')} introMessage={Msg.localize('linkedAccountsIntroMessage')}>
                <PageSection isFilled variant={PageSectionVariants.light}>
                    <Stack hasGutter>
                        <StackItem>
                            <Title headingLevel="h2" className="pf-u-mb-lg" size='xl'>
                                <Msg msgKey='linkedLoginProviders'/>
                            </Title>
                            <DataList id="linked-idps" aria-label={Msg.localize('linkedLoginProviders')}>
                                {this.makeRows(this.state.linkedAccounts, true)}
                            </DataList>
                        </StackItem>
                        <StackItem>
                            <Title headingLevel="h2" className="pf-u-mt-xl pf-u-mb-lg" size='xl'>
                                <Msg msgKey='unlinkedLoginProviders'/>
                            </Title>
                            <DataList id="unlinked-idps" aria-label={Msg.localize('unlinkedLoginProviders')}>
                                {this.makeRows(this.state.unLinkedAccounts, false)}
                            </DataList>
                        </StackItem>
                    </Stack>
                </PageSection>
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
            <DataListItem key='emptyItem' aria-labelledby={Msg.localize('isEmptyMessage')}>
                <DataListItemRow key='emptyRow'>
                    <DataListItemCells dataListCells={[
                        <DataListCell key='empty'>{isEmptyMessage}</DataListCell>
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
                    <DataListItem id={`${account.providerAlias}-idp`} key={account.providerName} aria-labelledby={Msg.localize('linkedAccountsTitle')}>
                        <DataListItemRow key={account.providerName}>
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell key='idp'>
                                        <Split>
                                            <SplitItem className="pf-u-mr-sm">{this.findIcon(account)}</SplitItem>
                                            <SplitItem className="pf-u-my-xs" isFilled><span id={`${account.providerAlias}-idp-name`}>{account.displayName}</span></SplitItem>
                                        </Split>
                                    </DataListCell>,
                                    <DataListCell key='label'>
                                        <Split>
                                            <SplitItem className="pf-u-my-xs" isFilled><span id={`${account.providerAlias}-idp-label`}>{this.label(account)}</span></SplitItem>
                                        </Split>
                                    </DataListCell>,
                                    <DataListCell key='username' width={5}>
                                        <Split>
                                            <SplitItem className="pf-u-my-xs" isFilled><span id={`${account.providerAlias}-idp-username`}>{account.linkedUsername}</span></SplitItem>
                                        </Split>
                                    </DataListCell>,
                                ]}/>
                            <DataListAction aria-labelledby={Msg.localize('link')} aria-label={Msg.localize('unLink')} id='setPasswordAction'>
                                {isLinked && <Button id={`${account.providerAlias}-idp-unlink`} variant='link' onClick={() => this.unLinkAccount(account)}><UnlinkIcon size='sm'/> <Msg msgKey='unLink'/></Button>}
                                {!isLinked && <Button id={`${account.providerAlias}-idp-link`} variant='link' onClick={() => this.linkAccount(account)}><LinkIcon size='sm'/> <Msg msgKey='link'/></Button>}
                            </DataListAction>
                        </DataListItemRow>
                    </DataListItem>
                ))

            } </>

        )
    }

    private label(account: LinkedAccount): React.ReactNode {
        if (account.social) {
            return (<Label color="blue"><Msg msgKey='socialLogin'/></Label>);
        }

        return (<Label color="green"><Msg msgKey='systemDefined'/></Label>);
    }

    private findIcon(account: LinkedAccount): React.ReactNode {
      const socialIconId = `${account.providerAlias}-idp-icon-social`;
      console.log(account);
      switch (true) {
        case account.providerName.toLowerCase().includes('linkedin'):
          return <LinkedinIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('facebook'):
          return <FacebookIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('google'):
          return <GoogleIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('instagram'):
          return <InstagramIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('microsoft'):
          return <MicrosoftIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('bitbucket'):
          return <BitbucketIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('twitter'):
          return <TwitterIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('openshift'):
          // return <div className="idp-icon-social" id="openshift-idp-icon-social" />;
          return <OpenshiftIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('gitlab'):
          return <GitlabIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('github'):
          return <GithubIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('paypal'):
          return <PaypalIcon id={socialIconId} size='lg'/>;
        case account.providerName.toLowerCase().includes('stackoverflow'):
          return <StackOverflowIcon id={socialIconId} size='lg'/>;
        case (account.providerName !== '' && account.social):
          return <div className="idp-icon-social" id={socialIconId}/>;
        default:
          return <CubeIcon id={`${account.providerAlias}-idp-icon-default`} size='lg'/>;
      }
    }

};

const LinkedAccountsPagewithRouter = withRouter(LinkedAccountsPage);
export {LinkedAccountsPagewithRouter as LinkedAccountsPage};
