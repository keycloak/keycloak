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
 */

function TreeOutline(listNode)
{
    this.children = [];
    this.selectedTreeElement = null;
    this._childrenListNode = listNode;
    this._childrenListNode.removeChildren();
    this._knownTreeElements = [];
    this._treeElementsExpandedState = [];
    this.expandTreeElementsWhenArrowing = false;
    this.root = true;
    this.hasChildren = false;
    this.expanded = true;
    this.selected = false;
    this.treeOutline = this;

    this._childrenListNode.tabIndex = 0;
    this._childrenListNode.addEventListener("keydown", this._treeKeyDown.bind(this), true);
}

TreeOutline._knownTreeElementNextIdentifier = 1;

TreeOutline._appendChild = function(child)
{
    if (!child)
        throw("child can't be undefined or null");

    var lastChild = this.children[this.children.length - 1];
    if (lastChild) {
        lastChild.nextSibling = child;
        child.previousSibling = lastChild;
    } else {
        child.previousSibling = null;
        child.nextSibling = null;
    }

    this.children.push(child);
    this.hasChildren = true;
    child.parent = this;
    child.treeOutline = this.treeOutline;
    child.treeOutline._rememberTreeElement(child);

    var current = child.children[0];
    while (current) {
        current.treeOutline = this.treeOutline;
        current.treeOutline._rememberTreeElement(current);
        current = current.traverseNextTreeElement(false, child, true);
    }

    if (child.hasChildren && child.treeOutline._treeElementsExpandedState[child.identifier] !== undefined)
        child.expanded = child.treeOutline._treeElementsExpandedState[child.identifier];

    if (!this._childrenListNode) {
        this._childrenListNode = this.treeOutline._childrenListNode.ownerDocument.createElement("ol");
        this._childrenListNode.parentTreeElement = this;
        this._childrenListNode.addStyleClass("children");
        if (this.hidden)
            this._childrenListNode.addStyleClass("hidden");
    }

    child._attach();
}

TreeOutline._insertChild = function(child, index)
{
    if (!child)
        throw("child can't be undefined or null");

    var previousChild = (index > 0 ? this.children[index - 1] : null);
    if (previousChild) {
        previousChild.nextSibling = child;
        child.previousSibling = previousChild;
    } else {
        child.previousSibling = null;
    }

    var nextChild = this.children[index];
    if (nextChild) {
        nextChild.previousSibling = child;
        child.nextSibling = nextChild;
    } else {
        child.nextSibling = null;
    }

    this.children.splice(index, 0, child);
    this.hasChildren = true;
    child.parent = this;
    child.treeOutline = this.treeOutline;
    child.treeOutline._rememberTreeElement(child);

    var current = child.children[0];
    while (current) {
        current.treeOutline = this.treeOutline;
        current.treeOutline._rememberTreeElement(current);
        current = current.traverseNextTreeElement(false, child, true);
    }

    if (child.hasChildren && child.treeOutline._treeElementsExpandedState[child.identifier] !== undefined)
        child.expanded = child.treeOutline._treeElementsExpandedState[child.identifier];

    if (!this._childrenListNode) {
        this._childrenListNode = this.treeOutline._childrenListNode.ownerDocument.createElement("ol");
        this._childrenListNode.parentTreeElement = this;
        this._childrenListNode.addStyleClass("children");
        if (this.hidden)
            this._childrenListNode.addStyleClass("hidden");
    }

    child._attach();
}

TreeOutline._removeChildAtIndex = function(childIndex)
{
    if (childIndex < 0 || childIndex >= this.children.length)
        throw("childIndex out of range");

    var child = this.children[childIndex];
    this.children.splice(childIndex, 1);

    var parent = child.parent;
    if (child.deselect()) {
        if (child.previousSibling)
            child.previousSibling.select();
        else if (child.nextSibling)
            child.nextSibling.select();
        else
            parent.select();
    }

    if (child.previousSibling)
        child.previousSibling.nextSibling = child.nextSibling;
    if (child.nextSibling)
        child.nextSibling.previousSibling = child.previousSibling;

    if (child.treeOutline) {
        child.treeOutline._forgetTreeElement(child);
        child.treeOutline._forgetChildrenRecursive(child);
    }

    child._detach();
    child.treeOutline = null;
    child.parent = null;
    child.nextSibling = null;
    child.previousSibling = null;
}

