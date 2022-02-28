import * as React from 'react';
export declare enum DividerVariant {
    hr = "hr",
    li = "li",
    div = "div"
}
export interface DividerProps extends React.HTMLProps<HTMLElement> {
    /** Additional classes added to the divider */
    className?: string;
    /** The component type to use */
    component?: 'hr' | 'li' | 'div';
}
export declare const Divider: React.FunctionComponent<DividerProps>;
