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
  Grid,
  GridItem,
  Button,
} from '@patternfly/react-core';

import { InfoAltIcon, CheckIcon, BuilderImageIcon, ExternalLinkAltIcon } from '@patternfly/react-icons';
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
      <ContentPage title={Msg.localize('applicationsPageTitle')}>
        <DataList id="applications-list" aria-label={Msg.localize('applicationsPageTitle')} isCompact>
          <DataListItem id="applications-list-header" aria-labelledby="Columns names">
            <DataListItemRow>
              // invisible toggle allows headings to line up properly
              <span style={{ visibility: 'hidden' }}>
                <DataListToggle
                  isExpanded={false}
                  id='applications-list-header-invisible-toggle'
                  aria-controls="hidden"
                />
              </span>
              <DataListItemCells
                dataListCells={[
                  <DataListCell key='applications-list-client-id-header' width={2}>
                    <strong><Msg msgKey='applicationName' /></strong>
                  </DataListCell>,
                  <DataListCell key='applications-list-app-type-header' width={2}>
                    <strong><Msg msgKey='applicationType' /></strong>
                  </DataListCell>,
                  <DataListCell key='applications-list-status' width={2}>
                    <strong><Msg msgKey='status' /></strong>
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          {this.state.applications.map((application: Application, appIndex: number) => {
            return (
              <DataListItem id={this.elementId("client-id", application)} key={'application-' + appIndex} aria-labelledby="applications-list" isExpanded={this.state.isRowOpen[appIndex]}>
                <DataListItemRow>
                  <DataListToggle
                    onClick={() => this.onToggle(appIndex)}
                    isExpanded={this.state.isRowOpen[appIndex]}
                    id={this.elementId('toggle', application)}
                    aria-controls={this.elementId("expandable", application)}
                  />
                  <DataListItemCells
                    dataListCells={[
                      <DataListCell id={this.elementId('name', application)} width={2} key={'app-' + appIndex}>
                        <Button component="a" variant="link" onClick={() => window.open(application.effectiveUrl)}>
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
                  noPadding={false}
                  aria-label={Msg.localize('applicationDetails')}
                  id={this.elementId("expandable", application)}
                  isHidden={!this.state.isRowOpen[appIndex]}
                >
                  <Grid sm={6} md={6} lg={6}>
                    <div className='pf-c-content'>
                      <GridItem><strong>{Msg.localize('client') + ': '}</strong> {application.clientId}</GridItem>
                      {application.description &&
                        <GridItem><strong>{Msg.localize('description') + ': '}</strong> {application.description}</GridItem>
                      }
                      {application.effectiveUrl &&
                        <GridItem><strong>URL: </strong> <span id={this.elementId('effectiveurl', application)}>{application.effectiveUrl.split('"')}</span></GridItem>
                      }
                      {application.consent &&
                        <React.Fragment>
                          <GridItem span={12}>
                            <strong>Has access to:</strong>
                          </GridItem>
                          {application.consent.grantedScopes.map((scope: GrantedScope, scopeIndex: number) => {
                            return (
                              <React.Fragment key={'scope-' + scopeIndex} >
                                <GridItem offset={1}><CheckIcon /> {scope.name}</GridItem>
                              </React.Fragment>
                            )
                          })}
                          {application.tosUri && <GridItem><strong>{Msg.localize('termsOfService') + ': '}</strong>{application.tosUri}</GridItem>}
                          {application.policyUri && <GridItem><strong>{Msg.localize('policy') + ': '}</strong>{application.policyUri}</GridItem>}
                          <GridItem><strong>{Msg.localize('accessGrantedOn') + ': '}</strong>
                            {new Intl.DateTimeFormat(locale, {
                              year: 'numeric',
                              month: 'long',
                              day: 'numeric',
                              hour: 'numeric',
                              minute: 'numeric',
                              second: 'numeric'
                            }).format(application.consent.createDate)}
                          </GridItem>
                        </React.Fragment>
                      }
                    </div>
                    {application.logoUri && <div className='pf-c-content'><img src={application.logoUri} /></div> }
                  </Grid>
                  {(application.consent || application.offlineAccess) &&
                    <Grid gutter='sm'>
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
      </ContentPage>
    );
  }
};
