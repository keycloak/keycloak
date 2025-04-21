import * as React from 'react';
export interface InputGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the input group. */
    className?: string;
    /** Content rendered inside the input group. */
    children: React.ReactNode;
    /** @hide A reference object to attach to the input box */
    innerRef?: React.RefObject<any>;
}
export declare const InputGroup: React.FunctionComponent<InputGroupProps>;
//# sourceMappingURL=InputGroup.d.ts.map