TreeOutline._removeChild = function(child)
{
    if (!child)
        throw("child can't be undefined or null");

    var childIndex = this.children.indexOf(child);
    if (childIndex === -1)
        throw("child not found in this node's children");

    TreeOutline._removeChildAtIndex.call(this, childIndex);
}

TreeOutline._removeChildren = function()
{
    for (var i = 0; i < this.children.length; ++i) {
        var child = this.children[i];
        child.deselect();

        if (child.treeOutline) {
            child.treeOutline._forgetTreeElement(child);
            child.treeOutline._forgetChildrenRecursive(child);
        }

        child._detach();
        child.treeOutline = null;
        child.parent = null;
        child.nextSibling = null;
        child.previousSibling = null;
    }

    this.children = [];
}

TreeOutline._removeChildrenRecursive = function()
{
    var childrenToRemove = this.children;

    var child = this.children[0];
    while (child) {
        if (child.children.length)
            childrenToRemove = childrenToRemove.concat(child.children);
        child = child.traverseNextTreeElement(false, this, true);
    }

    for (var i = 0; i < childrenToRemove.length; ++i) {
        var child = childrenToRemove[i];
        child.deselect();
        if (child.treeOutline)
            child.treeOutline._forgetTreeElement(child);
        child._detach();
        child.children = [];
        child.treeOutline = null;
        child.parent = null;
        child.nextSibling = null;
        child.previousSibling = null;
    }

    this.children = [];
}

TreeOutline.prototype._rememberTreeElement = function(element)
{
    if (!this._knownTreeElements[element.identifier])
        this._knownTreeElements[element.identifier] = [];

    // check if the element is already known
    var elements = this._knownTreeElements[element.identifier];
    if (elements.indexOf(element) !== -1)
        return;

    // add the element
    elements.push(element);
}

TreeOutline.prototype._forgetTreeElement = function(element)
{
    if (this._knownTreeElements[element.identifier])
        this._knownTreeElements[element.identifier].remove(element, true);
}

TreeOutline.prototype._forgetChildrenRecursive = function(parentElement)
{
    var child = parentElement.children[0];
    while (child) {
        this._forgetTreeElement(child);
        child = child.traverseNextTreeElement(false, this, true);
    }
}

TreeOutline.prototype.getCachedTreeElement = function(representedObject)
{
    if (!representedObject)
        return null;

    if ("__treeElementIdentifier" in representedObject) {
        // If this representedObject has a tree element identifier, and it is a known TreeElement
        // in our tree we can just return that tree element.
        var elements = this._knownTreeElements[representedObject.__treeElementIdentifier];
        if (elements) {
            for (var i = 0; i < elements.length; ++i)
                if (elements[i].representedObject === representedObject)
                    return elements[i];
        }
    }
    return null;
}

TreeOutline.prototype.findTreeElement = function(representedObject, isAncestor, getParent)
{
    if (!representedObject)
        return null;

    var cachedElement = this.getCachedTreeElement(representedObject);
    if (cachedElement)
        return cachedElement;

    // The representedObject isn't known, so we start at the top of the tree and work down to find the first
    // tree element that represents representedObject or one of its ancestors.
    var item;
    var found = false;
    for (var i = 0; i < this.children.length; ++i) {
        item = this.children[i];
        if (item.representedObject === representedObject || isAncestor(item.representedObject, representedObject)) {
            found = true;
            break;
        }
    }

    if (!found)
        return null;

    // Make sure the item that we found is connected to the root of the tree.
    // Build up a list of representedObject's ancestors that aren't already in our tree.
    var ancestors = [];
    var currentObject = representedObject;
    while (currentObject) {
        ancestors.unshift(currentObject);
        if (currentObject === item.representedObject)
            break;
        currentObject = getParent(currentObject);
    }

    // For each of those ancestors we populate them to fill in the tree.
    for (var i = 0; i < ancestors.length; ++i) {
        // Make sure we don't call findTreeElement with the same representedObject
        // again, to prevent infinite recursion.
        if (ancestors[i] === representedObject)
            continue;
        // FIXME: we could do something faster than findTreeElement since we will know the next
        // ancestor exists in the tree.
        item = this.findTreeElement(ancestors[i], isAncestor, getParent);
        if (item && item.onpopulate)
            item.onpopulate(item);
    }

    return this.getCachedTreeElement(representedObject);
}

