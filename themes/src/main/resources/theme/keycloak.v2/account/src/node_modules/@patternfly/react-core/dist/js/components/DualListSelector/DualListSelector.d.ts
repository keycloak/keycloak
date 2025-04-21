import * as React from 'react';
import { PickOptional } from '../../helpers';
import { DualListSelectorTreeItemData } from './DualListSelectorTree';
export interface DualListSelectorProps {
    /** Additional classes applied to the dual list selector. */
    className?: string;
    /** Id of the dual list selector. */
    id?: string;
    /** Flag indicating if the dual list selector uses trees instead of simple lists */
    isTree?: boolean;
    /** Flag indicating if the dual list selector is in a disabled state */
    isDisabled?: boolean;
    /** Content to be rendered in the dual list selector. Panes & controls will not be built dynamically when children are provided. */
    children?: React.ReactNode;
    /** Title applied to the dynamically built available options pane. */
    availableOptionsTitle?: string;
    /** Options to display in the dynamically built available options pane. When using trees, the options should be in the DualListSelectorTreeItemData[] format. */
    availableOptions?: React.ReactNode[] | DualListSelectorTreeItemData[];
    /** Status message to display above the dynamically built available options pane. */
    availableOptionsStatus?: string;
    /** Actions to be displayed above the dynamically built available options pane. */
    availableOptionsActions?: React.ReactNode[];
    /** Title applied to the dynamically built chosen options pane. */
    chosenOptionsTitle?: string;
    /** Options to display in the dynamically built chosen options pane. When using trees, the options should be in the DualListSelectorTreeItemData[] format. */
    chosenOptions?: React.ReactNode[] | DualListSelectorTreeItemData[];
    /** Status message to display above the dynamically built chosen options pane.*/
    chosenOptionsStatus?: string;
    /** Actions to be displayed above the dynamically built chosen options pane. */
    chosenOptionsActions?: React.ReactNode[];
    /** Accessible label for the dynamically built controls between the two panes. */
    controlsAriaLabel?: string;
    /** Optional callback for the dynamically built add selected button */
    addSelected?: (newAvailableOptions: React.ReactNode[], newChosenOptions: React.ReactNode[]) => void;
    /** Accessible label for the dynamically built add selected button */
    addSelectedAriaLabel?: string;
    /** Tooltip content for the dynamically built add selected button */
    addSelectedTooltip?: React.ReactNode;
    /** Additonal tooltip properties for the dynamically built add selected tooltip */
    addSelectedTooltipProps?: any;
    /** Callback fired every time dynamically built options are chosen or removed */
    onListChange?: (newAvailableOptions: React.ReactNode[], newChosenOptions: React.ReactNode[]) => void;
    /** Optional callback for the dynamically built add all button */
    addAll?: (newAvailableOptions: React.ReactNode[], newChosenOptions: React.ReactNode[]) => void;
    /** Accessible label for the dynamically built add all button */
    addAllAriaLabel?: string;
    /** Tooltip content for the dynamically built add all button */
    addAllTooltip?: React.ReactNode;
    /** Additonal tooltip properties for the dynamically built add all tooltip */
    addAllTooltipProps?: any;
    /** Optional callback for the dynamically built remove selected button */
    removeSelected?: (newAvailableOptions: React.ReactNode[], newChosenOptions: React.ReactNode[]) => void;
    /** Accessible label for the dynamically built remove selected button */
    removeSelectedAriaLabel?: string;
    /** Tooltip content for the dynamically built remove selected button */
    removeSelectedTooltip?: React.ReactNode;
    /** Additonal tooltip properties for the dynamically built remove selected tooltip  */
    removeSelectedTooltipProps?: any;
    /** Optional callback for the dynamically built remove all button */
    removeAll?: (newAvailableOptions: React.ReactNode[], newChosenOptions: React.ReactNode[]) => void;
    /** Accessible label for the dynamically built remove all button */
    removeAllAriaLabel?: string;
    /** Tooltip content for the dynamically built remove all button */
    removeAllTooltip?: React.ReactNode;
    /** Additonal tooltip properties for the dynamically built remove all tooltip */
    removeAllTooltipProps?: any;
    /** Optional callback fired when a dynamically built option is selected */
    onOptionSelect?: (e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent, index: number, isChosen: boolean, id: string, itemData: any, parentData: any) => void;
    /** Optional callback fired when a dynamically built option is checked */
    onOptionCheck?: (e: React.MouseEvent | React.ChangeEvent<HTMLInputElement> | React.KeyboardEvent, checked: boolean, checkedId: string, newCheckedItems: string[]) => void;
    /** Flag indicating a search bar should be included above both the dynamically built available and chosen panes. */
    isSearchable?: boolean;
    /** Accessible label for the search input on the dynamically built available options pane. */
    availableOptionsSearchAriaLabel?: string;
    /** A callback for when the search input value for the dynamically built available options changes. */
    onAvailableOptionsSearchInputChanged?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
    /** Accessible label for the search input on the dynamically built chosen options pane. */
    chosenOptionsSearchAriaLabel?: string;
    /** A callback for when the search input value for the dynamically built chosen options changes. */
    onChosenOptionsSearchInputChanged?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
    /** Optional filter function for custom filtering based on search string. Used with a dynamically built search input. */
    filterOption?: (option: React.ReactNode, input: string) => boolean;
}
interface DualListSelectorState {
    availableOptions: React.ReactNode[];
    availableOptionsSelected: number[];
    availableFilteredOptions: React.ReactNode[];
    chosenOptions: React.ReactNode[];
    chosenOptionsSelected: number[];
    chosenFilteredOptions: React.ReactNode[];
    availableTreeFilteredOptions: string[];
    availableTreeOptionsChecked: string[];
    chosenTreeOptionsChecked: string[];
    chosenTreeFilteredOptions: string[];
}
export declare class DualListSelector extends React.Component<DualListSelectorProps, DualListSelectorState> {
    static displayName: string;
    private addAllButtonRef;
    private addSelectedButtonRef;
    private removeSelectedButtonRef;
    private removeAllButtonRef;
    static defaultProps: PickOptional<DualListSelectorProps>;
    private createMergedCopy;
    constructor(props: DualListSelectorProps);
    /** In dev environment, prevents circular structure during JSON stringification when
     * options passed in to the dual list selector include HTML elements.
     */
    replacer: (key: string, value: any) => any;
    componentDidUpdate(): void;
    onFilterUpdate: (newFilteredOptions: React.ReactNode[], paneType: string, isSearchReset: boolean) => void;
    addAllVisible: () => void;
    addAllTreeVisible: () => void;
    addSelected: () => void;
    addTreeSelected: () => void;
    removeAllVisible: () => void;
    removeAllTreeVisible: () => void;
    removeSelected: () => void;
    removeTreeSelected: () => void;
    onOptionSelect: (e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent, index: number, isChosen: boolean, id?: string, itemData?: any, parentData?: any) => void;
    isChecked: (treeItem: DualListSelectorTreeItemData, isChosen: boolean) => boolean;
    areAllDescendantsChecked: (treeItem: DualListSelectorTreeItemData, isChosen: boolean) => boolean;
    areSomeDescendantsChecked: (treeItem: DualListSelectorTreeItemData, isChosen: boolean) => boolean;
    mapChecked: (item: DualListSelectorTreeItemData, isChosen: boolean) => DualListSelectorTreeItemData;
    onTreeOptionCheck: (evt: React.MouseEvent | React.ChangeEvent<HTMLInputElement> | React.KeyboardEvent, isChecked: boolean, itemData: DualListSelectorTreeItemData, isChosen: boolean) => void;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=DualListSelector.d.ts.map