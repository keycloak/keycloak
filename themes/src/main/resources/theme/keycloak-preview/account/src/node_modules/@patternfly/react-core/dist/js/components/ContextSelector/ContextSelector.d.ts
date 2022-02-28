import * as React from 'react';
export interface ContextSelectorProps {
    /** content rendered inside the Context Selector */
    children?: React.ReactNode;
    /** Classes applied to root element of Context Selector */
    className?: string;
    /** Flag to indicate if Context Selector is opened */
    isOpen?: boolean;
    /** Function callback called when user clicks toggle button */
    onToggle?: (event: any, value: boolean) => void;
    /** Function callback called when user selects item */
    onSelect?: (event: any, value: React.ReactNode) => void;
    /** Labels the Context Selector for Screen Readers */
    screenReaderLabel?: string;
    /** Text that appears in the Context Selector Toggle */
    toggleText?: string;
    /** aria-label for the Context Selector Search Button */
    searchButtonAriaLabel?: string;
    /** Value in the Search field */
    searchInputValue?: string;
    /** Function callback called when user changes the Search Input */
    onSearchInputChange?: (value: string) => void;
    /** Search Input placeholder */
    searchInputPlaceholder?: string;
    /** Function callback for when Search Button is clicked */
    onSearchButtonClick?: (event?: React.SyntheticEvent<HTMLButtonElement>) => void;
}
export declare class ContextSelector extends React.Component<ContextSelectorProps> {
    static defaultProps: ContextSelectorProps;
    parentRef: React.RefObject<HTMLDivElement>;
    onEnterPressed: (event: any) => void;
    render(): JSX.Element;
}
