export declare const GutterSize: {
    sm: string;
    md: string;
    lg: string;
};
/**
 * @param {any} styleObj - Style object
 * @param {'sm' | 'md' | 'lg'} size - Size string 'sm', 'md', or 'lg'
 * @param {any} defaultValue - Default value
 */
export declare function getGutterModifier(styleObj: any, size: 'sm' | 'md' | 'lg', defaultValue: any): string;
