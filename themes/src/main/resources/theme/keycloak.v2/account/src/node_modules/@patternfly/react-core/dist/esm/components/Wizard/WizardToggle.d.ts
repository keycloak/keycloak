import * as React from 'react';
import { WizardStep } from './Wizard';
export interface WizardToggleProps {
    /** Function that returns the WizardNav component */
    nav: (isWizardNavOpen: boolean) => React.ReactElement;
    /** The wizard steps */
    steps: WizardStep[];
    /** The currently active WizardStep */
    activeStep: WizardStep;
    /** The WizardFooter */
    children?: React.ReactNode;
    /** Set to true to remove body padding */
    hasNoBodyPadding: boolean;
    /** If the nav is open */
    isNavOpen: boolean;
    /** Callback function for when the nav is toggled */
    onNavToggle: (isOpen: boolean) => void;
    /** The button's aria-label */
    'aria-label'?: string;
    /** Sets aria-labelledby on the main element */
    mainAriaLabelledBy?: string;
    /** The main's aria-label */
    mainAriaLabel?: string;
    /** If the wizard is in-page */
    isInPage?: boolean;
    /** @beta Flag indicating the wizard has a drawer for at least one of the wizard steps */
    hasDrawer?: boolean;
    /** @beta Flag indicating the wizard drawer is expanded */
    isDrawerExpanded?: boolean;
}
export declare const WizardToggle: React.FunctionComponent<WizardToggleProps>;
//# sourceMappingURL=WizardToggle.d.ts.map