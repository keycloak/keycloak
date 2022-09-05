import * as React from 'react';

import {PageHeaderTools} from '@patternfly/react-core';
import {ReferrerLink} from './widgets/ReferrerLink';
import {LogoutButton} from './widgets/Logout';

declare const referrerName: string;
declare function loggedInUserName(): string;

export class PageHeaderTool extends React.Component {
    private hasReferrer: boolean = typeof referrerName !== 'undefined';

    public render(): React.ReactNode {
        const username = loggedInUserName();
    
        return (
            <PageHeaderTools>
                {this.hasReferrer &&
                    <div className="pf-c-page__header-tools-group">
                        <ReferrerLink/>
                    </div>
                }

                <div className="pf-c-page__header-tools-group">
                    <LogoutButton/>
                </div>

                <span style={{marginLeft: '10px'}} id="loggedInUser">{username}</span>
            </PageHeaderTools>
        );
    }
}
