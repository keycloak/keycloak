import type { Context } from "react";
import { createContext } from "react";

export type NamedContext<T> = Context<T> &
  Required<Pick<Context<T>, "displayName">>;

export function createNamedContext<T>(displayName: string, defaultValue: T) {
  const context = createContext(defaultValue);
  context.displayName = displayName;
  return context as NamedContext<T>;
}
