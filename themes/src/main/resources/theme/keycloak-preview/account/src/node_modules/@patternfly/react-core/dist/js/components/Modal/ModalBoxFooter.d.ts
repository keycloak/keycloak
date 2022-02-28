import * as React from 'react';
export interface ModalBoxFooterProps {
    /** Content rendered inside the Footer */
    children?: React.ReactNode;
    /** Additional classes added to the Footer */
    className?: string;
    /** Flag to align buttons to the left */
    isLeftAligned?: boolean;
}
export declare const ModalBoxFooter: React.FunctionComponent<ModalBoxFooterProps>;
