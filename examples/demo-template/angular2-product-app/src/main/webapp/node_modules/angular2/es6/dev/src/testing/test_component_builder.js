var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
import { DirectiveResolver, DynamicComponentLoader, Injector, Injectable, ViewResolver } from 'angular2/core';
import { isPresent } from 'angular2/src/facade/lang';
import { MapWrapper } from 'angular2/src/facade/collection';
import { el } from './utils';
import { DOCUMENT } from 'angular2/src/platform/dom/dom_tokens';
import { DOM } from 'angular2/src/platform/dom/dom_adapter';
import { getDebugNode } from 'angular2/src/core/debug/debug_node';
/**
 * Fixture for debugging and testing a component.
 */
export class ComponentFixture {
}
export class ComponentFixture_ extends ComponentFixture {
    constructor(componentRef) {
        super();
        this._componentParentView = componentRef.hostView.internalView;
        this.elementRef = this._componentParentView.appElements[0].ref;
        this.debugElement = getDebugNode(this._componentParentView.rootNodesOrAppElements[0].nativeElement);
        this.componentInstance = this.debugElement.componentInstance;
        this.nativeElement = this.debugElement.nativeElement;
        this._componentRef = componentRef;
    }
    detectChanges() {
        this._componentParentView.changeDetector.detectChanges();
        this._componentParentView.changeDetector.checkNoChanges();
    }
    destroy() { this._componentRef.dispose(); }
}
var _nextRootElementId = 0;
/**
 * Builds a ComponentFixture for use in component level tests.
 */
