import * as React from 'react';
import { ToolbarGroupProps } from './ToolbarGroup';
export interface ToolbarToggleGroupProps extends ToolbarGroupProps {
    /** An icon to be rendered when the toggle group has collapsed down */
    toggleIcon: React.ReactNode;
    /** Controls when filters are shown and when the toggle button is hidden. */
    breakpoint: 'md' | 'lg' | 'xl' | '2xl';
    /** Visibility at various breakpoints. */
    visibility?: {
        default?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
    /** @deprecated prop misspelled */
    visiblity?: {
        default?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
    /** Alignment at various breakpoints. */
    alignment?: {
        default?: 'alignRight' | 'alignLeft';
        md?: 'alignRight' | 'alignLeft';
        lg?: 'alignRight' | 'alignLeft';
        xl?: 'alignRight' | 'alignLeft';
        '2xl'?: 'alignRight' | 'alignLeft';
    };
    /** Spacers at various breakpoints. */
    spacer?: {
        default?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
        md?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
        lg?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
        xl?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
        '2xl'?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
    };
    /** Space items at various breakpoints. */
    spaceItems?: {
        default?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
        md?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
        lg?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
        xl?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
        '2xl'?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
    };
}
export declare class ToolbarToggleGroup extends React.Component<ToolbarToggleGroupProps> {
    static displayName: string;
    isContentPopup: () => boolean;
    render(): JSX.Element;
}
//# sourceMappingURL=ToolbarToggleGroup.d.ts.map