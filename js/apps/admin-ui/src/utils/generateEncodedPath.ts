import { generatePath, type PathParam } from "react-router-dom";

/**
 * Represents an object that contains the parameters to be included in a path.
 *
 * @example
 * const params: PathParams<"/user/:id"> = { id: "123" };
 */
export type PathParams<Path extends string> = {
  [key in PathParam<Path>]: string;
};

/**
 * Generates a path that represents the given path template with parameters interpolated and encoded in it, so that it can safely used in a URL.
 *
 * @param originalPath - The path template to use to generate the path.
 * @param params - An object that contains the parameters to be included in the path.
 *
 * @example
 * const path = "/user/:id";
 * const params = { id: "123" };
 * const encodedPath = generateEncodedPath(path, params);
 * // encodedPath will be "/user/123"
 */
export function generateEncodedPath<Path extends string>(
  originalPath: Path,
  params: PathParams<Path>,
): string {
  return generatePath(
    originalPath,
    params as Parameters<typeof generatePath<Path>>[1],
  );
}
