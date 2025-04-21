import { Rect, PositioningStrategy, Offsets, ClientRectObject } from '../types';
import { Placement } from '../enums';
/**
 *
 */
export default function computeOffsets({ reference, element, placement }: {
    reference: Rect | ClientRectObject;
    element: Rect | ClientRectObject;
    strategy: PositioningStrategy;
    placement?: Placement;
}): Offsets;
//# sourceMappingURL=computeOffsets.d.ts.map