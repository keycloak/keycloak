import * as React from 'react';
export declare enum DropdownPosition {
    right = "right",
    left = "left"
}
export declare enum DropdownDirection {
    up = "up",
    down = "down"
}
export declare const DropdownContext: React.Context<{
    onSelect?: (event?: any) => void;
    id?: string;
    toggleIconClass?: string;
    toggleTextClass?: string;
    menuClass?: string;
    itemClass?: string;
    toggleClass?: string;
    baseClass?: string;
    baseComponent?: string;
    sectionClass?: string;
    sectionTitleClass?: string;
    sectionComponent?: string;
    disabledClass?: string;
    hoverClass?: string;
    separatorClass?: string;
    menuComponent?: string;
}>;
export declare const DropdownArrowContext: React.Context<{
    keyHandler: any;
    sendRef: any;
}>;
