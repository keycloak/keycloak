/*
 * Copyright (C) 2007 Apple Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer. 
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution. 
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contains diff method based on Javascript Diff Algorithm By John Resig
 * http://ejohn.org/files/jsdiff.js (released under the MIT license).
 */

Function.prototype.bind = function(thisObject)
{
    var func = this;
    var args = Array.prototype.slice.call(arguments, 1);
    function bound()
    {
        return func.apply(thisObject, args.concat(Array.prototype.slice.call(arguments, 0)));
    }
    bound.toString = function() {
        return "bound: " + func;
    };
    return bound;
}

Node.prototype.rangeOfWord = function(offset, stopCharacters, stayWithinNode, direction)
{
    var startNode;
    var startOffset = 0;
    var endNode;
    var endOffset = 0;

    if (!stayWithinNode)
        stayWithinNode = this;

    if (!direction || direction === "backward" || direction === "both") {
        var node = this;
        while (node) {
            if (node === stayWithinNode) {
                if (!startNode)
                    startNode = stayWithinNode;
                break;
            }

            if (node.nodeType === Node.TEXT_NODE) {
                var start = (node === this ? (offset - 1) : (node.nodeValue.length - 1));
                for (var i = start; i >= 0; --i) {
                    if (stopCharacters.indexOf(node.nodeValue[i]) !== -1) {
                        startNode = node;
                        startOffset = i + 1;
                        break;
                    }
                }
            }

            if (startNode)
                break;

            node = node.traversePreviousNode(stayWithinNode);
        }

        if (!startNode) {
            startNode = stayWithinNode;
            startOffset = 0;
        }
    } else {
        startNode = this;
        startOffset = offset;
    }

    if (!direction || direction === "forward" || direction === "both") {
        node = this;
        while (node) {
            if (node === stayWithinNode) {
                if (!endNode)
                    endNode = stayWithinNode;
                break;
            }

            if (node.nodeType === Node.TEXT_NODE) {
                var start = (node === this ? offset : 0);
                for (var i = start; i < node.nodeValue.length; ++i) {
                    if (stopCharacters.indexOf(node.nodeValue[i]) !== -1) {
                        endNode = node;
                        endOffset = i;
                        break;
                    }
                }
            }

            if (endNode)
                break;

            node = node.traverseNextNode(stayWithinNode);
        }

        if (!endNode) {
            endNode = stayWithinNode;
            endOffset = stayWithinNode.nodeType === Node.TEXT_NODE ? stayWithinNode.nodeValue.length : stayWithinNode.childNodes.length;
        }
    } else {
        endNode = this;
        endOffset = offset;
    }

    var result = this.ownerDocument.createRange();
    result.setStart(startNode, startOffset);
    result.setEnd(endNode, endOffset);

    return result;
}

Node.prototype.traverseNextTextNode = function(stayWithin)
{
    var node = this.traverseNextNode(stayWithin);
    if (!node)
        return;

    while (node && node.nodeType !== Node.TEXT_NODE)
        node = node.traverseNextNode(stayWithin);

    return node;
}

Node.prototype.rangeBoundaryForOffset = function(offset)
{
    var node = this.traverseNextTextNode(this);
    while (node && offset > node.nodeValue.length) {
        offset -= node.nodeValue.length;
        node = node.traverseNextTextNode(this);
    }
    if (!node)
        return { container: this, offset: 0 };
    return { container: node, offset: offset };
}

Element.prototype.removeStyleClass = function(className) 
{
    // Test for the simple case first.
    if (this.className === className) {
        this.className = "";
        return;
    }

    var index = this.className.indexOf(className);
    if (index === -1)
        return;

    this.className = this.className.split(" ").filter(function(s) {
        return s && s !== className;
    }).join(" ");
}

Element.prototype.removeMatchingStyleClasses = function(classNameRegex)
{
    var regex = new RegExp("(^|\\s+)" + classNameRegex + "($|\\s+)");
    if (regex.test(this.className))
        this.className = this.className.replace(regex, " ");
}

Element.prototype.addStyleClass = function(className) 
{
    if (className && !this.hasStyleClass(className))
        this.className += (this.className.length ? " " + className : className);
}

