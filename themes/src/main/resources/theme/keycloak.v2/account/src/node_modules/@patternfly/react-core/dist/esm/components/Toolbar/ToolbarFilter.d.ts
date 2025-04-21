import * as React from 'react';
import { ToolbarItemProps } from './ToolbarItem';
import { ToolbarContext } from './ToolbarUtils';
import { PickOptional } from '../../helpers/typeUtils';
export interface ToolbarChipGroup {
    /** A unique key to identify this chip group category */
    key: string;
    /** The category name to display for the chip group */
    name: string;
}
export interface ToolbarChip {
    /** A unique key to identify this chip */
    key: string;
    /** The ReactNode to display in the chip */
    node: React.ReactNode;
}
export interface ToolbarFilterProps extends ToolbarItemProps {
    /** An array of strings to be displayed as chips in the expandable content */
    chips?: (string | ToolbarChip)[];
    /** Callback passed by consumer used to close the entire chip group */
    deleteChipGroup?: (category: string | ToolbarChipGroup) => void;
    /** Callback passed by consumer used to delete a chip from the chips[] */
    deleteChip?: (category: string | ToolbarChipGroup, chip: ToolbarChip | string) => void;
    /** Customizable "Show Less" text string for the chip group */
    chipGroupExpandedText?: string;
    /** Customizeable template string for the chip group. Use variable "${remaining}" for the overflow chip count. */
    chipGroupCollapsedText?: string;
    /** Content to be rendered inside the data toolbar item associated with the chip group */
    children: React.ReactNode;
    /** Unique category name to be used as a label for the chip group */
    categoryName: string | ToolbarChipGroup;
    /** Flag to show the toolbar item */
    showToolbarItem?: boolean;
}
interface ToolbarFilterState {
    isMounted: boolean;
}
export declare class ToolbarFilter extends React.Component<ToolbarFilterProps, ToolbarFilterState> {
    static displayName: string;
    static contextType: React.Context<import("./ToolbarUtils").ToolbarContextProps>;
    context: React.ContextType<typeof ToolbarContext>;
    static defaultProps: PickOptional<ToolbarFilterProps>;
    constructor(props: ToolbarFilterProps);
    componentDidMount(): void;
    componentDidUpdate(): void;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=ToolbarFilter.d.ts.map