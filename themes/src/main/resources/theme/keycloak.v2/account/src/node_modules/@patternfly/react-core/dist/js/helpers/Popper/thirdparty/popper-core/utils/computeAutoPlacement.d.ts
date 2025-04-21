import { State, Padding } from '../types';
import { Placement, ComputedPlacement, Boundary, RootBoundary } from '../enums';
interface Options {
    placement: Placement;
    padding: Padding;
    boundary: Boundary;
    rootBoundary: RootBoundary;
    flipVariations: boolean;
    allowedAutoPlacements?: Placement[];
}
/**
 * @param state
 * @param options
 */
export default function computeAutoPlacement(state: Partial<State>, options?: Options): ComputedPlacement[];
export {};
//# sourceMappingURL=computeAutoPlacement.d.ts.map