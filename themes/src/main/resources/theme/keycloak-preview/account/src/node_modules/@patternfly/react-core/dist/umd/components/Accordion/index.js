(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./Accordion", "./AccordionItem", "./AccordionContent", "./AccordionToggle"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./Accordion"), require("./AccordionItem"), require("./AccordionContent"), require("./AccordionToggle"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.Accordion, global.AccordionItem, global.AccordionContent, global.AccordionToggle);
    global.undefined = mod.exports;
  }
})(this, function (exports, _Accordion, _AccordionItem, _AccordionContent, _AccordionToggle) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_Accordion).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Accordion[key];
      }
    });
  });
  Object.keys(_AccordionItem).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _AccordionItem[key];
      }
    });
  });
  Object.keys(_AccordionContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _AccordionContent[key];
      }
    });
  });
  Object.keys(_AccordionToggle).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _AccordionToggle[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map