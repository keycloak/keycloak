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

import {HttpResponse} from '../../account-service/account.service';
import { AccountServiceContext } from '../../account-service/AccountServiceContext';
import TimeUtil from '../../util/TimeUtil';

import {
  Button,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListContent,
  DescriptionList,
  DescriptionListTerm,
  DescriptionListDescription,
  DescriptionListGroup,
  Grid,
  GridItem,
  Label,
  PageSection,
  PageSectionVariants,
  Title,
  Tooltip,
  SplitItem,
  Split
} from '@patternfly/react-core';

import {
        DesktopIcon,
        MobileAltIcon,
        SyncAltIcon,
} from '@patternfly/react-icons';

import {Msg} from '../../widgets/Msg';
import {ContinueCancelModal} from '../../widgets/ContinueCancelModal';
import { KeycloakService } from '../../keycloak-service/keycloak.service';
import { KeycloakContext } from '../../keycloak-service/KeycloakContext';

import {ContentPage} from '../ContentPage';
import { ContentAlert } from '../ContentAlert';

export interface DeviceActivityPageProps {
}

export interface DeviceActivityPageState {
  devices: Device[];
}

interface Device {
  browser: string;
  current: boolean;
  device: string;
  ipAddress: string;
  lastAccess: number;
  mobile: boolean;
  os: string;
  osVersion: string;
  sessions: Session[];
}

interface Session {
  browser: string;
  current: boolean;
  clients: Client[];
  expires: number;
  id: string;
  ipAddress: string;
  lastAccess: number;
  started: number;
}

interface Client {
  clientId: string;
  clientName: string;
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 */
export class DeviceActivityPage extends React.Component<DeviceActivityPageProps, DeviceActivityPageState> {
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(props: DeviceActivityPageProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;

        this.state = {
          devices: []
        };

        this.fetchDevices();
    }

    private signOutAll = (keycloakService: KeycloakService) => {
      this.context!.doDelete("/sessions")
        .then( () => {
          keycloakService.logout();
        });
    }

    private signOutSession = (device: Device, session: Session) => {
      this.context!.doDelete("/sessions/" + session.id)
          .then (() => {
            this.fetchDevices();
            ContentAlert.success('signedOutSession', [session.browser, device.os]);
          });
    }

    private fetchDevices(): void {
      this.context!.doGet<Device[]>("/sessions/devices")
          .then((response: HttpResponse<Device[]>) => {
            console.log({response});

            let devices: Device[] = this.moveCurrentToTop(response.data as Device[]);

            this.setState({
              devices: devices
            });

          });
    }

    // current device and session should display at the top of their respective lists
    private moveCurrentToTop(devices: Device[]): Device[] {
      let currentDevice: Device = devices[0];

      devices.forEach((device: Device, index: number) => {
        if (device.current) {
          currentDevice = device;
          devices.splice(index, 1);
          devices.unshift(device);
        }
      });

      currentDevice.sessions.forEach((session: Session, index: number) => {
        if (session.current) {
          const currentSession: Session[] = currentDevice.sessions.splice(index, 1);
          currentDevice.sessions.unshift(currentSession[0]);
        }
      });

      return devices;
    }

    private time(time: number): string {
      return TimeUtil.format(time * 1000);
    }

    private elementId(item: string, session: Session, element: string='session'): string {
        return `${element}-${session.id.substring(0,7)}-${item}`;
    }

    private findDeviceTypeIcon(session: Session, device: Device): React.ReactNode {
      const deviceType: boolean = device.mobile;
      if (deviceType === true) return (<MobileAltIcon id={this.elementId('icon-mobile', session, 'device')} />);

      return (<DesktopIcon id={this.elementId('icon-desktop', session, 'device')} />);
    }

    private findOS(device: Device): string {
      if (device.os.toLowerCase().includes('unknown')) return Msg.localize('unknownOperatingSystem');

      return device.os;
    }

    private findOSVersion(device: Device): string {
      if (device.osVersion.toLowerCase().includes('unknown')) return '';

      return device.osVersion;
    }

    private makeClientsString(clients: Client[]): string {
      let clientsString = "";
      clients.forEach( (client: Client, index: number) => {
        let clientName: string;
        if (client.hasOwnProperty('clientName') && (client.clientName !== undefined) && (client.clientName !== '')) {
          clientName = Msg.localize(client.clientName);
        } else {
          clientName = client.clientId;
        }

        clientsString += clientName;

        if (clients.length > index + 1) clientsString += ', ';
      })

      return clientsString;
    }

    private isShowSignOutAll(devices: Device[]): boolean {
      if (devices.length === 0) return false;
      if (devices.length > 1) return true;
      if (devices[0].sessions.length > 1) return true;

      return false;
    }

