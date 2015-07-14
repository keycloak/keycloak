# Bootstrap Tree View

---

A simple and elegant solution to displaying hierarchical tree structures (i.e. a Tree View) while levering the best that Twitter Bootstrap has to offer.

![Bootstrap Tree View Default](https://raw.github.com/jondmiles/bootstrap-treeview/master/screenshot/default.PNG)

<!--For full documentation and examples, please visit [Bootstrap Tree View Website](http://www.jondmiles.com/bootstrap-treeview/ "Click to visit Bootstrap Tree View")-->

## Requirements


Where provided these are the actual versions bootstrap-treeview has been tested against.  Other versions should work but you use them as your own risk.

- [Bootstrap v3.0.3](http://getbootstrap.com/)
- [jQuery v2.0.3](http://jquery.com/)

Sorry no support planned for Bootstrap 2.

## Usage

A full list of dependencies required for the bootstrap-treeview to function correctly.

	<!-- Required Stylesheets -->
    <link href="./css/bootstrap.css" rel="stylesheet">

	<!-- Required Javascript -->
    <script src="./js/jquery.js"></script>
    <script src="./js/bootstrap-treeview.js"></script>


The component will bind to any existing DOM element.

	<div id="tree"></div>


Basic usage may look something like this.

	function getTree() {
		// Some logic to retrieve, or generate tree structure
		return data;
	}
 
	$('#tree').treeview({data: getTree()});



## Data Structure

In order to define the hierarchical structure needed for the tree it's necessary to provide a nested array of JavaScript objects.

Example

	var tree = [
	  {
	    text: "Parent 1"
	    nodes: [
	      {
	        text: "Child 1"
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

At the lowest level a tree node is a represented as a simple JavaScript object.  Just one required property `text` will build you a tree.

	{
		text: "Node 1"
	}

If you want to do more, here's the full node specification

	{
		text: "Node 1", 
		icon: "glyphicon glyphicon-stop",
		color: "#000000",
		backColor: "#FFFFFF",
		href: "#node-1",
		tags: ['available'],
		nodes: [
			{}, 
			...
		]
	}



## Node Properties

The following properties are defined to allow node level overrides, such as node specific icons, colours and tags.

### text
String.  Mandatory

The text value displayed for a given tree node, typically to the right of the nodes icon.

### icon
String.  Optional

The icon displayed on a given node, typically to the left of the text.

For simplicity we directly leverage [Bootstraps Glyphicons support](http://getbootstrap.com/components/#glyphicons) and as such you should provide both the base class and individual icon class separated by a space.  

By providing the base class you retain full control over the icons used.  If you want to use your own then just add your class to this icon field.

### color
String.  Optional

The foreground color used on a given node, overrides global color option.

### backColor
String.  Optional

The background color used on a given node, overrides global color option.

### href
String.  Optional

Used in conjunction with global enableLinks option to specify anchor tag URL on a given node.

### tags
Array of Strings.  Optional

Used in conjunction with global showTags option to add additional information to the right of each node; using [Bootstrap Badges](http://getbootstrap.com/components/#badges) 

### Extendible

You can extend the node object by adding any number of additional key value pairs that you require for your application.  Remember this is the object which will be passed around during selection events.



## Options

### data
Array of Objects.  No default, expects data

This is the core data to be displayed by the tree view.

### backColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: inherits from Bootstrap.css.

Sets the default background color used by all nodes, except when overridden on a per node basis in data.

### borderColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: inherits from Bootstrap.css.

Sets the border color for the component; set showBorder to false if you don't want a visible border.

### collapseIcon
String, class name(s).  Default: "glyphicon glyphicon-minus" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be used on a collapsible tree node.

### color
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: inherits from Bootstrap.css.

Sets the default foreground color used by all nodes, except when overridden on a per node basis in data.

### enableLinks
Boolean.  Default: false

Whether or not to present node text as a hyperlink.  The href value of which must be provided in the data structure on a per node basis.

### expandIcon
String, class name(s).  Default: "glyphicon glyphicon-plus" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the icon to be used on an expandable tree node.

### highlightSelected 
Boolean.  Default: true

Whether or not to highlight the selected node.

### onhoverColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: '#F5F5F5'.

Sets the default background color activated when the users cursor hovers over a node.

### levels 
Integer. Default: 2

Sets the number of hierarchical levels deep the tree will be expanded to by default.

### nodeIcon
String, class name(s).  Default: "glyphicon glyphicon-stop" as defined by [Bootstrap Glyphicons](http://getbootstrap.com/components/#glyphicons)

Sets the default icon to be used on all nodes, except when overridden on a per node basis in data.

### selectedColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: '#FFFFFF'.

Sets the foreground color of the selected node.

### selectedBackColor
String, [any legal color value](http://www.w3schools.com/cssref/css_colors_legal.asp).  Default: '#FFFFFF'.

Sets the background color of the selected node.

### showBorder
Boolean.  Default: true

Whether or not to display a border around nodes.

### showTags
Boolean.  Default: false

Whether or not to display tags to the right of each node.  The values of which must be provided in the data structure on a per node basis.



## Methods

### remove

Removes the tree view component. Removing attached events, internal attached objects, and added HTML elements.

	$('#tree').treeview('remove');



## Events

### nodeSelected
Fired when a user selects a node. You can bind to it using either the callback handler or the standard jQuery .on method

Example using options callback handler:

	var options = {
		onNodeSelected: function(event, node) {
			// Your logic goes here
		}
	}
	$('#tree').treeview(options);

and using jQuery .on method

	$('#tree').on('nodeSelected', function(event, node) {
		// Your logic goes here
	});
		



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