export let TestComponentBuilder = class {
    constructor(_injector) {
        this._injector = _injector;
        /** @internal */
        this._bindingsOverrides = new Map();
        /** @internal */
        this._directiveOverrides = new Map();
        /** @internal */
        this._templateOverrides = new Map();
        /** @internal */
        this._viewBindingsOverrides = new Map();
        /** @internal */
        this._viewOverrides = new Map();
    }
    /** @internal */
    _clone() {
        var clone = new TestComponentBuilder(this._injector);
        clone._viewOverrides = MapWrapper.clone(this._viewOverrides);
        clone._directiveOverrides = MapWrapper.clone(this._directiveOverrides);
        clone._templateOverrides = MapWrapper.clone(this._templateOverrides);
        return clone;
    }
    /**
     * Overrides only the html of a {@link ComponentMetadata}.
     * All the other properties of the component's {@link ViewMetadata} are preserved.
     *
     * @param {Type} component
     * @param {string} html
     *
     * @return {TestComponentBuilder}
     */
    overrideTemplate(componentType, template) {
        var clone = this._clone();
        clone._templateOverrides.set(componentType, template);
        return clone;
    }
    /**
     * Overrides a component's {@link ViewMetadata}.
     *
     * @param {Type} component
     * @param {view} View
     *
     * @return {TestComponentBuilder}
     */
    overrideView(componentType, view) {
        var clone = this._clone();
        clone._viewOverrides.set(componentType, view);
        return clone;
    }
    /**
     * Overrides the directives from the component {@link ViewMetadata}.
     *
     * @param {Type} component
     * @param {Type} from
     * @param {Type} to
     *
     * @return {TestComponentBuilder}
     */
    overrideDirective(componentType, from, to) {
        var clone = this._clone();
        var overridesForComponent = clone._directiveOverrides.get(componentType);
        if (!isPresent(overridesForComponent)) {
            clone._directiveOverrides.set(componentType, new Map());
            overridesForComponent = clone._directiveOverrides.get(componentType);
        }
        overridesForComponent.set(from, to);
        return clone;
    }
    /**
     * Overrides one or more injectables configured via `providers` metadata property of a directive
     * or
     * component.
     * Very useful when certain providers need to be mocked out.
     *
     * The providers specified via this method are appended to the existing `providers` causing the
     * duplicated providers to
     * be overridden.
     *
     * @param {Type} component
     * @param {any[]} providers
     *
     * @return {TestComponentBuilder}
     */
    overrideProviders(type, providers) {
        var clone = this._clone();
        clone._bindingsOverrides.set(type, providers);
        return clone;
    }
    /**
     * @deprecated
     */
    overrideBindings(type, providers) {
        return this.overrideProviders(type, providers);
    }
    /**
     * Overrides one or more injectables configured via `providers` metadata property of a directive
     * or
     * component.
     * Very useful when certain providers need to be mocked out.
     *
     * The providers specified via this method are appended to the existing `providers` causing the
     * duplicated providers to
     * be overridden.
     *
     * @param {Type} component
     * @param {any[]} providers
     *
     * @return {TestComponentBuilder}
     */
    overrideViewProviders(type, providers) {
        var clone = this._clone();
        clone._viewBindingsOverrides.set(type, providers);
        return clone;
    }
    /**
     * @deprecated
     */
    overrideViewBindings(type, providers) {
        return this.overrideViewProviders(type, providers);
    }
    /**
     * Builds and returns a ComponentFixture.
     *
     * @return {Promise<ComponentFixture>}
     */
    createAsync(rootComponentType) {
        var mockDirectiveResolver = this._injector.get(DirectiveResolver);
        var mockViewResolver = this._injector.get(ViewResolver);
        this._viewOverrides.forEach((view, type) => mockViewResolver.setView(type, view));
        this._templateOverrides.forEach((template, type) => mockViewResolver.setInlineTemplate(type, template));
        this._directiveOverrides.forEach((overrides, component) => {
            overrides.forEach((to, from) => { mockViewResolver.overrideViewDirective(component, from, to); });
        });
        this._bindingsOverrides.forEach((bindings, type) => mockDirectiveResolver.setBindingsOverride(type, bindings));
        this._viewBindingsOverrides.forEach((bindings, type) => mockDirectiveResolver.setViewBindingsOverride(type, bindings));
        var rootElId = `root${_nextRootElementId++}`;
        var rootEl = el(`<div id="${rootElId}"></div>`);
        var doc = this._injector.get(DOCUMENT);
        // TODO(juliemr): can/should this be optional?
        var oldRoots = DOM.querySelectorAll(doc, '[id^=root]');
        for (var i = 0; i < oldRoots.length; i++) {
            DOM.remove(oldRoots[i]);
        }
        DOM.appendChild(doc.body, rootEl);
        return this._injector.get(DynamicComponentLoader)
            .loadAsRoot(rootComponentType, `#${rootElId}`, this._injector)
            .then((componentRef) => { return new ComponentFixture_(componentRef); });
    }
};
TestComponentBuilder = __decorate([
    Injectable(), 
    __metadata('design:paramtypes', [Injector])
], TestComponentBuilder);
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidGVzdF9jb21wb25lbnRfYnVpbGRlci5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbImFuZ3VsYXIyL3NyYy90ZXN0aW5nL3Rlc3RfY29tcG9uZW50X2J1aWxkZXIudHMiXSwibmFtZXMiOlsiQ29tcG9uZW50Rml4dHVyZSIsIkNvbXBvbmVudEZpeHR1cmVfIiwiQ29tcG9uZW50Rml4dHVyZV8uY29uc3RydWN0b3IiLCJDb21wb25lbnRGaXh0dXJlXy5kZXRlY3RDaGFuZ2VzIiwiQ29tcG9uZW50Rml4dHVyZV8uZGVzdHJveSIsIlRlc3RDb21wb25lbnRCdWlsZGVyIiwiVGVzdENvbXBvbmVudEJ1aWxkZXIuY29uc3RydWN0b3IiLCJUZXN0Q29tcG9uZW50QnVpbGRlci5fY2xvbmUiLCJUZXN0Q29tcG9uZW50QnVpbGRlci5vdmVycmlkZVRlbXBsYXRlIiwiVGVzdENvbXBvbmVudEJ1aWxkZXIub3ZlcnJpZGVWaWV3IiwiVGVzdENvbXBvbmVudEJ1aWxkZXIub3ZlcnJpZGVEaXJlY3RpdmUiLCJUZXN0Q29tcG9uZW50QnVpbGRlci5vdmVycmlkZVByb3ZpZGVycyIsIlRlc3RDb21wb25lbnRCdWlsZGVyLm92ZXJyaWRlQmluZGluZ3MiLCJUZXN0Q29tcG9uZW50QnVpbGRlci5vdmVycmlkZVZpZXdQcm92aWRlcnMiLCJUZXN0Q29tcG9uZW50QnVpbGRlci5vdmVycmlkZVZpZXdCaW5kaW5ncyIsIlRlc3RDb21wb25lbnRCdWlsZGVyLmNyZWF0ZUFzeW5jIl0sIm1hcHBpbmdzIjoiOzs7Ozs7Ozs7T0FBTyxFQUVMLGlCQUFpQixFQUNqQixzQkFBc0IsRUFDdEIsUUFBUSxFQUNSLFVBQVUsRUFJVixZQUFZLEVBRWIsTUFBTSxlQUFlO09BRWYsRUFBTyxTQUFTLEVBQVUsTUFBTSwwQkFBMEI7T0FFMUQsRUFBYyxVQUFVLEVBQUMsTUFBTSxnQ0FBZ0M7T0FLL0QsRUFBQyxFQUFFLEVBQUMsTUFBTSxTQUFTO09BRW5CLEVBQUMsUUFBUSxFQUFDLE1BQU0sc0NB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IHtcbiAgQ29tcG9uZW50UmVmLFxuICBEaXJlY3RpdmVSZXNvbHZlcixcbiAgRHluYW1pY0NvbXBvbmVudExvYWRlcixcbiAgSW5qZWN0b3IsXG4gIEluamVjdGFibGUsXG4gIFZpZXdNZXRhZGF0YSxcbiAgRWxlbWVudFJlZixcbiAgRW1iZWRkZWRWaWV3UmVmLFxuICBWaWV3UmVzb2x2ZXIsXG4gIHByb3ZpZGVcbn0gZnJvbSAnYW5ndWxhcjIvY29yZSc7XG5cbmltcG9ydCB7VHlwZSwgaXNQcmVzZW50LCBpc0JsYW5rfSBmcm9tICdhbmd1bGFyMi9zcmMvZmFjYWRlL2xhbmcnO1xuaW1wb3J0IHtQcm9taXNlfSBmcm9tICdhbmd1bGFyMi9zcmMvZmFjYWRlL2FzeW5jJztcbmltcG9ydCB7TGlzdFdyYXBwZXIsIE1hcFdyYXBwZXJ9IGZyb20gJ2FuZ3VsYXIyL3NyYy9mYWNhZGUvY29sbGVjdGlvbic7XG5cbmltcG9ydCB7Vmlld1JlZl99IGZyb20gJ2FuZ3VsYXIyL3NyYy9jb3JlL2xpbmtlci92aWV3X3JlZic7XG5pbXBvcnQge0FwcFZpZXd9IGZyb20gJ2FuZ3VsYXIyL3NyYy9jb3JlL2xpbmtlci92aWV3JztcblxuaW1wb3J0IHtlbH0gZnJvbSAnLi91dGlscyc7XG5cbmltcG9ydCB7RE9DVU1FTlR9IGZyb20gJ2FuZ3VsYXIyL3NyYy9wbGF0Zm9ybS9kb20vZG9tX3Rva2Vucyc7XG5pbXBvcnQge0RPTX0gZnJvbSAnYW5ndWxhcjIvc3JjL3BsYXRmb3JtL2RvbS9kb21fYWRhcHRlcic7XG5cbmltcG9ydCB7RGVidWdOb2RlLCBEZWJ1Z0VsZW1lbnQsIGdldERlYnVnTm9kZX0gZnJvbSAnYW5ndWxhcjIvc3JjL2NvcmUvZGVidWcvZGVidWdfbm9kZSc7XG5cblxuLyoqXG4gKiBGaXh0dXJlIGZvciBkZWJ1Z2dpbmcgYW5kIHRlc3RpbmcgYSBjb21wb25lbnQuXG4gKi9cbmV4cG9ydCBhYnN0cmFjdCBjbGFzcyBDb21wb25lbnRGaXh0dXJlIHtcbiAgLyoqXG4gICAqIFRoZSBEZWJ1Z0VsZW1lbnQgYXNzb2NpYXRlZCB3aXRoIHRoZSByb290IGVsZW1lbnQgb2YgdGhpcyBjb21wb25lbnQuXG4gICAqL1xuICBkZWJ1Z0VsZW1lbnQ6IERlYnVnRWxlbWVudDtcblxuICAvKipcbiAgICogVGhlIGluc3RhbmNlIG9mIHRoZSByb290IGNvbXBvbmVudCBjbGFzcy5cbiAgICovXG4gIGNvbXBvbmVudEluc3RhbmNlOiBhbnk7XG5cbiAgLyoqXG4gICAqIFRoZSBuYXRpdmUgZWxlbWVudCBhdCB0aGUgcm9vdCBvZiB0aGUgY29tcG9uZW50LlxuICAgKi9cbiAgbmF0aXZlRWxlbWVudDogYW55O1xuXG4gIC8qKlxuICAgKiBUaGUgRWxlbWVudFJlZiBmb3IgdGhlIGVsZW1lbnQgYXQgdGhlIHJvb3Qgb2YgdGhlIGNvbXBvbmVudC5cbiAgICovXG4gIGVsZW1lbnRSZWY6IEVsZW1lbnRSZWY7XG5cbiAgLyoqXG4gICAqIFRyaWdnZXIgYSBjaGFuZ2UgZGV0ZWN0aW9uIGN5Y2xlIGZvciB0aGUgY29tcG9uZW50LlxuICAgKi9cbiAgYWJzdHJhY3QgZGV0ZWN0Q2hhbmdlcygpOiB2b2lkO1xuXG4gIC8qKlxuICAgKiBUcmlnZ2VyIGNvbXBvbmVudCBkZXN0cnVjdGlvbi5cbiAgICovXG4gIGFic3RyYWN0IGRlc3Ryb3koKTogdm9pZDtcbn1cblxuXG5leHBvcnQgY2xhc3MgQ29tcG9uZW50Rml4dHVyZV8gZXh0ZW5kcyBDb21wb25lbnRGaXh0dXJlIHtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfY29tcG9uZW50UmVmOiBDb21wb25lbnRSZWY7XG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX2NvbXBvbmVudFBhcmVudFZpZXc6IEFwcFZpZXc7XG5cbiAgY29uc3RydWN0b3IoY29tcG9uZW50UmVmOiBDb21wb25lbnRSZWYpIHtcbiAgICBzdXBlcigpO1xuICAgIHRoaXMuX2NvbXBvbmVudFBhcmVudFZpZXcgPSAoPFZpZXdSZWZfPmNvbXBvbmVudFJlZi5ob3N0VmlldykuaW50ZXJuYWxWaWV3O1xuICAgIHRoaXMuZWxlbWVudFJlZiA9IHRoaXMuX2NvbXBvbmVudFBhcmVudFZpZXcuYXBwRWxlbWVudHNbMF0ucmVmO1xuICAgIHRoaXMuZGVidWdFbGVtZW50ID0gPERlYnVnRWxlbWVudD5nZXREZWJ1Z05vZGUoXG4gICAgICAgIHRoaXMuX2NvbXBvbmVudFBhcmVudFZpZXcucm9vdE5vZGVzT3JBcHBFbGVtZW50c1swXS5uYXRpdmVFbGVtZW50KTtcbiAgICB0aGlzLmNvbXBvbmVudEluc3RhbmNlID0gdGhpcy5kZWJ1Z0VsZW1lbnQuY29tcG9uZW50SW5zdGFuY2U7XG4gICAgdGhpcy5uYXRpdmVFbGVtZW50ID0gdGhpcy5kZWJ1Z0VsZW1lbnQubmF0aXZlRWxlbWVudDtcbiAgICB0aGlzLl9jb21wb25lbnRSZWYgPSBjb21wb25lbnRSZWY7XG4gIH1cblxuICBkZXRlY3RDaGFuZ2VzKCk6IHZvaWQge1xuICAgIHRoaXMuX2NvbXBvbmVudFBhcmVudFZpZXcuY2hhbmdlRGV0ZWN0b3IuZGV0ZWN0Q2hhbmdlcygpO1xuICAgIHRoaXMuX2NvbXBvbmVudFBhcmVudFZpZXcuY2hhbmdlRGV0ZWN0b3IuY2hlY2tOb0NoYW5nZXMoKTtcbiAgfVxuXG4gIGRlc3Ryb3koKTogdm9pZCB7IHRoaXMuX2NvbXBvbmVudFJlZi5kaXNwb3NlKCk7IH1cbn1cblxudmFyIF9uZXh0Um9vdEVsZW1lbnRJZCA9IDA7XG5cbi8qKlxuICogQnVpbGRzIGEgQ29tcG9uZW50Rml4dHVyZSBmb3IgdXNlIGluIGNvbXBvbmVudCBsZXZlbCB0ZXN0cy5cbiAqL1xuQEluamVjdGFibGUoKVxuZXhwb3J0IGNsYXNzIFRlc3RDb21wb25lbnRCdWlsZGVyIHtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfYmluZGluZ3NPdmVycmlkZXMgPSBuZXcgTWFwPFR5cGUsIGFueVtdPigpO1xuICAvKiogQGludGVybmFsICovXG4gIF9kaXJlY3RpdmVPdmVycmlkZXMgPSBuZXcgTWFwPFR5cGUsIE1hcDxUeXBlLCBUeXBlPj4oKTtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfdGVtcGxhdGVPdmVycmlkZXMgPSBuZXcgTWFwPFR5cGUsIHN0cmluZz4oKTtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfdmlld0JpbmRpbmdzT3ZlcnJpZGVzID0gbmV3IE1hcDxUeXBlLCBhbnlbXT4oKTtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfdmlld092ZXJyaWRlcyA9IG5ldyBNYXA8VHlwZSwgVmlld01ldGFkYXRhPigpO1xuXG5cbiAgY29uc3RydWN0b3IocHJpdmF0ZSBfaW5qZWN0b3I6IEluamVjdG9yKSB7fVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX2Nsb25lKCk6IFRlc3RDb21wb25lbnRCdWlsZGVyIHtcbiAgICB2YXIgY2xvbmUgPSBuZXcgVGVzdENvbXBvbmVudEJ1aWxkZXIodGhpcy5faW5qZWN0b3IpO1xuICAgIGNsb25lLl92aWV3T3ZlcnJpZGVzID0gTWFwV3JhcHBlci5jbG9uZSh0aGlzLl92aWV3T3ZlcnJpZGVzKTtcbiAgICBjbG9uZS5fZGlyZWN0aXZlT3ZlcnJpZGVzID0gTWFwV3JhcHBlci5jbG9uZSh0aGlzLl9kaXJlY3RpdmVPdmVycmlkZXMpO1xuICAgIGNsb25lLl90ZW1wbGF0ZU92ZXJyaWRlcyA9IE1hcFdyYXBwZXIuY2xvbmUodGhpcy5fdGVtcGxhdGVPdmVycmlkZXMpO1xuICAgIHJldHVybiBjbG9uZTtcbiAgfVxuXG4gIC8qKlxuICAgKiBPdmVycmlkZXMgb25seSB0aGUgaHRtbCBvZiBhIHtAbGluayBDb21wb25lbnRNZXRhZGF0YX0uXG4gICAqIEFsbCB0aGUgb3RoZXIgcHJvcGVydGllcyBvZiB0aGUgY29tcG9uZW50J3Mge0BsaW5rIFZpZXdNZXRhZGF0YX0gYXJlIHByZXNlcnZlZC5cbiAgICpcbiAgICogQHBhcmFtIHtUeXBlfSBjb21wb25lbnRcbiAgICogQHBhcmFtIHtzdHJpbmd9IGh0bWxcbiAgICpcbiAgICogQHJldHVybiB7VGVzdENvbXBvbmVudEJ1aWxkZXJ9XG4gICAqL1xuICBvdmVycmlkZVRlbXBsYXRlKGNvbXBvbmVudFR5cGU6IFR5cGUsIHRlbXBsYXRlOiBzdHJpbmcpOiBUZXN0Q29tcG9uZW50QnVpbGRlciB7XG4gICAgdmFyIGNsb25lID0gdGhpcy5fY2xvbmUoKTtcbiAgICBjbG9uZS5fdGVtcGxhdGVPdmVycmlkZXMuc2V0KGNvbXBvbmVudFR5cGUsIHRlbXBsYXRlKTtcbiAgICByZXR1cm4gY2xvbmU7XG4gIH1cblxuICAvKipcbiAgICogT3ZlcnJpZGVzIGEgY29tcG9uZW50J3Mge0BsaW5rIFZpZXdNZXRhZGF0YX0uXG4gICAqXG4gICAqIEBwYXJhbSB7VHlwZX0gY29tcG9uZW50XG4gICAqIEBwYXJhbSB7dmlld30gVmlld1xuICAgKlxuICAgKiBAcmV0dXJuIHtUZXN0Q29tcG9uZW50QnVpbGRlcn1cbiAgICovXG4gIG92ZXJyaWRlVmlldyhjb21wb25lbnRUeXBlOiBUeXBlLCB2aWV3OiBWaWV3TWV0YWRhdGEpOiBUZXN0Q29tcG9uZW50QnVpbGRlciB7XG4gICAgdmFyIGNsb25lID0gdGhpcy5fY2xvbmUoKTtcbiAgICBjbG9uZS5fdmlld092ZXJyaWRlcy5zZXQoY29tcG9uZW50VHlwZSwgdmlldyk7XG4gICAgcmV0dXJuIGNsb25lO1xuICB9XG5cbiAgLyoqXG4gICAqIE92ZXJyaWRlcyB0aGUgZGlyZWN0aXZlcyBmcm9tIHRoZSBjb21wb25lbnQge0BsaW5rIFZpZXdNZXRhZGF0YX0uXG4gICAqXG4gICAqIEBwYXJhbSB7VHlwZX0gY29tcG9uZW50XG4gICAqIEBwYXJhbSB7VHlwZX0gZnJvbVxuICAgKiBAcGFyYW0ge1R5cGV9IHRvXG4gICAqXG4gICAqIEByZXR1cm4ge1Rlc3RDb21wb25lbnRCdWlsZGVyfVxuICAgKi9cbiAgb3ZlcnJpZGVEaXJlY3RpdmUoY29tcG9uZW50VHlwZTogVHlwZSwgZnJvbTogVHlwZSwgdG86IFR5cGUpOiBUZXN0Q29tcG9uZW50QnVpbGRlciB7XG4gICAgdmFyIGNsb25lID0gdGhpcy5fY2xvbmUoKTtcbiAgICB2YXIgb3ZlcnJpZGVzRm9yQ29tcG9uZW50ID0gY2xvbmUuX2RpcmVjdGl2ZU92ZXJyaWRlcy5nZXQoY29tcG9uZW50VHlwZSk7XG4gICAgaWYgKCFpc1ByZXNlbnQob3ZlcnJpZGVzRm9yQ29tcG9uZW50KSkge1xuICAgICAgY2xvbmUuX2RpcmVjdGl2ZU92ZXJyaWRlcy5zZXQoY29tcG9uZW50VHlwZSwgbmV3IE1hcDxUeXBlLCBUeXBlPigpKTtcbiAgICAgIG92ZXJyaWRlc0ZvckNvbXBvbmVudCA9IGNsb25lLl9kaXJlY3RpdmVPdmVycmlkZXMuZ2V0KGNvbXBvbmVudFR5cGUpO1xuICAgIH1cbiAgICBvdmVycmlkZXNGb3JDb21wb25lbnQuc2V0KGZyb20sIHRvKTtcbiAgICByZXR1cm4gY2xvbmU7XG4gIH1cblxuICAvKipcbiAgICogT3ZlcnJpZGVzIG9uZSBvciBtb3JlIGluamVjdGFibGVzIGNvbmZpZ3VyZWQgdmlhIGBwcm92aWRlcnNgIG1ldGFkYXRhIHByb3BlcnR5IG9mIGEgZGlyZWN0aXZlXG4gICAqIG9yXG4gICAqIGNvbXBvbmVudC5cbiAgICogVmVyeSB1c2VmdWwgd2hlbiBjZXJ0YWluIHByb3ZpZGVycyBuZWVkIHRvIGJlIG1vY2tlZCBvdXQuXG4gICAqXG4gICAqIFRoZSBwcm92aWRlcnMgc3BlY2lmaWVkIHZpYSB0aGlzIG1ldGhvZCBhcmUgYXBwZW5kZWQgdG8gdGhlIGV4aXN0aW5nIGBwcm92aWRlcnNgIGNhdXNpbmcgdGhlXG4gICAqIGR1cGxpY2F0ZWQgcHJvdmlkZXJzIHRvXG4gICAqIGJlIG92ZXJyaWRkZW4uXG4gICAqXG4gICAqIEBwYXJhbSB7VHlwZX0gY29tcG9uZW50XG4gICAqIEBwYXJhbSB7YW55W119IHByb3ZpZGVyc1xuICAgKlxuICAgKiBAcmV0dXJuIHtUZXN0Q29tcG9uZW50QnVpbGRlcn1cbiAgICovXG4gIG92ZXJyaWRlUHJvdmlkZXJzKHR5cGU6IFR5cGUsIHByb3ZpZGVyczogYW55W10pOiBUZXN0Q29tcG9uZW50QnVpbGRlciB7XG4gICAgdmFyIGNsb25lID0gdGhpcy5fY2xvbmUoKTtcbiAgICBjbG9uZS5fYmluZGluZ3NPdmVycmlkZXMuc2V0KHR5cGUsIHByb3ZpZGVycyk7XG4gICAgcmV0dXJuIGNsb25lO1xuICB9XG5cbiAgLyoqXG4gICAqIEBkZXByZWNhdGVkXG4gICAqL1xuICBvdmVycmlkZUJpbmRpbmdzKHR5cGU6IFR5cGUsIHByb3ZpZGVyczogYW55W10pOiBUZXN0Q29tcG9uZW50QnVpbGRlciB7XG4gICAgcmV0dXJuIHRoaXMub3ZlcnJpZGVQcm92aWRlcnModHlwZSwgcHJvdmlkZXJzKTtcbiAgfVxuXG4gIC8qKlxuICAgKiBPdmVycmlkZXMgb25lIG9yIG1vcmUgaW5qZWN0YWJsZXMgY29uZmlndXJlZCB2aWEgYHByb3ZpZGVyc2AgbWV0YWRhdGEgcHJvcGVydHkgb2YgYSBkaXJlY3RpdmVcbiAgICogb3JcbiAgICogY29tcG9uZW50LlxuICAgKiBWZXJ5IHVzZWZ1bCB3aGVuIGNlcnRhaW4gcHJvdmlkZXJzIG5lZWQgdG8gYmUgbW9ja2VkIG91dC5cbiAgICpcbiAgICogVGhlIHByb3ZpZGVycyBzcGVjaWZpZWQgdmlhIHRoaXMgbWV0aG9kIGFyZSBhcHBlbmRlZCB0byB0aGUgZXhpc3RpbmcgYHByb3ZpZGVyc2AgY2F1c2luZyB0aGVcbiAgICogZHVwbGljYXRlZCBwcm92aWRlcnMgdG9cbiAgICogYmUgb3ZlcnJpZGRlbi5cbiAgICpcbiAgICogQHBhcmFtIHtUeXBlfSBjb21wb25lbnRcbiAgICogQHBhcmFtIHthbnlbXX0gcHJvdmlkZXJzXG4gICAqXG4gICAqIEByZXR1cm4ge1Rlc3RDb21wb25lbnRCdWlsZGVyfVxuICAgKi9cbiAgb3ZlcnJpZGVWaWV3UHJvdmlkZXJzKHR5cGU6IFR5cGUsIHByb3ZpZGVyczogYW55W10pOiBUZXN0Q29tcG9uZW50QnVpbGRlciB7XG4gICAgdmFyIGNsb25lID0gdGhpcy5fY2xvbmUoKTtcbiAgICBjbG9uZS5fdmlld0JpbmRpbmdzT3ZlcnJpZGVzLnNldCh0eXBlLCBwcm92aWRlcnMpO1xuICAgIHJldHVybiBjbG9uZTtcbiAgfVxuXG4gIC8qKlxuICAgKiBAZGVwcmVjYXRlZFxuICAgKi9cbiAgb3ZlcnJpZGVWaWV3QmluZGluZ3ModHlwZTogVHlwZSwgcHJvdmlkZXJzOiBhbnlbXSk6IFRlc3RDb21wb25lbnRCdWlsZGVyIHtcbiAgICByZXR1cm4gdGhpcy5vdmVycmlkZVZpZXdQcm92aWRlcnModHlwZSwgcHJvdmlkZXJzKTtcbiAgfVxuXG4gIC8qKlxuICAgKiBCdWlsZHMgYW5kIHJldHVybnMgYSBDb21wb25lbnRGaXh0dXJlLlxuICAgKlxuICAgKiBAcmV0dXJuIHtQcm9taXNlPENvbXBvbmVudEZpeHR1cmU+fVxuICAgKi9cbiAgY3JlYXRlQXN5bmMocm9vdENvbXBvbmVudFR5cGU6IFR5cGUpOiBQcm9taXNlPENvbXBvbmVudEZpeHR1cmU+IHtcbiAgICB2YXIgbW9ja0RpcmVjdGl2ZVJlc29sdmVyID0gdGhpcy5faW5qZWN0b3IuZ2V0KERpcmVjdGl2ZVJlc29sdmVyKTtcbiAgICB2YXIgbW9ja1ZpZXdSZXNvbHZlciA9IHRoaXMuX2luamVjdG9yLmdldChWaWV3UmVzb2x2ZXIpO1xuICAgIHRoaXMuX3ZpZXdPdmVycmlkZXMuZm9yRWFjaCgodmlldywgdHlwZSkgPT4gbW9ja1ZpZXdSZXNvbHZlci5zZXRWaWV3KHR5cGUsIHZpZXcpKTtcbiAgICB0aGlzLl90ZW1wbGF0ZU92ZXJyaWRlcy5mb3JFYWNoKCh0ZW1wbGF0ZSwgdHlwZSkgPT5cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBtb2NrVmlld1Jlc29sdmVyLnNldElubGluZVRlbXBsYXRlKHR5cGUsIHRlbXBsYXRlKSk7XG4gICAgdGhpcy5fZGlyZWN0aXZlT3ZlcnJpZGVzLmZvckVhY2goKG92ZXJyaWRlcywgY29tcG9uZW50KSA9PiB7XG4gICAgICBvdmVycmlkZXMuZm9yRWFjaChcbiAgICAgICAgICAodG8sIGZyb20pID0+IHsgbW9ja1ZpZXdSZXNvbHZlci5vdmVycmlkZVZpZXdEaXJlY3RpdmUoY29tcG9uZW50LCBmcm9tLCB0byk7IH0pO1xuICAgIH0pO1xuXG4gICAgdGhpcy5fYmluZGluZ3NPdmVycmlkZXMuZm9yRWFjaCgoYmluZGluZ3MsIHR5cGUpID0+XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbW9ja0RpcmVjdGl2ZVJlc29sdmVyLnNldEJpbmRpbmdzT3ZlcnJpZGUodHlwZSwgYmluZGluZ3MpKTtcbiAgICB0aGlzLl92aWV3QmluZGluZ3NPdmVycmlkZXMuZm9yRWFjaChcbiAgICAgICAgKGJpbmRpbmdzLCB0eXBlKSA9PiBtb2NrRGlyZWN0aXZlUmVzb2x2ZXIuc2V0Vmlld0JpbmRpbmdzT3ZlcnJpZGUodHlwZSwgYmluZGluZ3MpKTtcblxuICAgIHZhciByb290RWxJZCA9IGByb290JHtfbmV4dFJvb3RFbGVtZW50SWQrK31gO1xuICAgIHZhciByb290RWwgPSBlbChgPGRpdiBpZD1cIiR7cm9vdEVsSWR9XCI+PC9kaXY+YCk7XG4gICAgdmFyIGRvYyA9IHRoaXMuX2luamVjdG9yLmdldChET0NVTUVOVCk7XG5cbiAgICAvLyBUT0RPKGp1bGllbXIpOiBjYW4vc2hvdWxkIHRoaXMgYmUgb3B0aW9uYWw/XG4gICAgdmFyIG9sZFJvb3RzID0gRE9NLnF1ZXJ5U2VsZWN0b3JBbGwoZG9jLCAnW2lkXj1yb290XScpO1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgb2xkUm9vdHMubGVuZ3RoOyBpKyspIHtcbiAgICAgIERPTS5yZW1vdmUob2xkUm9vdHNbaV0pO1xuICAgIH1cbiAgICBET00uYXBwZW5kQ2hpbGQoZG9jLmJvZHksIHJvb3RFbCk7XG5cblxuICAgIHJldHVybiB0aGlzLl9pbmplY3Rvci5nZXQoRHluYW1pY0NvbXBvbmVudExvYWRlcilcbiAgICAgICAgLmxvYWRBc1Jvb3Qocm9vdENvbXBvbmVudFR5cGUsIGAjJHtyb290RWxJZH1gLCB0aGlzLl9pbmplY3RvcilcbiAgICAgICAgLnRoZW4oKGNvbXBvbmVudFJlZikgPT4geyByZXR1cm4gbmV3IENvbXBvbmVudEZpeHR1cmVfKGNvbXBvbmVudFJlZik7IH0pO1xuICB9XG59XG4iXX0=