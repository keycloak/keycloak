import * as React from 'react';
export interface WizardHeaderProps {
    /** Callback function called when the X (Close) button is clicked */
    onClose?: () => void;
    /** Title of the wizard */
    title: string;
    /** Description of the wizard */
    description?: React.ReactNode;
    /** Component type of the description */
    descriptionComponent?: 'div' | 'p';
    /** Flag indicating whether the close button should be in the header */
    hideClose?: boolean;
    /** Aria-label applied to the X (Close) button */
    closeButtonAriaLabel?: string;
    /** id for the title */
    titleId?: string;
    /** id for the description */
    descriptionId?: string;
}
export declare const WizardHeader: React.FunctionComponent<WizardHeaderProps>;
//# sourceMappingURL=WizardHeader.d.ts.map