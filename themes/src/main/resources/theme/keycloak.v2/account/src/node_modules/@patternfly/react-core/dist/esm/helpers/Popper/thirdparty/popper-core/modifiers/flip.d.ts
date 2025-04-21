import { Placement, Boundary, RootBoundary } from '../enums';
import { Modifier, Padding } from '../types';
export interface Options {
    mainAxis: boolean;
    altAxis: boolean;
    fallbackPlacements: Placement[];
    padding: Padding;
    boundary: Boundary;
    rootBoundary: RootBoundary;
    altBoundary: boolean;
    flipVariations: boolean;
    allowedAutoPlacements: Placement[];
}
export declare type FlipModifier = Modifier<'flip', Options>;
declare const _default: FlipModifier;
export default _default;
//# sourceMappingURL=flip.d.ts.map