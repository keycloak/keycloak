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

import {withRouter, RouteComponentProps} from 'react-router-dom';
import {
        Button,
        DataList,
        DataListAction,
        DataListItemCells,
        DataListCell,
        DataListItem,
        DataListItemRow,
        Stack,
        StackItem,
        Title,
        TitleLevel,
        DataListActionVisibility,
        Dropdown,
        DropdownPosition,
        KebabToggle,
    } from '@patternfly/react-core';

import {AIACommand} from '../../util/AIACommand';
import TimeUtil from '../../util/TimeUtil';
import {HttpResponse, AccountServiceClient} from '../../account-service/account.service';
import {AccountServiceContext} from '../../account-service/AccountServiceContext';
import {ContinueCancelModal} from '../../widgets/ContinueCancelModal';
import {Features} from '../../widgets/features';
import {Msg} from '../../widgets/Msg';
import {ContentPage} from '../ContentPage';
import {ContentAlert} from '../ContentAlert';
import { KeycloakContext } from '../../keycloak-service/KeycloakContext';
import { KeycloakService } from '../../keycloak-service/keycloak.service';
import { css } from '@patternfly/react-styles';

declare const features: Features;

interface PasswordDetails {
    registered: boolean;
    lastUpdate: number;
}

type CredCategory = 'password' | 'two-factor' | 'passwordless';
type CredType = string;
type CredTypeMap = Map<CredType, CredentialContainer>;
type CredContainerMap = Map<CredCategory, CredTypeMap>;

interface UserCredential {
    id: string;
    type: string;
    userLabel: string;
    createdDate?: number;
    strCreatedDate?: string;
}

// A CredentialContainer is unique by combo of credential type and credential category
interface CredentialContainer {
    category: CredCategory;
    type: CredType;
    displayName: string;
    helptext?: string;
    createAction?: string;
    updateAction?: string;
    removeable: boolean;
    userCredentials: UserCredential[];
    open: boolean;
}

interface SigningInPageProps extends RouteComponentProps {
}

interface SigningInPageState {
    // Credential containers organized by category then type
    credentialContainers: CredContainerMap;
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
class SigningInPage extends React.Component<SigningInPageProps, SigningInPageState> {
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(props: SigningInPageProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;
    
        this.state = {
            credentialContainers: new Map(),
        }

        this.getCredentialContainers();
    }

    private getCredentialContainers(): void {
        this.context!.doGet("/credentials")
            .then((response: HttpResponse<CredentialContainer[]>) => {

                const allContainers: CredContainerMap = new Map();
                const containers: CredentialContainer[] = response.data || [];
                containers.forEach(container => {
                    let categoryMap = allContainers.get(container.category);
                    if (!categoryMap) {
                        categoryMap = new Map();
                        allContainers.set(container.category, categoryMap);
                    }
                    categoryMap.set(container.type, container);
                });

                this.setState({credentialContainers: allContainers});
                console.log({allContainers})
            });
    }

    private handleRemove = (credentialId: string, userLabel: string) => {
      this.context!.doDelete("/credentials/" + credentialId)
        .then(() => {
            this.getCredentialContainers();
            ContentAlert.success('successRemovedMessage', [userLabel]);
        });
    }

    public static credElementId(credType: CredType, credId: string, item: string): string {
        return `${credType}-${item}-${credId.substring(0,8)}`;
    }

    public render(): React.ReactNode {
        return (
            <ContentPage title="signingIn"
                     introMessage="signingInSubMessage">
                <Stack gutter='md'>
                    {this.renderCategories()}
                </Stack>
            </ContentPage>
        );
    }

    private renderCategories(): React.ReactNode {
        return (<> {
            Array.from(this.state.credentialContainers.keys()).map(category => (
                <StackItem key={category} isFilled>
                    <Title id={`${category}-categ-title`} headingLevel={TitleLevel.h2} size='2xl'>
                        <strong><Msg msgKey={category}/></strong>
                    </Title>
                    <DataList aria-label='foo'>
                        {this.renderTypes(this.state.credentialContainers.get(category)!)}
                    </DataList>
                </StackItem>
            ))

        }</>)
    }

