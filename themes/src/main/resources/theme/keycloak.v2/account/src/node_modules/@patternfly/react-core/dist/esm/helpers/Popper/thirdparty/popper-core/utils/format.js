// @ts-nocheck
/**
 * @param str
 * @param args
 */
export default function format(str, ...args) {
    return [...args].reduce((p, c) => p.replace(/%s/, c), str);
}
//# sourceMappingURL=format.js.map