import * as React from 'react';
export interface TextInputGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the text input group */
    children?: React.ReactNode;
    /** Additional classes applied to the text input group container */
    className?: string;
    /** Adds disabled styling and a disabled context value which text input group main hooks into for the input itself */
    isDisabled?: boolean;
    /** @hide A reference object to attach to the input box */
    innerRef?: React.RefObject<any>;
}
export declare const TextInputGroupContext: React.Context<Partial<TextInputGroupProps>>;
export declare const TextInputGroup: React.FunctionComponent<TextInputGroupProps>;
//# sourceMappingURL=TextInputGroup.d.ts.map