TreeOutline.prototype.treeElementFromPoint = function(x, y)
{
    var node = this._childrenListNode.ownerDocument.elementFromPoint(x, y);
    var listNode = node.enclosingNodeOrSelfWithNodeNameInArray(["ol", "li"]);
    if (listNode)
        return listNode.parentTreeElement || listNode.treeElement;
    return null;
}

TreeOutline.prototype._treeKeyDown = function(event)
{
    if (event.target !== this._childrenListNode)
        return;

    if (!this.selectedTreeElement || event.shiftKey || event.metaKey || event.ctrlKey)
        return;

    var handled = false;
    var nextSelectedElement;
    if (event.keyIdentifier === "Up" && !event.altKey) {
        nextSelectedElement = this.selectedTreeElement.traversePreviousTreeElement(true);
        while (nextSelectedElement && !nextSelectedElement.selectable)
            nextSelectedElement = nextSelectedElement.traversePreviousTreeElement(!this.expandTreeElementsWhenArrowing);
        handled = nextSelectedElement ? true : false;
    } else if (event.keyIdentifier === "Down" && !event.altKey) {
        nextSelectedElement = this.selectedTreeElement.traverseNextTreeElement(true);
        while (nextSelectedElement && !nextSelectedElement.selectable)
            nextSelectedElement = nextSelectedElement.traverseNextTreeElement(!this.expandTreeElementsWhenArrowing);
        handled = nextSelectedElement ? true : false;
    } else if (event.keyIdentifier === "Left") {
        if (this.selectedTreeElement.expanded) {
            if (event.altKey)
                this.selectedTreeElement.collapseRecursively();
            else
                this.selectedTreeElement.collapse();
            handled = true;
        } else if (this.selectedTreeElement.parent && !this.selectedTreeElement.parent.root) {
            handled = true;
            if (this.selectedTreeElement.parent.selectable) {
                nextSelectedElement = this.selectedTreeElement.parent;
                handled = nextSelectedElement ? true : false;
            } else if (this.selectedTreeElement.parent)
                this.selectedTreeElement.parent.collapse();
        }
    } else if (event.keyIdentifier === "Right") {
        if (!this.selectedTreeElement.revealed()) {
            this.selectedTreeElement.reveal();
            handled = true;
        } else if (this.selectedTreeElement.hasChildren) {
            handled = true;
            if (this.selectedTreeElement.expanded) {
                nextSelectedElement = this.selectedTreeElement.children[0];
                handled = nextSelectedElement ? true : false;
            } else {
                if (event.altKey)
                    this.selectedTreeElement.expandRecursively();
                else
                    this.selectedTreeElement.expand();
            }
        }
    } else if (event.keyCode === WebInspector.KeyboardShortcut.Keys.Backspace.code || event.keyCode === WebInspector.KeyboardShortcut.Keys.Delete.code) {
        if (this.selectedTreeElement.ondelete)
            handled = this.selectedTreeElement.ondelete();
    } else if (isEnterKey(event)) {
        if (this.selectedTreeElement.onenter)
            handled = this.selectedTreeElement.onenter();
    }

    if (nextSelectedElement) {
        nextSelectedElement.reveal();
        nextSelectedElement.select(false, true);
    }

    if (handled) {
        event.preventDefault();
        event.stopPropagation();
    }
}

TreeOutline.prototype.expand = function()
{
    // this is the root, do nothing
}

TreeOutline.prototype.collapse = function()
{
    // this is the root, do nothing
}

TreeOutline.prototype.revealed = function()
{
    return true;
}

TreeOutline.prototype.reveal = function()
{
    // this is the root, do nothing
}

TreeOutline.prototype.select = function()
{
    // this is the root, do nothing
}

TreeOutline.prototype.appendChild = TreeOutline._appendChild;
TreeOutline.prototype.insertChild = TreeOutline._insertChild;
TreeOutline.prototype.removeChild = TreeOutline._removeChild;
TreeOutline.prototype.removeChildAtIndex = TreeOutline._removeChildAtIndex;
TreeOutline.prototype.removeChildren = TreeOutline._removeChildren;
TreeOutline.prototype.removeChildrenRecursive = TreeOutline._removeChildrenRecursive;

function TreeElement(title, representedObject, hasChildren)
{
    this._title = title;
    this.representedObject = (representedObject || {});

    if (this.representedObject.__treeElementIdentifier)
        this.identifier = this.representedObject.__treeElementIdentifier;
    else {
        this.identifier = TreeOutline._knownTreeElementNextIdentifier++;
        this.representedObject.__treeElementIdentifier = this.identifier;
    }

    this._hidden = false;
    this.expanded = false;
    this.selected = false;
    this.hasChildren = hasChildren;
    this.children = [];
    this.treeOutline = null;
    this.parent = null;
    this.previousSibling = null;
    this.nextSibling = null;
    this._listItemNode = null;
}

