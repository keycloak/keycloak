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

import * as React from "react";

import { withRouter, RouteComponentProps } from "react-router-dom";
import {
    Alert,
    Button,
    DataList,
    DataListAction,
    DataListItemCells,
    DataListCell,
    DataListItem,
    DataListItemRow,
    EmptyState,
    EmptyStateVariant,
    EmptyStateBody,
    Split,
    SplitItem,
    Title,
    Dropdown,
    DropdownPosition,
    KebabToggle,
    PageSection,
    PageSectionVariants
} from "@patternfly/react-core";

import { AIACommand } from "../../util/AIACommand";
import TimeUtil from "../../util/TimeUtil";
import {
    HttpResponse,
    AccountServiceClient,
} from "../../account-service/account.service";
import { AccountServiceContext } from "../../account-service/AccountServiceContext";
import { ContinueCancelModal } from "../../widgets/ContinueCancelModal";
import { Features } from "../../widgets/features";
import { Msg } from "../../widgets/Msg";
import { ContentPage } from "../ContentPage";
import { ContentAlert } from "../ContentAlert";
import { KeycloakContext } from "../../keycloak-service/KeycloakContext";
import { KeycloakService } from "../../keycloak-service/keycloak.service";
import { css } from "@patternfly/react-styles";

declare const features: Features;

interface PasswordDetails {
    registered: boolean;
    lastUpdate: number;
}

type CredCategory = "password" | "two-factor" | "passwordless";
type CredType = string;
type CredTypeMap = Map<CredType, CredentialContainer>;
type CredContainerMap = Map<CredCategory, CredTypeMap>;

interface CredMetadata {
    infoMessage?: string;
    warningMessageTitle?: string;
    warningMessageDescription?: string;
    credential: UserCredential;
}

interface UserCredential {
    id: string;
    type: string;
    userLabel: string;
    createdDate?: number;
    strCreatedDate?: string;
    credentialData?: string;
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
    userCredentialMetadatas: CredMetadata[];
    open: boolean;
}

interface SigningInPageProps extends RouteComponentProps {}

