import * as React from 'react';
import { DataToolbarGroupProps } from './DataToolbarGroup';
import { DataToolbarBreakpointMod } from './DataToolbarUtils';
import { PickOptional } from '../../helpers/typeUtils';
export interface DataToolbarToggleGroupProps extends DataToolbarGroupProps {
    /** An icon to be rendered when the toggle group has collapsed down */
    toggleIcon: React.ReactNode;
    /** The breakpoint at which the toggle group is collapsed down */
    breakpoint: 'md' | 'lg' | 'xl';
    /** An array of objects representing the various modifiers to apply to the data toolbar toggle group at various breakpoints */
    breakpointMods?: DataToolbarBreakpointMod[];
}
export declare class DataToolbarToggleGroup extends React.Component<DataToolbarToggleGroupProps> {
    static defaultProps: PickOptional<DataToolbarToggleGroupProps>;
    isContentPopup: () => boolean;
    render(): JSX.Element;
}
