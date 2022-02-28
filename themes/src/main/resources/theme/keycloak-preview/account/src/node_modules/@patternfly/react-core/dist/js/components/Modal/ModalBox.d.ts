import * as React from 'react';
export interface ModalBoxProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the ModalBox. */
    children: React.ReactNode;
    /** Additional classes added to the ModalBox */
    className?: string;
    /** Creates a large version of the ModalBox */
    isLarge?: boolean;
    /** Creates a small version of the ModalBox. */
    isSmall?: boolean;
    /** String to use for Modal Box aria-label */
    title: string;
    /** Id to use for Modal Box description */
    id: string;
}
export declare const ModalBox: React.FunctionComponent<ModalBoxProps>;
