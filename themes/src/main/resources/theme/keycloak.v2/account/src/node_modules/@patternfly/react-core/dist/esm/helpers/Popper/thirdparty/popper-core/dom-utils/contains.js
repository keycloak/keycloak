// @ts-nocheck
/**
 * @param parent
 * @param child
 */
export default function contains(parent, child) {
    // $FlowFixMe: hasOwnProperty doesn't seem to work in tests
    const isShadow = Boolean(child.getRootNode && child.getRootNode().host);
    // First, attempt with faster native method
    if (parent.contains(child)) {
        return true;
    } // then fallback to custom implementation with Shadow DOM support
    else if (isShadow) {
        let next = child;
        do {
            if (next && parent.isSameNode(next)) {
                return true;
            }
            // $FlowFixMe: need a better way to handle this...
            next = next.parentNode || next.host;
        } while (next);
    }
    // Give up, the result is false
    return false;
}
//# sourceMappingURL=contains.js.map