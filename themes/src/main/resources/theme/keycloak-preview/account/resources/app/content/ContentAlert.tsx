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
import {Alert, AlertActionCloseButton} from '@patternfly/react-core';
import {Msg} from '../widgets/Msg';
 
interface ContentAlertProps {}

type AlertVariant = 'success' | 'danger' | 'warning' | 'info';
interface ContentAlertState {
    isVisible: boolean;
    message: string;
    variant: AlertVariant;
}
export class ContentAlert extends React.Component<ContentAlertProps, ContentAlertState> {
    private static instance: ContentAlert;

    private constructor(props: ContentAlertProps) {
        super(props);
        
        this.state = {isVisible: false, message: '', variant: 'success'};
        ContentAlert.instance = this;
    }
    
    /**
     * @param message A literal text message or localization key.
     */
    public static success(message: string, params?: string[]): void {
        ContentAlert.instance.postAlert('success', message, params);
    }
    
    /**
     * @param message A literal text message or localization key.
     */
    public static danger(message: string, params?: string[]): void {
        ContentAlert.instance.postAlert('danger', message, params);
    }
    
    /**
     * @param message A literal text message or localization key.
     */
    public static warning(message: string, params?: string[]): void {
        ContentAlert.instance.postAlert('warning', message, params);
    }
    
    /**
     * @param message A literal text message or localization key.
     */
    public static info(message: string, params?: string[]): void {
        ContentAlert.instance.postAlert('info', message, params);
    }
    
    private hideAlert = () => {
        this.setState({isVisible: false});
    }
    
    private postAlert = (variant: AlertVariant, message: string, params?: string[]) => {
        this.setState({isVisible: true, 
                       message: Msg.localize(message, params), 
                       variant});
        
        if (variant !== 'danger') {
            setTimeout(() => this.setState({isVisible: false}), 5000);
        }
    }
    
    public render(): React.ReactNode {
        return (
            <React.Fragment>
            { this.state.isVisible &&
                <section className="pf-c-page__main-section pf-m-light">
                    <Alert
                      id="content-alert"
                      title=''
                      variant={this.state.variant}
                      variantLabel=''
                      aria-label=''
                      action={<AlertActionCloseButton id="content-alert-close" onClose={this.hideAlert} />}
                    >
                        {this.state.message}
                    </Alert>
                </section>
            }
            </React.Fragment>
        );
    }
}