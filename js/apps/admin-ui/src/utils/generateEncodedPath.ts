import { generatePath } from "react-router-dom";

export type PathParam = { [key: string]: string };

export function generateEncodedPath<Path extends string>(
  originalPath: Path,
  params: PathParam,
): string {
  const encodedParams: PathParam = {};

  Object.entries(params).forEach(
    ([k, v]) => (encodedParams[k] = encodeURIComponent(v)),
  );

  //TODO: Fix type once https://github.com/remix-run/react-router/pull/10719 is merged.
  return generatePath(originalPath, encodedParams as any);
}
