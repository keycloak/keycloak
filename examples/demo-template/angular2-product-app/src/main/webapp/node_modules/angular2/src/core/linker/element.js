'use strict';var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var lang_1 = require('angular2/src/facade/lang');
var exceptions_1 = require('angular2/src/facade/exceptions');
var collection_1 = require('angular2/src/facade/collection');
var di_1 = require('angular2/src/core/di');
var provider_1 = require('angular2/src/core/di/provider');
var injector_1 = require('angular2/src/core/di/injector');
var provider_2 = require('angular2/src/core/di/provider');
var di_2 = require('../metadata/di');
var view_type_1 = require('./view_type');
var element_ref_1 = require('./element_ref');
var view_container_ref_1 = require('./view_container_ref');
var element_ref_2 = require('./element_ref');
var api_1 = require('angular2/src/core/render/api');
var template_ref_1 = require('./template_ref');
var directives_1 = require('../metadata/directives');
var change_detection_1 = require('angular2/src/core/change_detection/change_detection');
var query_list_1 = require('./query_list');
var reflection_1 = require('angular2/src/core/reflection/reflection');
var pipe_provider_1 = require('angular2/src/core/pipes/pipe_provider');
var view_container_ref_2 = require("./view_container_ref");
var _staticKeys;
var StaticKeys = (function () {
    function StaticKeys() {
        this.templateRefId = di_1.Key.get(template_ref_1.TemplateRef).id;
        this.viewContainerId = di_1.Key.get(view_container_ref_1.ViewContainerRef).id;
        this.changeDetectorRefId = di_1.Key.get(change_detection_1.ChangeDetectorRef).id;
        this.elementRefId = di_1.Key.get(element_ref_2.ElementRef).id;
        this.rendererId = di_1.Key.get(api_1.Renderer).id;
    }
    StaticKeys.instance = function () {
        if (lang_1.isBlank(_staticKeys))
            _staticKeys = new StaticKeys();
        return _staticKeys;
    };
    return StaticKeys;
})();
exports.StaticKeys = StaticKeys;
var DirectiveDependency = (function (_super) {
    __extends(DirectiveDependency, _super);
    function DirectiveDependency(key, optional, lowerBoundVisibility, upperBoundVisibility, properties, attributeName, queryDecorator) {
        _super.call(this, key, optional, lowerBoundVisibility, upperBoundVisibility, properties);
        this.attributeName = attributeName;
        this.queryDecorator = queryDecorator;
        this._verify();
    }
    /** @internal */
    DirectiveDependency.prototype._verify = function () {
        var count = 0;
        if (lang_1.isPresent(this.queryDecorator))
            count++;
        if (lang_1.isPresent(this.attributeName))
            count++;
        if (count > 1)
            throw new exceptions_1.BaseException('A directive injectable can contain only one of the following @Attribute or @Query.');
    };
    DirectiveDependency.createFrom = function (d) {
        return new DirectiveDependency(d.key, d.optional, d.lowerBoundVisibility, d.upperBoundVisibility, d.properties, DirectiveDependency._attributeName(d.properties), DirectiveDependency._query(d.properties));
    };
    /** @internal */
    DirectiveDependency._attributeName = function (properties) {
        var p = properties.find(function (p) { return p instanceof di_2.AttributeMetadata; });
        return lang_1.isPresent(p) ? p.attributeName : null;
    };
    /** @internal */
    DirectiveDependency._query = function (properties) {
        return properties.find(function (p) { return p instanceof di_2.QueryMetadata; });
    };
    return DirectiveDependency;
})(di_1.Dependency);
exports.DirectiveDependency = DirectiveDependency;
var DirectiveProvider = (function (_super) {
    __extends(DirectiveProvider, _super);
    function DirectiveProvider(key, factory, deps, isComponent, providers, viewProviders, queries) {
        _super.call(this, key, [new provider_2.ResolvedFactory(factory, deps)], false);
        this.isComponent = isComponent;
        this.providers = providers;
        this.viewProviders = viewProviders;
        this.queries = queries;
    }
    Object.defineProperty(DirectiveProvider.prototype, "displayName", {
        get: function () { return this.key.displayName; },
        enumerable: true,
        configurable: true
    });
    DirectiveProvider.createFromType = function (type, meta) {
        var provider = new di_1.Provider(type, { useClass: type });
        if (lang_1.isBlank(meta)) {
            meta = new directives_1.DirectiveMetadata();
        }
        var rb = provider_2.resolveProvider(provider);
        var rf = rb.resolvedFactories[0];
        var deps = rf.dependencies.map(DirectiveDependency.createFrom);
        var isComponent = meta instanceof directives_1.ComponentMetadata;
        var resolvedProviders = lang_1.isPresent(meta.providers) ? di_1.Injector.resolve(meta.providers) : null;
        var resolvedViewProviders = meta instanceof directives_1.ComponentMetadata && lang_1.isPresent(meta.viewProviders) ?
            di_1.Injector.resolve(meta.viewProviders) :
            null;
        var queries = [];
        if (lang_1.isPresent(meta.queries)) {
            collection_1.StringMapWrapper.forEach(meta.queries, function (meta, fieldName) {
                var setter = reflection_1.reflector.setter(fieldName);
                queries.push(new QueryMetadataWithSetter(setter, meta));
            });
        }
        // queries passed into the constructor.
        // TODO: remove this after constructor queries are no longer supported
        deps.forEach(function (d) {
            if (lang_1.isPresent(d.queryDecorator)) {
                queries.push(new QueryMetadataWithSetter(null, d.queryDecorator));
            }
        });
        return new DirectiveProvider(rb.key, rf.factory, deps, isComponent, resolvedProviders, resolvedViewProviders, queries);
    };
    return DirectiveProvider;
})(provider_2.ResolvedProvider_);
exports.DirectiveProvider = DirectiveProvider;
var QueryMetadataWithSetter = (function () {
    function QueryMetadataWithSetter(setter, metadata) {
        this.setter = setter;
        this.metadata = metadata;
    }
    return QueryMetadataWithSetter;
})();
exports.QueryMetadataWithSetter = QueryMetadataWithSetter;
function setProvidersVisibility(providers, visibility, result) {
    for (var i = 0; i < providers.length; i++) {
        result.set(providers[i].key.id, visibility);
    }
}
var AppProtoElement = (function () {
    function AppProtoElement(firstProviderIsComponent, index, attributes, pwvs, protoQueryRefs, directiveVariableBindings) {
        this.firstProviderIsComponent = firstProviderIsComponent;
        this.index = index;
        this.attributes = attributes;
        this.protoQueryRefs = protoQueryRefs;
        this.directiveVariableBindings = directiveVariableBindings;
        var length = pwvs.length;
        if (length > 0) {
            this.protoInjector = new injector_1.ProtoInjector(pwvs);
        }
        else {
            this.protoInjector = null;
            this.protoQueryRefs = [];
        }
    }
    AppProtoElement.create = function (metadataCache, index, attributes, directiveTypes, directiveVariableBindings) {
        var componentDirProvider = null;
        var mergedProvidersMap = new Map();
        var providerVisibilityMap = new Map();
        var providers = collection_1.ListWrapper.createGrowableSize(directiveTypes.length);
        var protoQueryRefs = [];
        for (var i = 0; i < directiveTypes.length; i++) {
            var dirProvider = metadataCache.getResolvedDirectiveMetadata(directiveTypes[i]);
            providers[i] = new injector_1.ProviderWithVisibility(dirProvider, dirProvider.isComponent ? injector_1.Visibility.PublicAndPrivate : injector_1.Visibility.Public);
            if (dirProvider.isComponent) {
                componentDirProvider = dirProvider;
            }
            else {
                if (lang_1.isPresent(dirProvider.providers)) {
                    provider_1.mergeResolvedProviders(dirProvider.providers, mergedProvidersMap);
                    setProvidersVisibility(dirProvider.providers, injector_1.Visibility.Public, providerVisibilityMap);
                }
            }
            if (lang_1.isPresent(dirProvider.viewProviders)) {
                provider_1.mergeResolvedProviders(dirProvider.viewProviders, mergedProvidersMap);
                setProvidersVisibility(dirProvider.viewProviders, injector_1.Visibility.Private, providerVisibilityMap);
            }
            for (var queryIdx = 0; queryIdx < dirProvider.queries.length; queryIdx++) {
                var q = dirProvider.queries[queryIdx];
                protoQueryRefs.push(new ProtoQueryRef(i, q.setter, q.metadata));
            }
        }
        if (lang_1.isPresent(componentDirProvider) && lang_1.isPresent(componentDirProvider.providers)) {
            // directive providers need to be prioritized over component providers
            provider_1.mergeResolvedProviders(componentDirProvider.providers, mergedProvidersMap);
            setProvidersVisibility(componentDirProvider.providers, injector_1.Visibility.Public, providerVisibilityMap);
        }
        mergedProvidersMap.forEach(function (provider, _) {
            providers.push(new injector_1.ProviderWithVisibility(provider, providerVisibilityMap.get(provider.key.id)));
        });
        return new AppProtoElement(lang_1.isPresent(componentDirProvider), index, attributes, providers, protoQueryRefs, directiveVariableBindings);
    };
    AppProtoElement.prototype.getProviderAtIndex = function (index) { return this.protoInjector.getProviderAtIndex(index); };
    return AppProtoElement;
})();
exports.AppProtoElement = AppProtoElement;
var _Context = (function () {
    function _Context(element, componentElement, injector) {
        this.element = element;
        this.componentElement = componentElement;
        this.injector = injector;
    }
    return _Context;
})();
var InjectorWithHostBoundary = (function () {
    function InjectorWithHostBoundary(injector, hostInjectorBoundary) {
        this.injector = injector;
        this.hostInjectorBoundary = hostInjectorBoundary;
    }
    return InjectorWithHostBoundary;
})();
exports.InjectorWithHostBoundary = InjectorWithHostBoundary;
var AppElement = (function () {
    function AppElement(proto, parentView, parent, nativeElement, embeddedViewFactory) {
        var _this = this;
        this.proto = proto;
        this.parentView = parentView;
        this.parent = parent;
        this.nativeElement = nativeElement;
        this.embeddedViewFactory = embeddedViewFactory;
        this.nestedViews = null;
        this.componentView = null;
        this.ref = new element_ref_1.ElementRef_(this);
        var parentInjector = lang_1.isPresent(parent) ? parent._injector : parentView.parentInjector;
        if (lang_1.isPresent(this.proto.protoInjector)) {
            var isBoundary;
            if (lang_1.isPresent(parent) && lang_1.isPresent(parent.proto.protoInjector)) {
                isBoundary = false;
            }
            else {
                isBoundary = parentView.hostInjectorBoundary;
            }
            this._queryStrategy = this._buildQueryStrategy();
            this._injector = new di_1.Injector(this.proto.protoInjector, parentInjector, isBoundary, this, function () { return _this._debugContext(); });
            // we couple ourselves to the injector strategy to avoid polymorphic calls
            var injectorStrategy = this._injector.internalStrategy;
            this._strategy = injectorStrategy instanceof injector_1.InjectorInlineStrategy ?
                new ElementDirectiveInlineStrategy(injectorStrategy, this) :
                new ElementDirectiveDynamicStrategy(injectorStrategy, this);
            this._strategy.init();
        }
        else {
            this._queryStrategy = null;
            this._injector = parentInjector;
            this._strategy = null;
        }
    }
    AppElement.getViewParentInjector = function (parentViewType, containerAppElement, imperativelyCreatedProviders, rootInjector) {
        var parentInjector;
        var hostInjectorBoundary;
        switch (parentViewType) {
            case view_type_1.ViewType.COMPONENT:
                parentInjector = containerAppElement._injector;
                hostInjectorBoundary = true;
                break;
            case view_type_1.ViewType.EMBEDDED:
                parentInjector = lang_1.isPresent(containerAppElement.proto.protoInjector) ?
                    containerAppElement._injector.parent :
                    containerAppElement._injector;
                hostInjectorBoundary = containerAppElement._injector.hostBoundary;
                break;
            case view_type_1.ViewType.HOST:
                if (lang_1.isPresent(containerAppElement)) {
                    // host view is attached to a container
                    parentInjector = lang_1.isPresent(containerAppElement.proto.protoInjector) ?
                        containerAppElement._injector.parent :
                        containerAppElement._injector;
                    if (lang_1.isPresent(imperativelyCreatedProviders)) {
                        var imperativeProvidersWithVisibility = imperativelyCreatedProviders.map(function (p) { return new injector_1.ProviderWithVisibility(p, injector_1.Visibility.Public); });
                        // The imperative injector is similar to having an element between
                        // the dynamic-loaded component and its parent => no boundary between
                        // the component and imperativelyCreatedInjector.
                        parentInjector = new di_1.Injector(new injector_1.ProtoInjector(imperativeProvidersWithVisibility), parentInjector, true, null, null);
                        hostInjectorBoundary = false;
                    }
                    else {
                        hostInjectorBoundary = containerAppElement._injector.hostBoundary;
                    }
                }
                else {
                    // bootstrap
                    parentInjector = rootInjector;
                    hostInjectorBoundary = true;
                }
                break;
        }
        return new InjectorWithHostBoundary(parentInjector, hostInjectorBoundary);
    };
    AppElement.prototype.attachComponentView = function (componentView) { this.componentView = componentView; };
    AppElement.prototype._debugContext = function () {
        var c = this.parentView.getDebugContext(this, null, null);
        return lang_1.isPresent(c) ? new _Context(c.element, c.componentElement, c.injector) : null;
    };
    AppElement.prototype.hasVariableBinding = function (name) {
        var vb = this.proto.directiveVariableBindings;
        return lang_1.isPresent(vb) && collection_1.StringMapWrapper.contains(vb, name);
    };
    AppElement.prototype.getVariableBinding = function (name) {
        var index = this.proto.directiveVariableBindings[name];
        return lang_1.isPresent(index) ? this.getDirectiveAtIndex(index) : this.getElementRef();
    };
    AppElement.prototype.get = function (token) { return this._injector.get(token); };
    AppElement.prototype.hasDirective = function (type) { return lang_1.isPresent(this._injector.getOptional(type)); };
    AppElement.prototype.getComponent = function () { return lang_1.isPresent(this._strategy) ? this._strategy.getComponent() : null; };
    AppElement.prototype.getInjector = function () { return this._injector; };
    AppElement.prototype.getElementRef = function () { return this.ref; };
    AppElement.prototype.getViewContainerRef = function () { return new view_container_ref_2.ViewContainerRef_(this); };
    AppElement.prototype.getTemplateRef = function () {
        if (lang_1.isPresent(this.embeddedViewFactory)) {
            return new template_ref_1.TemplateRef_(this.ref);
        }
        return null;
    };
    AppElement.prototype.getDependency = function (injector, provider, dep) {
        if (provider instanceof DirectiveProvider) {
            var dirDep = dep;
            if (lang_1.isPresent(dirDep.attributeName))
                return this._buildAttribute(dirDep);
            if (lang_1.isPresent(dirDep.queryDecorator))
                return this._queryStrategy.findQuery(dirDep.queryDecorator).list;
            if (dirDep.key.id === StaticKeys.instance().changeDetectorRefId) {
                // We provide the component's view change detector to components and
                // the surrounding component's change detector to directives.
                if (this.proto.firstProviderIsComponent) {
                    // Note: The component view is not yet created when
                    // this method is called!
                    return new _ComponentViewChangeDetectorRef(this);
                }
                else {
                    return this.parentView.changeDetector.ref;
                }
            }
            if (dirDep.key.id === StaticKeys.instance().elementRefId) {
                return this.getElementRef();
            }
            if (dirDep.key.id === StaticKeys.instance().viewContainerId) {
                return this.getViewContainerRef();
            }
            if (dirDep.key.id === StaticKeys.instance().templateRefId) {
                var tr = this.getTemplateRef();
                if (lang_1.isBlank(tr) && !dirDep.optional) {
                    throw new di_1.NoProviderError(null, dirDep.key);
                }
                return tr;
            }
            if (dirDep.key.id === StaticKeys.instance().rendererId) {
                return this.parentView.renderer;
            }
        }
        else if (provider instanceof pipe_provider_1.PipeProvider) {
            if (dep.key.id === StaticKeys.instance().changeDetectorRefId) {
                // We provide the component's view change detector to components and
                // the surrounding component's change detector to directives.
                if (this.proto.firstProviderIsComponent) {
                    // Note: The component view is not yet created when
                    // this method is called!
                    return new _ComponentViewChangeDetectorRef(this);
                }
                else {
                    return this.parentView.changeDetector;
                }
            }
        }
        return injector_1.UNDEFINED;
    };
    AppElement.prototype._buildAttribute = function (dep) {
        var attributes = this.proto.attributes;
        if (lang_1.isPresent(attributes) && collection_1.StringMapWrapper.contains(attributes, dep.attributeName)) {
            return attributes[dep.attributeName];
        }
        else {
            return null;
        }
    };
    AppElement.prototype.addDirectivesMatchingQuery = function (query, list) {
        var templateRef = this.getTemplateRef();
        if (query.selector === template_ref_1.TemplateRef && lang_1.isPresent(templateRef)) {
            list.push(templateRef);
        }
        if (this._strategy != null) {
            this._strategy.addDirectivesMatchingQuery(query, list);
        }
    };
    AppElement.prototype._buildQueryStrategy = function () {
        if (this.proto.protoQueryRefs.length === 0) {
            return _emptyQueryStrategy;
        }
        else if (this.proto.protoQueryRefs.length <=
            InlineQueryStrategy.NUMBER_OF_SUPPORTED_QUERIES) {
            return new InlineQueryStrategy(this);
        }
        else {
            return new DynamicQueryStrategy(this);
        }
    };
    AppElement.prototype.getDirectiveAtIndex = function (index) { return this._injector.getAt(index); };
    AppElement.prototype.ngAfterViewChecked = function () {
        if (lang_1.isPresent(this._queryStrategy))
            this._queryStrategy.updateViewQueries();
    };
    AppElement.prototype.ngAfterContentChecked = function () {
        if (lang_1.isPresent(this._queryStrategy))
            this._queryStrategy.updateContentQueries();
    };
    AppElement.prototype.traverseAndSetQueriesAsDirty = function () {
        var inj = this;
        while (lang_1.isPresent(inj)) {
            inj._setQueriesAsDirty();
            if (lang_1.isBlank(inj.parent) && inj.parentView.proto.type === view_type_1.ViewType.EMBEDDED) {
                inj = inj.parentView.containerAppElement;
            }
            else {
                inj = inj.parent;
            }
        }
    };
    AppElement.prototype._setQueriesAsDirty = function () {
        if (lang_1.isPresent(this._queryStrategy)) {
            this._queryStrategy.setContentQueriesAsDirty();
        }
        if (this.parentView.proto.type === view_type_1.ViewType.COMPONENT) {
            this.parentView.containerAppElement._queryStrategy.setViewQueriesAsDirty();
        }
    };
    return AppElement;
})();
exports.AppElement = AppElement;
var _EmptyQueryStrategy = (function () {
    function _EmptyQueryStrategy() {
    }
    _EmptyQueryStrategy.prototype.setContentQueriesAsDirty = function () { };
    _EmptyQueryStrategy.prototype.setViewQueriesAsDirty = function () { };
    _EmptyQueryStrategy.prototype.updateContentQueries = function () { };
    _EmptyQueryStrategy.prototype.updateViewQueries = function () { };
    _EmptyQueryStrategy.prototype.findQuery = function (query) {
        throw new exceptions_1.BaseException("Cannot find query for directive " + query + ".");
    };
    return _EmptyQueryStrategy;
})();
var _emptyQueryStrategy = new _EmptyQueryStrategy();
var InlineQueryStrategy = (function () {
    function InlineQueryStrategy(ei) {
        var protoRefs = ei.proto.protoQueryRefs;
        if (protoRefs.length > 0)
            this.query0 = new QueryRef(protoRefs[0], ei);
        if (protoRefs.length > 1)
            this.query1 = new QueryRef(protoRefs[1], ei);
        if (protoRefs.length > 2)
            this.query2 = new QueryRef(protoRefs[2], ei);
    }
    InlineQueryStrategy.prototype.setContentQueriesAsDirty = function () {
        if (lang_1.isPresent(this.query0) && !this.query0.isViewQuery)
            this.query0.dirty = true;
        if (lang_1.isPresent(this.query1) && !this.query1.isViewQuery)
            this.query1.dirty = true;
        if (lang_1.isPresent(this.query2) && !this.query2.isViewQuery)
            this.query2.dirty = true;
    };
    InlineQueryStrategy.prototype.setViewQueriesAsDirty = function () {
        if (lang_1.isPresent(this.query0) && this.query0.isViewQuery)
            this.query0.dirty = true;
        if (lang_1.isPresent(this.query1) && this.query1.isViewQuery)
            this.query1.dirty = true;
        if (lang_1.isPresent(this.query2) && this.query2.isViewQuery)
            this.query2.dirty = true;
    };
    InlineQueryStrategy.prototype.updateContentQueries = function () {
        if (lang_1.isPresent(this.query0) && !this.query0.isViewQuery) {
            this.query0.update();
        }
        if (lang_1.isPresent(this.query1) && !this.query1.isViewQuery) {
            this.query1.update();
        }
        if (lang_1.isPresent(this.query2) && !this.query2.isViewQuery) {
            this.query2.update();
        }
    };
    InlineQueryStrategy.prototype.updateViewQueries = function () {
        if (lang_1.isPresent(this.query0) && this.query0.isViewQuery) {
            this.query0.update();
        }
        if (lang_1.isPresent(this.query1) && this.query1.isViewQuery) {
            this.query1.update();
        }
        if (lang_1.isPresent(this.query2) && this.query2.isViewQuery) {
            this.query2.update();
        }
    };
    InlineQueryStrategy.prototype.findQuery = function (query) {
        if (lang_1.isPresent(this.query0) && this.query0.protoQueryRef.query === query) {
            return this.query0;
        }
        if (lang_1.isPresent(this.query1) && this.query1.protoQueryRef.query === query) {
            return this.query1;
        }
        if (lang_1.isPresent(this.query2) && this.query2.protoQueryRef.query === query) {
            return this.query2;
        }
        throw new exceptions_1.BaseException("Cannot find query for directive " + query + ".");
    };
    InlineQueryStrategy.NUMBER_OF_SUPPORTED_QUERIES = 3;
    return InlineQueryStrategy;
})();
var DynamicQueryStrategy = (function () {
    function DynamicQueryStrategy(ei) {
        this.queries = ei.proto.protoQueryRefs.map(function (p) { return new QueryRef(p, ei); });
    }
    DynamicQueryStrategy.prototype.setContentQueriesAsDirty = function () {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (!q.isViewQuery)
                q.dirty = true;
        }
    };
    DynamicQueryStrategy.prototype.setViewQueriesAsDirty = function () {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (q.isViewQuery)
                q.dirty = true;
        }
    };
    DynamicQueryStrategy.prototype.updateContentQueries = function () {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (!q.isViewQuery) {
                q.update();
            }
        }
    };
    DynamicQueryStrategy.prototype.updateViewQueries = function () {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (q.isViewQuery) {
                q.update();
            }
        }
    };
    DynamicQueryStrategy.prototype.findQuery = function (query) {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (q.protoQueryRef.query === query) {
                return q;
            }
        }
        throw new exceptions_1.BaseException("Cannot find query for directive " + query + ".");
    };
    return DynamicQueryStrategy;
})();
/**
 * Strategy used by the `ElementInjector` when the number of providers is 10 or less.
 * In such a case, inlining fields is beneficial for performances.
 */
var ElementDirectiveInlineStrategy = (function () {
    function ElementDirectiveInlineStrategy(injectorStrategy, _ei) {
        this.injectorStrategy = injectorStrategy;
        this._ei = _ei;
    }
    ElementDirectiveInlineStrategy.prototype.init = function () {
        var i = this.injectorStrategy;
        var p = i.protoStrategy;
        i.resetConstructionCounter();
        if (p.provider0 instanceof DirectiveProvider && lang_1.isPresent(p.keyId0) && i.obj0 === injector_1.UNDEFINED)
            i.obj0 = i.instantiateProvider(p.provider0, p.visibility0);
        if (p.provider1 instanceof DirectiveProvider && lang_1.isPresent(p.keyId1) && i.obj1 === injector_1.UNDEFINED)
            i.obj1 = i.instantiateProvider(p.provider1, p.visibility1);
        if (p.provider2 instanceof DirectiveProvider && lang_1.isPresent(p.keyId2) && i.obj2 === injector_1.UNDEFINED)
            i.obj2 = i.instantiateProvider(p.provider2, p.visibility2);
        if (p.provider3 instanceof DirectiveProvider && lang_1.isPresent(p.keyId3) && i.obj3 === injector_1.UNDEFINED)
            i.obj3 = i.instantiateProvider(p.provider3, p.visibility3);
        if (p.provider4 instanceof DirectiveProvider && lang_1.isPresent(p.keyId4) && i.obj4 === injector_1.UNDEFINED)
            i.obj4 = i.instantiateProvider(p.provider4, p.visibility4);
        if (p.provider5 instanceof DirectiveProvider && lang_1.isPresent(p.keyId5) && i.obj5 === injector_1.UNDEFINED)
            i.obj5 = i.instantiateProvider(p.provider5, p.visibility5);
        if (p.provider6 instanceof DirectiveProvider && lang_1.isPresent(p.keyId6) && i.obj6 === injector_1.UNDEFINED)
            i.obj6 = i.instantiateProvider(p.provider6, p.visibility6);
        if (p.provider7 instanceof DirectiveProvider && lang_1.isPresent(p.keyId7) && i.obj7 === injector_1.UNDEFINED)
            i.obj7 = i.instantiateProvider(p.provider7, p.visibility7);
        if (p.provider8 instanceof DirectiveProvider && lang_1.isPresent(p.keyId8) && i.obj8 === injector_1.UNDEFINED)
            i.obj8 = i.instantiateProvider(p.provider8, p.visibility8);
        if (p.provider9 instanceof DirectiveProvider && lang_1.isPresent(p.keyId9) && i.obj9 === injector_1.UNDEFINED)
            i.obj9 = i.instantiateProvider(p.provider9, p.visibility9);
    };
    ElementDirectiveInlineStrategy.prototype.getComponent = function () { return this.injectorStrategy.obj0; };
    ElementDirectiveInlineStrategy.prototype.isComponentKey = function (key) {
        return this._ei.proto.firstProviderIsComponent && lang_1.isPresent(key) &&
            key.id === this.injectorStrategy.protoStrategy.keyId0;
    };
    ElementDirectiveInlineStrategy.prototype.addDirectivesMatchingQuery = function (query, list) {
        var i = this.injectorStrategy;
        var p = i.protoStrategy;
        if (lang_1.isPresent(p.provider0) && p.provider0.key.token === query.selector) {
            if (i.obj0 === injector_1.UNDEFINED)
                i.obj0 = i.instantiateProvider(p.provider0, p.visibility0);
            list.push(i.obj0);
        }
        if (lang_1.isPresent(p.provider1) && p.provider1.key.token === query.selector) {
            if (i.obj1 === injector_1.UNDEFINED)
                i.obj1 = i.instantiateProvider(p.provider1, p.visibility1);
            list.push(i.obj1);
        }
        if (lang_1.isPresent(p.provider2) && p.provider2.key.token === query.selector) {
            if (i.obj2 === injector_1.UNDEFINED)
                i.obj2 = i.instantiateProvider(p.provider2, p.visibility2);
            list.push(i.obj2);
        }
        if (lang_1.isPresent(p.provider3) && p.provider3.key.token === query.selector) {
            if (i.obj3 === injector_1.UNDEFINED)
                i.obj3 = i.instantiateProvider(p.provider3, p.visibility3);
            list.push(i.obj3);
        }
        if (lang_1.isPresent(p.provider4) && p.provider4.key.token === query.selector) {
            if (i.obj4 === injector_1.UNDEFINED)
                i.obj4 = i.instantiateProvider(p.provider4, p.visibility4);
            list.push(i.obj4);
        }
        if (lang_1.isPresent(p.provider5) && p.provider5.key.token === query.selector) {
            if (i.obj5 === injector_1.UNDEFINED)
                i.obj5 = i.instantiateProvider(p.provider5, p.visibility5);
            list.push(i.obj5);
        }
        if (lang_1.isPresent(p.provider6) && p.provider6.key.token === query.selector) {
            if (i.obj6 === injector_1.UNDEFINED)
                i.obj6 = i.instantiateProvider(p.provider6, p.visibility6);
            list.push(i.obj6);
        }
        if (lang_1.isPresent(p.provider7) && p.provider7.key.token === query.selector) {
            if (i.obj7 === injector_1.UNDEFINED)
                i.obj7 = i.instantiateProvider(p.provider7, p.visibility7);
            list.push(i.obj7);
        }
        if (lang_1.isPresent(p.provider8) && p.provider8.key.token === query.selector) {
            if (i.obj8 === injector_1.UNDEFINED)
                i.obj8 = i.instantiateProvider(p.provider8, p.visibility8);
            list.push(i.obj8);
        }
        if (lang_1.isPresent(p.provider9) && p.provider9.key.token === query.selector) {
            if (i.obj9 === injector_1.UNDEFINED)
                i.obj9 = i.instantiateProvider(p.provider9, p.visibility9);
            list.push(i.obj9);
        }
    };
    return ElementDirectiveInlineStrategy;
})();
/**
 * Strategy used by the `ElementInjector` when the number of bindings is 11 or more.
 * In such a case, there are too many fields to inline (see ElementInjectorInlineStrategy).
 */
