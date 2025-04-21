import * as React from 'react';
export interface WizardNavProps {
    /** children should be WizardNavItem components */
    children?: any;
    /** Aria-label applied to the nav element */
    'aria-label'?: string;
    /** Sets the aria-labelledby attribute on the nav element */
    'aria-labelledby'?: string;
    /** Whether the nav is expanded */
    isOpen?: boolean;
    /** True to return the inner list without the wrapping nav element */
    returnList?: boolean;
}
export declare const WizardNav: React.FunctionComponent<WizardNavProps>;
//# sourceMappingURL=WizardNav.d.ts.map