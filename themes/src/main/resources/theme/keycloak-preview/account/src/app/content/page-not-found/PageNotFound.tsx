/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import * as React from 'react';

import { WarningTriangleIcon } from '@patternfly/react-icons';
import {withRouter, RouteComponentProps} from 'react-router-dom';
import {Msg} from '../../widgets/Msg';
import EmptyMessageState from '../../widgets/EmptyMessageState';

export interface PageNotFoundProps extends RouteComponentProps {}

class PgNotFound extends React.Component<PageNotFoundProps> {

    public constructor(props: PageNotFoundProps) {
        super(props);
    }

    public render(): React.ReactNode {
        return (
            <EmptyMessageState icon={WarningTriangleIcon} messageKey="pageNotFound">
                <Msg msgKey="invalidRoute" params={[this.props.location.pathname]} />
            </EmptyMessageState>
        );
    }
};

export const PageNotFound = withRouter(PgNotFound);