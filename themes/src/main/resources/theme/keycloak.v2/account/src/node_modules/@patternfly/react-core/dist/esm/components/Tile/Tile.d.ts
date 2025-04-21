import * as React from 'react';
export interface TileProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the banner */
    children?: React.ReactNode;
    /** Additional classes added to the banner */
    className?: string;
    /** Title of the tile */
    title: string;
    /** Icon in the tile title */
    icon?: React.ReactNode;
    /** Flag indicating if the tile is selected */
    isSelected?: boolean;
    /** Flag indicating if the tile is disabled */
    isDisabled?: boolean;
    /** Flag indicating if the tile header is stacked */
    isStacked?: boolean;
    /** Flag indicating if the stacked tile icon is large */
    isDisplayLarge?: boolean;
}
export declare const Tile: React.FunctionComponent<TileProps>;
//# sourceMappingURL=Tile.d.ts.map