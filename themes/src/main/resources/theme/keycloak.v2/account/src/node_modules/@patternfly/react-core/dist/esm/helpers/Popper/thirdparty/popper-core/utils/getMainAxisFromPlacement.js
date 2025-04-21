/**
 * @param placement
 */
export default function getMainAxisFromPlacement(placement) {
    return ['top', 'bottom'].indexOf(placement) >= 0 ? 'x' : 'y';
}
//# sourceMappingURL=getMainAxisFromPlacement.js.map