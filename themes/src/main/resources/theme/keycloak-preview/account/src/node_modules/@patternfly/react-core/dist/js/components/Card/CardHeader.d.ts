import * as React from 'react';
export interface CardHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Card Footer */
    children?: React.ReactNode;
    /** Additional classes added to the Header */
    className?: string;
}
export declare const CardHeader: React.FunctionComponent<CardHeaderProps>;
