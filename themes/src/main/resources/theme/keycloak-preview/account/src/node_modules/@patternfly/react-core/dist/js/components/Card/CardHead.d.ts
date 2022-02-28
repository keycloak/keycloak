import * as React from 'react';
export interface CardHeadProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Card Head */
    children?: React.ReactNode;
    /** Additional classes added to the Head */
    className?: string;
}
export declare const CardHead: React.FunctionComponent<CardHeadProps>;
