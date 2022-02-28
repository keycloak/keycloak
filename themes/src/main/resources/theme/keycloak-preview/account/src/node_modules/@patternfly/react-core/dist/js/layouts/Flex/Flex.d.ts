import * as React from 'react';
import { FlexBreakpointMod } from './FlexUtils';
export interface FlexProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the Flex layout */
    children?: React.ReactNode;
    /** additional classes added to the Flex layout */
    className?: string;
    /** An array of objects representing the various modifiers to apply to the flex component at various breakpoints */
    breakpointMods?: FlexBreakpointMod[];
}
export declare const Flex: React.FunctionComponent<FlexProps>;
