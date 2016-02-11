import { isPresent } from 'angular2/src/facade/lang';
import { DebugNode, DebugElement, EventListener, getDebugNode, indexDebugNode, removeDebugNodeFromIndex } from 'angular2/src/core/debug/debug_node';
export class DebugDomRootRenderer {
    constructor(_delegate) {
        this._delegate = _delegate;
    }
    renderComponent(componentProto) {
        return new DebugDomRenderer(this, this._delegate.renderComponent(componentProto));
    }
}
export class DebugDomRenderer {
    constructor(_rootRenderer, _delegate) {
        this._rootRenderer = _rootRenderer;
        this._delegate = _delegate;
    }
    renderComponent(componentType) {
        return this._rootRenderer.renderComponent(componentType);
    }
    selectRootElement(selector) {
        var nativeEl = this._delegate.selectRootElement(selector);
        var debugEl = new DebugElement(nativeEl, null);
        indexDebugNode(debugEl);
        return nativeEl;
    }
    createElement(parentElement, name) {
        var nativeEl = this._delegate.createElement(parentElement, name);
        var debugEl = new DebugElement(nativeEl, getDebugNode(parentElement));
        debugEl.name = name;
        indexDebugNode(debugEl);
        return nativeEl;
    }
    createViewRoot(hostElement) { return this._delegate.createViewRoot(hostElement); }
    createTemplateAnchor(parentElement) {
        var comment = this._delegate.createTemplateAnchor(parentElement);
        var debugEl = new DebugNode(comment, getDebugNode(parentElement));
        indexDebugNode(debugEl);
        return comment;
    }
    createText(parentElement, value) {
        var text = this._delegate.createText(parentElement, value);
        var debugEl = new DebugNode(text, getDebugNode(parentElement));
        indexDebugNode(debugEl);
        return text;
    }
    projectNodes(parentElement, nodes) {
        var debugParent = getDebugNode(parentElement);
        if (isPresent(debugParent) && debugParent instanceof DebugElement) {
            nodes.forEach((node) => { debugParent.addChild(getDebugNode(node)); });
        }
        return this._delegate.projectNodes(parentElement, nodes);
    }
    attachViewAfter(node, viewRootNodes) {
        var debugNode = getDebugNode(node);
        if (isPresent(debugNode)) {
            var debugParent = debugNode.parent;
            if (viewRootNodes.length > 0 && isPresent(debugParent)) {
                var debugViewRootNodes = [];
                viewRootNodes.forEach((rootNode) => debugViewRootNodes.push(getDebugNode(rootNode)));
                debugParent.insertChildrenAfter(debugNode, debugViewRootNodes);
            }
        }
        return this._delegate.attachViewAfter(node, viewRootNodes);
    }
    detachView(viewRootNodes) {
        viewRootNodes.forEach((node) => {
            var debugNode = getDebugNode(node);
            if (isPresent(debugNode) && isPresent(debugNode.parent)) {
                debugNode.parent.removeChild(debugNode);
            }
        });
        return this._delegate.detachView(viewRootNodes);
    }
    destroyView(hostElement, viewAllNodes) {
        viewAllNodes.forEach((node) => { removeDebugNodeFromIndex(getDebugNode(node)); });
        return this._delegate.destroyView(hostElement, viewAllNodes);
    }
    listen(renderElement, name, callback) {
        var debugEl = getDebugNode(renderElement);
        if (isPresent(debugEl)) {
            debugEl.listeners.push(new EventListener(name, callback));
        }
        return this._delegate.listen(renderElement, name, callback);
    }
    listenGlobal(target, name, callback) {
        return this._delegate.listenGlobal(target, name, callback);
    }
    setElementProperty(renderElement, propertyName, propertyValue) {
        var debugEl = getDebugNode(renderElement);
        if (isPresent(debugEl) && debugEl instanceof DebugElement) {
            debugEl.properties.set(propertyName, propertyValue);
        }
        return this._delegate.setElementProperty(renderElement, propertyName, propertyValue);
    }
    setElementAttribute(renderElement, attributeName, attributeValue) {
        var debugEl = getDebugNode(renderElement);
        if (isPresent(debugEl) && debugEl instanceof DebugElement) {
            debugEl.attributes.set(attributeName, attributeValue);
        }
        return this._delegate.setElementAttribute(renderElement, attributeName, attributeValue);
    }
    /**
     * Used only in debug mode to serialize property changes to comment nodes,
     * such as <template> placeholders.
     */
    setBindingDebugInfo(renderElement, propertyName, propertyValue) {
        return this._delegate.setBindingDebugInfo(renderElement, propertyName, propertyValue);
    }
    /**
     * Used only in development mode to set information needed by the DebugNode for this element.
     */
    setElementDebugInfo(renderElement, info) {
        var debugEl = getDebugNode(renderElement);
        debugEl.setDebugInfo(info);
        return this._delegate.setElementDebugInfo(renderElement, info);
    }
    setElementClass(renderElement, className, isAdd) {
        return this._delegate.setElementClass(renderElement, className, isAdd);
    }
    setElementStyle(renderElement, styleName, styleValue) {
        return this._delegate.setElementStyle(renderElement, styleName, styleValue);
    }
    invokeElementMethod(renderElement, methodName, args) {
        return this._delegate.invokeElementMethod(renderElement, methodName, args);
    }
    setText(renderNode, text) { return this._delegate.setText(renderNode, text); }
}
