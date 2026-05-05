/**
 * @typedef {Object} MenuItem
 * @property {string} label
 * @property {string} [path]
 * @property {string} [component]
 * @property {string} [modulePath]
 * @property {string} [isVisible]
 * @property {MenuItem[]} [children]
 */

/**
 * @param {MenuItem[]} items
 * @returns {MenuItem[]}
 */
export function flattenMenuItems(items) {
  const result = [];

  for (const item of items) {
    if (item.path !== undefined && item.component) {
      result.push(item);
    }
    if (item.children) {
      result.push(...flattenMenuItems(item.children));
    }
  }

  return result;
}