var ElementDirectiveDynamicStrategy = (function () {
    function ElementDirectiveDynamicStrategy(injectorStrategy, _ei) {
        this.injectorStrategy = injectorStrategy;
        this._ei = _ei;
    }
    ElementDirectiveDynamicStrategy.prototype.init = function () {
        var inj = this.injectorStrategy;
        var p = inj.protoStrategy;
        inj.resetConstructionCounter();
        for (var i = 0; i < p.keyIds.length; i++) {
            if (p.providers[i] instanceof DirectiveProvider && lang_1.isPresent(p.keyIds[i]) &&
                inj.objs[i] === injector_1.UNDEFINED) {
                inj.objs[i] = inj.instantiateProvider(p.providers[i], p.visibilities[i]);
            }
        }
    };
    ElementDirectiveDynamicStrategy.prototype.getComponent = function () { return this.injectorStrategy.objs[0]; };
    ElementDirectiveDynamicStrategy.prototype.isComponentKey = function (key) {
        var p = this.injectorStrategy.protoStrategy;
        return this._ei.proto.firstProviderIsComponent && lang_1.isPresent(key) && key.id === p.keyIds[0];
    };
    ElementDirectiveDynamicStrategy.prototype.addDirectivesMatchingQuery = function (query, list) {
        var ist = this.injectorStrategy;
        var p = ist.protoStrategy;
        for (var i = 0; i < p.providers.length; i++) {
            if (p.providers[i].key.token === query.selector) {
                if (ist.objs[i] === injector_1.UNDEFINED) {
                    ist.objs[i] = ist.instantiateProvider(p.providers[i], p.visibilities[i]);
                }
                list.push(ist.objs[i]);
            }
        }
    };
    return ElementDirectiveDynamicStrategy;
})();
var ProtoQueryRef = (function () {
    function ProtoQueryRef(dirIndex, setter, query) {
        this.dirIndex = dirIndex;
        this.setter = setter;
        this.query = query;
    }
    Object.defineProperty(ProtoQueryRef.prototype, "usesPropertySyntax", {
        get: function () { return lang_1.isPresent(this.setter); },
        enumerable: true,
        configurable: true
    });
    return ProtoQueryRef;
})();
exports.ProtoQueryRef = ProtoQueryRef;
var QueryRef = (function () {
    function QueryRef(protoQueryRef, originator) {
        this.protoQueryRef = protoQueryRef;
        this.originator = originator;
        this.list = new query_list_1.QueryList();
        this.dirty = true;
    }
    Object.defineProperty(QueryRef.prototype, "isViewQuery", {
        get: function () { return this.protoQueryRef.query.isViewQuery; },
        enumerable: true,
        configurable: true
    });
    QueryRef.prototype.update = function () {
        if (!this.dirty)
            return;
        this._update();
        this.dirty = false;
        // TODO delete the check once only field queries are supported
        if (this.protoQueryRef.usesPropertySyntax) {
            var dir = this.originator.getDirectiveAtIndex(this.protoQueryRef.dirIndex);
            if (this.protoQueryRef.query.first) {
                this.protoQueryRef.setter(dir, this.list.length > 0 ? this.list.first : null);
            }
            else {
                this.protoQueryRef.setter(dir, this.list);
            }
        }
        this.list.notifyOnChanges();
    };
    QueryRef.prototype._update = function () {
        var aggregator = [];
        if (this.protoQueryRef.query.isViewQuery) {
            // intentionally skipping originator for view queries.
            var nestedView = this.originator.componentView;
            if (lang_1.isPresent(nestedView))
                this._visitView(nestedView, aggregator);
        }
        else {
            this._visit(this.originator, aggregator);
        }
        this.list.reset(aggregator);
    };
    ;
    QueryRef.prototype._visit = function (inj, aggregator) {
        var view = inj.parentView;
        var startIdx = inj.proto.index;
        for (var i = startIdx; i < view.appElements.length; i++) {
            var curInj = view.appElements[i];
            // The first injector after inj, that is outside the subtree rooted at
            // inj has to have a null parent or a parent that is an ancestor of inj.
            if (i > startIdx && (lang_1.isBlank(curInj.parent) || curInj.parent.proto.index < startIdx)) {
                break;
            }
            if (!this.protoQueryRef.query.descendants &&
                !(curInj.parent == this.originator || curInj == this.originator))
                continue;
            // We visit the view container(VC) views right after the injector that contains
            // the VC. Theoretically, that might not be the right order if there are
            // child injectors of said injector. Not clear whether if such case can
            // even be constructed with the current apis.
            this._visitInjector(curInj, aggregator);
            this._visitViewContainerViews(curInj.nestedViews, aggregator);
        }
    };
    QueryRef.prototype._visitInjector = function (inj, aggregator) {
        if (this.protoQueryRef.query.isVarBindingQuery) {
            this._aggregateVariableBinding(inj, aggregator);
        }
        else {
            this._aggregateDirective(inj, aggregator);
        }
    };
    QueryRef.prototype._visitViewContainerViews = function (views, aggregator) {
        if (lang_1.isPresent(views)) {
            for (var j = 0; j < views.length; j++) {
                this._visitView(views[j], aggregator);
            }
        }
    };
    QueryRef.prototype._visitView = function (view, aggregator) {
        for (var i = 0; i < view.appElements.length; i++) {
            var inj = view.appElements[i];
            this._visitInjector(inj, aggregator);
            this._visitViewContainerViews(inj.nestedViews, aggregator);
        }
    };
    QueryRef.prototype._aggregateVariableBinding = function (inj, aggregator) {
        var vb = this.protoQueryRef.query.varBindings;
        for (var i = 0; i < vb.length; ++i) {
            if (inj.hasVariableBinding(vb[i])) {
                aggregator.push(inj.getVariableBinding(vb[i]));
            }
        }
    };
    QueryRef.prototype._aggregateDirective = function (inj, aggregator) {
        inj.addDirectivesMatchingQuery(this.protoQueryRef.query, aggregator);
    };
    return QueryRef;
})();
exports.QueryRef = QueryRef;
var _ComponentViewChangeDetectorRef = (function (_super) {
    __extends(_ComponentViewChangeDetectorRef, _super);
    function _ComponentViewChangeDetectorRef(_appElement) {
        _super.call(this);
        this._appElement = _appElement;
    }
    _ComponentViewChangeDetectorRef.prototype.markForCheck = function () { this._appElement.componentView.changeDetector.ref.markForCheck(); };
    _ComponentViewChangeDetectorRef.prototype.detach = function () { this._appElement.componentView.changeDetector.ref.detach(); };
    _ComponentViewChangeDetectorRef.prototype.detectChanges = function () { this._appElement.componentView.changeDetector.ref.detectChanges(); };
    _ComponentViewChangeDetectorRef.prototype.checkNoChanges = function () { this._appElement.componentView.changeDetector.ref.checkNoChanges(); };
    _ComponentViewChangeDetectorRef.prototype.reattach = function () { this._appElement.componentView.changeDetector.ref.reattach(); };
    return _ComponentViewChangeDetectorRef;
})(change_detection_1.ChangeDetectorRef);
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZWxlbWVudC5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbImFuZ3VsYXIyL3NyYy9jb3JlL2xpbmtlci9lbGVtZW50LnRzIl0sIm5hbWVzIjpbIlN0YXRpY0tleXMiLCJTdGF0aWNLZXlzLmNvbnN0cnVjdG9yIiwiU3RhdGljS2V5cy5pbnN0YW5jZSIsIkRpcmVjdGl2ZURlcGVuZGVuY3kiLCJEaXJlY3RpdmVEZXBlbmRlbmN5LmNvbnN0cnVjdG9yIiwiRGlyZWN0aXZlRGVwZW5kZW5jeS5fdmVyaWZ5IiwiRGlyZWN0aXZlRGVwZW5kZW5jeS5jcmVhdGVGcm9tIiwiRGlyZWN0aXZlRGVwZW5kZW5jeS5fYXR0cmlidXRlTmFtZSIsIkRpcmVjdGl2ZURlcGVuZGVuY3kuX3F1ZXJ5IiwiRGlyZWN0aXZlUHJvdmlkZXIiLCJEaXJlY3RpdmVQcm92aWRlci5jb25zdHJ1Y3RvciIsIkRpcmVjdGl2ZVByb3ZpZGVyLmRpc3BsYXlOYW1lIiwiRGlyZWN0aXZlUHJvdmlkZXIuY3JlYXRlRnJvbVR5cGUiLCJRdWVyeU1ldGFkYXRhV2l0aFNldHRlciIsIlF1ZXJ5TWV0YWRhdGFXaXRoU2V0dGVyLmNvbnN0cnVjdG9yIiwic2V0UHJvdmlkZXJzVmlzaWJpbGl0eSIsIkFwcFByb3RvRWxlbWVudCIsIkFwcFByb3RvRWxlbWVudC5jb25zdHJ1Y3RvciIsIkFwcFByb3RvRWxlbWVudC5jcmVhdGUiLCJBcHBQcm90b0VsZW1lbnQuZ2V0UHJvdmlkZXJBdEluZGV4IiwiX0NvbnRleHQiLCJfQ29udGV4dC5jb25zdHJ1Y3RvciIsIkluamVjdG9yV2l0aEhvc3RCb3VuZGFyeSIsIkluamVjdG9yV2l0aEhvc3RCb3VuZGFyeS5jb25zdHJ1Y3RvciIsIkFwcEVsZW1lbnQiLCJBcHBFbGVtZW50LmNvbnN0cnVjdG9yIiwiQXBwRWxlbWVudC5nZXRWaWV3UGFyZW50SW5qZWN0b3IiLCJBcHBFbGVtZW50LmF0dGFjaENvbXBvbmVudFZpZXciLCJBcHBFbGVtZW50Ll9kZWJ1Z0NvbnRleHQiLCJBcHBFbGVtZW50Lmhhc1ZhcmlhYmxlQmluZGluZyIsIkFwcEVsZW1lbnQuZ2V0VmFyaWFibGVCaW5kaW5nIiwiQXBwRWxlbWVudC5nZXQiLCJBcHBFbGVtZW50Lmhhc0RpcmVjdGl2ZSIsIkFwcEVsZW1lbnQuZ2V0Q29tcG9uZW50IiwiQXBwRWxlbWVudC5nZXRJbmplY3RvciIsIkFwcEVsZW1lbnQuZ2V0RWxlbWVudFJlZiIsIkFwcEVsZW1lbnQuZ2V0Vmlld0NvbnRhaW5lclJlZiIsIkFwcEVsZW1lbnQuZ2V0VGVtcGxhdGVSZWYiLCJBcHBFbGVtZW50LmdldERlcGVuZGVuY3kiLCJBcHBFbGVtZW50Ll9idWlsZEF0dHJpYnV0ZSIsIkFwcEVsZW1lbnQuYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkiLCJBcHBFbGVtZW50Ll9idWlsZFF1ZXJ5U3RyYXRlZ3kiLCJBcHBFbGVtZW50LmdldERpcmVjdGl2ZUF0SW5kZXgiLCJBcHBFbGVtZW50Lm5nQWZ0ZXJWaWV3Q2hlY2tlZCIsIkFwcEVsZW1lbnQubmdBZnRlckNvbnRlbnRDaGVja2VkIiwiQXBwRWxlbWVudC50cmF2ZXJzZUFuZFNldFF1ZXJpZXNBc0RpcnR5IiwiQXBwRWxlbWVudC5fc2V0UXVlcmllc0FzRGlydHkiLCJfRW1wdHlRdWVyeVN0cmF0ZWd5IiwiX0VtcHR5UXVlcnlTdHJhdGVneS5jb25zdHJ1Y3RvciIsIl9FbXB0eVF1ZXJ5U3RyYXRlZ3kuc2V0Q29udGVudFF1ZXJpZXNBc0RpcnR5IiwiX0VtcHR5UXVlcnlTdHJhdGVneS5zZXRWaWV3UXVlcmllc0FzRGlydHkiLCJfRW1wdHlRdWVyeVN0cmF0ZWd5LnVwZGF0ZUNvbnRlbnRRdWVyaWVzIiwiX0VtcHR5UXVlcnlTdHJhdGVneS51cGRhdGVWaWV3UXVlcmllcyIsIl9FbXB0eVF1ZXJ5U3RyYXRlZ3kuZmluZFF1ZXJ5IiwiSW5saW5lUXVlcnlTdHJhdGVneSIsIklubGluZVF1ZXJ5U3RyYXRlZ3kuY29uc3RydWN0b3IiLCJJbmxpbmVRdWVyeVN0cmF0ZWd5LnNldENvbnRlbnRRdWVyaWVzQXNEaXJ0eSIsIklubGluZVF1ZXJ5U3RyYXRlZ3kuc2V0Vmlld1F1ZXJpZXNBc0RpcnR5IiwiSW5saW5lUXVlcnlTdHJhdGVneS51cGRhdGVDb250ZW50UXVlcmllcyIsIklubGluZVF1ZXJ5U3RyYXRlZ3kudXBkYXRlVmlld1F1ZXJpZXMiLCJJbmxpbmVRdWVyeVN0cmF0ZWd5LmZpbmRRdWVyeSIsIkR5bmFtaWNRdWVyeVN0cmF0ZWd5IiwiRHluYW1pY1F1ZXJ5U3RyYXRlZ3kuY29uc3RydWN0b3IiLCJEeW5hbWljUXVlcnlTdHJhdGVneS5zZXRDb250ZW50UXVlcmllc0FzRGlydHkiLCJEeW5hbWljUXVlcnlTdHJhdGVneS5zZXRWaWV3UXVlcmllc0FzRGlydHkiLCJEeW5hbWljUXVlcnlTdHJhdGVneS51cGRhdGVDb250ZW50UXVlcmllcyIsIkR5bmFtaWNRdWVyeVN0cmF0ZWd5LnVwZGF0ZVZpZXdRdWVyaWVzIiwiRHluYW1pY1F1ZXJ5U3RyYXRlZ3kuZmluZFF1ZXJ5IiwiRWxlbWVudERpcmVjdGl2ZUlubGluZVN0cmF0ZWd5IiwiRWxlbWVudERpcmVjdGl2ZUlubGluZVN0cmF0ZWd5LmNvbnN0cnVjdG9yIiwiRWxlbWVudERpcmVjdGl2ZUlubGluZVN0cmF0ZWd5LmluaXQiLCJFbGVtZW50RGlyZWN0aXZlSW5saW5lU3RyYXRlZ3kuZ2V0Q29tcG9uZW50IiwiRWxlbWVudERpcmVjdGl2ZUlubGluZVN0cmF0ZWd5LmlzQ29tcG9uZW50S2V5IiwiRWxlbWVudERpcmVjdGl2ZUlubGluZVN0cmF0ZWd5LmFkZERpcmVjdGl2ZXNNYXRjaGluZ1F1ZXJ5IiwiRWxlbWVudERpcmVjdGl2ZUR5bmFtaWNTdHJhdGVneSIsIkVsZW1lbnREaXJlY3RpdmVEeW5hbWljU3RyYXRlZ3kuY29uc3RydWN0b3IiLCJFbGVtZW50RGlyZWN0aXZlRHluYW1pY1N0cmF0ZWd5LmluaXQiLCJFbGVtZW50RGlyZWN0aXZlRHluYW1pY1N0cmF0ZWd5LmdldENvbXBvbmVudCIsIkVsZW1lbnREaXJlY3RpdmVEeW5hbWljU3RyYXRlZ3kuaXNDb21wb25lbnRLZXkiLCJFbGVtZW50RGlyZWN0aXZlRHluYW1pY1N0cmF0ZWd5LmFkZERpcmVjdGl2ZXNNYXRjaGluZ1F1ZXJ5IiwiUHJvdG9RdWVyeVJlZiIsIlByb3RvUXVlcnlSZWYuY29uc3RydWN0b3IiLCJQcm90b1F1ZXJ5UmVmLnVzZXNQcm9wZXJ0eVN5bnRheCIsIlF1ZXJ5UmVmIiwiUXVlcnlSZWYuY29uc3RydWN0b3IiLCJRdWVyeVJlZi5pc1ZpZXdRdWVyeSIsIlF1ZXJ5UmVmLnVwZGF0ZSIsIlF1ZXJ5UmVmLl91cGRhdGUiLCJRdWVyeVJlZi5fdmlzaXQiLCJRdWVyeVJlZi5fdmlzaXRJbmplY3RvciIsIlF1ZXJ5UmVmLl92aXNpdFZpZXdDb250YWluZXJWaWV3cyIsIlF1ZXJ5UmVmLl92aXNpdFZpZXciLCJRdWVyeVJlZi5fYWdncmVnYXRlVmFyaWFibGVCaW5kaW5nIiwiUXVlcnlSZWYuX2FnZ3JlZ2F0ZURpcmVjdGl2ZSIsIl9Db21wb25lbnRWaWV3Q2hhbmdlRGV0ZWN0b3JSZWYiLCJfQ29tcG9uZW50Vmlld0NoYW5nZURldGVjdG9yUmVmLmNvbnN0cnVjdG9yIiwiX0NvbXBvbmVudFZpZXdDaGFuZ2VEZXRlY3RvclJlZi5tYXJrRm9yQ2hlY2siLCJfQ29tcG9uZW50Vmlld0NoYW5nZURldGVjdG9yUmVmLmRldGFjaCIsIl9Db21wb25lbnRWaWV3Q2hhbmdlRGV0ZWN0b3JSZWYuZGV0ZWN0Q2hhbmdlcyIsIl9Db21wb25lbnRWaWV3Q2hhbmdlRGV0ZWN0b3JSZWYuY2hlY2tOb0NoYW5nZXMiLCJfQ29tcG9uZW50Vmlld0NoYW5nZURldGVjdG9yUmVmLnJlYXR0YWNoIl0sIm1hcHBpbmdzIjoiOzs7OztBQUFBLHFCQU9PLDBCQUEwQixDQUFDLENBQUE7QUFDbEMsMkJBQTRCLGdDQUFnQyxDQUFDLENBQUE7QUFDN0QsMkJBQXdELGdDQUFnQyxDQUFDLENBQUE7QUFDekYsbUJBWU8sc0JBQXNCLENBQUMsQ0FBQTtBQUM5Qix5QkFBcUMsK0JBQStCLENBQUMsQ0FBQTtBQUNyRSx5QkFRTywrQkFBK0IsQ0FBQyxDQUFBO0FBQ3ZDLHlCQUFrRSwrQkFBK0IsQ0FBQyxDQUFBO0FBRWxHLG1CQUErQyxnQkFBZ0IsQ0FBQyxDQUFBO0FBR2hFLDBCQUF1QixhQUFhLENBQUMsQ0FBQTtBQUNyQyw0QkFBMEIsZUFBZSxDQUFDLENBQUE7QUFFMUMsbUNBQStCLHNCQUFzQixDQUFDLENBQUE7QUFDdEQsNEJBQXlCLGVBQWUsQ0FBQyxDQUFBO0FBQ3pDLG9CQUF1Qiw4QkFBOEIsQ0FBQyxDQUFBO0FBQ3RELDZCQUF3QyxnQkFBZ0IsQ0FBQyxDQUFBO0FBQ3pELDJCQUFtRCx3QkFBd0IsQ0FBQyxDQUFBO0FBQzVFLGlDQUdPLHFEQUFxRCxDQUFDLENBQUE7QUFDN0QsMkJBQXdCLGNBQWMsQ0FBQyxDQUFBO0FBQ3ZDLDJCQUF3Qix5Q0FBeUMsQ0FBQyxDQUFBO0FBR2xFLDhCQUEyQix1Q0FBdUMsQ0FBQyxDQUFBO0FBRW5FLG1DQUFnQyxzQkFBc0IsQ0FBQyxDQUFBO0FBR3ZELElBQUksV0FBVyxDQUFDO0FBRWhCO0lBT0VBO1FBQ0VDLElBQUlBLENBQUNBLGFBQWFBLEdBQUdBLFFBQUdBLENBQUNBLEdBQUdBLENBQUNBLDBCQUFXQSxDQUFDQSxDQUFDQSxFQUFFQSxDQUFDQTtRQUM3Q0EsSUFBSUEsQ0FBQ0EsZUFBZUEsR0FBR0EsUUFBR0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EscUNBQWdCQSxDQUFDQSxDQUFDQSxFQUFFQSxDQUFDQTtRQUNwREEsSUFBSUEsQ0FBQ0EsbUJBQW1CQSxHQUFHQSxRQUFHQSxDQUFDQSxHQUFHQSxDQUFDQSxvQ0FBaUJBLENBQUNBLENBQUNBLEVBQUVBLENBQUNBO1FBQ3pEQSxJQUFJQSxDQUFDQSxZQUFZQSxHQUFHQSxRQUFHQSxDQUFDQSxHQUFHQSxDQUFDQSx3QkFBVUEsQ0FBQ0EsQ0FBQ0EsRUFBRUEsQ0FBQ0E7UUFDM0NBLElBQUlBLENBQUNBLFVBQVVBLEdBQUdBLFFBQUdBLENBQUNBLEdBQUdBLENBQUNBLGNBQVFBLENBQUNBLENBQUNBLEVBQUVBLENBQUNBO0lBQ3pDQSxDQUFDQTtJQUVNRCxtQkFBUUEsR0FBZkE7UUFDRUUsRUFBRUEsQ0FBQ0EsQ0FBQ0EsY0FBT0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0E7WUFBQ0EsV0FBV0EsR0FBR0EsSUFBSUEsVUFBVUEsRUFBRUEsQ0FBQ0E7UUFDekRBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBO0lBQ3JCQSxDQUFDQTtJQUNIRixpQkFBQ0E7QUFBREEsQ0FBQ0EsQUFuQkQsSUFtQkM7QUFuQlksa0JBQVUsYUFtQnRCLENBQUE7QUFFRDtJQUF5Q0csdUNBQVVBO0lBQ2pEQSw2QkFBWUEsR0FBUUEsRUFBRUEsUUFBaUJBLEVBQUVBLG9CQUE0QkEsRUFDekRBLG9CQUE0QkEsRUFBRUEsVUFBaUJBLEVBQVNBLGFBQXFCQSxFQUN0RUEsY0FBNkJBO1FBQzlDQyxrQkFBTUEsR0FBR0EsRUFBRUEsUUFBUUEsRUFBRUEsb0JBQW9CQSxFQUFFQSxvQkFBb0JBLEVBQUVBLFVBQVVBLENBQUNBLENBQUNBO1FBRlhBLGtCQUFhQSxHQUFiQSxhQUFhQSxDQUFRQTtRQUN0RUEsbUJBQWNBLEdBQWRBLGNBQWNBLENBQWVBO1FBRTlDQSxJQUFJQSxDQUFDQSxPQUFPQSxFQUFFQSxDQUFDQTtJQUNqQkEsQ0FBQ0E7SUFFREQsZ0JBQWdCQTtJQUNoQkEscUNBQU9BLEdBQVBBO1FBQ0VFLElBQUlBLEtBQUtBLEdBQUdBLENBQUNBLENBQUNBO1FBQ2RBLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxjQUFjQSxDQUFDQSxDQUFDQTtZQUFDQSxLQUFLQSxFQUFFQSxDQUFDQTtRQUM1Q0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLGFBQWFBLENBQUNBLENBQUNBO1lBQUNBLEtBQUtBLEVBQUVBLENBQUNBO1FBQzNDQSxFQUFFQSxDQUFDQSxDQUFDQSxLQUFLQSxHQUFHQSxDQUFDQSxDQUFDQTtZQUNaQSxNQUFNQSxJQUFJQSwwQkFBYUEsQ0FDbkJBLG9GQUFvRkEsQ0FBQ0EsQ0FBQ0E7SUFDOUZBLENBQUNBO0lBRU1GLDhCQUFVQSxHQUFqQkEsVUFBa0JBLENBQWFBO1FBQzdCRyxNQUFNQSxDQUFDQSxJQUFJQSxtQkFBbUJBLENBQzFCQSxDQUFDQSxDQUFDQSxHQUFHQSxFQUFFQSxDQUFDQSxDQUFDQSxRQUFRQSxFQUFFQSxDQUFDQSxDQUFDQSxvQkFBb0JBLEVBQUVBLENBQUNBLENBQUNBLG9CQUFvQkEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsVUFBVUEsRUFDL0VBLG1CQUFtQkEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsVUFBVUEsQ0FBQ0EsRUFBRUEsbUJBQW1CQSxDQUFDQSxNQUFNQSxDQUFDQSxDQUFDQSxDQUFDQSxVQUFVQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUNsR0EsQ0FBQ0E7SUFFREgsZ0JBQWdCQTtJQUNUQSxrQ0FBY0EsR0FBckJBLFVBQXNCQSxVQUFpQkE7UUFDckNJLElBQUlBLENBQUNBLEdBQXNCQSxVQUFVQSxDQUFDQSxJQUFJQSxDQUFDQSxVQUFBQSxDQUFDQSxJQUFJQSxPQUFBQSxDQUFDQSxZQUFZQSxzQkFBaUJBLEVBQTlCQSxDQUE4QkEsQ0FBQ0EsQ0FBQ0E7UUFDaEZBLE1BQU1BLENBQUNBLGdCQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxHQUFHQSxDQUFDQSxDQUFDQSxhQUFhQSxHQUFHQSxJQUFJQSxDQUFDQTtJQUMvQ0EsQ0FBQ0E7SUFFREosZ0JBQWdCQTtJQUNUQSwwQkFBTUEsR0FBYkEsVUFBY0EsVUFBaUJBO1FBQzdCSyxNQUFNQSxDQUFnQkEsVUFBVUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsVUFBQUEsQ0FBQ0EsSUFBSUEsT0FBQUEsQ0FBQ0EsWUFBWUEsa0JBQWFBLEVBQTFCQSxDQUEwQkEsQ0FBQ0EsQ0FBQ0E7SUFDekVBLENBQUNBO0lBQ0hMLDBCQUFDQTtBQUFEQSxDQUFDQSxBQWxDRCxFQUF5QyxlQUFVLEVBa0NsRDtBQWxDWSwyQkFBbUIsc0JBa0MvQixDQUFBO0FBRUQ7SUFBdUNNLHFDQUFpQkE7SUFDdERBLDJCQUFZQSxHQUFRQSxFQUFFQSxPQUFpQkEsRUFBRUEsSUFBa0JBLEVBQVNBLFdBQW9CQSxFQUNyRUEsU0FBNkJBLEVBQVNBLGFBQWlDQSxFQUN2RUEsT0FBa0NBO1FBQ25EQyxrQkFBTUEsR0FBR0EsRUFBRUEsQ0FBQ0EsSUFBSUEsMEJBQWVBLENBQUNBLE9BQU9BLEVBQUVBLElBQUlBLENBQUNBLENBQUNBLEVBQUVBLEtBQUtBLENBQUNBLENBQUNBO1FBSFVBLGdCQUFXQSxHQUFYQSxXQUFXQSxDQUFTQTtRQUNyRUEsY0FBU0EsR0FBVEEsU0FBU0EsQ0FBb0JBO1FBQVNBLGtCQUFhQSxHQUFiQSxhQUFhQSxDQUFvQkE7UUFDdkVBLFlBQU9BLEdBQVBBLE9BQU9BLENBQTJCQTtJQUVyREEsQ0FBQ0E7SUFFREQsc0JBQUlBLDBDQUFXQTthQUFmQSxjQUE0QkUsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7OztPQUFBRjtJQUVuREEsZ0NBQWNBLEdBQXJCQSxVQUFzQkEsSUFBVUEsRUFBRUEsSUFBdUJBO1FBQ3ZERyxJQUFJQSxRQUFRQSxHQUFHQSxJQUFJQSxhQUFRQSxDQUFDQSxJQUFJQSxFQUFFQSxFQUFDQSxRQUFRQSxFQUFFQSxJQUFJQSxFQUFDQSxDQUFDQSxDQUFDQTtRQUNwREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsY0FBT0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDbEJBLElBQUlBLEdBQUdBLElBQUlBLDhCQUFpQkEsRUFBRUEsQ0FBQ0E7UUFDakNBLENBQUNBO1FBQ0RBLElBQUlBLEVBQUVBLEdBQUdBLDBCQUFlQSxDQUFDQSxRQUFRQSxDQUFDQSxDQUFDQTtRQUNuQ0EsSUFBSUEsRUFBRUEsR0FBR0EsRUFBRUEsQ0FBQ0EsaUJBQWlCQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtRQUNqQ0EsSUFBSUEsSUFBSUEsR0FBMEJBLEVBQUVBLENBQUNBLFlBQVlBLENBQUNBLEdBQUdBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsVUFBVUEsQ0FBQ0EsQ0FBQ0E7UUFDdEZBLElBQUlBLFdBQVdBLEdBQUdBLElBQUlBLFlBQVlBLDhCQUFpQkEsQ0FBQ0E7UUFDcERBLElBQUlBLGlCQUFpQkEsR0FBR0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLFNBQVNBLENBQUNBLEdBQUdBLGFBQVFBLENBQUNBLE9BQU9BLENBQUNBLElBQUlBLENBQUNBLFNBQVNBLENBQUNBLEdBQUdBLElBQUlBLENBQUNBO1FBQzVGQSxJQUFJQSxxQkFBcUJBLEdBQUdBLElBQUlBLFlBQVlBLDhCQUFpQkEsSUFBSUEsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLGFBQWFBLENBQUNBO1lBQzlEQSxhQUFRQSxDQUFDQSxPQUFPQSxDQUFDQSxJQUFJQSxDQUFDQSxhQUFhQSxDQUFDQTtZQUNwQ0EsSUFBSUEsQ0FBQ0E7UUFDckNBLElBQUlBLE9BQU9BLEdBQUdBLEVBQUVBLENBQUNBO1FBQ2pCQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsT0FBT0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDNUJBLDZCQUFnQkEsQ0FBQ0EsT0FBT0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsT0FBT0EsRUFBRUEsVUFBQ0EsSUFBSUEsRUFBRUEsU0FBU0E7Z0JBQ3JEQSxJQUFJQSxNQUFNQSxHQUFHQSxzQkFBU0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0E7Z0JBQ3pDQSxPQUFPQSxDQUFDQSxJQUFJQSxDQUFDQSxJQUFJQSx1QkFBdUJBLENBQUNBLE1BQU1BLEVBQUVBLElBQUlBLENBQUNBLENBQUNBLENBQUNBO1lBQzFEQSxDQUFDQSxDQUFDQSxDQUFDQTtRQUNMQSxDQUFDQTtRQUNEQSx1Q0FBdUNBO1FBQ3ZDQSxzRUFBc0VBO1FBQ3RFQSxJQUFJQSxDQUFDQSxPQUFPQSxDQUFDQSxVQUFBQSxDQUFDQTtZQUNaQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsY0FBY0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQ2hDQSxPQUFPQSxDQUFDQSxJQUFJQSxDQUFDQSxJQUFJQSx1QkFBdUJBLENBQUNBLElBQUlBLEVBQUVBLENBQUNBLENBQUNBLGNBQWNBLENBQUNBLENBQUNBLENBQUNBO1lBQ3BFQSxDQUFDQTtRQUNIQSxDQUFDQSxDQUFDQSxDQUFDQTtRQUNIQSxNQUFNQSxDQUFDQSxJQUFJQSxpQkFBaUJBLENBQUNBLEVBQUVBLENBQUNBLEdBQUdBLEVBQUVBLEVBQUVBLENBQUNBLE9BQU9BLEVBQUVBLElBQUlBLEVBQUVBLFdBQVdBLEVBQUVBLGlCQUFpQkEsRUFDeERBLHFCQUFxQkEsRUFBRUEsT0FBT0EsQ0FBQ0EsQ0FBQ0E7SUFDL0RBLENBQUNBO0lBQ0hILHdCQUFDQTtBQUFEQSxDQUFDQSxBQXZDRCxFQUF1Qyw0QkFBaUIsRUF1Q3ZEO0FBdkNZLHlCQUFpQixvQkF1QzdCLENBQUE7QUFFRDtJQUNFSSxpQ0FBbUJBLE1BQWdCQSxFQUFTQSxRQUF1QkE7UUFBaERDLFdBQU1BLEdBQU5BLE1BQU1BLENBQVVBO1FBQVNBLGFBQVFBLEdBQVJBLFFBQVFBLENBQWVBO0lBQUdBLENBQUNBO0lBQ3pFRCw4QkFBQ0E7QUFBREEsQ0FBQ0EsQUFGRCxJQUVDO0FBRlksK0JBQXVCLDBCQUVuQyxDQUFBO0FBR0QsZ0NBQWdDLFNBQTZCLEVBQUUsVUFBc0IsRUFDckQsTUFBK0I7SUFDN0RFLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLENBQUNBLEdBQUdBLFNBQVNBLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBLEVBQUVBLEVBQUVBLENBQUNBO1FBQzFDQSxNQUFNQSxDQUFDQSxHQUFHQSxDQUFDQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxHQUFHQSxDQUFDQSxFQUFFQSxFQUFFQSxVQUFVQSxDQUFDQSxDQUFDQTtJQUM5Q0EsQ0FBQ0E7QUFDSEEsQ0FBQ0E7QUFFRDtJQWtERUMseUJBQW1CQSx3QkFBaUNBLEVBQVNBLEtBQWFBLEVBQ3ZEQSxVQUFtQ0EsRUFBRUEsSUFBOEJBLEVBQ25FQSxjQUErQkEsRUFDL0JBLHlCQUFrREE7UUFIbERDLDZCQUF3QkEsR0FBeEJBLHdCQUF3QkEsQ0FBU0E7UUFBU0EsVUFBS0EsR0FBTEEsS0FBS0EsQ0FBUUE7UUFDdkRBLGVBQVVBLEdBQVZBLFVBQVVBLENBQXlCQTtRQUNuQ0EsbUJBQWNBLEdBQWRBLGNBQWNBLENBQWlCQTtRQUMvQkEsOEJBQXlCQSxHQUF6QkEseUJBQXlCQSxDQUF5QkE7UUFDbkVBLElBQUlBLE1BQU1BLEdBQUdBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBO1FBQ3pCQSxFQUFFQSxDQUFDQSxDQUFDQSxNQUFNQSxHQUFHQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUNmQSxJQUFJQSxDQUFDQSxhQUFhQSxHQUFHQSxJQUFJQSx3QkFBYUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7UUFDL0NBLENBQUNBO1FBQUNBLElBQUlBLENBQUNBLENBQUNBO1lBQ05BLElBQUlBLENBQUNBLGFBQWFBLEdBQUdBLElBQUlBLENBQUNBO1lBQzFCQSxJQUFJQSxDQUFDQSxjQUFjQSxHQUFHQSxFQUFFQSxDQUFDQTtRQUMzQkEsQ0FBQ0E7SUFDSEEsQ0FBQ0E7SUExRE1ELHNCQUFNQSxHQUFiQSxVQUFjQSxhQUFvQ0EsRUFBRUEsS0FBYUEsRUFDbkRBLFVBQW1DQSxFQUFFQSxjQUFzQkEsRUFDM0RBLHlCQUFrREE7UUFDOURFLElBQUlBLG9CQUFvQkEsR0FBR0EsSUFBSUEsQ0FBQ0E7UUFDaENBLElBQUlBLGtCQUFrQkEsR0FBa0NBLElBQUlBLEdBQUdBLEVBQTRCQSxDQUFDQTtRQUM1RkEsSUFBSUEscUJBQXFCQSxHQUE0QkEsSUFBSUEsR0FBR0EsRUFBc0JBLENBQUNBO1FBQ25GQSxJQUFJQSxTQUFTQSxHQUFHQSx3QkFBV0EsQ0FBQ0Esa0JBQWtCQSxDQUFDQSxjQUFjQSxDQUFDQSxNQUFNQSxDQUFDQSxDQUFDQTtRQUV0RUEsSUFBSUEsY0FBY0EsR0FBR0EsRUFBRUEsQ0FBQ0E7UUFDeEJBLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLENBQUNBLEdBQUdBLGNBQWNBLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBLEVBQUVBLEVBQUVBLENBQUNBO1lBQy9DQSxJQUFJQSxXQUFXQSxHQUFHQSxhQUFhQSxDQUFDQSw0QkFBNEJBLENBQUNBLGNBQWNBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO1lBQ2hGQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxHQUFHQSxJQUFJQSxpQ0FBc0JBLENBQ3JDQSxXQUFXQSxFQUFFQSxXQUFXQSxDQUFDQSxXQUFXQSxHQUFHQSxxQkFBVUEsQ0FBQ0EsZ0JBQWdCQSxHQUFHQSxxQkFBVUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsQ0FBQ0E7WUFFNUZBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO2dCQUM1QkEsb0JBQW9CQSxHQUFHQSxXQUFXQSxDQUFDQTtZQUNyQ0EsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7Z0JBQ05BLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSxXQUFXQSxDQUFDQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtvQkFDckNBLGlDQUFzQkEsQ0FBQ0EsV0FBV0EsQ0FBQ0EsU0FBU0EsRUFBRUEsa0JBQWtCQSxDQUFDQSxDQUFDQTtvQkFDbEVBLHNCQUFzQkEsQ0FBQ0EsV0FBV0EsQ0FBQ0EsU0FBU0EsRUFBRUEscUJBQVVBLENBQUNBLE1BQU1BLEVBQUVBLHFCQUFxQkEsQ0FBQ0EsQ0FBQ0E7Z0JBQzFGQSxDQUFDQTtZQUNIQSxDQUFDQTtZQUNEQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsYUFBYUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQ3pDQSxpQ0FBc0JBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLEVBQUVBLGtCQUFrQkEsQ0FBQ0EsQ0FBQ0E7Z0JBQ3RFQSxzQkFBc0JBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLEVBQUVBLHFCQUFVQSxDQUFDQSxPQUFPQSxFQUM3Q0EscUJBQXFCQSxDQUFDQSxDQUFDQTtZQUNoREEsQ0FBQ0E7WUFDREEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsUUFBUUEsR0FBR0EsQ0FBQ0EsRUFBRUEsUUFBUUEsR0FBR0EsV0FBV0EsQ0FBQ0EsT0FBT0EsQ0FBQ0EsTUFBTUEsRUFBRUEsUUFBUUEsRUFBRUEsRUFBRUEsQ0FBQ0E7Z0JBQ3pFQSxJQUFJQSxDQUFDQSxHQUFHQSxXQUFXQSxDQUFDQSxPQUFPQSxDQUFDQSxRQUFRQSxDQUFDQSxDQUFDQTtnQkFDdENBLGNBQWNBLENBQUNBLElBQUlBLENBQUNBLElBQUlBLGFBQWFBLENBQUNBLENBQUNBLEVBQUVBLENBQUNBLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBLENBQUNBLFFBQVFBLENBQUNBLENBQUNBLENBQUNBO1lBQ2xFQSxDQUFDQTtRQUNIQSxDQUFDQTtRQUNEQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0Esb0JBQW9CQSxDQUFDQSxJQUFJQSxnQkFBU0EsQ0FBQ0Esb0JBQW9CQSxDQUFDQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUNqRkEsc0VBQXNFQTtZQUN0RUEsaUNBQXNCQSxDQUFDQSxvQkFBb0JBLENBQUNBLFNBQVNBLEVBQUVBLGtCQUFrQkEsQ0FBQ0EsQ0FBQ0E7WUFDM0VBLHNCQUFzQkEsQ0FBQ0Esb0JBQW9CQSxDQUFDQSxTQUFTQSxFQUFFQSxxQkFBVUEsQ0FBQ0EsTUFBTUEsRUFDakRBLHFCQUFxQkEsQ0FBQ0EsQ0FBQ0E7UUFDaERBLENBQUNBO1FBQ0RBLGtCQUFrQkEsQ0FBQ0EsT0FBT0EsQ0FBQ0EsVUFBQ0EsUUFBUUEsRUFBRUEsQ0FBQ0E7WUFDckNBLFNBQVNBLENBQUNBLElBQUlBLENBQ1ZBLElBQUlBLGlDQUFzQkEsQ0FBQ0EsUUFBUUEsRUFBRUEscUJBQXFCQSxDQUFDQSxHQUFHQSxDQUFDQSxRQUFRQSxDQUFDQSxHQUFHQSxDQUFDQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtRQUN4RkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7UUFFSEEsTUFBTUEsQ0FBQ0EsSUFBSUEsZUFBZUEsQ0FBQ0EsZ0JBQVNBLENBQUNBLG9CQUFvQkEsQ0FBQ0EsRUFBRUEsS0FBS0EsRUFBRUEsVUFBVUEsRUFBRUEsU0FBU0EsRUFDN0RBLGNBQWNBLEVBQUVBLHlCQUF5QkEsQ0FBQ0EsQ0FBQ0E7SUFDeEVBLENBQUNBO0lBZURGLDRDQUFrQkEsR0FBbEJBLFVBQW1CQSxLQUFhQSxJQUFTRyxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxhQUFhQSxDQUFDQSxrQkFBa0JBLENBQUNBLEtBQUtBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO0lBQ2pHSCxzQkFBQ0E7QUFBREEsQ0FBQ0EsQUFoRUQsSUFnRUM7QUFoRVksdUJBQWUsa0JBZ0UzQixDQUFBO0FBRUQ7SUFDRUksa0JBQW1CQSxPQUFZQSxFQUFTQSxnQkFBcUJBLEVBQVNBLFFBQWFBO1FBQWhFQyxZQUFPQSxHQUFQQSxPQUFPQSxDQUFLQTtRQUFTQSxxQkFBZ0JBLEdBQWhCQSxnQkFBZ0JBLENBQUtBO1FBQVNBLGFBQVFBLEdBQVJBLFFBQVFBLENBQUtBO0lBQUdBLENBQUNBO0lBQ3pGRCxlQUFDQTtBQUFEQSxDQUFDQSxBQUZELElBRUM7QUFFRDtJQUNFRSxrQ0FBbUJBLFFBQWtCQSxFQUFTQSxvQkFBNkJBO1FBQXhEQyxhQUFRQSxHQUFSQSxRQUFRQSxDQUFVQTtRQUFTQSx5QkFBb0JBLEdBQXBCQSxvQkFBb0JBLENBQVNBO0lBQUdBLENBQUNBO0lBQ2pGRCwrQkFBQ0E7QUFBREEsQ0FBQ0EsQUFGRCxJQUVDO0FBRlksZ0NBQXdCLDJCQUVwQyxDQUFBO0FBRUQ7SUFxREVFLG9CQUFtQkEsS0FBc0JBLEVBQVNBLFVBQW1CQSxFQUFTQSxNQUFrQkEsRUFDN0VBLGFBQWtCQSxFQUFTQSxtQkFBNkJBO1FBdEQ3RUMsaUJBNE9DQTtRQXZMb0JBLFVBQUtBLEdBQUxBLEtBQUtBLENBQWlCQTtRQUFTQSxlQUFVQSxHQUFWQSxVQUFVQSxDQUFTQTtRQUFTQSxXQUFNQSxHQUFOQSxNQUFNQSxDQUFZQTtRQUM3RUEsa0JBQWFBLEdBQWJBLGFBQWFBLENBQUtBO1FBQVNBLHdCQUFtQkEsR0FBbkJBLG1CQUFtQkEsQ0FBVUE7UUFUcEVBLGdCQUFXQSxHQUFjQSxJQUFJQSxDQUFDQTtRQUM5QkEsa0JBQWFBLEdBQVlBLElBQUlBLENBQUNBO1FBU25DQSxJQUFJQSxDQUFDQSxHQUFHQSxHQUFHQSxJQUFJQSx5QkFBV0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7UUFDakNBLElBQUlBLGNBQWNBLEdBQUdBLGdCQUFTQSxDQUFDQSxNQUFNQSxDQUFDQSxHQUFHQSxNQUFNQSxDQUFDQSxTQUFTQSxHQUFHQSxVQUFVQSxDQUFDQSxjQUFjQSxDQUFDQTtRQUN0RkEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLEtBQUtBLENBQUNBLGFBQWFBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO1lBQ3hDQSxJQUFJQSxVQUFVQSxDQUFDQTtZQUNmQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsZ0JBQVNBLENBQUNBLE1BQU1BLENBQUNBLEtBQUtBLENBQUNBLGFBQWFBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO2dCQUMvREEsVUFBVUEsR0FBR0EsS0FBS0EsQ0FBQ0E7WUFDckJBLENBQUNBO1lBQUNBLElBQUlBLENBQUNBLENBQUNBO2dCQUNOQSxVQUFVQSxHQUFHQSxVQUFVQSxDQUFDQSxvQkFBb0JBLENBQUNBO1lBQy9DQSxDQUFDQTtZQUNEQSxJQUFJQSxDQUFDQSxjQUFjQSxHQUFHQSxJQUFJQSxDQUFDQSxtQkFBbUJBLEVBQUVBLENBQUNBO1lBQ2pEQSxJQUFJQSxDQUFDQSxTQUFTQSxHQUFHQSxJQUFJQSxhQUFRQSxDQUFDQSxJQUFJQSxDQUFDQSxLQUFLQSxDQUFDQSxhQUFhQSxFQUFFQSxjQUFjQSxFQUFFQSxVQUFVQSxFQUFFQSxJQUFJQSxFQUMxREEsY0FBTUEsT0FBQUEsS0FBSUEsQ0FBQ0EsYUFBYUEsRUFBRUEsRUFBcEJBLENBQW9CQSxDQUFDQSxDQUFDQTtZQUUxREEsMEVBQTBFQTtZQUMxRUEsSUFBSUEsZ0JBQWdCQSxHQUFRQSxJQUFJQSxDQUFDQSxTQUFTQSxDQUFDQSxnQkFBZ0JBLENBQUNBO1lBQzVEQSxJQUFJQSxDQUFDQSxTQUFTQSxHQUFHQSxnQkFBZ0JBLFlBQVlBLGlDQUFzQkE7Z0JBQzlDQSxJQUFJQSw4QkFBOEJBLENBQUNBLGdCQUFnQkEsRUFBRUEsSUFBSUEsQ0FBQ0E7Z0JBQzFEQSxJQUFJQSwrQkFBK0JBLENBQUNBLGdCQUFnQkEsRUFBRUEsSUFBSUEsQ0FBQ0EsQ0FBQ0E7WUFDakZBLElBQUlBLENBQUNBLFNBQVNBLENBQUNBLElBQUlBLEVBQUVBLENBQUNBO1FBQ3hCQSxDQUFDQTtRQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtZQUNOQSxJQUFJQSxDQUFDQSxjQUFjQSxHQUFHQSxJQUFJQSxDQUFDQTtZQUMzQkEsSUFBSUEsQ0FBQ0EsU0FBU0EsR0FBR0EsY0FBY0EsQ0FBQ0E7WUFDaENBLElBQUlBLENBQUNBLFNBQVNBLEdBQUdBLElBQUlBLENBQUNBO1FBQ3hCQSxDQUFDQTtJQUNIQSxDQUFDQTtJQTlFTUQsZ0NBQXFCQSxHQUE1QkEsVUFBNkJBLGNBQXdCQSxFQUFFQSxtQkFBK0JBLEVBQ3pEQSw0QkFBZ0RBLEVBQ2hEQSxZQUFzQkE7UUFDakRFLElBQUlBLGNBQWNBLENBQUNBO1FBQ25CQSxJQUFJQSxvQkFBb0JBLENBQUNBO1FBQ3pCQSxNQUFNQSxDQUFDQSxDQUFDQSxjQUFjQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN2QkEsS0FBS0Esb0JBQVFBLENBQUNBLFNBQVNBO2dCQUNyQkEsY0FBY0EsR0FBR0EsbUJBQW1CQSxDQUFDQSxTQUFTQSxDQUFDQTtnQkFDL0NBLG9CQUFvQkEsR0FBR0EsSUFBSUEsQ0FBQ0E7Z0JBQzVCQSxLQUFLQSxDQUFDQTtZQUNSQSxLQUFLQSxvQkFBUUEsQ0FBQ0EsUUFBUUE7Z0JBQ3BCQSxjQUFjQSxHQUFHQSxnQkFBU0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxLQUFLQSxDQUFDQSxhQUFhQSxDQUFDQTtvQkFDOUNBLG1CQUFtQkEsQ0FBQ0EsU0FBU0EsQ0FBQ0EsTUFBTUE7b0JBQ3BDQSxtQkFBbUJBLENBQUNBLFNBQVNBLENBQUNBO2dCQUNuREEsb0JBQW9CQSxHQUFHQSxtQkFBbUJBLENBQUNBLFNBQVNBLENBQUNBLFlBQVlBLENBQUNBO2dCQUNsRUEsS0FBS0EsQ0FBQ0E7WUFDUkEsS0FBS0Esb0JBQVFBLENBQUNBLElBQUlBO2dCQUNoQkEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7b0JBQ25DQSx1Q0FBdUNBO29CQUN2Q0EsY0FBY0EsR0FBR0EsZ0JBQVNBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsS0FBS0EsQ0FBQ0EsYUFBYUEsQ0FBQ0E7d0JBQzlDQSxtQkFBbUJBLENBQUNBLFNBQVNBLENBQUNBLE1BQU1BO3dCQUNwQ0EsbUJBQW1CQSxDQUFDQSxTQUFTQSxDQUFDQTtvQkFDbkRBLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSw0QkFBNEJBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO3dCQUM1Q0EsSUFBSUEsaUNBQWlDQSxHQUFHQSw0QkFBNEJBLENBQUNBLEdBQUdBLENBQ3BFQSxVQUFBQSxDQUFDQSxJQUFJQSxPQUFBQSxJQUFJQSxpQ0FBc0JBLENBQUNBLENBQUNBLEVBQUVBLHFCQUFVQSxDQUFDQSxNQUFNQSxDQUFDQSxFQUFoREEsQ0FBZ0RBLENBQUNBLENBQUNBO3dCQUMzREEsa0VBQWtFQTt3QkFDbEVBLHFFQUFxRUE7d0JBQ3JFQSxpREFBaURBO3dCQUNqREEsY0FBY0EsR0FBR0EsSUFBSUEsYUFBUUEsQ0FBQ0EsSUFBSUEsd0JBQWFBLENBQUNBLGlDQUFpQ0EsQ0FBQ0EsRUFDcERBLGNBQWNBLEVBQUVBLElBQUlBLEVBQUVBLElBQUlBLEVBQUVBLElBQUlBLENBQUNBLENBQUNBO3dCQUNoRUEsb0JBQW9CQSxHQUFHQSxLQUFLQSxDQUFDQTtvQkFDL0JBLENBQUNBO29CQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTt3QkFDTkEsb0JBQW9CQSxHQUFHQSxtQkFBbUJBLENBQUNBLFNBQVNBLENBQUNBLFlBQVlBLENBQUNBO29CQUNwRUEsQ0FBQ0E7Z0JBQ0hBLENBQUNBO2dCQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtvQkFDTkEsWUFBWUE7b0JBQ1pBLGNBQWNBLEdBQUdBLFlBQVlBLENBQUNBO29CQUM5QkEsb0JBQW9CQSxHQUFHQSxJQUFJQSxDQUFDQTtnQkFDOUJBLENBQUNBO2dCQUNEQSxLQUFLQSxDQUFDQTtRQUNWQSxDQUFDQTtRQUNEQSxNQUFNQSxDQUFDQSxJQUFJQSx3QkFBd0JBLENBQUNBLGNBQWNBLEVBQUVBLG9CQUFvQkEsQ0FBQ0EsQ0FBQ0E7SUFDNUVBLENBQUNBO0lBc0NERix3Q0FBbUJBLEdBQW5CQSxVQUFvQkEsYUFBc0JBLElBQUlHLElBQUlBLENBQUNBLGFBQWFBLEdBQUdBLGFBQWFBLENBQUNBLENBQUNBLENBQUNBO0lBRTNFSCxrQ0FBYUEsR0FBckJBO1FBQ0VJLElBQUlBLENBQUNBLEdBQUdBLElBQUlBLENBQUNBLFVBQVVBLENBQUNBLGVBQWVBLENBQUNBLElBQUlBLEVBQUVBLElBQUlBLEVBQUVBLElBQUlBLENBQUNBLENBQUNBO1FBQzFEQSxNQUFNQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsR0FBR0EsSUFBSUEsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsT0FBT0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQWdCQSxFQUFFQSxDQUFDQSxDQUFDQSxRQUFRQSxDQUFDQSxHQUFHQSxJQUFJQSxDQUFDQTtJQUN2RkEsQ0FBQ0E7SUFFREosdUNBQWtCQSxHQUFsQkEsVUFBbUJBLElBQVlBO1FBQzdCSyxJQUFJQSxFQUFFQSxHQUFHQSxJQUFJQSxDQUFDQSxLQUFLQSxDQUFDQSx5QkFBeUJBLENBQUNBO1FBQzlDQSxNQUFNQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsRUFBRUEsQ0FBQ0EsSUFBSUEsNkJBQWdCQSxDQUFDQSxRQUFRQSxDQUFDQSxFQUFFQSxFQUFFQSxJQUFJQSxDQUFDQSxDQUFDQTtJQUM5REEsQ0FBQ0E7SUFFREwsdUNBQWtCQSxHQUFsQkEsVUFBbUJBLElBQVlBO1FBQzdCTSxJQUFJQSxLQUFLQSxHQUFHQSxJQUFJQSxDQUFDQSxLQUFLQSxDQUFDQSx5QkFBeUJBLENBQUNBLElBQUlBLENBQUNBLENBQUNBO1FBQ3ZEQSxNQUFNQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsS0FBS0EsQ0FBQ0EsR0FBR0EsSUFBSUEsQ0FBQ0EsbUJBQW1CQSxDQUFTQSxLQUFLQSxDQUFDQSxHQUFHQSxJQUFJQSxDQUFDQSxhQUFhQSxFQUFFQSxDQUFDQTtJQUMzRkEsQ0FBQ0E7SUFFRE4sd0JBQUdBLEdBQUhBLFVBQUlBLEtBQVVBLElBQVNPLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLFNBQVNBLENBQUNBLEdBQUdBLENBQUNBLEtBQUtBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO0lBRTFEUCxpQ0FBWUEsR0FBWkEsVUFBYUEsSUFBVUEsSUFBYVEsTUFBTUEsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLFNBQVNBLENBQUNBLFdBQVdBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO0lBRXpGUixpQ0FBWUEsR0FBWkEsY0FBc0JTLE1BQU1BLENBQUNBLGdCQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxTQUFTQSxDQUFDQSxHQUFHQSxJQUFJQSxDQUFDQSxTQUFTQSxDQUFDQSxZQUFZQSxFQUFFQSxHQUFHQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUVoR1QsZ0NBQVdBLEdBQVhBLGNBQTBCVSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUVsRFYsa0NBQWFBLEdBQWJBLGNBQThCVyxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxHQUFHQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUVoRFgsd0NBQW1CQSxHQUFuQkEsY0FBMENZLE1BQU1BLENBQUNBLElBQUlBLHNDQUFpQkEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7SUFFL0VaLG1DQUFjQSxHQUFkQTtRQUNFYSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN4Q0EsTUFBTUEsQ0FBQ0EsSUFBSUEsMkJBQVlBLENBQUNBLElBQUlBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBO1FBQ3BDQSxDQUFDQTtRQUNEQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQTtJQUNkQSxDQUFDQTtJQUVEYixrQ0FBYUEsR0FBYkEsVUFBY0EsUUFBa0JBLEVBQUVBLFFBQTBCQSxFQUFFQSxHQUFlQTtRQUMzRWMsRUFBRUEsQ0FBQ0EsQ0FBQ0EsUUFBUUEsWUFBWUEsaUJBQWlCQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUMxQ0EsSUFBSUEsTUFBTUEsR0FBd0JBLEdBQUdBLENBQUNBO1lBRXRDQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsYUFBYUEsQ0FBQ0EsQ0FBQ0E7Z0JBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLGVBQWVBLENBQUNBLE1BQU1BLENBQUNBLENBQUNBO1lBRXpFQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsQ0FBQ0E7Z0JBQ25DQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxjQUFjQSxDQUFDQSxTQUFTQSxDQUFDQSxNQUFNQSxDQUFDQSxjQUFjQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQTtZQUVuRUEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsRUFBRUEsS0FBS0EsVUFBVUEsQ0FBQ0EsUUFBUUEsRUFBRUEsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDaEVBLG9FQUFvRUE7Z0JBQ3BFQSw2REFBNkRBO2dCQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsS0FBS0EsQ0FBQ0Esd0JBQXdCQSxDQUFDQSxDQUFDQSxDQUFDQTtvQkFDeENBLG1EQUFtREE7b0JBQ25EQSx5QkFBeUJBO29CQUN6QkEsTUFBTUEsQ0FBQ0EsSUFBSUEsK0JBQStCQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtnQkFDbkRBLENBQUNBO2dCQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtvQkFDTkEsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsVUFBVUEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsR0FBR0EsQ0FBQ0E7Z0JBQzVDQSxDQUFDQTtZQUNIQSxDQUFDQTtZQUVEQSxFQUFFQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxHQUFHQSxDQUFDQSxFQUFFQSxLQUFLQSxVQUFVQSxDQUFDQSxRQUFRQSxFQUFFQSxDQUFDQSxZQUFZQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDekRBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLGFBQWFBLEVBQUVBLENBQUNBO1lBQzlCQSxDQUFDQTtZQUVEQSxFQUFFQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxHQUFHQSxDQUFDQSxFQUFFQSxLQUFLQSxVQUFVQSxDQUFDQSxRQUFRQSxFQUFFQSxDQUFDQSxlQUFlQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDNURBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLG1CQUFtQkEsRUFBRUEsQ0FBQ0E7WUFDcENBLENBQUNBO1lBRURBLEVBQUVBLENBQUNBLENBQUNBLE1BQU1BLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLEtBQUtBLFVBQVVBLENBQUNBLFFBQVFBLEVBQUVBLENBQUNBLGFBQWFBLENBQUNBLENBQUNBLENBQUNBO2dCQUMxREEsSUFBSUEsRUFBRUEsR0FBR0EsSUFBSUEsQ0FBQ0EsY0FBY0EsRUFBRUEsQ0FBQ0E7Z0JBQy9CQSxFQUFFQSxDQUFDQSxDQUFDQSxjQUFPQSxDQUFDQSxFQUFFQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxRQUFRQSxDQUFDQSxDQUFDQSxDQUFDQTtvQkFDcENBLE1BQU1BLElBQUlBLG9CQUFlQSxDQUFDQSxJQUFJQSxFQUFFQSxNQUFNQSxDQUFDQSxHQUFHQSxDQUFDQSxDQUFDQTtnQkFDOUNBLENBQUNBO2dCQUNEQSxNQUFNQSxDQUFDQSxFQUFFQSxDQUFDQTtZQUNaQSxDQUFDQTtZQUVEQSxFQUFFQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxHQUFHQSxDQUFDQSxFQUFFQSxLQUFLQSxVQUFVQSxDQUFDQSxRQUFRQSxFQUFFQSxDQUFDQSxVQUFVQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDdkRBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLFVBQVVBLENBQUNBLFFBQVFBLENBQUNBO1lBQ2xDQSxDQUFDQTtRQUVIQSxDQUFDQTtRQUFDQSxJQUFJQSxDQUFDQSxFQUFFQSxDQUFDQSxDQUFDQSxRQUFRQSxZQUFZQSw0QkFBWUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDNUNBLEVBQUVBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLEtBQUtBLFVBQVVBLENBQUNBLFFBQVFBLEVBQUVBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQzdEQSxvRUFBb0VBO2dCQUNwRUEsNkRBQTZEQTtnQkFDN0RBLEVBQUVBLENBQUNBLENBQUNBLElBQUlBLENBQUNBLEtBQUtBLENBQUNBLHdCQUF3QkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7b0JBQ3hDQSxtREFBbURBO29CQUNuREEseUJBQXlCQTtvQkFDekJBLE1BQU1BLENBQUNBLElBQUlBLCtCQUErQkEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7Z0JBQ25EQSxDQUFDQTtnQkFBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7b0JBQ05BLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLFVBQVVBLENBQUNBLGNBQWNBLENBQUNBO2dCQUN4Q0EsQ0FBQ0E7WUFDSEEsQ0FBQ0E7UUFDSEEsQ0FBQ0E7UUFFREEsTUFBTUEsQ0FBQ0Esb0JBQVNBLENBQUNBO0lBQ25CQSxDQUFDQTtJQUVPZCxvQ0FBZUEsR0FBdkJBLFVBQXdCQSxHQUF3QkE7UUFDOUNlLElBQUlBLFVBQVVBLEdBQUdBLElBQUlBLENBQUNBLEtBQUtBLENBQUNBLFVBQVVBLENBQUNBO1FBQ3ZDQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsVUFBVUEsQ0FBQ0EsSUFBSUEsNkJBQWdCQSxDQUFDQSxRQUFRQSxDQUFDQSxVQUFVQSxFQUFFQSxHQUFHQSxDQUFDQSxhQUFhQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN0RkEsTUFBTUEsQ0FBQ0EsVUFBVUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsYUFBYUEsQ0FBQ0EsQ0FBQ0E7UUFDdkNBLENBQUNBO1FBQUNBLElBQUlBLENBQUNBLENBQUNBO1lBQ05BLE1BQU1BLENBQUNBLElBQUlBLENBQUNBO1FBQ2RBLENBQUNBO0lBQ0hBLENBQUNBO0lBRURmLCtDQUEwQkEsR0FBMUJBLFVBQTJCQSxLQUFvQkEsRUFBRUEsSUFBV0E7UUFDMURnQixJQUFJQSxXQUFXQSxHQUFHQSxJQUFJQSxDQUFDQSxjQUFjQSxFQUFFQSxDQUFDQTtRQUN4Q0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsS0FBS0EsQ0FBQ0EsUUFBUUEsS0FBS0EsMEJBQVdBLElBQUlBLGdCQUFTQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUM3REEsSUFBSUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0E7UUFDekJBLENBQUNBO1FBQ0RBLEVBQUVBLENBQUNBLENBQUNBLElBQUlBLENBQUNBLFNBQVNBLElBQUlBLElBQUlBLENBQUNBLENBQUNBLENBQUNBO1lBQzNCQSxJQUFJQSxDQUFDQSxTQUFTQSxDQUFDQSwwQkFBMEJBLENBQUNBLEtBQUtBLEVBQUVBLElBQUlBLENBQUNBLENBQUNBO1FBQ3pEQSxDQUFDQTtJQUNIQSxDQUFDQTtJQUVPaEIsd0NBQW1CQSxHQUEzQkE7UUFDRWlCLEVBQUVBLENBQUNBLENBQUNBLElBQUlBLENBQUNBLEtBQUtBLENBQUNBLGNBQWNBLENBQUNBLE1BQU1BLEtBQUtBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO1lBQzNDQSxNQUFNQSxDQUFDQSxtQkFBbUJBLENBQUNBO1FBQzdCQSxDQUFDQTtRQUFDQSxJQUFJQSxDQUFDQSxFQUFFQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxLQUFLQSxDQUFDQSxjQUFjQSxDQUFDQSxNQUFNQTtZQUNoQ0EsbUJBQW1CQSxDQUFDQSwyQkFBMkJBLENBQUNBLENBQUNBLENBQUNBO1lBQzNEQSxNQUFNQSxDQUFDQSxJQUFJQSxtQkFBbUJBLENBQUNBLElBQUlBLENBQUNBLENBQUNBO1FBQ3ZDQSxDQUFDQTtRQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtZQUNOQSxNQUFNQSxDQUFDQSxJQUFJQSxvQkFBb0JBLENBQUNBLElBQUlBLENBQUNBLENBQUNBO1FBQ3hDQSxDQUFDQTtJQUNIQSxDQUFDQTtJQUdEakIsd0NBQW1CQSxHQUFuQkEsVUFBb0JBLEtBQWFBLElBQVNrQixNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxTQUFTQSxDQUFDQSxLQUFLQSxDQUFDQSxLQUFLQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUUvRWxCLHVDQUFrQkEsR0FBbEJBO1FBQ0VtQixFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsaUJBQWlCQSxFQUFFQSxDQUFDQTtJQUM5RUEsQ0FBQ0E7SUFFRG5CLDBDQUFxQkEsR0FBckJBO1FBQ0VvQixFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsY0FBY0EsQ0FBQ0Esb0JBQW9CQSxFQUFFQSxDQUFDQTtJQUNqRkEsQ0FBQ0E7SUFFRHBCLGlEQUE0QkEsR0FBNUJBO1FBQ0VxQixJQUFJQSxHQUFHQSxHQUFlQSxJQUFJQSxDQUFDQTtRQUMzQkEsT0FBT0EsZ0JBQVNBLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLENBQUNBO1lBQ3RCQSxHQUFHQSxDQUFDQSxrQkFBa0JBLEVBQUVBLENBQUNBO1lBQ3pCQSxFQUFFQSxDQUFDQSxDQUFDQSxjQUFPQSxDQUFDQSxHQUFHQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxVQUFVQSxDQUFDQSxLQUFLQSxDQUFDQSxJQUFJQSxLQUFLQSxvQkFBUUEsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQzNFQSxHQUFHQSxHQUFHQSxHQUFHQSxDQUFDQSxVQUFVQSxDQUFDQSxtQkFBbUJBLENBQUNBO1lBQzNDQSxDQUFDQTtZQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtnQkFDTkEsR0FBR0EsR0FBR0EsR0FBR0EsQ0FBQ0EsTUFBTUEsQ0FBQ0E7WUFDbkJBLENBQUNBO1FBQ0hBLENBQUNBO0lBQ0hBLENBQUNBO0lBRU9yQix1Q0FBa0JBLEdBQTFCQTtRQUNFc0IsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLGNBQWNBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO1lBQ25DQSxJQUFJQSxDQUFDQSxjQUFjQSxDQUFDQSx3QkFBd0JBLEVBQUVBLENBQUNBO1FBQ2pEQSxDQUFDQTtRQUNEQSxFQUFFQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxVQUFVQSxDQUFDQSxLQUFLQSxDQUFDQSxJQUFJQSxLQUFLQSxvQkFBUUEsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdERBLElBQUlBLENBQUNBLFVBQVVBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsY0FBY0EsQ0FBQ0EscUJBQXFCQSxFQUFFQSxDQUFDQTtRQUM3RUEsQ0FBQ0E7SUFDSEEsQ0FBQ0E7SUFDSHRCLGlCQUFDQTtBQUFEQSxDQUFDQSxBQTVPRCxJQTRPQztBQTVPWSxrQkFBVSxhQTRPdEIsQ0FBQTtBQVVEO0lBQUF1QjtJQVFBQyxDQUFDQTtJQVBDRCxzREFBd0JBLEdBQXhCQSxjQUFrQ0UsQ0FBQ0E7SUFDbkNGLG1EQUFxQkEsR0FBckJBLGNBQStCRyxDQUFDQTtJQUNoQ0gsa0RBQW9CQSxHQUFwQkEsY0FBOEJJLENBQUNBO0lBQy9CSiwrQ0FBaUJBLEdBQWpCQSxjQUEyQkssQ0FBQ0E7SUFDNUJMLHVDQUFTQSxHQUFUQSxVQUFVQSxLQUFvQkE7UUFDNUJNLE1BQU1BLElBQUlBLDBCQUFhQSxDQUFDQSxxQ0FBbUNBLEtBQUtBLE1BQUdBLENBQUNBLENBQUNBO0lBQ3ZFQSxDQUFDQTtJQUNITiwwQkFBQ0E7QUFBREEsQ0FBQ0EsQUFSRCxJQVFDO0FBRUQsSUFBSSxtQkFBbUIsR0FBRyxJQUFJLG1CQUFtQixFQUFFLENBQUM7QUFFcEQ7SUFPRU8sNkJBQVlBLEVBQWNBO1FBQ3hCQyxJQUFJQSxTQUFTQSxHQUFHQSxFQUFFQSxDQUFDQSxLQUFLQSxDQUFDQSxjQUFjQSxDQUFDQTtRQUN4Q0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsTUFBTUEsR0FBR0EsQ0FBQ0EsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsR0FBR0EsSUFBSUEsUUFBUUEsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsRUFBRUEsRUFBRUEsQ0FBQ0EsQ0FBQ0E7UUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLE1BQU1BLEdBQUdBLENBQUNBLENBQUNBO1lBQUNBLElBQUlBLENBQUNBLE1BQU1BLEdBQUdBLElBQUlBLFFBQVFBLENBQUNBLFNBQVNBLENBQUNBLENBQUNBLENBQUNBLEVBQUVBLEVBQUVBLENBQUNBLENBQUNBO1FBQ3ZFQSxFQUFFQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxNQUFNQSxHQUFHQSxDQUFDQSxDQUFDQTtZQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxHQUFHQSxJQUFJQSxRQUFRQSxDQUFDQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxFQUFFQSxFQUFFQSxDQUFDQSxDQUFDQTtJQUN6RUEsQ0FBQ0E7SUFFREQsc0RBQXdCQSxHQUF4QkE7UUFDRUUsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBO1lBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLEtBQUtBLEdBQUdBLElBQUlBLENBQUNBO1FBQ2pGQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsV0FBV0EsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsS0FBS0EsR0FBR0EsSUFBSUEsQ0FBQ0E7UUFDakZBLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxXQUFXQSxDQUFDQTtZQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxLQUFLQSxHQUFHQSxJQUFJQSxDQUFDQTtJQUNuRkEsQ0FBQ0E7SUFFREYsbURBQXFCQSxHQUFyQkE7UUFDRUcsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBO1lBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLEtBQUtBLEdBQUdBLElBQUlBLENBQUNBO1FBQ2hGQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsV0FBV0EsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsS0FBS0EsR0FBR0EsSUFBSUEsQ0FBQ0E7UUFDaEZBLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxXQUFXQSxDQUFDQTtZQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxLQUFLQSxHQUFHQSxJQUFJQSxDQUFDQTtJQUNsRkEsQ0FBQ0E7SUFFREgsa0RBQW9CQSxHQUFwQkE7UUFDRUksRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO1lBQ3ZEQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQTtRQUN2QkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO1lBQ3ZEQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQTtRQUN2QkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO1lBQ3ZEQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQTtRQUN2QkEsQ0FBQ0E7SUFDSEEsQ0FBQ0E7SUFFREosK0NBQWlCQSxHQUFqQkE7UUFDRUssRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO1lBQ3REQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQTtRQUN2QkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO1lBQ3REQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQTtRQUN2QkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO1lBQ3REQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQTtRQUN2QkEsQ0FBQ0E7SUFDSEEsQ0FBQ0E7SUFFREwsdUNBQVNBLEdBQVRBLFVBQVVBLEtBQW9CQTtRQUM1Qk0sRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLGFBQWFBLENBQUNBLEtBQUtBLEtBQUtBLEtBQUtBLENBQUNBLENBQUNBLENBQUNBO1lBQ3hFQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQTtRQUNyQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLGFBQWFBLENBQUNBLEtBQUtBLEtBQUtBLEtBQUtBLENBQUNBLENBQUNBLENBQUNBO1lBQ3hFQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQTtRQUNyQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLGFBQWFBLENB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QUNBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLG9CQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxnQkFBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsS0FBS0Esb0JBQVNBLENBQUNBO1lBQzFGQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1FBQzdEQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxZQUFZQSxpQkFBaUJBLElBQUlBLGdCQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxvQkFBU0EsQ0FBQ0E7WUFDMUZBLENBQUNBLENBQUNBLElBQUlBLEdBQUdBLENBQUNBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0E7UUFDN0RBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLFlBQVlBLGlCQUFpQkEsSUFBSUEsZ0JBQVNBLENBQUNBLENBQUNBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLG9CQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxnQkFBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsS0FBS0Esb0JBQVNBLENBQUNBO1lBQzFGQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1FBQzdEQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxZQUFZQSxpQkFBaUJBLElBQUlBLGdCQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxvQkFBU0EsQ0FBQ0E7WUFDMUZBLENBQUNBLENBQUNBLElBQUlBLEdBQUdBLENBQUNBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0E7UUFDN0RBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLFlBQVlBLGlCQUFpQkEsSUFBSUEsZ0JBQVNBLENBQUNBLENBQUNBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLG9CQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxnQkFBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsTUFBTUEsQ0FBQ0Es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QUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtZQUNyRkEsSUFBSUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7UUFDcEJBLENBQUNBO1FBQ0RBLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxHQUFHQSxDQUFDQSxLQUFLQSxLQUFLQSxLQUFLQSxDQUFDQSxRQUFRQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN2RUEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsSUFBSUEsS0FBS0Esb0JBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLEdBQUdBLENBQUNBLEtBQUtBLEtBQUtBLEtBQUtBLENBQUNBLFFBQVFBLENBQUNBLENBQUNBLENBQUNBO1lBQ3ZFQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxvQkFBU0EsQ0FBQ0E7Z0JBQUNBLENBQUNBLENBQUNBLElBQUlBLEdBQUdBLENBQUNBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0E7WUFDckZBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLENBQUNBLENBQUNBO1FBQ3BCQSxDQUFDQTtRQUNEQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLG9CQUFTQSxDQUFDQTtnQkFBQ0EsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtZQUNyRkEsSUFBSUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7UUFDcEJBLENBQUNBO1FBQ0RBLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxHQUFHQSxDQUFDQSxLQUFLQSxLQUFLQSxLQUFLQSxDQUFDQSxRQUFRQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN2RUEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsSUFBSUEsS0FBS0Esb0JBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLEdBQUdBLENBQUNBLEtBQUtBLEtBQUtBLEtBQUtBLENBQUNBLFFBQVFBLENBQUNBLENBQUNBLENBQUNBO1lBQ3ZFQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxvQkFBU0EsQ0FBQ0E7Z0JBQUNBLENBQUNBLENBQUNBLElBQUlBLEdBQUdBLENBQUNBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0E7WUFDckZBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLENBQUNBLENBQUNBO1FBQ3BCQSxDQUFDQTtRQUNEQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLG9CQUFTQSxDQUFDQTtnQkFBQ0EsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtZQUNyRkEsSUFBSUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7UUFDcEJBLENBQUNBO1FBQ0RBLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxHQUFHQSxDQUFDQSxLQUFLQSxLQUFLQSxLQUFLQSxDQUFDQSxRQUFRQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN2RUEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsSUFBSUEsS0FBS0Esb0JBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLEdBQUdBLENBQUNBLEtBQUtBLEtBQUtBLEtBQUtBLENBQUNBLFFBQVFBLENBQUNBLENBQUNBLENBQUNBO1lBQ3ZFQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxvQkFBU0EsQ0FBQ0E7Z0JBQUNBLENBQUNBLENBQUNBLElBQUlBLEdBQUdBLENBQUNBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0E7WUFDckZBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLENBQUNBLENBQUNBO1FBQ3BCQSxDQUFDQTtJQUNIQSxDQUFDQTtJQUNITCxxQ0FBQ0E7QUFBREEsQ0FBQ0EsQUFqRkQsSUFpRkM7QUFFRDs7O0dBR0c7QUFDSDtJQUNFTSx5Q0FBbUJBLGdCQUF5Q0EsRUFBU0EsR0FBZUE7UUFBakVDLHFCQUFnQkEsR0FBaEJBLGdCQUFnQkEsQ0FBeUJBO1FBQVNBLFFBQUdBLEdBQUhBLEdBQUdBLENBQVlBO0lBQUdBLENBQUNBO0lBRXhGRCw4Q0FBSUEsR0FBSkE7UUFDRUUsSUFBSUEsR0FBR0EsR0FBR0EsSUFBSUEsQ0FBQ0EsZ0JBQWdCQSxDQUFDQTtRQUNoQ0EsSUFBSUEsQ0FBQ0EsR0FBR0EsR0FBR0EsQ0FBQ0EsYUFBYUEsQ0FBQ0E7UUFDMUJBLEdBQUdBLENBQUNBLHdCQUF3QkEsRUFBRUEsQ0FBQ0E7UUFFL0JBLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBLE1BQU1BLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBLEVBQUVBLEVBQUVBLENBQUNBO1lBQ3pDQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxZQUFZQSxpQkFBaUJBLElBQUlBLGdCQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDckVBLEdBQUdBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLENBQUNBLEtBQUtBLG9CQUFTQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDOUJBLEdBQUdBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLENBQUNBLEdBQUdBLEdBQUdBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsWUFBWUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDM0VBLENBQUNBO1FBQ0hBLENBQUNBO0lBQ0hBLENBQUNBO0lBRURGLHNEQUFZQSxHQUFaQSxjQUFzQkcsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsZ0JBQWdCQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUU3REgsd0RBQWNBLEdBQWRBLFVBQWVBLEdBQVFBO1FBQ3JCSSxJQUFJQSxDQUFDQSxHQUFHQSxJQUFJQSxDQUFDQSxnQkFBZ0JBLENBQUNBLGFBQWFBLENBQUNBO1FBQzVDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxHQUFHQSxDQUFDQSxLQUFLQSxDQUFDQSx3QkFBd0JBLElBQUlBLGdCQUFTQSxDQUFDQSxHQUFHQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxFQUFFQSxLQUFLQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUM3RkEsQ0FBQ0E7SUFFREosb0VBQTBCQSxHQUExQkEsVUFBMkJBLEtBQW9CQSxFQUFFQSxJQUFXQTtRQUMxREssSUFBSUEsR0FBR0EsR0FBR0EsSUFBSUEsQ0FBQ0EsZ0JBQWdCQSxDQUFDQTtRQUNoQ0EsSUFBSUEsQ0FBQ0EsR0FBR0EsR0FBR0EsQ0FBQ0EsYUFBYUEsQ0FBQ0E7UUFFMUJBLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBLEVBQUVBLEVBQUVBLENBQUNBO1lBQzVDQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxHQUFHQSxDQUFDQSxLQUFLQSxLQUFLQSxLQUFLQSxDQUFDQSxRQUFRQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDaERBLEVBQUVBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLENBQUNBLEtBQUtBLG9CQUFTQSxDQUFDQSxDQUFDQSxDQUFDQTtvQkFDOUJBLEdBQUdBLENBQUNBLElBQUlBLENBQUNBLENBQUNBLENBQUNBLEdBQUdBLEdBQUdBLENBQUNBLG1CQUFtQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsWUFBWUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQzNFQSxDQUFDQTtnQkFDREEsSUFBSUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDekJBLENBQUNBO1FBQ0hBLENBQUNBO0lBQ0hBLENBQUNBO0lBQ0hMLHNDQUFDQTtBQUFEQSxDQUFDQSxBQXBDRCxJQW9DQztBQUVEO0lBQ0VNLHVCQUFtQkEsUUFBZ0JBLEVBQVNBLE1BQWdCQSxFQUFTQSxLQUFvQkE7UUFBdEVDLGFBQVFBLEdBQVJBLFFBQVFBLENBQVFBO1FBQVNBLFdBQU1BLEdBQU5BLE1BQU1BLENBQVVBO1FBQVNBLFVBQUtBLEdBQUxBLEtBQUtBLENBQWVBO0lBQUdBLENBQUNBO0lBRTdGRCxzQkFBSUEsNkNBQWtCQTthQUF0QkEsY0FBb0NFLE1BQU1BLENBQUNBLGdCQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTs7O09BQUFGO0lBQ3RFQSxvQkFBQ0E7QUFBREEsQ0FBQ0EsQUFKRCxJQUlDO0FBSlkscUJBQWEsZ0JBSXpCLENBQUE7QUFFRDtJQUlFRyxrQkFBbUJBLGFBQTRCQSxFQUFVQSxVQUFzQkE7UUFBNURDLGtCQUFhQSxHQUFiQSxhQUFhQSxDQUFlQTtRQUFVQSxlQUFVQSxHQUFWQSxVQUFVQSxDQUFZQTtRQUM3RUEsSUFBSUEsQ0FBQ0EsSUFBSUEsR0FBR0EsSUFBSUEsc0JBQVNBLEVBQU9BLENBQUNBO1FBQ2pDQSxJQUFJQSxDQUFDQSxLQUFLQSxHQUFHQSxJQUFJQSxDQUFDQTtJQUNwQkEsQ0FBQ0E7SUFFREQsc0JBQUlBLGlDQUFXQTthQUFmQSxjQUE2QkUsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsYUFBYUEsQ0FBQ0EsS0FBS0EsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7OztPQUFBRjtJQUUzRUEseUJBQU1BLEdBQU5BO1FBQ0VHLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLENBQUNBLEtBQUtBLENBQUNBO1lBQUNBLE1BQU1BLENBQUNBO1FBQ3hCQSxJQUFJQSxDQUFDQSxPQUFPQSxFQUFFQSxDQUFDQTtRQUNmQSxJQUFJQSxDQUFDQSxLQUFLQSxHQUFHQSxLQUFLQSxDQUFDQTtRQUVuQkEsOERBQThEQTtRQUM5REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsYUFBYUEsQ0FBQ0Esa0JBQWtCQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUMxQ0EsSUFBSUEsR0FBR0EsR0FBR0EsSUFBSUEsQ0FBQ0EsVUFBVUEsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxJQUFJQSxDQUFDQSxhQUFhQSxDQUFDQSxRQUFRQSxDQUFDQSxDQUFDQTtZQUMzRUEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsYUFBYUEsQ0FBQ0EsS0FBS0EsQ0FBQ0EsS0FBS0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQ25DQSxJQUFJQSxDQUFDQSxhQUFhQSxDQUFDQSxNQUFNQSxDQUFDQSxHQUFHQSxFQUFFQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxHQUFHQSxDQUFDQSxHQUFHQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxLQUFLQSxHQUFHQSxJQUFJQSxDQUFDQSxDQUFDQTtZQUNoRkEsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7Z0JBQ05BLElBQUlBLENBQUNBLGFBQWFBLENBQUNBLE1BQU1BLENBQUNBLEdBQUdBLEVBQUVBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLENBQUNBO1lBQzVDQSxDQUFDQTtRQUNIQSxDQUFDQTtRQUVEQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxlQUFlQSxFQUFFQSxDQUFDQTtJQUM5QkEsQ0FBQ0E7SUFFT0gsMEJBQU9BLEdBQWZBO1FBQ0VJLElBQUlBLFVBQVVBLEdBQUdBLEVBQUVBLENBQUNBO1FBQ3BCQSxFQUFFQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxhQUFhQSxDQUFDQSxLQUFLQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN6Q0Esc0RBQXNEQTtZQUN0REEsSUFBSUEsVUFBVUEsR0FBR0EsSUFBSUEsQ0FBQ0EsVUFBVUEsQ0FBQ0EsYUFBYUEsQ0FBQ0E7WUFDL0NBLEVBQUVBLENBQUNBLENBQUNBLGdCQUFTQSxDQUFDQSxVQUFVQSxDQUFDQSxDQUFDQTtnQkFBQ0EsSUFBSUEsQ0FBQ0EsVUFBVUEsQ0FBQ0EsVUFBVUEsRUFBRUEsVUFBVUEsQ0FBQ0EsQ0FBQ0E7UUFDckVBLENBQUNBO1FBQUNBLElBQUlBLENBQUNBLENBQUNBO1lBQ05BLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLFVBQVVBLEVBQUVBLFVBQVVBLENBQUNBLENBQUNBO1FBQzNDQSxDQUFDQTtRQUNEQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxLQUFLQSxDQUFDQSxVQUFVQSxDQUFDQSxDQUFDQTtJQUM5QkEsQ0FBQ0E7O0lBRU9KLHlCQUFNQSxHQUFkQSxVQUFlQSxHQUFlQSxFQUFFQSxVQUFpQkE7UUFDL0NLLElBQUlBLElBQUlBLEdBQUdBLEdBQUdBLENBQUNBLFVBQVVBLENBQUNBO1FBQzFCQSxJQUFJQSxRQUFRQSxHQUFHQSxHQUFHQSxDQUFDQSxLQUFLQSxDQUFDQSxLQUFLQSxDQUFDQTtRQUMvQkEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsQ0FBQ0EsR0FBR0EsUUFBUUEsRUFBRUEsQ0FBQ0EsR0FBR0EsSUFBSUEsQ0FBQ0EsV0FBV0EsQ0FBQ0EsTUFBTUEsRUFBRUEsQ0FBQ0EsRUFBRUEsRUFBRUEsQ0FBQ0E7WUFDeERBLElBQUlBLE1BQU1BLEdBQUdBLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO1lBQ2pDQSxzRUFBc0VBO1lBQ3RFQSx3RUFBd0VBO1lBQ3hFQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxHQUFHQSxRQUFRQSxJQUFJQSxDQUFDQSxjQUFPQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxNQUFNQSxDQUFDQSxNQUFNQSxDQUFDQSxLQUFLQSxDQUFDQSxLQUFLQSxHQUFHQSxRQUFRQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDckZBLEtBQUtBLENBQUNBO1lBQ1JBLENBQUNBO1lBRURBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLENBQUNBLGFBQWFBLENBQUNBLEtBQUtBLENBQUNBLFdBQVdBO2dCQUNyQ0EsQ0FBQ0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsTUFBTUEsSUFBSUEsSUFBSUEsQ0FBQ0EsVUFBVUEsSUFBSUEsTUFBTUEsSUFBSUEsSUFBSUEsQ0FBQ0EsVUFBVUEsQ0FBQ0EsQ0FBQ0E7Z0JBQ25FQSxRQUFRQSxDQUFDQTtZQUVYQSwrRUFBK0VBO1lBQy9FQSx3RUFBd0VBO1lBQ3hFQSx1RUFBdUVBO1lBQ3ZFQSw2Q0FBNkNBO1lBQzdDQSxJQUFJQSxDQUFDQSxjQUFjQSxDQUFDQSxNQUFNQSxFQUFFQSxVQUFVQSxDQUFDQSxDQUFDQTtZQUN4Q0EsSUFBSUEsQ0FBQ0Esd0JBQXdCQSxDQUFDQSxNQUFNQSxDQUFDQSxXQUFXQSxFQUFFQSxVQUFVQSxDQUFDQSxDQUFDQTtRQUNoRUEsQ0FBQ0E7SUFDSEEsQ0FBQ0E7SUFFT0wsaUNBQWNBLEdBQXRCQSxVQUF1QkEsR0FBZUEsRUFBRUEsVUFBaUJBO1FBQ3ZETSxFQUFFQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxhQUFhQSxDQUFDQSxLQUFLQSxDQUFDQSxpQkFBaUJBLENBQUNBLENBQUNBLENBQUNBO1lBQy9DQSxJQUFJQSxDQUFDQSx5QkFBeUJBLENBQUNBLEdBQUdBLEVBQUVBLFVBQVVBLENBQUNBLENBQUNBO1FBQ2xEQSxDQUFDQTtRQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtZQUNOQSxJQUFJQSxDQUFDQSxtQkFBbUJBLENBQUNBLEdBQUdBLEVBQUVBLFVBQVVBLENBQUNBLENBQUNBO1FBQzVDQSxDQUFDQTtJQUNIQSxDQUFDQTtJQUVPTiwyQ0FBd0JBLEdBQWhDQSxVQUFpQ0EsS0FBZ0JBLEVBQUVBLFVBQWlCQTtRQUNsRU8sRUFBRUEsQ0FBQ0EsQ0FBQ0EsZ0JBQVNBLENBQUNBLEtBQUtBLENBQUNBLENBQUNBLENBQUNBLENBQUNBO1lBQ3JCQSxHQUFHQSxDQUFDQSxDQUFDQSxHQUFHQSxDQUFDQSxDQUFDQSxHQUFHQSxDQUFDQSxFQUFFQSxDQUFDQSxHQUFHQSxLQUFLQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQSxFQUFFQSxFQUFFQSxDQUFDQTtnQkFDdENBLElBQUlBLENBQUNBLFVBQVVBLENBQUNBLEtBQUtBLENBQUNBLENBQUNBLENBQUNBLEVBQUVBLFVBQVVBLENBQUNBLENBQUNBO1lBQ3hDQSxDQUFDQTtRQUNIQSxDQUFDQTtJQUNIQSxDQUFDQTtJQUVPUCw2QkFBVUEsR0FBbEJBLFVBQW1CQSxJQUFhQSxFQUFFQSxVQUFpQkE7UUFDakRRLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLENBQUNBLEdBQUdBLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBLEVBQUVBLEVBQUVBLENBQUNBO1lBQ2pEQSxJQUFJQSxHQUFHQSxHQUFHQSxJQUFJQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUM5QkEsSUFBSUEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsR0FBR0EsRUFBRUEsVUFBVUEsQ0FBQ0EsQ0FBQ0E7WUFDckNBLElBQUlBLENBQUNBLHdCQUF3QkEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsV0FBV0EsRUFBRUEsVUFBVUEsQ0FBQ0EsQ0FBQ0E7UUFDN0RBLENBQUNBO0lBQ0hBLENBQUNBO0lBRU9SLDRDQUF5QkEsR0FBakNBLFVBQWtDQSxHQUFlQSxFQUFFQSxVQUFpQkE7UUFDbEVTLElBQUlBLEVBQUVBLEdBQUdBLElBQUlBLENBQUNBLGFBQWFBLENBQUNBLEtBQUtBLENBQUNBLFdBQVdBLENBQUNBO1FBQzlDQSxHQUFHQSxDQUFDQSxDQUFDQSxHQUFHQSxDQUFDQSxDQUFDQSxHQUFHQSxDQUFDQSxFQUFFQSxDQUFDQSxHQUFHQSxFQUFFQSxDQUFDQSxNQUFNQSxFQUFFQSxFQUFFQSxDQUFDQSxFQUFFQSxDQUFDQTtZQUNuQ0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsR0FBR0EsQ0FBQ0Esa0JBQWtCQSxDQUFDQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtnQkFDbENBLFVBQVVBLENBQUNBLElBQUlBLENBQUNBLEdBQUdBLENBQUNBLGtCQUFrQkEsQ0FBQ0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDakRBLENBQUNBO1FBQ0hBLENBQUNBO0lBQ0hBLENBQUNBO0lBRU9ULHNDQUFtQkEsR0FBM0JBLFVBQTRCQSxHQUFlQSxFQUFFQSxVQUFpQkE7UUFDNURVLEdBQUdBLENBQUNBLDBCQUEwQkEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsYUFBYUEsQ0FBQ0EsS0FBS0EsRUFBRUEsVUFBVUEsQ0FBQ0EsQ0FBQ0E7SUFDdkVBLENBQUNBO0lBQ0hWLGVBQUNBO0FBQURBLENBQUNBLEFBckdELElBcUdDO0FBckdZLGdCQUFRLFdBcUdwQixDQUFBO0FBRUQ7SUFBOENXLG1EQUFpQkE7SUFDN0RBLHlDQUFvQkEsV0FBdUJBO1FBQUlDLGlCQUFPQSxDQUFDQTtRQUFuQ0EsZ0JBQVdBLEdBQVhBLFdBQVdBLENBQVlBO0lBQWFBLENBQUNBO0lBRXpERCxzREFBWUEsR0FBWkEsY0FBdUJFLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLENBQUNBLGNBQWNBLENBQUNBLEdBQUdBLENBQUNBLFlBQVlBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBO0lBQzFGRixnREFBTUEsR0FBTkEsY0FBaUJHLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLENBQUNBLGNBQWNBLENBQUNBLEdBQUdBLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBLENBQUNBLENBQUNBO0lBQzlFSCx1REFBYUEsR0FBYkEsY0FBd0JJLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLENBQUNBLGNBQWNBLENBQUNBLEdBQUdBLENBQUNBLGFBQWFBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBO0lBQzVGSix3REFBY0EsR0FBZEEsY0FBeUJLLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLENBQUNBLGNBQWNBLENBQUNBLEdBQUdBLENBQUNBLGNBQWNBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBO0lBQzlGTCxrREFBUUEsR0FBUkEsY0FBbUJNLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLENBQUNBLGNBQWNBLENBQUNBLEdBQUdBLENBQUNBLFFBQVFBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBO0lBQ3BGTixzQ0FBQ0E7QUFBREEsQ0FBQ0EsQUFSRCxFQUE4QyxvQ0FBaUIsRUFROUQiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQge1xuICBpc1ByZXNlbnQsXG4gIGlzQmxhbmssXG4gIFR5cGUsXG4gIHN0cmluZ2lmeSxcbiAgQ09OU1RfRVhQUixcbiAgU3RyaW5nV3JhcHBlclxufSBmcm9tICdhbmd1bGFyMi9zcmMvZmFjYWRlL2xhbmcnO1xuaW1wb3J0IHtCYXNlRXhjZXB0aW9ufSBmcm9tICdhbmd1bGFyMi9zcmMvZmFjYWRlL2V4Y2VwdGlvbnMnO1xuaW1wb3J0IHtMaXN0V3JhcHBlciwgTWFwV3JhcHBlciwgU3RyaW5nTWFwV3JhcHBlcn0gZnJvbSAnYW5ndWxhcjIvc3JjL2ZhY2FkZS9jb2xsZWN0aW9uJztcbmltcG9ydCB7XG4gIEluamVjdG9yLFxuICBLZXksXG4gIERlcGVuZGVuY3ksXG4gIHByb3ZpZGUsXG4gIFByb3ZpZGVyLFxuICBSZXNvbHZlZFByb3ZpZGVyLFxuICBOb1Byb3ZpZGVyRXJyb3IsXG4gIEFic3RyYWN0UHJvdmlkZXJFcnJvcixcbiAgQ3ljbGljRGVwZW5kZW5jeUVycm9yLFxuICByZXNvbHZlRm9yd2FyZFJlZixcbiAgSW5qZWN0YWJsZVxufSBmcm9tICdhbmd1bGFyMi9zcmMvY29yZS9kaSc7XG5pbXBvcnQge21lcmdlUmVzb2x2ZWRQcm92aWRlcnN9IGZyb20gJ2FuZ3VsYXIyL3NyYy9jb3JlL2RpL3Byb3ZpZGVyJztcbmltcG9ydCB7XG4gIFVOREVGSU5FRCxcbiAgUHJvdG9JbmplY3RvcixcbiAgVmlzaWJpbGl0eSxcbiAgSW5qZWN0b3JJbmxpbmVTdHJhdGVneSxcbiAgSW5qZWN0b3JEeW5hbWljU3RyYXRlZ3ksXG4gIFByb3ZpZGVyV2l0aFZpc2liaWxpdHksXG4gIERlcGVuZGVuY3lQcm92aWRlclxufSBmcm9tICdhbmd1bGFyMi9zcmMvY29yZS9kaS9pbmplY3Rvcic7XG5pbXBvcnQge3Jlc29sdmVQcm92aWRlciwgUmVzb2x2ZWRGYWN0b3J5LCBSZXNvbHZlZFByb3ZpZGVyX30gZnJvbSAnYW5ndWxhcjIvc3JjL2NvcmUvZGkvcHJvdmlkZXInO1xuXG5pbXBvcnQge0F0dHJpYnV0ZU1ldGFkYXRhLCBRdWVyeU1ldGFkYXRhfSBmcm9tICcuLi9tZXRhZGF0YS9kaSc7XG5cbmltcG9ydCB7QXBwVmlld30gZnJvbSAnLi92aWV3JztcbmltcG9ydCB7Vmlld1R5cGV9IGZyb20gJy4vdmlld190eXBlJztcbmltcG9ydCB7RWxlbWVudFJlZl99IGZyb20gJy4vZWxlbWVudF9yZWYnO1xuXG5pbXBvcnQge1ZpZXdDb250YWluZXJSZWZ9IGZyb20gJy4vdmlld19jb250YWluZXJfcmVmJztcbmltcG9ydCB7RWxlbWVudFJlZn0gZnJvbSAnLi9lbGVtZW50X3JlZic7XG5pbXBvcnQge1JlbmRlcmVyfSBmcm9tICdhbmd1bGFyMi9zcmMvY29yZS9yZW5kZXIvYXBpJztcbmltcG9ydCB7VGVtcGxhdGVSZWYsIFRlbXBsYXRlUmVmX30gZnJvbSAnLi90ZW1wbGF0ZV9yZWYnO1xuaW1wb3J0IHtEaXJlY3RpdmVNZXRhZGF0YSwgQ29tcG9uZW50TWV0YWRhdGF9IGZyb20gJy4uL21ldGFkYXRhL2RpcmVjdGl2ZXMnO1xuaW1wb3J0IHtcbiAgQ2hhbmdlRGV0ZWN0b3IsXG4gIENoYW5nZURldGVjdG9yUmVmXG59IGZyb20gJ2FuZ3VsYXIyL3NyYy9jb3JlL2NoYW5nZV9kZXRlY3Rpb24vY2hhbmdlX2RldGVjdGlvbic7XG5pbXBvcnQge1F1ZXJ5TGlzdH0gZnJvbSAnLi9xdWVyeV9saXN0JztcbmltcG9ydCB7cmVmbGVjdG9yfSBmcm9tICdhbmd1bGFyMi9zcmMvY29yZS9yZWZsZWN0aW9uL3JlZmxlY3Rpb24nO1xuaW1wb3J0IHtTZXR0ZXJGbn0gZnJvbSAnYW5ndWxhcjIvc3JjL2NvcmUvcmVmbGVjdGlvbi90eXBlcyc7XG5pbXBvcnQge0FmdGVyVmlld0NoZWNrZWR9IGZyb20gJ2FuZ3VsYXIyL3NyYy9jb3JlL2xpbmtlci9pbnRlcmZhY2VzJztcbmltcG9ydCB7UGlwZVByb3ZpZGVyfSBmcm9tICdhbmd1bGFyMi9zcmMvY29yZS9waXBlcy9waXBlX3Byb3ZpZGVyJztcblxuaW1wb3J0IHtWaWV3Q29udGFpbmVyUmVmX30gZnJvbSBcIi4vdmlld19jb250YWluZXJfcmVmXCI7XG5pbXBvcnQge1Jlc29sdmVkTWV0YWRhdGFDYWNoZX0gZnJvbSAnLi9yZXNvbHZlZF9tZXRhZGF0YV9jYWNoZSc7XG5cbnZhciBfc3RhdGljS2V5cztcblxuZXhwb3J0IGNsYXNzIFN0YXRpY0tleXMge1xuICB0ZW1wbGF0ZVJlZklkOiBudW1iZXI7XG4gIHZpZXdDb250YWluZXJJZDogbnVtYmVyO1xuICBjaGFuZ2VEZXRlY3RvclJlZklkOiBudW1iZXI7XG4gIGVsZW1lbnRSZWZJZDogbnVtYmVyO1xuICByZW5kZXJlcklkOiBudW1iZXI7XG5cbiAgY29uc3RydWN0b3IoKSB7XG4gICAgdGhpcy50ZW1wbGF0ZVJlZklkID0gS2V5LmdldChUZW1wbGF0ZVJlZikuaWQ7XG4gICAgdGhpcy52aWV3Q29udGFpbmVySWQgPSBLZXkuZ2V0KFZpZXdDb250YWluZXJSZWYpLmlkO1xuICAgIHRoaXMuY2hhbmdlRGV0ZWN0b3JSZWZJZCA9IEtleS5nZXQoQ2hhbmdlRGV0ZWN0b3JSZWYpLmlkO1xuICAgIHRoaXMuZWxlbWVudFJlZklkID0gS2V5LmdldChFbGVtZW50UmVmKS5pZDtcbiAgICB0aGlzLnJlbmRlcmVySWQgPSBLZXkuZ2V0KFJlbmRlcmVyKS5pZDtcbiAgfVxuXG4gIHN0YXRpYyBpbnN0YW5jZSgpOiBTdGF0aWNLZXlzIHtcbiAgICBpZiAoaXNCbGFuayhfc3RhdGljS2V5cykpIF9zdGF0aWNLZXlzID0gbmV3IFN0YXRpY0tleXMoKTtcbiAgICByZXR1cm4gX3N0YXRpY0tleXM7XG4gIH1cbn1cblxuZXhwb3J0IGNsYXNzIERpcmVjdGl2ZURlcGVuZGVuY3kgZXh0ZW5kcyBEZXBlbmRlbmN5IHtcbiAgY29uc3RydWN0b3Ioa2V5OiBLZXksIG9wdGlvbmFsOiBib29sZWFuLCBsb3dlckJvdW5kVmlzaWJpbGl0eTogT2JqZWN0LFxuICAgICAgICAgICAgICB1cHBlckJvdW5kVmlzaWJpbGl0eTogT2JqZWN0LCBwcm9wZXJ0aWVzOiBhbnlbXSwgcHVibGljIGF0dHJpYnV0ZU5hbWU6IHN0cmluZyxcbiAgICAgICAgICAgICAgcHVibGljIHF1ZXJ5RGVjb3JhdG9yOiBRdWVyeU1ldGFkYXRhKSB7XG4gICAgc3VwZXIoa2V5LCBvcHRpb25hbCwgbG93ZXJCb3VuZFZpc2liaWxpdHksIHVwcGVyQm91bmRWaXNpYmlsaXR5LCBwcm9wZXJ0aWVzKTtcbiAgICB0aGlzLl92ZXJpZnkoKTtcbiAgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX3ZlcmlmeSgpOiB2b2lkIHtcbiAgICB2YXIgY291bnQgPSAwO1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeURlY29yYXRvcikpIGNvdW50Kys7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLmF0dHJpYnV0ZU5hbWUpKSBjb3VudCsrO1xuICAgIGlmIChjb3VudCA+IDEpXG4gICAgICB0aHJvdyBuZXcgQmFzZUV4Y2VwdGlvbihcbiAgICAgICAgICAnQSBkaXJlY3RpdmUgaW5qZWN0YWJsZSBjYW4gY29udGFpbiBvbmx5IG9uZSBvZiB0aGUgZm9sbG93aW5nIEBBdHRyaWJ1dGUgb3IgQFF1ZXJ5LicpO1xuICB9XG5cbiAgc3RhdGljIGNyZWF0ZUZyb20oZDogRGVwZW5kZW5jeSk6IERpcmVjdGl2ZURlcGVuZGVuY3kge1xuICAgIHJldHVybiBuZXcgRGlyZWN0aXZlRGVwZW5kZW5jeShcbiAgICAgICAgZC5rZXksIGQub3B0aW9uYWwsIGQubG93ZXJCb3VuZFZpc2liaWxpdHksIGQudXBwZXJCb3VuZFZpc2liaWxpdHksIGQucHJvcGVydGllcyxcbiAgICAgICAgRGlyZWN0aXZlRGVwZW5kZW5jeS5fYXR0cmlidXRlTmFtZShkLnByb3BlcnRpZXMpLCBEaXJlY3RpdmVEZXBlbmRlbmN5Ll9xdWVyeShkLnByb3BlcnRpZXMpKTtcbiAgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgc3RhdGljIF9hdHRyaWJ1dGVOYW1lKHByb3BlcnRpZXM6IGFueVtdKTogc3RyaW5nIHtcbiAgICB2YXIgcCA9IDxBdHRyaWJ1dGVNZXRhZGF0YT5wcm9wZXJ0aWVzLmZpbmQocCA9PiBwIGluc3RhbmNlb2YgQXR0cmlidXRlTWV0YWRhdGEpO1xuICAgIHJldHVybiBpc1ByZXNlbnQocCkgPyBwLmF0dHJpYnV0ZU5hbWUgOiBudWxsO1xuICB9XG5cbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBzdGF0aWMgX3F1ZXJ5KHByb3BlcnRpZXM6IGFueVtdKTogUXVlcnlNZXRhZGF0YSB7XG4gICAgcmV0dXJuIDxRdWVyeU1ldGFkYXRhPnByb3BlcnRpZXMuZmluZChwID0+IHAgaW5zdGFuY2VvZiBRdWVyeU1ldGFkYXRhKTtcbiAgfVxufVxuXG5leHBvcnQgY2xhc3MgRGlyZWN0aXZlUHJvdmlkZXIgZXh0ZW5kcyBSZXNvbHZlZFByb3ZpZGVyXyB7XG4gIGNvbnN0cnVjdG9yKGtleTogS2V5LCBmYWN0b3J5OiBGdW5jdGlvbiwgZGVwczogRGVwZW5kZW5jeVtdLCBwdWJsaWMgaXNDb21wb25lbnQ6IGJvb2xlYW4sXG4gICAgICAgICAgICAgIHB1YmxpYyBwcm92aWRlcnM6IFJlc29sdmVkUHJvdmlkZXJbXSwgcHVibGljIHZpZXdQcm92aWRlcnM6IFJlc29sdmVkUHJvdmlkZXJbXSxcbiAgICAgICAgICAgICAgcHVibGljIHF1ZXJpZXM6IFF1ZXJ5TWV0YWRhdGFXaXRoU2V0dGVyW10pIHtcbiAgICBzdXBlcihrZXksIFtuZXcgUmVzb2x2ZWRGYWN0b3J5KGZhY3RvcnksIGRlcHMpXSwgZmFsc2UpO1xuICB9XG5cbiAgZ2V0IGRpc3BsYXlOYW1lKCk6IHN0cmluZyB7IHJldHVybiB0aGlzLmtleS5kaXNwbGF5TmFtZTsgfVxuXG4gIHN0YXRpYyBjcmVhdGVGcm9tVHlwZSh0eXBlOiBUeXBlLCBtZXRhOiBEaXJlY3RpdmVNZXRhZGF0YSk6IERpcmVjdGl2ZVByb3ZpZGVyIHtcbiAgICB2YXIgcHJvdmlkZXIgPSBuZXcgUHJvdmlkZXIodHlwZSwge3VzZUNsYXNzOiB0eXBlfSk7XG4gICAgaWYgKGlzQmxhbmsobWV0YSkpIHtcbiAgICAgIG1ldGEgPSBuZXcgRGlyZWN0aXZlTWV0YWRhdGEoKTtcbiAgICB9XG4gICAgdmFyIHJiID0gcmVzb2x2ZVByb3ZpZGVyKHByb3ZpZGVyKTtcbiAgICB2YXIgcmYgPSByYi5yZXNvbHZlZEZhY3Rvcmllc1swXTtcbiAgICB2YXIgZGVwczogRGlyZWN0aXZlRGVwZW5kZW5jeVtdID0gcmYuZGVwZW5kZW5jaWVzLm1hcChEaXJlY3RpdmVEZXBlbmRlbmN5LmNyZWF0ZUZyb20pO1xuICAgIHZhciBpc0NvbXBvbmVudCA9IG1ldGEgaW5zdGFuY2VvZiBDb21wb25lbnRNZXRhZGF0YTtcbiAgICB2YXIgcmVzb2x2ZWRQcm92aWRlcnMgPSBpc1ByZXNlbnQobWV0YS5wcm92aWRlcnMpID8gSW5qZWN0b3IucmVzb2x2ZShtZXRhLnByb3ZpZGVycykgOiBudWxsO1xuICAgIHZhciByZXNvbHZlZFZpZXdQcm92aWRlcnMgPSBtZXRhIGluc3RhbmNlb2YgQ29tcG9uZW50TWV0YWRhdGEgJiYgaXNQcmVzZW50KG1ldGEudmlld1Byb3ZpZGVycykgP1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgSW5qZWN0b3IucmVzb2x2ZShtZXRhLnZpZXdQcm92aWRlcnMpIDpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG51bGw7XG4gICAgdmFyIHF1ZXJpZXMgPSBbXTtcbiAgICBpZiAoaXNQcmVzZW50KG1ldGEucXVlcmllcykpIHtcbiAgICAgIFN0cmluZ01hcFdyYXBwZXIuZm9yRWFjaChtZXRhLnF1ZXJpZXMsIChtZXRhLCBmaWVsZE5hbWUpID0+IHtcbiAgICAgICAgdmFyIHNldHRlciA9IHJlZmxlY3Rvci5zZXR0ZXIoZmllbGROYW1lKTtcbiAgICAgICAgcXVlcmllcy5wdXNoKG5ldyBRdWVyeU1ldGFkYXRhV2l0aFNldHRlcihzZXR0ZXIsIG1ldGEpKTtcbiAgICAgIH0pO1xuICAgIH1cbiAgICAvLyBxdWVyaWVzIHBhc3NlZCBpbnRvIHRoZSBjb25zdHJ1Y3Rvci5cbiAgICAvLyBUT0RPOiByZW1vdmUgdGhpcyBhZnRlciBjb25zdHJ1Y3RvciBxdWVyaWVzIGFyZSBubyBsb25nZXIgc3VwcG9ydGVkXG4gICAgZGVwcy5mb3JFYWNoKGQgPT4ge1xuICAgICAgaWYgKGlzUHJlc2VudChkLnF1ZXJ5RGVjb3JhdG9yKSkge1xuICAgICAgICBxdWVyaWVzLnB1c2gobmV3IFF1ZXJ5TWV0YWRhdGFXaXRoU2V0dGVyKG51bGwsIGQucXVlcnlEZWNvcmF0b3IpKTtcbiAgICAgIH1cbiAgICB9KTtcbiAgICByZXR1cm4gbmV3IERpcmVjdGl2ZVByb3ZpZGVyKHJiLmtleSwgcmYuZmFjdG9yeSwgZGVwcywgaXNDb21wb25lbnQsIHJlc29sdmVkUHJvdmlkZXJzLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcmVzb2x2ZWRWaWV3UHJvdmlkZXJzLCBxdWVyaWVzKTtcbiAgfVxufVxuXG5leHBvcnQgY2xhc3MgUXVlcnlNZXRhZGF0YVdpdGhTZXR0ZXIge1xuICBjb25zdHJ1Y3RvcihwdWJsaWMgc2V0dGVyOiBTZXR0ZXJGbiwgcHVibGljIG1ldGFkYXRhOiBRdWVyeU1ldGFkYXRhKSB7fVxufVxuXG5cbmZ1bmN0aW9uIHNldFByb3ZpZGVyc1Zpc2liaWxpdHkocHJvdmlkZXJzOiBSZXNvbHZlZFByb3ZpZGVyW10sIHZpc2liaWxpdHk6IFZpc2liaWxpdHksXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlc3VsdDogTWFwPG51bWJlciwgVmlzaWJpbGl0eT4pIHtcbiAgZm9yICh2YXIgaSA9IDA7IGkgPCBwcm92aWRlcnMubGVuZ3RoOyBpKyspIHtcbiAgICByZXN1bHQuc2V0KHByb3ZpZGVyc1tpXS5rZXkuaWQsIHZpc2liaWxpdHkpO1xuICB9XG59XG5cbmV4cG9ydCBjbGFzcyBBcHBQcm90b0VsZW1lbnQge1xuICBwcm90b0luamVjdG9yOiBQcm90b0luamVjdG9yO1xuXG4gIHN0YXRpYyBjcmVhdGUobWV0YWRhdGFDYWNoZTogUmVzb2x2ZWRNZXRhZGF0YUNhY2hlLCBpbmRleDogbnVtYmVyLFxuICAgICAgICAgICAgICAgIGF0dHJpYnV0ZXM6IHtba2V5OiBzdHJpbmddOiBzdHJpbmd9LCBkaXJlY3RpdmVUeXBlczogVHlwZVtdLFxuICAgICAgICAgICAgICAgIGRpcmVjdGl2ZVZhcmlhYmxlQmluZGluZ3M6IHtba2V5OiBzdHJpbmddOiBudW1iZXJ9KTogQXBwUHJvdG9FbGVtZW50IHtcbiAgICB2YXIgY29tcG9uZW50RGlyUHJvdmlkZXIgPSBudWxsO1xuICAgIHZhciBtZXJnZWRQcm92aWRlcnNNYXA6IE1hcDxudW1iZXIsIFJlc29sdmVkUHJvdmlkZXI+ID0gbmV3IE1hcDxudW1iZXIsIFJlc29sdmVkUHJvdmlkZXI+KCk7XG4gICAgdmFyIHByb3ZpZGVyVmlzaWJpbGl0eU1hcDogTWFwPG51bWJlciwgVmlzaWJpbGl0eT4gPSBuZXcgTWFwPG51bWJlciwgVmlzaWJpbGl0eT4oKTtcbiAgICB2YXIgcHJvdmlkZXJzID0gTGlzdFdyYXBwZXIuY3JlYXRlR3Jvd2FibGVTaXplKGRpcmVjdGl2ZVR5cGVzLmxlbmd0aCk7XG5cbiAgICB2YXIgcHJvdG9RdWVyeVJlZnMgPSBbXTtcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IGRpcmVjdGl2ZVR5cGVzLmxlbmd0aDsgaSsrKSB7XG4gICAgICB2YXIgZGlyUHJvdmlkZXIgPSBtZXRhZGF0YUNhY2hlLmdldFJlc29sdmVkRGlyZWN0aXZlTWV0YWRhdGEoZGlyZWN0aXZlVHlwZXNbaV0pO1xuICAgICAgcHJvdmlkZXJzW2ldID0gbmV3IFByb3ZpZGVyV2l0aFZpc2liaWxpdHkoXG4gICAgICAgICAgZGlyUHJvdmlkZXIsIGRpclByb3ZpZGVyLmlzQ29tcG9uZW50ID8gVmlzaWJpbGl0eS5QdWJsaWNBbmRQcml2YXRlIDogVmlzaWJpbGl0eS5QdWJsaWMpO1xuXG4gICAgICBpZiAoZGlyUHJvdmlkZXIuaXNDb21wb25lbnQpIHtcbiAgICAgICAgY29tcG9uZW50RGlyUHJvdmlkZXIgPSBkaXJQcm92aWRlcjtcbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIGlmIChpc1ByZXNlbnQoZGlyUHJvdmlkZXIucHJvdmlkZXJzKSkge1xuICAgICAgICAgIG1lcmdlUmVzb2x2ZWRQcm92aWRlcnMoZGlyUHJvdmlkZXIucHJvdmlkZXJzLCBtZXJnZWRQcm92aWRlcnNNYXApO1xuICAgICAgICAgIHNldFByb3ZpZGVyc1Zpc2liaWxpdHkoZGlyUHJvdmlkZXIucHJvdmlkZXJzLCBWaXNpYmlsaXR5LlB1YmxpYywgcHJvdmlkZXJWaXNpYmlsaXR5TWFwKTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgICAgaWYgKGlzUHJlc2VudChkaXJQcm92aWRlci52aWV3UHJvdmlkZXJzKSkge1xuICAgICAgICBtZXJnZVJlc29sdmVkUHJvdmlkZXJzKGRpclByb3ZpZGVyLnZpZXdQcm92aWRlcnMsIG1lcmdlZFByb3ZpZGVyc01hcCk7XG4gICAgICAgIHNldFByb3ZpZGVyc1Zpc2liaWxpdHkoZGlyUHJvdmlkZXIudmlld1Byb3ZpZGVycywgVmlzaWJpbGl0eS5Qcml2YXRlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHByb3ZpZGVyVmlzaWJpbGl0eU1hcCk7XG4gICAgICB9XG4gICAgICBmb3IgKHZhciBxdWVyeUlkeCA9IDA7IHF1ZXJ5SWR4IDwgZGlyUHJvdmlkZXIucXVlcmllcy5sZW5ndGg7IHF1ZXJ5SWR4KyspIHtcbiAgICAgICAgdmFyIHEgPSBkaXJQcm92aWRlci5xdWVyaWVzW3F1ZXJ5SWR4XTtcbiAgICAgICAgcHJvdG9RdWVyeVJlZnMucHVzaChuZXcgUHJvdG9RdWVyeVJlZihpLCBxLnNldHRlciwgcS5tZXRhZGF0YSkpO1xuICAgICAgfVxuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KGNvbXBvbmVudERpclByb3ZpZGVyKSAmJiBpc1ByZXNlbnQoY29tcG9uZW50RGlyUHJvdmlkZXIucHJvdmlkZXJzKSkge1xuICAgICAgLy8gZGlyZWN0aXZlIHByb3ZpZGVycyBuZWVkIHRvIGJlIHByaW9yaXRpemVkIG92ZXIgY29tcG9uZW50IHByb3ZpZGVyc1xuICAgICAgbWVyZ2VSZXNvbHZlZFByb3ZpZGVycyhjb21wb25lbnREaXJQcm92aWRlci5wcm92aWRlcnMsIG1lcmdlZFByb3ZpZGVyc01hcCk7XG4gICAgICBzZXRQcm92aWRlcnNWaXNpYmlsaXR5KGNvbXBvbmVudERpclByb3ZpZGVyLnByb3ZpZGVycywgVmlzaWJpbGl0eS5QdWJsaWMsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgIHByb3ZpZGVyVmlzaWJpbGl0eU1hcCk7XG4gICAgfVxuICAgIG1lcmdlZFByb3ZpZGVyc01hcC5mb3JFYWNoKChwcm92aWRlciwgXykgPT4ge1xuICAgICAgcHJvdmlkZXJzLnB1c2goXG4gICAgICAgICAgbmV3IFByb3ZpZGVyV2l0aFZpc2liaWxpdHkocHJvdmlkZXIsIHByb3ZpZGVyVmlzaWJpbGl0eU1hcC5nZXQocHJvdmlkZXIua2V5LmlkKSkpO1xuICAgIH0pO1xuXG4gICAgcmV0dXJuIG5ldyBBcHBQcm90b0VsZW1lbnQoaXNQcmVzZW50KGNvbXBvbmVudERpclByb3ZpZGVyKSwgaW5kZXgsIGF0dHJpYnV0ZXMsIHByb3ZpZGVycyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwcm90b1F1ZXJ5UmVmcywgZGlyZWN0aXZlVmFyaWFibGVCaW5kaW5ncyk7XG4gIH1cblxuICBjb25zdHJ1Y3RvcihwdWJsaWMgZmlyc3RQcm92aWRlcklzQ29tcG9uZW50OiBib29sZWFuLCBwdWJsaWMgaW5kZXg6IG51bWJlcixcbiAgICAgICAgICAgICAgcHVibGljIGF0dHJpYnV0ZXM6IHtba2V5OiBzdHJpbmddOiBzdHJpbmd9LCBwd3ZzOiBQcm92aWRlcldpdGhWaXNpYmlsaXR5W10sXG4gICAgICAgICAgICAgIHB1YmxpYyBwcm90b1F1ZXJ5UmVmczogUHJvdG9RdWVyeVJlZltdLFxuICAgICAgICAgICAgICBwdWJsaWMgZGlyZWN0aXZlVmFyaWFibGVCaW5kaW5nczoge1trZXk6IHN0cmluZ106IG51bWJlcn0pIHtcbiAgICB2YXIgbGVuZ3RoID0gcHd2cy5sZW5ndGg7XG4gICAgaWYgKGxlbmd0aCA+IDApIHtcbiAgICAgIHRoaXMucHJvdG9JbmplY3RvciA9IG5ldyBQcm90b0luamVjdG9yKHB3dnMpO1xuICAgIH0gZWxzZSB7XG4gICAgICB0aGlzLnByb3RvSW5qZWN0b3IgPSBudWxsO1xuICAgICAgdGhpcy5wcm90b1F1ZXJ5UmVmcyA9IFtdO1xuICAgIH1cbiAgfVxuXG4gIGdldFByb3ZpZGVyQXRJbmRleChpbmRleDogbnVtYmVyKTogYW55IHsgcmV0dXJuIHRoaXMucHJvdG9JbmplY3Rvci5nZXRQcm92aWRlckF0SW5kZXgoaW5kZXgpOyB9XG59XG5cbmNsYXNzIF9Db250ZXh0IHtcbiAgY29uc3RydWN0b3IocHVibGljIGVsZW1lbnQ6IGFueSwgcHVibGljIGNvbXBvbmVudEVsZW1lbnQ6IGFueSwgcHVibGljIGluamVjdG9yOiBhbnkpIHt9XG59XG5cbmV4cG9ydCBjbGFzcyBJbmplY3RvcldpdGhIb3N0Qm91bmRhcnkge1xuICBjb25zdHJ1Y3RvcihwdWJsaWMgaW5qZWN0b3I6IEluamVjdG9yLCBwdWJsaWMgaG9zdEluamVjdG9yQm91bmRhcnk6IGJvb2xlYW4pIHt9XG59XG5cbmV4cG9ydCBjbGFzcyBBcHBFbGVtZW50IGltcGxlbWVudHMgRGVwZW5kZW5jeVByb3ZpZGVyLCBFbGVtZW50UmVmLCBBZnRlclZpZXdDaGVja2VkIHtcbiAgc3RhdGljIGdldFZpZXdQYXJlbnRJbmplY3RvcihwYXJlbnRWaWV3VHlwZTogVmlld1R5cGUsIGNvbnRhaW5lckFwcEVsZW1lbnQ6IEFwcEVsZW1lbnQsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1wZXJhdGl2ZWx5Q3JlYXRlZFByb3ZpZGVyczogUmVzb2x2ZWRQcm92aWRlcltdLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJvb3RJbmplY3RvcjogSW5qZWN0b3IpOiBJbmplY3RvcldpdGhIb3N0Qm91bmRhcnkge1xuICAgIHZhciBwYXJlbnRJbmplY3RvcjtcbiAgICB2YXIgaG9zdEluamVjdG9yQm91bmRhcnk7XG4gICAgc3dpdGNoIChwYXJlbnRWaWV3VHlwZSkge1xuICAgICAgY2FzZSBWaWV3VHlwZS5DT01QT05FTlQ6XG4gICAgICAgIHBhcmVudEluamVjdG9yID0gY29udGFpbmVyQXBwRWxlbWVudC5faW5qZWN0b3I7XG4gICAgICAgIGhvc3RJbmplY3RvckJvdW5kYXJ5ID0gdHJ1ZTtcbiAgICAgICAgYnJlYWs7XG4gICAgICBjYXNlIFZpZXdUeXBlLkVNQkVEREVEOlxuICAgICAgICBwYXJlbnRJbmplY3RvciA9IGlzUHJlc2VudChjb250YWluZXJBcHBFbGVtZW50LnByb3RvLnByb3RvSW5qZWN0b3IpID9cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyQXBwRWxlbWVudC5faW5qZWN0b3IucGFyZW50IDpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyQXBwRWxlbWVudC5faW5qZWN0b3I7XG4gICAgICAgIGhvc3RJbmplY3RvckJvdW5kYXJ5ID0gY29udGFpbmVyQXBwRWxlbWVudC5faW5qZWN0b3IuaG9zdEJvdW5kYXJ5O1xuICAgICAgICBicmVhaztcbiAgICAgIGNhc2UgVmlld1R5cGUuSE9TVDpcbiAgICAgICAgaWYgKGlzUHJlc2VudChjb250YWluZXJBcHBFbGVtZW50KSkge1xuICAgICAgICAgIC8vIGhvc3QgdmlldyBpcyBhdHRhY2hlZCB0byBhIGNvbnRhaW5lclxuICAgICAgICAgIHBhcmVudEluamVjdG9yID0gaXNQcmVzZW50KGNvbnRhaW5lckFwcEVsZW1lbnQucHJvdG8ucHJvdG9JbmplY3RvcikgP1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lckFwcEVsZW1lbnQuX2luamVjdG9yLnBhcmVudCA6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyQXBwRWxlbWVudC5faW5qZWN0b3I7XG4gICAgICAgICAgaWYgKGlzUHJlc2VudChpbXBlcmF0aXZlbHlDcmVhdGVkUHJvdmlkZXJzKSkge1xuICAgICAgICAgICAgdmFyIGltcGVyYXRpdmVQcm92aWRlcnNXaXRoVmlzaWJpbGl0eSA9IGltcGVyYXRpdmVseUNyZWF0ZWRQcm92aWRlcnMubWFwKFxuICAgICAgICAgICAgICAgIHAgPT4gbmV3IFByb3ZpZGVyV2l0aFZpc2liaWxpdHkocCwgVmlzaWJpbGl0eS5QdWJsaWMpKTtcbiAgICAgICAgICAgIC8vIFRoZSBpbXBlcmF0aXZlIGluamVjdG9yIGlzIHNpbWlsYXIgdG8gaGF2aW5nIGFuIGVsZW1lbnQgYmV0d2VlblxuICAgICAgICAgICAgLy8gdGhlIGR5bmFtaWMtbG9hZGVkIGNvbXBvbmVudCBhbmQgaXRzIHBhcmVudCA9PiBubyBib3VuZGFyeSBiZXR3ZWVuXG4gICAgICAgICAgICAvLyB0aGUgY29tcG9uZW50IGFuZCBpbXBlcmF0aXZlbHlDcmVhdGVkSW5qZWN0b3IuXG4gICAgICAgICAgICBwYXJlbnRJbmplY3RvciA9IG5ldyBJbmplY3RvcihuZXcgUHJvdG9JbmplY3RvcihpbXBlcmF0aXZlUHJvdmlkZXJzV2l0aFZpc2liaWxpdHkpLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGFyZW50SW5qZWN0b3IsIHRydWUsIG51bGwsIG51bGwpO1xuICAgICAgICAgICAgaG9zdEluamVjdG9yQm91bmRhcnkgPSBmYWxzZTtcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgaG9zdEluamVjdG9yQm91bmRhcnkgPSBjb250YWluZXJBcHBFbGVtZW50Ll9pbmplY3Rvci5ob3N0Qm91bmRhcnk7XG4gICAgICAgICAgfVxuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgIC8vIGJvb3RzdHJhcFxuICAgICAgICAgIHBhcmVudEluamVjdG9yID0gcm9vdEluamVjdG9yO1xuICAgICAgICAgIGhvc3RJbmplY3RvckJvdW5kYXJ5ID0gdHJ1ZTtcbiAgICAgICAgfVxuICAgICAgICBicmVhaztcbiAgICB9XG4gICAgcmV0dXJuIG5ldyBJbmplY3RvcldpdGhIb3N0Qm91bmRhcnkocGFyZW50SW5qZWN0b3IsIGhvc3RJbmplY3RvckJvdW5kYXJ5KTtcbiAgfVxuXG4gIHB1YmxpYyBuZXN0ZWRWaWV3czogQXBwVmlld1tdID0gbnVsbDtcbiAgcHVibGljIGNvbXBvbmVudFZpZXc6IEFwcFZpZXcgPSBudWxsO1xuXG4gIHByaXZhdGUgX3F1ZXJ5U3RyYXRlZ3k6IF9RdWVyeVN0cmF0ZWd5O1xuICBwcml2YXRlIF9pbmplY3RvcjogSW5qZWN0b3I7XG4gIHByaXZhdGUgX3N0cmF0ZWd5OiBfRWxlbWVudERpcmVjdGl2ZVN0cmF0ZWd5O1xuICBwdWJsaWMgcmVmOiBFbGVtZW50UmVmXztcblxuICBjb25zdHJ1Y3RvcihwdWJsaWMgcHJvdG86IEFwcFByb3RvRWxlbWVudCwgcHVibGljIHBhcmVudFZpZXc6IEFwcFZpZXcsIHB1YmxpYyBwYXJlbnQ6IEFwcEVsZW1lbnQsXG4gICAgICAgICAgICAgIHB1YmxpYyBuYXRpdmVFbGVtZW50OiBhbnksIHB1YmxpYyBlbWJlZGRlZFZpZXdGYWN0b3J5OiBGdW5jdGlvbikge1xuICAgIHRoaXMucmVmID0gbmV3IEVsZW1lbnRSZWZfKHRoaXMpO1xuICAgIHZhciBwYXJlbnRJbmplY3RvciA9IGlzUHJlc2VudChwYXJlbnQpID8gcGFyZW50Ll9pbmplY3RvciA6IHBhcmVudFZpZXcucGFyZW50SW5qZWN0b3I7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnByb3RvLnByb3RvSW5qZWN0b3IpKSB7XG4gICAgICB2YXIgaXNCb3VuZGFyeTtcbiAgICAgIGlmIChpc1ByZXNlbnQocGFyZW50KSAmJiBpc1ByZXNlbnQocGFyZW50LnByb3RvLnByb3RvSW5qZWN0b3IpKSB7XG4gICAgICAgIGlzQm91bmRhcnkgPSBmYWxzZTtcbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIGlzQm91bmRhcnkgPSBwYXJlbnRWaWV3Lmhvc3RJbmplY3RvckJvdW5kYXJ5O1xuICAgICAgfVxuICAgICAgdGhpcy5fcXVlcnlTdHJhdGVneSA9IHRoaXMuX2J1aWxkUXVlcnlTdHJhdGVneSgpO1xuICAgICAgdGhpcy5faW5qZWN0b3IgPSBuZXcgSW5qZWN0b3IodGhpcy5wcm90by5wcm90b0luamVjdG9yLCBwYXJlbnRJbmplY3RvciwgaXNCb3VuZGFyeSwgdGhpcyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICgpID0+IHRoaXMuX2RlYnVnQ29udGV4dCgpKTtcblxuICAgICAgLy8gd2UgY291cGxlIG91cnNlbHZlcyB0byB0aGUgaW5qZWN0b3Igc3RyYXRlZ3kgdG8gYXZvaWQgcG9seW1vcnBoaWMgY2FsbHNcbiAgICAgIHZhciBpbmplY3RvclN0cmF0ZWd5ID0gPGFueT50aGlzLl9pbmplY3Rvci5pbnRlcm5hbFN0cmF0ZWd5O1xuICAgICAgdGhpcy5fc3RyYXRlZ3kgPSBpbmplY3RvclN0cmF0ZWd5IGluc3RhbmNlb2YgSW5qZWN0b3JJbmxpbmVTdHJhdGVneSA/XG4gICAgICAgICAgICAgICAgICAgICAgICAgICBuZXcgRWxlbWVudERpcmVjdGl2ZUlubGluZVN0cmF0ZWd5KGluamVjdG9yU3RyYXRlZ3ksIHRoaXMpIDpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgIG5ldyBFbGVtZW50RGlyZWN0aXZlRHluYW1pY1N0cmF0ZWd5KGluamVjdG9yU3RyYXRlZ3ksIHRoaXMpO1xuICAgICAgdGhpcy5fc3RyYXRlZ3kuaW5pdCgpO1xuICAgIH0gZWxzZSB7XG4gICAgICB0aGlzLl9xdWVyeVN0cmF0ZWd5ID0gbnVsbDtcbiAgICAgIHRoaXMuX2luamVjdG9yID0gcGFyZW50SW5qZWN0b3I7XG4gICAgICB0aGlzLl9zdHJhdGVneSA9IG51bGw7XG4gICAgfVxuICB9XG5cbiAgYXR0YWNoQ29tcG9uZW50Vmlldyhjb21wb25lbnRWaWV3OiBBcHBWaWV3KSB7IHRoaXMuY29tcG9uZW50VmlldyA9IGNvbXBvbmVudFZpZXc7IH1cblxuICBwcml2YXRlIF9kZWJ1Z0NvbnRleHQoKTogYW55IHtcbiAgICB2YXIgYyA9IHRoaXMucGFyZW50Vmlldy5nZXREZWJ1Z0NvbnRleHQodGhpcywgbnVsbCwgbnVsbCk7XG4gICAgcmV0dXJuIGlzUHJlc2VudChjKSA/IG5ldyBfQ29udGV4dChjLmVsZW1lbnQsIGMuY29tcG9uZW50RWxlbWVudCwgYy5pbmplY3RvcikgOiBudWxsO1xuICB9XG5cbiAgaGFzVmFyaWFibGVCaW5kaW5nKG5hbWU6IHN0cmluZyk6IGJvb2xlYW4ge1xuICAgIHZhciB2YiA9IHRoaXMucHJvdG8uZGlyZWN0aXZlVmFyaWFibGVCaW5kaW5ncztcbiAgICByZXR1cm4gaXNQcmVzZW50KHZiKSAmJiBTdHJpbmdNYXBXcmFwcGVyLmNvbnRhaW5zKHZiLCBuYW1lKTtcbiAgfVxuXG4gIGdldFZhcmlhYmxlQmluZGluZyhuYW1lOiBzdHJpbmcpOiBhbnkge1xuICAgIHZhciBpbmRleCA9IHRoaXMucHJvdG8uZGlyZWN0aXZlVmFyaWFibGVCaW5kaW5nc1tuYW1lXTtcbiAgICByZXR1cm4gaXNQcmVzZW50KGluZGV4KSA/IHRoaXMuZ2V0RGlyZWN0aXZlQXRJbmRleCg8bnVtYmVyPmluZGV4KSA6IHRoaXMuZ2V0RWxlbWVudFJlZigpO1xuICB9XG5cbiAgZ2V0KHRva2VuOiBhbnkpOiBhbnkgeyByZXR1cm4gdGhpcy5faW5qZWN0b3IuZ2V0KHRva2VuKTsgfVxuXG4gIGhhc0RpcmVjdGl2ZSh0eXBlOiBUeXBlKTogYm9vbGVhbiB7IHJldHVybiBpc1ByZXNlbnQodGhpcy5faW5qZWN0b3IuZ2V0T3B0aW9uYWwodHlwZSkpOyB9XG5cbiAgZ2V0Q29tcG9uZW50KCk6IGFueSB7IHJldHVybiBpc1ByZXNlbnQodGhpcy5fc3RyYXRlZ3kpID8gdGhpcy5fc3RyYXRlZ3kuZ2V0Q29tcG9uZW50KCkgOiBudWxsOyB9XG5cbiAgZ2V0SW5qZWN0b3IoKTogSW5qZWN0b3IgeyByZXR1cm4gdGhpcy5faW5qZWN0b3I7IH1cblxuICBnZXRFbGVtZW50UmVmKCk6IEVsZW1lbnRSZWYgeyByZXR1cm4gdGhpcy5yZWY7IH1cblxuICBnZXRWaWV3Q29udGFpbmVyUmVmKCk6IFZpZXdDb250YWluZXJSZWYgeyByZXR1cm4gbmV3IFZpZXdDb250YWluZXJSZWZfKHRoaXMpOyB9XG5cbiAgZ2V0VGVtcGxhdGVSZWYoKTogVGVtcGxhdGVSZWYge1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5lbWJlZGRlZFZpZXdGYWN0b3J5KSkge1xuICAgICAgcmV0dXJuIG5ldyBUZW1wbGF0ZVJlZl8odGhpcy5yZWYpO1xuICAgIH1cbiAgICByZXR1cm4gbnVsbDtcbiAgfVxuXG4gIGdldERlcGVuZGVuY3koaW5qZWN0b3I6IEluamVjdG9yLCBwcm92aWRlcjogUmVzb2x2ZWRQcm92aWRlciwgZGVwOiBEZXBlbmRlbmN5KTogYW55IHtcbiAgICBpZiAocHJvdmlkZXIgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlcikge1xuICAgICAgdmFyIGRpckRlcCA9IDxEaXJlY3RpdmVEZXBlbmRlbmN5PmRlcDtcblxuICAgICAgaWYgKGlzUHJlc2VudChkaXJEZXAuYXR0cmlidXRlTmFtZSkpIHJldHVybiB0aGlzLl9idWlsZEF0dHJpYnV0ZShkaXJEZXApO1xuXG4gICAgICBpZiAoaXNQcmVzZW50KGRpckRlcC5xdWVyeURlY29yYXRvcikpXG4gICAgICAgIHJldHVybiB0aGlzLl9xdWVyeVN0cmF0ZWd5LmZpbmRRdWVyeShkaXJEZXAucXVlcnlEZWNvcmF0b3IpLmxpc3Q7XG5cbiAgICAgIGlmIChkaXJEZXAua2V5LmlkID09PSBTdGF0aWNLZXlzLmluc3RhbmNlKCkuY2hhbmdlRGV0ZWN0b3JSZWZJZCkge1xuICAgICAgICAvLyBXZSBwcm92aWRlIHRoZSBjb21wb25lbnQncyB2aWV3IGNoYW5nZSBkZXRlY3RvciB0byBjb21wb25lbnRzIGFuZFxuICAgICAgICAvLyB0aGUgc3Vycm91bmRpbmcgY29tcG9uZW50J3MgY2hhbmdlIGRldGVjdG9yIHRvIGRpcmVjdGl2ZXMuXG4gICAgICAgIGlmICh0aGlzLnByb3RvLmZpcnN0UHJvdmlkZXJJc0NvbXBvbmVudCkge1xuICAgICAgICAgIC8vIE5vdGU6IFRoZSBjb21wb25lbnQgdmlldyBpcyBub3QgeWV0IGNyZWF0ZWQgd2hlblxuICAgICAgICAgIC8vIHRoaXMgbWV0aG9kIGlzIGNhbGxlZCFcbiAgICAgICAgICByZXR1cm4gbmV3IF9Db21wb25lbnRWaWV3Q2hhbmdlRGV0ZWN0b3JSZWYodGhpcyk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgcmV0dXJuIHRoaXMucGFyZW50Vmlldy5jaGFuZ2VEZXRlY3Rvci5yZWY7XG4gICAgICAgIH1cbiAgICAgIH1cblxuICAgICAgaWYgKGRpckRlcC5rZXkuaWQgPT09IFN0YXRpY0tleXMuaW5zdGFuY2UoKS5lbGVtZW50UmVmSWQpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuZ2V0RWxlbWVudFJlZigpO1xuICAgICAgfVxuXG4gICAgICBpZiAoZGlyRGVwLmtleS5pZCA9PT0gU3RhdGljS2V5cy5pbnN0YW5jZSgpLnZpZXdDb250YWluZXJJZCkge1xuICAgICAgICByZXR1cm4gdGhpcy5nZXRWaWV3Q29udGFpbmVyUmVmKCk7XG4gICAgICB9XG5cbiAgICAgIGlmIChkaXJEZXAua2V5LmlkID09PSBTdGF0aWNLZXlzLmluc3RhbmNlKCkudGVtcGxhdGVSZWZJZCkge1xuICAgICAgICB2YXIgdHIgPSB0aGlzLmdldFRlbXBsYXRlUmVmKCk7XG4gICAgICAgIGlmIChpc0JsYW5rKHRyKSAmJiAhZGlyRGVwLm9wdGlvbmFsKSB7XG4gICAgICAgICAgdGhyb3cgbmV3IE5vUHJvdmlkZXJFcnJvcihudWxsLCBkaXJEZXAua2V5KTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdHI7XG4gICAgICB9XG5cbiAgICAgIGlmIChkaXJEZXAua2V5LmlkID09PSBTdGF0aWNLZXlzLmluc3RhbmNlKCkucmVuZGVyZXJJZCkge1xuICAgICAgICByZXR1cm4gdGhpcy5wYXJlbnRWaWV3LnJlbmRlcmVyO1xuICAgICAgfVxuXG4gICAgfSBlbHNlIGlmIChwcm92aWRlciBpbnN0YW5jZW9mIFBpcGVQcm92aWRlcikge1xuICAgICAgaWYgKGRlcC5rZXkuaWQgPT09IFN0YXRpY0tleXMuaW5zdGFuY2UoKS5jaGFuZ2VEZXRlY3RvclJlZklkKSB7XG4gICAgICAgIC8vIFdlIHByb3ZpZGUgdGhlIGNvbXBvbmVudCdzIHZpZXcgY2hhbmdlIGRldGVjdG9yIHRvIGNvbXBvbmVudHMgYW5kXG4gICAgICAgIC8vIHRoZSBzdXJyb3VuZGluZyBjb21wb25lbnQncyBjaGFuZ2UgZGV0ZWN0b3IgdG8gZGlyZWN0aXZlcy5cbiAgICAgICAgaWYgKHRoaXMucHJvdG8uZmlyc3RQcm92aWRlcklzQ29tcG9uZW50KSB7XG4gICAgICAgICAgLy8gTm90ZTogVGhlIGNvbXBvbmVudCB2aWV3IGlzIG5vdCB5ZXQgY3JlYXRlZCB3aGVuXG4gICAgICAgICAgLy8gdGhpcyBtZXRob2QgaXMgY2FsbGVkIVxuICAgICAgICAgIHJldHVybiBuZXcgX0NvbXBvbmVudFZpZXdDaGFuZ2VEZXRlY3RvclJlZih0aGlzKTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICByZXR1cm4gdGhpcy5wYXJlbnRWaWV3LmNoYW5nZURldGVjdG9yO1xuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuXG4gICAgcmV0dXJuIFVOREVGSU5FRDtcbiAgfVxuXG4gIHByaXZhdGUgX2J1aWxkQXR0cmlidXRlKGRlcDogRGlyZWN0aXZlRGVwZW5kZW5jeSk6IHN0cmluZyB7XG4gICAgdmFyIGF0dHJpYnV0ZXMgPSB0aGlzLnByb3RvLmF0dHJpYnV0ZXM7XG4gICAgaWYgKGlzUHJlc2VudChhdHRyaWJ1dGVzKSAmJiBTdHJpbmdNYXBXcmFwcGVyLmNvbnRhaW5zKGF0dHJpYnV0ZXMsIGRlcC5hdHRyaWJ1dGVOYW1lKSkge1xuICAgICAgcmV0dXJuIGF0dHJpYnV0ZXNbZGVwLmF0dHJpYnV0ZU5hbWVdO1xuICAgIH0gZWxzZSB7XG4gICAgICByZXR1cm4gbnVsbDtcbiAgICB9XG4gIH1cblxuICBhZGREaXJlY3RpdmVzTWF0Y2hpbmdRdWVyeShxdWVyeTogUXVlcnlNZXRhZGF0YSwgbGlzdDogYW55W10pOiB2b2lkIHtcbiAgICB2YXIgdGVtcGxhdGVSZWYgPSB0aGlzLmdldFRlbXBsYXRlUmVmKCk7XG4gICAgaWYgKHF1ZXJ5LnNlbGVjdG9yID09PSBUZW1wbGF0ZVJlZiAmJiBpc1ByZXNlbnQodGVtcGxhdGVSZWYpKSB7XG4gICAgICBsaXN0LnB1c2godGVtcGxhdGVSZWYpO1xuICAgIH1cbiAgICBpZiAodGhpcy5fc3RyYXRlZ3kgIT0gbnVsbCkge1xuICAgICAgdGhpcy5fc3RyYXRlZ3kuYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkocXVlcnksIGxpc3QpO1xuICAgIH1cbiAgfVxuXG4gIHByaXZhdGUgX2J1aWxkUXVlcnlTdHJhdGVneSgpOiBfUXVlcnlTdHJhdGVneSB7XG4gICAgaWYgKHRoaXMucHJvdG8ucHJvdG9RdWVyeVJlZnMubGVuZ3RoID09PSAwKSB7XG4gICAgICByZXR1cm4gX2VtcHR5UXVlcnlTdHJhdGVneTtcbiAgICB9IGVsc2UgaWYgKHRoaXMucHJvdG8ucHJvdG9RdWVyeVJlZnMubGVuZ3RoIDw9XG4gICAgICAgICAgICAgICBJbmxpbmVRdWVyeVN0cmF0ZWd5Lk5VTUJFUl9PRl9TVVBQT1JURURfUVVFUklFUykge1xuICAgICAgcmV0dXJuIG5ldyBJbmxpbmVRdWVyeVN0cmF0ZWd5KHRoaXMpO1xuICAgIH0gZWxzZSB7XG4gICAgICByZXR1cm4gbmV3IER5bmFtaWNRdWVyeVN0cmF0ZWd5KHRoaXMpO1xuICAgIH1cbiAgfVxuXG5cbiAgZ2V0RGlyZWN0aXZlQXRJbmRleChpbmRleDogbnVtYmVyKTogYW55IHsgcmV0dXJuIHRoaXMuX2luamVjdG9yLmdldEF0KGluZGV4KTsgfVxuXG4gIG5nQWZ0ZXJWaWV3Q2hlY2tlZCgpOiB2b2lkIHtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMuX3F1ZXJ5U3RyYXRlZ3kpKSB0aGlzLl9xdWVyeVN0cmF0ZWd5LnVwZGF0ZVZpZXdRdWVyaWVzKCk7XG4gIH1cblxuICBuZ0FmdGVyQ29udGVudENoZWNrZWQoKTogdm9pZCB7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLl9xdWVyeVN0cmF0ZWd5KSkgdGhpcy5fcXVlcnlTdHJhdGVneS51cGRhdGVDb250ZW50UXVlcmllcygpO1xuICB9XG5cbiAgdHJhdmVyc2VBbmRTZXRRdWVyaWVzQXNEaXJ0eSgpOiB2b2lkIHtcbiAgICB2YXIgaW5qOiBBcHBFbGVtZW50ID0gdGhpcztcbiAgICB3aGlsZSAoaXNQcmVzZW50KGluaikpIHtcbiAgICAgIGluai5fc2V0UXVlcmllc0FzRGlydHkoKTtcbiAgICAgIGlmIChpc0JsYW5rKGluai5wYXJlbnQpICYmIGluai5wYXJlbnRWaWV3LnByb3RvLnR5cGUgPT09IFZpZXdUeXBlLkVNQkVEREVEKSB7XG4gICAgICAgIGluaiA9IGluai5wYXJlbnRWaWV3LmNvbnRhaW5lckFwcEVsZW1lbnQ7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICBpbmogPSBpbmoucGFyZW50O1xuICAgICAgfVxuICAgIH1cbiAgfVxuXG4gIHByaXZhdGUgX3NldFF1ZXJpZXNBc0RpcnR5KCk6IHZvaWQge1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5fcXVlcnlTdHJhdGVneSkpIHtcbiAgICAgIHRoaXMuX3F1ZXJ5U3RyYXRlZ3kuc2V0Q29udGVudFF1ZXJpZXNBc0RpcnR5KCk7XG4gICAgfVxuICAgIGlmICh0aGlzLnBhcmVudFZpZXcucHJvdG8udHlwZSA9PT0gVmlld1R5cGUuQ09NUE9ORU5UKSB7XG4gICAgICB0aGlzLnBhcmVudFZpZXcuY29udGFpbmVyQXBwRWxlbWVudC5fcXVlcnlTdHJhdGVneS5zZXRWaWV3UXVlcmllc0FzRGlydHkoKTtcbiAgICB9XG4gIH1cbn1cblxuaW50ZXJmYWNlIF9RdWVyeVN0cmF0ZWd5IHtcbiAgc2V0Q29udGVudFF1ZXJpZXNBc0RpcnR5KCk6IHZvaWQ7XG4gIHNldFZpZXdRdWVyaWVzQXNEaXJ0eSgpOiB2b2lkO1xuICB1cGRhdGVDb250ZW50UXVlcmllcygpOiB2b2lkO1xuICB1cGRhdGVWaWV3UXVlcmllcygpOiB2b2lkO1xuICBmaW5kUXVlcnkocXVlcnk6IFF1ZXJ5TWV0YWRhdGEpOiBRdWVyeVJlZjtcbn1cblxuY2xhc3MgX0VtcHR5UXVlcnlTdHJhdGVneSBpbXBsZW1lbnRzIF9RdWVyeVN0cmF0ZWd5IHtcbiAgc2V0Q29udGVudFF1ZXJpZXNBc0RpcnR5KCk6IHZvaWQge31cbiAgc2V0Vmlld1F1ZXJpZXNBc0RpcnR5KCk6IHZvaWQge31cbiAgdXBkYXRlQ29udGVudFF1ZXJpZXMoKTogdm9pZCB7fVxuICB1cGRhdGVWaWV3UXVlcmllcygpOiB2b2lkIHt9XG4gIGZpbmRRdWVyeShxdWVyeTogUXVlcnlNZXRhZGF0YSk6IFF1ZXJ5UmVmIHtcbiAgICB0aHJvdyBuZXcgQmFzZUV4Y2VwdGlvbihgQ2Fubm90IGZpbmQgcXVlcnkgZm9yIGRpcmVjdGl2ZSAke3F1ZXJ5fS5gKTtcbiAgfVxufVxuXG52YXIgX2VtcHR5UXVlcnlTdHJhdGVneSA9IG5ldyBfRW1wdHlRdWVyeVN0cmF0ZWd5KCk7XG5cbmNsYXNzIElubGluZVF1ZXJ5U3RyYXRlZ3kgaW1wbGVtZW50cyBfUXVlcnlTdHJhdGVneSB7XG4gIHN0YXRpYyBOVU1CRVJfT0ZfU1VQUE9SVEVEX1FVRVJJRVMgPSAzO1xuXG4gIHF1ZXJ5MDogUXVlcnlSZWY7XG4gIHF1ZXJ5MTogUXVlcnlSZWY7XG4gIHF1ZXJ5MjogUXVlcnlSZWY7XG5cbiAgY29uc3RydWN0b3IoZWk6IEFwcEVsZW1lbnQpIHtcbiAgICB2YXIgcHJvdG9SZWZzID0gZWkucHJvdG8ucHJvdG9RdWVyeVJlZnM7XG4gICAgaWYgKHByb3RvUmVmcy5sZW5ndGggPiAwKSB0aGlzLnF1ZXJ5MCA9IG5ldyBRdWVyeVJlZihwcm90b1JlZnNbMF0sIGVpKTtcbiAgICBpZiAocHJvdG9SZWZzLmxlbmd0aCA+IDEpIHRoaXMucXVlcnkxID0gbmV3IFF1ZXJ5UmVmKHByb3RvUmVmc1sxXSwgZWkpO1xuICAgIGlmIChwcm90b1JlZnMubGVuZ3RoID4gMikgdGhpcy5xdWVyeTIgPSBuZXcgUXVlcnlSZWYocHJvdG9SZWZzWzJdLCBlaSk7XG4gIH1cblxuICBzZXRDb250ZW50UXVlcmllc0FzRGlydHkoKTogdm9pZCB7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MCkgJiYgIXRoaXMucXVlcnkwLmlzVmlld1F1ZXJ5KSB0aGlzLnF1ZXJ5MC5kaXJ0eSA9IHRydWU7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MSkgJiYgIXRoaXMucXVlcnkxLmlzVmlld1F1ZXJ5KSB0aGlzLnF1ZXJ5MS5kaXJ0eSA9IHRydWU7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MikgJiYgIXRoaXMucXVlcnkyLmlzVmlld1F1ZXJ5KSB0aGlzLnF1ZXJ5Mi5kaXJ0eSA9IHRydWU7XG4gIH1cblxuICBzZXRWaWV3UXVlcmllc0FzRGlydHkoKTogdm9pZCB7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MCkgJiYgdGhpcy5xdWVyeTAuaXNWaWV3UXVlcnkpIHRoaXMucXVlcnkwLmRpcnR5ID0gdHJ1ZTtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkxKSAmJiB0aGlzLnF1ZXJ5MS5pc1ZpZXdRdWVyeSkgdGhpcy5xdWVyeTEuZGlydHkgPSB0cnVlO1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTIpICYmIHRoaXMucXVlcnkyLmlzVmlld1F1ZXJ5KSB0aGlzLnF1ZXJ5Mi5kaXJ0eSA9IHRydWU7XG4gIH1cblxuICB1cGRhdGVDb250ZW50UXVlcmllcygpIHtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkwKSAmJiAhdGhpcy5xdWVyeTAuaXNWaWV3UXVlcnkpIHtcbiAgICAgIHRoaXMucXVlcnkwLnVwZGF0ZSgpO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkxKSAmJiAhdGhpcy5xdWVyeTEuaXNWaWV3UXVlcnkpIHtcbiAgICAgIHRoaXMucXVlcnkxLnVwZGF0ZSgpO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkyKSAmJiAhdGhpcy5xdWVyeTIuaXNWaWV3UXVlcnkpIHtcbiAgICAgIHRoaXMucXVlcnkyLnVwZGF0ZSgpO1xuICAgIH1cbiAgfVxuXG4gIHVwZGF0ZVZpZXdRdWVyaWVzKCkge1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTApICYmIHRoaXMucXVlcnkwLmlzVmlld1F1ZXJ5KSB7XG4gICAgICB0aGlzLnF1ZXJ5MC51cGRhdGUoKTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MSkgJiYgdGhpcy5xdWVyeTEuaXNWaWV3UXVlcnkpIHtcbiAgICAgIHRoaXMucXVlcnkxLnVwZGF0ZSgpO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkyKSAmJiB0aGlzLnF1ZXJ5Mi5pc1ZpZXdRdWVyeSkge1xuICAgICAgdGhpcy5xdWVyeTIudXBkYXRlKCk7XG4gICAgfVxuICB9XG5cbiAgZmluZFF1ZXJ5KHF1ZXJ5OiBRdWVyeU1ldGFkYXRhKTogUXVlcnlSZWYge1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTApICYmIHRoaXMucXVlcnkwLnByb3RvUXVlcnlSZWYucXVlcnkgPT09IHF1ZXJ5KSB7XG4gICAgICByZXR1cm4gdGhpcy5xdWVyeTA7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTEpICYmIHRoaXMucXVlcnkxLnByb3RvUXVlcnlSZWYucXVlcnkgPT09IHF1ZXJ5KSB7XG4gICAgICByZXR1cm4gdGhpcy5xdWVyeTE7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTIpICYmIHRoaXMucXVlcnkyLnByb3RvUXVlcnlSZWYucXVlcnkgPT09IHF1ZXJ5KSB7XG4gICAgICByZXR1cm4gdGhpcy5xdWVyeTI7XG4gICAgfVxuICAgIHRocm93IG5ldyBCYXNlRXhjZXB0aW9uKGBDYW5ub3QgZmluZCBxdWVyeSBmb3IgZGlyZWN0aXZlICR7cXVlcnl9LmApO1xuICB9XG59XG5cbmNsYXNzIER5bmFtaWNRdWVyeVN0cmF0ZWd5IGltcGxlbWVudHMgX1F1ZXJ5U3RyYXRlZ3kge1xuICBxdWVyaWVzOiBRdWVyeVJlZltdO1xuXG4gIGNvbnN0cnVjdG9yKGVpOiBBcHBFbGVtZW50KSB7XG4gICAgdGhpcy5xdWVyaWVzID0gZWkucHJvdG8ucHJvdG9RdWVyeVJlZnMubWFwKHAgPT4gbmV3IFF1ZXJ5UmVmKHAsIGVpKSk7XG4gIH1cblxuICBzZXRDb250ZW50UXVlcmllc0FzRGlydHkoKTogdm9pZCB7XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCB0aGlzLnF1ZXJpZXMubGVuZ3RoOyArK2kpIHtcbiAgICAgIHZhciBxID0gdGhpcy5xdWVyaWVzW2ldO1xuICAgICAgaWYgKCFxLmlzVmlld1F1ZXJ5KSBxLmRpcnR5ID0gdHJ1ZTtcbiAgICB9XG4gIH1cblxuICBzZXRWaWV3UXVlcmllc0FzRGlydHkoKTogdm9pZCB7XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCB0aGlzLnF1ZXJpZXMubGVuZ3RoOyArK2kpIHtcbiAgICAgIHZhciBxID0gdGhpcy5xdWVyaWVzW2ldO1xuICAgICAgaWYgKHEuaXNWaWV3UXVlcnkpIHEuZGlydHkgPSB0cnVlO1xuICAgIH1cbiAgfVxuXG4gIHVwZGF0ZUNvbnRlbnRRdWVyaWVzKCkge1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdGhpcy5xdWVyaWVzLmxlbmd0aDsgKytpKSB7XG4gICAgICB2YXIgcSA9IHRoaXMucXVlcmllc1tpXTtcbiAgICAgIGlmICghcS5pc1ZpZXdRdWVyeSkge1xuICAgICAgICBxLnVwZGF0ZSgpO1xuICAgICAgfVxuICAgIH1cbiAgfVxuXG4gIHVwZGF0ZVZpZXdRdWVyaWVzKCkge1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdGhpcy5xdWVyaWVzLmxlbmd0aDsgKytpKSB7XG4gICAgICB2YXIgcSA9IHRoaXMucXVlcmllc1tpXTtcbiAgICAgIGlmIChxLmlzVmlld1F1ZXJ5KSB7XG4gICAgICAgIHEudXBkYXRlKCk7XG4gICAgICB9XG4gICAgfVxuICB9XG5cbiAgZmluZFF1ZXJ5KHF1ZXJ5OiBRdWVyeU1ldGFkYXRhKTogUXVlcnlSZWYge1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdGhpcy5xdWVyaWVzLmxlbmd0aDsgKytpKSB7XG4gICAgICB2YXIgcSA9IHRoaXMucXVlcmllc1tpXTtcbiAgICAgIGlmIChxLnByb3RvUXVlcnlSZWYucXVlcnkgPT09IHF1ZXJ5KSB7XG4gICAgICAgIHJldHVybiBxO1xuICAgICAgfVxuICAgIH1cbiAgICB0aHJvdyBuZXcgQmFzZUV4Y2VwdGlvbihgQ2Fubm90IGZpbmQgcXVlcnkgZm9yIGRpcmVjdGl2ZSAke3F1ZXJ5fS5gKTtcbiAgfVxufVxuXG5pbnRlcmZhY2UgX0VsZW1lbnREaXJlY3RpdmVTdHJhdGVneSB7XG4gIGdldENvbXBvbmVudCgpOiBhbnk7XG4gIGlzQ29tcG9uZW50S2V5KGtleTogS2V5KTogYm9vbGVhbjtcbiAgYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkocTogUXVlcnlNZXRhZGF0YSwgcmVzOiBhbnlbXSk6IHZvaWQ7XG4gIGluaXQoKTogdm9pZDtcbn1cblxuLyoqXG4gKiBTdHJhdGVneSB1c2VkIGJ5IHRoZSBgRWxlbWVudEluamVjdG9yYCB3aGVuIHRoZSBudW1iZXIgb2YgcHJvdmlkZXJzIGlzIDEwIG9yIGxlc3MuXG4gKiBJbiBzdWNoIGEgY2FzZSwgaW5saW5pbmcgZmllbGRzIGlzIGJlbmVmaWNpYWwgZm9yIHBlcmZvcm1hbmNlcy5cbiAqL1xuY2xhc3MgRWxlbWVudERpcmVjdGl2ZUlubGluZVN0cmF0ZWd5IGltcGxlbWVudHMgX0VsZW1lbnREaXJlY3RpdmVTdHJhdGVneSB7XG4gIGNvbnN0cnVjdG9yKHB1YmxpYyBpbmplY3RvclN0cmF0ZWd5OiBJbmplY3RvcklubGluZVN0cmF0ZWd5LCBwdWJsaWMgX2VpOiBBcHBFbGVtZW50KSB7fVxuXG4gIGluaXQoKTogdm9pZCB7XG4gICAgdmFyIGkgPSB0aGlzLmluamVjdG9yU3RyYXRlZ3k7XG4gICAgdmFyIHAgPSBpLnByb3RvU3RyYXRlZ3k7XG4gICAgaS5yZXNldENvbnN0cnVjdGlvbkNvdW50ZXIoKTtcblxuICAgIGlmIChwLnByb3ZpZGVyMCBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkMCkgJiYgaS5vYmowID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajAgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjAsIHAudmlzaWJpbGl0eTApO1xuICAgIGlmIChwLnByb3ZpZGVyMSBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkMSkgJiYgaS5vYmoxID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajEgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjEsIHAudmlzaWJpbGl0eTEpO1xuICAgIGlmIChwLnByb3ZpZGVyMiBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkMikgJiYgaS5vYmoyID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajIgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjIsIHAudmlzaWJpbGl0eTIpO1xuICAgIGlmIChwLnByb3ZpZGVyMyBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkMykgJiYgaS5vYmozID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajMgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjMsIHAudmlzaWJpbGl0eTMpO1xuICAgIGlmIChwLnByb3ZpZGVyNCBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkNCkgJiYgaS5vYmo0ID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajQgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjQsIHAudmlzaWJpbGl0eTQpO1xuICAgIGlmIChwLnByb3ZpZGVyNSBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkNSkgJiYgaS5vYmo1ID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajUgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjUsIHAudmlzaWJpbGl0eTUpO1xuICAgIGlmIChwLnByb3ZpZGVyNiBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkNikgJiYgaS5vYmo2ID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajYgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjYsIHAudmlzaWJpbGl0eTYpO1xuICAgIGlmIChwLnByb3ZpZGVyNyBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkNykgJiYgaS5vYmo3ID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajcgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjcsIHAudmlzaWJpbGl0eTcpO1xuICAgIGlmIChwLnByb3ZpZGVyOCBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkOCkgJiYgaS5vYmo4ID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajggPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjgsIHAudmlzaWJpbGl0eTgpO1xuICAgIGlmIChwLnByb3ZpZGVyOSBpbnN0YW5jZW9mIERpcmVjdGl2ZVByb3ZpZGVyICYmIGlzUHJlc2VudChwLmtleUlkOSkgJiYgaS5vYmo5ID09PSBVTkRFRklORUQpXG4gICAgICBpLm9iajkgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjksIHAudmlzaWJpbGl0eTkpO1xuICB9XG5cbiAgZ2V0Q29tcG9uZW50KCk6IGFueSB7IHJldHVybiB0aGlzLmluamVjdG9yU3RyYXRlZ3kub2JqMDsgfVxuXG4gIGlzQ29tcG9uZW50S2V5KGtleTogS2V5KTogYm9vbGVhbiB7XG4gICAgcmV0dXJuIHRoaXMuX2VpLnByb3RvLmZpcnN0UHJvdmlkZXJJc0NvbXBvbmVudCAmJiBpc1ByZXNlbnQoa2V5KSAmJlxuICAgICAgICAgICBrZXkuaWQgPT09IHRoaXMuaW5qZWN0b3JTdHJhdGVneS5wcm90b1N0cmF0ZWd5LmtleUlkMDtcbiAgfVxuXG4gIGFkZERpcmVjdGl2ZXNNYXRjaGluZ1F1ZXJ5KHF1ZXJ5OiBRdWVyeU1ldGFkYXRhLCBsaXN0OiBhbnlbXSk6IHZvaWQge1xuICAgIHZhciBpID0gdGhpcy5pbmplY3RvclN0cmF0ZWd5O1xuICAgIHZhciBwID0gaS5wcm90b1N0cmF0ZWd5O1xuICAgIGlmIChpc1ByZXNlbnQocC5wcm92aWRlcjApICYmIHAucHJvdmlkZXIwLmtleS50b2tlbiA9PT0gcXVlcnkuc2VsZWN0b3IpIHtcbiAgICAgIGlmIChpLm9iajAgPT09IFVOREVGSU5FRCkgaS5vYmowID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXIwLCBwLnZpc2liaWxpdHkwKTtcbiAgICAgIGxpc3QucHVzaChpLm9iajApO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHAucHJvdmlkZXIxKSAmJiBwLnByb3ZpZGVyMS5rZXkudG9rZW4gPT09IHF1ZXJ5LnNlbGVjdG9yKSB7XG4gICAgICBpZiAoaS5vYmoxID09PSBVTkRFRklORUQpIGkub2JqMSA9IGkuaW5zdGFudGlhdGVQcm92aWRlcihwLnByb3ZpZGVyMSwgcC52aXNpYmlsaXR5MSk7XG4gICAgICBsaXN0LnB1c2goaS5vYmoxKTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudChwLnByb3ZpZGVyMikgJiYgcC5wcm92aWRlcjIua2V5LnRva2VuID09PSBxdWVyeS5zZWxlY3Rvcikge1xuICAgICAgaWYgKGkub2JqMiA9PT0gVU5ERUZJTkVEKSBpLm9iajIgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjIsIHAudmlzaWJpbGl0eTIpO1xuICAgICAgbGlzdC5wdXNoKGkub2JqMik7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQocC5wcm92aWRlcjMpICYmIHAucHJvdmlkZXIzLmtleS50b2tlbiA9PT0gcXVlcnkuc2VsZWN0b3IpIHtcbiAgICAgIGlmIChpLm9iajMgPT09IFVOREVGSU5FRCkgaS5vYmozID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXIzLCBwLnZpc2liaWxpdHkzKTtcbiAgICAgIGxpc3QucHVzaChpLm9iajMpO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHAucHJvdmlkZXI0KSAmJiBwLnByb3ZpZGVyNC5rZXkudG9rZW4gPT09IHF1ZXJ5LnNlbGVjdG9yKSB7XG4gICAgICBpZiAoaS5vYmo0ID09PSBVTkRFRklORUQpIGkub2JqNCA9IGkuaW5zdGFudGlhdGVQcm92aWRlcihwLnByb3ZpZGVyNCwgcC52aXNpYmlsaXR5NCk7XG4gICAgICBsaXN0LnB1c2goaS5vYmo0KTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudChwLnByb3ZpZGVyNSkgJiYgcC5wcm92aWRlcjUua2V5LnRva2VuID09PSBxdWVyeS5zZWxlY3Rvcikge1xuICAgICAgaWYgKGkub2JqNSA9PT0gVU5ERUZJTkVEKSBpLm9iajUgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjUsIHAudmlzaWJpbGl0eTUpO1xuICAgICAgbGlzdC5wdXNoKGkub2JqNSk7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQocC5wcm92aWRlcjYpICYmIHAucHJvdmlkZXI2LmtleS50b2tlbiA9PT0gcXVlcnkuc2VsZWN0b3IpIHtcbiAgICAgIGlmIChpLm9iajYgPT09IFVOREVGSU5FRCkgaS5vYmo2ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI2LCBwLnZpc2liaWxpdHk2KTtcbiAgICAgIGxpc3QucHVzaChpLm9iajYpO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHAucHJvdmlkZXI3KSAmJiBwLnByb3ZpZGVyNy5rZXkudG9rZW4gPT09IHF1ZXJ5LnNlbGVjdG9yKSB7XG4gICAgICBpZiAoaS5vYmo3ID09PSBVTkRFRklORUQpIGkub2JqNyA9IGkuaW5zdGFudGlhdGVQcm92aWRlcihwLnByb3ZpZGVyNywgcC52aXNpYmlsaXR5Nyk7XG4gICAgICBsaXN0LnB1c2goaS5vYmo3KTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudChwLnByb3ZpZGVyOCkgJiYgcC5wcm92aWRlcjgua2V5LnRva2VuID09PSBxdWVyeS5zZWxlY3Rvcikge1xuICAgICAgaWYgKGkub2JqOCA9PT0gVU5ERUZJTkVEKSBpLm9iajggPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjgsIHAudmlzaWJpbGl0eTgpO1xuICAgICAgbGlzdC5wdXNoKGkub2JqOCk7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQocC5wcm92aWRlcjkpICYmIHAucHJvdmlkZXI5LmtleS50b2tlbiA9PT0gcXVlcnkuc2VsZWN0b3IpIHtcbiAgICAgIGlmIChpLm9iajkgPT09IFVOREVGSU5FRCkgaS5vYmo5ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI5LCBwLnZpc2liaWxpdHk5KTtcbiAgICAgIGxpc3QucHVzaChpLm9iajkpO1xuICAgIH1cbiAgfVxufVxuXG4vKipcbiAqIFN0cmF0ZWd5IHVzZWQgYnkgdGhlIGBFbGVtZW50SW5qZWN0b3JgIHdoZW4gdGhlIG51bWJlciBvZiBiaW5kaW5ncyBpcyAxMSBvciBtb3JlLlxuICogSW4gc3VjaCBhIGNhc2UsIHRoZXJlIGFyZSB0b28gbWFueSBmaWVsZHMgdG8gaW5saW5lIChzZWUgRWxlbWVudEluamVjdG9ySW5saW5lU3RyYXRlZ3kpLlxuICovXG5jbGFzcyBFbGVtZW50RGlyZWN0aXZlRHluYW1pY1N0cmF0ZWd5IGltcGxlbWVudHMgX0VsZW1lbnREaXJlY3RpdmVTdHJhdGVneSB7XG4gIGNvbnN0cnVjdG9yKHB1YmxpYyBpbmplY3RvclN0cmF0ZWd5OiBJbmplY3RvckR5bmFtaWNTdHJhdGVneSwgcHVibGljIF9laTogQXBwRWxlbWVudCkge31cblxuICBpbml0KCk6IHZvaWQge1xuICAgIHZhciBpbmogPSB0aGlzLmluamVjdG9yU3RyYXRlZ3k7XG4gICAgdmFyIHAgPSBpbmoucHJvdG9TdHJhdGVneTtcbiAgICBpbmoucmVzZXRDb25zdHJ1Y3Rpb25Db3VudGVyKCk7XG5cbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IHAua2V5SWRzLmxlbmd0aDsgaSsrKSB7XG4gICAgICBpZiAocC5wcm92aWRlcnNbaV0gaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZHNbaV0pICYmXG4gICAgICAgICAgaW5qLm9ianNbaV0gPT09IFVOREVGSU5FRCkge1xuICAgICAgICBpbmoub2Jqc1tpXSA9IGluai5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXJzW2ldLCBwLnZpc2liaWxpdGllc1tpXSk7XG4gICAgICB9XG4gICAgfVxuICB9XG5cbiAgZ2V0Q29tcG9uZW50KCk6IGFueSB7IHJldHVybiB0aGlzLmluamVjdG9yU3RyYXRlZ3kub2Jqc1swXTsgfVxuXG4gIGlzQ29tcG9uZW50S2V5KGtleTogS2V5KTogYm9vbGVhbiB7XG4gICAgdmFyIHAgPSB0aGlzLmluamVjdG9yU3RyYXRlZ3kucHJvdG9TdHJhdGVneTtcbiAgICByZXR1cm4gdGhpcy5fZWkucHJvdG8uZmlyc3RQcm92aWRlcklzQ29tcG9uZW50ICYmIGlzUHJlc2VudChrZXkpICYmIGtleS5pZCA9PT0gcC5rZXlJZHNbMF07XG4gIH1cblxuICBhZGREaXJlY3RpdmVzTWF0Y2hpbmdRdWVyeShxdWVyeTogUXVlcnlNZXRhZGF0YSwgbGlzdDogYW55W10pOiB2b2lkIHtcbiAgICB2YXIgaXN0ID0gdGhpcy5pbmplY3RvclN0cmF0ZWd5O1xuICAgIHZhciBwID0gaXN0LnByb3RvU3RyYXRlZ3k7XG5cbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IHAucHJvdmlkZXJzLmxlbmd0aDsgaSsrKSB7XG4gICAgICBpZiAocC5wcm92aWRlcnNbaV0ua2V5LnRva2VuID09PSBxdWVyeS5zZWxlY3Rvcikge1xuICAgICAgICBpZiAoaXN0Lm9ianNbaV0gPT09IFVOREVGSU5FRCkge1xuICAgICAgICAgIGlzdC5vYmpzW2ldID0gaXN0Lmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcnNbaV0sIHAudmlzaWJpbGl0aWVzW2ldKTtcbiAgICAgICAgfVxuICAgICAgICBsaXN0LnB1c2goaXN0Lm9ianNbaV0pO1xuICAgICAgfVxuICAgIH1cbiAgfVxufVxuXG5leHBvcnQgY2xhc3MgUHJvdG9RdWVyeVJlZiB7XG4gIGNvbnN0cnVjdG9yKHB1YmxpYyBkaXJJbmRleDogbnVtYmVyLCBwdWJsaWMgc2V0dGVyOiBTZXR0ZXJGbiwgcHVibGljIHF1ZXJ5OiBRdWVyeU1ldGFkYXRhKSB7fVxuXG4gIGdldCB1c2VzUHJvcGVydHlTeW50YXgoKTogYm9vbGVhbiB7IHJldHVybiBpc1ByZXNlbnQodGhpcy5zZXR0ZXIpOyB9XG59XG5cbmV4cG9ydCBjbGFzcyBRdWVyeVJlZiB7XG4gIHB1YmxpYyBsaXN0OiBRdWVyeUxpc3Q8YW55PjtcbiAgcHVibGljIGRpcnR5OiBib29sZWFuO1xuXG4gIGNvbnN0cnVjdG9yKHB1YmxpYyBwcm90b1F1ZXJ5UmVmOiBQcm90b1F1ZXJ5UmVmLCBwcml2YXRlIG9yaWdpbmF0b3I6IEFwcEVsZW1lbnQpIHtcbiAgICB0aGlzLmxpc3QgPSBuZXcgUXVlcnlMaXN0PGFueT4oKTtcbiAgICB0aGlzLmRpcnR5ID0gdHJ1ZTtcbiAgfVxuXG4gIGdldCBpc1ZpZXdRdWVyeSgpOiBib29sZWFuIHsgcmV0dXJuIHRoaXMucHJvdG9RdWVyeVJlZi5xdWVyeS5pc1ZpZXdRdWVyeTsgfVxuXG4gIHVwZGF0ZSgpOiB2b2lkIHtcbiAgICBpZiAoIXRoaXMuZGlydHkpIHJldHVybjtcbiAgICB0aGlzLl91cGRhdGUoKTtcbiAgICB0aGlzLmRpcnR5ID0gZmFsc2U7XG5cbiAgICAvLyBUT0RPIGRlbGV0ZSB0aGUgY2hlY2sgb25jZSBvbmx5IGZpZWxkIHF1ZXJpZXMgYXJlIHN1cHBvcnRlZFxuICAgIGlmICh0aGlzLnByb3RvUXVlcnlSZWYudXNlc1Byb3BlcnR5U3ludGF4KSB7XG4gICAgICB2YXIgZGlyID0gdGhpcy5vcmlnaW5hdG9yLmdldERpcmVjdGl2ZUF0SW5kZXgodGhpcy5wcm90b1F1ZXJ5UmVmLmRpckluZGV4KTtcbiAgICAgIGlmICh0aGlzLnByb3RvUXVlcnlSZWYucXVlcnkuZmlyc3QpIHtcbiAgICAgICAgdGhpcy5wcm90b1F1ZXJ5UmVmLnNldHRlcihkaXIsIHRoaXMubGlzdC5sZW5ndGggPiAwID8gdGhpcy5saXN0LmZpcnN0IDogbnVsbCk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICB0aGlzLnByb3RvUXVlcnlSZWYuc2V0dGVyKGRpciwgdGhpcy5saXN0KTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICB0aGlzLmxpc3Qubm90aWZ5T25DaGFuZ2VzKCk7XG4gIH1cblxuICBwcml2YXRlIF91cGRhdGUoKTogdm9pZCB7XG4gICAgdmFyIGFnZ3JlZ2F0b3IgPSBbXTtcbiAgICBpZiAodGhpcy5wcm90b1F1ZXJ5UmVmLnF1ZXJ5LmlzVmlld1F1ZXJ5KSB7XG4gICAgICAvLyBpbnRlbnRpb25hbGx5IHNraXBwaW5nIG9yaWdpbmF0b3IgZm9yIHZpZXcgcXVlcmllcy5cbiAgICAgIHZhciBuZXN0ZWRWaWV3ID0gdGhpcy5vcmlnaW5hdG9yLmNvbXBvbmVudFZpZXc7XG4gICAgICBpZiAoaXNQcmVzZW50KG5lc3RlZFZpZXcpKSB0aGlzLl92aXNpdFZpZXcobmVzdGVkVmlldywgYWdncmVnYXRvcik7XG4gICAgfSBlbHNlIHtcbiAgICAgIHRoaXMuX3Zpc2l0KHRoaXMub3JpZ2luYXRvciwgYWdncmVnYXRvcik7XG4gICAgfVxuICAgIHRoaXMubGlzdC5yZXNldChhZ2dyZWdhdG9yKTtcbiAgfTtcblxuICBwcml2YXRlIF92aXNpdChpbmo6IEFwcEVsZW1lbnQsIGFnZ3JlZ2F0b3I6IGFueVtdKTogdm9pZCB7XG4gICAgdmFyIHZpZXcgPSBpbmoucGFyZW50VmlldztcbiAgICB2YXIgc3RhcnRJZHggPSBpbmoucHJvdG8uaW5kZXg7XG4gICAgZm9yICh2YXIgaSA9IHN0YXJ0SWR4OyBpIDwgdmlldy5hcHBFbGVtZW50cy5sZW5ndGg7IGkrKykge1xuICAgICAgdmFyIGN1ckluaiA9IHZpZXcuYXBwRWxlbWVudHNbaV07XG4gICAgICAvLyBUaGUgZmlyc3QgaW5qZWN0b3IgYWZ0ZXIgaW5qLCB0aGF0IGlzIG91dHNpZGUgdGhlIHN1YnRyZWUgcm9vdGVkIGF0XG4gICAgICAvLyBpbmogaGFzIHRvIGhhdmUgYSBudWxsIHBhcmVudCBvciBhIHBhcmVudCB0aGF0IGlzIGFuIGFuY2VzdG9yIG9mIGluai5cbiAgICAgIGlmIChpID4gc3RhcnRJZHggJiYgKGlzQmxhbmsoY3VySW5qLnBhcmVudCkgfHwgY3VySW5qLnBhcmVudC5wcm90by5pbmRleCA8IHN0YXJ0SWR4KSkge1xuICAgICAgICBicmVhaztcbiAgICAgIH1cblxuICAgICAgaWYgKCF0aGlzLnByb3RvUXVlcnlSZWYucXVlcnkuZGVzY2VuZGFudHMgJiZcbiAgICAgICAgICAhKGN1ckluai5wYXJlbnQgPT0gdGhpcy5vcmlnaW5hdG9yIHx8IGN1ckluaiA9PSB0aGlzLm9yaWdpbmF0b3IpKVxuICAgICAgICBjb250aW51ZTtcblxuICAgICAgLy8gV2UgdmlzaXQgdGhlIHZpZXcgY29udGFpbmVyKFZDKSB2aWV3cyByaWdodCBhZnRlciB0aGUgaW5qZWN0b3IgdGhhdCBjb250YWluc1xuICAgICAgLy8gdGhlIFZDLiBUaGVvcmV0aWNhbGx5LCB0aGF0IG1pZ2h0IG5vdCBiZSB0aGUgcmlnaHQgb3JkZXIgaWYgdGhlcmUgYXJlXG4gICAgICAvLyBjaGlsZCBpbmplY3RvcnMgb2Ygc2FpZCBpbmplY3Rvci4gTm90IGNsZWFyIHdoZXRoZXIgaWYgc3VjaCBjYXNlIGNhblxuICAgICAgLy8gZXZlbiBiZSBjb25zdHJ1Y3RlZCB3aXRoIHRoZSBjdXJyZW50IGFwaXMuXG4gICAgICB0aGlzLl92aXNpdEluamVjdG9yKGN1ckluaiwgYWdncmVnYXRvcik7XG4gICAgICB0aGlzLl92aXNpdFZpZXdDb250YWluZXJWaWV3cyhjdXJJbmoubmVzdGVkVmlld3MsIGFnZ3JlZ2F0b3IpO1xuICAgIH1cbiAgfVxuXG4gIHByaXZhdGUgX3Zpc2l0SW5qZWN0b3IoaW5qOiBBcHBFbGVtZW50LCBhZ2dyZWdhdG9yOiBhbnlbXSkge1xuICAgIGlmICh0aGlzLnByb3RvUXVlcnlSZWYucXVlcnkuaXNWYXJCaW5kaW5nUXVlcnkpIHtcbiAgICAgIHRoaXMuX2FnZ3JlZ2F0ZVZhcmlhYmxlQmluZGluZyhpbmosIGFnZ3JlZ2F0b3IpO1xuICAgIH0gZWxzZSB7XG4gICAgICB0aGlzLl9hZ2dyZWdhdGVEaXJlY3RpdmUoaW5qLCBhZ2dyZWdhdG9yKTtcbiAgICB9XG4gIH1cblxuICBwcml2YXRlIF92aXNpdFZpZXdDb250YWluZXJWaWV3cyh2aWV3czogQXBwVmlld1tdLCBhZ2dyZWdhdG9yOiBhbnlbXSkge1xuICAgIGlmIChpc1ByZXNlbnQodmlld3MpKSB7XG4gICAgICBmb3IgKHZhciBqID0gMDsgaiA8IHZpZXdzLmxlbmd0aDsgaisrKSB7XG4gICAgICAgIHRoaXMuX3Zpc2l0Vmlldyh2aWV3c1tqXSwgYWdncmVnYXRvcik7XG4gICAgICB9XG4gICAgfVxuICB9XG5cbiAgcHJpdmF0ZSBfdmlzaXRWaWV3KHZpZXc6IEFwcFZpZXcsIGFnZ3JlZ2F0b3I6IGFueVtdKSB7XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCB2aWV3LmFwcEVsZW1lbnRzLmxlbmd0aDsgaSsrKSB7XG4gICAgICB2YXIgaW5qID0gdmlldy5hcHBFbGVtZW50c1tpXTtcbiAgICAgIHRoaXMuX3Zpc2l0SW5qZWN0b3IoaW5qLCBhZ2dyZWdhdG9yKTtcbiAgICAgIHRoaXMuX3Zpc2l0Vmlld0NvbnRhaW5lclZpZXdzKGluai5uZXN0ZWRWaWV3cywgYWdncmVnYXRvcik7XG4gICAgfVxuICB9XG5cbiAgcHJpdmF0ZSBfYWdncmVnYXRlVmFyaWFibGVCaW5kaW5nKGluajogQXBwRWxlbWVudCwgYWdncmVnYXRvcjogYW55W10pOiB2b2lkIHtcbiAgICB2YXIgdmIgPSB0aGlzLnByb3RvUXVlcnlSZWYucXVlcnkudmFyQmluZGluZ3M7XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCB2Yi5sZW5ndGg7ICsraSkge1xuICAgICAgaWYgKGluai5oYXNWYXJpYWJsZUJpbmRpbmcodmJbaV0pKSB7XG4gICAgICAgIGFnZ3JlZ2F0b3IucHVzaChpbmouZ2V0VmFyaWFibGVCaW5kaW5nKHZiW2ldKSk7XG4gICAgICB9XG4gICAgfVxuICB9XG5cbiAgcHJpdmF0ZSBfYWdncmVnYXRlRGlyZWN0aXZlKGluajogQXBwRWxlbWVudCwgYWdncmVnYXRvcjogYW55W10pOiB2b2lkIHtcbiAgICBpbmouYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkodGhpcy5wcm90b1F1ZXJ5UmVmLnF1ZXJ5LCBhZ2dyZWdhdG9yKTtcbiAgfVxufVxuXG5jbGFzcyBfQ29tcG9uZW50Vmlld0NoYW5nZURldGVjdG9yUmVmIGV4dGVuZHMgQ2hhbmdlRGV0ZWN0b3JSZWYge1xuICBjb25zdHJ1Y3Rvcihwcml2YXRlIF9hcHBFbGVtZW50OiBBcHBFbGVtZW50KSB7IHN1cGVyKCk7IH1cblxuICBtYXJrRm9yQ2hlY2soKTogdm9pZCB7IHRoaXMuX2FwcEVsZW1lbnQuY29tcG9uZW50Vmlldy5jaGFuZ2VEZXRlY3Rvci5yZWYubWFya0ZvckNoZWNrKCk7IH1cbiAgZGV0YWNoKCk6IHZvaWQgeyB0aGlzLl9hcHBFbGVtZW50LmNvbXBvbmVudFZpZXcuY2hhbmdlRGV0ZWN0b3IucmVmLmRldGFjaCgpOyB9XG4gIGRldGVjdENoYW5nZXMoKTogdm9pZCB7IHRoaXMuX2FwcEVsZW1lbnQuY29tcG9uZW50Vmlldy5jaGFuZ2VEZXRlY3Rvci5yZWYuZGV0ZWN0Q2hhbmdlcygpOyB9XG4gIGNoZWNrTm9DaGFuZ2VzKCk6IHZvaWQgeyB0aGlzLl9hcHBFbGVtZW50LmNvbXBvbmVudFZpZXcuY2hhbmdlRGV0ZWN0b3IucmVmLmNoZWNrTm9DaGFuZ2VzKCk7IH1cbiAgcmVhdHRhY2goKTogdm9pZCB7IHRoaXMuX2FwcEVsZW1lbnQuY29tcG9uZW50Vmlldy5jaGFuZ2VEZXRlY3Rvci5yZWYucmVhdHRhY2goKTsgfVxufVxuIl19