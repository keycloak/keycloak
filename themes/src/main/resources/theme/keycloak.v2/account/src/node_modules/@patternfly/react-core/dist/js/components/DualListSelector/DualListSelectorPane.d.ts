import * as React from 'react';
import { DualListSelectorTreeItemData } from './DualListSelectorTree';
export interface DualListSelectorPaneProps {
    /** Additional classes applied to the dual list selector pane. */
    className?: string;
    /** A dual list selector list or dual list selector tree to be rendered in the pane. */
    children?: React.ReactNode;
    /** Flag indicating if this pane is the chosen pane. */
    isChosen?: boolean;
    /** Status to display above the pane. */
    status?: string;
    /** Title of the pane. */
    title?: React.ReactNode;
    /** A search input placed above the list at the top of the pane, before actions. */
    searchInput?: React.ReactNode;
    /** Actions to place above the pane. */
    actions?: React.ReactNode[];
    /** Id of the pane. */
    id?: string;
    /** @hide Options to list in the pane. */
    options?: React.ReactNode[];
    /** @hide Options currently selected in the pane. */
    selectedOptions?: string[] | number[];
    /** @hide Callback for when an option is selected. Optionally used only when options prop is provided. */
    onOptionSelect?: (e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent, index: number, isChosen: boolean, id?: string, itemData?: any, parentData?: any) => void;
    /** @hide Callback for when a tree option is checked. Optionally used only when options prop is provided. */
    onOptionCheck?: (evt: React.MouseEvent | React.ChangeEvent<HTMLInputElement> | React.KeyboardEvent, isChecked: boolean, itemData: DualListSelectorTreeItemData) => void;
    /** @hide Flag indicating a dynamically built search bar should be included above the pane. */
    isSearchable?: boolean;
    /** Flag indicating whether the component is disabled. */
    isDisabled?: boolean;
    /** Callback for search input. To be used when isSearchable is true. */
    onSearch?: (event: React.ChangeEvent<HTMLInputElement>) => void;
    /** @hide A callback for when the search input value for changes.  To be used when isSearchable is true. */
    onSearchInputChanged?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
    /** @hide Filter function for custom filtering based on search string. To be used when isSearchable is true. */
    filterOption?: (option: React.ReactNode, input: string) => boolean;
    /** @hide Accessible label for the search input. To be used when isSearchable is true. */
    searchInputAriaLabel?: string;
    /** @hide Callback for updating the filtered options in DualListSelector. To be used when isSearchable is true. */
    onFilterUpdate?: (newFilteredOptions: React.ReactNode[], paneType: string, isSearchReset: boolean) => void;
}
export declare const DualListSelectorPane: React.FunctionComponent<DualListSelectorPaneProps>;
//# sourceMappingURL=DualListSelectorPane.d.ts.map