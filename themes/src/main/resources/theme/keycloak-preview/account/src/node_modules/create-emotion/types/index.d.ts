// Definitions by: Junyoung Clare Jang <https://github.com/Ailrun>
// TypeScript Version: 2.3

import * as CSS from 'csstype';

export type CSSBaseObject = CSS.PropertiesFallback<number | string>;
export type CSSPseudoObject = { [K in CSS.Pseudos]?: CSSObject };
export interface CSSOthersObject {
  [propertiesName: string]: Interpolation;
}
export interface CSSObject extends CSSBaseObject, CSSPseudoObject, CSSOthersObject {}

export interface ArrayInterpolation extends Array<Interpolation> {}

export interface ClassInterpolation extends Function {
  __emotion_real: any;
  __emotion_styles: Array<Interpolation>;
  __emotion_base: ClassInterpolation;
  __emotion_target: string;
  __emotion_forwardProp: undefined | null | ((arg: string) => boolean);
}

export type Interpolation =
  | undefined | null | boolean | string | number
  | TemplateStringsArray
  | CSSObject
  | ArrayInterpolation
  | ClassInterpolation
  ;

export interface ArrayClassNameArg extends Array<ClassNameArg> {}

export type ClassNameArg =
  | undefined | null | boolean | string
  | { [key: string]: undefined | null | boolean | string }
  | ArrayClassNameArg
  ;

export interface StyleSheet {
  inject(): void;
  speedy(bool: boolean): void;
  insert(rule: string, sourceMap?: string): void;
  flush(): void;
}

export interface EmotionCache {
  registered: {
    [key: string]: string;
  };
  inserted: {
    [key: string]: string;
  };
  nonce?: string;
  key: string;
}

export interface Emotion {
  flush(): void;
  hydrate(ids: Array<string>): void;
  cx(...classNames: Array<ClassNameArg>): string;
  merge(className: string, sourceMap?: string): string;
  getRegisteredStyles(registeredStyles: Array<string>, classNames: string): string;
  css(...args: Array<Interpolation>): string;
  injectGlobal(...args: Array<Interpolation>): void;
  keyframes(...args: Array<Interpolation>): string;
  sheet: StyleSheet;
  caches: EmotionCache;
}

export interface EmotionBaseContext {
  __SECRET_EMOTION__?: Emotion;
}

export interface EmotionContext extends EmotionBaseContext {
  [key: string]: any;
}

export type StylisPlugins =
  | null
  | ((...args: Array<any>) => any)
  | Array<(...args: Array<any>) => any>
  ;

export interface EmotionOptions {
  nonce?: string;
  stylisPlugins?: StylisPlugins;
  prefix?: boolean | ((key: string, value: string, context: 1 | 2 | 3) => boolean);
  key?: string;
  container?: HTMLElement;
}

export default function createEmotion(context: EmotionContext, options?: EmotionOptions): Emotion;

declare module 'react' {
  interface HTMLAttributes<T> {
    css?: Interpolation;
  }
}

// Preact support for css prop
declare global {
  namespace JSX {
    interface HTMLAttributes {
      css?: Interpolation;
    }
  }
}
