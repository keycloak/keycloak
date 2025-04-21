import * as React from 'react';
import { ToggleTemplateProps } from './ToggleTemplate';
export interface OptionsToggleProps extends React.HTMLProps<HTMLDivElement> {
    /** The type or title of the items being paginated */
    itemsTitle?: string;
    /** Accessible label for the options toggle */
    optionsToggle?: string;
    /** The title of the pagination options menu */
    itemsPerPageTitle?: string;
    /** The first index of the items being paginated */
    firstIndex?: number;
    /** The last index of the items being paginated */
    lastIndex?: number;
    /** The total number of items being paginated */
    itemCount?: number;
    /** Id added to the title of the pagination options menu */
    widgetId?: string;
    /** showToggle */
    showToggle?: boolean;
    /** Event function that fires when user clicks the options menu toggle */
    onToggle?: (isOpen: boolean) => void;
    /** Flag indicating if the options menu dropdown is open or not */
    isOpen?: boolean;
    /** Flag indicating if the options menu is disabled */
    isDisabled?: boolean;
    /** */
    parentRef?: HTMLElement;
    /** This will be shown in pagination toggle span. You can use firstIndex, lastIndex, itemCount, itemsTitle props. */
    toggleTemplate?: ((props: ToggleTemplateProps) => React.ReactElement) | string;
    /** Callback for toggle open on keyboard entry */
    onEnter?: () => void;
    /** Label for the English word "of" */
    ofWord?: string;
    /** Component to be used for wrapping the toggle contents. Use 'button' when you want
     * all of the toggle text to be clickable.
     */
    perPageComponent?: 'div' | 'button';
}
export declare const OptionsToggle: React.FunctionComponent<OptionsToggleProps>;
//# sourceMappingURL=OptionsToggle.d.ts.map