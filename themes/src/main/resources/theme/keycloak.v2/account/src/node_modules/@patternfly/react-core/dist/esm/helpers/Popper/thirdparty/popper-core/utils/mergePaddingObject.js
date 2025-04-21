import getFreshSideObject from './getFreshSideObject';
/**
 * @param paddingObject
 */
export default function mergePaddingObject(paddingObject) {
    return Object.assign(Object.assign({}, getFreshSideObject()), paddingObject);
}
//# sourceMappingURL=mergePaddingObject.js.map