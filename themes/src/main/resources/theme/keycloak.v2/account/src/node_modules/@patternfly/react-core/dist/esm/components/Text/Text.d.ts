import * as React from 'react';
import { OUIAProps } from '../../helpers';
export declare enum TextVariants {
    h1 = "h1",
    h2 = "h2",
    h3 = "h3",
    h4 = "h4",
    h5 = "h5",
    h6 = "h6",
    p = "p",
    a = "a",
    small = "small",
    blockquote = "blockquote",
    pre = "pre"
}
export interface TextProps extends React.HTMLProps<HTMLElement>, OUIAProps {
    /** The text component */
    component?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'p' | 'a' | 'small' | 'blockquote' | 'pre';
    /** Content rendered within the Text */
    children?: React.ReactNode;
    /** Additional classes added to the Text */
    className?: string;
    /** Flag to indicate the link has visited styles applied if the browser determines the link has been visited */
    isVisitedLink?: boolean;
}
export declare const Text: React.FunctionComponent<TextProps>;
//# sourceMappingURL=Text.d.ts.map