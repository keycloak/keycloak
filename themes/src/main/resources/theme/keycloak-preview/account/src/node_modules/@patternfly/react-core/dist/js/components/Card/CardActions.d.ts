import * as React from 'react';
export interface CardActionsProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Card Action */
    children?: React.ReactNode;
    /** Additional classes added to the Action */
    className?: string;
}
export declare const CardActions: React.FunctionComponent<CardActionsProps>;
