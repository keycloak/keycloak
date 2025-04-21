import * as React from 'react';
export declare enum IconSize {
    sm = "sm",
    md = "md",
    lg = "lg",
    xl = "xl"
}
export declare const getSize: (size: IconSize | keyof typeof IconSize) => "1em" | "1.5em" | "2em" | "3em";
export interface IconDefinition {
    name?: string;
    width: number;
    height: number;
    svgPath: string;
    xOffset?: number;
    yOffset?: number;
}
export interface SVGIconProps extends Omit<React.HTMLProps<SVGElement>, 'size' | 'ref'> {
    color?: string;
    size?: IconSize | keyof typeof IconSize;
    title?: string;
    noVerticalAlign?: boolean;
}
/**
 * Factory to create Icon class components for consumers
 */
export declare function createIcon({ name, xOffset, yOffset, width, height, svgPath }: IconDefinition): React.ComponentClass<SVGIconProps>;
//# sourceMappingURL=createIcon.d.ts.map