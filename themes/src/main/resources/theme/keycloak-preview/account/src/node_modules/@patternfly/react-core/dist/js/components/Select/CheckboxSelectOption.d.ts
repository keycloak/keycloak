import * as React from 'react';
export interface CheckboxSelectOptionProps extends React.HTMLProps<HTMLLabelElement> {
    /** Optional alternate display for the option */
    children?: React.ReactNode;
    /** Additional classes added to the Select Option */
    className?: string;
    /** Internal index of the option */
    index?: number;
    /** The value for the option */
    value: string;
    /** Flag indicating if the option is disabled */
    isDisabled?: boolean;
    /** Internal flag indicating if the option is checked */
    isChecked?: boolean;
    /** Internal callback for ref tracking */
    sendRef?: (ref: React.ReactNode, index: number) => void;
    /** Internal callback for keyboard navigation */
    keyHandler?: (index: number, position: string) => void;
    /** Optional callback for click event */
    onClick?: (event: React.MouseEvent | React.ChangeEvent) => void;
}
export declare class CheckboxSelectOption extends React.Component<CheckboxSelectOptionProps> {
    private ref;
    static defaultProps: CheckboxSelectOptionProps;
    componentDidMount(): void;
    componentDidUpdate(): void;
    onKeyDown: (event: React.KeyboardEvent<Element>) => void;
    render(): JSX.Element;
}
