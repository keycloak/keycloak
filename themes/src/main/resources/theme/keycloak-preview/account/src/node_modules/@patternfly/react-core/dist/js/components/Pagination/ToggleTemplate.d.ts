/// <reference types="react" />
export interface ToggleTemplateProps {
    /** The first index of the items being paginated */
    firstIndex?: number;
    /** The last index of the items being paginated */
    lastIndex?: number;
    /** The total number of items being paginated */
    itemCount?: number;
    /** The type or title of the items being paginated */
    itemsTitle?: string;
}
export declare const ToggleTemplate: ({ firstIndex, lastIndex, itemCount, itemsTitle }: ToggleTemplateProps) => JSX.Element;
