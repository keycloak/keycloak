import * as React from 'react';
export interface ModalBoxProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the ModalBox. */
    children: React.ReactNode;
    /** Additional classes added to the ModalBox */
    className?: string;
    /** Variant of the modal */
    variant?: 'small' | 'medium' | 'large' | 'default';
    /** Alternate position of the modal */
    position?: 'top';
    /** Offset from alternate position. Can be any valid CSS length/percentage */
    positionOffset?: string;
    /** Id to use for Modal Box label */
    'aria-labelledby'?: string;
    /** Accessible descriptor of modal */
    'aria-label'?: string;
    /** Id to use for Modal Box description */
    'aria-describedby': string;
}
export declare const ModalBox: React.FunctionComponent<ModalBoxProps>;
//# sourceMappingURL=ModalBox.d.ts.map