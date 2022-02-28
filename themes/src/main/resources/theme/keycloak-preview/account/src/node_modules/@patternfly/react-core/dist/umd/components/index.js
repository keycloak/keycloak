(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "./AboutModal", "./Accordion", "./Alert", "./AlertGroup", "./ApplicationLauncher", "./Avatar", "./Backdrop", "./BackgroundImage", "./Badge", "./Brand", "./Breadcrumb", "./Button", "./Card", "./Checkbox", "./ChipGroup", "./ClipboardCopy", "./ContextSelector", "./DataList", "./Dropdown", "./EmptyState", "./Expandable", "./FileUpload", "./Form", "./FormSelect", "./InputGroup", "./Label", "./List", "./LoginPage", "./Modal", "./Nav", "./NotificationBadge", "./OptionsMenu", "./Page", "./Pagination", "./Popover", "./Progress", "./Radio", "./Select", "./SimpleList", "./SkipToContent", "./Switch", "./Tabs", "./Text", "./TextArea", "./TextInput", "./Title", "./Tooltip", "./Wizard", "./withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("./AboutModal"), require("./Accordion"), require("./Alert"), require("./AlertGroup"), require("./ApplicationLauncher"), require("./Avatar"), require("./Backdrop"), require("./BackgroundImage"), require("./Badge"), require("./Brand"), require("./Breadcrumb"), require("./Button"), require("./Card"), require("./Checkbox"), require("./ChipGroup"), require("./ClipboardCopy"), require("./ContextSelector"), require("./DataList"), require("./Dropdown"), require("./EmptyState"), require("./Expandable"), require("./FileUpload"), require("./Form"), require("./FormSelect"), require("./InputGroup"), require("./Label"), require("./List"), require("./LoginPage"), require("./Modal"), require("./Nav"), require("./NotificationBadge"), require("./OptionsMenu"), require("./Page"), require("./Pagination"), require("./Popover"), require("./Progress"), require("./Radio"), require("./Select"), require("./SimpleList"), require("./SkipToContent"), require("./Switch"), require("./Tabs"), require("./Text"), require("./TextArea"), require("./TextInput"), require("./Title"), require("./Tooltip"), require("./Wizard"), require("./withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.AboutModal, global.Accordion, global.Alert, global.AlertGroup, global.ApplicationLauncher, global.Avatar, global.Backdrop, global.BackgroundImage, global.Badge, global.Brand, global.Breadcrumb, global.Button, global.Card, global.Checkbox, global.ChipGroup, global.ClipboardCopy, global.ContextSelector, global.DataList, global.Dropdown, global.EmptyState, global.Expandable, global.FileUpload, global.Form, global.FormSelect, global.InputGroup, global.Label, global.List, global.LoginPage, global.Modal, global.Nav, global.NotificationBadge, global.OptionsMenu, global.Page, global.Pagination, global.Popover, global.Progress, global.Radio, global.Select, global.SimpleList, global.SkipToContent, global.Switch, global.Tabs, global.Text, global.TextArea, global.TextInput, global.Title, global.Tooltip, global.Wizard, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _AboutModal, _Accordion, _Alert, _AlertGroup, _ApplicationLauncher, _Avatar, _Backdrop, _BackgroundImage, _Badge, _Brand, _Breadcrumb, _Button, _Card, _Checkbox, _ChipGroup, _ClipboardCopy, _ContextSelector, _DataList, _Dropdown, _EmptyState, _Expandable, _FileUpload, _Form, _FormSelect, _InputGroup, _Label, _List, _LoginPage, _Modal, _Nav, _NotificationBadge, _OptionsMenu, _Page, _Pagination, _Popover, _Progress, _Radio, _Select, _SimpleList, _SkipToContent, _Switch, _Tabs, _Text, _TextArea, _TextInput, _Title, _Tooltip, _Wizard, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  Object.keys(_AboutModal).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _AboutModal[key];
      }
    });
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
  Object.keys(_Alert).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Alert[key];
      }
    });
  });
  Object.keys(_AlertGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _AlertGroup[key];
      }
    });
  });
  Object.keys(_ApplicationLauncher).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ApplicationLauncher[key];
      }
    });
  });
  Object.keys(_Avatar).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Avatar[key];
      }
    });
  });
  Object.keys(_Backdrop).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Backdrop[key];
      }
    });
  });
  Object.keys(_BackgroundImage).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _BackgroundImage[key];
      }
    });
  });
  Object.keys(_Badge).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Badge[key];
      }
    });
  });
  Object.keys(_Brand).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Brand[key];
      }
    });
  });
  Object.keys(_Breadcrumb).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Breadcrumb[key];
      }
    });
  });
  Object.keys(_Button).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Button[key];
      }
    });
  });
  Object.keys(_Card).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Card[key];
      }
    });
  });
  Object.keys(_Checkbox).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Checkbox[key];
      }
    });
  });
  Object.keys(_ChipGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ChipGroup[key];
      }
    });
  });
  Object.keys(_ClipboardCopy).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ClipboardCopy[key];
      }
    });
  });
  Object.keys(_ContextSelector).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _ContextSelector[key];
      }
    });
  });
  Object.keys(_DataList).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _DataList[key];
      }
    });
  });
  Object.keys(_Dropdown).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Dropdown[key];
      }
    });
  });
  Object.keys(_EmptyState).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _EmptyState[key];
      }
    });
  });
  Object.keys(_Expandable).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Expandable[key];
      }
    });
  });
  Object.keys(_FileUpload).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FileUpload[key];
      }
    });
  });
  Object.keys(_Form).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Form[key];
      }
    });
  });
  Object.keys(_FormSelect).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _FormSelect[key];
      }
    });
  });
  Object.keys(_InputGroup).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _InputGroup[key];
      }
    });
  });
  Object.keys(_Label).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Label[key];
      }
    });
  });
  Object.keys(_List).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _List[key];
      }
    });
  });
  Object.keys(_LoginPage).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _LoginPage[key];
      }
    });
  });
  Object.keys(_Modal).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Modal[key];
      }
    });
  });
  Object.keys(_Nav).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Nav[key];
      }
    });
  });
  Object.keys(_NotificationBadge).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _NotificationBadge[key];
      }
    });
  });
  Object.keys(_OptionsMenu).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _OptionsMenu[key];
      }
    });
  });
  Object.keys(_Page).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Page[key];
      }
    });
  });
  Object.keys(_Pagination).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Pagination[key];
      }
    });
  });
  Object.keys(_Popover).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Popover[key];
      }
    });
  });
  Object.keys(_Progress).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Progress[key];
      }
    });
  });
  Object.keys(_Radio).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Radio[key];
      }
    });
  });
  Object.keys(_Select).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Select[key];
      }
    });
  });
  Object.keys(_SimpleList).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _SimpleList[key];
      }
    });
  });
  Object.keys(_SkipToContent).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _SkipToContent[key];
      }
    });
  });
  Object.keys(_Switch).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Switch[key];
      }
    });
  });
  Object.keys(_Tabs).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Tabs[key];
      }
    });
  });
  Object.keys(_Text).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Text[key];
      }
    });
  });
  Object.keys(_TextArea).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _TextArea[key];
      }
    });
  });
  Object.keys(_TextInput).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _TextInput[key];
      }
    });
  });
  Object.keys(_Title).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Title[key];
      }
    });
  });
  Object.keys(_Tooltip).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Tooltip[key];
      }
    });
  });
  Object.keys(_Wizard).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _Wizard[key];
      }
    });
  });
  Object.keys(_withOuia).forEach(function (key) {
    if (key === "default" || key === "__esModule") return;
    Object.defineProperty(exports, key, {
      enumerable: true,
      get: function () {
        return _withOuia[key];
      }
    });
  });
});
//# sourceMappingURL=index.js.map