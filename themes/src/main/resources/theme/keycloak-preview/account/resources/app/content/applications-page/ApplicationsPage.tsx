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
import { AxiosResponse } from 'axios';

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
} from '@patternfly/react-core';

import { InfoAltIcon, CheckIcon, BuilderImageIcon } from '@patternfly/react-icons';
import { ContentPage } from '../ContentPage';
import { ContinueCancelModal } from '../../widgets/ContinueCancelModal';
import { AccountServiceClient } from '../../account-service/account.service';
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
  baseUrl: string;
  clientId: string;
  clientName: string;
  consent: Consent;
  description: string;
  inUse: boolean;
  offlineAccess: boolean;
  userConsentRequired: boolean;
  scope: string[];
}

export class ApplicationsPage extends React.Component<ApplicationsPageProps, ApplicationsPageState> {

  public constructor(props: ApplicationsPageProps) {
    super(props);
    this.state = {
      isRowOpen: [],
      applications: []
    };

    this.fetchApplications();
  }

  private removeConsent = (clientId: string) => {
    AccountServiceClient.Instance.doDelete("/applications/" + clientId + "/consent")
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
    AccountServiceClient.Instance.doGet("/applications")
      .then((response: AxiosResponse<Application[]>) => {
        const applications = response.data;
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
        <DataList id="applications-list" aria-label={Msg.localize('applicationsPageTitle')}>
          {this.state.applications.map((application: Application, appIndex: number) => {
            const appUrl: string = application.userConsentRequired ? application.baseUrl : '/auth' + application.baseUrl;

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
                        <BuilderImageIcon size='sm' /> {application.clientName ? application.clientName : application.clientId}
                      </DataListCell>,
                      <DataListCell id={this.elementId('internal', application)} width={2} key={'internal-' + appIndex}>
                        {application.userConsentRequired ? Msg.localize('thirdPartyApp') : Msg.localize('internalApp')}
                        {application.offlineAccess ? ', ' + Msg.localize('offlineAccess') : ''}
                      </DataListCell>,
                      <DataListCell id={this.elementId('status', application)} width={2} key={'status-' + appIndex}>
                        {application.inUse ? Msg.localize('inUse') : Msg.localize('notInUse')}
                      </DataListCell>,
                      <DataListCell id={this.elementId('baseurl', application)} width={4} key={'baseUrl-' + appIndex}>
                        <button className="pf-c-button pf-m-link" type="button" onClick={() => window.open(appUrl)}>
                          <span className="pf-c-button__icon">
                            <i className="fas fa-link" aria-hidden="true"></i>
                          </span>{application.baseUrl}</button>
                      </DataListCell>,
                    ]}
                  />
                </DataListItemRow>
                <DataListContent
                  noPadding={false}
                  aria-label={Msg.localize('applicationDetails')}
                  id={this.elementId("expandable", application)}
                  isHidden={!this.state.isRowOpen[appIndex]}
                >
                  <Grid sm={12} md={12} lg={12}>
                    <div className='pf-c-content'>
                      <GridItem><strong>{Msg.localize('client') + ': '}</strong> {application.clientId}</GridItem>
                      {application.description &&
                        <GridItem><strong>{Msg.localize('description') + ': '}</strong> {application.description}</GridItem>
                      }
                      <GridItem><strong>{Msg.localize('baseUrl') + ': '}</strong> {application.baseUrl}</GridItem>
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
                  </Grid>
                  <Grid gutter='sm'>
                    <hr />
                    {application.consent &&
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
                    }
                    <GridItem><InfoAltIcon /> {Msg.localize('infoMessage')}</GridItem>
                  </Grid>
                </DataListContent>
              </DataListItem>
            )
          })}
        </DataList>
      </ContentPage>
    );
  }
};
