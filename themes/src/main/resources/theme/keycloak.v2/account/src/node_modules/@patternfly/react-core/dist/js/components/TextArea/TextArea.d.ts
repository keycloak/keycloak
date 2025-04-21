import * as React from 'react';
import { HTMLProps } from 'react';
export declare enum TextAreResizeOrientation {
    horizontal = "horizontal",
    vertical = "vertical",
    both = "both"
}
export interface TextAreaProps extends Omit<HTMLProps<HTMLTextAreaElement>, 'onChange' | 'ref'> {
    /** Additional classes added to the TextArea. */
    className?: string;
    /** Flag to show if the TextArea is required. */
    isRequired?: boolean;
    /** Flag to show if the TextArea is disabled. */
    isDisabled?: boolean;
    /** Flag to show if the TextArea is read only. */
    isReadOnly?: boolean;
    /** Use the external file instead of a data URI */
    isIconSprite?: boolean;
    /** Flag to modify height based on contents. */
    autoResize?: boolean;
    /** Value to indicate if the textarea is modified to show that validation state.
     * If set to success, textarea will be modified to indicate valid state.
     * If set to error, textarea will be modified to indicate error state.
     */
    validated?: 'success' | 'warning' | 'error' | 'default';
    /** Value of the TextArea. */
    value?: string | number;
    /** A callback for when the TextArea value changes. */
    onChange?: (value: string, event: React.ChangeEvent<HTMLTextAreaElement>) => void;
    /** Sets the orientation to limit the resize to */
    resizeOrientation?: 'horizontal' | 'vertical' | 'both';
    /** Custom flag to show that the TextArea requires an associated id or aria-label. */
    'aria-label'?: string;
    /** A reference object to attach to the textarea. */
    innerRef?: React.RefObject<any>;
}
export declare class TextAreaBase extends React.Component<TextAreaProps> {
    static displayName: string;
    static defaultProps: TextAreaProps;
    constructor(props: TextAreaProps);
    private handleChange;
    render(): JSX.Element;
}
export declare const TextArea: React.ForwardRefExoticComponent<TextAreaProps & React.RefAttributes<HTMLTextAreaElement>>;
//# sourceMappingURL=TextArea.d.ts.map