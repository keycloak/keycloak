import * as React from 'react';
import { RefObject } from 'react';
interface DataToolbarContextProps {
    isExpanded: boolean;
    toggleIsExpanded: () => void;
    chipGroupContentRef: RefObject<HTMLDivElement>;
    updateNumberFilters: (categoryName: string, numberOfFilters: number) => void;
    numberOfFilters: number;
}
export declare const DataToolbarContext: React.Context<Partial<DataToolbarContextProps>>;
interface DataToolbarContentContextProps {
    expandableContentRef: RefObject<HTMLDivElement>;
    expandableContentId: string;
    chipContainerRef: RefObject<any>;
}
export declare const DataToolbarContentContext: React.Context<Partial<DataToolbarContentContextProps>>;
export interface DataToolbarBreakpointMod {
    /** The attribute to modify  */
    modifier: 'hidden' | 'visible' | 'align-right' | 'align-left' | 'spacer-none' | 'spacer-sm' | 'spacer-md' | 'spacer-lg' | 'space-items-none' | 'space-items-sm' | 'space-items-md' | 'space-items-lg';
    /** The breakpoint at which to apply the modifier */
    breakpoint: 'md' | 'lg' | 'xl' | '2xl';
}
export declare const globalBreakpoints: (breakpoint: string) => number;
export {};
