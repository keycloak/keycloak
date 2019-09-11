/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import * as React from 'react';

import {EmptyState, EmptyStateBody, EmptyStateIcon, Title, TitleLevel} from '@patternfly/react-core';
import { WarningTriangleIcon } from '@patternfly/react-icons';
import {withRouter, RouteComponentProps} from 'react-router-dom';
import {Msg} from '../../widgets/Msg';
 
export interface PageNotFoundProps extends RouteComponentProps {}
 
class PgNotFound extends React.Component<PageNotFoundProps> {
    
    public constructor(props: PageNotFoundProps) {
        super(props);
    }

    public render(): React.ReactNode {
        return (
            <EmptyState variant='full'>
                <EmptyStateIcon icon={WarningTriangleIcon} />
                <Title headingLevel={TitleLevel.h5} size="lg">
                    <Msg msgKey='pageNotFound'/>
                </Title>
                <EmptyStateBody>
                    <Msg msgKey='invalidRoute' params={[this.props.location.pathname]} />
                </EmptyStateBody>
            </EmptyState>
        );
    }
};

export const PageNotFound = withRouter(PgNotFound);