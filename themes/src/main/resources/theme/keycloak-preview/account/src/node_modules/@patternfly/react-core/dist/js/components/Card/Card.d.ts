import * as React from 'react';
export interface CardProps extends React.HTMLProps<HTMLElement> {
    /** Content rendered inside the Card */
    children?: React.ReactNode;
    /** Additional classes added to the Card */
    className?: string;
    /** Sets the base component to render. defaults to article */
    component?: keyof JSX.IntrinsicElements;
    /** Modifies the card to include hover styles on :hover */
    isHoverable?: boolean;
    /** Modifies the card to include compact styling */
    isCompact?: boolean;
    /** Modifies the card to include selectable styling */
    isSelectable?: boolean;
    /** Modifies the card to include selected styling */
    isSelected?: boolean;
}
export declare const Card: React.FunctionComponent<CardProps>;
