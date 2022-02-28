import * as React from 'react';
export interface WizardBodyProps {
    /** Anything that can be rendered in the Wizard body */
    children: any;
    /** Set to false to remove the default body padding */
    hasBodyPadding: boolean;
}
export declare const WizardBody: React.FunctionComponent<WizardBodyProps>;
