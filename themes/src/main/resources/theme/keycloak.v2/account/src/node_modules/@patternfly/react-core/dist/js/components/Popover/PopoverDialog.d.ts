import * as React from 'react';
export declare const PopoverPosition: {
    top: string;
    bottom: string;
    left: string;
    right: string;
};
export declare const PopoverDialog: React.FunctionComponent<PopoverDialogProps>;
export interface PopoverDialogProps extends React.HTMLProps<HTMLDivElement> {
    /** PopoverDialog position */
    position?: 'top' | 'bottom' | 'left' | 'right';
    /** PopoverDialog additional class */
    className?: string;
    /** PopoverDialog body */
    children: React.ReactNode;
}
//# sourceMappingURL=PopoverDialog.d.ts.map