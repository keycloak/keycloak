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
    /** Description of the item for single and both typeahead select variants */
    description?: React.ReactNode;
    /** Number of items matching the option */
    itemCount?: number;
    /** @hide Internal index of the option */
    index?: number;
    /** Indicates which component will be used as select item */
    component?: React.ReactNode;
    /** The value for the option, can be a string or select option object */
    value: string | SelectOptionObject;
    /** Flag indicating if the option is disabled */
    isDisabled?: boolean;
    /** Flag indicating if the option acts as a placeholder */
    isPlaceholder?: boolean;
    /** Flag indicating if the option acts as a "no results" indicator */
    isNoResultsOption?: boolean;
    /** @hide Internal flag indicating if the option is selected */
    isSelected?: boolean;
    /** @hide Internal flag indicating if the option is checked */
    isChecked?: boolean;
    /** Flag forcing the focused state */
    isFocused?: boolean;
    /** @hide Internal callback for ref tracking */
    sendRef?: (ref: React.ReactNode, favoriteRef: React.ReactNode, optionContainerRef: React.ReactNode, index: number) => void;
    /** @hide Internal callback for keyboard navigation */
    keyHandler?: (index: number, innerIndex: number, position: string) => void;
    /** Optional callback for click event */
    onClick?: (event: React.MouseEvent | React.ChangeEvent) => void;
    /** Id of the checkbox input */
    inputId?: string;
    /** @hide Internal Flag indicating if the option is favorited */
    isFavorite?: boolean;
    /** Aria label text for favoritable button when favorited */
    ariaIsFavoriteLabel?: string;
    /** Aria label text for favoritable button when not favorited */
    ariaIsNotFavoriteLabel?: string;
    /** ID of the item. Required for tracking favorites */
    id?: string;
    /** @hide Internal flag to apply the load styling to view more option */
    isLoad?: boolean;
    /** @hide Internal flag to apply the loading styling to spinner */
    isLoading?: boolean;
    /** @hide Internal callback for the setting the index of the next item to focus after view more is clicked */
    setViewMoreNextIndex?: () => void;
    /** @hide Flag indicating this is the last option when there is a footer */
    isLastOptionBeforeFooter?: (index: number) => boolean;
    /** @hide Flag indicating that the the option loading variant is in a grouped select */
    isGrouped?: boolean;
}
export declare class SelectOption extends React.Component<SelectOptionProps> {
    static displayName: string;
    private ref;
    private liRef;
    private favoriteRef;
    static defaultProps: SelectOptionProps;
    componentDidMount(): void;
    componentDidUpdate(): void;
    onKeyDown: (event: React.KeyboardEvent, innerIndex: number, onEnter?: any, isCheckbox?: boolean) => void;
    render(): JSX.Element;
}
//# sourceMappingURL=SelectOption.d.ts.map