TreeElement.prototype = {
    selectable: true,
    arrowToggleWidth: 10,

    get listItemElement() {
        return this._listItemNode;
    },

    get childrenListElement() {
        return this._childrenListNode;
    },

    get title() {
        return this._title;
    },

    set title(x) {
        this._title = x;
        this._setListItemNodeContent();
    },

    get titleHTML() {
        return this._titleHTML;
    },

    set titleHTML(x) {
        this._titleHTML = x;
        this._setListItemNodeContent();
    },

    get tooltip() {
        return this._tooltip;
    },

    set tooltip(x) {
        this._tooltip = x;
        if (this._listItemNode)
            this._listItemNode.title = x ? x : "";
    },

    get hasChildren() {
        return this._hasChildren;
    },

    set hasChildren(x) {
        if (this._hasChildren === x)
            return;

        this._hasChildren = x;

        if (!this._listItemNode)
            return;

        if (x)
            this._listItemNode.addStyleClass("parent");
        else {
            this._listItemNode.removeStyleClass("parent");
            this.collapse();
        }
    },

    get hidden() {
        return this._hidden;
    },

    set hidden(x) {
        if (this._hidden === x)
            return;

        this._hidden = x;

        if (x) {
            if (this._listItemNode)
                this._listItemNode.addStyleClass("hidden");
            if (this._childrenListNode)
                this._childrenListNode.addStyleClass("hidden");
        } else {
            if (this._listItemNode)
                this._listItemNode.removeStyleClass("hidden");
            if (this._childrenListNode)
                this._childrenListNode.removeStyleClass("hidden");
        }
    },

    get shouldRefreshChildren() {
        return this._shouldRefreshChildren;
    },

    set shouldRefreshChildren(x) {
        this._shouldRefreshChildren = x;
        if (x && this.expanded)
            this.expand();
    },

    _setListItemNodeContent: function()
    {
        if (!this._listItemNode)
            return;

        if (!this._titleHTML && !this._title)
            this._listItemNode.removeChildren();
        else if (typeof this._titleHTML === "string")
            this._listItemNode.innerHTML = this._titleHTML;
        else if (typeof this._title === "string")
            this._listItemNode.textContent = this._title;
        else {
            this._listItemNode.removeChildren();
            if (this._title.parentNode)
                this._title.parentNode.removeChild(this._title);
            this._listItemNode.appendChild(this._title);
        }
    }
}

TreeElement.prototype.appendChild = TreeOutline._appendChild;
TreeElement.prototype.insertChild = TreeOutline._insertChild;
TreeElement.prototype.removeChild = TreeOutline._removeChild;
TreeElement.prototype.removeChildAtIndex = TreeOutline._removeChildAtIndex;
TreeElement.prototype.removeChildren = TreeOutline._removeChildren;
TreeElement.prototype.removeChildrenRecursive = TreeOutline._removeChildrenRecursive;

TreeElement.prototype._attach = function()
{
    if (!this._listItemNode || this.parent._shouldRefreshChildren) {
        if (this._listItemNode && this._listItemNode.parentNode)
            this._listItemNode.parentNode.removeChild(this._listItemNode);

        this._listItemNode = this.treeOutline._childrenListNode.ownerDocument.createElement("li");
        this._listItemNode.treeElement = this;
        this._setListItemNodeContent();
        this._listItemNode.title = this._tooltip ? this._tooltip : "";

        if (this.hidden)
            this._listItemNode.addStyleClass("hidden");
        if (this.hasChildren)
            this._listItemNode.addStyleClass("parent");
        if (this.expanded)
            this._listItemNode.addStyleClass("expanded");
        if (this.selected)
            this._listItemNode.addStyleClass("selected");

        this._listItemNode.addEventListener("mousedown", TreeElement.treeElementMouseDown, false);
        this._listItemNode.addEventListener("click", TreeElement.treeElementToggled, false);
        this._listItemNode.addEventListener("dblclick", TreeElement.treeElementDoubleClicked, false);

        if (this.onattach)
            this.onattach(this);
    }

    var nextSibling = null;
    if (this.nextSibling && this.nextSibling._listItemNode && this.nextSibling._listItemNode.parentNode === this.parent._childrenListNode)
        nextSibling = this.nextSibling._listItemNode;
    this.parent._childrenListNode.insertBefore(this._listItemNode, nextSibling);
    if (this._childrenListNode)
        this.parent._childrenListNode.insertBefore(this._childrenListNode, this._listItemNode.nextSibling);
    if (this.selected)
        this.select();
    if (this.expanded)
        this.expand();
}

