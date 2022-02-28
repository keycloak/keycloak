import * as React from 'react';
import { BaseSizes } from '../../styles/sizes';
export declare enum TitleLevel {
    h1 = "h1",
    h2 = "h2",
    h3 = "h3",
    h4 = "h4",
    h5 = "h5",
    h6 = "h6"
}
export interface TitleProps extends Omit<React.HTMLProps<HTMLHeadingElement>, 'size' | 'className'> {
    /** the size of the Title  */
    size: BaseSizes | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl';
    /** content rendered inside the Title */
    children?: React.ReactNode;
    /** Additional classes added to the Title */
    className?: string;
    /** the heading level to use */
    headingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}
export declare const Title: React.FunctionComponent<TitleProps>;