    private renderTypes(credTypeMap: CredTypeMap): React.ReactNode {
        return (
        <KeycloakContext.Consumer> 
        { keycloak => (
            <>{
            Array.from(credTypeMap.keys()).map((credType: CredType, index: number, typeArray: string[]) => ([
                this.renderCredTypeTitle(credTypeMap.get(credType)!, keycloak!),
                this.renderUserCredentials(credTypeMap, credType, keycloak!),
                this.renderEmptyRow(credTypeMap.get(credType)!.type, index === typeArray.length - 1)
            ]))
            }</>
        )}
        </KeycloakContext.Consumer>
        );
    }

    private renderEmptyRow(type: string, isLast: boolean): React.ReactNode {
        if (isLast) return; // don't put empty row at the end

        return (
            <DataListItem aria-labelledby={'empty-list-item-' + type}>
                <DataListItemRow key={'empty-row-' + type}>
                    <DataListItemCells dataListCells={[<DataListCell></DataListCell>]}/>
                </DataListItemRow>
            </DataListItem>
        )
    }

    private renderUserCredentials(credTypeMap: CredTypeMap, credType: CredType, keycloak: KeycloakService): React.ReactNode {
        const credContainer: CredentialContainer = credTypeMap.get(credType)!;
        const userCredentials: UserCredential[] = credContainer.userCredentials;
        const removeable: boolean = credContainer.removeable;
        const type: string = credContainer.type;
        const displayName: string = credContainer.displayName;

        if (!userCredentials || userCredentials.length === 0) {
            const localizedDisplayName = Msg.localize(displayName);
            return (
                <DataListItem key='no-credentials-list-item' aria-labelledby='no-credentials-list-item'>
                    <DataListItemRow key='no-credentials-list-item-row'>
                        <DataListItemCells
                                    dataListCells={[
                                        <DataListCell key={'no-credentials-cell-0'}/>,
                                        <strong id={`${type}-not-set-up`} key={'no-credentials-cell-1'}><Msg msgKey='notSetUp' params={[localizedDisplayName]}/></strong>,
                                        <DataListCell key={'no-credentials-cell-2'}/>
                                    ]}/>
                    </DataListItemRow>
                </DataListItem>
            );
        }

        userCredentials.forEach(credential => {
            if (!credential.userLabel) credential.userLabel = Msg.localize(credential.type);
            if (credential.hasOwnProperty('createdDate') && credential.createdDate && credential.createdDate! > 0) {
                credential.strCreatedDate = TimeUtil.format(credential.createdDate as number);
            }
        });

        let updateAIA: AIACommand;
        if (credContainer.updateAction) {
            updateAIA = new AIACommand(keycloak, credContainer.updateAction);
        }

        return (
            <React.Fragment key='userCredentials'> {
                userCredentials.map(credential => (
                    <DataListItem id={`${SigningInPage.credElementId(type, credential.id, 'row')}`} key={'credential-list-item-' + credential.id} aria-labelledby={'credential-list-item-' + credential.userLabel}>
                        <DataListItemRow key={'userCredentialRow-' + credential.id}>
                            <DataListItemCells dataListCells={this.credentialRowCells(credential, type)}/>

                            <CredentialAction credential={credential}
                                              removeable={removeable}
                                              updateAction={updateAIA}
                                              credRemover={this.handleRemove}/>
                        </DataListItemRow>
                    </DataListItem>
                ))
            }
            </React.Fragment>)
    }

    private credentialRowCells(credential: UserCredential, type: string): React.ReactNode[] {
        const credRowCells: React.ReactNode[] = [];
        credRowCells.push(<DataListCell id={`${SigningInPage.credElementId(type, credential.id, 'label')}`} key={'userLabel-' + credential.id}>{credential.userLabel}</DataListCell>);
        if (credential.strCreatedDate) {
            credRowCells.push(<DataListCell id={`${SigningInPage.credElementId(type, credential.id, 'created-at')}`} key={'created-' + credential.id}><strong><Msg msgKey='credentialCreatedAt'/>: </strong>{credential.strCreatedDate}</DataListCell>);
            credRowCells.push(<DataListCell key={'spacer-' + credential.id}/>);
        }

        return credRowCells;
    }

