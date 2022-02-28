import * as React from 'react';
import { FlexItemBreakpointMod } from './FlexUtils';
export interface FlexItemProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the Flex layout */
    children?: React.ReactNode;
    /** additional classes added to the Flex layout */
    className?: string;
    /** An array of objects representing the various modifiers to apply to the flex item at various breakpoints */
    breakpointMods?: FlexItemBreakpointMod[];
}
export declare const FlexItem: React.FunctionComponent<FlexItemProps>;
