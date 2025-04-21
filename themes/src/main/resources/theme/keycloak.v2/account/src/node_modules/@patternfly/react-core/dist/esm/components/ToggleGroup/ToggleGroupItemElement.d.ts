import * as React from 'react';
export declare enum ToggleGroupItemVariant {
    icon = "icon",
    text = "text"
}
export interface ToggleGroupItemElementProps {
    /** Content rendered inside the toggle group item */
    children?: React.ReactNode;
    /** Adds toggle group item variant styles */
    variant?: ToggleGroupItemVariant | 'icon' | 'text';
}
export declare const ToggleGroupItemElement: React.FunctionComponent<ToggleGroupItemElementProps>;
//# sourceMappingURL=ToggleGroupItemElement.d.ts.map