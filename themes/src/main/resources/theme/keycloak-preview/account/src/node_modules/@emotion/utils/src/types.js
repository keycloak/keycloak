// @flow
export type RegisteredCache = { [string]: string }

interface StyleSheet {
  container: HTMLElement;
  nonce: string | void;
  key: string;
  insert(rule: string): void;
  flush(): void;
  tags: Array<HTMLStyleElement>;
}

export type CSSContextType = {
  stylis: (string, string) => Array<string>,
  inserted: { [string]: string | true },
  registered: RegisteredCache,
  sheet: StyleSheet,
  theme: Object,
  key: string,
  compat?: true
}

export type Interpolation = any

export type ScopedInsertableStyles = {|
  name: string,
  styles: string
|}
