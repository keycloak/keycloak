import { StyleDeclarationStatic } from './utils';
import { Interpolation } from 'emotion';
import { ClassNameArg } from 'create-emotion';

// eslint-disable-next-line @typescript-eslint/array-type
type emotionCss = (...classNames: Array<ClassNameArg | StyleDeclarationStatic>) => string;

export interface StyleSheetStatic {
  parse(cssString: string): StyleSheetValueStatic;
  create<T extends Record<keyof T, Interpolation>>(styles: T): Record<keyof T, string>;
}

export type StyleSheetValueStatic = {
  modifiers: { [key: string]: StyleDeclarationStatic };
  inject(): void;
} & {
  [key: string]: any;
};

export const StyleSheet: StyleSheetStatic;

export const css: emotionCss;
