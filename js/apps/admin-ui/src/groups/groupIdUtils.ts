export const getId = (pathname: string) => {
  const pathParts = pathname.substring(1).split("/");
  const spliceStart = pathParts[1] === "organizations" ? 4 : 2;
  return pathParts.length > 1 ? pathParts.splice(spliceStart) : undefined;
};

export const getLastId = (pathname: string) => {
  const pathParts = getId(pathname);
  return pathParts ? pathParts[pathParts.length - 1] : undefined;
};