TreeElement.prototype._detach = function()
{
    if (this._listItemNode && this._listItemNode.parentNode)
        this._listItemNode.parentNode.removeChild(this._listItemNode);
    if (this._childrenListNode && this._childrenListNode.parentNode)
        this._childrenListNode.parentNode.removeChild(this._childrenListNode);
}

TreeElement.treeElementMouseDown = function(event)
{
    var element = event.currentTarget;
    if (!element || !element.treeElement || !element.treeElement.selectable)
        return;

    if (element.treeElement.isEventWithinDisclosureTriangle(event))
        return;

    element.treeElement.selectOnMouseDown(event);
}

TreeElement.treeElementToggled = function(event)
{
    var element = event.currentTarget;
    if (!element || !element.treeElement)
        return;

    if (!element.treeElement.isEventWithinDisclosureTriangle(event))
        return;

    if (element.treeElement.expanded) {
        if (event.altKey)
            element.treeElement.collapseRecursively();
        else
            element.treeElement.collapse();
    } else {
        if (event.altKey)
            element.treeElement.expandRecursively();
        else
            element.treeElement.expand();
    }
    event.stopPropagation();
}

TreeElement.treeElementDoubleClicked = function(event)
{
    var element = event.currentTarget;
    if (!element || !element.treeElement)
        return;

    if (element.treeElement.ondblclick)
        element.treeElement.ondblclick.call(element.treeElement, event);
    else if (element.treeElement.hasChildren && !element.treeElement.expanded)
        element.treeElement.expand();
}

TreeElement.prototype.collapse = function()
{
    if (this._listItemNode)
        this._listItemNode.removeStyleClass("expanded");
    if (this._childrenListNode)
        this._childrenListNode.removeStyleClass("expanded");

    this.expanded = false;
    if (this.treeOutline)
        this.treeOutline._treeElementsExpandedState[this.identifier] = true;

    if (this.oncollapse)
        this.oncollapse(this);
}

TreeElement.prototype.collapseRecursively = function()
{
    var item = this;
    while (item) {
        if (item.expanded)
            item.collapse();
        item = item.traverseNextTreeElement(false, this, true);
    }
}

TreeElement.prototype.expand = function()
{
    if (!this.hasChildren || (this.expanded && !this._shouldRefreshChildren && this._childrenListNode))
        return;

    if (this.treeOutline && (!this._childrenListNode || this._shouldRefreshChildren)) {
        if (this._childrenListNode && this._childrenListNode.parentNode)
            this._childrenListNode.parentNode.removeChild(this._childrenListNode);

        this._childrenListNode = this.treeOutline._childrenListNode.ownerDocument.createElement("ol");
        this._childrenListNode.parentTreeElement = this;
        this._childrenListNode.addStyleClass("children");

        if (this.hidden)
            this._childrenListNode.addStyleClass("hidden");

        if (this.onpopulate)
            this.onpopulate(this);

        for (var i = 0; i < this.children.length; ++i)
            this.children[i]._attach();

        delete this._shouldRefreshChildren;
    }

    if (this._listItemNode) {
        this._listItemNode.addStyleClass("expanded");
        if (this._childrenListNode && this._childrenListNode.parentNode != this._listItemNode.parentNode)
            this.parent._childrenListNode.insertBefore(this._childrenListNode, this._listItemNode.nextSibling);
    }

    if (this._childrenListNode)
        this._childrenListNode.addStyleClass("expanded");

    this.expanded = true;
    if (this.treeOutline)
        this.treeOutline._treeElementsExpandedState[this.identifier] = true;

    if (this.onexpand)
        this.onexpand(this);
}

TreeElement.prototype.expandRecursively = function(maxDepth)
{
    var item = this;
    var info = {};
    var depth = 0;

    // The Inspector uses TreeOutlines to represents object properties, so recursive expansion
    // in some case can be infinite, since JavaScript objects can hold circular references.
    // So default to a recursion cap of 3 levels, since that gives fairly good results.
    if (typeof maxDepth === "undefined" || typeof maxDepth === "null")
        maxDepth = 3;

    while (item) {
        if (depth < maxDepth)
            item.expand();
        item = item.traverseNextTreeElement(false, this, (depth >= maxDepth), info);
        depth += info.depthChange;
    }
}

