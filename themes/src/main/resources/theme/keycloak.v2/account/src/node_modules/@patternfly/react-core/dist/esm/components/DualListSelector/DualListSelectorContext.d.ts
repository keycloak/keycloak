import * as React from 'react';
export declare const DualListSelectorContext: React.Context<{
    isTree?: boolean;
}>;
export declare const DualListSelectorListContext: React.Context<{
    setFocusedOption?: (id: string) => void;
    isTree?: boolean;
    ariaLabelledBy?: string;
    focusedOption?: string;
    displayOption?: (option: React.ReactNode) => boolean;
    selectedOptions?: string[] | number[];
    id?: string;
    onOptionSelect?: (e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent, index: number, id: string) => void;
    options?: React.ReactNode[];
    isDisabled?: boolean;
}>;
export declare const DualListSelectorPaneContext: React.Context<{
    isChosen: boolean;
}>;
//# sourceMappingURL=DualListSelectorContext.d.ts.map