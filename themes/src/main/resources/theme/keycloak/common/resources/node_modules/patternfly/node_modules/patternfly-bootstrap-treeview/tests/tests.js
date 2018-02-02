/* global $, module, test, equal, ok */

;(function () {

	'use strict';

	function init(options) {
		return $('#treeview').treeview(options);
	}

	function getOptions(el) {
		return el.data().treeview.options;
	}

	var data = [
		{
			text: 'Parent 1',
			nodes: [
				{
					text: 'Child 1',
					nodes: [
						{
							text: 'Grandchild 1'
						},
						{
							text: 'Grandchild 2'
						}
					]
				},
				{
					text: 'Child 2'
				}
			]
		},
		{
			text: 'Parent 2'
		},
		{
			text: 'Parent 3'
		},
		{
			text: 'Parent 4'
		},
		{
			text: 'Parent 5'
		}
	];

	var singleNode = {
		text: 'Single 1'
	};

	var multiNodes = [
		{
			text: 'Multi 1'
		},
		{
			text: 'Multi 2'
		}
	];

	var json = '[' +
		'{' +
			'"text": "Parent 1",' +
			'"nodes": [' +
				'{' +
					'"text": "Child 1",' +
					'"nodes": [' +
						'{' +
							'"text": "Grandchild 1"' +
						'},' +
						'{' +
							'"text": "Grandchild 2"' +
						'}' +
					']' +
				'},' +
				'{' +
					'"text": "Child 2"' +
				'}' +
			']' +
		'},' +
		'{' +
			'"text": "Parent 2"' +
		'},' +
		'{' +
			'"text": "Parent 3"' +
		'},' +
		'{' +
			'"text": "Parent 4"' +
		'},' +
		'{' +
			'"text": "Parent 5"' +
		'}' +
	']';

	module('Options');

	test('Options setup', function () {
		// First test defaults option values
		var el = init(),
			options = getOptions(el);
		ok(options, 'Defaults created ok');
		equal(options.levels, 2, 'levels default ok');
		equal(options.expandIcon, 'glyphicon glyphicon-plus', 'expandIcon default ok');
		equal(options.collapseIcon, 'glyphicon glyphicon-minus', 'collapseIcon default ok');
		equal(options.emptyIcon, 'glyphicon', 'emptyIcon default ok');
		equal(options.nodeIcon, '', 'nodeIcon default ok');
		equal(options.selectedIcon, '', 'selectedIcon default ok');
		equal(options.checkedIcon, 'glyphicon glyphicon-check', 'checkedIcon default ok');
		equal(options.uncheckedIcon, 'glyphicon glyphicon-unchecked', 'uncheckedIcon default ok');
		equal(options.color, undefined, 'color default ok');
		equal(options.backColor, undefined, 'backColor default ok');
		equal(options.borderColor, undefined, 'borderColor default ok');
		equal(options.onhoverColor, '#F5F5F5', 'onhoverColor default ok');
		equal(options.selectedColor, '#FFFFFF', 'selectedColor default ok');
		equal(options.selectedBackColor, '#428bca', 'selectedBackColor default ok');
		equal(options.searchResultColor, '#D9534F', 'searchResultColor default ok');
		equal(options.searchResultBackColor, undefined, 'searchResultBackColor default ok');
		equal(options.highlightSelected, true, 'highlightSelected default ok');
		equal(options.highlightSearchResults, true, 'highlightSearchResults default ok');
		equal(options.showBorder, true, 'showBorder default ok');
		equal(options.showIcon, true, 'showIcon default ok');
		equal(options.showCheckbox, false, 'showCheckbox default ok');
		equal(options.showTags, false, 'showTags default ok');
		equal(options.multiSelect, false, 'multiSelect default ok');
		equal(options.preventUnselect, false, 'preventUnselect default ok');
		equal(options.onLoading, null, 'onLoading default ok');
		equal(options.onLoadingFailed, null, 'onLoadingFailed default ok');
		equal(options.onInitialized, null, 'onInitialized default ok');
		equal(options.onNodeRendered, null, 'onNodeRendered default ok');
		equal(options.onRendered, null, 'onRendered default ok');
		equal(options.onDestroyed, null, 'onDestroyed default ok');
		equal(options.onNodeChecked, null, 'onNodeChecked default ok');
		equal(options.onNodeCollapsed, null, 'onNodeCollapsed default ok');
		equal(options.onNodeDisabled, null, 'onNodeDisabled default ok');
		equal(options.onNodeEnabled, null, 'onNodeEnabled default ok');
		equal(options.onNodeExpanded, null, 'onNodeExpanded default ok');
		equal(options.onNodeSelected, null, 'onNodeSelected default ok');
		equal(options.onNodeUnchecked, null, 'onNodeUnchecked default ok');
		equal(options.onNodeUnselected, null, 'onNodeUnselected default ok');
		equal(options.onSearchComplete, null, 'onSearchComplete default ok');
		equal(options.onSearchCleared, null, 'onSearchCleared default ok');

		// Then test user options are correctly set
		var opts = {
			levels: 99,
			expandIcon: 'glyphicon glyphicon-expand',
			collapseIcon: 'glyphicon glyphicon-collapse',
			emptyIcon: 'glyphicon',
			nodeIcon: 'glyphicon glyphicon-stop',
			selectedIcon: 'glyphicon glyphicon-selected',
			checkedIcon: 'glyphicon glyphicon-checked-icon',
			uncheckedIcon: 'glyphicon glyphicon-unchecked-icon',
			color: 'yellow',
			backColor: 'purple',
			borderColor: 'purple',
			onhoverColor: 'orange',
			selectedColor: 'yellow',
			selectedBackColor: 'darkorange',
			searchResultColor: 'yellow',
			searchResultBackColor: 'darkorange',
			highlightSelected: false,
			highlightSearchResults: true,
			showBorder: false,
			showIcon: false,
			showCheckbox: true,
			showTags: true,
			multiSelect: true,
			preventUnselect: true,
			onLoading: function () {},
			onLoadingFailed: function () {},
			onInitialized: function () {},
			onNodeRendered: function () {},
			onRendered: function () {},
			onDestroyed: function () {},
			onNodeChecked: function () {},
			onNodeCollapsed: function () {},
			onNodeDisabled: function () {},
			onNodeEnabled: function () {},
			onNodeExpanded: function () {},
			onNodeSelected: function () {},
			onNodeUnchecked: function () {},
			onNodeUnselected: function () {},
			onSearchComplete: function () {},
			onSearchCleared: function () {}
		};

		options = getOptions(init(opts));
		ok(options, 'User options created ok');
		equal(options.levels, 99, 'levels set ok');
		equal(options.expandIcon, 'glyphicon glyphicon-expand', 'expandIcon set ok');
		equal(options.collapseIcon, 'glyphicon glyphicon-collapse', 'collapseIcon set ok');
		equal(options.emptyIcon, 'glyphicon', 'emptyIcon set ok');
		equal(options.nodeIcon, 'glyphicon glyphicon-stop', 'nodeIcon set ok');
		equal(options.selectedIcon, 'glyphicon glyphicon-selected', 'selectedIcon set ok');
		equal(options.checkedIcon, 'glyphicon glyphicon-checked-icon', 'checkedIcon set ok');
		equal(options.uncheckedIcon, 'glyphicon glyphicon-unchecked-icon', 'uncheckedIcon set ok');
		equal(options.color, 'yellow', 'color set ok');
		equal(options.backColor, 'purple', 'backColor set ok');
		equal(options.borderColor, 'purple', 'borderColor set ok');
		equal(options.onhoverColor, 'orange', 'onhoverColor set ok');
		equal(options.selectedColor, 'yellow', 'selectedColor set ok');
		equal(options.selectedBackColor, 'darkorange', 'selectedBackColor set ok');
		equal(options.searchResultColor, 'yellow', 'searchResultColor set ok');
		equal(options.searchResultBackColor, 'darkorange', 'searchResultBackColor set ok');
		equal(options.highlightSelected, false, 'highlightSelected set ok');
		equal(options.highlightSearchResults, true, 'highlightSearchResults set ok');
		equal(options.showBorder, false, 'showBorder set ok');
		equal(options.showIcon, false, 'showIcon set ok');
		equal(options.showCheckbox, true, 'showCheckbox set ok');
		equal(options.showTags, true, 'showTags set ok');
		equal(options.multiSelect, true, 'multiSelect set ok');
		equal(options.preventUnselect, true, 'preventUnselect set ok');
		equal(typeof options.onLoading, 'function', 'onLoading set ok');
		equal(typeof options.onLoadingFailed, 'function', 'onLoadingFailed set ok');
		equal(typeof options.onInitialized, 'function', 'onInitialized set ok');
		equal(typeof options.onNodeRendered, 'function', 'onNodeRendered set ok');
		equal(typeof options.onRendered, 'function', 'onRendered set ok');
		equal(typeof options.onDestroyed, 'function', 'onDestroyed set ok');
		equal(typeof options.onNodeChecked, 'function', 'onNodeChecked set ok');
		equal(typeof options.onNodeCollapsed, 'function', 'onNodeCollapsed set ok');
		equal(typeof options.onNodeDisabled, 'function', 'onNodeDisabled set ok');
		equal(typeof options.onNodeEnabled, 'function', 'onNodeEnabled set ok');
		equal(typeof options.onNodeExpanded, 'function', 'onNodeExpanded set ok');
		equal(typeof options.onNodeSelected, 'function', 'onNodeSelected set ok');
		equal(typeof options.onNodeUnchecked, 'function', 'onNodeUnchecked set ok');
		equal(typeof options.onNodeUnselected, 'function', 'onNodeUnselected set ok');
		equal(typeof options.onSearchComplete, 'function', 'onSearchComplete set ok');
		equal(typeof options.onSearchCleared, 'function', 'onSearchCleared set ok');
	});


	module('Data');

	test('Accepts local data', function () {
		var el = init({levels: 1, data: data});
		equal($(el.selector + ' ul li:not(.node-hidden)').length, 5, 'Correct number of root nodes');
	});

	test('Accepts local JSON', function () {
		var el = init({levels: 1, data: json});
		equal($(el.selector + ' ul li:not(.node-hidden)').length, 5, 'Correct number of root nodes');
	});

	asyncTest('Accepts remote JSON', function (assert) {
		var el = init({
			levels: 1,
			dataUrl: {url: 'data.json'},
			onRendered: function () {
				assert.equal($(el.selector + ' ul li:not(.node-hidden)').length, 5, 'Correct number of root nodes');
				start();
			}
		});
	});


	module('Behaviour');

	test('Is chainable', function () {
		var el = init();
		equal(el.addClass('test').attr('class'), 'test', 'Is chainable');
	});

	test('Correct initial levels shown', function () {

		var el = init({levels:1,data:data});
		equal($(el.selector + ' ul li:not(.node-hidden)').length, 5, 'Correctly display 5 root nodes when levels set to 1');

		el = init({levels:2,data:data});
		equal($(el.selector + ' ul li:not(.node-hidden)').length, 7, 'Correctly display 5 root and 2 child nodes when levels set to 2');

		el = init({levels:3,data:data});
		equal($(el.selector + ' ul li:not(.node-hidden)').length, 9, 'Correctly display 5 root, 2 children and 2 grand children nodes when levels set to 3');
	});

	test('Expanding a node', function () {

		var cbWorked, onWorked = false;
		var el = init({
			data: data,
			levels: 1,
			onNodeExpanded: function(/*event, date*/) {
				cbWorked = true;
			}
		})
		.on('nodeExpanded', function(/*event, date*/) {
			onWorked = true;
		});

		var nodeCount = $(el.selector + ' ul li:not(.node-hidden)').length;
		var firstNode = $('.expand-icon:first');
		firstNode.trigger('click');
		ok(($(el.selector + ' ul li:not(.node-hidden)').length > nodeCount), 'Number of nodes are increased, so node must have expanded');
		ok(cbWorked, 'onNodeExpanded function was called');
		ok(onWorked, 'nodeExpanded was fired');
	});

	test('Collapsing a node', function () {

		var cbWorked, onWorked = false;
		var el = init({
			data: data,
			levels: 2,
			onNodeCollapsed: function(/*event, date*/) {
				cbWorked = true;
			}
		})
		.on('nodeCollapsed', function(/*event, date*/) {
			onWorked = true;
		});

		var nodeCount = $(el.selector + ' ul li:not(.node-hidden)').length;
		var firstNode = $('.expand-icon:first');
		firstNode.trigger('click');
		ok(($(el.selector + ' ul li:not(.node-hidden)').length < nodeCount), 'Number of nodes has decreased, so node must have collapsed');
		ok(cbWorked, 'onNodeCollapsed function was called');
		ok(onWorked, 'nodeCollapsed was fired');
	});

	test('Selecting a node', function () {

		var cbWorked, onWorked = false;
		var $tree = init({
			data: data,
			onNodeSelected: function(/*event, date*/) {
				cbWorked = true;
			}
		})
		.on('nodeSelected', function(/*event, date*/) {
			onWorked = true;
		});
		var options = getOptions($tree);

		// Simulate click
		$('.list-group-item:first').trigger('click');

		// Has class node-selected
		ok($('.list-group-item:first').hasClass('node-selected'), 'Node is correctly selected : class "node-selected" added');

		// Only one can be selected
		ok(($('.node-selected').length === 1), 'There is only one selected node');

		// Has correct icon
		var iconClass = options.selectedIcon || options.nodeIcon;
		ok(!iconClass || $('.expand-icon:first').hasClass(iconClass), 'Node icon is correct');

		// Events triggered
		ok(cbWorked, 'onNodeSelected function was called');
		ok(onWorked, 'nodeSelected was fired');
	});

	test('Unselecting a node', function () {

		var cbWorked, onWorked = false;
		var $tree = init({
			data: data,
			onNodeUnselected: function(/*event, date*/) {
				cbWorked = true;
			}
		})
		.on('nodeUnselected', function(/*event, date*/) {
			onWorked = true;
		});
		var options = getOptions($tree);

		// First select a node
		$('.list-group-item:first').trigger('click');
		cbWorked = onWorked = false;

		// Simulate click
		$('.list-group-item:first').trigger('click');

		// Does not have class node-selected
		ok(!$('.list-group-item:first').hasClass('node-selected'), 'Node is correctly unselected : class "node-selected" removed');

		// There are no selected nodes
		ok(($('.node-selected').length === 0), 'There are no selected nodes');

		// Has correct icon
		ok(!options.nodeIcon || $('.expand-icon:first').hasClass(options.nodeIcon), 'Node icon is correct');

		// Events triggered
		ok(cbWorked, 'onNodeUnselected function was called');
		ok(onWorked, 'nodeUnselected was fired');
	});

	test('Prevent a node being unselected (preventUnselect true)', function () {
		var cbWorked, onWorked = false;
		var $tree = init({
			data: data,
			preventUnselect: true,
			onNodeUnselected: function(/*event, date*/) {
				cbWorked = true;
			}
		})
		.on('nodeUnselected', function(/*event, date*/) {
			onWorked = true;
		});
		var options = getOptions($tree);

		// First select a node
		$('.list-group-item:first').trigger('click');
		cbWorked = onWorked = false;

		// Simulate click
		$('.list-group-item:first').trigger('click');

		// Class node-selected was not removed
		ok($('.list-group-item:first').hasClass('node-selected'), 'Node was not unselected : class "node-selected" not removed');

		// A single node remains selected
		ok(($('.node-selected').length === 1), 'A single node is still selected');

		// Has correct icon
		ok(!options.nodeIcon || $('.expand-icon:first').hasClass(options.nodeIcon), 'Node icon is correct');

		// Events where not triggered
		ok(!cbWorked, 'onNodeUnselected function was not called');
		ok(!onWorked, 'nodeUnselected was not fired');
	});

	test('Selecting multiple nodes (multiSelect true)', function () {

		init({
			data: data,
			multiSelect: true
		});

		var $firstEl = $('.list-group-item:nth-child(1)').trigger('click');
		var $secondEl = $('.list-group-item:nth-child(2)').trigger('click');

		$firstEl = $('.list-group-item:nth-child(1)');
		$secondEl = $('.list-group-item:nth-child(2)');

		ok($firstEl.hasClass('node-selected'), 'First node is correctly selected : class "node-selected" added');
		ok($secondEl.hasClass('node-selected'), 'Second node is correctly selected : class "node-selected" added');
		ok(($('.node-selected').length === 2), 'There are two selected nodes');
	});

	test('Clicking a non-selectable, collapsed node expands the node', function () {
		var testData = $.extend(true, {}, data);
		testData[0].selectable = false;

		var cbCalled, onCalled = false;
		var el = init({
			levels: 1,
			data: testData,
			onNodeSelected: function(/*event, date*/) {
				cbCalled = true;
			}
		})
		.on('nodeSelected', function(/*event, date*/) {
			onCalled = true;
		});

		var nodeCount = $(el.selector + ' ul li:not(.node-hidden)').length;
		var firstNode = $('.list-group-item:first');
		firstNode.trigger('click');
		firstNode = $('.list-group-item:first');
		ok(!firstNode.hasClass('node-selected'), 'Node should not be selected');
		ok(!cbCalled, 'onNodeSelected function should not be called');
		ok(!onCalled, 'nodeSelected should not fire');
		ok(($(el.selector + ' ul li:not(.node-hidden)').length > nodeCount), 'Number of nodes are increased, so node must have expanded');
	});

	test('Clicking a non-selectable, expanded node collapses the node', function () {
		var testData = $.extend(true, {}, data);
		testData[0].selectable = false;

		var cbCalled, onCalled = false;
		var el = init({
			levels: 2,
			data: testData,
			onNodeSelected: function(/*event, date*/) {
				cbCalled = true;
			}
		})
		.on('nodeSelected', function(/*event, date*/) {
			onCalled = true;
		});

		var nodeCount = $(el.selector + ' ul li:not(.node-hidden)').length;
		var firstNode = $('.list-group-item:first');
		firstNode.trigger('click');
		firstNode = $('.list-group-item:first');

		ok(!firstNode.hasClass('node-selected'), 'Node should not be selected');
		ok(!cbCalled, 'onNodeSelected function should not be called');
		ok(!onCalled, 'nodeSelected should not fire');
		ok(($(el.selector + ' ul li:not(.node-hidden)').length < nodeCount), 'Number of nodes has decreased, so node must have collapsed');
	});

	test('Checking a node', function () {

		// setup test
		var cbWorked, onWorked = false;
		var $tree = init({
			data: data,
			showCheckbox: true,
			onNodeChecked: function(/*event, date*/) {
				cbWorked = true;
			}
		})
		.on('nodeChecked', function(/*event, date*/) {
			onWorked = true;
		});
		var options = getOptions($tree);

		// simulate click event on check icon
		var $el = $('.check-icon:first');
		$el.trigger('click');

		// check state is correct
		$el = $('.check-icon:first');
		ok(($el.attr('class').indexOf(options.checkedIcon) !== -1), 'Node is checked : icon is correct');
		ok(cbWorked, 'onNodeChecked function was called');
		ok(onWorked, 'nodeChecked was fired');
	});

	test('Unchecking a node', function () {

		// setup test
		var cbWorked, onWorked = false;
		var $tree = init({
			data: data,
			showCheckbox: true,
			onNodeUnchecked: function(/*event, date*/) {
				cbWorked = true;
			}
		})
		.on('nodeUnchecked', function(/*event, date*/) {
			onWorked = true;
		});
		var options = getOptions($tree);

		// first check a node
		var $el = $('.check-icon:first');
		$el.trigger('click');

		// then simulate unchecking a node
		cbWorked = onWorked = false;
		$el = $('.check-icon:first');
		$el.trigger('click');

		// check state is correct
		$el = $('.check-icon:first');
		ok(($el.attr('class').indexOf(options.uncheckedIcon) !== -1), 'Node is unchecked : icon is correct');
		ok(cbWorked, 'onNodeUnchecked function was called');
		ok(onWorked, 'nodeUnchecked was fired');
	});


	module('Methods');

	test('findNodes', function () {
		var tree = init({ data: data }).treeview(true);
		var nodeParent1 = tree.findNodes('Parent 1', 'text')[0];
		equal(nodeParent1.text, 'Parent 1', 'Correct node returned : requested "Parent 1", got "Parent 1"');
	});

	test('getNodes', function () {
		var tree = init({ data: data }).treeview(true);
		var nodes = tree.getNodes();
		ok((nodes instanceof Array), 'Result is an array');
		equal(nodes.length, 9, 'Correct number of nodes returned');
	});

	test('getParents', function () {
		var tree = init({ data: data }).treeview(true);
		var nodeChild1 = tree.findNodes('Child 1', 'text');
		var parentNode = tree.getParents(nodeChild1)[0];
		equal(parentNode.text, 'Parent 1', 'Correct node returned : requested parent of "Child 1", got "Parent 1"');
	});

	test('getSiblings', function () {
		var tree = init({ data: data }).treeview(true);

		// Test root level, internally uses the this.tree
		var nodeParent1 = tree.findNodes('Parent 1', 'text');
		var nodeParent1Siblings = tree.getSiblings(nodeParent1);
		var isArray = (nodeParent1Siblings instanceof Array);
		var countOK = nodeParent1Siblings.length === 4;
		var resultsOK = nodeParent1Siblings[0].text === 'Parent 2';
		resultsOK = resultsOK && nodeParent1Siblings[1].text === 'Parent 3';
		resultsOK = resultsOK && nodeParent1Siblings[2].text === 'Parent 4';
		resultsOK = resultsOK && nodeParent1Siblings[3].text === 'Parent 5';
		ok(isArray, 'Correct siblings for "Parent 1" [root] : is array');
		ok(countOK, 'Correct siblings for "Parent 1" [root] : count OK');
		ok(resultsOK, 'Correct siblings for "Parent 1" [root] : results OK');

		// Test non root level, internally uses getParent.nodes
		var nodeChild1 = tree.findNodes('Child 1', 'text');
		var nodeChild1Siblings = tree.getSiblings(nodeChild1);
		var isArray = (nodeChild1Siblings instanceof Array);
		var countOK = nodeChild1Siblings.length === 1;
		var results = nodeChild1Siblings[0].text === 'Child 2'
		ok(isArray, 'Correct siblings for "Child 1" [non root] : is array');
		ok(countOK, 'Correct siblings for "Child 1" [non root] : count OK');
		ok(results, 'Correct siblings for "Child 1" [non root] : results OK');
	});

	test('getSelected', function () {
		var tree = init({ data: data }).treeview(true);
		tree.selectNode(tree.findNodes('Parent 1', 'text'));

		var selectedNodes = tree.getSelected();
		ok((selectedNodes instanceof Array), 'Result is an array');
		equal(selectedNodes.length, 1, 'Correct number of nodes returned');
		equal(selectedNodes[0].text, 'Parent 1', 'Correct node returned');
	});

	test('getUnselected', function () {
		var tree = init({ data: data }).treeview(true);
		tree.selectNode(tree.findNodes('Parent 1', 'text'));

		var unselectedNodes = tree.getUnselected();
		ok((unselectedNodes instanceof Array), 'Result is an array');
		equal(unselectedNodes.length, 8, 'Correct number of nodes returned');
	});

	// Assumptions:
	// Default tree + expanded to 2 levels,
	// means 1 node 'Parent 1' should be expanded and therefore returned
	test('getExpanded', function () {
		var tree = init({ data: data }).treeview(true);
		var expandedNodes = tree.getExpanded();
		ok((expandedNodes instanceof Array), 'Result is an array');
		equal(expandedNodes.length, 1, 'Correct number of nodes returned');
		equal(expandedNodes[0].text, 'Parent 1', 'Correct node returned');
	});

	// Assumptions:
	// Default tree + expanded to 2 levels, means only 'Parent 1' should be expanded
	// as all other parent nodes have no children their state will be collapsed
	// which means 8 of the 9 nodes should be returned
	test('getCollapsed', function () {
		var tree = init({ data: data }).treeview(true);
		var collapsedNodes = tree.getCollapsed();
		ok((collapsedNodes instanceof Array), 'Result is an array');
		equal(collapsedNodes.length, 8, 'Correct number of nodes returned');
	});

	test('getChecked', function () {
		var tree = init({ data: data }).treeview(true);
		tree.checkNode(tree.findNodes('Parent 1', 'text'));

		var checkedNodes = tree.getChecked();
		ok((checkedNodes instanceof Array), 'Result is an array');
		equal(checkedNodes.length, 1, 'Correct number of nodes returned');
		equal(checkedNodes[0].text, 'Parent 1', 'Correct node returned');
	});

	test('getUnchecked', function () {
		var tree = init({ data: data }).treeview(true);
		tree.checkNode(tree.findNodes('Parent 1', 'text'));

		var uncheckedNodes = tree.getUnchecked();
		ok((uncheckedNodes instanceof Array), 'Result is an array');
		equal(uncheckedNodes.length, 8, 'Correct number of nodes returned');
	});

	test('getDisabled', function () {
		var tree = init({ data: data }).treeview(true);
		tree.disableNode(tree.findNodes('Parent 1', 'text'));

		var disabledNodes = tree.getDisabled();
		ok((disabledNodes instanceof Array), 'Result is an array');
		equal(disabledNodes.length, 1, 'Correct number of nodes returned');
		equal(disabledNodes[0].text, 'Parent 1', 'Correct node returned');
	});

	test('getEnabled', function () {
		var tree = init({ data: data }).treeview(true);
		tree.disableNode(tree.findNodes('Parent 1', 'text'));

		var enabledNodes = tree.getEnabled();
		ok((enabledNodes instanceof Array), 'Result is an array');
		equal(enabledNodes.length, 8, 'Correct number of nodes returned');
	});

	asyncTest('addNode', function (assert) {
		// expect(9);
		var $tree, tree, parent;

		// Append single root node
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text !== singleNode.text) {
					return;
				}
				equal(node.parentId, undefined, 'Append single root node : is root node');
				equal(node.level, 1, 'Append single root node : correct level');
				equal(node.nodeId, '0.5', 'Append single root node : correct id');
			}
		})
		.treeview(true)
		.addNode(singleNode);

		// Append single child node
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text !== singleNode.text) {
					return;
				}
				equal(node.parentId, parent.nodeId, 'Append single child node : is child node');
				equal(node.level, 2, 'Append single child node : correct level');
				equal(node.nodeId, '0.0.2', 'Append single child node : correct id');
			}
		});
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text')[0];
		tree.addNode(singleNode, parent);

		// Insert single root node
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text !== singleNode.text) {
					return;
				}
				equal(node.parentId, undefined, 'Insert single root node : is root node');
				equal(node.level, 1, 'Insert single root node : correct level');
				equal(node.nodeId, '0.0', 'Insert single root node : correct id');
			}
		});
		tree = $tree.treeview(true);
		tree.addNode(singleNode, null, 0);

		// Insert single child node
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text !== singleNode.text) {
					return;
				}
				equal(node.parentId, parent.nodeId, 'Insert single child node : is child node');
				equal(node.level, 2, 'Insert single child node : correct level');
				equal(node.nodeId, '0.0.0', 'Insert single child node : correct id');
			}
		});
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text')[0];
		tree.addNode(singleNode, parent, 0);

		// Append multiple root nodes
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text === multiNodes[0].text) {
					equal(node.parentId, undefined, 'Append multiple root node 1 : is root node');
					equal(node.level, 1, 'Append multiple root node 1 : correct level');
					equal(node.nodeId, '0.5', 'Append multiple root node 1 : correct id');
				} else if (node.text === multiNodes[1].text) {
					equal(node.parentId, undefined, 'Append multiple root node 2 : is root node');
					equal(node.level, 1, 'Append multiple root node 2 : correct level');
					equal(node.nodeId, '0.6', 'Append multiple root node 2 : correct id');
				}
			}
		})
		.treeview(true)
		.addNode(multiNodes);

		// Append multiple child nodes
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text === multiNodes[0].text) {
					equal(node.parentId, parent.nodeId, 'Append multiple child node 1 : is child node');
					equal(node.level, 2, 'Append multiple child node 1 : correct level');
					equal(node.nodeId, '0.0.2', 'Append multiple child node 1 : correct id');
				} else if (node.text === multiNodes[1].text) {
					equal(node.parentId, parent.nodeId, 'Append multiple child node 2 : is child node');
					equal(node.level, 2, 'Append multiple child node 2 : correct level');
					equal(node.nodeId, '0.0.3', 'Append multiple child node 2 : correct id');
				}
			}
		})
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text')[0];
		tree.addNode(multiNodes, parent);

		// Insert multiple root nodes
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text === multiNodes[0].text) {
					equal(node.parentId, undefined, 'Insert multiple root node 1 : is root node');
					equal(node.level, 1, 'Insert multiple root node 1 : correct level');
					equal(node.nodeId, '0.0', 'Insert multiple root node 1 : correct id');
				} else if (node.text === multiNodes[1].text) {
					equal(node.parentId, undefined, 'Insert multiple root node 2 : is root node');
					equal(node.level, 1, 'Insert multiple root node 2 : correct level');
					equal(node.nodeId, '0.1', 'Insert multiple root node 2 : correct id');
				}
			}
		})
		.treeview(true)
		.addNode(multiNodes, null, 0);

		// Insert multiple child nodes
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text === multiNodes[0].text) {
					equal(node.parentId, parent.nodeId, 'Insert multiple child node 1 : is child node');
					equal(node.level, 2, 'Insert multiple child node 1 : correct level');
					equal(node.nodeId, '0.0.0', 'Insert multiple child node 1 : correct id');
				} else if (node.text === multiNodes[1].text) {
					equal(node.parentId, parent.nodeId, 'Insert multiple child node 2 : is child node');
					equal(node.level, 2, 'Insert multiple child node 2 : correct level');
					equal(node.nodeId, '0.0.1', 'Insert multiple child node 2 : correct id');
					start();
				}
			}
		})
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text')[0];
		tree.addNode(multiNodes, parent, 0);
	});

	asyncTest('addNodeAfter', function (assert) {
		var $tree, tree, parent;

		// Append single node after
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text !== singleNode.text) {
					return;
				}
				equal(node.level, 1, 'Append single node after : correct level');
				equal(node.nodeId, '0.1', 'Append single node after : correct id');
			}
		});
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text');
		tree.addNodeAfter(singleNode, parent);

		// Append multiple nodes after
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text === multiNodes[0].text) {
					equal(node.level, 1, 'Append multiple nodes after 1 : correct level');
					equal(node.nodeId, '0.1', 'Append multiple nodes after 1 : correct id');
				} else if (node.text === multiNodes[1].text) {
					equal(node.level, 1, 'Append multiple nodes after 2 : correct level');
					equal(node.nodeId, '0.2', 'Append multiple nodes after 2 : correct id');
					start();
				}
			}
		});
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text');
		tree.addNodeAfter(multiNodes, parent);
	});

	asyncTest('addNodeBefore', function (assert) {
		var $tree, tree, parent;

		// Append single node before
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text !== singleNode.text) {
					return;
				}
				equal(node.level, 1, 'Append single node before : correct level');
				equal(node.nodeId, '0.0', 'Append single node before : correct id');
			}
		});
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text');
		tree.addNodeBefore(singleNode, parent);

		// Append multiple nodes before
		$tree = init({
			data: data,
			onNodeRendered: function (events, node) {
				if (node.text === multiNodes[0].text) {
					equal(node.level, 1, 'Append multiple nodes before 1 : correct level');
					equal(node.nodeId, '0.0', 'Append multiple nodes before 1 : correct id');
				} else if (node.text === multiNodes[1].text) {
					equal(node.level, 1, 'Append multiple nodes before 2 : correct level');
					equal(node.nodeId, '0.1', 'Append multiple nodes before 2 : correct id');
					start();
				}
			}
		});
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text');
		tree.addNodeBefore(multiNodes, parent);
	});

	asyncTest('removeNode', function (assert) {
		var $tree, tree, node, first = true;

		// Remove single node
		$tree = init({
			data: data,
			onRendered: function (events, nodes) {
				if (first) {
					first = false;
					return;
				}
				equal(tree.getNodes().length, 4, 'Remove single node - correct number of nodes remain');
				start();
			}
		});
		tree = $tree.treeview(true);
		node = tree.findNodes('Parent 1', 'text');
		tree.removeNode(node);
	});

	asyncTest('updateNode', function (assert) {
		var $tree, tree, parent, first = true;

		// Remove single node
		$tree = init({
			data: data,
			onRendered: function (events, nodes) {
				if (first) {
					first = false;
					return;
				}
				equal(tree.getNodes().length, 5, 'Update single node - correct number of nodes');
				// equal($($tree.selector + ' ul li').length, 5, 'Update single node - correct number of elements');
				equal(tree.getNodes()[0].text, singleNode.text, 'Update single node - correct text');
				start();
			}
		});
		tree = $tree.treeview(true);
		parent = tree.findNodes('Parent 1', 'text');
		tree.updateNode(parent, singleNode);
	});

	test('disableAll / enableAll', function () {
		var $tree = init({ data: data, levels: 1 });
		var tree = $tree.treeview(true);

		tree.disableAll();
		equal($($tree.selector + ' ul li:not(.node-hidden).node-disabled').length, 5, 'Disable all works, 9 nodes with node-disabled class');

		tree.enableAll();
		equal($($tree.selector + ' ul li:not(.node-hidden).node-disabled').length, 0, 'Check all works, 9 nodes non with node-disabled class');
	});

	test('disableNode / enableNode', function () {
		var tree = init({ data: data, levels: 1 }).treeview(true);
		var node = tree.findNodes('Parent 1', 'text');

		tree.disableNode(node);
		ok($('.list-group-item:first').hasClass('node-disabled'), 'Disable node : Node has class node-disabled');
		ok(($('.node-disabled').length === 1), 'Disable node : There is only one disabled node');

		tree.enableNode(node);
		ok(!$('.list-group-item:first').hasClass('node-disabled'), 'Enable node : Node does not have class node-disabled');
		ok(($('.node-checked').length === 0), 'Enable node : There are no disabled nodes');
	});

	test('toggleNodeDisabled', function () {
		var tree = init({ data: data, levels: 1 }).treeview(true);
		var node = tree.findNodes('Parent 1', 'text');

		tree.toggleNodeDisabled(node);
		ok($('.list-group-item:first').hasClass('node-disabled'), 'Toggle node : Node has class node-disabled');
		ok(($('.node-disabled').length === 1), 'Toggle node : There is only one disabled node');

		tree.toggleNodeDisabled(node);
		ok(!$('.list-group-item:first').hasClass('node-disabled'), 'Toggle node : Node does not have class node-disabled');
		ok(($('.node-disabled').length === 0), 'Toggle node : There are no disabled nodes');
	});

	test('checkAll / uncheckAll', function () {
		var $tree = init({ data: data, levels: 3, showCheckbox: true });
		var tree = $tree.treeview(true);

		tree.checkAll();
		equal($($tree.selector + ' ul li.node-checked').length, 9, 'Check all works, 9 nodes with node-checked class');
		equal($($tree.selector + ' ul li .glyphicon-check').length, 9, 'Check all works, 9 nodes with glyphicon-check icon');

		tree.uncheckAll();
		equal($($tree.selector + ' ul li.node-checked').length, 0, 'Check all works, 9 nodes non with node-checked class');
		equal($($tree.selector + ' ul li .glyphicon-unchecked').length, 9, 'Check all works, 9 nodes with glyphicon-unchecked icon');
	});

	test('checkNode / uncheckNode', function () {
		var tree = init({ data: data, showCheckbox: true }).treeview(true);
		var node = tree.findNodes('Parent 1', 'text');

		tree.checkNode(node);
		ok($('.list-group-item:first').hasClass('node-checked'), 'Check node : Node has class node-checked');
		ok(($('.node-checked').length === 1), 'Check node : There is only one checked node');
		ok($('.check-icon:first').hasClass(tree.options.checkedIcon), 'Check node : Node icon is correct');

		tree.uncheckNode(node);
		ok(!$('.list-group-item:first').hasClass('node-checked'), 'Uncheck node : Node does not have class node-checked');
		ok(($('.node-checked').length === 0), 'Uncheck node : There are no checked nodes');
		ok($('.check-icon:first').hasClass(tree.options.uncheckedIcon), 'Uncheck node : Node icon is correct');
	});

	test('toggleNodeChecked', function () {
		var tree = init({ data: data, showCheckbox: true }).treeview(true);
		var node = tree.findNodes('Parent 1', 'text');

		tree.toggleNodeChecked(node);
		ok($('.list-group-item:first').hasClass('node-checked'), 'Toggle node : Node has class node-checked');
		ok(($('.node-checked').length === 1), 'Toggle node : There is only one checked node');
		ok($('.check-icon:first').hasClass(tree.options.checkedIcon), 'Toggle node : Node icon is correct');

		tree.toggleNodeChecked(node);
		ok(!$('.list-group-item:first').hasClass('node-checked'), 'Toggle node : Node does not have class node-checked');
		ok(($('.node-checked').length === 0), 'Toggle node : There are no checked nodes');
		ok($('.check-icon:first').hasClass(tree.options.uncheckedIcon), 'Toggle node : Node icon is correct');
	});

	test('selectNode / unselectNode', function () {
		var tree = init({ data: data, selectedIcon: 'glyphicon glyphicon-selected' }).treeview(true);
		var node = tree.findNodes('Parent 1', 'text');

		tree.selectNode(node);
		ok($('.list-group-item:first').hasClass('node-selected'), 'Select node : Node has class node-selected');
		ok(($('.node-selected').length === 1), 'Select node : There is only one selected node');

		tree.unselectNode(node);
		ok(!$('.list-group-item:first').hasClass('node-selected'), 'Unselect node : Node does not have class node-selected');
		ok(($('.node-selected').length === 0), 'Unselect node : There are no selected nodes');
	});

	test('toggleNodeSelected', function () {
		var tree = init({ data: data }).treeview(true);
		var node = tree.findNodes('Parent 1', 'text');

		tree.toggleNodeSelected(node);
		ok($('.list-group-item:first').hasClass('node-selected'), 'Toggle node : Node has class node-selected');
		ok(($('.node-selected').length === 1), 'Toggle node : There is only one selected node');

		tree.toggleNodeSelected(node);
		ok(!$('.list-group-item:first').hasClass('node-selected'), 'Toggle node : Node does not have class node-selected');
		ok(($('.node-selected').length === 0), 'Toggle node : There are no selected nodes');
	});

	test('expandAll / collapseAll', function () {
		var $tree = init({ data: data, levels: 1 });
		var tree = $tree.treeview(true);

		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 5, 'Starts in collapsed state, 5 root nodes displayed');

		tree.expandAll();
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 9, 'Expand all works, all 9 nodes displayed');

		tree.collapseAll();
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 5, 'Collapse all works, 5 original root nodes displayed');

		tree.expandAll({ levels: 1 });
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 7, 'Expand all (levels = 1) works, correctly displayed 7 nodes');
	});

	test('expandNode / collapseNode / toggleExpanded', function () {
		var $tree = init({ data: data, levels: 1 });
		var tree = $tree.treeview(true);
		var node = tree.findNodes('Parent 1', 'text');

		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 5, 'Starts in collapsed state, 5 root nodes displayed');

		tree.expandNode(node);
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 7, 'Expand node works, 7 nodes displayed');

		tree.collapseNode(node);
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 5, 'Collapse node works, 5 original nodes displayed');

		tree.toggleNodeExpanded(node);
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 7, 'Toggle node works, 7 nodes displayed');

		tree.toggleNodeExpanded(node);
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 5, 'Toggle node works, 5 original nodes displayed');

		tree.expandNode(node, { levels: 2 });
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 9, 'Expand node (levels = 2, by id) works, 9 nodes displayed');
	});

	test('revealNode', function () {
		var $tree = init({ data: data, levels: 1 });
		var tree = $tree.treeview(true);

		tree.revealNode(tree.findNodes('Child 1', 'text'));
		equal($($tree.selector + ' ul li:not(.node-hidden)').length, 7, 'Reveal node works, reveal Child 1 and 7 nodes displayed');
	});

	test('search', function () {
		var cbWorked, onWorked = false;
		var $tree = init({
			data: data,
			onSearchComplete: function(/*event, results*/) {
				cbWorked = true;
			}
		})
		.on('searchComplete', function(/*event, results*/) {
			onWorked = true;
		});
		var tree = $tree.treeview(true);

		// Case sensitive, exact match
		var result = tree.search('Parent 1', { ignoreCase: false, exactMatch: true });
		equal(result.length, 1, 'Search "Parent 1" case sensitive, exact match - returns 1 result');

		// Case sensitive, like
		result = tree.search('Parent', { ignoreCase: false, exactMatch: false });
		equal(result.length, 5, 'Search "Parent" case sensitive, exact match - returns 5 results');

		// Case insensitive, exact match
		result = tree.search('parent 1', { ignoreCase: true, exactMatch: true });
		equal(result.length, 1, 'Search "parent 1" case insensitive, exact match - returns 1 result');

		// Case insensitive, like
		result = tree.search('parent', { ignoreCase: true, exactMatch: false });
		equal(result.length, 5, 'Search "parent" case insensitive, exact match - returns 5 results')

		// Check events fire
		ok(cbWorked, 'onSearchComplete function was called');
		ok(onWorked, 'searchComplete was fired');
	});

	test('clearSearch', function () {
		var cbWorked, onWorked = false;
		var $tree = init({
			data: data,
			onSearchCleared: function(/*event, results*/) {
				cbWorked = true;
			}
		})
		.on('searchCleared', function(/*event, results*/) {
			onWorked = true;
		});
		var tree = $tree.treeview(true);

		// Check results are cleared
		tree.search('Parent 1', { ignoreCase: false, exactMatch: true });
		equal($tree.find('.node-result').length, 1, 'Search results highlighted');
		tree.clearSearch();
		equal($tree.find('.node-result').length, 0, 'Search results cleared');

		// Check events fire
		ok(cbWorked, 'onSearchCleared function was called');
		ok(onWorked, 'searchCleared was fired');
	});


	module('Events');

	asyncTest('Lifecycle Events', function (assert) {
		expect(5);

		var nodeRenderedTriggered = false;
		var $tree = init({
			data: data,
			onLoading: function(/*event, results*/) {
				ok(true, 'onLoading triggered');
			},
			onInitialized: function(/*event, results*/) {
				ok(true, 'onLoading triggered');
			},
			onNodeRendered: function(/*event, results*/) {
				// nodeRendered triggers per node
				if (!nodeRenderedTriggered) {
					nodeRenderedTriggered = true;
					ok(true, 'onLoading triggered');
				}
			},
			onRendered: function(/*event, results*/) {
				ok(true, 'onLoading triggered');
			},
			onDestroyed: function(/*event, results*/) {
				ok(true, 'onLoading triggered');
				start();
			}
		})
		.treeview(true)
		.remove();
	});

	asyncTest('Loading Failed Event', function (assert) {
		expect(1);

		var $tree = init({
			dataUrl: {url: 'no.json'},
			onLoadingFailed: function(/*event, results*/) {
				ok(true, 'onLoadingFailed triggered');
				start();
			}
		})
		.treeview(true);
	});

}());
