export const isLightweightUser = (userId?: string) =>
  userId?.startsWith("lightweight-");
