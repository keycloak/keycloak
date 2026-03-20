/**
 * @param {...string} paths
 * @returns {string}
 */
export function joinPath(...paths) {
  return paths
    .map((path, index) => {
      if (index === 0) {
        return path.replace(/\/+$/, "");
      }
      return path.replace(/^\/+|\/+$/g, "");
    })
    .filter(Boolean)
    .join("/");
}
