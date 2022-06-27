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
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListToggle,
  DataListContent,
  DataListItemCells,
  DescriptionList,
  DescriptionListTerm,
  DescriptionListGroup,
  DescriptionListDescription,
  Grid,
  GridItem,
  Button,
  PageSection,
  PageSectionVariants,
  Stack,
  StackItem,
  SplitItem,
  Split,
  TextContent
} from '@patternfly/react-core';

import { InfoAltIcon, CheckIcon, ExternalLinkAltIcon } from '@patternfly/react-icons';
import { ContentPage } from '../ContentPage';
import { ContinueCancelModal } from '../../widgets/ContinueCancelModal';
import { HttpResponse } from '../../account-service/account.service';
import { AccountServiceContext } from '../../account-service/AccountServiceContext';
import { Msg } from '../../widgets/Msg';

declare const locale: string;

export interface ApplicationsPageProps {
}

export interface ApplicationsPageState {
  isRowOpen: boolean[];
  applications: Application[];
}

export interface GrantedScope {
  displayTest: string;
  id: string;
  name: string;
}

export interface Consent {
  createDate: number;
  grantedScopes: GrantedScope[];
  lastUpdatedDate: number;
}

interface Application {
  effectiveUrl: string;
  clientId: string;
  clientName: string;
  consent: Consent;
  description: string;
  inUse: boolean;
  offlineAccess: boolean;
  userConsentRequired: boolean;
  scope: string[];
  logoUri: string;
  policyUri: string;
  tosUri: string;
}

export class ApplicationsPage extends React.Component<ApplicationsPageProps, ApplicationsPageState> {
  static contextType = AccountServiceContext;
  context: React.ContextType<typeof AccountServiceContext>;

  public constructor(props: ApplicationsPageProps, context: React.ContextType<typeof AccountServiceContext>) {
    super(props);
    this.context = context;
    this.state = {
      isRowOpen: [],
      applications: []
    };

    this.fetchApplications();
  }

  private removeConsent = (clientId: string) => {
    this.context!.doDelete("/applications/" + clientId + "/consent")
      .then(() => {
        this.fetchApplications();
      });
  }

  private onToggle = (row: number): void => {
    const newIsRowOpen: boolean[] = this.state.isRowOpen;
    newIsRowOpen[row] = !newIsRowOpen[row];
    this.setState({ isRowOpen: newIsRowOpen });
  };

  private fetchApplications(): void {
    this.context!.doGet<Application[]>("/applications")
      .then((response: HttpResponse<Application[]>) => {
        const applications = response.data || [];
        this.setState({
          isRowOpen: new Array(applications.length).fill(false),
          applications: applications
        });
      });
  }

  private elementId(item: string, application: Application): string {
    return `application-${item}-${application.clientId}`;
  }