    private renderCredTypeTitle(credContainer: CredentialContainer, keycloak: KeycloakService): React.ReactNode {
        if (!credContainer.hasOwnProperty('helptext') && !credContainer.hasOwnProperty('createAction')) return;

        let setupAction: AIACommand;
        if (credContainer.createAction) {
            setupAction = new AIACommand(keycloak, credContainer.createAction);
        }
        const credContainerDisplayName: string = Msg.localize(credContainer.displayName);

        return (
            <React.Fragment key={'credTypeTitle-' + credContainer.type}>
                <DataListItem aria-labelledby={'type-datalistitem-' + credContainer.type}>
                    <DataListItemRow key={'credTitleRow-' + credContainer.type}>
                        <DataListItemCells
                            dataListCells={[
                                <DataListCell width={5} key={'credTypeTitle-' + credContainer.type}>
                                    <Title headingLevel={TitleLevel.h3} size='2xl'>
                                        <strong id={`${credContainer.type}-cred-title`}><Msg msgKey={credContainer.displayName}/></strong>
                                    </Title>
                                    <span id={`${credContainer.type}-cred-help`}>
                                        {credContainer.helptext && <Msg msgKey={credContainer.helptext}/>}
                                    </span>
                                </DataListCell>,

                            ]}/>
                        {credContainer.createAction &&
                        <DataListAction
                            aria-labelledby='create'
                            aria-label='create action'
                            id={'mob-setUpAction-' + credContainer.type}
                            className={DataListActionVisibility.hiddenOnLg}
                        >
                            <Dropdown
                                isPlain
                                position={DropdownPosition.right}
                                toggle={<KebabToggle onToggle={isOpen => {
                                    credContainer.open = isOpen;
                                    this.setState({ credentialContainers: new Map(this.state.credentialContainers) });
                                }} />}
                                isOpen={credContainer.open}
                                dropdownItems={[
                                    <button id={`mob-${credContainer.type}-set-up`} className="pf-c-button pf-m-link" type="button" onClick={() => setupAction.execute()}>
                                        <span className="pf-c-button__icon">
                                            <i className="fas fa-plus-circle" aria-hidden="true"></i>
                                        </span>
                                        <Msg msgKey='setUpNew' params={[credContainerDisplayName]} />
                                    </button>]}
                            />
                        </DataListAction>}
                        {credContainer.createAction &&
                        <DataListAction
                            aria-labelledby='create'
                            aria-label='create action'
                            id={'setUpAction-' + credContainer.type}
                            className={css(DataListActionVisibility.visibleOnLg, DataListActionVisibility.hidden)}
                        >
                            <button id={`${credContainer.type}-set-up`} className="pf-c-button pf-m-link" type="button" onClick={()=> setupAction.execute()}>
                                <span className="pf-c-button__icon">
                                    <i className="fas fa-plus-circle" aria-hidden="true"></i>
                                </span>
                                <Msg msgKey='setUpNew' params={[credContainerDisplayName]}/>
                            </button>
                        </DataListAction>}
                    </DataListItemRow>
                </DataListItem>
            </React.Fragment>
        )
    }

};

type CredRemover = (credentialId: string, userLabel: string) => void;
interface CredentialActionProps {credential: UserCredential;
                                removeable: boolean;
                                updateAction: AIACommand;
                                credRemover: CredRemover;};
class CredentialAction extends React.Component<CredentialActionProps> {

    render(): React.ReactNode {
        if (this.props.updateAction) {
            return (
                <DataListAction aria-labelledby='foo' aria-label='foo action' id={'updateAction-' + this.props.credential.id}>
                    <Button id={`${SigningInPage.credElementId(this.props.credential.type, this.props.credential.id, 'update')}`} variant='primary'onClick={()=> this.props.updateAction.execute()}><Msg msgKey='update'/></Button>
                </DataListAction>
            )
        }

        if (this.props.removeable) {
            const userLabel: string = this.props.credential.userLabel;
            return (
                <DataListAction aria-labelledby='foo' aria-label='foo action' id={'removeAction-' + this.props.credential.id }>
                    <ContinueCancelModal buttonTitle='remove'
                                        buttonId={`${SigningInPage.credElementId(this.props.credential.type, this.props.credential.id, 'remove')}`}
                                        modalTitle={Msg.localize('removeCred', [userLabel])}
                                        modalMessage={Msg.localize('stopUsingCred', [userLabel])}
                                        onContinue={() => this.props.credRemover(this.props.credential.id, userLabel)}
                                            />
                </DataListAction>
            )
        }

        return (<></>)
    }
}

const SigningInPageWithRouter = withRouter(SigningInPage);
export { SigningInPageWithRouter as SigningInPage};