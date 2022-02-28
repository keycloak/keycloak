import * as React from 'react';
export interface SelectOptionObject {
    /** Function returns a string to represent the select option object */
    toString(): string;
    /** Function returns a true if the passed in select option is equal to this select option object, false otherwise */
    compareTo?(selectOption: any): boolean;
}
export interface SelectOptionProps extends Omit<React.HTMLProps<HTMLElement>, 'type' | 'ref' | 'value'> {
    /** Optional alternate display for the option */
    children?: React.ReactNode;
    /** Additional classes added to the Select Option */
    className?: string;
    /** Internal index of the option */
    index?: number;
    /** Indicates which component will be used as select item */
    component?: React.ReactNode;
    /** The value for the option, can be a string or select option object */
    value: string | SelectOptionObject;
    /** Flag indicating if the option is disabled */
    isDisabled?: boolean;
    /** Flag indicating if the option acts as a placeholder */
    isPlaceholder?: boolean;
    /** Flad indicating if the option acts as a "no results" indicator */
    isNoResultsOption?: boolean;
    /** Internal flag indicating if the option is selected */
    isSelected?: boolean;
    /** Internal flag indicating if the option is checked */
    isChecked?: boolean;
    /** Internal flag indicating if the option is focused */
    isFocused?: boolean;
    /** Internal callback for ref tracking */
    sendRef?: (ref: React.ReactNode, index: number) => void;
    /** Internal callback for keyboard navigation */
    keyHandler?: (index: number, position: string) => void;
    /** Optional callback for click event */
    onClick?: (event: React.MouseEvent | React.ChangeEvent) => void;
}
export declare class SelectOption extends React.Component<SelectOptionProps> {
    private ref;
    static defaultProps: SelectOptionProps;
    componentDidMount(): void;
    componentDidUpdate(): void;
    onKeyDown: (event: React.KeyboardEvent<Element>) => void;
    render(): JSX.Element;
}
