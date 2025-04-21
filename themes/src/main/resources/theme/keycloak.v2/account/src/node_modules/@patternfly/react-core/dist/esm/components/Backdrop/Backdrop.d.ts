import * as React from 'react';
export interface BackdropProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the backdrop */
    children?: React.ReactNode;
    /** additional classes added to the button */
    className?: string;
}
export declare const Backdrop: React.FunctionComponent<BackdropProps>;
//# sourceMappingURL=Backdrop.d.ts.map