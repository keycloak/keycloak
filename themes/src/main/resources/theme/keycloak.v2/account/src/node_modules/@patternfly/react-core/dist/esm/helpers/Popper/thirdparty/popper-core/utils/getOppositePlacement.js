const hash = { left: 'right', right: 'left', bottom: 'top', top: 'bottom' };
/**
 * @param placement
 */
export default function getOppositePlacement(placement) {
    return placement.replace(/left|right|bottom|top/g, matched => hash[matched]);
}
//# sourceMappingURL=getOppositePlacement.js.map