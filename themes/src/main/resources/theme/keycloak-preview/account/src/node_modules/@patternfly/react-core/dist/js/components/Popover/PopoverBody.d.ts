import * as React from 'react';
export declare const PopoverBody: React.FunctionComponent<PopoverBodyProps>;
export interface PopoverBodyProps extends React.HTMLProps<HTMLDivElement> {
    /** PopoverBody id */
    id: string;
    /** PopoverBody content */
    children: React.ReactNode;
}
