import * as React from 'react';
export interface ModalBoxCloseButtonProps {
    /** Additional classes added to the close button */
    className?: string;
    /** A callback for when the close button is clicked */
    onClose?: () => void;
}
export declare const ModalBoxCloseButton: React.FunctionComponent<ModalBoxCloseButtonProps>;
