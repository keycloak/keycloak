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
import * as moment from 'moment';
import {AxiosResponse} from 'axios';

import {withRouter, RouteComponentProps} from 'react-router-dom';
import {
        Button,
        DataList, 
        DataListAction,
        DataListItemCells, 
        DataListCell,
        DataListItemRow, 
        Stack,
        StackItem,
        Switch,
        Title, 
        TitleLevel
    } from '@patternfly/react-core';

import {AIACommand} from '../../util/AIACommand';
import {AccountServiceClient} from '../../account-service/account.service';
import {ContinueCancelModal} from '../../widgets/ContinueCancelModal';
import {Features} from '../../widgets/features';
import {Msg} from '../../widgets/Msg';
import {ContentPage} from '../ContentPage';
import {ContentAlert} from '../ContentAlert';

declare const features: Features;

interface PasswordDetails {
    registered: boolean;
    lastUpdate: number;
}
 
interface SigningInPageProps extends RouteComponentProps {
}

interface SigningInPageState {
    twoFactorEnabled: boolean;
    twoFactorEnabledText: string;
    isTotpConfigured: boolean;
    lastPasswordUpdate?: number;
}

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2018 Red Hat Inc.
 */
class SigningInPage extends React.Component<SigningInPageProps, SigningInPageState> {
    private readonly updatePassword: AIACommand = new AIACommand('UPDATE_PASSWORD', this.props.location.pathname);
    private readonly setUpTOTP: AIACommand = new AIACommand('CONFIGURE_TOTP', this.props.location.pathname);
    
    public constructor(props: SigningInPageProps) {
        super(props);
        this.state = {
            twoFactorEnabled: true,
            twoFactorEnabledText: Msg.localize('twoFactorEnabled'),
            isTotpConfigured: features.isTotpConfigured,
        }
        this.setLastPwdUpdate();
    }

    private setLastPwdUpdate(): void {
        AccountServiceClient.Instance.doGet("/credentials/password")
            .then((response: AxiosResponse<PasswordDetails>) => {
                if (response.data.lastUpdate) {
                    const lastUpdate: number = response.data.lastUpdate;
                    this.setState({lastPasswordUpdate: lastUpdate});
                }
            });
    }
    
    private handleTwoFactorSwitch = () => {
        if (this.state.twoFactorEnabled) {
            this.setState({twoFactorEnabled: false, twoFactorEnabledText: Msg.localize('twoFactorDisabled')})
        } else {
            this.setState({twoFactorEnabled: true, twoFactorEnabledText:  Msg.localize('twoFactorEnabled')})
        }
    }

    private handleRemoveTOTP = () => {
      AccountServiceClient.Instance.doDelete("/totp/remove")
        .then(() => {
            this.setState({isTotpConfigured: false});
            ContentAlert.success('successTotpRemovedMessage');
        });
    }

    public render(): React.ReactNode {
        let lastPwdUpdate: string = Msg.localize('unknown');
        if (this.state.lastPasswordUpdate) {
            lastPwdUpdate = moment(this.state.lastPasswordUpdate).format('LLL');
        }

        return (
            <ContentPage title="signingIn" 
                     introMessage="signingInSubMessage">
                <Stack gutter='md'>
                    <StackItem isFilled>
                        <Title headingLevel={TitleLevel.h2} size='2xl'>
                        <strong><Msg msgKey='password'/></strong>
                        </Title>
                        <DataList aria-label='foo'>
                            <DataListItemRow>
                                <DataListItemCells
                                    dataListCells={[
                                        <DataListCell key='password'><Msg msgKey='password'/></DataListCell>,
                                        <DataListCell key='lastPwdUpdate'><strong><Msg msgKey='lastUpdate'/>: </strong>{lastPwdUpdate}</DataListCell>,
                                        <DataListCell key='spacer'/>
                                    ]}/>
                                <DataListAction aria-labelledby='foo' aria-label='foo action' id='setPasswordAction'>
                                    <Button variant='primary'onClick={()=> this.updatePassword.execute()}><Msg msgKey='update'/></Button>
                                </DataListAction>
                            </DataListItemRow>
                        </DataList>
                    </StackItem>
                    <StackItem isFilled>
                        <Title headingLevel={TitleLevel.h2} size='2xl'>
                        <strong><Msg msgKey='twoFactorAuth'/></strong>
                        </Title>
                        <DataList aria-label='foo'>
                            <DataListItemRow>
                                <DataListAction aria-labelledby='foo' aria-label='foo action' id='twoFactorOnOff'>
                                        <Switch
                                            aria-label='twoFactorSwitch'
                                            label={this.state.twoFactorEnabledText}
                                            isChecked={this.state.twoFactorEnabled}
                                            onClick={this.handleTwoFactorSwitch}  
                                        />
                                    </DataListAction>
                                </DataListItemRow>
                            <DataListItemRow>
                                <DataListItemCells
                                    dataListCells={[
                                        <DataListCell key='mobileAuth'><Msg msgKey='mobileAuthDefault'/></DataListCell>
                                    ]}/>
                                {!this.state.isTotpConfigured && 
                                    <DataListAction aria-labelledby='foo' aria-label='foo action' id='setMobileAuthAction'>
                                        <Button isDisabled={!this.state.twoFactorEnabled} variant='primary' onClick={()=> this.setUpTOTP.execute()}><Msg msgKey='setUp'/></Button>
                                    </DataListAction>
                                }
                                {this.state.isTotpConfigured &&
                                    <DataListAction aria-labelledby='foo' aria-label='foo action' id='setMobileAuthAction'>
                                        <ContinueCancelModal buttonTitle='remove'
                                                            isDisabled={!this.state.twoFactorEnabled}
                                                            modalTitle={Msg.localize('removeMobileAuth')}
                                                            modalMessage={Msg.localize('stopMobileAuth')}
                                                            onContinue={this.handleRemoveTOTP}
                                            />
                                    </DataListAction>
                                }
                            </DataListItemRow>
                        </DataList>
                    </StackItem>
                    <StackItem isFilled>
                        <Title headingLevel={TitleLevel.h2} size='2xl'>
                        <strong><Msg msgKey='passwordless'/></strong>
                        </Title>
                    </StackItem>
                </Stack>
            </ContentPage>
        );
    }

};

const SigningInPageWithRouter = withRouter(SigningInPage);
export { SigningInPageWithRouter as SigningInPage};