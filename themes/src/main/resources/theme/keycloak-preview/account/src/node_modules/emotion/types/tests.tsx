import {
  flush,
  hydrate,
  cx,
  merge,
  getRegisteredStyles,
  css,
  injectGlobal,
  keyframes,
  sheet,
  caches,
} from '../';
// tslint:disable-next-line:no-implicit-dependencies
import * as React from 'react';

flush();

hydrate(['css-123', 'css-456']);

const cssObject = {
  height: 100,
  width: '100%',
  display: 'block',
  position: undefined,
  ':hover': {
    display: 'block'
  }
};

const className: string = css`
  ${(true as boolean) && ''}
  ${'bar'}
  ${css``}
  ${1}
  ${cssObject}
`;

const className2: string = css(cssObject);

css([
  { display: 'none' },
  [
    { position: 'relative' },
    { width: 100 }
  ]
]);

css(
  { display: 'none' },
  [
    { position: 'relative' },
    { width: 100 }
  ]
);

css(null);

injectGlobal`
  #foo {
    font-face: 'Foo';
  }
`;

injectGlobal({
  html: {
    width: '100vw',
    height: '100vh',
  },
  '#root': {
    fontWeight: 'bold',
  },
});

keyframes({
  '0%': {
    transform: 'scaleY(0.5)',
  },
  to: {
    transform: 'scaleY(1)',
  },
});

keyframes`
  0% {
    transform: translateX(100%);
  }
  40% {
    transform: translateX(50%);
  }
  60% {
    transform: translateX(30%);
  }
  100% {
    transform: translateX(100%);
  }
`;

const cxResult: string = cx([
  [className, false && className2, 'modal'],
  [[className, { [className2]: true }, 'profile']]
]);

merge(`class1 class2 ${className}`);

getRegisteredStyles([], className);

sheet.speedy(true);
sheet.inject();
sheet.insert('.foo { font-size: 1 };', 'some source map');
sheet.flush();

caches.inserted;
caches.key;
caches.nonce;
caches.registered;

/*
 * Can use css prop, transpiled by babel plugin
 */

<div css={`color: red;`} />;
<div css={{ color: 'red' }} />;