TreeElement.prototype.hasAncestor = function(ancestor) {
    if (!ancestor)
        return false;

    var currentNode = this.parent;
    while (currentNode) {
        if (ancestor === currentNode)
            return true;
        currentNode = currentNode.parent;
    }

    return false;
}

TreeElement.prototype.reveal = function()
{
    var currentAncestor = this.parent;
    while (currentAncestor && !currentAncestor.root) {
        if (!currentAncestor.expanded)
            currentAncestor.expand();
        currentAncestor = currentAncestor.parent;
    }

    if (this.onreveal)
        this.onreveal(this);
}

TreeElement.prototype.revealed = function()
{
    var currentAncestor = this.parent;
    while (currentAncestor && !currentAncestor.root) {
        if (!currentAncestor.expanded)
            return false;
        currentAncestor = currentAncestor.parent;
    }

    return true;
}

TreeElement.prototype.selectOnMouseDown = function(event)
{
    this.select(false, true);
}

TreeElement.prototype.select = function(supressOnSelect, selectedByUser)
{
    if (!this.treeOutline || !this.selectable || this.selected)
        return;

    if (this.treeOutline.selectedTreeElement)
        this.treeOutline.selectedTreeElement.deselect();

    this.selected = true;
    this.treeOutline._childrenListNode.focus();

    // Focusing on another node may detach "this" from tree.
    if (!this.treeOutline)
        return;
    this.treeOutline.selectedTreeElement = this;
    if (this._listItemNode)
        this._listItemNode.addStyleClass("selected");

    if (this.onselect && !supressOnSelect)
        this.onselect(this, selectedByUser);
}

TreeElement.prototype.deselect = function(supressOnDeselect)
{
    if (!this.treeOutline || this.treeOutline.selectedTreeElement !== this || !this.selected)
        return false;

    this.selected = false;
    this.treeOutline.selectedTreeElement = null;
    if (this._listItemNode)
        this._listItemNode.removeStyleClass("selected");

    if (this.ondeselect && !supressOnDeselect)
        this.ondeselect(this);
    return true;
}

TreeElement.prototype.traverseNextTreeElement = function(skipHidden, stayWithin, dontPopulate, info)
{
    if (!dontPopulate && this.hasChildren && this.onpopulate)
        this.onpopulate(this);

    if (info)
        info.depthChange = 0;

    var element = skipHidden ? (this.revealed() ? this.children[0] : null) : this.children[0];
    if (element && (!skipHidden || (skipHidden && this.expanded))) {
        if (info)
            info.depthChange = 1;
        return element;
    }

    if (this === stayWithin)
        return null;

    element = skipHidden ? (this.revealed() ? this.nextSibling : null) : this.nextSibling;
    if (element)
        return element;

    element = this;
    while (element && !element.root && !(skipHidden ? (element.revealed() ? element.nextSibling : null) : element.nextSibling) && element.parent !== stayWithin) {
        if (info)
            info.depthChange -= 1;
        element = element.parent;
    }

    if (!element)
        return null;

    return (skipHidden ? (element.revealed() ? element.nextSibling : null) : element.nextSibling);
}

TreeElement.prototype.traversePreviousTreeElement = function(skipHidden, dontPopulate)
{
    var element = skipHidden ? (this.revealed() ? this.previousSibling : null) : this.previousSibling;
    if (!dontPopulate && element && element.hasChildren && element.onpopulate)
        element.onpopulate(element);

    while (element && (skipHidden ? (element.revealed() && element.expanded ? element.children[element.children.length - 1] : null) : element.children[element.children.length - 1])) {
        if (!dontPopulate && element.hasChildren && element.onpopulate)
            element.onpopulate(element);
        element = (skipHidden ? (element.revealed() && element.expanded ? element.children[element.children.length - 1] : null) : element.children[element.children.length - 1]);
    }

    if (element)
        return element;

    if (!this.parent || this.parent.root)
        return null;

    return this.parent;
}

TreeElement.prototype.isEventWithinDisclosureTriangle = function(event)
{
    var left = this._listItemNode.totalOffsetLeft;
    return event.pageX >= left && event.pageX <= left + this.arrowToggleWidth && this.hasChildren;
}
