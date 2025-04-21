import * as React from 'react';
export declare enum InputGroupTextVariant {
    default = "default",
    plain = "plain"
}
export interface InputGroupTextProps extends React.HTMLProps<HTMLSpanElement | HTMLLabelElement> {
    /** Additional classes added to the input group text. */
    className?: string;
    /** Content rendered inside the input group text. */
    children: React.ReactNode;
    /** Component that wraps the input group text. */
    component?: React.ReactNode;
    /** Input group text variant */
    variant?: InputGroupTextVariant | 'default' | 'plain';
}
export declare const InputGroupText: React.FunctionComponent<InputGroupTextProps>;
//# sourceMappingURL=InputGroupText.d.ts.map