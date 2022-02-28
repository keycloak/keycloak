import * as React from 'react';
export interface ModalBoxHeaderProps {
    /** Content rendered inside the Header */
    children?: React.ReactNode;
    /** Additional classes added to the button */
    className?: string;
    /** Flag to hide the title */
    hideTitle?: boolean;
    /** The heading level to use */
    headingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}
export declare const ModalBoxHeader: React.FunctionComponent<ModalBoxHeaderProps>;
