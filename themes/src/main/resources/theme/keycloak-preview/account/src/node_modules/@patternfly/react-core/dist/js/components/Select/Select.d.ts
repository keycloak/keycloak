import * as React from 'react';
import { SelectOptionObject } from './SelectOption';
import { InjectedOuiaProps } from '../withOuia';
export interface SelectProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onSelect' | 'ref' | 'checked' | 'selected'> {
    /** Content rendered inside the Select */
    children?: React.ReactElement[];
    /** Classes applied to the root of the Select */
    className?: string;
    /** Flag specifying which direction the Select menu expands */
    direction?: 'up' | 'down';
    /** Flag to indicate if select is expanded */
    isExpanded?: boolean;
    /** Flag to indicate if select options are grouped */
    isGrouped?: boolean;
    /** Display the toggle with no border or background */
    isPlain?: boolean;
    /** Flag to indicate if select is disabled */
    isDisabled?: boolean;
    /** Flag to indicate if the typeahead select allows new items */
    isCreatable?: boolean;
    /** Text displayed in typeahead select to prompt the user to create an item */
    createText?: string;
    /** Title text of Select */
    placeholderText?: string | React.ReactNode;
    /** Text to display in typeahead select when no results are found */
    noResultsFoundText?: string;
    /** Selected item for single select variant.  Array of selected items for multi select variants. */
    selections?: string | SelectOptionObject | (string | SelectOptionObject)[];
    /** Flag indicating if selection badge should be hidden for checkbox variant,default false */
    isCheckboxSelectionBadgeHidden?: boolean;
    /** Id for select toggle element */
    toggleId?: string;
    /** Adds accessible text to Select */
    'aria-label'?: string;
    /** Id of label for the Select aria-labelledby */
    ariaLabelledBy?: string;
    /** Label for input field of type ahead select variants */
    ariaLabelTypeAhead?: string;
    /** Label for clear selection button of type ahead select variants */
    ariaLabelClear?: string;
    /** Label for toggle of type ahead select variants */
    ariaLabelToggle?: string;
    /** Label for remove chip button of multiple type ahead select variant */
    ariaLabelRemove?: string;
    /** Callback for selection behavior */
    onSelect?: (event: React.MouseEvent | React.ChangeEvent, value: string | SelectOptionObject, isPlaceholder?: boolean) => void;
    /** Callback for toggle button behavior */
    onToggle: (isExpanded: boolean) => void;
    /** Callback for typeahead clear button */
    onClear?: (event: React.MouseEvent) => void;
    /** Optional callback for custom filtering */
    onFilter?: (e: React.ChangeEvent<HTMLInputElement>) => React.ReactElement[];
    /** Optional callback for newly created options */
    onCreateOption?: (newOptionValue: string) => void;
    /** Variant of rendered Select */
    variant?: 'single' | 'checkbox' | 'typeahead' | 'typeaheadmulti';
    /** Width of the select container as a number of px or string percentage */
    width?: string | number;
    /** Max height of the select container as a number of px or string percentage */
    maxHeight?: string | number;
    /** Icon element to render inside the select toggle */
    toggleIcon?: React.ReactElement;
    /** Custom content to render in the select menu.  If this prop is defined, the variant prop will be ignored and the select will render with a single select toggle */
    customContent?: React.ReactNode;
    /** Flag indicating if select should have an inline text input for filtering */
    hasInlineFilter?: boolean;
}
export interface SelectState {
    openedOnEnter: boolean;
    typeaheadInputValue: string | null;
    typeaheadActiveChild?: HTMLElement;
    typeaheadFilteredChildren: React.ReactNode[];
    typeaheadCurrIndex: number;
    creatableValue: string;
}
declare const SelectWithOuiaContext: React.FunctionComponent<SelectProps & InjectedOuiaProps>;
export { SelectWithOuiaContext as Select };
