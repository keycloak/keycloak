import * as React from 'react';
export interface SearchAttribute {
    /** The search attribute's value to be provided in the search input's query string.
     * It should have no spaces and be unique for every attribute */
    attr: string;
    /** The search attribute's display name. It is used to label the field in the advanced search menu */
    display: React.ReactNode;
}
export interface SearchInputProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onChange' | 'results' | 'ref'> {
    /** Additional classes added to the banner */
    className?: string;
    /** Value of the search input */
    value?: string;
    /** Flag indicating if search input is disabled */
    isDisabled?: boolean;
    /** An accessible label for the search input */
    'aria-label'?: string;
    /** placeholder text of the search input */
    placeholder?: string;
    /** @hide A reference object to attach to the input box */
    innerRef?: React.RefObject<any>;
    /** A callback for when the input value changes */
    onChange?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
    /** A suggestion for autocompleting */
    hint?: string;
    /** A callback for when the search button clicked changes */
    onSearch?: (value: string, event: React.SyntheticEvent<HTMLButtonElement>, attrValueMap: {
        [key: string]: string;
    }) => void;
    /** A callback for when the user clicks the clear button */
    onClear?: (event: React.SyntheticEvent<HTMLButtonElement>) => void;
    /** Label for the buttons which reset the advanced search form and clear the search input */
    resetButtonLabel?: string;
    /** Label for the buttons which called the onSearch event handler */
    submitSearchButtonLabel?: string;
    /** A callback for when the open advanced search button is clicked */
    onToggleAdvancedSearch?: (event: React.SyntheticEvent<HTMLButtonElement>, isOpen?: boolean) => void;
    /** A flag for controlling the open state of a custom advanced search implementation */
    isAdvancedSearchOpen?: boolean;
    /** Label for the button which opens the advanced search form menu */
    openMenuButtonAriaLabel?: string;
    /** Label for the button to navigate to previous result  */
    previousNavigationButtonAriaLabel?: string;
    /** Flag indicating if the previous navigation button is disabled */
    isPreviousNavigationButtonDisabled?: boolean;
    /** Label for the button to navigate to next result */
    nextNavigationButtonAriaLabel?: string;
    /** Flag indicating if the next navigation button is disabled */
    isNextNavigationButtonDisabled?: boolean;
    /** Function called when user clicks to navigate to next result */
    onNextClick?: (event: React.SyntheticEvent<HTMLButtonElement>) => void;
    /** Function called when user clicks to navigate to previous result */
    onPreviousClick?: (event: React.SyntheticEvent<HTMLButtonElement>) => void;
    /** The number of search results returned. Either a total number of results,
     * or a string representing the current result over the total number of results. i.e. "1 / 5" */
    resultsCount?: number | string;
    /** Array of attribute values used for dynamically generated advanced search */
    attributes?: string[] | SearchAttribute[];
    formAdditionalItems?: React.ReactNode;
    /** Attribute label for strings unassociated with one of the provided listed attributes */
    hasWordsAttrLabel?: React.ReactNode;
    /** Delimiter in the query string for pairing attributes with search values.
     * Required whenever attributes are passed as props */
    advancedSearchDelimiter?: string;
}
export declare const SearchInput: React.ForwardRefExoticComponent<SearchInputProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=SearchInput.d.ts.map