Element.prototype.hasStyleClass = function(className) 
{
    if (!className)
        return false;
    // Test for the simple case
    if (this.className === className)
        return true;

    var index = this.className.indexOf(className);
    if (index === -1)
        return false;
    var toTest = " " + this.className + " ";
    return toTest.indexOf(" " + className + " ", index) !== -1;
}

Element.prototype.positionAt = function(x, y)
{
    this.style.left = x + "px";
    this.style.top = y + "px";
}

Element.prototype.pruneEmptyTextNodes = function()
{
    var sibling = this.firstChild;
    while (sibling) {
        var nextSibling = sibling.nextSibling;
        if (sibling.nodeType === this.TEXT_NODE && sibling.nodeValue === "")
            this.removeChild(sibling);
        sibling = nextSibling;
    }
}

Element.prototype.isScrolledToBottom = function()
{
    // This code works only for 0-width border
    return this.scrollTop + this.clientHeight === this.scrollHeight;
}

Node.prototype.enclosingNodeOrSelfWithNodeNameInArray = function(nameArray)
{
    for (var node = this; node && node !== this.ownerDocument; node = node.parentNode)
        for (var i = 0; i < nameArray.length; ++i)
            if (node.nodeName.toLowerCase() === nameArray[i].toLowerCase())
                return node;
    return null;
}

Node.prototype.enclosingNodeOrSelfWithNodeName = function(nodeName)
{
    return this.enclosingNodeOrSelfWithNodeNameInArray([nodeName]);
}

Node.prototype.enclosingNodeOrSelfWithClass = function(className)
{
    for (var node = this; node && node !== this.ownerDocument; node = node.parentNode)
        if (node.nodeType === Node.ELEMENT_NODE && node.hasStyleClass(className))
            return node;
    return null;
}

Node.prototype.enclosingNodeWithClass = function(className)
{
    if (!this.parentNode)
        return null;
    return this.parentNode.enclosingNodeOrSelfWithClass(className);
}

