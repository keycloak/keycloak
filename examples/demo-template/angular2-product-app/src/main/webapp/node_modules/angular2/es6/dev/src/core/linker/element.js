import { isPresent, isBlank } from 'angular2/src/facade/lang';
import { BaseException } from 'angular2/src/facade/exceptions';
import { ListWrapper, StringMapWrapper } from 'angular2/src/facade/collection';
import { Injector, Key, Dependency, Provider, NoProviderError } from 'angular2/src/core/di';
import { mergeResolvedProviders } from 'angular2/src/core/di/provider';
import { UNDEFINED, ProtoInjector, Visibility, InjectorInlineStrategy, ProviderWithVisibility } from 'angular2/src/core/di/injector';
import { resolveProvider, ResolvedFactory, ResolvedProvider_ } from 'angular2/src/core/di/provider';
import { AttributeMetadata, QueryMetadata } from '../metadata/di';
import { ViewType } from './view_type';
import { ElementRef_ } from './element_ref';
import { ViewContainerRef } from './view_container_ref';
import { ElementRef } from './element_ref';
import { Renderer } from 'angular2/src/core/render/api';
import { TemplateRef, TemplateRef_ } from './template_ref';
import { DirectiveMetadata, ComponentMetadata } from '../metadata/directives';
import { ChangeDetectorRef } from 'angular2/src/core/change_detection/change_detection';
import { QueryList } from './query_list';
import { reflector } from 'angular2/src/core/reflection/reflection';
import { PipeProvider } from 'angular2/src/core/pipes/pipe_provider';
import { ViewContainerRef_ } from "./view_container_ref";
var _staticKeys;
export class StaticKeys {
    constructor() {
        this.templateRefId = Key.get(TemplateRef).id;
        this.viewContainerId = Key.get(ViewContainerRef).id;
        this.changeDetectorRefId = Key.get(ChangeDetectorRef).id;
        this.elementRefId = Key.get(ElementRef).id;
        this.rendererId = Key.get(Renderer).id;
    }
    static instance() {
        if (isBlank(_staticKeys))
            _staticKeys = new StaticKeys();
        return _staticKeys;
    }
}
export class DirectiveDependency extends Dependency {
    constructor(key, optional, lowerBoundVisibility, upperBoundVisibility, properties, attributeName, queryDecorator) {
        super(key, optional, lowerBoundVisibility, upperBoundVisibility, properties);
        this.attributeName = attributeName;
        this.queryDecorator = queryDecorator;
        this._verify();
    }
    /** @internal */
    _verify() {
        var count = 0;
        if (isPresent(this.queryDecorator))
            count++;
        if (isPresent(this.attributeName))
            count++;
        if (count > 1)
            throw new BaseException('A directive injectable can contain only one of the following @Attribute or @Query.');
    }
    static createFrom(d) {
        return new DirectiveDependency(d.key, d.optional, d.lowerBoundVisibility, d.upperBoundVisibility, d.properties, DirectiveDependency._attributeName(d.properties), DirectiveDependency._query(d.properties));
    }
    /** @internal */
    static _attributeName(properties) {
        var p = properties.find(p => p instanceof AttributeMetadata);
        return isPresent(p) ? p.attributeName : null;
    }
    /** @internal */
    static _query(properties) {
        return properties.find(p => p instanceof QueryMetadata);
    }
}
export class DirectiveProvider extends ResolvedProvider_ {
    constructor(key, factory, deps, isComponent, providers, viewProviders, queries) {
        super(key, [new ResolvedFactory(factory, deps)], false);
        this.isComponent = isComponent;
        this.providers = providers;
        this.viewProviders = viewProviders;
        this.queries = queries;
    }
    get displayName() { return this.key.displayName; }
    static createFromType(type, meta) {
        var provider = new Provider(type, { useClass: type });
        if (isBlank(meta)) {
            meta = new DirectiveMetadata();
        }
        var rb = resolveProvider(provider);
        var rf = rb.resolvedFactories[0];
        var deps = rf.dependencies.map(DirectiveDependency.createFrom);
        var isComponent = meta instanceof ComponentMetadata;
        var resolvedProviders = isPresent(meta.providers) ? Injector.resolve(meta.providers) : null;
        var resolvedViewProviders = meta instanceof ComponentMetadata && isPresent(meta.viewProviders) ?
            Injector.resolve(meta.viewProviders) :
            null;
        var queries = [];
        if (isPresent(meta.queries)) {
            StringMapWrapper.forEach(meta.queries, (meta, fieldName) => {
                var setter = reflector.setter(fieldName);
                queries.push(new QueryMetadataWithSetter(setter, meta));
            });
        }
        // queries passed into the constructor.
        // TODO: remove this after constructor queries are no longer supported
        deps.forEach(d => {
            if (isPresent(d.queryDecorator)) {
                queries.push(new QueryMetadataWithSetter(null, d.queryDecorator));
            }
        });
        return new DirectiveProvider(rb.key, rf.factory, deps, isComponent, resolvedProviders, resolvedViewProviders, queries);
    }
}
export class QueryMetadataWithSetter {
    constructor(setter, metadata) {
        this.setter = setter;
        this.metadata = metadata;
    }
}
function setProvidersVisibility(providers, visibility, result) {
    for (var i = 0; i < providers.length; i++) {
        result.set(providers[i].key.id, visibility);
    }
}
export class AppProtoElement {
    constructor(firstProviderIsComponent, index, attributes, pwvs, protoQueryRefs, directiveVariableBindings) {
        this.firstProviderIsComponent = firstProviderIsComponent;
        this.index = index;
        this.attributes = attributes;
        this.protoQueryRefs = protoQueryRefs;
        this.directiveVariableBindings = directiveVariableBindings;
        var length = pwvs.length;
        if (length > 0) {
            this.protoInjector = new ProtoInjector(pwvs);
        }
        else {
            this.protoInjector = null;
            this.protoQueryRefs = [];
        }
    }
    static create(metadataCache, index, attributes, directiveTypes, directiveVariableBindings) {
        var componentDirProvider = null;
        var mergedProvidersMap = new Map();
        var providerVisibilityMap = new Map();
        var providers = ListWrapper.createGrowableSize(directiveTypes.length);
        var protoQueryRefs = [];
        for (var i = 0; i < directiveTypes.length; i++) {
            var dirProvider = metadataCache.getResolvedDirectiveMetadata(directiveTypes[i]);
            providers[i] = new ProviderWithVisibility(dirProvider, dirProvider.isComponent ? Visibility.PublicAndPrivate : Visibility.Public);
            if (dirProvider.isComponent) {
                componentDirProvider = dirProvider;
            }
            else {
                if (isPresent(dirProvider.providers)) {
                    mergeResolvedProviders(dirProvider.providers, mergedProvidersMap);
                    setProvidersVisibility(dirProvider.providers, Visibility.Public, providerVisibilityMap);
                }
            }
            if (isPresent(dirProvider.viewProviders)) {
                mergeResolvedProviders(dirProvider.viewProviders, mergedProvidersMap);
                setProvidersVisibility(dirProvider.viewProviders, Visibility.Private, providerVisibilityMap);
            }
            for (var queryIdx = 0; queryIdx < dirProvider.queries.length; queryIdx++) {
                var q = dirProvider.queries[queryIdx];
                protoQueryRefs.push(new ProtoQueryRef(i, q.setter, q.metadata));
            }
        }
        if (isPresent(componentDirProvider) && isPresent(componentDirProvider.providers)) {
            // directive providers need to be prioritized over component providers
            mergeResolvedProviders(componentDirProvider.providers, mergedProvidersMap);
            setProvidersVisibility(componentDirProvider.providers, Visibility.Public, providerVisibilityMap);
        }
        mergedProvidersMap.forEach((provider, _) => {
            providers.push(new ProviderWithVisibility(provider, providerVisibilityMap.get(provider.key.id)));
        });
        return new AppProtoElement(isPresent(componentDirProvider), index, attributes, providers, protoQueryRefs, directiveVariableBindings);
    }
    getProviderAtIndex(index) { return this.protoInjector.getProviderAtIndex(index); }
}
class _Context {
    constructor(element, componentElement, injector) {
        this.element = element;
        this.componentElement = componentElement;
        this.injector = injector;
    }
}
export class InjectorWithHostBoundary {
    constructor(injector, hostInjectorBoundary) {
        this.injector = injector;
        this.hostInjectorBoundary = hostInjectorBoundary;
    }
}
export class AppElement {
    constructor(proto, parentView, parent, nativeElement, embeddedViewFactory) {
        this.proto = proto;
        this.parentView = parentView;
        this.parent = parent;
        this.nativeElement = nativeElement;
        this.embeddedViewFactory = embeddedViewFactory;
        this.nestedViews = null;
        this.componentView = null;
        this.ref = new ElementRef_(this);
        var parentInjector = isPresent(parent) ? parent._injector : parentView.parentInjector;
        if (isPresent(this.proto.protoInjector)) {
            var isBoundary;
            if (isPresent(parent) && isPresent(parent.proto.protoInjector)) {
                isBoundary = false;
            }
            else {
                isBoundary = parentView.hostInjectorBoundary;
            }
            this._queryStrategy = this._buildQueryStrategy();
            this._injector = new Injector(this.proto.protoInjector, parentInjector, isBoundary, this, () => this._debugContext());
            // we couple ourselves to the injector strategy to avoid polymorphic calls
            var injectorStrategy = this._injector.internalStrategy;
            this._strategy = injectorStrategy instanceof InjectorInlineStrategy ?
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
    static getViewParentInjector(parentViewType, containerAppElement, imperativelyCreatedProviders, rootInjector) {
        var parentInjector;
        var hostInjectorBoundary;
        switch (parentViewType) {
            case ViewType.COMPONENT:
                parentInjector = containerAppElement._injector;
                hostInjectorBoundary = true;
                break;
            case ViewType.EMBEDDED:
                parentInjector = isPresent(containerAppElement.proto.protoInjector) ?
                    containerAppElement._injector.parent :
                    containerAppElement._injector;
                hostInjectorBoundary = containerAppElement._injector.hostBoundary;
                break;
            case ViewType.HOST:
                if (isPresent(containerAppElement)) {
                    // host view is attached to a container
                    parentInjector = isPresent(containerAppElement.proto.protoInjector) ?
                        containerAppElement._injector.parent :
                        containerAppElement._injector;
                    if (isPresent(imperativelyCreatedProviders)) {
                        var imperativeProvidersWithVisibility = imperativelyCreatedProviders.map(p => new ProviderWithVisibility(p, Visibility.Public));
                        // The imperative injector is similar to having an element between
                        // the dynamic-loaded component and its parent => no boundary between
                        // the component and imperativelyCreatedInjector.
                        parentInjector = new Injector(new ProtoInjector(imperativeProvidersWithVisibility), parentInjector, true, null, null);
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
    }
    attachComponentView(componentView) { this.componentView = componentView; }
    _debugContext() {
        var c = this.parentView.getDebugContext(this, null, null);
        return isPresent(c) ? new _Context(c.element, c.componentElement, c.injector) : null;
    }
    hasVariableBinding(name) {
        var vb = this.proto.directiveVariableBindings;
        return isPresent(vb) && StringMapWrapper.contains(vb, name);
    }
    getVariableBinding(name) {
        var index = this.proto.directiveVariableBindings[name];
        return isPresent(index) ? this.getDirectiveAtIndex(index) : this.getElementRef();
    }
    get(token) { return this._injector.get(token); }
    hasDirective(type) { return isPresent(this._injector.getOptional(type)); }
    getComponent() { return isPresent(this._strategy) ? this._strategy.getComponent() : null; }
    getInjector() { return this._injector; }
    getElementRef() { return this.ref; }
    getViewContainerRef() { return new ViewContainerRef_(this); }
    getTemplateRef() {
        if (isPresent(this.embeddedViewFactory)) {
            return new TemplateRef_(this.ref);
        }
        return null;
    }
    getDependency(injector, provider, dep) {
        if (provider instanceof DirectiveProvider) {
            var dirDep = dep;
            if (isPresent(dirDep.attributeName))
                return this._buildAttribute(dirDep);
            if (isPresent(dirDep.queryDecorator))
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
                if (isBlank(tr) && !dirDep.optional) {
                    throw new NoProviderError(null, dirDep.key);
                }
                return tr;
            }
            if (dirDep.key.id === StaticKeys.instance().rendererId) {
                return this.parentView.renderer;
            }
        }
        else if (provider instanceof PipeProvider) {
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
        return UNDEFINED;
    }
    _buildAttribute(dep) {
        var attributes = this.proto.attributes;
        if (isPresent(attributes) && StringMapWrapper.contains(attributes, dep.attributeName)) {
            return attributes[dep.attributeName];
        }
        else {
            return null;
        }
    }
    addDirectivesMatchingQuery(query, list) {
        var templateRef = this.getTemplateRef();
        if (query.selector === TemplateRef && isPresent(templateRef)) {
            list.push(templateRef);
        }
        if (this._strategy != null) {
            this._strategy.addDirectivesMatchingQuery(query, list);
        }
    }
    _buildQueryStrategy() {
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
    }
    getDirectiveAtIndex(index) { return this._injector.getAt(index); }
    ngAfterViewChecked() {
        if (isPresent(this._queryStrategy))
            this._queryStrategy.updateViewQueries();
    }
    ngAfterContentChecked() {
        if (isPresent(this._queryStrategy))
            this._queryStrategy.updateContentQueries();
    }
    traverseAndSetQueriesAsDirty() {
        var inj = this;
        while (isPresent(inj)) {
            inj._setQueriesAsDirty();
            if (isBlank(inj.parent) && inj.parentView.proto.type === ViewType.EMBEDDED) {
                inj = inj.parentView.containerAppElement;
            }
            else {
                inj = inj.parent;
            }
        }
    }
    _setQueriesAsDirty() {
        if (isPresent(this._queryStrategy)) {
            this._queryStrategy.setContentQueriesAsDirty();
        }
        if (this.parentView.proto.type === ViewType.COMPONENT) {
            this.parentView.containerAppElement._queryStrategy.setViewQueriesAsDirty();
        }
    }
}
class _EmptyQueryStrategy {
    setContentQueriesAsDirty() { }
    setViewQueriesAsDirty() { }
    updateContentQueries() { }
    updateViewQueries() { }
    findQuery(query) {
        throw new BaseException(`Cannot find query for directive ${query}.`);
    }
}
var _emptyQueryStrategy = new _EmptyQueryStrategy();
class InlineQueryStrategy {
    constructor(ei) {
        var protoRefs = ei.proto.protoQueryRefs;
        if (protoRefs.length > 0)
            this.query0 = new QueryRef(protoRefs[0], ei);
        if (protoRefs.length > 1)
            this.query1 = new QueryRef(protoRefs[1], ei);
        if (protoRefs.length > 2)
            this.query2 = new QueryRef(protoRefs[2], ei);
    }
    setContentQueriesAsDirty() {
        if (isPresent(this.query0) && !this.query0.isViewQuery)
            this.query0.dirty = true;
        if (isPresent(this.query1) && !this.query1.isViewQuery)
            this.query1.dirty = true;
        if (isPresent(this.query2) && !this.query2.isViewQuery)
            this.query2.dirty = true;
    }
    setViewQueriesAsDirty() {
        if (isPresent(this.query0) && this.query0.isViewQuery)
            this.query0.dirty = true;
        if (isPresent(this.query1) && this.query1.isViewQuery)
            this.query1.dirty = true;
        if (isPresent(this.query2) && this.query2.isViewQuery)
            this.query2.dirty = true;
    }
    updateContentQueries() {
        if (isPresent(this.query0) && !this.query0.isViewQuery) {
            this.query0.update();
        }
        if (isPresent(this.query1) && !this.query1.isViewQuery) {
            this.query1.update();
        }
        if (isPresent(this.query2) && !this.query2.isViewQuery) {
            this.query2.update();
        }
    }
    updateViewQueries() {
        if (isPresent(this.query0) && this.query0.isViewQuery) {
            this.query0.update();
        }
        if (isPresent(this.query1) && this.query1.isViewQuery) {
            this.query1.update();
        }
        if (isPresent(this.query2) && this.query2.isViewQuery) {
            this.query2.update();
        }
    }
    findQuery(query) {
        if (isPresent(this.query0) && this.query0.protoQueryRef.query === query) {
            return this.query0;
        }
        if (isPresent(this.query1) && this.query1.protoQueryRef.query === query) {
            return this.query1;
        }
        if (isPresent(this.query2) && this.query2.protoQueryRef.query === query) {
            return this.query2;
        }
        throw new BaseException(`Cannot find query for directive ${query}.`);
    }
}
InlineQueryStrategy.NUMBER_OF_SUPPORTED_QUERIES = 3;
class DynamicQueryStrategy {
    constructor(ei) {
        this.queries = ei.proto.protoQueryRefs.map(p => new QueryRef(p, ei));
    }
    setContentQueriesAsDirty() {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (!q.isViewQuery)
                q.dirty = true;
        }
    }
    setViewQueriesAsDirty() {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (q.isViewQuery)
                q.dirty = true;
        }
    }
    updateContentQueries() {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (!q.isViewQuery) {
                q.update();
            }
        }
    }
    updateViewQueries() {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (q.isViewQuery) {
                q.update();
            }
        }
    }
    findQuery(query) {
        for (var i = 0; i < this.queries.length; ++i) {
            var q = this.queries[i];
            if (q.protoQueryRef.query === query) {
                return q;
            }
        }
        throw new BaseException(`Cannot find query for directive ${query}.`);
    }
}
/**
 * Strategy used by the `ElementInjector` when the number of providers is 10 or less.
 * In such a case, inlining fields is beneficial for performances.
 */
class ElementDirectiveInlineStrategy {
    constructor(injectorStrategy, _ei) {
        this.injectorStrategy = injectorStrategy;
        this._ei = _ei;
    }
    init() {
        var i = this.injectorStrategy;
        var p = i.protoStrategy;
        i.resetConstructionCounter();
        if (p.provider0 instanceof DirectiveProvider && isPresent(p.keyId0) && i.obj0 === UNDEFINED)
            i.obj0 = i.instantiateProvider(p.provider0, p.visibility0);
        if (p.provider1 instanceof DirectiveProvider && isPresent(p.keyId1) && i.obj1 === UNDEFINED)
            i.obj1 = i.instantiateProvider(p.provider1, p.visibility1);
        if (p.provider2 instanceof DirectiveProvider && isPresent(p.keyId2) && i.obj2 === UNDEFINED)
            i.obj2 = i.instantiateProvider(p.provider2, p.visibility2);
        if (p.provider3 instanceof DirectiveProvider && isPresent(p.keyId3) && i.obj3 === UNDEFINED)
            i.obj3 = i.instantiateProvider(p.provider3, p.visibility3);
        if (p.provider4 instanceof DirectiveProvider && isPresent(p.keyId4) && i.obj4 === UNDEFINED)
            i.obj4 = i.instantiateProvider(p.provider4, p.visibility4);
        if (p.provider5 instanceof DirectiveProvider && isPresent(p.keyId5) && i.obj5 === UNDEFINED)
            i.obj5 = i.instantiateProvider(p.provider5, p.visibility5);
        if (p.provider6 instanceof DirectiveProvider && isPresent(p.keyId6) && i.obj6 === UNDEFINED)
            i.obj6 = i.instantiateProvider(p.provider6, p.visibility6);
        if (p.provider7 instanceof DirectiveProvider && isPresent(p.keyId7) && i.obj7 === UNDEFINED)
            i.obj7 = i.instantiateProvider(p.provider7, p.visibility7);
        if (p.provider8 instanceof DirectiveProvider && isPresent(p.keyId8) && i.obj8 === UNDEFINED)
            i.obj8 = i.instantiateProvider(p.provider8, p.visibility8);
        if (p.provider9 instanceof DirectiveProvider && isPresent(p.keyId9) && i.obj9 === UNDEFINED)
            i.obj9 = i.instantiateProvider(p.provider9, p.visibility9);
    }
    getComponent() { return this.injectorStrategy.obj0; }
    isComponentKey(key) {
        return this._ei.proto.firstProviderIsComponent && isPresent(key) &&
            key.id === this.injectorStrategy.protoStrategy.keyId0;
    }
    addDirectivesMatchingQuery(query, list) {
        var i = this.injectorStrategy;
        var p = i.protoStrategy;
        if (isPresent(p.provider0) && p.provider0.key.token === query.selector) {
            if (i.obj0 === UNDEFINED)
                i.obj0 = i.instantiateProvider(p.provider0, p.visibility0);
            list.push(i.obj0);
        }
        if (isPresent(p.provider1) && p.provider1.key.token === query.selector) {
            if (i.obj1 === UNDEFINED)
                i.obj1 = i.instantiateProvider(p.provider1, p.visibility1);
            list.push(i.obj1);
        }
        if (isPresent(p.provider2) && p.provider2.key.token === query.selector) {
            if (i.obj2 === UNDEFINED)
                i.obj2 = i.instantiateProvider(p.provider2, p.visibility2);
            list.push(i.obj2);
        }
        if (isPresent(p.provider3) && p.provider3.key.token === query.selector) {
            if (i.obj3 === UNDEFINED)
                i.obj3 = i.instantiateProvider(p.provider3, p.visibility3);
            list.push(i.obj3);
        }
        if (isPresent(p.provider4) && p.provider4.key.token === query.selector) {
            if (i.obj4 === UNDEFINED)
                i.obj4 = i.instantiateProvider(p.provider4, p.visibility4);
            list.push(i.obj4);
        }
        if (isPresent(p.provider5) && p.provider5.key.token === query.selector) {
            if (i.obj5 === UNDEFINED)
                i.obj5 = i.instantiateProvider(p.provider5, p.visibility5);
            list.push(i.obj5);
        }
        if (isPresent(p.provider6) && p.provider6.key.token === query.selector) {
            if (i.obj6 === UNDEFINED)
                i.obj6 = i.instantiateProvider(p.provider6, p.visibility6);
            list.push(i.obj6);
        }
        if (isPresent(p.provider7) && p.provider7.key.token === query.selector) {
            if (i.obj7 === UNDEFINED)
                i.obj7 = i.instantiateProvider(p.provider7, p.visibility7);
            list.push(i.obj7);
        }
        if (isPresent(p.provider8) && p.provider8.key.token === query.selector) {
            if (i.obj8 === UNDEFINED)
                i.obj8 = i.instantiateProvider(p.provider8, p.visibility8);
            list.push(i.obj8);
        }
        if (isPresent(p.provider9) && p.provider9.key.token === query.selector) {
            if (i.obj9 === UNDEFINED)
                i.obj9 = i.instantiateProvider(p.provider9, p.visibility9);
            list.push(i.obj9);
        }
    }
}
/**
 * Strategy used by the `ElementInjector` when the number of bindings is 11 or more.
 * In such a case, there are too many fields to inline (see ElementInjectorInlineStrategy).
 */
class ElementDirectiveDynamicStrategy {
    constructor(injectorStrategy, _ei) {
        this.injectorStrategy = injectorStrategy;
        this._ei = _ei;
    }
    init() {
        var inj = this.injectorStrategy;
        var p = inj.protoStrategy;
        inj.resetConstructionCounter();
        for (var i = 0; i < p.keyIds.length; i++) {
            if (p.providers[i] instanceof DirectiveProvider && isPresent(p.keyIds[i]) &&
                inj.objs[i] === UNDEFINED) {
                inj.objs[i] = inj.instantiateProvider(p.providers[i], p.visibilities[i]);
            }
        }
    }
    getComponent() { return this.injectorStrategy.objs[0]; }
    isComponentKey(key) {
        var p = this.injectorStrategy.protoStrategy;
        return this._ei.proto.firstProviderIsComponent && isPresent(key) && key.id === p.keyIds[0];
    }
    addDirectivesMatchingQuery(query, list) {
        var ist = this.injectorStrategy;
        var p = ist.protoStrategy;
        for (var i = 0; i < p.providers.length; i++) {
            if (p.providers[i].key.token === query.selector) {
                if (ist.objs[i] === UNDEFINED) {
                    ist.objs[i] = ist.instantiateProvider(p.providers[i], p.visibilities[i]);
                }
                list.push(ist.objs[i]);
            }
        }
    }
}
export class ProtoQueryRef {
    constructor(dirIndex, setter, query) {
        this.dirIndex = dirIndex;
        this.setter = setter;
        this.query = query;
    }
    get usesPropertySyntax() { return isPresent(this.setter); }
}
export class QueryRef {
    constructor(protoQueryRef, originator) {
        this.protoQueryRef = protoQueryRef;
        this.originator = originator;
        this.list = new QueryList();
        this.dirty = true;
    }
    get isViewQuery() { return this.protoQueryRef.query.isViewQuery; }
    update() {
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
    }
    _update() {
        var aggregator = [];
        if (this.protoQueryRef.query.isViewQuery) {
            // intentionally skipping originator for view queries.
            var nestedView = this.originator.componentView;
            if (isPresent(nestedView))
                this._visitView(nestedView, aggregator);
        }
        else {
            this._visit(this.originator, aggregator);
        }
        this.list.reset(aggregator);
    }
    ;
    _visit(inj, aggregator) {
        var view = inj.parentView;
        var startIdx = inj.proto.index;
        for (var i = startIdx; i < view.appElements.length; i++) {
            var curInj = view.appElements[i];
            // The first injector after inj, that is outside the subtree rooted at
            // inj has to have a null parent or a parent that is an ancestor of inj.
            if (i > startIdx && (isBlank(curInj.parent) || curInj.parent.proto.index < startIdx)) {
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
    }
    _visitInjector(inj, aggregator) {
        if (this.protoQueryRef.query.isVarBindingQuery) {
            this._aggregateVariableBinding(inj, aggregator);
        }
        else {
            this._aggregateDirective(inj, aggregator);
        }
    }
    _visitViewContainerViews(views, aggregator) {
        if (isPresent(views)) {
            for (var j = 0; j < views.length; j++) {
                this._visitView(views[j], aggregator);
            }
        }
    }
    _visitView(view, aggregator) {
        for (var i = 0; i < view.appElements.length; i++) {
            var inj = view.appElements[i];
            this._visitInjector(inj, aggregator);
            this._visitViewContainerViews(inj.nestedViews, aggregator);
        }
    }
    _aggregateVariableBinding(inj, aggregator) {
        var vb = this.protoQueryRef.query.varBindings;
        for (var i = 0; i < vb.length; ++i) {
            if (inj.hasVariableBinding(vb[i])) {
                aggregator.push(inj.getVariableBinding(vb[i]));
            }
        }
    }
    _aggregateDirective(inj, aggregator) {
        inj.addDirectivesMatchingQuery(this.protoQueryRef.query, aggregator);
    }
}
class _ComponentViewChangeDetectorRef extends ChangeDetectorRef {
    constructor(_appElement) {
        super();
        this._appElement = _appElement;
    }
    markForCheck() { this._appElement.componentView.changeDetector.ref.markForCheck(); }
    detach() { this._appElement.componentView.changeDetector.ref.detach(); }
    detectChanges() { this._appElement.componentView.changeDetector.ref.detectChanges(); }
    checkNoChanges() { this._appElement.componentView.changeDetector.ref.checkNoChanges(); }
    reattach() { this._appElement.componentView.changeDetector.ref.reattach(); }
}
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZWxlbWVudC5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbImFuZ3VsYXIyL3NyYy9jb3JlL2xpbmtlci9lbGVtZW50LnRzIl0sIm5hbWVzIjpbIlN0YXRpY0tleXMiLCJTdGF0aWNLZXlzLmNvbnN0cnVjdG9yIiwiU3RhdGljS2V5cy5pbnN0YW5jZSIsIkRpcmVjdGl2ZURlcGVuZGVuY3kiLCJEaXJlY3RpdmVEZXBlbmRlbmN5LmNvbnN0cnVjdG9yIiwiRGlyZWN0aXZlRGVwZW5kZW5jeS5fdmVyaWZ5IiwiRGlyZWN0aXZlRGVwZW5kZW5jeS5jcmVhdGVGcm9tIiwiRGlyZWN0aXZlRGVwZW5kZW5jeS5fYXR0cmlidXRlTmFtZSIsIkRpcmVjdGl2ZURlcGVuZGVuY3kuX3F1ZXJ5IiwiRGlyZWN0aXZlUHJvdmlkZXIiLCJEaXJlY3RpdmVQcm92aWRlci5jb25zdHJ1Y3RvciIsIkRpcmVjdGl2ZVByb3ZpZGVyLmRpc3BsYXlOYW1lIiwiRGlyZWN0aXZlUHJvdmlkZXIuY3JlYXRlRnJvbVR5cGUiLCJRdWVyeU1ldGFkYXRhV2l0aFNldHRlciIsIlF1ZXJ5TWV0YWRhdGFXaXRoU2V0dGVyLmNvbnN0cnVjdG9yIiwic2V0UHJvdmlkZXJzVmlzaWJpbGl0eSIsIkFwcFByb3RvRWxlbWVudCIsIkFwcFByb3RvRWxlbWVudC5jb25zdHJ1Y3RvciIsIkFwcFByb3RvRWxlbWVudC5jcmVhdGUiLCJBcHBQcm90b0VsZW1lbnQuZ2V0UHJvdmlkZXJBdEluZGV4IiwiX0NvbnRleHQiLCJfQ29udGV4dC5jb25zdHJ1Y3RvciIsIkluamVjdG9yV2l0aEhvc3RCb3VuZGFyeSIsIkluamVjdG9yV2l0aEhvc3RCb3VuZGFyeS5jb25zdHJ1Y3RvciIsIkFwcEVsZW1lbnQiLCJBcHBFbGVtZW50LmNvbnN0cnVjdG9yIiwiQXBwRWxlbWVudC5nZXRWaWV3UGFyZW50SW5qZWN0b3IiLCJBcHBFbGVtZW50LmF0dGFjaENvbXBvbmVudFZpZXciLCJBcHBFbGVtZW50Ll9kZWJ1Z0NvbnRleHQiLCJBcHBFbGVtZW50Lmhhc1ZhcmlhYmxlQmluZGluZyIsIkFwcEVsZW1lbnQuZ2V0VmFyaWFibGVCaW5kaW5nIiwiQXBwRWxlbWVudC5nZXQiLCJBcHBFbGVtZW50Lmhhc0RpcmVjdGl2ZSIsIkFwcEVsZW1lbnQuZ2V0Q29tcG9uZW50IiwiQXBwRWxlbWVudC5nZXRJbmplY3RvciIsIkFwcEVsZW1lbnQuZ2V0RWxlbWVudFJlZiIsIkFwcEVsZW1lbnQuZ2V0Vmlld0NvbnRhaW5lclJlZiIsIkFwcEVsZW1lbnQuZ2V0VGVtcGxhdGVSZWYiLCJBcHBFbGVtZW50LmdldERlcGVuZGVuY3kiLCJBcHBFbGVtZW50Ll9idWlsZEF0dHJpYnV0ZSIsIkFwcEVsZW1lbnQuYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkiLCJBcHBFbGVtZW50Ll9idWlsZFF1ZXJ5U3RyYXRlZ3kiLCJBcHBFbGVtZW50LmdldERpcmVjdGl2ZUF0SW5kZXgiLCJBcHBFbGVtZW50Lm5nQWZ0ZXJWaWV3Q2hlY2tlZCIsIkFwcEVsZW1lbnQubmdBZnRlckNvbnRlbnRDaGVja2VkIiwiQXBwRWxlbWVudC50cmF2ZXJzZUFuZFNldFF1ZXJpZXNBc0RpcnR5IiwiQXBwRWxlbWVudC5fc2V0UXVlcmllc0FzRGlydHkiLCJfRW1wdHlRdWVyeVN0cmF0ZWd5IiwiX0VtcHR5UXVlcnlTdHJhdGVneS5zZXRDb250ZW50UXVlcmllc0FzRGlydHkiLCJfRW1wdHlRdWVyeVN0cmF0ZWd5LnNldFZpZXdRdWVyaWVzQXNEaXJ0eSIsIl9FbXB0eVF1ZXJ5U3RyYXRlZ3kudXBkYXRlQ29udGVudFF1ZXJpZXMiLCJfRW1wdHlRdWVyeVN0cmF0ZWd5LnVwZGF0ZVZpZXdRdWVyaWVzIiwiX0VtcHR5UXVlcnlTdHJhdGVneS5maW5kUXVlcnkiLCJJbmxpbmVRdWVyeVN0cmF0ZWd5IiwiSW5saW5lUXVlcnlTdHJhdGVneS5jb25zdHJ1Y3RvciIsIklubGluZVF1ZXJ5U3RyYXRlZ3kuc2V0Q29udGVudFF1ZXJpZXNBc0RpcnR5IiwiSW5saW5lUXVlcnlTdHJhdGVneS5zZXRWaWV3UXVlcmllc0FzRGlydHkiLCJJbmxpbmVRdWVyeVN0cmF0ZWd5LnVwZGF0ZUNvbnRlbnRRdWVyaWVzIiwiSW5saW5lUXVlcnlTdHJhdGVneS51cGRhdGVWaWV3UXVlcmllcyIsIklubGluZVF1ZXJ5U3RyYXRlZ3kuZmluZFF1ZXJ5IiwiRHluYW1pY1F1ZXJ5U3RyYXRlZ3kiLCJEeW5hbWljUXVlcnlTdHJhdGVneS5jb25zdHJ1Y3RvciIsIkR5bmFtaWNRdWVyeVN0cmF0ZWd5LnNldENvbnRlbnRRdWVyaWVzQXNEaXJ0eSIsIkR5bmFtaWNRdWVyeVN0cmF0ZWd5LnNldFZpZXdRdWVyaWVzQXNEaXJ0eSIsIkR5bmFtaWNRdWVyeVN0cmF0ZWd5LnVwZGF0ZUNvbnRlbnRRdWVyaWVzIiwiRHluYW1pY1F1ZXJ5U3RyYXRlZ3kudXBkYXRlVmlld1F1ZXJpZXMiLCJEeW5hbWljUXVlcnlTdHJhdGVneS5maW5kUXVlcnkiLCJFbGVtZW50RGlyZWN0aXZlSW5saW5lU3RyYXRlZ3kiLCJFbGVtZW50RGlyZWN0aXZlSW5saW5lU3RyYXRlZ3kuY29uc3RydWN0b3IiLCJFbGVtZW50RGlyZWN0aXZlSW5saW5lU3RyYXRlZ3kuaW5pdCIsIkVsZW1lbnREaXJlY3RpdmVJbmxpbmVTdHJhdGVneS5nZXRDb21wb25lbnQiLCJFbGVtZW50RGlyZWN0aXZlSW5saW5lU3RyYXRlZ3kuaXNDb21wb25lbnRLZXkiLCJFbGVtZW50RGlyZWN0aXZlSW5saW5lU3RyYXRlZ3kuYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkiLCJFbGVtZW50RGlyZWN0aXZlRHluYW1pY1N0cmF0ZWd5IiwiRWxlbWVudERpcmVjdGl2ZUR5bmFtaWNTdHJhdGVneS5jb25zdHJ1Y3RvciIsIkVsZW1lbnREaXJlY3RpdmVEeW5hbWljU3RyYXRlZ3kuaW5pdCIsIkVsZW1lbnREaXJlY3RpdmVEeW5hbWljU3RyYXRlZ3kuZ2V0Q29tcG9uZW50IiwiRWxlbWVudERpcmVjdGl2ZUR5bmFtaWNTdHJhdGVneS5pc0NvbXBvbmVudEtleSIsIkVsZW1lbnREaXJlY3RpdmVEeW5hbWljU3RyYXRlZ3kuYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkiLCJQcm90b1F1ZXJ5UmVmIiwiUHJvdG9RdWVyeVJlZi5jb25zdHJ1Y3RvciIsIlByb3RvUXVlcnlSZWYudXNlc1Byb3BlcnR5U3ludGF4IiwiUXVlcnlSZWYiLCJRdWVyeVJlZi5jb25zdHJ1Y3RvciIsIlF1ZXJ5UmVmLmlzVmlld1F1ZXJ5IiwiUXVlcnlSZWYudXBkYXRlIiwiUXVlcnlSZWYuX3VwZGF0ZSIsIlF1ZXJ5UmVmLl92aXNpdCIsIlF1ZXJ5UmVmLl92aXNpdEluamVjdG9yIiwiUXVlcnlSZWYuX3Zpc2l0Vmlld0NvbnRhaW5lclZpZXdzIiwiUXVlcnlSZWYuX3Zpc2l0VmlldyIsIlF1ZXJ5UmVmLl9hZ2dyZWdhdGVWYXJpYWJsZUJpbmRpbmciLCJRdWVyeVJlZi5fYWdncmVnYXRlRGlyZWN0aXZlIiwiX0NvbXBvbmVudFZpZXdDaGFuZ2VEZXRlY3RvclJlZiIsIl9Db21wb25lbnRWaWV3Q2hhbmdlRGV0ZWN0b3JSZWYuY29uc3RydWN0b3IiLCJfQ29tcG9uZW50Vmlld0NoYW5nZURldGVjdG9yUmVmLm1hcmtGb3JDaGVjayIsIl9Db21wb25lbnRWaWV3Q2hhbmdlRGV0ZWN0b3JSZWYuZGV0YWNoIiwiX0NvbXBvbmVudFZpZXdDaGFuZ2VEZXRlY3RvclJlZi5kZXRlY3RDaGFuZ2VzIiwiX0NvbXBvbmVudFZpZXdDaGFuZ2VEZXRlY3RvclJlZi5jaGVja05vQ2hhbmdlcyIsIl9Db21wb25lbnRWaWV3Q2hhbmdlRGV0ZWN0b3JSZWYucmVhdHRhY2giXSwibWFwcGluZ3MiOiJPQUFPLEVBQ0wsU0FBUyxFQUNULE9BQU8sRUFLUixNQUFNLDBCQUEwQjtPQUMxQixFQUFDLGFBQWEsRUFBQyxNQUFNLGdDQUFnQztPQUNyRCxFQUFDLFdBQVcsRUFBYyxnQkFBZ0IsRUFBQyxNQUFNLGdDQUFnQztPQUNqRixFQUNMLFFBQVEsRUFDUixHQUFHLEVBQ0gsVUFBVSxFQUVWLFFBQVEsRUFFUixlQUFlLEVB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TUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsYUFBYUEsRUFBRUEsQ0FBQ0E7WUFDOUJBLENBQUNBO1lBRURBLEVBQUVBLENBQUNBLENBQUNBLE1BQU1BLENBQUNBLEdBQUdBLENBQUNBLEVBQUVBLEtBQUtBLFVBQVVBLENBQUNBLFFBQVFBLEVBQUVBLENBQUNBLGVBQWVBLENBQUNBLENBQUNBLENBQUNBO2dCQUM1REEsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsbUJBQW1CQSxFQUFFQSxDQUFDQTtZQUNwQ0EsQ0FBQ0E7WUFFREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsRUFBRUEsS0FBS0EsVUFBVUEsQ0FBQ0EsUUFBUUEsRUFBRUEsQ0FBQ0EsYUFBYUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQzFEQSxJQUFJQSxFQUFFQSxHQUFHQSxJQUFJQSxDQUFDQSxjQUFjQSxFQUFFQSxDQUFDQTtnQkFDL0JBLEVBQUVBLENBQUNBLENBQUNBLE9BQU9BLENBQUNBLEVBQUVBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFFBQVFBLENBQUNBLENBQUNBLENBQUNBO29CQUNwQ0EsTUFBTUEsSUFBSUEsZUFBZUEsQ0FBQ0EsSUFBSUEsRUFBRUEsTUFBTUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsQ0FBQ0E7Z0JBQzlDQSxDQUFDQTtnQkFDREEsTUFBTUEsQ0FBQ0EsRUFBRUEsQ0FBQ0E7WUFDWkEsQ0FBQ0E7WUFFREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsRUFBRUEsS0FBS0EsVUFBVUEsQ0FBQ0EsUUFBUUEsRUFBRUEsQ0FBQ0EsVUFBVUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQ3ZEQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxVQUFVQSxDQUFDQSxRQUFRQSxDQUFDQTtZQUNsQ0EsQ0FBQ0E7UUFFSEEsQ0FBQ0E7UUFBQ0EsSUFBSUEsQ0FBQ0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsUUFBUUEsWUFBWUEsWUFBWUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFD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TUEsR0FBR0EsSUFBSUEsUUFBUUEsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsRUFBRUEsRUFBRUEsQ0FBQ0EsQ0FBQ0E7UUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLE1BQU1BLEdBQUdBLENBQUNBLENBQUNBO1lBQUNBLElBQUlBLENBQUNBLE1BQU1BLEdBQUdBLElBQUlBLFFBQVFBLENBQUNBLFNBQVNBLENBQUNBLENBQUNBLENBQUNBLEVBQUVBLEVBQUVBLENBQUNBLENBQUNBO0lBQ3pFQSxDQUFDQTtJQUVERCx3QkFBd0JBO1FBQ3RCRSxFQUFFQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxXQUFXQSxDQUFDQTtZQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxLQUFLQSxHQUFHQSxJQUFJQSxDQUFDQTtRQUNqRkEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsV0FBV0EsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsS0FBS0EsR0FBR0EsSUFBSUEsQ0FBQ0E7UUFDakZBLEVBQUVBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBO1lBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLEtBQUtBLEdBQUdBLElBQUlBLENBQUNBO0lBQ25GQSxDQUFDQTtJQUVERixxQkFBcUJBO1FBQ25CRyxFQUFFQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxXQUFXQSxDQUFDQTtZQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxLQUFLQSxHQUFHQSxJQUFJQSxDQUFDQTtRQUNoRkEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsV0FBV0EsQ0FBQ0E7WUFBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsS0FBS0EsR0FBR0EsSUFBSUEsQ0FBQ0E7UUFDaEZBLEVBQUVBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBO1lBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLEtBQUtBLEdBQUdBLElBQUlBLENBQUNBO0lBQ2xGQSxDQUFDQTtJQUVESCxvQkFBb0JBO1FBQ2xCSSxFQUFFQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN2REEsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsTUFBTUEsRUFBRUEsQ0FBQ0E7UUFDdkJBLENBQUNBO1FBQ0RBLEVBQUVBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO1lBQ3ZEQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQTtRQUN2QkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkRBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBO1FBQ3ZCQSxDQUFDQTtJQUNIQSxDQUFDQTtJQUVESixpQkFBaUJBO1FBQ2ZLLEVBQUVBLENBQUNBLENBQUNBLFNBQVNBLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLElBQUlBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLFdBQVdBLENBQUNBLENBQUNBLENBQUNBO1lBQ3REQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxFQUFFQSxDQUFDQTtRQUN2QkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsV0FBV0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdERBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBO1FBQ3ZCQSxDQUFDQTtRQUNEQSxFQUFFQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUN0REEsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsTUFBTUEsRUFBRUEsQ0FBQ0E7UUFDdkJBLENBQUNBO0lBQ0hBLENBQUNBO0lBRURMLFNBQVNBLENBQUNBLEtBQW9CQTtRQUM1Qk0sRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsSUFBSUEsSUFBSUEsQ0FBQ0EsTUFBTUEsQ0FBQ0EsYUFBYUEsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDeEVBLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLE1BQU1BLENBQUNBO1FBQ3JCQSxDQUFDQTtRQUNEQSxFQUFFQSxDQUFDQSxDQUFDQSxTQUFTQSxDQUFDQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxJQUFJQSxDQUFDQSxNQUFNQSxDQUFDQSxhQUFh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QkEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtRQUM3REEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsWUFBWUEsaUJBQWlCQSxJQUFJQSxTQUFTQSxDQUFDQSxDQUFDQSxDQUFDQSxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxJQUFJQSxLQUFLQSxTQUFTQSxDQUFDQTtZQUMxRkEsQ0FBQ0EsQ0FBQ0EsSUFBSUEsR0FBR0EsQ0FBQ0EsQ0FBQ0EsbUJBQW1CQSxDQUFDQSxDQUFDQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQSxDQUFDQSxXQUFXQSxDQUFDQSxDQUFDQTtJQUMvREEsQ0FBQ0E7SUFFREYsWUFBWUEsS0FBVUcsTUFBTUEsQ0FBQ0EsSUFBSUEsQ0FBQ0EsZ0JBQWdCQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUUxREgsY0FBY0EsQ0FBQ0EsR0FBUUE7UUFDckJJLE1BQU1BLENBQUNBLElBQUlBLENBQUNBLEdBQUdBLENBQUNBLEtBQUtBLENBQUNBLHdCQUF3QkEsSUFBSUEsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0E7WUFDekRBLEdBQUdBLENBQUNBLEVBQUVBLEtBQUtBLElBQUlBLENBQUNBLGdCQUFnQkEsQ0FBQ0EsYUFBYUEsQ0FBQ0EsTUFBTUEsQ0FBQ0E7SUFDL0RBLENBQUNBO0lBRURKLDBCQUEwQkEsQ0FBQ0EsS0FBb0JBLEVBQUVBLElBQVdBO1FBQzFESyxJQUFJQSxDQUFDQSxHQUFHQSxJQUFJQSxDQUFDQSxnQkFBZ0JBLENBQUNBO1FBQzlCQSxJQUFJQSxDQUFDQSxHQUFHQSxDQUFDQSxDQUFDQSxhQUFhQSxDQUFDQTtRQUN4QkEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLFNBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLFNBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLFNBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLFNBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLFNBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLFNBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLFNBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBLENBQUNBLElBQUlBLEtBQUtBLFNBQVNBLENBQUNBO2dCQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxHQUFHQSxDQUFDQSxDQUFDQSxtQkFBbUJBLENBQUNBLENBQUNBLENBQUNBLFNBQVNBLEVBQUVBLENBQUNBLENBQUNBLFdBQVdBLENBQUNBLENBQUNBO1lBQ3JGQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQSxDQUFDQSxJQUFJQSxDQUFDQSxDQUFDQTtRQUNwQkEsQ0FBQ0E7UUFDREEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0EsU0FBU0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsS0FBS0EsS0FBS0EsQ0FBQ0EsUUFBUUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7WUFDdkVBLEVBQUVBLENBQUNBLENB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QUNIVixDQUFDQTtBQUVELDhDQUE4QyxpQkFBaUI7SUFDN0RXLFlBQW9CQSxXQUF1QkE7UUFBSUMsT0FBT0EsQ0FBQ0E7UUFBbkNBLGdCQUFXQSxHQUFYQSxXQUFXQSxDQUFZQTtJQUFhQSxDQUFDQTtJQUV6REQsWUFBWUEsS0FBV0UsSUFBSUEsQ0FBQ0EsV0FBV0EsQ0FBQ0EsYUFBYUEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsWUFBWUEsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7SUFDMUZGLE1BQU1BLEtBQVdHLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLENBQUNBLGNBQWNBLENBQUNBLEdBQUdBLENBQUNBLE1BQU1BLEVBQUVBLENBQUNBLENBQUNBLENBQUNBO0lBQzlFSCxhQUFhQSxLQUFXSSxJQUFJQSxDQUFDQSxXQUFXQSxDQUFDQSxhQUFhQSxDQUFDQSxjQUFjQSxDQUFDQSxHQUFHQSxDQUFDQSxhQUFhQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUM1RkosY0FBY0EsS0FBV0ssSUFBSUEsQ0FBQ0EsV0FBV0EsQ0FBQ0EsYUFBYUEsQ0FBQ0EsY0FBY0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsY0FBY0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7SUFDOUZMLFFBQVFBLEtBQVdNLElBQUlBLENBQUNBLFdBQVdBLENBQUNBLGFBQWFBLENBQUNBLGNBQWNBLENBQUNBLEdBQUdBLENBQUNBLFFBQVFBLEVBQUVBLENBQUNBLENBQUNBLENBQUNBO0FBQ3BGTixDQUFDQTtBQUFBIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHtcbiAgaXNQcmVzZW50LFxuICBpc0JsYW5rLFxuICBUeXBlLFxuICBzdHJpbmdpZnksXG4gIENPTlNUX0VYUFIsXG4gIFN0cmluZ1dyYXBwZXJcbn0gZnJvbSAnYW5ndWxhcjIvc3JjL2ZhY2FkZS9sYW5nJztcbmltcG9ydCB7QmFzZUV4Y2VwdGlvbn0gZnJvbSAnYW5ndWxhcjIvc3JjL2ZhY2FkZS9leGNlcHRpb25zJztcbmltcG9ydCB7TGlzdFdyYXBwZXIsIE1hcFdyYXBwZXIsIFN0cmluZ01hcFdyYXBwZXJ9IGZyb20gJ2FuZ3VsYXIyL3NyYy9mYWNhZGUvY29sbGVjdGlvbic7XG5pbXBvcnQge1xuICBJbmplY3RvcixcbiAgS2V5LFxuICBEZXBlbmRlbmN5LFxuICBwcm92aWRlLFxuICBQcm92aWRlcixcbiAgUmVzb2x2ZWRQcm92aWRlcixcbiAgTm9Qcm92aWRlckVycm9yLFxuICBBYnN0cmFjdFByb3ZpZGVyRXJyb3IsXG4gIEN5Y2xpY0RlcGVuZGVuY3lFcnJvcixcbiAgcmVzb2x2ZUZvcndhcmRSZWYsXG4gIEluamVjdGFibGVcbn0gZnJvbSAnYW5ndWxhcjIvc3JjL2NvcmUvZGknO1xuaW1wb3J0IHttZXJnZVJlc29sdmVkUHJvdmlkZXJzfSBmcm9tICdhbmd1bGFyMi9zcmMvY29yZS9kaS9wcm92aWRlcic7XG5pbXBvcnQge1xuICBVTkRFRklORUQsXG4gIFByb3RvSW5qZWN0b3IsXG4gIFZpc2liaWxpdHksXG4gIEluamVjdG9ySW5saW5lU3RyYXRlZ3ksXG4gIEluamVjdG9yRHluYW1pY1N0cmF0ZWd5LFxuICBQcm92aWRlcldpdGhWaXNpYmlsaXR5LFxuICBEZXBlbmRlbmN5UHJvdmlkZXJcbn0gZnJvbSAnYW5ndWxhcjIvc3JjL2NvcmUvZGkvaW5qZWN0b3InO1xuaW1wb3J0IHtyZXNvbHZlUHJvdmlkZXIsIFJlc29sdmVkRmFjdG9yeSwgUmVzb2x2ZWRQcm92aWRlcl99IGZyb20gJ2FuZ3VsYXIyL3NyYy9jb3JlL2RpL3Byb3ZpZGVyJztcblxuaW1wb3J0IHtBdHRyaWJ1dGVNZXRhZGF0YSwgUXVlcnlNZXRhZGF0YX0gZnJvbSAnLi4vbWV0YWRhdGEvZGknO1xuXG5pbXBvcnQge0FwcFZpZXd9IGZyb20gJy4vdmlldyc7XG5pbXBvcnQge1ZpZXdUeXBlfSBmcm9tICcuL3ZpZXdfdHlwZSc7XG5pbXBvcnQge0VsZW1lbnRSZWZffSBmcm9tICcuL2VsZW1lbnRfcmVmJztcblxuaW1wb3J0IHtWaWV3Q29udGFpbmVyUmVmfSBmcm9tICcuL3ZpZXdfY29udGFpbmVyX3JlZic7XG5pbXBvcnQge0VsZW1lbnRSZWZ9IGZyb20gJy4vZWxlbWVudF9yZWYnO1xuaW1wb3J0IHtSZW5kZXJlcn0gZnJvbSAnYW5ndWxhcjIvc3JjL2NvcmUvcmVuZGVyL2FwaSc7XG5pbXBvcnQge1RlbXBsYXRlUmVmLCBUZW1wbGF0ZVJlZl99IGZyb20gJy4vdGVtcGxhdGVfcmVmJztcbmltcG9ydCB7RGlyZWN0aXZlTWV0YWRhdGEsIENvbXBvbmVudE1ldGFkYXRhfSBmcm9tICcuLi9tZXRhZGF0YS9kaXJlY3RpdmVzJztcbmltcG9ydCB7XG4gIENoYW5nZURldGVjdG9yLFxuICBDaGFuZ2VEZXRlY3RvclJlZlxufSBmcm9tICdhbmd1bGFyMi9zcmMvY29yZS9jaGFuZ2VfZGV0ZWN0aW9uL2NoYW5nZV9kZXRlY3Rpb24nO1xuaW1wb3J0IHtRdWVyeUxpc3R9IGZyb20gJy4vcXVlcnlfbGlzdCc7XG5pbXBvcnQge3JlZmxlY3Rvcn0gZnJvbSAnYW5ndWxhcjIvc3JjL2NvcmUvcmVmbGVjdGlvbi9yZWZsZWN0aW9uJztcbmltcG9ydCB7U2V0dGVyRm59IGZyb20gJ2FuZ3VsYXIyL3NyYy9jb3JlL3JlZmxlY3Rpb24vdHlwZXMnO1xuaW1wb3J0IHtBZnRlclZpZXdDaGVja2VkfSBmcm9tICdhbmd1bGFyMi9zcmMvY29yZS9saW5rZXIvaW50ZXJmYWNlcyc7XG5pbXBvcnQge1BpcGVQcm92aWRlcn0gZnJvbSAnYW5ndWxhcjIvc3JjL2NvcmUvcGlwZXMvcGlwZV9wcm92aWRlcic7XG5cbmltcG9ydCB7Vmlld0NvbnRhaW5lclJlZl99IGZyb20gXCIuL3ZpZXdfY29udGFpbmVyX3JlZlwiO1xuaW1wb3J0IHtSZXNvbHZlZE1ldGFkYXRhQ2FjaGV9IGZyb20gJy4vcmVzb2x2ZWRfbWV0YWRhdGFfY2FjaGUnO1xuXG52YXIgX3N0YXRpY0tleXM7XG5cbmV4cG9ydCBjbGFzcyBTdGF0aWNLZXlzIHtcbiAgdGVtcGxhdGVSZWZJZDogbnVtYmVyO1xuICB2aWV3Q29udGFpbmVySWQ6IG51bWJlcjtcbiAgY2hhbmdlRGV0ZWN0b3JSZWZJZDogbnVtYmVyO1xuICBlbGVtZW50UmVmSWQ6IG51bWJlcjtcbiAgcmVuZGVyZXJJZDogbnVtYmVyO1xuXG4gIGNvbnN0cnVjdG9yKCkge1xuICAgIHRoaXMudGVtcGxhdGVSZWZJZCA9IEtleS5nZXQoVGVtcGxhdGVSZWYpLmlkO1xuICAgIHRoaXMudmlld0NvbnRhaW5lcklkID0gS2V5LmdldChWaWV3Q29udGFpbmVyUmVmKS5pZDtcbiAgICB0aGlzLmNoYW5nZURldGVjdG9yUmVmSWQgPSBLZXkuZ2V0KENoYW5nZURldGVjdG9yUmVmKS5pZDtcbiAgICB0aGlzLmVsZW1lbnRSZWZJZCA9IEtleS5nZXQoRWxlbWVudFJlZikuaWQ7XG4gICAgdGhpcy5yZW5kZXJlcklkID0gS2V5LmdldChSZW5kZXJlcikuaWQ7XG4gIH1cblxuICBzdGF0aWMgaW5zdGFuY2UoKTogU3RhdGljS2V5cyB7XG4gICAgaWYgKGlzQmxhbmsoX3N0YXRpY0tleXMpKSBfc3RhdGljS2V5cyA9IG5ldyBTdGF0aWNLZXlzKCk7XG4gICAgcmV0dXJuIF9zdGF0aWNLZXlzO1xuICB9XG59XG5cbmV4cG9ydCBjbGFzcyBEaXJlY3RpdmVEZXBlbmRlbmN5IGV4dGVuZHMgRGVwZW5kZW5jeSB7XG4gIGNvbnN0cnVjdG9yKGtleTogS2V5LCBvcHRpb25hbDogYm9vbGVhbiwgbG93ZXJCb3VuZFZpc2liaWxpdHk6IE9iamVjdCxcbiAgICAgICAgICAgICAgdXBwZXJCb3VuZFZpc2liaWxpdHk6IE9iamVjdCwgcHJvcGVydGllczogYW55W10sIHB1YmxpYyBhdHRyaWJ1dGVOYW1lOiBzdHJpbmcsXG4gICAgICAgICAgICAgIHB1YmxpYyBxdWVyeURlY29yYXRvcjogUXVlcnlNZXRhZGF0YSkge1xuICAgIHN1cGVyKGtleSwgb3B0aW9uYWwsIGxvd2VyQm91bmRWaXNpYmlsaXR5LCB1cHBlckJvdW5kVmlzaWJpbGl0eSwgcHJvcGVydGllcyk7XG4gICAgdGhpcy5fdmVyaWZ5KCk7XG4gIH1cblxuICAvKiogQGludGVybmFsICovXG4gIF92ZXJpZnkoKTogdm9pZCB7XG4gICAgdmFyIGNvdW50ID0gMDtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnlEZWNvcmF0b3IpKSBjb3VudCsrO1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5hdHRyaWJ1dGVOYW1lKSkgY291bnQrKztcbiAgICBpZiAoY291bnQgPiAxKVxuICAgICAgdGhyb3cgbmV3IEJhc2VFeGNlcHRpb24oXG4gICAgICAgICAgJ0EgZGlyZWN0aXZlIGluamVjdGFibGUgY2FuIGNvbnRhaW4gb25seSBvbmUgb2YgdGhlIGZvbGxvd2luZyBAQXR0cmlidXRlIG9yIEBRdWVyeS4nKTtcbiAgfVxuXG4gIHN0YXRpYyBjcmVhdGVGcm9tKGQ6IERlcGVuZGVuY3kpOiBEaXJlY3RpdmVEZXBlbmRlbmN5IHtcbiAgICByZXR1cm4gbmV3IERpcmVjdGl2ZURlcGVuZGVuY3koXG4gICAgICAgIGQua2V5LCBkLm9wdGlvbmFsLCBkLmxvd2VyQm91bmRWaXNpYmlsaXR5LCBkLnVwcGVyQm91bmRWaXNpYmlsaXR5LCBkLnByb3BlcnRpZXMsXG4gICAgICAgIERpcmVjdGl2ZURlcGVuZGVuY3kuX2F0dHJpYnV0ZU5hbWUoZC5wcm9wZXJ0aWVzKSwgRGlyZWN0aXZlRGVwZW5kZW5jeS5fcXVlcnkoZC5wcm9wZXJ0aWVzKSk7XG4gIH1cblxuICAvKiogQGludGVybmFsICovXG4gIHN0YXRpYyBfYXR0cmlidXRlTmFtZShwcm9wZXJ0aWVzOiBhbnlbXSk6IHN0cmluZyB7XG4gICAgdmFyIHAgPSA8QXR0cmlidXRlTWV0YWRhdGE+cHJvcGVydGllcy5maW5kKHAgPT4gcCBpbnN0YW5jZW9mIEF0dHJpYnV0ZU1ldGFkYXRhKTtcbiAgICByZXR1cm4gaXNQcmVzZW50KHApID8gcC5hdHRyaWJ1dGVOYW1lIDogbnVsbDtcbiAgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgc3RhdGljIF9xdWVyeShwcm9wZXJ0aWVzOiBhbnlbXSk6IFF1ZXJ5TWV0YWRhdGEge1xuICAgIHJldHVybiA8UXVlcnlNZXRhZGF0YT5wcm9wZXJ0aWVzLmZpbmQocCA9PiBwIGluc3RhbmNlb2YgUXVlcnlNZXRhZGF0YSk7XG4gIH1cbn1cblxuZXhwb3J0IGNsYXNzIERpcmVjdGl2ZVByb3ZpZGVyIGV4dGVuZHMgUmVzb2x2ZWRQcm92aWRlcl8ge1xuICBjb25zdHJ1Y3RvcihrZXk6IEtleSwgZmFjdG9yeTogRnVuY3Rpb24sIGRlcHM6IERlcGVuZGVuY3lbXSwgcHVibGljIGlzQ29tcG9uZW50OiBib29sZWFuLFxuICAgICAgICAgICAgICBwdWJsaWMgcHJvdmlkZXJzOiBSZXNvbHZlZFByb3ZpZGVyW10sIHB1YmxpYyB2aWV3UHJvdmlkZXJzOiBSZXNvbHZlZFByb3ZpZGVyW10sXG4gICAgICAgICAgICAgIHB1YmxpYyBxdWVyaWVzOiBRdWVyeU1ldGFkYXRhV2l0aFNldHRlcltdKSB7XG4gICAgc3VwZXIoa2V5LCBbbmV3IFJlc29sdmVkRmFjdG9yeShmYWN0b3J5LCBkZXBzKV0sIGZhbHNlKTtcbiAgfVxuXG4gIGdldCBkaXNwbGF5TmFtZSgpOiBzdHJpbmcgeyByZXR1cm4gdGhpcy5rZXkuZGlzcGxheU5hbWU7IH1cblxuICBzdGF0aWMgY3JlYXRlRnJvbVR5cGUodHlwZTogVHlwZSwgbWV0YTogRGlyZWN0aXZlTWV0YWRhdGEpOiBEaXJlY3RpdmVQcm92aWRlciB7XG4gICAgdmFyIHByb3ZpZGVyID0gbmV3IFByb3ZpZGVyKHR5cGUsIHt1c2VDbGFzczogdHlwZX0pO1xuICAgIGlmIChpc0JsYW5rKG1ldGEpKSB7XG4gICAgICBtZXRhID0gbmV3IERpcmVjdGl2ZU1ldGFkYXRhKCk7XG4gICAgfVxuICAgIHZhciByYiA9IHJlc29sdmVQcm92aWRlcihwcm92aWRlcik7XG4gICAgdmFyIHJmID0gcmIucmVzb2x2ZWRGYWN0b3JpZXNbMF07XG4gICAgdmFyIGRlcHM6IERpcmVjdGl2ZURlcGVuZGVuY3lbXSA9IHJmLmRlcGVuZGVuY2llcy5tYXAoRGlyZWN0aXZlRGVwZW5kZW5jeS5jcmVhdGVGcm9tKTtcbiAgICB2YXIgaXNDb21wb25lbnQgPSBtZXRhIGluc3RhbmNlb2YgQ29tcG9uZW50TWV0YWRhdGE7XG4gICAgdmFyIHJlc29sdmVkUHJvdmlkZXJzID0gaXNQcmVzZW50KG1ldGEucHJvdmlkZXJzKSA/IEluamVjdG9yLnJlc29sdmUobWV0YS5wcm92aWRlcnMpIDogbnVsbDtcbiAgICB2YXIgcmVzb2x2ZWRWaWV3UHJvdmlkZXJzID0gbWV0YSBpbnN0YW5jZW9mIENvbXBvbmVudE1ldGFkYXRhICYmIGlzUHJlc2VudChtZXRhLnZpZXdQcm92aWRlcnMpID9cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIEluamVjdG9yLnJlc29sdmUobWV0YS52aWV3UHJvdmlkZXJzKSA6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBudWxsO1xuICAgIHZhciBxdWVyaWVzID0gW107XG4gICAgaWYgKGlzUHJlc2VudChtZXRhLnF1ZXJpZXMpKSB7XG4gICAgICBTdHJpbmdNYXBXcmFwcGVyLmZvckVhY2gobWV0YS5xdWVyaWVzLCAobWV0YSwgZmllbGROYW1lKSA9PiB7XG4gICAgICAgIHZhciBzZXR0ZXIgPSByZWZsZWN0b3Iuc2V0dGVyKGZpZWxkTmFtZSk7XG4gICAgICAgIHF1ZXJpZXMucHVzaChuZXcgUXVlcnlNZXRhZGF0YVdpdGhTZXR0ZXIoc2V0dGVyLCBtZXRhKSk7XG4gICAgICB9KTtcbiAgICB9XG4gICAgLy8gcXVlcmllcyBwYXNzZWQgaW50byB0aGUgY29uc3RydWN0b3IuXG4gICAgLy8gVE9ETzogcmVtb3ZlIHRoaXMgYWZ0ZXIgY29uc3RydWN0b3IgcXVlcmllcyBhcmUgbm8gbG9uZ2VyIHN1cHBvcnRlZFxuICAgIGRlcHMuZm9yRWFjaChkID0+IHtcbiAgICAgIGlmIChpc1ByZXNlbnQoZC5xdWVyeURlY29yYXRvcikpIHtcbiAgICAgICAgcXVlcmllcy5wdXNoKG5ldyBRdWVyeU1ldGFkYXRhV2l0aFNldHRlcihudWxsLCBkLnF1ZXJ5RGVjb3JhdG9yKSk7XG4gICAgICB9XG4gICAgfSk7XG4gICAgcmV0dXJuIG5ldyBEaXJlY3RpdmVQcm92aWRlcihyYi5rZXksIHJmLmZhY3RvcnksIGRlcHMsIGlzQ29tcG9uZW50LCByZXNvbHZlZFByb3ZpZGVycyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlc29sdmVkVmlld1Byb3ZpZGVycywgcXVlcmllcyk7XG4gIH1cbn1cblxuZXhwb3J0IGNsYXNzIFF1ZXJ5TWV0YWRhdGFXaXRoU2V0dGVyIHtcbiAgY29uc3RydWN0b3IocHVibGljIHNldHRlcjogU2V0dGVyRm4sIHB1YmxpYyBtZXRhZGF0YTogUXVlcnlNZXRhZGF0YSkge31cbn1cblxuXG5mdW5jdGlvbiBzZXRQcm92aWRlcnNWaXNpYmlsaXR5KHByb3ZpZGVyczogUmVzb2x2ZWRQcm92aWRlcltdLCB2aXNpYmlsaXR5OiBWaXNpYmlsaXR5LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXN1bHQ6IE1hcDxudW1iZXIsIFZpc2liaWxpdHk+KSB7XG4gIGZvciAodmFyIGkgPSAwOyBpIDwgcHJvdmlkZXJzLmxlbmd0aDsgaSsrKSB7XG4gICAgcmVzdWx0LnNldChwcm92aWRlcnNbaV0ua2V5LmlkLCB2aXNpYmlsaXR5KTtcbiAgfVxufVxuXG5leHBvcnQgY2xhc3MgQXBwUHJvdG9FbGVtZW50IHtcbiAgcHJvdG9JbmplY3RvcjogUHJvdG9JbmplY3RvcjtcblxuICBzdGF0aWMgY3JlYXRlKG1ldGFkYXRhQ2FjaGU6IFJlc29sdmVkTWV0YWRhdGFDYWNoZSwgaW5kZXg6IG51bWJlcixcbiAgICAgICAgICAgICAgICBhdHRyaWJ1dGVzOiB7W2tleTogc3RyaW5nXTogc3RyaW5nfSwgZGlyZWN0aXZlVHlwZXM6IFR5cGVbXSxcbiAgICAgICAgICAgICAgICBkaXJlY3RpdmVWYXJpYWJsZUJpbmRpbmdzOiB7W2tleTogc3RyaW5nXTogbnVtYmVyfSk6IEFwcFByb3RvRWxlbWVudCB7XG4gICAgdmFyIGNvbXBvbmVudERpclByb3ZpZGVyID0gbnVsbDtcbiAgICB2YXIgbWVyZ2VkUHJvdmlkZXJzTWFwOiBNYXA8bnVtYmVyLCBSZXNvbHZlZFByb3ZpZGVyPiA9IG5ldyBNYXA8bnVtYmVyLCBSZXNvbHZlZFByb3ZpZGVyPigpO1xuICAgIHZhciBwcm92aWRlclZpc2liaWxpdHlNYXA6IE1hcDxudW1iZXIsIFZpc2liaWxpdHk+ID0gbmV3IE1hcDxudW1iZXIsIFZpc2liaWxpdHk+KCk7XG4gICAgdmFyIHByb3ZpZGVycyA9IExpc3RXcmFwcGVyLmNyZWF0ZUdyb3dhYmxlU2l6ZShkaXJlY3RpdmVUeXBlcy5sZW5ndGgpO1xuXG4gICAgdmFyIHByb3RvUXVlcnlSZWZzID0gW107XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCBkaXJlY3RpdmVUeXBlcy5sZW5ndGg7IGkrKykge1xuICAgICAgdmFyIGRpclByb3ZpZGVyID0gbWV0YWRhdGFDYWNoZS5nZXRSZXNvbHZlZERpcmVjdGl2ZU1ldGFkYXRhKGRpcmVjdGl2ZVR5cGVzW2ldKTtcbiAgICAgIHByb3ZpZGVyc1tpXSA9IG5ldyBQcm92aWRlcldpdGhWaXNpYmlsaXR5KFxuICAgICAgICAgIGRpclByb3ZpZGVyLCBkaXJQcm92aWRlci5pc0NvbXBvbmVudCA/IFZpc2liaWxpdHkuUHVibGljQW5kUHJpdmF0ZSA6IFZpc2liaWxpdHkuUHVibGljKTtcblxuICAgICAgaWYgKGRpclByb3ZpZGVyLmlzQ29tcG9uZW50KSB7XG4gICAgICAgIGNvbXBvbmVudERpclByb3ZpZGVyID0gZGlyUHJvdmlkZXI7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICBpZiAoaXNQcmVzZW50KGRpclByb3ZpZGVyLnByb3ZpZGVycykpIHtcbiAgICAgICAgICBtZXJnZVJlc29sdmVkUHJvdmlkZXJzKGRpclByb3ZpZGVyLnByb3ZpZGVycywgbWVyZ2VkUHJvdmlkZXJzTWFwKTtcbiAgICAgICAgICBzZXRQcm92aWRlcnNWaXNpYmlsaXR5KGRpclByb3ZpZGVyLnByb3ZpZGVycywgVmlzaWJpbGl0eS5QdWJsaWMsIHByb3ZpZGVyVmlzaWJpbGl0eU1hcCk7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICAgIGlmIChpc1ByZXNlbnQoZGlyUHJvdmlkZXIudmlld1Byb3ZpZGVycykpIHtcbiAgICAgICAgbWVyZ2VSZXNvbHZlZFByb3ZpZGVycyhkaXJQcm92aWRlci52aWV3UHJvdmlkZXJzLCBtZXJnZWRQcm92aWRlcnNNYXApO1xuICAgICAgICBzZXRQcm92aWRlcnNWaXNpYmlsaXR5KGRpclByb3ZpZGVyLnZpZXdQcm92aWRlcnMsIFZpc2liaWxpdHkuUHJpdmF0ZSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwcm92aWRlclZpc2liaWxpdHlNYXApO1xuICAgICAgfVxuICAgICAgZm9yICh2YXIgcXVlcnlJZHggPSAwOyBxdWVyeUlkeCA8IGRpclByb3ZpZGVyLnF1ZXJpZXMubGVuZ3RoOyBxdWVyeUlkeCsrKSB7XG4gICAgICAgIHZhciBxID0gZGlyUHJvdmlkZXIucXVlcmllc1txdWVyeUlkeF07XG4gICAgICAgIHByb3RvUXVlcnlSZWZzLnB1c2gobmV3IFByb3RvUXVlcnlSZWYoaSwgcS5zZXR0ZXIsIHEubWV0YWRhdGEpKTtcbiAgICAgIH1cbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudChjb21wb25lbnREaXJQcm92aWRlcikgJiYgaXNQcmVzZW50KGNvbXBvbmVudERpclByb3ZpZGVyLnByb3ZpZGVycykpIHtcbiAgICAgIC8vIGRpcmVjdGl2ZSBwcm92aWRlcnMgbmVlZCB0byBiZSBwcmlvcml0aXplZCBvdmVyIGNvbXBvbmVudCBwcm92aWRlcnNcbiAgICAgIG1lcmdlUmVzb2x2ZWRQcm92aWRlcnMoY29tcG9uZW50RGlyUHJvdmlkZXIucHJvdmlkZXJzLCBtZXJnZWRQcm92aWRlcnNNYXApO1xuICAgICAgc2V0UHJvdmlkZXJzVmlzaWJpbGl0eShjb21wb25lbnREaXJQcm92aWRlci5wcm92aWRlcnMsIFZpc2liaWxpdHkuUHVibGljLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwcm92aWRlclZpc2liaWxpdHlNYXApO1xuICAgIH1cbiAgICBtZXJnZWRQcm92aWRlcnNNYXAuZm9yRWFjaCgocHJvdmlkZXIsIF8pID0+IHtcbiAgICAgIHByb3ZpZGVycy5wdXNoKFxuICAgICAgICAgIG5ldyBQcm92aWRlcldpdGhWaXNpYmlsaXR5KHByb3ZpZGVyLCBwcm92aWRlclZpc2liaWxpdHlNYXAuZ2V0KHByb3ZpZGVyLmtleS5pZCkpKTtcbiAgICB9KTtcblxuICAgIHJldHVybiBuZXcgQXBwUHJvdG9FbGVtZW50KGlzUHJlc2VudChjb21wb25lbnREaXJQcm92aWRlciksIGluZGV4LCBhdHRyaWJ1dGVzLCBwcm92aWRlcnMsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcHJvdG9RdWVyeVJlZnMsIGRpcmVjdGl2ZVZhcmlhYmxlQmluZGluZ3MpO1xuICB9XG5cbiAgY29uc3RydWN0b3IocHVibGljIGZpcnN0UHJvdmlkZXJJc0NvbXBvbmVudDogYm9vbGVhbiwgcHVibGljIGluZGV4OiBudW1iZXIsXG4gICAgICAgICAgICAgIHB1YmxpYyBhdHRyaWJ1dGVzOiB7W2tleTogc3RyaW5nXTogc3RyaW5nfSwgcHd2czogUHJvdmlkZXJXaXRoVmlzaWJpbGl0eVtdLFxuICAgICAgICAgICAgICBwdWJsaWMgcHJvdG9RdWVyeVJlZnM6IFByb3RvUXVlcnlSZWZbXSxcbiAgICAgICAgICAgICAgcHVibGljIGRpcmVjdGl2ZVZhcmlhYmxlQmluZGluZ3M6IHtba2V5OiBzdHJpbmddOiBudW1iZXJ9KSB7XG4gICAgdmFyIGxlbmd0aCA9IHB3dnMubGVuZ3RoO1xuICAgIGlmIChsZW5ndGggPiAwKSB7XG4gICAgICB0aGlzLnByb3RvSW5qZWN0b3IgPSBuZXcgUHJvdG9JbmplY3Rvcihwd3ZzKTtcbiAgICB9IGVsc2Uge1xuICAgICAgdGhpcy5wcm90b0luamVjdG9yID0gbnVsbDtcbiAgICAgIHRoaXMucHJvdG9RdWVyeVJlZnMgPSBbXTtcbiAgICB9XG4gIH1cblxuICBnZXRQcm92aWRlckF0SW5kZXgoaW5kZXg6IG51bWJlcik6IGFueSB7IHJldHVybiB0aGlzLnByb3RvSW5qZWN0b3IuZ2V0UHJvdmlkZXJBdEluZGV4KGluZGV4KTsgfVxufVxuXG5jbGFzcyBfQ29udGV4dCB7XG4gIGNvbnN0cnVjdG9yKHB1YmxpYyBlbGVtZW50OiBhbnksIHB1YmxpYyBjb21wb25lbnRFbGVtZW50OiBhbnksIHB1YmxpYyBpbmplY3RvcjogYW55KSB7fVxufVxuXG5leHBvcnQgY2xhc3MgSW5qZWN0b3JXaXRoSG9zdEJvdW5kYXJ5IHtcbiAgY29uc3RydWN0b3IocHVibGljIGluamVjdG9yOiBJbmplY3RvciwgcHVibGljIGhvc3RJbmplY3RvckJvdW5kYXJ5OiBib29sZWFuKSB7fVxufVxuXG5leHBvcnQgY2xhc3MgQXBwRWxlbWVudCBpbXBsZW1lbnRzIERlcGVuZGVuY3lQcm92aWRlciwgRWxlbWVudFJlZiwgQWZ0ZXJWaWV3Q2hlY2tlZCB7XG4gIHN0YXRpYyBnZXRWaWV3UGFyZW50SW5qZWN0b3IocGFyZW50Vmlld1R5cGU6IFZpZXdUeXBlLCBjb250YWluZXJBcHBFbGVtZW50OiBBcHBFbGVtZW50LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGltcGVyYXRpdmVseUNyZWF0ZWRQcm92aWRlcnM6IFJlc29sdmVkUHJvdmlkZXJbXSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICByb290SW5qZWN0b3I6IEluamVjdG9yKTogSW5qZWN0b3JXaXRoSG9zdEJvdW5kYXJ5IHtcbiAgICB2YXIgcGFyZW50SW5qZWN0b3I7XG4gICAgdmFyIGhvc3RJbmplY3RvckJvdW5kYXJ5O1xuICAgIHN3aXRjaCAocGFyZW50Vmlld1R5cGUpIHtcbiAgICAgIGNhc2UgVmlld1R5cGUuQ09NUE9ORU5UOlxuICAgICAgICBwYXJlbnRJbmplY3RvciA9IGNvbnRhaW5lckFwcEVsZW1lbnQuX2luamVjdG9yO1xuICAgICAgICBob3N0SW5qZWN0b3JCb3VuZGFyeSA9IHRydWU7XG4gICAgICAgIGJyZWFrO1xuICAgICAgY2FzZSBWaWV3VHlwZS5FTUJFRERFRDpcbiAgICAgICAgcGFyZW50SW5qZWN0b3IgPSBpc1ByZXNlbnQoY29udGFpbmVyQXBwRWxlbWVudC5wcm90by5wcm90b0luamVjdG9yKSA/XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lckFwcEVsZW1lbnQuX2luamVjdG9yLnBhcmVudCA6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lckFwcEVsZW1lbnQuX2luamVjdG9yO1xuICAgICAgICBob3N0SW5qZWN0b3JCb3VuZGFyeSA9IGNvbnRhaW5lckFwcEVsZW1lbnQuX2luamVjdG9yLmhvc3RCb3VuZGFyeTtcbiAgICAgICAgYnJlYWs7XG4gICAgICBjYXNlIFZpZXdUeXBlLkhPU1Q6XG4gICAgICAgIGlmIChpc1ByZXNlbnQoY29udGFpbmVyQXBwRWxlbWVudCkpIHtcbiAgICAgICAgICAvLyBob3N0IHZpZXcgaXMgYXR0YWNoZWQgdG8gYSBjb250YWluZXJcbiAgICAgICAgICBwYXJlbnRJbmplY3RvciA9IGlzUHJlc2VudChjb250YWluZXJBcHBFbGVtZW50LnByb3RvLnByb3RvSW5qZWN0b3IpID9cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXJBcHBFbGVtZW50Ll9pbmplY3Rvci5wYXJlbnQgOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lckFwcEVsZW1lbnQuX2luamVjdG9yO1xuICAgICAgICAgIGlmIChpc1ByZXNlbnQoaW1wZXJhdGl2ZWx5Q3JlYXRlZFByb3ZpZGVycykpIHtcbiAgICAgICAgICAgIHZhciBpbXBlcmF0aXZlUHJvdmlkZXJzV2l0aFZpc2liaWxpdHkgPSBpbXBlcmF0aXZlbHlDcmVhdGVkUHJvdmlkZXJzLm1hcChcbiAgICAgICAgICAgICAgICBwID0+IG5ldyBQcm92aWRlcldpdGhWaXNpYmlsaXR5KHAsIFZpc2liaWxpdHkuUHVibGljKSk7XG4gICAgICAgICAgICAvLyBUaGUgaW1wZXJhdGl2ZSBpbmplY3RvciBpcyBzaW1pbGFyIHRvIGhhdmluZyBhbiBlbGVtZW50IGJldHdlZW5cbiAgICAgICAgICAgIC8vIHRoZSBkeW5hbWljLWxvYWRlZCBjb21wb25lbnQgYW5kIGl0cyBwYXJlbnQgPT4gbm8gYm91bmRhcnkgYmV0d2VlblxuICAgICAgICAgICAgLy8gdGhlIGNvbXBvbmVudCBhbmQgaW1wZXJhdGl2ZWx5Q3JlYXRlZEluamVjdG9yLlxuICAgICAgICAgICAgcGFyZW50SW5qZWN0b3IgPSBuZXcgSW5qZWN0b3IobmV3IFByb3RvSW5qZWN0b3IoaW1wZXJhdGl2ZVByb3ZpZGVyc1dpdGhWaXNpYmlsaXR5KSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhcmVudEluamVjdG9yLCB0cnVlLCBudWxsLCBudWxsKTtcbiAgICAgICAgICAgIGhvc3RJbmplY3RvckJvdW5kYXJ5ID0gZmFsc2U7XG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGhvc3RJbmplY3RvckJvdW5kYXJ5ID0gY29udGFpbmVyQXBwRWxlbWVudC5faW5qZWN0b3IuaG9zdEJvdW5kYXJ5O1xuICAgICAgICAgIH1cbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAvLyBib290c3RyYXBcbiAgICAgICAgICBwYXJlbnRJbmplY3RvciA9IHJvb3RJbmplY3RvcjtcbiAgICAgICAgICBob3N0SW5qZWN0b3JCb3VuZGFyeSA9IHRydWU7XG4gICAgICAgIH1cbiAgICAgICAgYnJlYWs7XG4gICAgfVxuICAgIHJldHVybiBuZXcgSW5qZWN0b3JXaXRoSG9zdEJvdW5kYXJ5KHBhcmVudEluamVjdG9yLCBob3N0SW5qZWN0b3JCb3VuZGFyeSk7XG4gIH1cblxuICBwdWJsaWMgbmVzdGVkVmlld3M6IEFwcFZpZXdbXSA9IG51bGw7XG4gIHB1YmxpYyBjb21wb25lbnRWaWV3OiBBcHBWaWV3ID0gbnVsbDtcblxuICBwcml2YXRlIF9xdWVyeVN0cmF0ZWd5OiBfUXVlcnlTdHJhdGVneTtcbiAgcHJpdmF0ZSBfaW5qZWN0b3I6IEluamVjdG9yO1xuICBwcml2YXRlIF9zdHJhdGVneTogX0VsZW1lbnREaXJlY3RpdmVTdHJhdGVneTtcbiAgcHVibGljIHJlZjogRWxlbWVudFJlZl87XG5cbiAgY29uc3RydWN0b3IocHVibGljIHByb3RvOiBBcHBQcm90b0VsZW1lbnQsIHB1YmxpYyBwYXJlbnRWaWV3OiBBcHBWaWV3LCBwdWJsaWMgcGFyZW50OiBBcHBFbGVtZW50LFxuICAgICAgICAgICAgICBwdWJsaWMgbmF0aXZlRWxlbWVudDogYW55LCBwdWJsaWMgZW1iZWRkZWRWaWV3RmFjdG9yeTogRnVuY3Rpb24pIHtcbiAgICB0aGlzLnJlZiA9IG5ldyBFbGVtZW50UmVmXyh0aGlzKTtcbiAgICB2YXIgcGFyZW50SW5qZWN0b3IgPSBpc1ByZXNlbnQocGFyZW50KSA/IHBhcmVudC5faW5qZWN0b3IgOiBwYXJlbnRWaWV3LnBhcmVudEluamVjdG9yO1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5wcm90by5wcm90b0luamVjdG9yKSkge1xuICAgICAgdmFyIGlzQm91bmRhcnk7XG4gICAgICBpZiAoaXNQcmVzZW50KHBhcmVudCkgJiYgaXNQcmVzZW50KHBhcmVudC5wcm90by5wcm90b0luamVjdG9yKSkge1xuICAgICAgICBpc0JvdW5kYXJ5ID0gZmFsc2U7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICBpc0JvdW5kYXJ5ID0gcGFyZW50Vmlldy5ob3N0SW5qZWN0b3JCb3VuZGFyeTtcbiAgICAgIH1cbiAgICAgIHRoaXMuX3F1ZXJ5U3RyYXRlZ3kgPSB0aGlzLl9idWlsZFF1ZXJ5U3RyYXRlZ3koKTtcbiAgICAgIHRoaXMuX2luamVjdG9yID0gbmV3IEluamVjdG9yKHRoaXMucHJvdG8ucHJvdG9JbmplY3RvciwgcGFyZW50SW5qZWN0b3IsIGlzQm91bmRhcnksIHRoaXMsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAoKSA9PiB0aGlzLl9kZWJ1Z0NvbnRleHQoKSk7XG5cbiAgICAgIC8vIHdlIGNvdXBsZSBvdXJzZWx2ZXMgdG8gdGhlIGluamVjdG9yIHN0cmF0ZWd5IHRvIGF2b2lkIHBvbHltb3JwaGljIGNhbGxzXG4gICAgICB2YXIgaW5qZWN0b3JTdHJhdGVneSA9IDxhbnk+dGhpcy5faW5qZWN0b3IuaW50ZXJuYWxTdHJhdGVneTtcbiAgICAgIHRoaXMuX3N0cmF0ZWd5ID0gaW5qZWN0b3JTdHJhdGVneSBpbnN0YW5jZW9mIEluamVjdG9ySW5saW5lU3RyYXRlZ3kgP1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgbmV3IEVsZW1lbnREaXJlY3RpdmVJbmxpbmVTdHJhdGVneShpbmplY3RvclN0cmF0ZWd5LCB0aGlzKSA6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICBuZXcgRWxlbWVudERpcmVjdGl2ZUR5bmFtaWNTdHJhdGVneShpbmplY3RvclN0cmF0ZWd5LCB0aGlzKTtcbiAgICAgIHRoaXMuX3N0cmF0ZWd5LmluaXQoKTtcbiAgICB9IGVsc2Uge1xuICAgICAgdGhpcy5fcXVlcnlTdHJhdGVneSA9IG51bGw7XG4gICAgICB0aGlzLl9pbmplY3RvciA9IHBhcmVudEluamVjdG9yO1xuICAgICAgdGhpcy5fc3RyYXRlZ3kgPSBudWxsO1xuICAgIH1cbiAgfVxuXG4gIGF0dGFjaENvbXBvbmVudFZpZXcoY29tcG9uZW50VmlldzogQXBwVmlldykgeyB0aGlzLmNvbXBvbmVudFZpZXcgPSBjb21wb25lbnRWaWV3OyB9XG5cbiAgcHJpdmF0ZSBfZGVidWdDb250ZXh0KCk6IGFueSB7XG4gICAgdmFyIGMgPSB0aGlzLnBhcmVudFZpZXcuZ2V0RGVidWdDb250ZXh0KHRoaXMsIG51bGwsIG51bGwpO1xuICAgIHJldHVybiBpc1ByZXNlbnQoYykgPyBuZXcgX0NvbnRleHQoYy5lbGVtZW50LCBjLmNvbXBvbmVudEVsZW1lbnQsIGMuaW5qZWN0b3IpIDogbnVsbDtcbiAgfVxuXG4gIGhhc1ZhcmlhYmxlQmluZGluZyhuYW1lOiBzdHJpbmcpOiBib29sZWFuIHtcbiAgICB2YXIgdmIgPSB0aGlzLnByb3RvLmRpcmVjdGl2ZVZhcmlhYmxlQmluZGluZ3M7XG4gICAgcmV0dXJuIGlzUHJlc2VudCh2YikgJiYgU3RyaW5nTWFwV3JhcHBlci5jb250YWlucyh2YiwgbmFtZSk7XG4gIH1cblxuICBnZXRWYXJpYWJsZUJpbmRpbmcobmFtZTogc3RyaW5nKTogYW55IHtcbiAgICB2YXIgaW5kZXggPSB0aGlzLnByb3RvLmRpcmVjdGl2ZVZhcmlhYmxlQmluZGluZ3NbbmFtZV07XG4gICAgcmV0dXJuIGlzUHJlc2VudChpbmRleCkgPyB0aGlzLmdldERpcmVjdGl2ZUF0SW5kZXgoPG51bWJlcj5pbmRleCkgOiB0aGlzLmdldEVsZW1lbnRSZWYoKTtcbiAgfVxuXG4gIGdldCh0b2tlbjogYW55KTogYW55IHsgcmV0dXJuIHRoaXMuX2luamVjdG9yLmdldCh0b2tlbik7IH1cblxuICBoYXNEaXJlY3RpdmUodHlwZTogVHlwZSk6IGJvb2xlYW4geyByZXR1cm4gaXNQcmVzZW50KHRoaXMuX2luamVjdG9yLmdldE9wdGlvbmFsKHR5cGUpKTsgfVxuXG4gIGdldENvbXBvbmVudCgpOiBhbnkgeyByZXR1cm4gaXNQcmVzZW50KHRoaXMuX3N0cmF0ZWd5KSA/IHRoaXMuX3N0cmF0ZWd5LmdldENvbXBvbmVudCgpIDogbnVsbDsgfVxuXG4gIGdldEluamVjdG9yKCk6IEluamVjdG9yIHsgcmV0dXJuIHRoaXMuX2luamVjdG9yOyB9XG5cbiAgZ2V0RWxlbWVudFJlZigpOiBFbGVtZW50UmVmIHsgcmV0dXJuIHRoaXMucmVmOyB9XG5cbiAgZ2V0Vmlld0NvbnRhaW5lclJlZigpOiBWaWV3Q29udGFpbmVyUmVmIHsgcmV0dXJuIG5ldyBWaWV3Q29udGFpbmVyUmVmXyh0aGlzKTsgfVxuXG4gIGdldFRlbXBsYXRlUmVmKCk6IFRlbXBsYXRlUmVmIHtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMuZW1iZWRkZWRWaWV3RmFjdG9yeSkpIHtcbiAgICAgIHJldHVybiBuZXcgVGVtcGxhdGVSZWZfKHRoaXMucmVmKTtcbiAgICB9XG4gICAgcmV0dXJuIG51bGw7XG4gIH1cblxuICBnZXREZXBlbmRlbmN5KGluamVjdG9yOiBJbmplY3RvciwgcHJvdmlkZXI6IFJlc29sdmVkUHJvdmlkZXIsIGRlcDogRGVwZW5kZW5jeSk6IGFueSB7XG4gICAgaWYgKHByb3ZpZGVyIGluc3RhbmNlb2YgRGlyZWN0aXZlUHJvdmlkZXIpIHtcbiAgICAgIHZhciBkaXJEZXAgPSA8RGlyZWN0aXZlRGVwZW5kZW5jeT5kZXA7XG5cbiAgICAgIGlmIChpc1ByZXNlbnQoZGlyRGVwLmF0dHJpYnV0ZU5hbWUpKSByZXR1cm4gdGhpcy5fYnVpbGRBdHRyaWJ1dGUoZGlyRGVwKTtcblxuICAgICAgaWYgKGlzUHJlc2VudChkaXJEZXAucXVlcnlEZWNvcmF0b3IpKVxuICAgICAgICByZXR1cm4gdGhpcy5fcXVlcnlTdHJhdGVneS5maW5kUXVlcnkoZGlyRGVwLnF1ZXJ5RGVjb3JhdG9yKS5saXN0O1xuXG4gICAgICBpZiAoZGlyRGVwLmtleS5pZCA9PT0gU3RhdGljS2V5cy5pbnN0YW5jZSgpLmNoYW5nZURldGVjdG9yUmVmSWQpIHtcbiAgICAgICAgLy8gV2UgcHJvdmlkZSB0aGUgY29tcG9uZW50J3MgdmlldyBjaGFuZ2UgZGV0ZWN0b3IgdG8gY29tcG9uZW50cyBhbmRcbiAgICAgICAgLy8gdGhlIHN1cnJvdW5kaW5nIGNvbXBvbmVudCdzIGNoYW5nZSBkZXRlY3RvciB0byBkaXJlY3RpdmVzLlxuICAgICAgICBpZiAodGhpcy5wcm90by5maXJzdFByb3ZpZGVySXNDb21wb25lbnQpIHtcbiAgICAgICAgICAvLyBOb3RlOiBUaGUgY29tcG9uZW50IHZpZXcgaXMgbm90IHlldCBjcmVhdGVkIHdoZW5cbiAgICAgICAgICAvLyB0aGlzIG1ldGhvZCBpcyBjYWxsZWQhXG4gICAgICAgICAgcmV0dXJuIG5ldyBfQ29tcG9uZW50Vmlld0NoYW5nZURldGVjdG9yUmVmKHRoaXMpO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgIHJldHVybiB0aGlzLnBhcmVudFZpZXcuY2hhbmdlRGV0ZWN0b3IucmVmO1xuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgIGlmIChkaXJEZXAua2V5LmlkID09PSBTdGF0aWNLZXlzLmluc3RhbmNlKCkuZWxlbWVudFJlZklkKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmdldEVsZW1lbnRSZWYoKTtcbiAgICAgIH1cblxuICAgICAgaWYgKGRpckRlcC5rZXkuaWQgPT09IFN0YXRpY0tleXMuaW5zdGFuY2UoKS52aWV3Q29udGFpbmVySWQpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuZ2V0Vmlld0NvbnRhaW5lclJlZigpO1xuICAgICAgfVxuXG4gICAgICBpZiAoZGlyRGVwLmtleS5pZCA9PT0gU3RhdGljS2V5cy5pbnN0YW5jZSgpLnRlbXBsYXRlUmVmSWQpIHtcbiAgICAgICAgdmFyIHRyID0gdGhpcy5nZXRUZW1wbGF0ZVJlZigpO1xuICAgICAgICBpZiAoaXNCbGFuayh0cikgJiYgIWRpckRlcC5vcHRpb25hbCkge1xuICAgICAgICAgIHRocm93IG5ldyBOb1Byb3ZpZGVyRXJyb3IobnVsbCwgZGlyRGVwLmtleSk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHRyO1xuICAgICAgfVxuXG4gICAgICBpZiAoZGlyRGVwLmtleS5pZCA9PT0gU3RhdGljS2V5cy5pbnN0YW5jZSgpLnJlbmRlcmVySWQpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMucGFyZW50Vmlldy5yZW5kZXJlcjtcbiAgICAgIH1cblxuICAgIH0gZWxzZSBpZiAocHJvdmlkZXIgaW5zdGFuY2VvZiBQaXBlUHJvdmlkZXIpIHtcbiAgICAgIGlmIChkZXAua2V5LmlkID09PSBTdGF0aWNLZXlzLmluc3RhbmNlKCkuY2hhbmdlRGV0ZWN0b3JSZWZJZCkge1xuICAgICAgICAvLyBXZSBwcm92aWRlIHRoZSBjb21wb25lbnQncyB2aWV3IGNoYW5nZSBkZXRlY3RvciB0byBjb21wb25lbnRzIGFuZFxuICAgICAgICAvLyB0aGUgc3Vycm91bmRpbmcgY29tcG9uZW50J3MgY2hhbmdlIGRldGVjdG9yIHRvIGRpcmVjdGl2ZXMuXG4gICAgICAgIGlmICh0aGlzLnByb3RvLmZpcnN0UHJvdmlkZXJJc0NvbXBvbmVudCkge1xuICAgICAgICAgIC8vIE5vdGU6IFRoZSBjb21wb25lbnQgdmlldyBpcyBub3QgeWV0IGNyZWF0ZWQgd2hlblxuICAgICAgICAgIC8vIHRoaXMgbWV0aG9kIGlzIGNhbGxlZCFcbiAgICAgICAgICByZXR1cm4gbmV3IF9Db21wb25lbnRWaWV3Q2hhbmdlRGV0ZWN0b3JSZWYodGhpcyk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgcmV0dXJuIHRoaXMucGFyZW50Vmlldy5jaGFuZ2VEZXRlY3RvcjtcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cblxuICAgIHJldHVybiBVTkRFRklORUQ7XG4gIH1cblxuICBwcml2YXRlIF9idWlsZEF0dHJpYnV0ZShkZXA6IERpcmVjdGl2ZURlcGVuZGVuY3kpOiBzdHJpbmcge1xuICAgIHZhciBhdHRyaWJ1dGVzID0gdGhpcy5wcm90by5hdHRyaWJ1dGVzO1xuICAgIGlmIChpc1ByZXNlbnQoYXR0cmlidXRlcykgJiYgU3RyaW5nTWFwV3JhcHBlci5jb250YWlucyhhdHRyaWJ1dGVzLCBkZXAuYXR0cmlidXRlTmFtZSkpIHtcbiAgICAgIHJldHVybiBhdHRyaWJ1dGVzW2RlcC5hdHRyaWJ1dGVOYW1lXTtcbiAgICB9IGVsc2Uge1xuICAgICAgcmV0dXJuIG51bGw7XG4gICAgfVxuICB9XG5cbiAgYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkocXVlcnk6IFF1ZXJ5TWV0YWRhdGEsIGxpc3Q6IGFueVtdKTogdm9pZCB7XG4gICAgdmFyIHRlbXBsYXRlUmVmID0gdGhpcy5nZXRUZW1wbGF0ZVJlZigpO1xuICAgIGlmIChxdWVyeS5zZWxlY3RvciA9PT0gVGVtcGxhdGVSZWYgJiYgaXNQcmVzZW50KHRlbXBsYXRlUmVmKSkge1xuICAgICAgbGlzdC5wdXNoKHRlbXBsYXRlUmVmKTtcbiAgICB9XG4gICAgaWYgKHRoaXMuX3N0cmF0ZWd5ICE9IG51bGwpIHtcbiAgICAgIHRoaXMuX3N0cmF0ZWd5LmFkZERpcmVjdGl2ZXNNYXRjaGluZ1F1ZXJ5KHF1ZXJ5LCBsaXN0KTtcbiAgICB9XG4gIH1cblxuICBwcml2YXRlIF9idWlsZFF1ZXJ5U3RyYXRlZ3koKTogX1F1ZXJ5U3RyYXRlZ3kge1xuICAgIGlmICh0aGlzLnByb3RvLnByb3RvUXVlcnlSZWZzLmxlbmd0aCA9PT0gMCkge1xuICAgICAgcmV0dXJuIF9lbXB0eVF1ZXJ5U3RyYXRlZ3k7XG4gICAgfSBlbHNlIGlmICh0aGlzLnByb3RvLnByb3RvUXVlcnlSZWZzLmxlbmd0aCA8PVxuICAgICAgICAgICAgICAgSW5saW5lUXVlcnlTdHJhdGVneS5OVU1CRVJfT0ZfU1VQUE9SVEVEX1FVRVJJRVMpIHtcbiAgICAgIHJldHVybiBuZXcgSW5saW5lUXVlcnlTdHJhdGVneSh0aGlzKTtcbiAgICB9IGVsc2Uge1xuICAgICAgcmV0dXJuIG5ldyBEeW5hbWljUXVlcnlTdHJhdGVneSh0aGlzKTtcbiAgICB9XG4gIH1cblxuXG4gIGdldERpcmVjdGl2ZUF0SW5kZXgoaW5kZXg6IG51bWJlcik6IGFueSB7IHJldHVybiB0aGlzLl9pbmplY3Rvci5nZXRBdChpbmRleCk7IH1cblxuICBuZ0FmdGVyVmlld0NoZWNrZWQoKTogdm9pZCB7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLl9xdWVyeVN0cmF0ZWd5KSkgdGhpcy5fcXVlcnlTdHJhdGVneS51cGRhdGVWaWV3UXVlcmllcygpO1xuICB9XG5cbiAgbmdBZnRlckNvbnRlbnRDaGVja2VkKCk6IHZvaWQge1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5fcXVlcnlTdHJhdGVneSkpIHRoaXMuX3F1ZXJ5U3RyYXRlZ3kudXBkYXRlQ29udGVudFF1ZXJpZXMoKTtcbiAgfVxuXG4gIHRyYXZlcnNlQW5kU2V0UXVlcmllc0FzRGlydHkoKTogdm9pZCB7XG4gICAgdmFyIGluajogQXBwRWxlbWVudCA9IHRoaXM7XG4gICAgd2hpbGUgKGlzUHJlc2VudChpbmopKSB7XG4gICAgICBpbmouX3NldFF1ZXJpZXNBc0RpcnR5KCk7XG4gICAgICBpZiAoaXNCbGFuayhpbmoucGFyZW50KSAmJiBpbmoucGFyZW50Vmlldy5wcm90by50eXBlID09PSBWaWV3VHlwZS5FTUJFRERFRCkge1xuICAgICAgICBpbmogPSBpbmoucGFyZW50Vmlldy5jb250YWluZXJBcHBFbGVtZW50O1xuICAgICAgfSBlbHNlIHtcbiAgICAgICAgaW5qID0gaW5qLnBhcmVudDtcbiAgICAgIH1cbiAgICB9XG4gIH1cblxuICBwcml2YXRlIF9zZXRRdWVyaWVzQXNEaXJ0eSgpOiB2b2lkIHtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMuX3F1ZXJ5U3RyYXRlZ3kpKSB7XG4gICAgICB0aGlzLl9xdWVyeVN0cmF0ZWd5LnNldENvbnRlbnRRdWVyaWVzQXNEaXJ0eSgpO1xuICAgIH1cbiAgICBpZiAodGhpcy5wYXJlbnRWaWV3LnByb3RvLnR5cGUgPT09IFZpZXdUeXBlLkNPTVBPTkVOVCkge1xuICAgICAgdGhpcy5wYXJlbnRWaWV3LmNvbnRhaW5lckFwcEVsZW1lbnQuX3F1ZXJ5U3RyYXRlZ3kuc2V0Vmlld1F1ZXJpZXNBc0RpcnR5KCk7XG4gICAgfVxuICB9XG59XG5cbmludGVyZmFjZSBfUXVlcnlTdHJhdGVneSB7XG4gIHNldENvbnRlbnRRdWVyaWVzQXNEaXJ0eSgpOiB2b2lkO1xuICBzZXRWaWV3UXVlcmllc0FzRGlydHkoKTogdm9pZDtcbiAgdXBkYXRlQ29udGVudFF1ZXJpZXMoKTogdm9pZDtcbiAgdXBkYXRlVmlld1F1ZXJpZXMoKTogdm9pZDtcbiAgZmluZFF1ZXJ5KHF1ZXJ5OiBRdWVyeU1ldGFkYXRhKTogUXVlcnlSZWY7XG59XG5cbmNsYXNzIF9FbXB0eVF1ZXJ5U3RyYXRlZ3kgaW1wbGVtZW50cyBfUXVlcnlTdHJhdGVneSB7XG4gIHNldENvbnRlbnRRdWVyaWVzQXNEaXJ0eSgpOiB2b2lkIHt9XG4gIHNldFZpZXdRdWVyaWVzQXNEaXJ0eSgpOiB2b2lkIHt9XG4gIHVwZGF0ZUNvbnRlbnRRdWVyaWVzKCk6IHZvaWQge31cbiAgdXBkYXRlVmlld1F1ZXJpZXMoKTogdm9pZCB7fVxuICBmaW5kUXVlcnkocXVlcnk6IFF1ZXJ5TWV0YWRhdGEpOiBRdWVyeVJlZiB7XG4gICAgdGhyb3cgbmV3IEJhc2VFeGNlcHRpb24oYENhbm5vdCBmaW5kIHF1ZXJ5IGZvciBkaXJlY3RpdmUgJHtxdWVyeX0uYCk7XG4gIH1cbn1cblxudmFyIF9lbXB0eVF1ZXJ5U3RyYXRlZ3kgPSBuZXcgX0VtcHR5UXVlcnlTdHJhdGVneSgpO1xuXG5jbGFzcyBJbmxpbmVRdWVyeVN0cmF0ZWd5IGltcGxlbWVudHMgX1F1ZXJ5U3RyYXRlZ3kge1xuICBzdGF0aWMgTlVNQkVSX09GX1NVUFBPUlRFRF9RVUVSSUVTID0gMztcblxuICBxdWVyeTA6IFF1ZXJ5UmVmO1xuICBxdWVyeTE6IFF1ZXJ5UmVmO1xuICBxdWVyeTI6IFF1ZXJ5UmVmO1xuXG4gIGNvbnN0cnVjdG9yKGVpOiBBcHBFbGVtZW50KSB7XG4gICAgdmFyIHByb3RvUmVmcyA9IGVpLnByb3RvLnByb3RvUXVlcnlSZWZzO1xuICAgIGlmIChwcm90b1JlZnMubGVuZ3RoID4gMCkgdGhpcy5xdWVyeTAgPSBuZXcgUXVlcnlSZWYocHJvdG9SZWZzWzBdLCBlaSk7XG4gICAgaWYgKHByb3RvUmVmcy5sZW5ndGggPiAxKSB0aGlzLnF1ZXJ5MSA9IG5ldyBRdWVyeVJlZihwcm90b1JlZnNbMV0sIGVpKTtcbiAgICBpZiAocHJvdG9SZWZzLmxlbmd0aCA+IDIpIHRoaXMucXVlcnkyID0gbmV3IFF1ZXJ5UmVmKHByb3RvUmVmc1syXSwgZWkpO1xuICB9XG5cbiAgc2V0Q29udGVudFF1ZXJpZXNBc0RpcnR5KCk6IHZvaWQge1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTApICYmICF0aGlzLnF1ZXJ5MC5pc1ZpZXdRdWVyeSkgdGhpcy5xdWVyeTAuZGlydHkgPSB0cnVlO1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTEpICYmICF0aGlzLnF1ZXJ5MS5pc1ZpZXdRdWVyeSkgdGhpcy5xdWVyeTEuZGlydHkgPSB0cnVlO1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTIpICYmICF0aGlzLnF1ZXJ5Mi5pc1ZpZXdRdWVyeSkgdGhpcy5xdWVyeTIuZGlydHkgPSB0cnVlO1xuICB9XG5cbiAgc2V0Vmlld1F1ZXJpZXNBc0RpcnR5KCk6IHZvaWQge1xuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTApICYmIHRoaXMucXVlcnkwLmlzVmlld1F1ZXJ5KSB0aGlzLnF1ZXJ5MC5kaXJ0eSA9IHRydWU7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MSkgJiYgdGhpcy5xdWVyeTEuaXNWaWV3UXVlcnkpIHRoaXMucXVlcnkxLmRpcnR5ID0gdHJ1ZTtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkyKSAmJiB0aGlzLnF1ZXJ5Mi5pc1ZpZXdRdWVyeSkgdGhpcy5xdWVyeTIuZGlydHkgPSB0cnVlO1xuICB9XG5cbiAgdXBkYXRlQ29udGVudFF1ZXJpZXMoKSB7XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MCkgJiYgIXRoaXMucXVlcnkwLmlzVmlld1F1ZXJ5KSB7XG4gICAgICB0aGlzLnF1ZXJ5MC51cGRhdGUoKTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MSkgJiYgIXRoaXMucXVlcnkxLmlzVmlld1F1ZXJ5KSB7XG4gICAgICB0aGlzLnF1ZXJ5MS51cGRhdGUoKTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MikgJiYgIXRoaXMucXVlcnkyLmlzVmlld1F1ZXJ5KSB7XG4gICAgICB0aGlzLnF1ZXJ5Mi51cGRhdGUoKTtcbiAgICB9XG4gIH1cblxuICB1cGRhdGVWaWV3UXVlcmllcygpIHtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkwKSAmJiB0aGlzLnF1ZXJ5MC5pc1ZpZXdRdWVyeSkge1xuICAgICAgdGhpcy5xdWVyeTAudXBkYXRlKCk7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQodGhpcy5xdWVyeTEpICYmIHRoaXMucXVlcnkxLmlzVmlld1F1ZXJ5KSB7XG4gICAgICB0aGlzLnF1ZXJ5MS51cGRhdGUoKTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudCh0aGlzLnF1ZXJ5MikgJiYgdGhpcy5xdWVyeTIuaXNWaWV3UXVlcnkpIHtcbiAgICAgIHRoaXMucXVlcnkyLnVwZGF0ZSgpO1xuICAgIH1cbiAgfVxuXG4gIGZpbmRRdWVyeShxdWVyeTogUXVlcnlNZXRhZGF0YSk6IFF1ZXJ5UmVmIHtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkwKSAmJiB0aGlzLnF1ZXJ5MC5wcm90b1F1ZXJ5UmVmLnF1ZXJ5ID09PSBxdWVyeSkge1xuICAgICAgcmV0dXJuIHRoaXMucXVlcnkwO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkxKSAmJiB0aGlzLnF1ZXJ5MS5wcm90b1F1ZXJ5UmVmLnF1ZXJ5ID09PSBxdWVyeSkge1xuICAgICAgcmV0dXJuIHRoaXMucXVlcnkxO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHRoaXMucXVlcnkyKSAmJiB0aGlzLnF1ZXJ5Mi5wcm90b1F1ZXJ5UmVmLnF1ZXJ5ID09PSBxdWVyeSkge1xuICAgICAgcmV0dXJuIHRoaXMucXVlcnkyO1xuICAgIH1cbiAgICB0aHJvdyBuZXcgQmFzZUV4Y2VwdGlvbihgQ2Fubm90IGZpbmQgcXVlcnkgZm9yIGRpcmVjdGl2ZSAke3F1ZXJ5fS5gKTtcbiAgfVxufVxuXG5jbGFzcyBEeW5hbWljUXVlcnlTdHJhdGVneSBpbXBsZW1lbnRzIF9RdWVyeVN0cmF0ZWd5IHtcbiAgcXVlcmllczogUXVlcnlSZWZbXTtcblxuICBjb25zdHJ1Y3RvcihlaTogQXBwRWxlbWVudCkge1xuICAgIHRoaXMucXVlcmllcyA9IGVpLnByb3RvLnByb3RvUXVlcnlSZWZzLm1hcChwID0+IG5ldyBRdWVyeVJlZihwLCBlaSkpO1xuICB9XG5cbiAgc2V0Q29udGVudFF1ZXJpZXNBc0RpcnR5KCk6IHZvaWQge1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdGhpcy5xdWVyaWVzLmxlbmd0aDsgKytpKSB7XG4gICAgICB2YXIgcSA9IHRoaXMucXVlcmllc1tpXTtcbiAgICAgIGlmICghcS5pc1ZpZXdRdWVyeSkgcS5kaXJ0eSA9IHRydWU7XG4gICAgfVxuICB9XG5cbiAgc2V0Vmlld1F1ZXJpZXNBc0RpcnR5KCk6IHZvaWQge1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdGhpcy5xdWVyaWVzLmxlbmd0aDsgKytpKSB7XG4gICAgICB2YXIgcSA9IHRoaXMucXVlcmllc1tpXTtcbiAgICAgIGlmIChxLmlzVmlld1F1ZXJ5KSBxLmRpcnR5ID0gdHJ1ZTtcbiAgICB9XG4gIH1cblxuICB1cGRhdGVDb250ZW50UXVlcmllcygpIHtcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IHRoaXMucXVlcmllcy5sZW5ndGg7ICsraSkge1xuICAgICAgdmFyIHEgPSB0aGlzLnF1ZXJpZXNbaV07XG4gICAgICBpZiAoIXEuaXNWaWV3UXVlcnkpIHtcbiAgICAgICAgcS51cGRhdGUoKTtcbiAgICAgIH1cbiAgICB9XG4gIH1cblxuICB1cGRhdGVWaWV3UXVlcmllcygpIHtcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IHRoaXMucXVlcmllcy5sZW5ndGg7ICsraSkge1xuICAgICAgdmFyIHEgPSB0aGlzLnF1ZXJpZXNbaV07XG4gICAgICBpZiAocS5pc1ZpZXdRdWVyeSkge1xuICAgICAgICBxLnVwZGF0ZSgpO1xuICAgICAgfVxuICAgIH1cbiAgfVxuXG4gIGZpbmRRdWVyeShxdWVyeTogUXVlcnlNZXRhZGF0YSk6IFF1ZXJ5UmVmIHtcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IHRoaXMucXVlcmllcy5sZW5ndGg7ICsraSkge1xuICAgICAgdmFyIHEgPSB0aGlzLnF1ZXJpZXNbaV07XG4gICAgICBpZiAocS5wcm90b1F1ZXJ5UmVmLnF1ZXJ5ID09PSBxdWVyeSkge1xuICAgICAgICByZXR1cm4gcTtcbiAgICAgIH1cbiAgICB9XG4gICAgdGhyb3cgbmV3IEJhc2VFeGNlcHRpb24oYENhbm5vdCBmaW5kIHF1ZXJ5IGZvciBkaXJlY3RpdmUgJHtxdWVyeX0uYCk7XG4gIH1cbn1cblxuaW50ZXJmYWNlIF9FbGVtZW50RGlyZWN0aXZlU3RyYXRlZ3kge1xuICBnZXRDb21wb25lbnQoKTogYW55O1xuICBpc0NvbXBvbmVudEtleShrZXk6IEtleSk6IGJvb2xlYW47XG4gIGFkZERpcmVjdGl2ZXNNYXRjaGluZ1F1ZXJ5KHE6IFF1ZXJ5TWV0YWRhdGEsIHJlczogYW55W10pOiB2b2lkO1xuICBpbml0KCk6IHZvaWQ7XG59XG5cbi8qKlxuICogU3RyYXRlZ3kgdXNlZCBieSB0aGUgYEVsZW1lbnRJbmplY3RvcmAgd2hlbiB0aGUgbnVtYmVyIG9mIHByb3ZpZGVycyBpcyAxMCBvciBsZXNzLlxuICogSW4gc3VjaCBhIGNhc2UsIGlubGluaW5nIGZpZWxkcyBpcyBiZW5lZmljaWFsIGZvciBwZXJmb3JtYW5jZXMuXG4gKi9cbmNsYXNzIEVsZW1lbnREaXJlY3RpdmVJbmxpbmVTdHJhdGVneSBpbXBsZW1lbnRzIF9FbGVtZW50RGlyZWN0aXZlU3RyYXRlZ3kge1xuICBjb25zdHJ1Y3RvcihwdWJsaWMgaW5qZWN0b3JTdHJhdGVneTogSW5qZWN0b3JJbmxpbmVTdHJhdGVneSwgcHVibGljIF9laTogQXBwRWxlbWVudCkge31cblxuICBpbml0KCk6IHZvaWQge1xuICAgIHZhciBpID0gdGhpcy5pbmplY3RvclN0cmF0ZWd5O1xuICAgIHZhciBwID0gaS5wcm90b1N0cmF0ZWd5O1xuICAgIGkucmVzZXRDb25zdHJ1Y3Rpb25Db3VudGVyKCk7XG5cbiAgICBpZiAocC5wcm92aWRlcjAgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDApICYmIGkub2JqMCA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmowID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXIwLCBwLnZpc2liaWxpdHkwKTtcbiAgICBpZiAocC5wcm92aWRlcjEgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDEpICYmIGkub2JqMSA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmoxID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXIxLCBwLnZpc2liaWxpdHkxKTtcbiAgICBpZiAocC5wcm92aWRlcjIgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDIpICYmIGkub2JqMiA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmoyID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXIyLCBwLnZpc2liaWxpdHkyKTtcbiAgICBpZiAocC5wcm92aWRlcjMgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDMpICYmIGkub2JqMyA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmozID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXIzLCBwLnZpc2liaWxpdHkzKTtcbiAgICBpZiAocC5wcm92aWRlcjQgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDQpICYmIGkub2JqNCA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmo0ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI0LCBwLnZpc2liaWxpdHk0KTtcbiAgICBpZiAocC5wcm92aWRlcjUgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDUpICYmIGkub2JqNSA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmo1ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI1LCBwLnZpc2liaWxpdHk1KTtcbiAgICBpZiAocC5wcm92aWRlcjYgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDYpICYmIGkub2JqNiA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmo2ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI2LCBwLnZpc2liaWxpdHk2KTtcbiAgICBpZiAocC5wcm92aWRlcjcgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDcpICYmIGkub2JqNyA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmo3ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI3LCBwLnZpc2liaWxpdHk3KTtcbiAgICBpZiAocC5wcm92aWRlcjggaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDgpICYmIGkub2JqOCA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmo4ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI4LCBwLnZpc2liaWxpdHk4KTtcbiAgICBpZiAocC5wcm92aWRlcjkgaW5zdGFuY2VvZiBEaXJlY3RpdmVQcm92aWRlciAmJiBpc1ByZXNlbnQocC5rZXlJZDkpICYmIGkub2JqOSA9PT0gVU5ERUZJTkVEKVxuICAgICAgaS5vYmo5ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI5LCBwLnZpc2liaWxpdHk5KTtcbiAgfVxuXG4gIGdldENvbXBvbmVudCgpOiBhbnkgeyByZXR1cm4gdGhpcy5pbmplY3RvclN0cmF0ZWd5Lm9iajA7IH1cblxuICBpc0NvbXBvbmVudEtleShrZXk6IEtleSk6IGJvb2xlYW4ge1xuICAgIHJldHVybiB0aGlzLl9laS5wcm90by5maXJzdFByb3ZpZGVySXNDb21wb25lbnQgJiYgaXNQcmVzZW50KGtleSkgJiZcbiAgICAgICAgICAga2V5LmlkID09PSB0aGlzLmluamVjdG9yU3RyYXRlZ3kucHJvdG9TdHJhdGVneS5rZXlJZDA7XG4gIH1cblxuICBhZGREaXJlY3RpdmVzTWF0Y2hpbmdRdWVyeShxdWVyeTogUXVlcnlNZXRhZGF0YSwgbGlzdDogYW55W10pOiB2b2lkIHtcbiAgICB2YXIgaSA9IHRoaXMuaW5qZWN0b3JTdHJhdGVneTtcbiAgICB2YXIgcCA9IGkucHJvdG9TdHJhdGVneTtcbiAgICBpZiAoaXNQcmVzZW50KHAucHJvdmlkZXIwKSAmJiBwLnByb3ZpZGVyMC5rZXkudG9rZW4gPT09IHF1ZXJ5LnNlbGVjdG9yKSB7XG4gICAgICBpZiAoaS5vYmowID09PSBVTkRFRklORUQpIGkub2JqMCA9IGkuaW5zdGFudGlhdGVQcm92aWRlcihwLnByb3ZpZGVyMCwgcC52aXNpYmlsaXR5MCk7XG4gICAgICBsaXN0LnB1c2goaS5vYmowKTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudChwLnByb3ZpZGVyMSkgJiYgcC5wcm92aWRlcjEua2V5LnRva2VuID09PSBxdWVyeS5zZWxlY3Rvcikge1xuICAgICAgaWYgKGkub2JqMSA9PT0gVU5ERUZJTkVEKSBpLm9iajEgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjEsIHAudmlzaWJpbGl0eTEpO1xuICAgICAgbGlzdC5wdXNoKGkub2JqMSk7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQocC5wcm92aWRlcjIpICYmIHAucHJvdmlkZXIyLmtleS50b2tlbiA9PT0gcXVlcnkuc2VsZWN0b3IpIHtcbiAgICAgIGlmIChpLm9iajIgPT09IFVOREVGSU5FRCkgaS5vYmoyID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXIyLCBwLnZpc2liaWxpdHkyKTtcbiAgICAgIGxpc3QucHVzaChpLm9iajIpO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHAucHJvdmlkZXIzKSAmJiBwLnByb3ZpZGVyMy5rZXkudG9rZW4gPT09IHF1ZXJ5LnNlbGVjdG9yKSB7XG4gICAgICBpZiAoaS5vYmozID09PSBVTkRFRklORUQpIGkub2JqMyA9IGkuaW5zdGFudGlhdGVQcm92aWRlcihwLnByb3ZpZGVyMywgcC52aXNpYmlsaXR5Myk7XG4gICAgICBsaXN0LnB1c2goaS5vYmozKTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudChwLnByb3ZpZGVyNCkgJiYgcC5wcm92aWRlcjQua2V5LnRva2VuID09PSBxdWVyeS5zZWxlY3Rvcikge1xuICAgICAgaWYgKGkub2JqNCA9PT0gVU5ERUZJTkVEKSBpLm9iajQgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjQsIHAudmlzaWJpbGl0eTQpO1xuICAgICAgbGlzdC5wdXNoKGkub2JqNCk7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQocC5wcm92aWRlcjUpICYmIHAucHJvdmlkZXI1LmtleS50b2tlbiA9PT0gcXVlcnkuc2VsZWN0b3IpIHtcbiAgICAgIGlmIChpLm9iajUgPT09IFVOREVGSU5FRCkgaS5vYmo1ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI1LCBwLnZpc2liaWxpdHk1KTtcbiAgICAgIGxpc3QucHVzaChpLm9iajUpO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHAucHJvdmlkZXI2KSAmJiBwLnByb3ZpZGVyNi5rZXkudG9rZW4gPT09IHF1ZXJ5LnNlbGVjdG9yKSB7XG4gICAgICBpZiAoaS5vYmo2ID09PSBVTkRFRklORUQpIGkub2JqNiA9IGkuaW5zdGFudGlhdGVQcm92aWRlcihwLnByb3ZpZGVyNiwgcC52aXNpYmlsaXR5Nik7XG4gICAgICBsaXN0LnB1c2goaS5vYmo2KTtcbiAgICB9XG4gICAgaWYgKGlzUHJlc2VudChwLnByb3ZpZGVyNykgJiYgcC5wcm92aWRlcjcua2V5LnRva2VuID09PSBxdWVyeS5zZWxlY3Rvcikge1xuICAgICAgaWYgKGkub2JqNyA9PT0gVU5ERUZJTkVEKSBpLm9iajcgPSBpLmluc3RhbnRpYXRlUHJvdmlkZXIocC5wcm92aWRlcjcsIHAudmlzaWJpbGl0eTcpO1xuICAgICAgbGlzdC5wdXNoKGkub2JqNyk7XG4gICAgfVxuICAgIGlmIChpc1ByZXNlbnQocC5wcm92aWRlcjgpICYmIHAucHJvdmlkZXI4LmtleS50b2tlbiA9PT0gcXVlcnkuc2VsZWN0b3IpIHtcbiAgICAgIGlmIChpLm9iajggPT09IFVOREVGSU5FRCkgaS5vYmo4ID0gaS5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXI4LCBwLnZpc2liaWxpdHk4KTtcbiAgICAgIGxpc3QucHVzaChpLm9iajgpO1xuICAgIH1cbiAgICBpZiAoaXNQcmVzZW50KHAucHJvdmlkZXI5KSAmJiBwLnByb3ZpZGVyOS5rZXkudG9rZW4gPT09IHF1ZXJ5LnNlbGVjdG9yKSB7XG4gICAgICBpZiAoaS5vYmo5ID09PSBVTkRFRklORUQpIGkub2JqOSA9IGkuaW5zdGFudGlhdGVQcm92aWRlcihwLnByb3ZpZGVyOSwgcC52aXNpYmlsaXR5OSk7XG4gICAgICBsaXN0LnB1c2goaS5vYmo5KTtcbiAgICB9XG4gIH1cbn1cblxuLyoqXG4gKiBTdHJhdGVneSB1c2VkIGJ5IHRoZSBgRWxlbWVudEluamVjdG9yYCB3aGVuIHRoZSBudW1iZXIgb2YgYmluZGluZ3MgaXMgMTEgb3IgbW9yZS5cbiAqIEluIHN1Y2ggYSBjYXNlLCB0aGVyZSBhcmUgdG9vIG1hbnkgZmllbGRzIHRvIGlubGluZSAoc2VlIEVsZW1lbnRJbmplY3RvcklubGluZVN0cmF0ZWd5KS5cbiAqL1xuY2xhc3MgRWxlbWVudERpcmVjdGl2ZUR5bmFtaWNTdHJhdGVneSBpbXBsZW1lbnRzIF9FbGVtZW50RGlyZWN0aXZlU3RyYXRlZ3kge1xuICBjb25zdHJ1Y3RvcihwdWJsaWMgaW5qZWN0b3JTdHJhdGVneTogSW5qZWN0b3JEeW5hbWljU3RyYXRlZ3ksIHB1YmxpYyBfZWk6IEFwcEVsZW1lbnQpIHt9XG5cbiAgaW5pdCgpOiB2b2lkIHtcbiAgICB2YXIgaW5qID0gdGhpcy5pbmplY3RvclN0cmF0ZWd5O1xuICAgIHZhciBwID0gaW5qLnByb3RvU3RyYXRlZ3k7XG4gICAgaW5qLnJlc2V0Q29uc3RydWN0aW9uQ291bnRlcigpO1xuXG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCBwLmtleUlkcy5sZW5ndGg7IGkrKykge1xuICAgICAgaWYgKHAucHJvdmlkZXJzW2ldIGluc3RhbmNlb2YgRGlyZWN0aXZlUHJvdmlkZXIgJiYgaXNQcmVzZW50KHAua2V5SWRzW2ldKSAmJlxuICAgICAgICAgIGluai5vYmpzW2ldID09PSBVTkRFRklORUQpIHtcbiAgICAgICAgaW5qLm9ianNbaV0gPSBpbmouaW5zdGFudGlhdGVQcm92aWRlcihwLnByb3ZpZGVyc1tpXSwgcC52aXNpYmlsaXRpZXNbaV0pO1xuICAgICAgfVxuICAgIH1cbiAgfVxuXG4gIGdldENvbXBvbmVudCgpOiBhbnkgeyByZXR1cm4gdGhpcy5pbmplY3RvclN0cmF0ZWd5Lm9ianNbMF07IH1cblxuICBpc0NvbXBvbmVudEtleShrZXk6IEtleSk6IGJvb2xlYW4ge1xuICAgIHZhciBwID0gdGhpcy5pbmplY3RvclN0cmF0ZWd5LnByb3RvU3RyYXRlZ3k7XG4gICAgcmV0dXJuIHRoaXMuX2VpLnByb3RvLmZpcnN0UHJvdmlkZXJJc0NvbXBvbmVudCAmJiBpc1ByZXNlbnQoa2V5KSAmJiBrZXkuaWQgPT09IHAua2V5SWRzWzBdO1xuICB9XG5cbiAgYWRkRGlyZWN0aXZlc01hdGNoaW5nUXVlcnkocXVlcnk6IFF1ZXJ5TWV0YWRhdGEsIGxpc3Q6IGFueVtdKTogdm9pZCB7XG4gICAgdmFyIGlzdCA9IHRoaXMuaW5qZWN0b3JTdHJhdGVneTtcbiAgICB2YXIgcCA9IGlzdC5wcm90b1N0cmF0ZWd5O1xuXG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCBwLnByb3ZpZGVycy5sZW5ndGg7IGkrKykge1xuICAgICAgaWYgKHAucHJvdmlkZXJzW2ldLmtleS50b2tlbiA9PT0gcXVlcnkuc2VsZWN0b3IpIHtcbiAgICAgICAgaWYgKGlzdC5vYmpzW2ldID09PSBVTkRFRklORUQpIHtcbiAgICAgICAgICBpc3Qub2Jqc1tpXSA9IGlzdC5pbnN0YW50aWF0ZVByb3ZpZGVyKHAucHJvdmlkZXJzW2ldLCBwLnZpc2liaWxpdGllc1tpXSk7XG4gICAgICAgIH1cbiAgICAgICAgbGlzdC5wdXNoKGlzdC5vYmpzW2ldKTtcbiAgICAgIH1cbiAgICB9XG4gIH1cbn1cblxuZXhwb3J0IGNsYXNzIFByb3RvUXVlcnlSZWYge1xuICBjb25zdHJ1Y3RvcihwdWJsaWMgZGlySW5kZXg6IG51bWJlciwgcHVibGljIHNldHRlcjogU2V0dGVyRm4sIHB1YmxpYyBxdWVyeTogUXVlcnlNZXRhZGF0YSkge31cblxuICBnZXQgdXNlc1Byb3BlcnR5U3ludGF4KCk6IGJvb2xlYW4geyByZXR1cm4gaXNQcmVzZW50KHRoaXMuc2V0dGVyKTsgfVxufVxuXG5leHBvcnQgY2xhc3MgUXVlcnlSZWYge1xuICBwdWJsaWMgbGlzdDogUXVlcnlMaXN0PGFueT47XG4gIHB1YmxpYyBkaXJ0eTogYm9vbGVhbjtcblxuICBjb25zdHJ1Y3RvcihwdWJsaWMgcHJvdG9RdWVyeVJlZjogUHJvdG9RdWVyeVJlZiwgcHJpdmF0ZSBvcmlnaW5hdG9yOiBBcHBFbGVtZW50KSB7XG4gICAgdGhpcy5saXN0ID0gbmV3IFF1ZXJ5TGlzdDxhbnk+KCk7XG4gICAgdGhpcy5kaXJ0eSA9IHRydWU7XG4gIH1cblxuICBnZXQgaXNWaWV3UXVlcnkoKTogYm9vbGVhbiB7IHJldHVybiB0aGlzLnByb3RvUXVlcnlSZWYucXVlcnkuaXNWaWV3UXVlcnk7IH1cblxuICB1cGRhdGUoKTogdm9pZCB7XG4gICAgaWYgKCF0aGlzLmRpcnR5KSByZXR1cm47XG4gICAgdGhpcy5fdXBkYXRlKCk7XG4gICAgdGhpcy5kaXJ0eSA9IGZhbHNlO1xuXG4gICAgLy8gVE9ETyBkZWxldGUgdGhlIGNoZWNrIG9uY2Ugb25seSBmaWVsZCBxdWVyaWVzIGFyZSBzdXBwb3J0ZWRcbiAgICBpZiAodGhpcy5wcm90b1F1ZXJ5UmVmLnVzZXNQcm9wZXJ0eVN5bnRheCkge1xuICAgICAgdmFyIGRpciA9IHRoaXMub3JpZ2luYXRvci5nZXREaXJlY3RpdmVBdEluZGV4KHRoaXMucHJvdG9RdWVyeVJlZi5kaXJJbmRleCk7XG4gICAgICBpZiAodGhpcy5wcm90b1F1ZXJ5UmVmLnF1ZXJ5LmZpcnN0KSB7XG4gICAgICAgIHRoaXMucHJvdG9RdWVyeVJlZi5zZXR0ZXIoZGlyLCB0aGlzLmxpc3QubGVuZ3RoID4gMCA/IHRoaXMubGlzdC5maXJzdCA6IG51bGwpO1xuICAgICAgfSBlbHNlIHtcbiAgICAgICAgdGhpcy5wcm90b1F1ZXJ5UmVmLnNldHRlcihkaXIsIHRoaXMubGlzdCk7XG4gICAgICB9XG4gICAgfVxuXG4gICAgdGhpcy5saXN0Lm5vdGlmeU9uQ2hhbmdlcygpO1xuICB9XG5cbiAgcHJpdmF0ZSBfdXBkYXRlKCk6IHZvaWQge1xuICAgIHZhciBhZ2dyZWdhdG9yID0gW107XG4gICAgaWYgKHRoaXMucHJvdG9RdWVyeVJlZi5xdWVyeS5pc1ZpZXdRdWVyeSkge1xuICAgICAgLy8gaW50ZW50aW9uYWxseSBza2lwcGluZyBvcmlnaW5hdG9yIGZvciB2aWV3IHF1ZXJpZXMuXG4gICAgICB2YXIgbmVzdGVkVmlldyA9IHRoaXMub3JpZ2luYXRvci5jb21wb25lbnRWaWV3O1xuICAgICAgaWYgKGlzUHJlc2VudChuZXN0ZWRWaWV3KSkgdGhpcy5fdmlzaXRWaWV3KG5lc3RlZFZpZXcsIGFnZ3JlZ2F0b3IpO1xuICAgIH0gZWxzZSB7XG4gICAgICB0aGlzLl92aXNpdCh0aGlzLm9yaWdpbmF0b3IsIGFnZ3JlZ2F0b3IpO1xuICAgIH1cbiAgICB0aGlzLmxpc3QucmVzZXQoYWdncmVnYXRvcik7XG4gIH07XG5cbiAgcHJpdmF0ZSBfdmlzaXQoaW5qOiBBcHBFbGVtZW50LCBhZ2dyZWdhdG9yOiBhbnlbXSk6IHZvaWQge1xuICAgIHZhciB2aWV3ID0gaW5qLnBhcmVudFZpZXc7XG4gICAgdmFyIHN0YXJ0SWR4ID0gaW5qLnByb3RvLmluZGV4O1xuICAgIGZvciAodmFyIGkgPSBzdGFydElkeDsgaSA8IHZpZXcuYXBwRWxlbWVudHMubGVuZ3RoOyBpKyspIHtcbiAgICAgIHZhciBjdXJJbmogPSB2aWV3LmFwcEVsZW1lbnRzW2ldO1xuICAgICAgLy8gVGhlIGZpcnN0IGluamVjdG9yIGFmdGVyIGluaiwgdGhhdCBpcyBvdXRzaWRlIHRoZSBzdWJ0cmVlIHJvb3RlZCBhdFxuICAgICAgLy8gaW5qIGhhcyB0byBoYXZlIGEgbnVsbCBwYXJlbnQgb3IgYSBwYXJlbnQgdGhhdCBpcyBhbiBhbmNlc3RvciBvZiBpbmouXG4gICAgICBpZiAoaSA+IHN0YXJ0SWR4ICYmIChpc0JsYW5rKGN1ckluai5wYXJlbnQpIHx8IGN1ckluai5wYXJlbnQucHJvdG8uaW5kZXggPCBzdGFydElkeCkpIHtcbiAgICAgICAgYnJlYWs7XG4gICAgICB9XG5cbiAgICAgIGlmICghdGhpcy5wcm90b1F1ZXJ5UmVmLnF1ZXJ5LmRlc2NlbmRhbnRzICYmXG4gICAgICAgICAgIShjdXJJbmoucGFyZW50ID09IHRoaXMub3JpZ2luYXRvciB8fCBjdXJJbmogPT0gdGhpcy5vcmlnaW5hdG9yKSlcbiAgICAgICAgY29udGludWU7XG5cbiAgICAgIC8vIFdlIHZpc2l0IHRoZSB2aWV3IGNvbnRhaW5lcihWQykgdmlld3MgcmlnaHQgYWZ0ZXIgdGhlIGluamVjdG9yIHRoYXQgY29udGFpbnNcbiAgICAgIC8vIHRoZSBWQy4gVGhlb3JldGljYWxseSwgdGhhdCBtaWdodCBub3QgYmUgdGhlIHJpZ2h0IG9yZGVyIGlmIHRoZXJlIGFyZVxuICAgICAgLy8gY2hpbGQgaW5qZWN0b3JzIG9mIHNhaWQgaW5qZWN0b3IuIE5vdCBjbGVhciB3aGV0aGVyIGlmIHN1Y2ggY2FzZSBjYW5cbiAgICAgIC8vIGV2ZW4gYmUgY29uc3RydWN0ZWQgd2l0aCB0aGUgY3VycmVudCBhcGlzLlxuICAgICAgdGhpcy5fdmlzaXRJbmplY3RvcihjdXJJbmosIGFnZ3JlZ2F0b3IpO1xuICAgICAgdGhpcy5fdmlzaXRWaWV3Q29udGFpbmVyVmlld3MoY3VySW5qLm5lc3RlZFZpZXdzLCBhZ2dyZWdhdG9yKTtcbiAgICB9XG4gIH1cblxuICBwcml2YXRlIF92aXNpdEluamVjdG9yKGluajogQXBwRWxlbWVudCwgYWdncmVnYXRvcjogYW55W10pIHtcbiAgICBpZiAodGhpcy5wcm90b1F1ZXJ5UmVmLnF1ZXJ5LmlzVmFyQmluZGluZ1F1ZXJ5KSB7XG4gICAgICB0aGlzLl9hZ2dyZWdhdGVWYXJpYWJsZUJpbmRpbmcoaW5qLCBhZ2dyZWdhdG9yKTtcbiAgICB9IGVsc2Uge1xuICAgICAgdGhpcy5fYWdncmVnYXRlRGlyZWN0aXZlKGluaiwgYWdncmVnYXRvcik7XG4gICAgfVxuICB9XG5cbiAgcHJpdmF0ZSBfdmlzaXRWaWV3Q29udGFpbmVyVmlld3Modmlld3M6IEFwcFZpZXdbXSwgYWdncmVnYXRvcjogYW55W10pIHtcbiAgICBpZiAoaXNQcmVzZW50KHZpZXdzKSkge1xuICAgICAgZm9yICh2YXIgaiA9IDA7IGogPCB2aWV3cy5sZW5ndGg7IGorKykge1xuICAgICAgICB0aGlzLl92aXNpdFZpZXcodmlld3Nbal0sIGFnZ3JlZ2F0b3IpO1xuICAgICAgfVxuICAgIH1cbiAgfVxuXG4gIHByaXZhdGUgX3Zpc2l0Vmlldyh2aWV3OiBBcHBWaWV3LCBhZ2dyZWdhdG9yOiBhbnlbXSkge1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdmlldy5hcHBFbGVtZW50cy5sZW5ndGg7IGkrKykge1xuICAgICAgdmFyIGluaiA9IHZpZXcuYXBwRWxlbWVudHNbaV07XG4gICAgICB0aGlzLl92aXNpdEluamVjdG9yKGluaiwgYWdncmVnYXRvcik7XG4gICAgICB0aGlzLl92aXNpdFZpZXdDb250YWluZXJWaWV3cyhpbmoubmVzdGVkVmlld3MsIGFnZ3JlZ2F0b3IpO1xuICAgIH1cbiAgfVxuXG4gIHByaXZhdGUgX2FnZ3JlZ2F0ZVZhcmlhYmxlQmluZGluZyhpbmo6IEFwcEVsZW1lbnQsIGFnZ3JlZ2F0b3I6IGFueVtdKTogdm9pZCB7XG4gICAgdmFyIHZiID0gdGhpcy5wcm90b1F1ZXJ5UmVmLnF1ZXJ5LnZhckJpbmRpbmdzO1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdmIubGVuZ3RoOyArK2kpIHtcbiAgICAgIGlmIChpbmouaGFzVmFyaWFibGVCaW5kaW5nKHZiW2ldKSkge1xuICAgICAgICBhZ2dyZWdhdG9yLnB1c2goaW5qLmdldFZhcmlhYmxlQmluZGluZyh2YltpXSkpO1xuICAgICAgfVxuICAgIH1cbiAgfVxuXG4gIHByaXZhdGUgX2FnZ3JlZ2F0ZURpcmVjdGl2ZShpbmo6IEFwcEVsZW1lbnQsIGFnZ3JlZ2F0b3I6IGFueVtdKTogdm9pZCB7XG4gICAgaW5qLmFkZERpcmVjdGl2ZXNNYXRjaGluZ1F1ZXJ5KHRoaXMucHJvdG9RdWVyeVJlZi5xdWVyeSwgYWdncmVnYXRvcik7XG4gIH1cbn1cblxuY2xhc3MgX0NvbXBvbmVudFZpZXdDaGFuZ2VEZXRlY3RvclJlZiBleHRlbmRzIENoYW5nZURldGVjdG9yUmVmIHtcbiAgY29uc3RydWN0b3IocHJpdmF0ZSBfYXBwRWxlbWVudDogQXBwRWxlbWVudCkgeyBzdXBlcigpOyB9XG5cbiAgbWFya0ZvckNoZWNrKCk6IHZvaWQgeyB0aGlzLl9hcHBFbGVtZW50LmNvbXBvbmVudFZpZXcuY2hhbmdlRGV0ZWN0b3IucmVmLm1hcmtGb3JDaGVjaygpOyB9XG4gIGRldGFjaCgpOiB2b2lkIHsgdGhpcy5fYXBwRWxlbWVudC5jb21wb25lbnRWaWV3LmNoYW5nZURldGVjdG9yLnJlZi5kZXRhY2goKTsgfVxuICBkZXRlY3RDaGFuZ2VzKCk6IHZvaWQgeyB0aGlzLl9hcHBFbGVtZW50LmNvbXBvbmVudFZpZXcuY2hhbmdlRGV0ZWN0b3IucmVmLmRldGVjdENoYW5nZXMoKTsgfVxuICBjaGVja05vQ2hhbmdlcygpOiB2b2lkIHsgdGhpcy5fYXBwRWxlbWVudC5jb21wb25lbnRWaWV3LmNoYW5nZURldGVjdG9yLnJlZi5jaGVja05vQ2hhbmdlcygpOyB9XG4gIHJlYXR0YWNoKCk6IHZvaWQgeyB0aGlzLl9hcHBFbGVtZW50LmNvbXBvbmVudFZpZXcuY2hhbmdlRGV0ZWN0b3IucmVmLnJlYXR0YWNoKCk7IH1cbn1cbiJdfQ==