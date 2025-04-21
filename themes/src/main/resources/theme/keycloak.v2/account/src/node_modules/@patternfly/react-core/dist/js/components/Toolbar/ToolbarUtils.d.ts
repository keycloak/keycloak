import * as React from 'react';
import { RefObject } from 'react';
export interface ToolbarContextProps {
    isExpanded: boolean;
    toggleIsExpanded: () => void;
    chipGroupContentRef: RefObject<HTMLDivElement>;
    updateNumberFilters: (categoryName: string, numberOfFilters: number) => void;
    numberOfFilters: number;
    clearAllFilters?: () => void;
    clearFiltersButtonText?: string;
    showClearFiltersButton?: boolean;
    toolbarId?: string;
    customChipGroupContent?: React.ReactNode;
}
export declare const ToolbarContext: React.Context<ToolbarContextProps>;
interface ToolbarContentContextProps {
    expandableContentRef: RefObject<HTMLDivElement>;
    expandableContentId: string;
    chipContainerRef: RefObject<any>;
}
export declare const ToolbarContentContext: React.Context<ToolbarContentContextProps>;
export declare const globalBreakpoints: {
    md: number;
    lg: number;
    xl: number;
    '2xl': number;
};
export {};
//# sourceMappingURL=ToolbarUtils.d.ts.map