import * as React from 'react';
export interface PopoverBodyProps extends React.HTMLProps<HTMLDivElement> {
    /** Popover body id */
    id: string;
    /** Popover body content */
    children: React.ReactNode;
    /** Classes to be applied to the popover body. */
    className?: string;
}
export declare const PopoverBody: React.FunctionComponent<PopoverBodyProps>;
//# sourceMappingURL=PopoverBody.d.ts.map