interface SigningInPageState {
    // Credential containers organized by category then type
    credentialContainers: CredContainerMap;
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
class SigningInPage extends React.Component<
    SigningInPageProps,
    SigningInPageState
> {
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(
        props: SigningInPageProps,
        context: React.ContextType<typeof AccountServiceContext>
    ) {
        super(props);
        this.context = context;

        this.state = {
            credentialContainers: new Map(),
        };

        this.getCredentialContainers();
    }

    private getCredentialContainers(): void {
        this.context!.doGet<CredentialContainer[]>("/credentials").then(
            (response) => {
                const allContainers: CredContainerMap = new Map();
                const containers: CredentialContainer[] = response.data || [];
                containers.forEach((container) => {
                    let categoryMap = allContainers.get(container.category);
                    if (!categoryMap) {
                        categoryMap = new Map();
                        allContainers.set(container.category, categoryMap);
                    }
                    categoryMap.set(container.type, container);
                });

                this.setState({ credentialContainers: allContainers });
            }
        );
    }

    private handleRemove = (credentialId: string, userLabel: string) => {
        this.context!.doDelete("/credentials/" + encodeURIComponent(credentialId)).then(() => {
            this.getCredentialContainers();
            ContentAlert.success("successRemovedMessage", [userLabel]);
        });
    };

    public static credElementId(
        credType: CredType,
        credId: string,
        item: string
    ): string {
        return `${credType}-${item}-${credId.substring(0, 8)}`;
    }

    public render(): React.ReactNode {
        return (
            <ContentPage title="signingIn" introMessage="signingInSubMessage">
                {this.renderCategories()}
            </ContentPage>
        );
    }

    private renderCategories(): React.ReactNode {
      return Array.from(this.state.credentialContainers.keys()).map(
          (category) => (
            <PageSection key={category} variant={PageSectionVariants.light}>
              <Title
                  id={`${category}-categ-title`}
                  headingLevel="h2"
                  size="xl"
              >
                <Msg msgKey={category} />
              </Title>
              {this.renderTypes(category!)}
            </PageSection>
         )
       ) 
    }

    private renderTypes(category: CredCategory): React.ReactNode {
        let credTypeMap: CredTypeMap = this.state.credentialContainers.get(
            category
        )!;

        return (
            <KeycloakContext.Consumer>
                {(keycloak) => (
                    <>
                        {Array.from(
                            credTypeMap.keys()
                        ).map(
                            (
                                credType: CredType,
                                index: number,
                                typeArray: string[]
                            ) => [
                                this.renderCredTypeTitle(
                                    credTypeMap.get(credType)!,
                                    keycloak!,
                                    category
                                ),
                                this.renderUserCredentials(
                                    credTypeMap,
                                    credType,
                                    keycloak!
                                ),
                            ]
                        )}
                    </>
                )}
            </KeycloakContext.Consumer>
        );
    }

    private renderEmptyRow(type: string, isLast: boolean): React.ReactNode {
        if (isLast) return; // don't put empty row at the end

        return (
            <DataListItem aria-labelledby={"empty-list-item-" + type}>
                <DataListItemRow key={"empty-row-" + type}>
                    <DataListItemCells
                        dataListCells={[<DataListCell></DataListCell>]}
                    />
                </DataListItemRow>
            </DataListItem>
        );
    }

    private renderUserCredentials(
        credTypeMap: CredTypeMap,
        credType: CredType,
        keycloak: KeycloakService
    ): React.ReactNode {
        const credContainer: CredentialContainer = credTypeMap.get(credType)!;
        const userCredentialMetadatas: CredMetadata[] = credContainer.userCredentialMetadatas;
        const removeable: boolean = credContainer.removeable;
        const type: string = credContainer.type;
        const displayName: string = credContainer.displayName;

        if (!userCredentialMetadatas || userCredentialMetadatas.length === 0) {
            const localizedDisplayName = Msg.localize(displayName);
            return (
              <DataList aria-label={Msg.localize('notSetUp', [localizedDisplayName])} className="pf-u-mb-xl">
                <DataListItem key='no-credentials-list-item' aria-labelledby={Msg.localize('notSetUp', [localizedDisplayName])}>
                    <DataListItemRow key='no-credentials-list-item-row' className="pf-u-align-items-center">
                        <DataListItemCells
                            dataListCells={[
                                <DataListCell key={'no-credentials-cell-0'}/>,
                                <EmptyState id={`${type}-not-set-up`} key={'no-credentials-cell-1'} variant={EmptyStateVariant.xs}>
                                    <EmptyStateBody>
                                        <Msg msgKey='notSetUp' params={[localizedDisplayName]}/>
                                    </EmptyStateBody>
                                </EmptyState>,
                                <DataListCell key={'no-credentials-cell-2'}/>
                            ]}/>
                    </DataListItemRow>
                </DataListItem>
              </DataList>
            );
        }

        userCredentialMetadatas.forEach(credentialMetadata => {
            let credential = credentialMetadata.credential;
            if (!credential.userLabel) credential.userLabel = Msg.localize(credential.type);
            if (credential.hasOwnProperty('createdDate') && credential.createdDate && credential.createdDate! > 0) {
                credential.strCreatedDate = TimeUtil.format(credential.createdDate as number);
            }
        });

        let updateAIA: AIACommand;
        if (credContainer.updateAction) {
            updateAIA = new AIACommand(keycloak, credContainer.updateAction);
        }

        let maxWidth = { maxWidth: 689 } as React.CSSProperties;

        return (
            <React.Fragment key='userCredentialMetadatas'> {
                userCredentialMetadatas.map(credentialMetadata => (
                  <>
                    {(credentialMetadata.infoMessage && !credentialMetadata.warningMessageTitle && !credentialMetadata.warningMessageDescription) && 
                      <Alert variant="default" className="pf-u-mb-md" isInline isPlain title={Msg.localize(JSON.parse(credentialMetadata.infoMessage).key, JSON.parse(credentialMetadata.infoMessage).parameters)} />
                    }
                    {(credentialMetadata.warningMessageTitle && credentialMetadata.warningMessageDescription) &&
                      <Alert variant="warning" className="pf-u-mb-md" isInline title={Msg.localize(JSON.parse(credentialMetadata.warningMessageTitle).key, JSON.parse(credentialMetadata.warningMessageTitle).parameters)} style={maxWidth}>
                                
                        <p>{Msg.localize(JSON.parse(credentialMetadata.warningMessageDescription).key, JSON.parse(credentialMetadata.warningMessageDescription).parameters)}</p>
                      </Alert>
                    }
                    <DataList aria-label="user credential" className="pf-u-mb-xl">
                      <DataListItem id={`${SigningInPage.credElementId(type, credentialMetadata.credential.id, 'row')}`} key={'credential-list-item-' + credentialMetadata.credential.id} aria-labelledby={'credential-list-item-' + credentialMetadata.credential.userLabel}>
                          <DataListItemRow key={'userCredentialRow-' + credentialMetadata.credential.id} className="pf-u-align-items-center">
                              <DataListItemCells dataListCells={this.credentialRowCells(credentialMetadata, type)}/>
                              <CredentialAction
                                credential={credentialMetadata.credential}
                                removeable={removeable}
                                updateAction={updateAIA}
                                credRemover={this.handleRemove}
                              />
                          </DataListItemRow>
                      </DataListItem>
                    </DataList>
                  </>
                ))
            } </React.Fragment>
        )
    }

    private credentialRowCells(credMetadata: CredMetadata, type: string): React.ReactNode[] {
        const credRowCells: React.ReactNode[] = [];
        const credential = credMetadata.credential;
        let maxWidth = { "--pf-u-max-width--MaxWidth": "300px" } as React.CSSProperties;
        credRowCells.push(
            <DataListCell id={`${SigningInPage.credElementId(type, credential.id, 'label')}`} key={'userLabel-' + credential.id} className="pf-u-max-width" style={maxWidth}>
                {credential.userLabel}
            </DataListCell>
        );
        if (credential.strCreatedDate) {
            credRowCells.push(
                <DataListCell
                    id={`${SigningInPage.credElementId(
                        type,
                        credential.id,
                        "created-at"
                    )}`}
                    key={"created-" + credential.id}
                >
                    <strong className="pf-u-mr-md">
                        <Msg msgKey="credentialCreatedAt" />{" "}
                    </strong>
                    {credential.strCreatedDate}
                </DataListCell>
            );
            credRowCells.push(<DataListCell key={"spacer-" + credential.id} />);
        }

        return credRowCells;
    }

    private renderCredTypeTitle(
        credContainer: CredentialContainer,
        keycloak: KeycloakService,
        category: CredCategory
    ): React.ReactNode {
      
        if (
            !credContainer.hasOwnProperty("helptext") &&
            !credContainer.hasOwnProperty("createAction")
        )
            return;

        let setupAction: AIACommand;
        if (credContainer.createAction) {
            setupAction = new AIACommand(keycloak, credContainer.createAction);
        }

        const credContainerDisplayName: string = Msg.localize(
            credContainer.displayName
        );
        return (
            <React.Fragment key={"credTypeTitle-" + credContainer.type}>
                <Split className="pf-u-mt-lg pf-u-mb-lg">
                    <SplitItem>
                        <Title
                            headingLevel="h3"
                            size="md"
                            className="pf-u-mb-md"
                        >
                            <span className="cred-title pf-u-display-block" id={`${credContainer.type}-cred-title`}>
                                <Msg msgKey={credContainer.displayName} />
                            </span>
                        </Title>
                        <span id={`${credContainer.type}-cred-help`}>
                            {credContainer.helptext && (
                                <Msg msgKey={credContainer.helptext} />
                            )}
                        </span>
                    </SplitItem>

                    <SplitItem isFilled>
                        {credContainer.createAction && (
                            <div
                                id={"mob-setUpAction-" + credContainer.type}
                                className="pf-u-display-none-on-lg pf-u-float-right"
                            >
                                <Dropdown
                                    isPlain
                                    position={DropdownPosition.right}
                                    toggle={
                                        <KebabToggle
                                            onToggle={(isOpen) => {
                                                credContainer.open = isOpen;
                                                this.setState({
                                                    credentialContainers: new Map(
                                                        this.state.credentialContainers
                                                    ),
                                                });
                                            }}
                                        />
                                    }
                                    isOpen={credContainer.open}
                                    dropdownItems={[
                                        <button
                                            id={`mob-${credContainer.type}-set-up`}
                                            className="pf-c-button pf-m-link"
                                            type="button"
                                            onClick={() =>
                                                setupAction.execute()
                                            }
                                        >
                                            <span className="pf-c-button__icon">
                                                <i
                                                    className="fa fa-plus-circle"
                                                    aria-hidden="true"
                                                ></i>
                                            </span>
                                            <Msg
                                                msgKey="setUpNew"
                                                params={[
                                                    credContainerDisplayName,
                                                ]}
                                            />
                                        </button>,
                                    ]}
                                />
                            </div>
                        )}
                        {credContainer.createAction && (
                            <div
                                id={"setUpAction-" + credContainer.type}
                                className="pf-u-display-none pf-u-display-inline-flex-on-lg pf-u-float-right"
                            >
                                <button
                                    id={`${credContainer.type}-set-up`}
                                    className="pf-c-button pf-m-link"
                                    type="button"
                                    onClick={() => setupAction.execute()}
                                >
                                    <span className="pf-c-button__icon">
                                        <i
                                            className="fa fa-plus-circle"
                                            aria-hidden="true"
                                        ></i>
                                    </span>
                                    <Msg
                                        msgKey="setUpNew"
                                        params={[credContainerDisplayName]}
                                    />
                                </button>
                            </div>
                        )}
                    </SplitItem>
                </Split>
            </React.Fragment>
        );
    }
}

type CredRemover = (credentialId: string, userLabel: string) => void;
interface CredentialActionProps {
    credential: UserCredential;
    removeable: boolean;
    updateAction: AIACommand;
    credRemover: CredRemover;
};

class CredentialAction extends React.Component<CredentialActionProps> {
    render(): React.ReactNode {
        if (this.props.updateAction) {
            return (
                <DataListAction
                    aria-labelledby={Msg.localize('updateCredAriaLabel')}
                    aria-label={Msg.localize('updateCredAriaLabel')}
                    id={"updateAction-" + this.props.credential.id}
                >
                    <Button
                        variant="secondary"
                        id={`${SigningInPage.credElementId(
                            this.props.credential.type,
                            this.props.credential.id,
                            "update"
                        )}`}
                        onClick={() => this.props.updateAction.execute()}
                    >
                        <Msg msgKey="update" />
                    </Button>
                </DataListAction>
            );
        }

        if (this.props.removeable) {
            const userLabel: string = this.props.credential.userLabel;
            return (
                <DataListAction
                  aria-label={Msg.localize('removeCredAriaLabel')}
                  aria-labelledby={Msg.localize('removeCredAriaLabel')}
                  id={'removeAction-' + this.props.credential.id }
                >
                    <ContinueCancelModal 
                        buttonTitle='remove'
                        buttonVariant='danger'
                        buttonId={`${SigningInPage.credElementId(this.props.credential.type, this.props.credential.id, 'remove')}`}
                        modalTitle={Msg.localize('removeCred', [userLabel])}
                        modalMessage={Msg.localize('stopUsingCred', [userLabel])}
                        onContinue={() => this.props.credRemover(this.props.credential.id, userLabel)}
                    />
                </DataListAction>
            );
        }

        return <></>;
    }
}

const SigningInPageWithRouter = withRouter(SigningInPage);
export { SigningInPageWithRouter as SigningInPage };
