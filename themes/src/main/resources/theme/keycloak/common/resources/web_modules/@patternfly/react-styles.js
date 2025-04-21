function n(...o){const r=[],e={}.hasOwnProperty;return o.filter(Boolean).forEach(o=>{const t=typeof o;if("string"===t||"number"===t)r.push(o);else if(Array.isArray(o)&&o.length){const e=n(...o);e&&r.push(e)}else if("object"===t)for(const n in o)e.call(o,n)&&o[n]&&r.push(n)}),r.join(" ")}export{n as css};
//# sourceMappingURL=react-styles.js.map
