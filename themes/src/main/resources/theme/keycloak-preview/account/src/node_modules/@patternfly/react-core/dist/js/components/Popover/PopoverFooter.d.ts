import * as React from 'react';
export declare const PopoverFooter: React.FunctionComponent<PopoverFooterProps>;
export interface PopoverFooterProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the Popover Footer */
    className?: string;
    /** Footer node */
    children: React.ReactNode;
}
