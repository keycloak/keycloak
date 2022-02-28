import * as React from 'react';
export interface WizardNavProps {
    /** children should be WizardNavItem components */
    children?: any;
    /** aria-label applied to the nav element */
    ariaLabel?: string;
    /** Whether the nav is expanded */
    isOpen?: boolean;
    /** True to return the inner list without the wrapping nav element */
    returnList?: boolean;
}
export declare const WizardNav: React.FunctionComponent<WizardNavProps>;
