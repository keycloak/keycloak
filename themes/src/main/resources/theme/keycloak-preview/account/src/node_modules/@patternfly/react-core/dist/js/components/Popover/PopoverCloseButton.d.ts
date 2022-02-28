import * as React from 'react';
export declare const PopoverCloseButton: React.FunctionComponent<PopoverCloseButtonProps>;
export interface PopoverCloseButtonProps {
    /** PopoverCloseButton onClose function */
    onClose?: () => void;
    /** Aria label for the Close button */
    'aria-label': string;
}
