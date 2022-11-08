export function stringifyQueryParams(params: Record<string, unknown>) {
  return new URLSearchParams(
    Object.entries(params).filter((param): param is [string, string] => {
      const [, value] = param;

      if (typeof value === "undefined" || value === null) {
        return false;
      }

      if (typeof value === "string" && value.length === 0) {
        return false;
      }

      if (Array.isArray(value) && value.length === 0) {
        return false;
      }

      return true;
    })
  ).toString();
}
