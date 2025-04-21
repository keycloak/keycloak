import callBind from 'call-bind';

import getPolyfill from 'object.hasown/polyfill';

export default callBind(getPolyfill(), null);

export { default as getPolyfill } from 'object.hasown/polyfill';
export { default as implementation } from 'object.hasown/implementation';
export { default as shim } from 'object.hasown/shim';
