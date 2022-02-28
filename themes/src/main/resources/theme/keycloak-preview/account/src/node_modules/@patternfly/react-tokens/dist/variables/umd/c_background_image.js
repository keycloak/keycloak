(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports);
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports);
    global.undefined = mod.exports;
  }
})(this, function (exports) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = {
    ".pf-c-background-image": [{
      "property": "--pf-c-background-image--BackgroundColor",
      "value": "#151515",
      "token": "c_background_image_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-100", "$pf-global--BackgroundColor--dark-100", "$pf-color-black-900", "#151515"]
    }, {
      "property": "--pf-c-background-image--BackgroundImage",
      "value": "url(\"../../assets/images/pfbg_576.jpg\")",
      "token": "c_background_image_BackgroundImage"
    }, {
      "property": "--pf-c-background-image--BackgroundImage-2x",
      "value": "url(\"../../assets/images/pfbg_576@2x.jpg\")",
      "token": "c_background_image_BackgroundImage_2x"
    }, {
      "property": "--pf-c-background-image--BackgroundImage--sm",
      "value": "url(\"../../assets/images/pfbg_768.jpg\")",
      "token": "c_background_image_BackgroundImage_sm"
    }, {
      "property": "--pf-c-background-image--BackgroundImage--sm-2x",
      "value": "url(\"../../assets/images/pfbg_768@2x.jpg\")",
      "token": "c_background_image_BackgroundImage_sm_2x"
    }, {
      "property": "--pf-c-background-image--BackgroundImage--lg",
      "value": "url(\"../../assets/images/pfbg_2000.jpg\")",
      "token": "c_background_image_BackgroundImage_lg"
    }, {
      "property": "--pf-c-background-image--Filter",
      "value": "url(\"#image_overlay\")",
      "token": "c_background_image_Filter"
    }]
  };
});