export function stringifyQueryParams(params: Record<string, unknown>) {
  const searchParams = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    // Ignore undefined and null values.
    if (value === undefined || value === null) {
      continue;
    }

    // Ignore empty strings.
    if (typeof value === "string" && value.length === 0) {
      continue;
    }

    // Ignore empty arrays.
    if (Array.isArray(value) && value.length === 0) {
      continue;
    }

    // Append each entry of an array as a separate parameter, or the value itself otherwise.
    if (Array.isArray(value)) {
      value.forEach((item) => searchParams.append(key, item.toString()));
    } else {
      searchParams.append(key, value.toString());
    }
  }

  return searchParams.toString();
}
