import { State, SideObject, Padding } from '../types';
import { Placement, Boundary, RootBoundary, Context } from '../enums';
export interface Options {
    placement: Placement;
    boundary: Boundary;
    rootBoundary: RootBoundary;
    elementContext: Context;
    altBoundary: boolean;
    padding: Padding;
}
/**
 * @param state
 * @param options
 */
export default function detectOverflow(state: State, options?: Partial<Options>): SideObject;
//# sourceMappingURL=detectOverflow.d.ts.map