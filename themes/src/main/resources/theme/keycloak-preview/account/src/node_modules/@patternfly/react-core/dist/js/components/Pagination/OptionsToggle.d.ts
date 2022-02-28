import * as React from 'react';
import { ToggleTemplateProps } from './ToggleTemplate';
export interface OptionsToggleProps extends React.HTMLProps<HTMLDivElement> {
    /** The type or title of the items being paginated */
    itemsTitle?: string;
    /** The text to be displayed on the Options Toggle */
    optionsToggle?: string;
    /** The Title of the Pagination Options Menu */
    itemsPerPageTitle?: string;
    /** The first index of the items being paginated */
    firstIndex?: number;
    /** The last index of the items being paginated */
    lastIndex?: number;
    /** The total number of items being paginated */
    itemCount?: number;
    /** Id added to the title of the Pagination Options Menu */
    widgetId?: string;
    /** showToggle */
    showToggle?: boolean;
    /** Event function that fires when user clicks the Options Menu toggle */
    onToggle?: (isOpen: boolean) => void;
    /** Flag indicating if the Options Menu dropdown is open or not */
    isOpen?: boolean;
    /** Flag indicating if the Options Menu is disabled */
    isDisabled?: boolean;
    /** */
    parentRef?: HTMLElement;
    /** This will be shown in pagination toggle span. You can use firstIndex, lastIndex, itemCount, itemsTitle props. */
    toggleTemplate?: ((props: ToggleTemplateProps) => React.ReactElement) | string;
    /** Callback for toggle open on keyboard entry */
    onEnter?: () => void;
}
export declare const OptionsToggle: React.FunctionComponent<OptionsToggleProps>;
