/* =========================================================
 * patternfly-bootstrap-treeview.js v2.1.0
 * =========================================================
 * Copyright 2013 Jonathan Miles
 * Project URL : http://www.jondmiles.com/bootstrap-treeview
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================= */

;(function ($, window, document, undefined) {

	/*global jQuery, console*/

	'use strict';

	var pluginName = 'treeview';

	var _default = {};

	_default.settings = {

		injectStyle: true,

		levels: 2,

		expandIcon: 'glyphicon glyphicon-plus',
		collapseIcon: 'glyphicon glyphicon-minus',
		loadingIcon: 'glyphicon glyphicon-hourglass',
		emptyIcon: 'glyphicon',
		nodeIcon: '',
		selectedIcon: '',
		checkedIcon: 'glyphicon glyphicon-check',
		partiallyCheckedIcon: 'glyphicon glyphicon-expand',
		uncheckedIcon: 'glyphicon glyphicon-unchecked',
		tagsClass: 'badge',

		color: undefined,
		backColor: undefined,
		borderColor: undefined,
		changedNodeColor: '#39A5DC',
		onhoverColor: '#F5F5F5',
		selectedColor: '#FFFFFF',
		selectedBackColor: '#428bca',
		searchResultColor: '#D9534F',
		searchResultBackColor: undefined,

		highlightSelected: true,
		highlightSearchResults: true,
		showBorder: true,
		showIcon: true,
		showImage: false,
		showCheckbox: false,
		checkboxFirst: false,
		highlightChanges: false,
		showTags: false,
		multiSelect: false,
		preventUnselect: false,
		allowReselect: false,
		hierarchicalCheck: false,
		propagateCheckEvent: false,
		wrapNodeText: false,

		// Event handlers
		onLoading: undefined,
		onLoadingFailed: undefined,
		onInitialized: undefined,
		onNodeRendered: undefined,
		onRendered: undefined,
		onDestroyed: undefined,

		onNodeChecked: undefined,
		onNodeCollapsed: undefined,
		onNodeDisabled: undefined,
		onNodeEnabled: undefined,
		onNodeExpanded: undefined,
		onNodeChanged: undefined,
		onNodeSelected: undefined,
		onNodeUnchecked: undefined,
		onNodeUnselected: undefined,

		onSearchComplete: undefined,
		onSearchCleared: undefined
	};

	_default.options = {
		silent: false,
		ignoreChildren: false
	};

	_default.searchOptions = {
		ignoreCase: true,
		exactMatch: false,
		revealResults: true
	};

	_default.dataUrl = {
		method: 'GET',
		dataType: 'json',
		cache: false
	};

	var Tree = function (element, options) {
		this.$element = $(element);
		this._elementId = element.id;
		this._styleId = this._elementId + '-style';

		this._init(options);

		return {

			// Options (public access)
			options: this._options,

			// Initialize / destroy methods
			init: $.proxy(this._init, this),
			remove: $.proxy(this._remove, this),

			// Query methods
			findNodes: $.proxy(this.findNodes, this),
			getNodes: $.proxy(this.getNodes, this), // todo document + test
			getParents: $.proxy(this.getParents, this),
			getSiblings: $.proxy(this.getSiblings, this),
			getSelected: $.proxy(this.getSelected, this),
			getUnselected: $.proxy(this.getUnselected, this),
			getExpanded: $.proxy(this.getExpanded, this),
			getCollapsed: $.proxy(this.getCollapsed, this),
			getChecked: $.proxy(this.getChecked, this),
			getUnchecked: $.proxy(this.getUnchecked, this),
			getDisabled: $.proxy(this.getDisabled, this),
			getEnabled: $.proxy(this.getEnabled, this),

			// Tree manipulation methods
			addNode: $.proxy(this.addNode, this),
			addNodeAfter: $.proxy(this.addNodeAfter, this),
			addNodeBefore: $.proxy(this.addNodeBefore, this),
			removeNode: $.proxy(this.removeNode, this),
			updateNode: $.proxy(this.updateNode, this),

			// Select methods
			selectNode: $.proxy(this.selectNode, this),
			unselectNode: $.proxy(this.unselectNode, this),
			toggleNodeSelected: $.proxy(this.toggleNodeSelected, this),

			// Expand / collapse methods
			collapseAll: $.proxy(this.collapseAll, this),
			collapseNode: $.proxy(this.collapseNode, this),
			expandAll: $.proxy(this.expandAll, this),
			expandNode: $.proxy(this.expandNode, this),
			toggleNodeExpanded: $.proxy(this.toggleNodeExpanded, this),
			revealNode: $.proxy(this.revealNode, this),

			// Check / uncheck methods
			checkAll: $.proxy(this.checkAll, this),
			checkNode: $.proxy(this.checkNode, this),
			uncheckAll: $.proxy(this.uncheckAll, this),
			uncheckNode: $.proxy(this.uncheckNode, this),
			toggleNodeChecked: $.proxy(this.toggleNodeChecked, this),
			unmarkCheckboxChanges: $.proxy(this.unmarkCheckboxChanges, this),

			// Disable / enable methods
			disableAll: $.proxy(this.disableAll, this),
			disableNode: $.proxy(this.disableNode, this),
			enableAll: $.proxy(this.enableAll, this),
			enableNode: $.proxy(this.enableNode, this),
			toggleNodeDisabled: $.proxy(this.toggleNodeDisabled, this),

			// Search methods
			search: $.proxy(this.search, this),
			clearSearch: $.proxy(this.clearSearch, this)
		};
	};

	Tree.prototype._init = function (options) {
		this._tree = [];
		this._initialized = false;

		this._options = $.extend({}, _default.settings, options);

		// Cache empty icon DOM template
		this._template.icon.empty.addClass(this._options.emptyIcon);

		this._destroy();
		this._subscribeEvents();

		this._triggerEvent('loading', null, _default.options);
		this._load(options)
			.then($.proxy(function (data) {
				// load done
				return this._tree = $.extend(true, [], data);
			}, this), $.proxy(function (error) {
				// load fail
				this._triggerEvent('loadingFailed', error, _default.options);
			}, this))
			.then($.proxy(function (treeData) {
				// initialize data
				return this._setInitialStates({ nodes: treeData }, 0);
			}, this))
			.then($.proxy(function () {
				// render to DOM
				this._render();
			}, this));
	};

	Tree.prototype._load = function (options) {
		var done = new $.Deferred();
		if (options.data) {
			this._loadLocalData(options, done);
		} else if (options.dataUrl) {
			this._loadRemoteData(options, done);
		}
		return done.promise();
	};

	Tree.prototype._loadRemoteData = function (options, done) {
		$.ajax($.extend(true, {}, _default.dataUrl, options.dataUrl))
			.done(function (data) {
				done.resolve(data);
			})
			.fail(function (xhr, status, error) {
				done.reject(error);
			});
	};

	Tree.prototype._loadLocalData = function (options, done) {
		done.resolve((typeof options.data === 'string') ?
								JSON.parse(options.data) :
								$.extend(true, [], options.data));
	};

	Tree.prototype._remove = function () {
		this._destroy();
		$.removeData(this, pluginName);
		$('#' + this._styleId).remove();
	};

	Tree.prototype._destroy = function () {
		if (!this._initialized) return;
		this._initialized = false;

		this._triggerEvent('destroyed', null, _default.options);

		// Switch off events
		this._unsubscribeEvents();

		// Tear down
		this.$wrapper.remove();
		this.$wrapper = null;
	};

	Tree.prototype._unsubscribeEvents = function () {
		this.$element.off('loading');
		this.$element.off('loadingFailed');
		this.$element.off('initialized');
		this.$element.off('nodeRendered');
		this.$element.off('rendered');
		this.$element.off('destroyed');
		this.$element.off('click');
		this.$element.off('nodeChecked');
		this.$element.off('nodeCollapsed');
		this.$element.off('nodeDisabled');
		this.$element.off('nodeEnabled');
		this.$element.off('nodeExpanded');
		this.$element.off('nodeChanged');
		this.$element.off('nodeSelected');
		this.$element.off('nodeUnchecked');
		this.$element.off('nodeUnselected');
		this.$element.off('searchComplete');
		this.$element.off('searchCleared');
	};

	Tree.prototype._subscribeEvents = function () {
		this._unsubscribeEvents();

		if (typeof (this._options.onLoading) === 'function') {
			this.$element.on('loading', this._options.onLoading);
		}

		if (typeof (this._options.onLoadingFailed) === 'function') {
			this.$element.on('loadingFailed', this._options.onLoadingFailed);
		}

		if (typeof (this._options.onInitialized) === 'function') {
			this.$element.on('initialized', this._options.onInitialized);
		}

		if (typeof (this._options.onNodeRendered) === 'function') {
			this.$element.on('nodeRendered', this._options.onNodeRendered);
		}

		if (typeof (this._options.onRendered) === 'function') {
			this.$element.on('rendered', this._options.onRendered);
		}

		if (typeof (this._options.onDestroyed) === 'function') {
			this.$element.on('destroyed', this._options.onDestroyed);
		}

		this.$element.on('click', $.proxy(this._clickHandler, this));

		if (typeof (this._options.onNodeChecked) === 'function') {
			this.$element.on('nodeChecked', this._options.onNodeChecked);
		}

		if (typeof (this._options.onNodeCollapsed) === 'function') {
			this.$element.on('nodeCollapsed', this._options.onNodeCollapsed);
		}

		if (typeof (this._options.onNodeDisabled) === 'function') {
			this.$element.on('nodeDisabled', this._options.onNodeDisabled);
		}

		if (typeof (this._options.onNodeEnabled) === 'function') {
			this.$element.on('nodeEnabled', this._options.onNodeEnabled);
		}

		if (typeof (this._options.onNodeExpanded) === 'function') {
			this.$element.on('nodeExpanded', this._options.onNodeExpanded);
		}

		if (typeof (this._options.onNodeChanged) === 'function') {
			this.$element.on('nodeChanged', this._options.onNodeChanged);
		}

		if (typeof (this._options.onNodeSelected) === 'function') {
			this.$element.on('nodeSelected', this._options.onNodeSelected);
		}

		if (typeof (this._options.onNodeUnchecked) === 'function') {
			this.$element.on('nodeUnchecked', this._options.onNodeUnchecked);
		}

		if (typeof (this._options.onNodeUnselected) === 'function') {
			this.$element.on('nodeUnselected', this._options.onNodeUnselected);
		}

		if (typeof (this._options.onSearchComplete) === 'function') {
			this.$element.on('searchComplete', this._options.onSearchComplete);
		}

		if (typeof (this._options.onSearchCleared) === 'function') {
			this.$element.on('searchCleared', this._options.onSearchCleared);
		}
	};

	Tree.prototype._triggerEvent = function (event, data, options) {
		if (options && !options.silent) {
			this.$element.trigger(event, $.extend(true, {}, data));
		}
	}

	/*
		Recurse the tree structure and ensure all nodes have
		valid initial states.  User defined states will be preserved.
		For performance we also take this opportunity to
		index nodes in a flattened ordered structure
	*/
	Tree.prototype._setInitialStates = function (node, level) {
		this._nodes = {};
		return $.when.apply(this, this._setInitialState(node, level))
			.done($.proxy(function () {
				this._orderedNodes = this._sortNodes();
				this._inheritCheckboxChanges();
				this._triggerEvent('initialized', this._orderedNodes, _default.options);
				return;
			}, this));
	};

	Tree.prototype._setInitialState = function (node, level, done) {
		if (!node.nodes) return;
		level += 1;
		done = done || [];

		var parent = node;
		$.each(node.nodes, $.proxy(function (index, node) {
			var deferred = new $.Deferred();
			done.push(deferred.promise());

			// level : hierarchical tree level, starts at 1
			node.level = level;

			// index : relative to siblings
			node.index = index;

			// nodeId : unique, hierarchical identifier
			node.nodeId = (parent && parent.nodeId) ?
											parent.nodeId + '.' + node.index :
											(level - 1) + '.' + node.index;

			// parentId : transversing up the tree
			node.parentId = parent.nodeId;

			// if not provided set selectable default value
			if (!node.hasOwnProperty('selectable')) {
				node.selectable = true;
			}

			// if not provided set checkable default value
			if (!node.hasOwnProperty('checkable')) {
				node.checkable = true;
			}

			// where provided we should preserve states
			node.state = node.state || {};

			// set checked state; unless set always false
			if (!node.state.hasOwnProperty('checked')) {
				node.state.checked = false;
			}

			// convert the undefined string if hierarchical checks are enabled
			if (this._options.hierarchicalCheck && node.state.checked === 'undefined') {
				node.state.checked = undefined;
			}

			// set enabled state; unless set always false
			if (!node.state.hasOwnProperty('disabled')) {
				node.state.disabled = false;
			}

			// set expanded state; if not provided based on levels
			if (!node.state.hasOwnProperty('expanded')) {
				if (!node.state.disabled &&
						(level < this._options.levels) &&
						(node.nodes && node.nodes.length > 0)) {
					node.state.expanded = true;
				}
				else {
					node.state.expanded = false;
				}
			}

			// set selected state; unless set always false
			if (!node.state.hasOwnProperty('selected')) {
				node.state.selected = false;
			}

			// set visible state; based parent state plus levels
			if ((parent && parent.state && parent.state.expanded) ||
					(level <= this._options.levels)) {
				node.state.visible = true;
			}
			else {
				node.state.visible = false;
			}

			// recurse child nodes and transverse the tree, depth-first
			if (node.nodes) {
				if (node.nodes.length > 0) {
					this._setInitialState(node, level, done);
				}
				else {
					delete node.nodes;
				}
			}

			// add / update indexed collection
			this._nodes[node.nodeId] = node;

			// mark task as complete
			deferred.resolve();
		}, this));

		return done;
	};

	Tree.prototype._sortNodes = function () {
		return $.map(Object.keys(this._nodes).sort(function (a, b) {
			if (a === b) return 0;
			var a = a.split('.').map(function (level) { return parseInt(level); });
			var b = b.split('.').map(function (level) { return parseInt(level); });

			var c = Math.max(a.length, b.length);
			for (var i=0; i<c; i++) {
				if (a[i] === undefined) return -1;
				if (b[i] === undefined) return +1;
				if (a[i] - b[i] > 0) return +1;
				if (a[i] - b[i] < 0) return -1;
			};

		}), $.proxy(function (value, index) {
		  return this._nodes[value];
		}, this));
	};

	Tree.prototype._clickHandler = function (event) {

		var target = $(event.target);
		var node = this.targetNode(target);
		if (!node || node.state.disabled) return;

		var classList = target.attr('class') ? target.attr('class').split(' ') : [];
		if ((classList.indexOf('expand-icon') !== -1)) {
			this._toggleExpanded(node, $.extend({}, _default.options));
		}
		else if ((classList.indexOf('check-icon') !== -1)) {
			if (node.checkable) {
				this._toggleChecked(node, $.extend({}, _default.options));
			}
		}
		else {
			if (node.selectable) {
				this._toggleSelected(node, $.extend({}, _default.options));
			} else {
				this._toggleExpanded(node, $.extend({}, _default.options));
			}
		}
	};

	// Looks up the DOM for the closest parent list item to retrieve the
	// data attribute nodeid, which is used to lookup the node in the flattened structure.
	Tree.prototype.targetNode = function (target) {
		var nodeId = target.closest('li.list-group-item').attr('data-nodeId');
		var node = this._nodes[nodeId];
		if (!node) {
			console.log('Error: node does not exist');
		}
		return node;
	};

	Tree.prototype._toggleExpanded = function (node, options) {
		if (!node) return;

		// Lazy-load the child nodes if possible
		if (typeof(this._options.lazyLoad) === 'function' && node.lazyLoad) {
			this._lazyLoad(node);
		} else {
			this._setExpanded(node, !node.state.expanded, options);
		}
	};

	Tree.prototype._lazyLoad = function (node) {
		// Show a different icon while loading the child nodes
		node.$el.children('span.expand-icon')
			.removeClass(this._options.expandIcon)
			.addClass(this._options.loadingIcon);

		var _this = this;
		this._options.lazyLoad(node, function (nodes) {
			// Adding the node will expand its parent automatically
			_this.addNode(nodes, node);
		});
		// Only the first expand should do a lazy-load
		delete node.lazyLoad;
	};

	Tree.prototype._setExpanded = function (node, state, options) {

		// We never pass options when rendering, so the only time
		// we need to validate state is from user interaction
		if (options && state === node.state.expanded) return;

		if (state && node.nodes) {

			// Set node state
			node.state.expanded = true;

			// Set element
			if (node.$el) {
				node.$el.children('span.expand-icon')
					.removeClass(this._options.expandIcon)
					.removeClass(this._options.loadingIcon)
					.addClass(this._options.collapseIcon);
			}

			// Expand children
			if (node.nodes && options) {
				$.each(node.nodes, $.proxy(function (index, node) {
					this._setVisible(node, true, options);
				}, this));
			}

			// Optionally trigger event
			this._triggerEvent('nodeExpanded', node, options);
		}
		else if (!state) {

			// Set node state
			node.state.expanded = false;

			// Set element
			if (node.$el) {
				node.$el.children('span.expand-icon')
					.removeClass(this._options.collapseIcon)
					.addClass(this._options.expandIcon);
			}

			// Collapse children
			if (node.nodes && options) {
				$.each(node.nodes, $.proxy(function (index, node) {
					this._setVisible(node, false, options);
					this._setExpanded(node, false, options);
				}, this));
			}

			// Optionally trigger event
			this._triggerEvent('nodeCollapsed', node, options);
		}
	};

	Tree.prototype._setVisible = function (node, state, options) {

		if (options && state === node.state.visible) return;

		if (state) {

			// Set node state
			node.state.visible = true;

			// Set element
			if (node.$el) {
				node.$el.removeClass('node-hidden');
			}
		}
		else {

			// Set node state to unchecked
			node.state.visible = false;

			// Set element
			if (node.$el) {
				node.$el.addClass('node-hidden');
			}
		}
	};

	Tree.prototype._toggleSelected = function (node, options) {
		if (!node) return;
		this._setSelected(node, !node.state.selected, options);
		return this;
	};

	Tree.prototype._setSelected = function (node, state, options, fired) {

		// We never pass options when rendering, so the only time
		// we need to validate state is from user interaction
		if (options && (state === node.state.selected)) return;

		if (state) {

			// If multiSelect false, unselect previously selected
			if (!this._options.multiSelect) {
				$.each(this._findNodes('true', 'state.selected'), $.proxy(function (index, node) {
					this._setSelected(node, false, $.extend(options, {unselecting: true}), true);
				}, this));
			}

			// Set node state
			node.state.selected = true;

			// Set element
			if (node.$el) {
				node.$el.addClass('node-selected');

				if (node.selectedIcon || this._options.selectedIcon) {
					node.$el.children('span.node-icon')
						.removeClass(node.icon || this._options.nodeIcon)
						.addClass(node.selectedIcon || this._options.selectedIcon);
				}
			}

			// Optionally trigger event
			this._triggerEvent('nodeSelected', node, options);
			this._triggerEvent('nodeChanged', node, options);
		}
		else {

			// If preventUnselect true + only one remaining selection, disable unselect
			if (this._options.preventUnselect &&
					(options && !options.unselecting) &&
					(this._findNodes('true', 'state.selected').length === 1)) {
				// Fire the nodeSelected event if reselection is allowed
				if (this._options.allowReselect) {
					this._triggerEvent('nodeSelected', node, options);
					this._triggerEvent('nodeChanged', node, options);
				}
				return this;
			}

			// Set node state
			node.state.selected = false;

			// Set element
			if (node.$el) {
				node.$el.removeClass('node-selected');

				if (node.selectedIcon || this._options.selectedIcon) {
					node.$el.children('span.node-icon')
						.removeClass(node.selectedIcon || this._options.selectedIcon)
						.addClass(node.icon || this._options.nodeIcon);
				}
			}

			// Optionally trigger event
			this._triggerEvent('nodeUnselected', node, options);
			if (!fired) {
				this._triggerEvent('nodeChanged', node, options);
			}
		}

		return this;
	};

	Tree.prototype._inheritCheckboxChanges = function () {
		if (this._options.showCheckbox && this._options.highlightChanges) {
			this._checkedNodes = $.grep(this._orderedNodes, function (node) {
				return node.state.checked;
			});
		}
	};

	Tree.prototype._toggleChecked = function (node, options) {
		if (!node) return;

		if (this._options.hierarchicalCheck) {
			// Event propagation to the parent/child nodes
			var childOptions = $.extend({}, options, {silent: options.silent || !this._options.propagateCheckEvent});

			var state, currentNode = node;
			// Temporarily swap the tree state
			node.state.checked = !node.state.checked;

			// Iterate through each parent node
			while (currentNode = this._nodes[currentNode.parentId]) {

				// Calculate the state
				state = currentNode.nodes.reduce(function (acc, curr) {
					return (acc === curr.state.checked) ? acc : undefined;
				}, currentNode.nodes[0].state.checked);

				// Set the state
				this._setChecked(currentNode, state, childOptions);
			}

			if (node.nodes && node.nodes.length > 0) {
				// Copy the content of the array
				var child, children = node.nodes.slice();
				// Iterate through each child node
				while (children && children.length > 0) {
					child = children.pop();

					// Set the state
					this._setChecked(child, node.state.checked, childOptions);

					// Append children to the end of the list
					if (child.nodes && child.nodes.length > 0) {
						children = children.concat(child.nodes.slice());
					}
				}
			}
			// Swap back the tree state
			node.state.checked = !node.state.checked;
		}

		this._setChecked(node, !node.state.checked, options);
	};

	Tree.prototype._setChecked = function (node, state, options) {

		// We never pass options when rendering, so the only time
		// we need to validate state is from user interaction
		if (options && state === node.state.checked) return;

		// Highlight the node if its checkbox has unsaved changes
		if (this._options.highlightChanges) {
			node.$el.toggleClass('node-check-changed', (this._checkedNodes.indexOf(node) == -1) == state);
		}

		if (state) {

			// Set node state
			node.state.checked = true;

			// Set element
			if (node.$el) {
				node.$el.addClass('node-checked').removeClass('node-checked-partial');
				node.$el.children('span.check-icon')
					.removeClass(this._options.uncheckedIcon)
					.removeClass(this._options.partiallyCheckedIcon)
					.addClass(this._options.checkedIcon);
			}

			// Optionally trigger event
			this._triggerEvent('nodeChecked', node, options);
		}
		else if (state === undefined && this._options.hierarchicalCheck) {

			// Set node state to partially checked
			node.state.checked = undefined;

			// Set element
			if (node.$el) {
				node.$el.addClass('node-checked-partial').removeClass('node-checked');
				node.$el.children('span.check-icon')
					.removeClass(this._options.uncheckedIcon)
					.removeClass(this._options.checkedIcon)
					.addClass(this._options.partiallyCheckedIcon);
			}

			// Optionally trigger event, partially checked is technically unchecked
			this._triggerEvent('nodeUnchecked', node, options);
		} else {

			// Set node state to unchecked
			node.state.checked = false;

			// Set element
			if (node.$el) {
				node.$el.removeClass('node-checked node-checked-partial');
				node.$el.children('span.check-icon')
					.removeClass(this._options.checkedIcon)
					.removeClass(this._options.partiallyCheckedIcon)
					.addClass(this._options.uncheckedIcon);
			}

			// Optionally trigger event
			this._triggerEvent('nodeUnchecked', node, options);
		}
	};

	Tree.prototype._setDisabled = function (node, state, options) {

		// We never pass options when rendering, so the only time
		// we need to validate state is from user interaction
		if (options && state === node.state.disabled) return;

		if (state) {

			// Set node state to disabled
			node.state.disabled = true;

			// Disable all other states
			if (options && !options.keepState) {
				this._setSelected(node, false, options);
				this._setChecked(node, false, options);
				this._setExpanded(node, false, options);
			}

			// Set element
			if (node.$el) {
				node.$el.addClass('node-disabled');
			}

			// Optionally trigger event
			this._triggerEvent('nodeDisabled', node, options);
		}
		else {

			// Set node state to enabled
			node.state.disabled = false;

			// Set element
			if (node.$el) {
				node.$el.removeClass('node-disabled');
			}

			// Optionally trigger event
			this._triggerEvent('nodeEnabled', node, options);
		}
	};

	Tree.prototype._setSearchResult = function (node, state, options) {
		if (options && state === node.searchResult) return;

		if (state) {

			node.searchResult = true;

			if (node.$el) {
				node.$el.addClass('node-result');
			}
		}
		else {

			node.searchResult = false;

			if (node.$el) {
				node.$el.removeClass('node-result');
			}
		}
	};

	Tree.prototype._render = function () {
		if (!this._initialized) {

			// Setup first time only components
			this.$wrapper = this._template.tree.clone();
			this.$element.empty()
				.addClass(pluginName)
				.append(this.$wrapper);

			this._injectStyle();

			this._initialized = true;
		}

		var previousNode;
		$.each(this._orderedNodes, $.proxy(function (id, node) {
			this._renderNode(node, previousNode);
			previousNode = node;
		}, this));

		this._triggerEvent('rendered', this._orderedNodes, _default.options);
	};

	Tree.prototype._renderNode = function (node, previousNode) {
		if (!node) return;

		if (!node.$el) {
			node.$el = this._newNodeEl(node, previousNode)
				.addClass('node-' + this._elementId);
		}
		else {
			node.$el.empty();
		}

		// Append .classes to the node
		node.$el.addClass(node.class);

		// Set the #id of the node if specified
		if (node.id) {
			node.$el.attr('id', node.id);
		}

		// Append custom data- attributes to the node
		if (node.dataAttr) {
			$.each(node.dataAttr, function (key, value) {
				node.$el.attr('data-' + key, value);
			});
		}

		// Set / update nodeid; it can change as a result of addNode etc.
		node.$el.attr('data-nodeId', node.nodeId);

		// Set the tooltip attribute if present
		if (node.tooltip) {
			node.$el.attr('title', node.tooltip);
		}

		// Add indent/spacer to mimic tree structure
		for (var i = 0; i < (node.level - 1); i++) {
			node.$el.append(this._template.indent.clone());
		}

		// Add expand / collapse or empty spacer icons
		node.$el
			.append(
				node.nodes || node.lazyLoad ? this._template.icon.expand.clone() : this._template.icon.empty.clone()
			);

		// Add checkbox and node icons
		if (this._options.checkboxFirst) {
			this._addCheckbox(node);
			this._addIcon(node);
			this._addImage(node);
		} else {
			this._addIcon(node);
			this._addImage(node);
			this._addCheckbox(node);
		}

		// Add text
		if (this._options.wrapNodeText) {
			var wrapper = this._template.text.clone();
			node.$el.append(wrapper);
			wrapper.append(node.text);
		} else {
			node.$el.append(node.text);
		}

		// Add tags as badges
		if (this._options.showTags && node.tags) {
			$.each(node.tags, $.proxy(function addTag(id, tag) {
				node.$el
					.append(this._template.badge.clone()
						.addClass(
							(typeof tag === 'object' ? tag.class : undefined)
							|| node.tagsClass
							|| this._options.tagsClass
						)
						.append(
							(typeof tag === 'object' ? tag.text : undefined)
							|| tag
						)
					);
			}, this));
		}

		// Set various node states
		this._setSelected(node, node.state.selected);
		this._setChecked(node, node.state.checked);
		this._setSearchResult(node, node.searchResult);
		this._setExpanded(node, node.state.expanded);
		this._setDisabled(node, node.state.disabled);
		this._setVisible(node, node.state.visible);

		// Trigger nodeRendered event
		this._triggerEvent('nodeRendered', node, _default.options);
	};

	// Add checkable icon
	Tree.prototype._addCheckbox = function (node) {
		if (this._options.showCheckbox && (node.hideCheckbox === undefined || node.hideCheckbox === false)) {
			node.$el
				.append(this._template.icon.check.clone());
		}
	}

	// Add node icon
	Tree.prototype._addIcon = function (node) {
		if (this._options.showIcon && !(this._options.showImage && node.image)) {
			var icon = this._template.icon.node.clone().addClass(node.icon || this._options.nodeIcon);
			if (node.iconColor) {
				icon.css('color', node.iconColor);
			}
			if (node.iconBackground) {
				icon.addClass('node-icon-background');
				icon.css('background', node.iconBackground);
			}

			node.$el.append(icon);
		}
	}

	Tree.prototype._addImage = function (node) {
 		if (this._options.showImage && node.image) {
 			node.$el
 				.append(this._template.image.clone()
 					.addClass('node-image')
 					.css('background-image', "url('" + node.image + "')")
 				);
 		}
 	}

	// Creates a new node element from template and
	// ensures the template is inserted at the correct position
	Tree.prototype._newNodeEl = function (node, previousNode) {
		var $el = this._template.node.clone();

		if (previousNode) {
			// typical usage, as nodes are rendered in
			// sort order we add after the previous element
			previousNode.$el.after($el);
		} else {
			// we use prepend instead of append,
			// to cater for root inserts i.e. nodeId 0.0
			this.$wrapper.prepend($el);
		}

		return $el;
	};

	// Recursively remove node elements from DOM
	Tree.prototype._removeNodeEl = function (node) {
		if (!node) return;

		if (node.nodes) {
			$.each(node.nodes, $.proxy(function (index, node) {
				this._removeNodeEl(node);
			}, this));
		}
		node.$el.remove();
	};

	// Expand node, rendering it's immediate children
	Tree.prototype._expandNode = function (node) {
		if (!node.nodes) return;

		$.each(node.nodes.slice(0).reverse(), $.proxy(function (index, childNode) {
			childNode.level = node.level + 1;
			this._renderNode(childNode, node.$el);
		}, this));
	};

	// Add inline style into head
	Tree.prototype._injectStyle = function () {
		if (this._options.injectStyle && !document.getElementById(this._styleId)) {
			$('<style type="text/css" id="' + this._styleId + '"> ' + this._buildStyle() + ' </style>').appendTo('head');
		}
	};

	// Construct trees style based on user options
	Tree.prototype._buildStyle = function () {
		var style = '.node-' + this._elementId + '{';

		// Basic bootstrap style overrides
		if (this._options.color) {
			style += 'color:' + this._options.color + ';';
		}

		if (this._options.backColor) {
			style += 'background-color:' + this._options.backColor + ';';
		}

		if (!this._options.showBorder) {
			style += 'border:none;';
		}
		else if (this._options.borderColor) {
			style += 'border:1px solid ' + this._options.borderColor + ';';
		}
		style += '}';

		if (this._options.onhoverColor) {
			style += '.node-' + this._elementId + ':not(.node-disabled):hover{' +
				'background-color:' + this._options.onhoverColor + ';' +
			'}';
		}

		// Style search results
		if (this._options.highlightSearchResults && (this._options.searchResultColor || this._options.searchResultBackColor)) {

			var innerStyle = ''
			if (this._options.searchResultColor) {
				innerStyle += 'color:' + this._options.searchResultColor + ';';
			}
			if (this._options.searchResultBackColor) {
				innerStyle += 'background-color:' + this._options.searchResultBackColor + ';';
			}

			style += '.node-' + this._elementId + '.node-result{' + innerStyle + '}';
			style += '.node-' + this._elementId + '.node-result:hover{' + innerStyle + '}';
		}

		// Style selected nodes
		if (this._options.highlightSelected && (this._options.selectedColor || this._options.selectedBackColor)) {

			var innerStyle = ''
			if (this._options.selectedColor) {
				innerStyle += 'color:' + this._options.selectedColor + ';';
			}
			if (this._options.selectedBackColor) {
				innerStyle += 'background-color:' + this._options.selectedBackColor + ';';
			}

			style += '.node-' + this._elementId + '.node-selected{' + innerStyle + '}';
			style += '.node-' + this._elementId + '.node-selected:hover{' + innerStyle + '}';
		}

		// Style changed nodes
		if (this._options.highlightChanges) {
			var innerStyle = 'color: ' + this._options.changedNodeColor + ';';
			style += '.node-' + this._elementId + '.node-check-changed{' + innerStyle + '}';
		}

		// Node level style overrides
		$.each(this._orderedNodes, $.proxy(function (index, node) {
			if (node.color || node.backColor) {
				var innerStyle = '';
				if (node.color) {
					innerStyle += 'color:' + node.color + ';';
				}
				if (node.backColor) {
					innerStyle += 'background-color:' + node.backColor + ';';
				}
				style += '.node-' + this._elementId + '[data-nodeId="' + node.nodeId + '"]{' + innerStyle + '}';
			}
		}, this));

		return this._css + style;
	};

	Tree.prototype._template = {
		tree: $('<ul class="list-group"></ul>'),
		node: $('<li class="list-group-item"></li>'),
		indent: $('<span class="indent"></span>'),
		icon: {
			node: $('<span class="icon node-icon"></span>'),
			expand: $('<span class="icon expand-icon"></span>'),
			check: $('<span class="icon check-icon"></span>'),
			empty: $('<span class="icon"></span>')
		},
		image: $('<span class="image"></span>'),
		badge: $('<span></span>'),
		text: $('<span class="text"></span>')
	};

	Tree.prototype._css = '.treeview .list-group-item{cursor:pointer}.treeview span.indent{margin-left:10px;margin-right:10px}.treeview span.icon{width:12px;margin-right:5px}.treeview .node-disabled{color:silver;cursor:not-allowed}'


	/**
		Returns an array of matching node objects.
		@param {String} pattern - A pattern to match against a given field
		@return {String} field - Field to query pattern against
	*/
	Tree.prototype.findNodes = function (pattern, field, modifier) {
		return this._findNodes(pattern, field, modifier);
	};


	/**
		Returns an ordered aarray of node objects.
		@return {Array} nodes - An array of all nodes
	*/
	Tree.prototype.getNodes = function () {
		return this._orderedNodes;
	};

	/**
		Returns parent nodes for given nodes, if valid otherwise returns undefined.
		@param {Array} nodes - An array of nodes
		@returns {Array} nodes - An array of parent nodes
	*/
	Tree.prototype.getParents = function (nodes) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		var parentNodes = [];
		$.each(nodes, $.proxy(function (index, node) {
			var parentNode = node.parentId ? this._nodes[node.parentId] : false;
			if (parentNode) {
				parentNodes.push(parentNode);
			}
		}, this));
		return parentNodes;
	};

	/**
		Returns an array of sibling nodes for given nodes, if valid otherwise returns undefined.
		@param {Array} nodes - An array of nodes
		@returns {Array} nodes - An array of sibling nodes
	*/
	Tree.prototype.getSiblings = function (nodes) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		var siblingNodes = [];
		$.each(nodes, $.proxy(function (index, node) {
			var parent = this.getParents([node]);
			var nodes = parent[0] ? parent[0].nodes : this._tree;
			siblingNodes = nodes.filter(function (obj) {
				return obj.nodeId !== node.nodeId;
			});
		}, this));

		// flatten possible nested array before returning
		return $.map(siblingNodes, function (obj) {
			return obj;
		});
	};

	/**
		Returns an array of selected nodes.
		@returns {Array} nodes - Selected nodes
	*/
	Tree.prototype.getSelected = function () {
		return this._findNodes('true', 'state.selected');
	};

	/**
		Returns an array of unselected nodes.
		@returns {Array} nodes - Unselected nodes
	*/
	Tree.prototype.getUnselected = function () {
		return this._findNodes('false', 'state.selected');
	};

	/**
		Returns an array of expanded nodes.
		@returns {Array} nodes - Expanded nodes
	*/
	Tree.prototype.getExpanded = function () {
		return this._findNodes('true', 'state.expanded');
	};

	/**
		Returns an array of collapsed nodes.
		@returns {Array} nodes - Collapsed nodes
	*/
	Tree.prototype.getCollapsed = function () {
		return this._findNodes('false', 'state.expanded');
	};

	/**
		Returns an array of checked nodes.
		@returns {Array} nodes - Checked nodes
	*/
	Tree.prototype.getChecked = function () {
		return this._findNodes('true', 'state.checked');
	};

	/**
		Returns an array of unchecked nodes.
		@returns {Array} nodes - Unchecked nodes
	*/
	Tree.prototype.getUnchecked = function () {
		return this._findNodes('false', 'state.checked');
	};

	/**
		Returns an array of disabled nodes.
		@returns {Array} nodes - Disabled nodes
	*/
	Tree.prototype.getDisabled = function () {
		return this._findNodes('true', 'state.disabled');
	};

	/**
		Returns an array of enabled nodes.
		@returns {Array} nodes - Enabled nodes
	*/
	Tree.prototype.getEnabled = function () {
		return this._findNodes('false', 'state.disabled');
	};


	/**
	 	Add nodes to the tree.
		@param {Array} nodes  - An array of nodes to add
		@param {optional Object} parentNode  - The node to which nodes will be added as children
		@param {optional number} index  - Zero based insert index
		@param {optional Object} options
	*/
	Tree.prototype.addNode = function (nodes, parentNode, index, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		if (parentNode instanceof Array) {
			parentNode = parentNode[0];
		}

		options = $.extend({}, _default.options, options);

		// identify target nodes; either the tree's root or a parent's child nodes
		var targetNodes;
		if (parentNode && parentNode.nodes) {
			targetNodes = parentNode.nodes;
		} else if (parentNode) {
			targetNodes = parentNode.nodes = [];
		} else {
			targetNodes = this._tree;
		}

		// inserting nodes at specified positions
		$.each(nodes, $.proxy(function (i, node) {
			var insertIndex = (typeof(index) === 'number') ? (index + i) : (targetNodes.length + 1);
			targetNodes.splice(insertIndex, 0, node);
		}, this));

		// initialize new state and render changes
		this._setInitialStates({nodes: this._tree}, 0)
			.done($.proxy(function () {
				if (parentNode && !parentNode.state.expanded) {
					this._setExpanded(parentNode, true, options);
				}
				this._render();
			}, this));
	}

	/**
	 	Add nodes to the tree after given node.
		@param {Array} nodes  - An array of nodes to add
		@param {Object} node  - The node to which nodes will be added after
		@param {optional Object} options
	*/
	Tree.prototype.addNodeAfter = function (nodes, node, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		if (node instanceof Array) {
			node = node[0];
		}

		options = $.extend({}, _default.options, options);

		this.addNode(nodes, this.getParents(node)[0], (node.index + 1), options);
	}

	/**
	 	Add nodes to the tree before given node.
		@param {Array} nodes  - An array of nodes to add
		@param {Object} node  - The node to which nodes will be added before
		@param {optional Object} options
	*/
	Tree.prototype.addNodeBefore = function (nodes, node, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		if (node instanceof Array) {
			node = node[0];
		}

		options = $.extend({}, _default.options, options);

		this.addNode(nodes, this.getParents(node)[0], node.index, options);
	}

	/**
	 	Removes given nodes from the tree.
		@param {Array} nodes  - An array of nodes to remove
		@param {optional Object} options
	*/
	Tree.prototype.removeNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		var targetNodes, parentNode;
		$.each(nodes, $.proxy(function (index, node) {

			// remove nodes from tree
			parentNode = this._nodes[node.parentId];
			if (parentNode) {
				targetNodes = parentNode.nodes;
			} else {
				targetNodes = this._tree;
			}
			targetNodes.splice(node.index, 1);

			// remove node from DOM
			this._removeNodeEl(node);
		}, this));

		// initialize new state and render changes
		this._setInitialStates({nodes: this._tree}, 0)
			.done(this._render.bind(this));
	};

	/**
	 	Updates / replaces a given tree node
		@param {Object} node  - A single node to be replaced
		@param {Object} newNode  - THe replacement node
		@param {optional Object} options
	*/
	Tree.prototype.updateNode = function (node, newNode, options) {
		if (node instanceof Array) {
			node = node[0];
		}

		options = $.extend({}, _default.options, options);

		// insert new node
		var targetNodes;
		var parentNode = this._nodes[node.parentId];
		if (parentNode) {
			targetNodes = parentNode.nodes;
		} else {
			targetNodes = this._tree;
		}
		targetNodes.splice(node.index, 1, newNode);

		// remove old node from DOM
		this._removeNodeEl(node);

		// initialize new state and render changes
		this._setInitialStates({nodes: this._tree}, 0)
			.done(this._render.bind(this));
	};


	/**
		Selects given tree nodes
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.selectNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setSelected(node, true, options);
		}, this));
	};

	/**
		Unselects given tree nodes
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.unselectNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setSelected(node, false, options);
		}, this));
	};

	/**
		Toggles a node selected state; selecting if unselected, unselecting if selected.
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.toggleNodeSelected = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._toggleSelected(node, options);
		}, this));
	};


	/**
		Collapse all tree nodes
		@param {optional Object} options
	*/
	Tree.prototype.collapseAll = function (options) {
		options = $.extend({}, _default.options, options);
		options.levels = options.levels || 999;
		this.collapseNode(this._tree, options);
	};

	/**
		Collapse a given tree node
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.collapseNode = function (nodes, options) {
		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setExpanded(node, false, options);
		}, this));
	};

	/**
		Expand all tree nodes
		@param {optional Object} options
	*/
	Tree.prototype.expandAll = function (options) {
		options = $.extend({}, _default.options, options);
		options.levels = options.levels || 999;
		this.expandNode(this._tree, options);
	};

	/**
		Expand given tree nodes
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.expandNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			// Do not re-expand already expanded nodes
			if (node.state.expanded) return;

			if (typeof(this._options.lazyLoad) === 'function' && node.lazyLoad) {
				this._lazyLoad(node);
			}

			this._setExpanded(node, true, options);
			if (node.nodes) {
				this._expandLevels(node.nodes, options.levels-1, options);
			}
		}, this));
	};

	Tree.prototype._expandLevels = function (nodes, level, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setExpanded(node, (level > 0) ? true : false, options);
			if (node.nodes) {
				this._expandLevels(node.nodes, level-1, options);
			}
		}, this));
	};

	/**
		Reveals given tree nodes, expanding the tree from node to root.
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.revealNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			var parentNode = node;
			var tmpNode;
			while (tmpNode = this.getParents([parentNode])[0]) {
				parentNode = tmpNode;
				this._setExpanded(parentNode, true, options);
			};
		}, this));
	};

	/**
		Toggles a node's expanded state; collapsing if expanded, expanding if collapsed.
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.toggleNodeExpanded = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._toggleExpanded(node, options);
		}, this));
	};


	/**
		Check all tree nodes
		@param {optional Object} options
	*/
	Tree.prototype.checkAll = function (options) {
		options = $.extend({}, _default.options, options);

		var nodes = $.grep(this._orderedNodes, function (node) {
			return !node.state.checked;
		});
		$.each(nodes, $.proxy(function (index, node) {
			this._setChecked(node, true, options);
		}, this));
	};

	/**
		Checks given tree nodes
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.checkNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setChecked(node, true, options);
		}, this));
	};

	/**
		Uncheck all tree nodes
		@param {optional Object} options
	*/
	Tree.prototype.uncheckAll = function (options) {
		options = $.extend({}, _default.options, options);

		var nodes = $.grep(this._orderedNodes, function (node) {
			return node.state.checked || node.state.checked === undefined;
		});
		$.each(nodes, $.proxy(function (index, node) {
			this._setChecked(node, false, options);
		}, this));
	};

	/**
		Uncheck given tree nodes
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.uncheckNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setChecked(node, false, options);
		}, this));
	};

	/**
		Toggles a node's checked state; checking if unchecked, unchecking if checked.
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.toggleNodeChecked = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._toggleChecked(node, options);
		}, this));
	};

	/**
		Saves the current state of checkboxes as default, cleaning up any highlighted changes
	*/
	Tree.prototype.unmarkCheckboxChanges = function () {
		this._inheritCheckboxChanges();

		$.each(this._nodes, function (index, node) {
			node.$el.removeClass('node-check-changed');
		});
	};

	/**
		Disable all tree nodes
		@param {optional Object} options
	*/
	Tree.prototype.disableAll = function (options) {
		options = $.extend({}, _default.options, options);

		var nodes = this._findNodes('false', 'state.disabled');
		$.each(nodes, $.proxy(function (index, node) {
			this._setDisabled(node, true, options);
		}, this));
	};

	/**
		Disable given tree nodes
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.disableNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setDisabled(node, true, options);
		}, this));
	};

	/**
		Enable all tree nodes
		@param {optional Object} options
	*/
	Tree.prototype.enableAll = function (options) {
		options = $.extend({}, _default.options, options);

		var nodes = this._findNodes('true', 'state.disabled');
		$.each(nodes, $.proxy(function (index, node) {
			this._setDisabled(node, false, options);
		}, this));
	};

	/**
		Enable given tree nodes
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.enableNode = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setDisabled(node, false, options);
		}, this));
	};

	/**
		Toggles a node's disabled state; disabling is enabled, enabling if disabled.
		@param {Array} nodes - An array of nodes
		@param {optional Object} options
	*/
	Tree.prototype.toggleNodeDisabled = function (nodes, options) {
		if (!(nodes instanceof Array)) {
			nodes = [nodes];
		}

		options = $.extend({}, _default.options, options);

		$.each(nodes, $.proxy(function (index, node) {
			this._setDisabled(node, !node.state.disabled, options);
		}, this));
	};


	/**
		Searches the tree for nodes (text) that match given criteria
		@param {String} pattern - A given string to match against
		@param {optional Object} options - Search criteria options
		@return {Array} nodes - Matching nodes
	*/
	Tree.prototype.search = function (pattern, options) {
		options = $.extend({}, _default.searchOptions, options);

		var previous = this._getSearchResults();
		var results = [];

		if (pattern && pattern.length > 0) {

			if (options.exactMatch) {
				pattern = '^' + pattern + '$';
			}

			var modifier = 'g';
			if (options.ignoreCase) {
				modifier += 'i';
			}

			results = this._findNodes(pattern, 'text', modifier);
		}

		// Clear previous results no longer matched
		$.each(this._diffArray(results, previous), $.proxy(function (index, node) {
			this._setSearchResult(node, false, options);
		}, this));

		// Set new results
		$.each(this._diffArray(previous, results), $.proxy(function (index, node) {
			this._setSearchResult(node, true, options);
		}, this));

		// Reveal hidden nodes
		if (results && options.revealResults) {
			this.revealNode(results);
		}

		this._triggerEvent('searchComplete', results, options);

		return results;
	};

	/**
		Clears previous search results
	*/
	Tree.prototype.clearSearch = function (options) {
		options = $.extend({}, { render: true }, options);

		var results = $.each(this._getSearchResults(), $.proxy(function (index, node) {
			this._setSearchResult(node, false, options);
		}, this));

		this._triggerEvent('searchCleared', results, options);
	};

	Tree.prototype._getSearchResults = function () {
		return this._findNodes('true', 'searchResult');
	};

	Tree.prototype._diffArray = function (a, b) {
		var diff = [];
		$.grep(b, function (n) {
			if ($.inArray(n, a) === -1) {
				diff.push(n);
			}
		});
		return diff;
	};

	/**
		Find nodes that match a given criteria
		@param {String} pattern - A given string to match against
		@param {optional String} attribute - Attribute to compare pattern against
		@param {optional String} modifier - Valid RegEx modifiers
		@return {Array} nodes - Nodes that match your criteria
	*/
	Tree.prototype._findNodes = function (pattern, attribute, modifier) {
		attribute = attribute || 'text';
		modifier = modifier || 'g';
		return $.grep(this._orderedNodes, $.proxy(function (node) {
			var val = this._getNodeValue(node, attribute);
			if (typeof val === 'string') {
				return val.match(new RegExp(pattern, modifier));
			}
		}, this));
	};

	/**
		Recursive find for retrieving nested attributes values
		All values are return as strings, unless invalid
		@param {Object} obj - Typically a node, could be any object
		@param {String} attr - Identifies an object property using dot notation
		@return {String} value - Matching attributes string representation
	*/
	Tree.prototype._getNodeValue = function (obj, attr) {
		var index = attr.indexOf('.');
		if (index > 0) {
			var _obj = obj[attr.substring(0, index)];
			var _attr = attr.substring(index + 1, attr.length);
			return this._getNodeValue(_obj, _attr);
		}
		else {
			if (obj.hasOwnProperty(attr) && obj[attr] !== undefined) {
				return obj[attr].toString();
			}
			else {
				return undefined;
			}
		}
	};

	var logError = function (message) {
		if (window.console) {
			window.console.error(message);
		}
	};

	// Prevent against multiple instantiations,
	// handle updates and method calls
	$.fn[pluginName] = function (options, args) {

		var result;
		if (this.length == 0) {
			throw "No element has been found!";
		}

		this.each(function () {
			var _this = $.data(this, pluginName);
			if (typeof options === 'string') {
				if (!_this) {
					logError('Not initialized, can not call method : ' + options);
				}
				else if (!$.isFunction(_this[options]) || options.charAt(0) === '_') {
					logError('No such method : ' + options);
				}
				else {
					if (!(args instanceof Array)) {
						args = [ args ];
					}
					result = _this[options].apply(_this, args);
				}
			}
			else if (typeof options === 'boolean') {
				result = _this;
			}
			else {
				$.data(this, pluginName, new Tree(this, $.extend(true, {}, options)));
			}
		});

		return result || this;
	};

})(jQuery, window, document);
