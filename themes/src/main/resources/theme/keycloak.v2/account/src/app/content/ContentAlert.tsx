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
import { Alert, AlertActionCloseButton, AlertGroup, AlertVariant } from '@patternfly/react-core';
import { Msg } from '../widgets/Msg';

interface ContentAlertProps { }

interface ContentAlertState {
    alerts: {
        key: number;
        message: string;
        variant: AlertVariant;
    }[];
}
export class ContentAlert extends React.Component<ContentAlertProps, ContentAlertState> {
    private static instance: ContentAlert;

    private constructor(props: ContentAlertProps) {
        super(props);

        this.state = {
            alerts: []
        };
        ContentAlert.instance = this;
    }

    /**
     * @param message A literal text message or localization key.
     */
    public static success(message: string, params?: string[]): void {
        ContentAlert.instance.postAlert(AlertVariant.success, message, params);
    }

    /**
     * @param message A literal text message or localization key.
     */
    public static danger(message: string, params?: string[]): void {
        ContentAlert.instance.postAlert(AlertVariant.danger, message, params);
    }

    /**
     * @param message A literal text message or localization key.
     */
    public static warning(message: string, params?: string[]): void {
        ContentAlert.instance.postAlert(AlertVariant.warning, message, params);
    }

    /**
     * @param message A literal text message or localization key.
     */
    public static info(message: string, params?: string[]): void {
        ContentAlert.instance.postAlert(AlertVariant.info, message, params);
    }

    private hideAlert = (key: number) => {
        this.setState({ alerts: [...this.state.alerts.filter(el => el.key !== key)] });
    }

    private getUniqueId = () => (new Date().getTime());

    private postAlert = (variant: AlertVariant, message: string, params?: string[]) => {
        const alerts = this.state.alerts;
        const key = this.getUniqueId();
        alerts.push({
            key,
            message: Msg.localize(message, params),
            variant
        });
        this.setState({ alerts });

        if (variant !== AlertVariant.danger) {
            setTimeout(() => this.hideAlert(key), 8000);
        }
    }

    public render(): React.ReactNode {
        return (
            <AlertGroup isToast aria-live="assertive">
                {this.state.alerts.map(({ key, variant, message }) => (
                    <Alert
                        aria-details={message}
                        isLiveRegion
                        variant={variant}
                        title={message}
                        action={
                            <AlertActionCloseButton
                                title={message}
                                variantLabel={`${variant} alert`}
                                onClose={() => this.hideAlert(key)}
                            />
                        }
                        key={key} />
                ))}
            </AlertGroup>
        );
    }
}