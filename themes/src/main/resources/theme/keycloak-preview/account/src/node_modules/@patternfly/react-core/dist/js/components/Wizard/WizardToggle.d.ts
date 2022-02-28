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
    children: React.ReactNode;
    /** Set to false to remove body padding */
    hasBodyPadding: boolean;
    /** If the nav is open */
    isNavOpen: boolean;
    /** Callback function for when the nav is toggled */
    onNavToggle: (isOpen: boolean) => void;
}
export declare const WizardToggle: React.FunctionComponent<WizardToggleProps>;
