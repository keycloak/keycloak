import * as React from 'react';
export interface CardHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the CardHeader */
    children?: React.ReactNode;
    /** Additional classes added to the CardHeader */
    className?: string;
    /** ID of the card header. */
    id?: string;
    /** Callback expandable card */
    onExpand?: (event: React.MouseEvent, id: string) => void;
    /** Additional props for expandable toggle button */
    toggleButtonProps?: any;
    /** Whether to right-align expandable toggle button */
    isToggleRightAligned?: boolean;
}
export declare const CardHeader: React.FunctionComponent<CardHeaderProps>;
//# sourceMappingURL=CardHeader.d.ts.map