  public render(): React.ReactNode {
    return (
      <ContentPage
        title={Msg.localize('applicationsPageTitle')}
        introMessage={Msg.localize('applicationsPageSubTitle')}
      >
        <PageSection isFilled variant={PageSectionVariants.light}>

          <Stack hasGutter>
            <DataList id="applications-list" aria-label={Msg.localize('applicationsPageTitle')}>
              <DataListItem id="applications-list-header" aria-labelledby="Columns names">
                <DataListItemRow>
                  // invisible toggle allows headings to line up properly
                  <span style={{ visibility: 'hidden', height: 55 }}>
                    <DataListToggle
                      isExpanded={false}
                      id='applications-list-header-invisible-toggle'
                      aria-controls="hidden"
                    />
                  </span>
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell key='applications-list-client-id-header' width={2} className="pf-u-pt-md">
                        <strong><Msg msgKey='applicationName' /></strong>
                      </DataListCell>,
                      <DataListCell key='applications-list-app-type-header' width={2} className="pf-u-pt-md">
                        <strong><Msg msgKey='applicationType' /></strong>
                      </DataListCell>,
                      <DataListCell key='applications-list-status' width={2} className="pf-u-pt-md">
                        <strong><Msg msgKey='status' /></strong>
                      </DataListCell>,
                    ]}
                  />
                </DataListItemRow>
              </DataListItem>
              {this.state.applications.map((application: Application, appIndex: number) => {
                return (
                  <DataListItem id={this.elementId("client-id", application)} key={'application-' + appIndex} aria-labelledby="applications-list" isExpanded={this.state.isRowOpen[appIndex]}>
                    <DataListItemRow className="pf-u-align-items-center">
                      <DataListToggle
                        onClick={() => this.onToggle(appIndex)}
                        isExpanded={this.state.isRowOpen[appIndex]}
                        id={this.elementId('toggle', application)}
                        aria-controls={this.elementId("expandable", application)}
                      />
                      <DataListItemCells
                        className="pf-u-align-items-center"
                        dataListCells={[
                          <DataListCell id={this.elementId('name', application)} width={2} key={'app-' + appIndex}>
                            <Button className="pf-u-pl-0 title-case" component="a" variant="link" onClick={() => window.open(application.effectiveUrl)}>
                              {application.clientName || application.clientId} <ExternalLinkAltIcon/>
                            </Button>
                          </DataListCell>,
                          <DataListCell id={this.elementId('internal', application)} width={2} key={'internal-' + appIndex}>
                            {application.userConsentRequired ? Msg.localize('thirdPartyApp') : Msg.localize('internalApp')}
                            {application.offlineAccess ? ', ' + Msg.localize('offlineAccess') : ''}
                          </DataListCell>,
                          <DataListCell id={this.elementId('status', application)} width={2} key={'status-' + appIndex}>
                            {application.inUse ? Msg.localize('inUse') : Msg.localize('notInUse')}
                          </DataListCell>
                        ]}
                      />
                  </DataListItemRow>
                  
                  <DataListContent
                    className="pf-u-pl-35xl"
                    hasNoPadding={false}
                    aria-label={Msg.localize('applicationDetails')}
                    id={this.elementId("expandable", application)}
                    isHidden={!this.state.isRowOpen[appIndex]}
                  >
                    <DescriptionList>
                      <DescriptionListGroup>
                        <DescriptionListTerm>{Msg.localize('client')}</DescriptionListTerm>
                        <DescriptionListDescription>{application.clientId}</DescriptionListDescription>
                      </DescriptionListGroup>
                      {application.description &&
                        <DescriptionListGroup>
                          <DescriptionListTerm>{Msg.localize('description')}</DescriptionListTerm>
                          <DescriptionListDescription>{application.description}</DescriptionListDescription>
                        </DescriptionListGroup>
                      }
                      {application.effectiveUrl &&
                        <DescriptionListGroup>
                          <DescriptionListTerm>URL</DescriptionListTerm>
                          <DescriptionListDescription id={this.elementId("effectiveurl", application)}>
                            {application.effectiveUrl.split('"')}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                      }
                      {application.consent &&
                        <React.Fragment>
                          <DescriptionListGroup>
                            <DescriptionListTerm>Has access to</DescriptionListTerm>
                            {application.consent.grantedScopes.map((scope: GrantedScope, scopeIndex: number) => {
                                return (
                                  <React.Fragment key={'scope-' + scopeIndex} >
                                    <DescriptionListDescription><CheckIcon /> {scope.name}</DescriptionListDescription>
                                  </React.Fragment>
                                )
                              })}
                          </DescriptionListGroup>
                          <DescriptionListGroup>
                            <DescriptionListTerm>{Msg.localize('accessGrantedOn') + ': '}</DescriptionListTerm>
                            <DescriptionListDescription>
                              {new Intl.DateTimeFormat(locale, {
                                  year: 'numeric',
                                  month: 'long',
                                  day: 'numeric',
                                  hour: 'numeric',
                                  minute: 'numeric',
                                  second: 'numeric'
                                }).format(application.consent.createDate)}
                            </DescriptionListDescription>
                          </DescriptionListGroup>
                        </React.Fragment>
                      }
                    </DescriptionList>
                    {(application.consent || application.offlineAccess) &&
                    <Grid hasGutter>
                      <hr />
                      <GridItem>
                        <React.Fragment>
                          <ContinueCancelModal
                            buttonTitle={Msg.localize('removeButton')} // required
                            buttonVariant='secondary' // defaults to 'primary'
                            modalTitle={Msg.localize('removeModalTitle')} // required
                            modalMessage={Msg.localize('removeModalMessage', [application.clientId])}
                            modalContinueButtonLabel={Msg.localize('confirmButton')} // defaults to 'Continue'
                            onContinue={() => this.removeConsent(application.clientId)} // required
                          />
                        </React.Fragment>
                      </GridItem>
                      <GridItem><InfoAltIcon /> {Msg.localize('infoMessage')}</GridItem>
                    </Grid>
                    }
                  </DataListContent>
                  </DataListItem>
                )
              })}
      </DataList>
      </Stack>
      </PageSection>
      </ContentPage>
    );
  }
};
