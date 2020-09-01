import { ProviderRepresentation } from "./model/server-info";

export const sortProvider = (
  a: [string, ProviderRepresentation],
  b: [string, ProviderRepresentation]
) => {
  let s1, s2;
  if (a[1].order != b[1].order) {
    s1 = b[1].order;
    s2 = a[1].order;
  } else {
    s1 = a[0];
    s2 = b[0];
  }
  if (s1 < s2) {
    return -1;
  } else if (s1 > s2) {
    return 1;
  } else {
    return 0;
  }
};
