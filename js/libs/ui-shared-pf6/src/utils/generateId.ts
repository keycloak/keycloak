let nextId = 0;

/** Monotonic ID for React keys / alert dismissal; avoids Math.random collisions. */
export const generateId = () => {
  nextId += 1;
  return nextId;
};
