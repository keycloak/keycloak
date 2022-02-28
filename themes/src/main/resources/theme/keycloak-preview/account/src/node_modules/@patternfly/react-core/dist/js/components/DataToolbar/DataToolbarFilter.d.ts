import * as React from 'react';
import { DataToolbarItemProps } from './DataToolbarItem';
import { PickOptional } from '../../helpers/typeUtils';
export interface DataToolbarChipGroup {
    /** A unique key to identify this chip group category */
    key: string;
    /** The category name to display for the chip group */
    name: string;
}
export interface DataToolbarChip {
    /** A unique key to identify this chip */
    key: string;
    /** The ReactNode to display in the chip */
    node: React.ReactNode;
}
export interface DataToolbarFilterProps extends DataToolbarItemProps {
    /** An array of strings to be displayed as chips in the expandable content */
    chips?: (string | DataToolbarChip)[];
    /** Callback passed by consumer used to delete a chip from the chips[] */
    deleteChip?: (category: string | DataToolbarChipGroup, chip: DataToolbarChip | string) => void;
    /** Content to be rendered inside the data toolbar item associated with the chip group */
    children: React.ReactNode;
    /** Unique category name to be used as a label for the chip group */
    categoryName: string | DataToolbarChipGroup;
    /** Flag to show the toolbar item */
    showToolbarItem?: boolean;
}
interface DataToolbarFilterState {
    isMounted: boolean;
}
export declare class DataToolbarFilter extends React.Component<DataToolbarFilterProps, DataToolbarFilterState> {
    static contextType: any;
    static defaultProps: PickOptional<DataToolbarFilterProps>;
    constructor(props: DataToolbarFilterProps);
    componentDidMount(): void;
    componentDidUpdate(): void;
    render(): JSX.Element;
}
export {};
