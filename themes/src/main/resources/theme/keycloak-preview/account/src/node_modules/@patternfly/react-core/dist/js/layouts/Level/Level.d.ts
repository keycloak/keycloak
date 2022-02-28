import * as React from 'react';
export interface LevelProps extends React.HTMLProps<HTMLDivElement> {
    /** Adds space between children. */
    gutter?: 'sm' | 'md' | 'lg';
    /** additional classes added to the Level layout */
    className?: string;
    /** content rendered inside the Level layout */
    children?: React.ReactNode;
}
export declare const Level: React.FunctionComponent<LevelProps>;
