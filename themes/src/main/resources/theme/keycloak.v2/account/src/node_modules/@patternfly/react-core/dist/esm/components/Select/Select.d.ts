import * as React from 'react';
import { SelectOptionObject } from './SelectOption';
import { SelectPosition } from './selectConstants';
import { ChipGroupProps } from '../ChipGroup';
import { OUIAProps, PickOptional } from '../../helpers';
import { ToggleMenuBaseProps } from '../../helpers/Popper/Popper';
export interface SelectViewMoreObject {
    /** View more text */
    text: string;
    /** Callback for when the view more button is clicked */
    onClick: (event: React.MouseEvent | React.ChangeEvent) => void;
}
export interface SelectProps extends Omit<ToggleMenuBaseProps, 'menuAppendTo'>, Omit<React.HTMLProps<HTMLDivElement>, 'onSelect' | 'ref' | 'checked' | 'selected'>, OUIAProps {
    /** Content rendered inside the Select. Must be React.ReactElement<SelectGroupProps>[] */
    children?: React.ReactElement[];
    /** Classes applied to the root of the Select */
    className?: string;
    /** Indicates where menu will be aligned horizontally */
    position?: SelectPosition | 'right' | 'left';
    /** Flag specifying which direction the Select menu expands */
    direction?: 'up' | 'down';
    /** Flag to indicate if select is open */
    isOpen?: boolean;
    /** Flag to indicate if select options are grouped */
    isGrouped?: boolean;
    /** Display the toggle with no border or background */
    isPlain?: boolean;
    /** Flag to indicate if select is disabled */
    isDisabled?: boolean;
    /** Flag to indicate if the typeahead select allows new items */
    isCreatable?: boolean;
    /** Flag indicating if placeholder styles should be applied */
    hasPlaceholderStyle?: boolean;
    /** @beta Flag indicating if the creatable option should set its value as a SelectOptionObject */
    isCreateSelectOptionObject?: boolean;
    /** Value to indicate if the select is modified to show that validation state.
     * If set to success, select will be modified to indicate valid state.
     * If set to error, select will be modified to indicate error state.
     * If set to warning, select will be modified to indicate warning state.
     */
    validated?: 'success' | 'warning' | 'error' | 'default';
    /** @beta Loading variant to display either the spinner or the view more text button */
    loadingVariant?: 'spinner' | SelectViewMoreObject;
    /** Text displayed in typeahead select to prompt the user to create an item */
    createText?: string;
    /** Title text of Select */
    placeholderText?: string | React.ReactNode;
    /** Text to display in typeahead select when no results are found */
    noResultsFoundText?: string;
    /** Array of selected items for multi select variants. */
    selections?: string | SelectOptionObject | (string | SelectOptionObject)[];
    /** Flag indicating if selection badge should be hidden for checkbox variant,default false */
    isCheckboxSelectionBadgeHidden?: boolean;
    /** Id for select toggle element */
    toggleId?: string;
    /** Adds accessible text to Select */
    'aria-label'?: string;
    /** Id of label for the Select aria-labelledby */
    'aria-labelledby'?: string;
    /** Id of div for the select aria-labelledby */
    'aria-describedby'?: string;
    /** Flag indicating if the select is an invalid state */
    'aria-invalid'?: boolean;
    /** Label for input field of type ahead select variants */
    typeAheadAriaLabel?: string;
    /** Id of div for input field of type ahead select variants */
    typeAheadAriaDescribedby?: string;
    /** Label for clear selection button of type ahead select variants */
    clearSelectionsAriaLabel?: string;
    /** Label for toggle of type ahead select variants */
    toggleAriaLabel?: string;
    /** Label for remove chip button of multiple type ahead select variant */
    removeSelectionAriaLabel?: string;
    /** ID list of favorited select items */
    favorites?: string[];
    /** Label for the favorites group */
    favoritesLabel?: string;
    /** Enables favorites. Callback called when a select options's favorite button is clicked */
    onFavorite?: (itemId: string, isFavorite: boolean) => void;
    /** Callback for selection behavior */
    onSelect?: (event: React.MouseEvent | React.ChangeEvent, value: string | SelectOptionObject, isPlaceholder?: boolean) => void;
    /** Callback for toggle button behavior */
    onToggle: (isExpanded: boolean, event: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent | Event) => void;
    /** Callback for toggle blur */
    onBlur?: (event?: any) => void;
    /** Callback for typeahead clear button */
    onClear?: (event: React.MouseEvent) => void;
    /** Optional callback for custom filtering */
    onFilter?: (e: React.ChangeEvent<HTMLInputElement> | null, value: string) => React.ReactElement[] | undefined;
    /** Optional callback for newly created options */
    onCreateOption?: (newOptionValue: string) => void;
    /** Optional event handler called each time the value in the typeahead input changes. */
    onTypeaheadInputChanged?: (value: string) => void;
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
    /** Placeholder text for inline filter */
    inlineFilterPlaceholderText?: string;
    /** Custom text for select badge */
    customBadgeText?: string | number;
    /** Prefix for the id of the input in the checkbox select variant*/
    inputIdPrefix?: string;
    /** Value for the typeahead and inline filtering input autocomplete attribute. When targeting Chrome this property should be a random string. */
    inputAutoComplete?: string;
    /** Optional props to pass to the chip group in the typeaheadmulti variant */
    chipGroupProps?: Omit<ChipGroupProps, 'children' | 'ref'>;
    /** Optional props to render custom chip group in the typeaheadmulti variant */
    chipGroupComponent?: React.ReactNode;
    /** Flag for retaining keyboard-entered value in typeahead text field when focus leaves input away */
    isInputValuePersisted?: boolean;
    /** @beta Flag for retaining filter results on blur from keyboard-entered typeahead text */
    isInputFilterPersisted?: boolean;
    /** Flag indicating the typeahead input value should reset upon selection */
    shouldResetOnSelect?: boolean;
    /** Content rendered in the footer of the select menu */
    footer?: React.ReactNode;
    /** The container to append the menu to. Defaults to 'inline'.
     * If your menu is being cut off you can append it to an element higher up the DOM tree.
     * Some examples:
     * menuAppendTo="parent"
     * menuAppendTo={() => document.body}
     * menuAppendTo={document.getElementById('target')}
     */
    menuAppendTo?: HTMLElement | (() => HTMLElement) | 'inline' | 'parent';
    /** Flag for indicating that the select menu should automatically flip vertically when
     * it reaches the boundary. This prop can only be used when the select component is not
     * appended inline, e.g. `menuAppendTo="parent"`
     */
    isFlipEnabled?: boolean;
}
export interface SelectState {
    focusFirstOption: boolean;
    typeaheadInputValue: string | null;
    typeaheadFilteredChildren: React.ReactNode[];
    favoritesGroup: React.ReactNode[];
    typeaheadCurrIndex: number;
    creatableValue: string;
    tabbedIntoFavoritesMenu: boolean;
    typeaheadStoredIndex: number;
    ouiaStateId: string;
    viewMoreNextIndex: number;
}
export declare class Select extends React.Component<SelectProps & OUIAProps, SelectState> {
    static displayName: string;
    private parentRef;
    private menuComponentRef;
    private filterRef;
    private clearRef;
    private inputRef;
    private refCollection;
    private optionContainerRefCollection;
    private footerRef;
    static defaultProps: PickOptional<SelectProps>;
    state: SelectState;
    getTypeaheadActiveChild: (typeaheadCurrIndex: number) => HTMLElement;
    componentDidUpdate: (prevProps: SelectProps, prevState: SelectState) => void;
    onEnter: () => void;
    onToggle: (isExpanded: boolean, e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent | Event) => void;
    onClose: () => void;
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    updateTypeAheadFilteredChildren: (typeaheadInputValue: string, e: React.ChangeEvent<HTMLInputElement> | null) => void;
    onClick: (e: React.MouseEvent) => void;
    clearSelection: (_e: React.MouseEvent) => void;
    extendTypeaheadChildren(typeaheadCurrIndex: number, favoritesGroup?: React.ReactNode[]): React.ReactNode[];
    sendRef: (optionRef: React.ReactNode, favoriteRef: React.ReactNode, optionContainerRef: React.ReactNode, index: number) => void;
    handleMenuKeys: (index: number, innerIndex: number, position: string) => void;
    moveFocus: (nextIndex: number, updateCurrentIndex?: boolean) => void;
    switchFocusToFavoriteMenu: () => void;
    moveFocusToLastMenuItem: () => void;
    handleTypeaheadKeys: (position: string, shiftKey?: boolean) => void;
    onClickTypeaheadToggleButton: () => void;
    getDisplay: (value: string | SelectOptionObject, type?: 'node' | 'text') => any;
    findText: (item: React.ReactNode) => string;
    generateSelectedBadge: () => string | number;
    setVieMoreNextIndex: () => void;
    isLastOptionBeforeFooter: (index: any) => boolean;
    render(): JSX.Element;
}
//# sourceMappingURL=Select.d.ts.map