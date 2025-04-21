import * as React from 'react';
import { SearchAttribute } from './SearchInput';
export interface AdvancedSearchMenuProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onChange'> {
    /** Additional classes added to the Advanced search menu */
    className?: string;
    /** Value of the search input */
    value?: string;
    /** Ref of the div wrapping the whole search input **/
    parentRef?: React.RefObject<any>;
    /** Ref of the input element within the search input**/
    parentInputRef?: React.RefObject<any>;
    /** Function which builds an attribute-value map by parsing the value in the search input */
    getAttrValueMap?: () => {
        [key: string]: string;
    };
    /** A callback for when the search button clicked changes */
    onSearch?: (value: string, event: React.SyntheticEvent<HTMLButtonElement>, attrValueMap: {
        [key: string]: string;
    }) => void;
    /** A callback for when the user clicks the clear button */
    onClear?: (event: React.SyntheticEvent<HTMLButtonElement>) => void;
    /** A callback for when the input value changes */
    onChange?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
    /** Function called to toggle the advanced search menu */
    onToggleAdvancedMenu?: (e: React.SyntheticEvent<HTMLButtonElement>) => void;
    /** Flag for toggling the open/close state of the advanced search menu */
    isSearchMenuOpen?: boolean;
    /** Label for the buttons which reset the advanced search form and clear the search input */
    resetButtonLabel?: string;
    /** Label for the buttons which called the onSearch event handler */
    submitSearchButtonLabel?: string;
    /** Array of attribute values used for dynamically generated advanced search */
    attributes?: string[] | SearchAttribute[];
    formAdditionalItems?: React.ReactNode;
    /** Attribute label for strings unassociated with one of the provided listed attributes */
    hasWordsAttrLabel?: React.ReactNode;
    /** Delimiter in the query string for pairing attributes with search values.
     * Required whenever attributes are passed as props */
    advancedSearchDelimiter?: string;
}
export declare const AdvancedSearchMenu: React.FunctionComponent<AdvancedSearchMenuProps>;
//# sourceMappingURL=AdvancedSearchMenu.d.ts.map