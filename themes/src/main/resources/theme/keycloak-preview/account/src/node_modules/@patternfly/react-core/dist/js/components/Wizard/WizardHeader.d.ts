import * as React from 'react';
export interface WizardHeaderProps {
    /** Callback function called when the X (Close) button is clicked */
    onClose?: () => void;
    /** Title of the wizard */
    title: string;
    /** Description of the wizard */
    description?: string;
    /** aria-label applied to the X (Close) button */
    ariaLabelCloseButton?: string;
    /** id for the title */
    titleId?: string;
    /** id for the description */
    descriptionId?: string;
}
export declare const WizardHeader: React.FunctionComponent<WizardHeaderProps>;