    public render(): React.ReactNode {

      return (
          <ContentPage 
            title="device-activity" 
            introMessage="signedInDevicesExplanation" 
          >
            <PageSection isFilled variant={PageSectionVariants.light}>
            <Split hasGutter className="pf-u-mb-lg">
              <SplitItem isFilled>
                <div id="signedInDevicesTitle" className="pf-c-content"><Title headingLevel="h2" size="xl"><Msg msgKey="signedInDevices"/></Title></div>
              </SplitItem>
              <SplitItem>
                <Tooltip content={<Msg msgKey="refreshPage" />}>
                  <Button
                    aria-describedby="refresh page"
                    id="refresh-page"
                    variant="link"
                    onClick={this.fetchDevices.bind(this)}
                    icon={<SyncAltIcon />}
                  >
                    Refresh
                  </Button>
                </Tooltip>
              </SplitItem>
              <SplitItem>
              <KeycloakContext.Consumer>
                { (keycloak: KeycloakService) => (
                    this.isShowSignOutAll(this.state.devices) &&
                      <ContinueCancelModal buttonTitle='signOutAllDevices'
                                    buttonId='sign-out-all'
                                    modalTitle='signOutAllDevices'
                                    modalMessage='signOutAllDevicesWarning'
                                    onContinue={() => this.signOutAll(keycloak)}
                      />
                )}
              </KeycloakContext.Consumer>
              </SplitItem>
            </Split>
            <DataList className="signed-in-device-list" aria-label={Msg.localize('signedInDevices')}>
              <DataListItem aria-labelledby='sessions' id='device-activity-sessions'>
                {this.state.devices.map((device: Device, deviceIndex: number) => {
                  return (
                    <React.Fragment>
                      {device.sessions.map((session: Session, sessionIndex: number) => {
                        return (
                          <React.Fragment key={'device-' + deviceIndex + '-session-' + sessionIndex}>
                            <DataListItemRow>
                              <DataListContent aria-label="device-sessions-content" isHidden={false} className="pf-u-flex-grow-1">
                                <Grid className="signed-in-device-grid" hasGutter>
                                  <GridItem className="device-icon" span={1} rowSpan={2}>
                                    <span>{this.findDeviceTypeIcon(session, device)}</span>
                                  </GridItem>
                                  <GridItem sm={8} md={9} span={10}>
                                    <span id={this.elementId('browser', session)} className="pf-u-mr-md">{this.findOS(device)} {this.findOSVersion(device)} / {session.browser}</span>
                                    {session.current &&
                                      <Label color="green"><Msg msgKey="currentSession" /></Label>}
                                  </GridItem>
                                  <GridItem className="pf-u-text-align-right" sm={3} md={2} span={1}>
                                    {!session.current &&
                                      <ContinueCancelModal buttonTitle='doSignOut'
                                        buttonId={this.elementId('sign-out', session)}
                                        modalTitle='doSignOut'
                                        buttonVariant='secondary'
                                        modalMessage='signOutWarning'
                                        onContinue={() => this.signOutSession(device, session)}
                                      />
                                    }
                                  </GridItem>
                                  <GridItem span={11}>
                                    <DescriptionList columnModifier={{ sm: '2Col', lg: '3Col' }}>
                                      <DescriptionListGroup>
                                        <DescriptionListTerm>{Msg.localize('ipAddress')}</DescriptionListTerm>
                                        <DescriptionListDescription>{session.ipAddress}</DescriptionListDescription>
                                      </DescriptionListGroup>
                                      <DescriptionListGroup>
                                        <DescriptionListTerm>{Msg.localize('lastAccessedOn')}</DescriptionListTerm>
                                        <DescriptionListDescription>{this.time(session.lastAccess)}</DescriptionListDescription>
                                      </DescriptionListGroup>
                                      <DescriptionListGroup>
                                        <DescriptionListTerm>{Msg.localize('clients')}</DescriptionListTerm>
                                        <DescriptionListDescription>{this.makeClientsString(session.clients)}</DescriptionListDescription>
                                      </DescriptionListGroup>
                                      <DescriptionListGroup>
                                        <DescriptionListTerm>{Msg.localize('started')}</DescriptionListTerm>
                                        <DescriptionListDescription>{this.time(session.started)}</DescriptionListDescription>
                                      </DescriptionListGroup>
                                      <DescriptionListGroup>
                                        <DescriptionListTerm>{Msg.localize('expires')}</DescriptionListTerm>
                                        <DescriptionListDescription>{this.time(session.expires)}</DescriptionListDescription>
                                      </DescriptionListGroup>
                                    </DescriptionList>
                                  </GridItem>
                                </Grid>
                              </DataListContent>
                            </DataListItemRow>
                          </React.Fragment>
                        );
                      })}
                    </React.Fragment>
                  )
                })}
              </DataListItem>
            </DataList>
          </PageSection>
        </ContentPage>
      );
    }
};
