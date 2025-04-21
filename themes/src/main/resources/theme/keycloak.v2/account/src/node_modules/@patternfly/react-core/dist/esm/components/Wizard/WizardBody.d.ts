import * as React from 'react';
import { WizardStep } from './Wizard';
export interface WizardBodyProps {
    /** Anything that can be rendered in the Wizard body */
    children: any;
    /** Set to true to remove the default body padding */
    hasNoBodyPadding: boolean;
    /** An aria-label to use for the main element */
    'aria-label'?: string;
    /** Sets the aria-labelledby attribute for the main element */
    'aria-labelledby': string;
    /** Component used as the primary content container */
    mainComponent?: React.ElementType;
    /** The currently active WizardStep */
    activeStep: WizardStep;
    hasDrawer?: boolean;
    /** Flag indicating the wizard drawer is expanded */
    isDrawerExpanded?: boolean;
}
export declare const WizardBody: React.FunctionComponent<WizardBodyProps>;
//# sourceMappingURL=WizardBody.d.ts.map