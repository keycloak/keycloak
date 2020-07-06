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

import {AIACommand} from '../../util/AIACommand';
import {PageDef} from '../../ContentPages';
import {Msg} from '../../widgets/Msg';

import {
  Title,
  TitleLevel,
  Button,
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  EmptyStateBody
} from '@patternfly/react-core';
import { PassportIcon } from '@patternfly/react-icons';
import { KeycloakService } from '../../keycloak-service/keycloak.service';
import { KeycloakContext } from '../../keycloak-service/KeycloakContext';

// Note: This class demonstrates two features of the ContentPages framework:
// 1) The PageDef is available as a React property.
// 2) You can add additional custom properties to the PageDef.  In this case,
//    we add a value called kcAction in content.js and access it by extending the
//    PageDef interface.
interface ActionPageDef extends PageDef {
    kcAction: string;
}

// Extend RouteComponentProps to get access to router information such as
// the hash-routed path associated with this page.  See this.props.location.pathname
// as used below.
interface AppInitiatedActionPageProps extends RouteComponentProps {
    pageDef: ActionPageDef;
}

/**
 * @author Stan Silvert
 */
class ApplicationInitiatedActionPage extends React.Component<AppInitiatedActionPageProps> {

    public constructor(props: AppInitiatedActionPageProps) {
        super(props);
    }

    private handleClick = (keycloak: KeycloakService): void => {
        new AIACommand(keycloak, this.props.pageDef.kcAction).execute();
    }

    public render(): React.ReactNode {
        return (
            <EmptyState variant={EmptyStateVariant.full}>
                <EmptyStateIcon icon={PassportIcon} />
                <Title headingLevel={TitleLevel.h5} size="lg">
                  <Msg msgKey={this.props.pageDef.label} params={this.props.pageDef.labelParams}/>
                </Title>
                <EmptyStateBody>
                  <Msg msgKey="actionRequiresIDP"/>
                </EmptyStateBody>
                <KeycloakContext.Consumer>
                { keycloak => (
                    <Button variant="primary"
                            onClick={() => this.handleClick(keycloak!)}
                            target="_blank"><Msg msgKey="continue"/></Button>
                )}
                </KeycloakContext.Consumer>
            
            </EmptyState>
        );
    }
};

// Note that the class name is not exported above.  To get access to the router,
// we use withRouter() and export a different name.
export const AppInitiatedActionPage = withRouter(ApplicationInitiatedActionPage);