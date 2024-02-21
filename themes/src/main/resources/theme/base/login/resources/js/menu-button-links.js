/*
 *   This content is licensed according to the W3C Software License at
 *   https://www.w3.org/Consortium/Legal/2015/copyright-software-and-document
 *
 *   File:   menu-button-links.js
 *
 *   Desc:   Creates a menu button that opens a menu of links
 * 
 *   Modified: Peter Keuter. The original source file is transpiled to ES5
 */

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }
function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, _toPropertyKey(descriptor.key), descriptor); } }
function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); Object.defineProperty(Constructor, "prototype", { writable: false }); return Constructor; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : String(i); }
function _toPrimitive(t, r) { if ("object" != typeof t || !t) return t; var e = t[Symbol.toPrimitive]; if (void 0 !== e) { var i = e.call(t, r || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === r ? String : Number)(t); }
let MenuButtonLinks = /*#__PURE__*/function () {
  function MenuButtonLinks(domNode) {
    _classCallCheck(this, MenuButtonLinks);
    this.domNode = domNode;
    this.buttonNode = domNode.querySelector('button');
    this.menuNode = domNode.querySelector('[role="menu"]');
    this.menuitemNodes = [];
    this.firstMenuitem = false;
    this.lastMenuitem = false;
    this.firstChars = [];
    this.buttonNode.addEventListener('keydown', this.onButtonKeydown.bind(this));
    this.buttonNode.addEventListener('click', this.onButtonClick.bind(this));
    var nodes = domNode.querySelectorAll('[role="menuitem"]');
    for (var i = 0; i < nodes.length; i++) {
      var menuitem = nodes[i];
      this.menuitemNodes.push(menuitem);
      menuitem.tabIndex = -1;
      this.firstChars.push(menuitem.textContent.trim()[0].toLowerCase());
      menuitem.addEventListener('keydown', this.onMenuitemKeydown.bind(this));
      menuitem.addEventListener('mouseover', this.onMenuitemMouseover.bind(this));
      if (!this.firstMenuitem) {
        this.firstMenuitem = menuitem;
      }
      this.lastMenuitem = menuitem;
    }
    domNode.addEventListener('focusin', this.onFocusin.bind(this));
    domNode.addEventListener('focusout', this.onFocusout.bind(this));
    window.addEventListener('mousedown', this.onBackgroundMousedown.bind(this), true);
  }
  _createClass(MenuButtonLinks, [{
    key: "setFocusToMenuitem",
    value: function setFocusToMenuitem(newMenuitem) {
      this.menuitemNodes.forEach(function (item) {
        if (item === newMenuitem) {
          item.tabIndex = 0;
          newMenuitem.focus();
        } else {
          item.tabIndex = -1;
        }
      });
    }
  }, {
    key: "setFocusToFirstMenuitem",
    value: function setFocusToFirstMenuitem() {
      this.setFocusToMenuitem(this.firstMenuitem);
    }
  }, {
    key: "setFocusToLastMenuitem",
    value: function setFocusToLastMenuitem() {
      this.setFocusToMenuitem(this.lastMenuitem);
    }
  }, {
    key: "setFocusToPreviousMenuitem",
    value: function setFocusToPreviousMenuitem(currentMenuitem) {
      var newMenuitem, index;
      if (currentMenuitem === this.firstMenuitem) {
        newMenuitem = this.lastMenuitem;
      } else {
        index = this.menuitemNodes.indexOf(currentMenuitem);
        newMenuitem = this.menuitemNodes[index - 1];
      }
      this.setFocusToMenuitem(newMenuitem);
      return newMenuitem;
    }
  }, {
    key: "setFocusToNextMenuitem",
    value: function setFocusToNextMenuitem(currentMenuitem) {
      var newMenuitem, index;
      if (currentMenuitem === this.lastMenuitem) {
        newMenuitem = this.firstMenuitem;
      } else {
        index = this.menuitemNodes.indexOf(currentMenuitem);
        newMenuitem = this.menuitemNodes[index + 1];
      }
      this.setFocusToMenuitem(newMenuitem);
      return newMenuitem;
    }
  }, {
    key: "setFocusByFirstCharacter",
    value: function setFocusByFirstCharacter(currentMenuitem, char) {
      var start, index;
      if (char.length > 1) {
        return;
      }
      char = char.toLowerCase();

      // Get start index for search based on position of currentItem
      start = this.menuitemNodes.indexOf(currentMenuitem) + 1;
      if (start >= this.menuitemNodes.length) {
        start = 0;
      }

      // Check remaining slots in the menu
      index = this.firstChars.indexOf(char, start);

      // If not found in remaining slots, check from beginning
      if (index === -1) {
        index = this.firstChars.indexOf(char, 0);
      }

      // If match was found...
      if (index > -1) {
        this.setFocusToMenuitem(this.menuitemNodes[index]);
      }
    }

    // Utilities
  }, {
    key: "getIndexFirstChars",
    value: function getIndexFirstChars(startIndex, char) {
      for (var i = startIndex; i < this.firstChars.length; i++) {
        if (char === this.firstChars[i]) {
          return i;
        }
      }
      return -1;
    }

    // Popup menu methods
  }, {
    key: "openPopup",
    value: function openPopup() {
      this.menuNode.style.display = 'block';
      this.buttonNode.setAttribute('aria-expanded', 'true');
    }
  }, {
    key: "closePopup",
    value: function closePopup() {
      if (this.isOpen()) {
        this.buttonNode.setAttribute('aria-expanded', 'false');
        this.menuNode.style.display = 'none';
      }
    }
  }, {
    key: "isOpen",
    value: function isOpen() {
      return this.buttonNode.getAttribute('aria-expanded') === 'true';
    }

    // Menu event handlers
  }, {
    key: "onFocusin",
    value: function onFocusin() {
      this.domNode.classList.add('focus');
    }
  }, {
    key: "onFocusout",
    value: function onFocusout() {
      this.domNode.classList.remove('focus');
    }
  }, {
    key: "onButtonKeydown",
    value: function onButtonKeydown(event) {
      var key = event.key,
        flag = false;
      switch (key) {
        case ' ':
        case 'Enter':
        case 'ArrowDown':
        case 'Down':
          this.openPopup();
          this.setFocusToFirstMenuitem();
          flag = true;
          break;
        case 'Esc':
        case 'Escape':
          this.closePopup();
          this.buttonNode.focus();
          flag = true;
          break;
        case 'Up':
        case 'ArrowUp':
          this.openPopup();
          this.setFocusToLastMenuitem();
          flag = true;
          break;
        default:
          break;
      }
      if (flag) {
        event.stopPropagation();
        event.preventDefault();
      }
    }
  }, {
    key: "onButtonClick",
    value: function onButtonClick(event) {
      if (this.isOpen()) {
        this.closePopup();
        this.buttonNode.focus();
      } else {
        this.openPopup();
        this.setFocusToFirstMenuitem();
      }
      event.stopPropagation();
      event.preventDefault();
    }
  }, {
    key: "onMenuitemKeydown",
    value: function onMenuitemKeydown(event) {
      var tgt = event.currentTarget,
        key = event.key,
        flag = false;
      function isPrintableCharacter(str) {
        return str.length === 1 && str.match(/\S/);
      }
      if (event.ctrlKey || event.altKey || event.metaKey) {
        return;
      }
      if (event.shiftKey) {
        if (isPrintableCharacter(key)) {
          this.setFocusByFirstCharacter(tgt, key);
          flag = true;
        }
        if (event.key === 'Tab') {
          this.buttonNode.focus();
          this.closePopup();
          flag = true;
        }
      } else {
        switch (key) {
          case 'Enter':
          case ' ':
            window.location.href = tgt.href;
            break;
          case 'Esc':
          case 'Escape':
            this.closePopup();
            this.buttonNode.focus();
            flag = true;
            break;
          case 'Up':
          case 'ArrowUp':
            this.setFocusToPreviousMenuitem(tgt);
            flag = true;
            break;
          case 'ArrowDown':
          case 'Down':
            this.setFocusToNextMenuitem(tgt);
            flag = true;
            break;
          case 'Home':
          case 'PageUp':
            this.setFocusToFirstMenuitem();
            flag = true;
            break;
          case 'End':
          case 'PageDown':
            this.setFocusToLastMenuitem();
            flag = true;
            break;
          case 'Tab':
            this.closePopup();
            break;
          default:
            if (isPrintableCharacter(key)) {
              this.setFocusByFirstCharacter(tgt, key);
              flag = true;
            }
            break;
        }
      }
      if (flag) {
        event.stopPropagation();
        event.preventDefault();
      }
    }
  }, {
    key: "onMenuitemMouseover",
    value: function onMenuitemMouseover(event) {
      var tgt = event.currentTarget;
      tgt.focus();
    }
  }, {
    key: "onBackgroundMousedown",
    value: function onBackgroundMousedown(event) {
      if (!this.domNode.contains(event.target)) {
        if (this.isOpen()) {
          this.closePopup();
          this.buttonNode.focus();
        }
      }
    }
  }]);
  return MenuButtonLinks;
}(); // Initialize menu buttons
window.addEventListener('DOMContentLoaded', function () {
  var menuButtons = document.querySelectorAll('.menu-button-links');
  for (let i = 0; i < menuButtons.length; i++) {
    new MenuButtonLinks(menuButtons[i]);
  }
});