Element.prototype.query = function(query) 
{
    return this.ownerDocument.evaluate(query, this, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
}

Element.prototype.removeChildren = function()
{
    if (this.firstChild)
        this.textContent = "";
}

Element.prototype.isInsertionCaretInside = function()
{
    var selection = window.getSelection();
    if (!selection.rangeCount || !selection.isCollapsed)
        return false;
    var selectionRange = selection.getRangeAt(0);
    return selectionRange.startContainer === this || selectionRange.startContainer.isDescendant(this);
}

Element.prototype.createChild = function(elementName, className)
{
    var element = document.createElement(elementName);
    if (className)
        element.className = className;
    this.appendChild(element);
    return element;
}

Object.defineProperty(Element.prototype, "totalOffsetLeft", {get: function()
{
    var total = 0;
    for (var element = this; element; element = element.offsetParent)
        total += element.offsetLeft + (this !== element ? element.clientLeft : 0);
    return total;
}});

Object.defineProperty(Element.prototype, "totalOffsetTop", {get: function()
{
    var total = 0;
    for (var element = this; element; element = element.offsetParent)
        total += element.offsetTop + (this !== element ? element.clientTop : 0);
    return total;
}});

Element.prototype.offsetRelativeToWindow = function(targetWindow)
{
    var elementOffset = {x: 0, y: 0};
    var curElement = this;
    var curWindow = this.ownerDocument.defaultView;
    while (curWindow && curElement) {
        elementOffset.x += curElement.totalOffsetLeft;
        elementOffset.y += curElement.totalOffsetTop;
        if (curWindow === targetWindow)
            break;

        curElement = curWindow.frameElement;
        curWindow = curWindow.parent;
    }

    return elementOffset;
}

Object.defineProperty(KeyboardEvent.prototype, "data", {get: function()
{
    // Emulate "data" attribute from DOM 3 TextInput event.
    // See http://www.w3.org/TR/DOM-Level-3-Events/#events-Events-TextEvent-data
    switch (this.type) {
        case "keypress":
            if (!this.ctrlKey && !this.metaKey)
                return String.fromCharCode(this.charCode);
            else
                return "";
        case "keydown":
        case "keyup":
            if (!this.ctrlKey && !this.metaKey && !this.altKey)
                return String.fromCharCode(this.which);
            else
                return "";
    }
}});

Text.prototype.select = function(start, end)
{
    start = start || 0;
    end = end || this.textContent.length;

    if (start < 0)
        start = end + start;

    var selection = window.getSelection();
    selection.removeAllRanges();
    var range = document.createRange();
    range.setStart(this, start);
    range.setEnd(this, end);
    selection.addRange(range);
    return this;
}

Object.defineProperty(Element.prototype, "selectionLeftOffset", {get: function() {
    // Calculate selection offset relative to the current element.

    var selection = window.getSelection();
    if (!selection.containsNode(this, true))
        return null;

    var leftOffset = selection.anchorOffset;
    var node = selection.anchorNode;

    while (node !== this) {
        while (node.previousSibling) {
            node = node.previousSibling;
            leftOffset += node.textContent.length;
        }
        node = node.parentNode;
    }

    return leftOffset;
}});

Node.prototype.isWhitespace = isNodeWhitespace;
Node.prototype.displayName = nodeDisplayName;
Node.prototype.isAncestor = function(node)
{
    return isAncestorNode(this, node);
};
Node.prototype.isDescendant = isDescendantNode;
Node.prototype.traverseNextNode = traverseNextNode;
Node.prototype.traversePreviousNode = traversePreviousNode;
Node.prototype.onlyTextChild = onlyTextChild;

String.prototype.hasSubstring = function(string, caseInsensitive)
{
    if (!caseInsensitive)
        return this.indexOf(string) !== -1;
    return this.match(new RegExp(string.escapeForRegExp(), "i"));
}

String.prototype.findAll = function(string)
{
    var matches = [];
    var i = this.indexOf(string);
    while (i !== -1) {
        matches.push(i);
        i = this.indexOf(string, i + string.length);
    }
    return matches;
}

String.prototype.lineEndings = function()
{
    if (!this._lineEndings) {
        this._lineEndings = this.findAll("\n");
        this._lineEndings.push(this.length);
    }
    return this._lineEndings;
}

String.prototype.asParsedURL = function()
{
    // RegExp groups:
    // 1 - scheme
    // 2 - hostname
    // 3 - ?port
    // 4 - ?path
    // 5 - ?fragment
    var match = this.match(/^([^:]+):\/\/([^\/:]*)(?::([\d]+))?(?:(\/[^#]*)(?:#(.*))?)?$/i);
    if (!match)
        return null;
    var result = {};
    result.scheme = match[1].toLowerCase();
    result.host = match[2];
    result.port = match[3];
    result.path = match[4] || "/";
    result.fragment = match[5];
    return result;
}

String.prototype.escapeCharacters = function(chars)
{
    var foundChar = false;
    for (var i = 0; i < chars.length; ++i) {
        if (this.indexOf(chars.charAt(i)) !== -1) {
            foundChar = true;
            break;
        }
    }

    if (!foundChar)
        return this;

    var result = "";
    for (var i = 0; i < this.length; ++i) {
        if (chars.indexOf(this.charAt(i)) !== -1)
            result += "\\";
        result += this.charAt(i);
    }

    return result;
}

String.prototype.escapeForRegExp = function()
{
    return this.escapeCharacters("^[]{}()\\.$*+?|");
}

String.prototype.escapeHTML = function()
{
    return this.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

String.prototype.collapseWhitespace = function()
{
    return this.replace(/[\s\xA0]+/g, " ");
}

String.prototype.trimURL = function(baseURLDomain)
{
    var result = this.replace(/^(https|http|file):\/\//i, "");
    if (baseURLDomain)
        result = result.replace(new RegExp("^" + baseURLDomain.escapeForRegExp(), "i"), "");
    return result;
}

function isNodeWhitespace()
{
    if (!this || this.nodeType !== Node.TEXT_NODE)
        return false;
    if (!this.nodeValue.length)
        return true;
    return this.nodeValue.match(/^[\s\xA0]+$/);
}

function nodeDisplayName()
{
    if (!this)
        return "";

    switch (this.nodeType) {
        case Node.DOCUMENT_NODE:
            return "Document";

        case Node.ELEMENT_NODE:
            var name = "<" + this.nodeName.toLowerCase();

            if (this.hasAttributes()) {
                var value = this.getAttribute("id");
                if (value)
                    name += " id=\"" + value + "\"";
                value = this.getAttribute("class");
                if (value)
                    name += " class=\"" + value + "\"";
                if (this.nodeName.toLowerCase() === "a") {
                    value = this.getAttribute("name");
                    if (value)
                        name += " name=\"" + value + "\"";
                    value = this.getAttribute("href");
                    if (value)
                        name += " href=\"" + value + "\"";
                } else if (this.nodeName.toLowerCase() === "img") {
                    value = this.getAttribute("src");
                    if (value)
                        name += " src=\"" + value + "\"";
                } else if (this.nodeName.toLowerCase() === "iframe") {
                    value = this.getAttribute("src");
                    if (value)
                        name += " src=\"" + value + "\"";
                } else if (this.nodeName.toLowerCase() === "input") {
                    value = this.getAttribute("name");
                    if (value)
                        name += " name=\"" + value + "\"";
                    value = this.getAttribute("type");
                    if (value)
                        name += " type=\"" + value + "\"";
                } else if (this.nodeName.toLowerCase() === "form") {
                    value = this.getAttribute("action");
                    if (value)
                        name += " action=\"" + value + "\"";
                }
            }

            return name + ">";

        case Node.TEXT_NODE:
            if (isNodeWhitespace.call(this))
                return "(whitespace)";
            return "\"" + this.nodeValue + "\"";

        case Node.COMMENT_NODE:
            return "<!--" + this.nodeValue + "-->";
            
        case Node.DOCUMENT_TYPE_NODE:
            var docType = "<!DOCTYPE " + this.nodeName;
            if (this.publicId) {
                docType += " PUBLIC \"" + this.publicId + "\"";
                if (this.systemId)
                    docType += " \"" + this.systemId + "\"";
            } else if (this.systemId)
                docType += " SYSTEM \"" + this.systemId + "\"";
            if (this.internalSubset)
                docType += " [" + this.internalSubset + "]";
            return docType + ">";
    }

    return this.nodeName.toLowerCase().collapseWhitespace();
}

function isAncestorNode(ancestor, node)
{
    if (!node || !ancestor)
        return false;

    var currentNode = node.parentNode;
    while (currentNode) {
        if (ancestor === currentNode)
            return true;
        currentNode = currentNode.parentNode;
    }
    return false;
}

function isDescendantNode(descendant)
{
    return isAncestorNode(descendant, this);
}

function traverseNextNode(stayWithin)
{
    if (!this)
        return;

    var node = this.firstChild;
    if (node)
        return node;

    if (stayWithin && this === stayWithin)
        return null;

    node = this.nextSibling;
    if (node)
        return node;

    node = this;
    while (node && !node.nextSibling && (!stayWithin || !node.parentNode || node.parentNode !== stayWithin))
        node = node.parentNode;
    if (!node)
        return null;

    return node.nextSibling;
}

function traversePreviousNode(stayWithin)
{
    if (!this)
        return;
    if (stayWithin && this === stayWithin)
        return null;
    var node = this.previousSibling;
    while (node && node.lastChild)
        node = node.lastChild;
    if (node)
        return node;
    return this.parentNode;
}

function onlyTextChild()
{
    if (!this)
        return null;

    var firstChild = this.firstChild;
    if (!firstChild || firstChild.nodeType !== Node.TEXT_NODE)
        return null;

    var sibling = firstChild.nextSibling;
    return sibling ? null : firstChild;
}

function appropriateSelectorForNode(node, justSelector)
{
    if (!node)
        return "";

    var lowerCaseName = node.localName || node.nodeName.toLowerCase();

    var id = node.getAttribute("id");
    if (id) {
        var selector = "#" + id;
        return (justSelector ? selector : lowerCaseName + selector);
    }

    var className = node.getAttribute("class");
    if (className) {
        var selector = "." + className.replace(/\s+/, ".");
        return (justSelector ? selector : lowerCaseName + selector);
    }

    if (lowerCaseName === "input" && node.getAttribute("type"))
        return lowerCaseName + "[type=\"" + node.getAttribute("type") + "\"]";

    return lowerCaseName;
}

function getDocumentForNode(node)
{
    return node.nodeType == Node.DOCUMENT_NODE ? node : node.ownerDocument;
}

function parentNode(node)
{
    return node.parentNode;
}

Number.millisToString = function(ms, higherResolution)
{
    return Number.secondsToString(ms / 1000, higherResolution);
}

Number.secondsToString = function(seconds, higherResolution)
{
    if (seconds === 0)
        return "0";

    var ms = seconds * 1000;
    if (higherResolution && ms < 1000)
        return WebInspector.UIString("%.3fms", ms);
    else if (ms < 1000)
        return WebInspector.UIString("%.0fms", ms);

    if (seconds < 60)
        return WebInspector.UIString("%.2fs", seconds);

    var minutes = seconds / 60;
    if (minutes < 60)
        return WebInspector.UIString("%.1fmin", minutes);

    var hours = minutes / 60;
    if (hours < 24)
        return WebInspector.UIString("%.1fhrs", hours);

    var days = hours / 24;
    return WebInspector.UIString("%.1f days", days);
}

Number.bytesToString = function(bytes, higherResolution)
{
    if (typeof higherResolution === "undefined")
        higherResolution = true;

    if (bytes < 1024)
        return WebInspector.UIString("%.0fB", bytes);

    var kilobytes = bytes / 1024;
    if (higherResolution && kilobytes < 1024)
        return WebInspector.UIString("%.2fKB", kilobytes);
    else if (kilobytes < 1024)
        return WebInspector.UIString("%.0fKB", kilobytes);

    var megabytes = kilobytes / 1024;
    if (higherResolution)
        return WebInspector.UIString("%.2fMB", megabytes);
    else
        return WebInspector.UIString("%.0fMB", megabytes);
}

Number.constrain = function(num, min, max)
{
    if (num < min)
        num = min;
    else if (num > max)
        num = max;
    return num;
}

HTMLTextAreaElement.prototype.moveCursorToEnd = function()
{
    var length = this.value.length;
    this.setSelectionRange(length, length);
}

Object.defineProperty(Array.prototype, "remove", { value: function(value, onlyFirst)
{
    if (onlyFirst) {
        var index = this.indexOf(value);
        if (index !== -1)
            this.splice(index, 1);
        return;
    }

    var length = this.length;
    for (var i = 0; i < length; ++i) {
        if (this[i] === value)
            this.splice(i, 1);
    }
}});

Object.defineProperty(Array.prototype, "keySet", { value: function()
{
    var keys = {};
    for (var i = 0; i < this.length; ++i)
        keys[this[i]] = true;
    return keys;
}});

Object.defineProperty(Array.prototype, "upperBound", { value: function(value)
{
    var first = 0;
    var count = this.length;
    while (count > 0) {
      var step = count >> 1;
      var middle = first + step;
      if (value >= this[middle]) {
          first = middle + 1;
          count -= step + 1;
      } else
          count = step;
    }
    return first;
}});

Array.diff = function(left, right)
{
    var o = left;
    var n = right;

    var ns = {};
    var os = {};

    for (var i = 0; i < n.length; i++) {
        if (ns[n[i]] == null)
            ns[n[i]] = { rows: [], o: null };
        ns[n[i]].rows.push(i);
    }

    for (var i = 0; i < o.length; i++) {
        if (os[o[i]] == null)
            os[o[i]] = { rows: [], n: null };
        os[o[i]].rows.push(i);
    }

    for (var i in ns) {
        if (ns[i].rows.length == 1 && typeof(os[i]) != "undefined" && os[i].rows.length == 1) {
            n[ns[i].rows[0]] = { text: n[ns[i].rows[0]], row: os[i].rows[0] };
            o[os[i].rows[0]] = { text: o[os[i].rows[0]], row: ns[i].rows[0] };
        }
    }

    for (var i = 0; i < n.length - 1; i++) {
        if (n[i].text != null && n[i + 1].text == null && n[i].row + 1 < o.length && o[n[i].row + 1].text == null && n[i + 1] == o[n[i].row + 1]) {
            n[i + 1] = { text: n[i + 1], row: n[i].row + 1 };
            o[n[i].row + 1] = { text: o[n[i].row + 1], row: i + 1 };
        }
    }

    for (var i = n.length - 1; i > 0; i--) {
        if (n[i].text != null && n[i - 1].text == null && n[i].row > 0 && o[n[i].row - 1].text == null && 
            n[i - 1] == o[n[i].row - 1]) {
            n[i - 1] = { text: n[i - 1], row: n[i].row - 1 };
            o[n[i].row - 1] = { text: o[n[i].row - 1], row: i - 1 };
        }
    }

    return { left: o, right: n };
}

Array.convert = function(list)
{
    // Cast array-like object to an array.
    return Array.prototype.slice.call(list);
}

function insertionIndexForObjectInListSortedByFunction(anObject, aList, aFunction)
{
    var first = 0;
    var last = aList.length - 1;
    var floor = Math.floor;
    var mid, c;

    while (first <= last) {
        mid = floor((first + last) / 2);
        c = aFunction(anObject, aList[mid]);

        if (c > 0)
            first = mid + 1;
        else if (c < 0)
            last = mid - 1;
        else {
            // Return the first occurance of an item in the list.
            while (mid > 0 && aFunction(anObject, aList[mid - 1]) === 0)
                mid--;
            first = mid;
            break;
        }
    }

    return first;
}

String.sprintf = function(format)
{
    return String.vsprintf(format, Array.prototype.slice.call(arguments, 1));
}

String.tokenizeFormatString = function(format)
{
    var tokens = [];
    var substitutionIndex = 0;

    function addStringToken(str)
    {
        tokens.push({ type: "string", value: str });
    }

    function addSpecifierToken(specifier, precision, substitutionIndex)
    {
        tokens.push({ type: "specifier", specifier: specifier, precision: precision, substitutionIndex: substitutionIndex });
    }

    var index = 0;
    for (var precentIndex = format.indexOf("%", index); precentIndex !== -1; precentIndex = format.indexOf("%", index)) {
        addStringToken(format.substring(index, precentIndex));
        index = precentIndex + 1;

        if (format[index] === "%") {
            addStringToken("%");
            ++index;
            continue;
        }

        if (!isNaN(format[index])) {
            // The first character is a number, it might be a substitution index.
            var number = parseInt(format.substring(index));
            while (!isNaN(format[index]))
                ++index;
            // If the number is greater than zero and ends with a "$",
            // then this is a substitution index.
            if (number > 0 && format[index] === "$") {
                substitutionIndex = (number - 1);
                ++index;
            }
        }

        var precision = -1;
        if (format[index] === ".") {
            // This is a precision specifier. If no digit follows the ".",
            // then the precision should be zero.
            ++index;
            precision = parseInt(format.substring(index));
            if (isNaN(precision))
                precision = 0;
            while (!isNaN(format[index]))
                ++index;
        }

        addSpecifierToken(format[index], precision, substitutionIndex);

        ++substitutionIndex;
        ++index;
    }

    addStringToken(format.substring(index));

    return tokens;
}

String.standardFormatters = {
    d: function(substitution)
    {
        if (typeof substitution == "object" && WebInspector.RemoteObject.type(substitution) === "number")
            substitution = substitution.description;
        substitution = parseInt(substitution);
        return !isNaN(substitution) ? substitution : 0;
    },

    f: function(substitution, token)
    {
        if (typeof substitution == "object" && WebInspector.RemoteObject.type(substitution) === "number")
            substitution = substitution.description;
        substitution = parseFloat(substitution);
        if (substitution && token.precision > -1)
            substitution = substitution.toFixed(token.precision);
        return !isNaN(substitution) ? substitution : (token.precision > -1 ? Number(0).toFixed(token.precision) : 0);
    },

    s: function(substitution)
    {
        if (typeof substitution == "object" && WebInspector.RemoteObject.type(substitution) !== "null")
            substitution = substitution.description;
        return substitution;
    },
};

String.vsprintf = function(format, substitutions)
{
    return String.format(format, substitutions, String.standardFormatters, "", function(a, b) { return a + b; }).formattedResult;
}

String.format = function(format, substitutions, formatters, initialValue, append)
{
    if (!format || !substitutions || !substitutions.length)
        return { formattedResult: append(initialValue, format), unusedSubstitutions: substitutions };

    function prettyFunctionName()
    {
        return "String.format(\"" + format + "\", \"" + substitutions.join("\", \"") + "\")";
    }

    function warn(msg)
    {
        console.warn(prettyFunctionName() + ": " + msg);
    }

    function error(msg)
    {
        console.error(prettyFunctionName() + ": " + msg);
    }

    var result = initialValue;
    var tokens = String.tokenizeFormatString(format);
    var usedSubstitutionIndexes = {};

    for (var i = 0; i < tokens.length; ++i) {
        var token = tokens[i];

        if (token.type === "string") {
            result = append(result, token.value);
            continue;
        }

        if (token.type !== "specifier") {
            error("Unknown token type \"" + token.type + "\" found.");
            continue;
        }

        if (token.substitutionIndex >= substitutions.length) {
            // If there are not enough substitutions for the current substitutionIndex
            // just output the format specifier literally and move on.
            error("not enough substitution arguments. Had " + substitutions.length + " but needed " + (token.substitutionIndex + 1) + ", so substitution was skipped.");
            result = append(result, "%" + (token.precision > -1 ? token.precision : "") + token.specifier);
            continue;
        }

        usedSubstitutionIndexes[token.substitutionIndex] = true;

        if (!(token.specifier in formatters)) {
            // Encountered an unsupported format character, treat as a string.
            warn("unsupported format character \u201C" + token.specifier + "\u201D. Treating as a string.");
            result = append(result, substitutions[token.substitutionIndex]);
            continue;
        }

        result = append(result, formatters[token.specifier](substitutions[token.substitutionIndex], token));
    }

    var unusedSubstitutions = [];
    for (var i = 0; i < substitutions.length; ++i) {
        if (i in usedSubstitutionIndexes)
            continue;
        unusedSubstitutions.push(substitutions[i]);
    }

    return { formattedResult: result, unusedSubstitutions: unusedSubstitutions };
}

function isEnterKey(event) {
    // Check if in IME.
    return (event.keyCode !== 229 && event.keyIdentifier === "Enter") || event.keyCode == 13 || event.charCode == 13;
}


function highlightSearchResult(element, offset, length)
{
    var lineText = element.textContent;
    var endOffset = offset + length;
    var highlightNode = document.createElement("span");
    highlightNode.className = "webkit-search-result";
    highlightNode.textContent = lineText.substring(offset, endOffset);

    var boundary = element.rangeBoundaryForOffset(offset);
    var textNode = boundary.container;
    var text = textNode.textContent;

    if (boundary.offset + length < text.length) {
        // Selection belong to a single split mode.
        textNode.textContent = text.substring(boundary.offset + length);
        textNode.parentElement.insertBefore(highlightNode, textNode);
        var prefixNode = document.createTextNode(text.substring(0, boundary.offset));
        textNode.parentElement.insertBefore(prefixNode, highlightNode);
        return highlightNode;
    }

    var parentElement = textNode.parentElement;
    var anchorElement = textNode.nextSibling;

    length -= text.length - boundary.offset;
    textNode.textContent = text.substring(0, boundary.offset);
    textNode = textNode.traverseNextTextNode(element);

    while (textNode) {
        var text = textNode.textContent;
        if (length < text.length) {
            textNode.textContent = text.substring(length);
            break;
        }

        length -= text.length;
        textNode.textContent = "";
        textNode = textNode.traverseNextTextNode(element);
    }

    parentElement.insertBefore(highlightNode, anchorElement);
    return highlightNode;
}

function createSearchRegex(query)
{
    var regex = "";
    for (var i = 0; i < query.length; ++i) {
        var char = query.charAt(i);
        if (char === "]")
            char = "\\]";
        regex += "[" + char + "]";
    }
    return new RegExp(regex, "i");
}

function offerFileForDownload(contents)
{
    var builder = new BlobBuilder();
    builder.append(contents);
    var blob = builder.getBlob("application/octet-stream");
    var url = window.webkitURL.createObjectURL(blob);
    window.open(url);
}
