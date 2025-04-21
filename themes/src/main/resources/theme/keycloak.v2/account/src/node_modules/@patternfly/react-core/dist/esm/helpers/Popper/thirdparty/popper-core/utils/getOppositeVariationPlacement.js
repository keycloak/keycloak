const hash = { start: 'end', end: 'start' };
/**
 * @param placement
 */
export default function getOppositeVariationPlacement(placement) {
    return placement.replace(/start|end/g, matched => hash[matched]);
}
//# sourceMappingURL=getOppositeVariationPlacement.js.map