export const normalizeNavRoutePath = (routePath: string): string =>
  routePath.replace(/\/:.+?(\?|(?:(?!\/).)*|$)/g, "").replace(/\/\*$/, "");
