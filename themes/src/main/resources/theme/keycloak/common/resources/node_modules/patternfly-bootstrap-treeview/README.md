# Bootstrap Tree View

---

![Bower version](https://img.shields.io/bower/v/patternfly-bootstrap-treeview.svg?style=flat)
[![npm version](https://img.shields.io/npm/v/patternfly-bootstrap-treeview.svg?style=flat)](https://www.npmjs.com/package/patternfly-bootstrap-treeview)
[![Build Status](https://img.shields.io/travis/patternfly/patternfly-bootstrap-treeview/master.svg?style=flat)](https://travis-ci.org/patternfly/patternfly-bootstrap-treeview)

A simple and elegant solution to displaying hierarchical tree structures (i.e. a Tree View) while leveraging the best that Twitter Bootstrap has to offer.

![Bootstrap Tree View Default](https://raw.github.com/jonmiles/patternfly-bootstrap-treeview/master/screenshot/default.PNG)

## Dependencies

Where provided these are the actual versions patternfly-bootstrap-treeview has been tested against.

- [Bootstrap v3.3.4 (>= 3.0.0)](http://getbootstrap.com/) (doesn't support Bootstrap 4)
- [jQuery v2.1.3 (>= 1.9.0)](http://jquery.com/)


## Getting Started

### Install

You can install using bower (recommended):

```javascript
$ bower install patternfly-bootstrap-treeview
```

or using npm:

```javascript
$ npm install patternfly-bootstrap-treeview
```

or [download](https://github.com/jonmiles/patternfly-bootstrap-treeview/releases) manually.



### Usage

Add the following resources for the patternfly-bootstrap-treeview to function correctly.

```html
<!-- Required Stylesheets -->
<link href="bootstrap.css" rel="stylesheet">

<!-- Required Javascript -->
<script src="jquery.js"></script>
<script src="bootstrap-treeview.js"></script>
```

The component will bind to any existing DOM element.

```html
<div id="tree"></div>
```

Basic usage may look something like this.

```javascript
function getTree() {
  // Some logic to retrieve, or generate tree structure
  return data;
}

$('#tree').treeview({data: getTree()});
```


## Data Structure

In order to define the hierarchical structure needed for the tree it's necessary to provide a nested array of JavaScript objects.

Example

```javascript
var tree = [
  {
    text: "Parent 1",
    nodes: [
      {
        text: "Child 1",
        nodes: [
          {
            text: "Grandchild 1"
          },
          {
            text: "Grandchild 2"
          }
        ]
      },
      {
        text: "Child 2"
      }
    ]
  },
  {
    text: "Parent 2"
  },
  {
    text: "Parent 3"
  },
  {
    text: "Parent 4"
  },
  {
    text: "Parent 5"
  }
];
```

At the lowest level a tree node is a represented as a simple JavaScript object.  This one required property `text` will build you a tree.

```javascript
{
  text: "Node 1"
}
```

If you want to do more, here's the full node specification

```javascript
{
  text: "Node 1",
  icon: "glyphicon glyphicon-stop",
  image: "something.png",
  selectedIcon: "glyphicon glyphicon-stop",
  color: "#000000",
  backColor: "#FFFFFF",
  iconColor: "#FFFFFF",
  iconBackground: "#000000",
  selectable: true,
  checkable: true,
  state: {
    checked: true,
    disabled: true,
    expanded: true,
    selected: true
  },
  tags: [
    'available',
    {text:'not available', class:'disabled'}
  ],
  dataAttr: {
    target: '#tree'
  }
  id: 'something',
  class: 'special extraordinary',
  hideCheckbox: true,
  nodes: [
    {},
    ...
  ]
}
```

### Node Properties

The following properties are defined to allow node level overrides, such as node specific icons, colours and tags.

#### text
`String` `Mandatory`

The text value displayed for a given tree node, typically to the right of the nodes icon.

#### tooltip
`String` `Option`

The tooltip value displayed for a given tree node on mouse hover.

#### icon
`String` `Optional`

The icon displayed on a given node, typically to the left of the text.

#### image
`String` `Optional`

The image displayed on a given node, overrides the icon.

For simplicity we directly leverage [Bootstraps Glyphicons support](http://getbootstrap.com/components/#glyphicons) and as such you should provide both the base class and individual icon class separated by a space.  

By providing the base class you retain full control over the icons used.  If you want to use your own then just add your class to this icon field.

#### selectedIcon
`String` `Optional`

The icon displayed on a given node when selected, typically to the left of the text.

#### color
`String` `Optional`

The foreground color used on a given node, overrides global color option.

#### backColor
`String` `Optional`

The background color used on a given node, overrides global color option.

#### iconColor
`String` `Optional`

The color used on a given node's icon.

#### iconBackground
`String` `Optional`

The color used under a given node's background icon.

#### lazyLoad
`Boolean` `Default: false`

Adds an expand icon to the node even if it has no children, it calls the lazyLoad() function (described below) upon the first expand.

#### selectable
`Boolean` `Default: true`

Whether or not a node is selectable in the tree. False indicates the node should act as an expansion heading and will not fire selection events.

#### checkable
`Boolean` `Default: true`

Whether or not a node is checkable in the tree, used in conjunction with `showCheckbox`.

#### state
`Object` `Optional`
Describes a node's initial state.

#### state.checked
`Boolean` `Default: false`

Whether or not a node is checked, represented by a checkbox style glyphicon.

#### state.disabled
`Boolean` `Default: false`

Whether or not a node is disabled (not selectable, expandable or checkable).

#### state.expanded
`Boolean` `Default: false`

Whether or not a node is expanded i.e. open.  Takes precedence over global option levels.

#### state.selected
`Boolean` `Default: false`

Whether or not a node is selected.

#### tags
`Array of Strings`  `Optional`

Used in conjunction with global showTags option to add additional information to the right of each node; using [Bootstrap Badges](http://getbootstrap.com/components/#badges)

A tag can be an object with properties 'text' for tag value and 'class' for class names(s) of this tag

#### tagsClass
`Strings`  `Optional`

String, class names(s).  Default: "badge"

Sets the class of node tags

#### dataAttr
`Array of Objects`  `Optional`

List of per-node HTML `data-` attributes to append.

#### class
`String`  `Optional`

List of custom CSS classes to append, separated by space.

#### id
`String`  `Optional`

Custom HTML id attribute.

#### hideCheckbox
`Boolean` `Default: false`

Used to hide the checkbox of the given node when [showCheckbox](#showcheckbox) is set to `true`.

### Extendible

You can extend the node object by adding any number of additional key value pairs that you require for your application.  Remember this is the object which will be passed around during selection events.



## Options

Options allow you to customise the treeview's default appearance and behaviour.  They are passed to the plugin on initialization, as an object.

```javascript
// Example: initializing the treeview
// expanded to 5 levels
// with a background color of green
$('#tree').treeview({
  data: data,
  levels: 5,
  backColor: 'green'
});
```
You can pass a new options object to the treeview at any time but this will have the effect of re-initializing the treeview.

### List of Options

The following is a list of all available options.

#### data
Array of Objects.  No default, expects either data or dataUrl.

This is the core data to be displayed by the tree view.  If data is provided, dataUrl will be ignored.

#### dataUrl
jQuery Ajax settings object, [as documented here](http://api.jquery.com/jquery.ajax/#jQuery-ajax-settings).

Accepts a set of key/value pairs that configure an Ajax request.  All settings are optional, any provided will be merge with the following default configuration.

```javascript
{
  method: 'GET',
  dataType: 'json',
  cache: false
}
```

> JSON is the only format accepted.

#### backColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: inherits from Bootstrap.css.

Sets the default background color used by all nodes, except when overridden on a per node basis in data.

#### borderColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: inherits from Bootstrap.css.

Sets the border color for the component; set showBorder to false if you don't want a visible border.

#### changedNodeColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: blue

Sets the text color for a node with a changed checkbox.

#### checkboxFirst
Boolean. Default: false

Swaps the node icon with the checkbox, used in conjunction with showCheckbox.

#### checkedIcon
String, class names(s).  Default: "glyphicon glyphicon-check" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be as a checked checkbox, used in conjunction with showCheckbox.

#### collapseIcon
String, class name(s).  Default: "glyphicon glyphicon-minus" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be used on a collapsible tree node.

#### color
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: inherits from Bootstrap.css.

Sets the default foreground color used by all nodes, except when overridden on a per node basis in data.

#### emptyIcon
String, class name(s).  Default: "glyphicon" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be used on a tree node with no child nodes.

#### expandIcon
String, class name(s).  Default: "glyphicon glyphicon-plus" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be used on an expandable tree node.

#### loadingIcon
String, class name(s).  Default: "glyphicon glyphicon-hourglass" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be used on an a lazyLoad node before its content gets loaded.

#### hierarchicalCheck
Boolean.  Default: false

Whether or not to enable hierarchical checking/unchecking of checkboxes.

#### propagateCheckEvent
Boolean.  Default: false

Whether or not to propagate nodeChecked and nodeUnchecked events to the parent/child nodes, used in conjunction with hierarchicalCheck.

#### highlightChanges
Boolean.  Default: false
Highlights the nodes with changed checkbox state, used in conjunction with showCheckbox.

#### highlightSearchResults
Boolean.  Default: true

Whether or not to highlight search results.

#### highlightSelected
Boolean.  Default: true

Whether or not to highlight the selected node.

#### lazyLoad
Function.  Default: undefined

This function is called when a lazyly-loadable node is being expanded for the first time. The node is available as the first argument, while the second argument is a function responsible for passing the loaded data to the renderer. The data needs to be in the same JSON format as specified above.

#### levels
Integer. Default: 2

Sets the number of hierarchical levels deep the tree will be expanded to by default.

#### multiSelect
Boolean.  Default: false

Whether or not multiple nodes can be selected at the same time.

#### nodeIcon
String, class name(s).  Default: "glyphicon glyphicon-stop" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the default icon to be used on all nodes, except when overridden on a per node basis in data.

#### onhoverColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: '#F5F5F5'.

Sets the default background color activated when the users cursor hovers over a node.

#### partiallyCheckedIcon
String, class names(s).  Default: "glyphicon glyphicon-expand" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be as a partially checked checkbox, used in conjunction with showCheckbox and hierarchicalCheck.

#### preventUnselect
Boolean.  Default: false

Whether or not a node can be unselected without another node first being selected.

#### allowReselect
Boolean.  Default: false

Whether or not a node can be reselected when its already selected, used in conjunction with preventUnselect.

#### selectedIcon
String, class name(s).  Default: "glyphicon glyphicon-stop" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the default icon to be used on all selected nodes, except when overridden on a per node basis in data.

#### searchResultBackColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: undefined, inherits.

Sets the background color of the selected node.

#### searchResultColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: '#D9534F'.

Sets the foreground color of the selected node.

#### selectedBackColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: '#428bca'.

Sets the background color of the selected node.

#### selectedColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: '#FFFFFF'.

Sets the foreground color of the selected node.

#### showBorder
Boolean.  Default: true

Whether or not to display a border around nodes.

#### showCheckbox
Boolean.  Default: false

Whether or not to display checkboxes on nodes.

#### showIcon
Boolean.  Default: true

Whether or not to display a nodes icon.

#### showImage
Boolean.  Default: false

Whether or not to display a nodes image instead of the icon.


#### showTags
Boolean.  Default: false

Whether or not to display tags to the right of each node.  The values of which must be provided in the data structure on a per node basis.

#### tagsClass
String, class names(s).  Default: "badge"

Sets the class of tags

#### uncheckedIcon
String, class names(s).  Default: "glyphicon glyphicon-unchecked" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be as an unchecked checkbox, used in conjunction with showCheckbox.

#### wrapNodeText
Boolean.  Default: false

Whether or not to surround the text of the node with a `<span class='text'>` tag.


## Methods

Methods provide a way of interacting with the plugin programmatically.  For example, expanding a node is possible via the expandNode method.

You can invoke methods in one of two ways, using either:

#### 1. The plugin's wrapper

The plugin's wrapper works as a proxy for accessing the underlying methods.

```javascript
$('#tree').treeview('methodName', args)
```
> Limitation, multiple arguments must be passed as an array of arguments.

#### 2. The treeview directly

You can get an instance of the treeview using one of the two following methods.

```javascript
// This special method returns an instance of the treeview.
$('#tree').treeview(true)
  .methodName(args);

// The instance is also saved in the DOM elements data,
// and accessible using the plugin's id 'treeview'.
$('#tree').data('treeview')
  .methodName(args);
```
> A better approach, if you plan a lot of interaction.

If you intend to make multiple API calls, store a reference to the treeview instance.

```javascript
var tree = $('#tree').treeview(true);
tree.method1(args);
tree.method2(args);
```


### List of Methods

The following is a list of all available methods.

> All methods that all declare argument `nodes` will accept either a single node, or an Array of nodes.

#### addNode(nodes, parentNode, index, options)

Add nodes to the tree.

```javascript
$('#tree').treeview('addNode', [ nodes, parentNode, index, { silent: true } ]);
```

> If parentNode evaluates to false, node will be added to root

> If index evaluates to false, node will be appended to the nodes

Triggers `nodeRendered` event; pass silent to suppress events.

#### addNodeAfter(nodes, node, options)

Add nodes to the tree after given node.

```javascript
$('#tree').treeview('addNodeAfter', [ nodes, node, { silent: true } ]);
```

> If node evaluates to false, node will be prepended to the tree's root

Triggers `nodeRendered` event; pass silent to suppress events.

#### addNodeBefore(nodes, node, options)

Add nodes to the tree before given node.

```javascript
$('#tree').treeview('addNodeAfter', [ nodes, node, { silent: true } ]);
```

> If node evaluates to false, node will be appended to the tree's root

Triggers `nodeRendered` event; pass silent to suppress events.

#### checkAll(options)

Checks all tree nodes

```javascript
$('#tree').treeview('checkAll', { silent: true });
```

Triggers `nodeChecked` event; pass silent to suppress events.

#### checkNode(nodes, options)

Checks given tree nodes.

```javascript
$('#tree').treeview('checkNode', [ nodes, { silent: true } ]);
```

Triggers `nodeChecked` event; pass silent to suppress events.

#### clearSearch()

Clear the tree view of any previous search results e.g. remove their highlighted state.

```javascript
$('#tree').treeview('clearSearch');
```

Triggers `searchCleared` event

#### collapseAll(options)

Collapse all tree nodes, collapsing the entire tree.

```javascript
$('#tree').treeview('collapseAll', { silent: true });
```

Triggers `nodeCollapsed` event; pass silent to suppress events.

#### collapseNode(nodes, options)

Collapse a given tree node and it's child nodes.  If you don't want to collapse the child nodes, pass option `{ ignoreChildren: true }`.

```javascript
$('#tree').treeview('collapseNode', [ nodes, { silent: true, ignoreChildren: false } ]);
```

Triggers `nodeCollapsed` event; pass silent to suppress events.

#### disableAll(options)

Disable all tree nodes

```javascript
$('#tree').treeview('disableAll', { silent: true, keepState: true });
```

Triggers `nodeDisabled` event; pass silent to suppress events and keepState to keep the expanded/checked/selected state.

#### disableNode(nodes, options)

Disable given tree nodes.

```javascript
$('#tree').treeview('disableNode', [ nodes, { silent: true, keepState: true } ]);
```

Triggers `nodeDisabled` event; pass silent to suppress events and keepState to keep the expanded/checked/selected state.

#### enableAll(options)

Enable all tree nodes

```javascript
$('#tree').treeview('enableAll', { silent: true });
```

Triggers `nodeEnabled` event; pass silent to suppress events.

#### enableNode(nodes, options)

Enable given tree nodes.

```javascript
$('#tree').treeview('enableNode', [ nodes, { silent: true } ]);
```

Triggers `nodeEnabled` event; pass silent to suppress events.

#### expandAll(options)

Expand all tree nodes.  Optionally can be expanded to any given number of levels.

```javascript
$('#tree').treeview('expandAll', { levels: 2, silent: true });
```

Triggers `nodeExpanded` event; pass silent to suppress events.

#### expandNode(nodes, options)

Expand given tree nodes.  Optionally can be expanded to any given number of levels.

```javascript
$('#tree').treeview('expandNode', [ nodes, { levels: 2, silent: true } ]);
```

Triggers `nodeExpanded` event; pass silent to suppress events.

#### findNodes(pattern, field)

Returns an array of matching node objects.

```javascript
$('#tree').treeview('findNodes', ['Parent', 'text']);
```

> Use regular expressions for pattern matching NOT string equals, if you need to match an exact string use start and end string anchors e.g. ^pattern$.

#### getChecked()

Returns an array of checked nodes e.g. state.checked = true.

```javascript
$('#tree').treeview('getChecked');
```

#### getCollapsed()

Returns an array of collapsed nodes e.g. state.expanded = false.

```javascript
$('#tree').treeview('getCollapsed');
```

#### getDisabled()

Returns an array of disabled nodes e.g. state.disabled = true.

```javascript
$('#tree').treeview('getDisabled');
```

#### getEnabled()

Returns an array of enabled nodes e.g. state.disabled = false.

```javascript
$('#tree').treeview('getEnabled');
```

#### getExpanded()

Returns an array of expanded nodes e.g. state.expanded = true.

```javascript
$('#tree').treeview('getExpanded');
```

#### getNodes()

Returns an array of all initialized nodes.

```javascript
$('#tree').treeview('getNodes', nodes);
```

#### getParents(nodes)

Returns parent nodes for given nodes, if valid otherwise returns undefined.

```javascript
$('#tree').treeview('getParent', nodes);
```

#### getSelected()

Returns an array of selected nodes e.g. state.selected = true.

```javascript
$('#tree').treeview('getSelected');
```

#### getSiblings(nodes)

Returns an array of sibling nodes for given nodes, if valid otherwise returns undefined.

```javascript
$('#tree').treeview('getSiblings', nodes);
```

#### getUnchecked()

Returns an array of unchecked nodes e.g. state.checked = false.

```javascript
$('#tree').treeview('getUnchecked');
```

#### getUnselected()

Returns an array of unselected nodes e.g. state.selected = false.

```javascript
$('#tree').treeview('getUnselected');
```

#### remove()

Removes the tree view component. Removing attached events, internal attached objects, and added HTML elements.

```javascript
$('#tree').treeview('remove');
```

#### removeNode()

Removes given nodes from the tree.

```javascript
$('#tree').treeview('removeNode', [ nodes, { silent: true } ]);
```

#### revealNode(nodes, options)

Reveals given tree nodes, expanding the tree from node to root.

```javascript
$('#tree').treeview('revealNode', [ nodes, { silent: true } ]);
```

Triggers `nodeExpanded` event; pass silent to suppress events.

#### search(pattern, options)

Searches the tree view for nodes that match a given string, highlighting them in the tree.  

Returns an array of matching nodes.

```javascript
$('#tree').treeview('search', [ 'Parent', {
  ignoreCase: true,     // case insensitive
  exactMatch: false,    // like or equals
  revealResults: true,  // reveal matching nodes
}]);
```

Triggers `searchComplete` event

#### selectNode(nodes, options)

Selects given tree nodes.

```javascript
$('#tree').treeview('selectNode', [ nodes, { silent: true } ]);
```

Triggers `nodeSelected` event; pass silent to suppress events.

#### toggleNodeChecked(nodes, options)

Toggles a node's checked state; checking if unchecked, unchecking if checked.

```javascript
$('#tree').treeview('toggleNodeChecked', [ nodes, { silent: true } ]);
```

Triggers either `nodeChecked` or `nodeUnchecked` event; pass silent to suppress events.

#### toggleNodeDisabled(nodes, options)

Toggles a node's disabled state; disabling if enabled, enabling if disabled.

```javascript
$('#tree').treeview('toggleNodeDisabled', [ nodes, { silent: true } ]);
```

Triggers either `nodeDisabled` or `nodeEnabled` event; pass silent to suppress events.

#### toggleNodeExpanded(nodes, options)

Toggles a node's expanded state; collapsing if expanded, expanding if collapsed.

```javascript
$('#tree').treeview('toggleNodeExpanded', [ nodes, { silent: true } ]);
```

Triggers either `nodeExpanded` or `nodeCollapsed` event; pass silent to suppress events.

#### toggleNodeSelected(nodes, options)

Toggles a node selected state; selecting if unselected, unselecting if selected.

```javascript
$('#tree').treeview('toggleNodeSelected', [ nodes, { silent: true } ]);
```

Triggers either `nodeSelected` or `nodeUnselected` event; pass silent to suppress events.

#### uncheckAll(options)

Uncheck all tree nodes.

```javascript
$('#tree').treeview('uncheckAll', { silent: true });
```

Triggers `nodeUnchecked` event; pass silent to suppress events.

#### uncheckNode(nodes, options)

Uncheck given tree nodes.

```javascript
$('#tree').treeview('uncheckNode', [ nodes, { silent: true } ]);
```

Triggers `nodeUnchecked` event; pass silent to suppress events.

#### updateNode(node, newNode, option)

Updates / replaces a given tree node.

```javascript
$('#tree').treeview('updateNode', [ node, newNode, { silent: true } ]);
```

Triggers `nodeRendered` event; pass silent to suppress events.

#### unmarkCheckboxChanges()

Marks all checkboxes as unchanged, removing the highlighted class from each of them.


#### unselectNode(nodes, options)

Unselects given tree nodes.

```javascript
$('#tree').treeview('unselectNode', [ nodes, { silent: true } ]);
```

Triggers `nodeUnselected` event; pass silent to suppress events.



## Events

Events are provided so that your application can respond to changes in the treeview's state.  For example, if you want to update a display when a node is selected use the `nodeSelected` event.

You can bind to any event defined below by either using an options callback handler, or the standard jQuery .on method.

Example using options callback handler:

```javascript
$('#tree').treeview({
  // The naming convention for callback's is to prepend with `on`
  // and capitalize the first letter of the event name
  // e.g. nodeSelected -> onNodeSelected
  onNodeSelected: function(event, data) {
    // Your logic goes here
  });
```

and using jQuery .on method

```javascript
$('#tree').on('nodeSelected', function(event, data) {
  // Your logic goes here
});
```


### List of Events

#### Lifecycle Events

> Use callback handlers for lifecycle events otherwise you'll miss the events fired during creation.

`loading (event)`  - The tree has initiated data loading.

`loadingFailed (event, error)`  - The tree failed to load data (ajax error)

`initialized (event, nodes)`  - The tree has initialized itself and data ready for rendering.

`nodeRendered (event, node)`  - A new node is rendered;

`rendered (event, nodes)`  - The tree is rendered;

`destroyed (event)`  The tree is being destroyed;

#### State Events

`nodeChecked (event, node)`  - A node is checked.

`nodeCollapsed (event, node)`  - A node is collapsed.

`nodeDisabled (event, node)`  - A node is disabled.

`nodeEnabled (event, node)`  - A node is enabled.

`nodeExpanded (event, node)` - A node is expanded.

`nodeSelected (event, node)`  - A node is selected.

`nodeUnchecked (event, node)`  - A node is unchecked.

`nodeUnselected (event, node)`  - A node is unselected.  

#### Other Events

`searchComplete (event, results)`  - After a search completes

`searchCleared (event, results)`  - After search results are cleared

> All events that emit multiple nodes, do so as an object collection not an array.  This is due to limitations of jQuery in cloning plain JavaScript objects.  If you need to an Array of nodes you'll need to reduce the object back into an array.


## Copyright and Licensing
Copyright 2013 Jonathan Miles

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
