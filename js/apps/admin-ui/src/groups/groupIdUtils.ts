export const getId = (pathname: string) => {
  const pathParts = pathname.substring(1).split("/");
  return pathParts.length > 1 ? pathParts.splice(2) : undefined;
};

export const getLastId = (pathname: string) => {
  const pathParts = getId(pathname);
  return pathParts ? pathParts[pathParts.length - 1] : undefined;
};
