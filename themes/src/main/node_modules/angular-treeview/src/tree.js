/**
 * Created by Allen Zou on 2016/10/13.
 */

"use strict";

require("./view/tree.less");
var fileIcon = require("./view/imgs/file.png");
var folderIcon = require("./view/imgs/folder.png");
var closedFolderIcon = require("./view/imgs/folder-closed.png");
var plusIcon = require("./view/imgs/plus.png");
var removeIcon = require("./view/imgs/remove.png");

var tree = angular.module("angular.tree", []);
tree
    .directive("treeNode", function () {
        return {
            scope: {
                item: "=",
                adapter: "=",
                icon: "=",
                folderOpen: "=",
                folderClose: "=",
                nodeClick: "=",
                childrenLoader: "=",
                addItem: "=",
                removeItem: "=",
                editItem: "="
            },
            require: [],
            restrict: "E",
            // templateUrl: "directive/tree/node.html",
            template: require("./view/node.html"),
            link: function($scope, element, attributes, controllers) {
                $scope.open = false;
                $scope.add_btn = plusIcon;
                $scope.remove_btn = removeIcon;
                function load_children() {
                    if ($scope.childrenLoader) {
                        $scope.childrenLoader($scope.item)
                            .then(function(children) {
                                $scope.subNodes = children;
                            })
                            .catch(function(error) {
                                console.error(error);
                                $scope.subNodes = [];
                            })
                    } else {
                        $scope.subNodes = [];
                    }
                }
                $scope.wrap_node_click = function() {
                    if ($scope.item) {
                        var adaptedItem = $scope.adapter($scope.item);
                        if (adaptedItem.type === "branch") {
                            if ($scope.open) {
                                $scope.open = false;
                                $scope.folderClose && $scope.folderClose($scope.item);
                            }
                            else {
                                $scope.open = true;
                                $scope.folderOpen && $scope.folderOpen($scope.item);
                                load_children();
                            }
                        }
                        $scope.nodeClick && $scope.nodeClick($scope.item);

                    }
                    return false;
                };
                $scope.resolve_icon = function() {
                    var icon = null;
                    var adaptedItem = $scope.adapter($scope.item);
                    if (adaptedItem.type === 'branch') {
                        icon = ($scope.icon && $scope.icon($scope.item, $scope.open))
                            || (!$scope.open && closedFolderIcon)
                            || ($scope.open && folderIcon);
                    }
                    else {
                        icon = ($scope.icon && $scope.icon($scope.item))
                            || fileIcon;
                    }
                    return icon;
                };
                $scope.node_class = function() {
                    var classes = ["node"];
                    var adaptedItem = $scope.adapter($scope.item);
                    if (adaptedItem.type === 'branch') {
                        classes.push("branch");
                        if ($scope.open) {
                            classes.push("open");
                        }
                        else {
                            classes.push("closed");
                        }
                    }
                    else {
                        classes.push("leaf");
                    }
                    return classes;
                };
                $scope.add_child = function() {
                    if ($scope.addItem) {
                        $scope.addItem($scope.item)
                            .then(function() {
                                load_children();
                            })
                        ;
                    }
                    return false;
                };
                $scope.remove_self = function() {
                    if ($scope.removeItem) {
                        $scope.removeItem($scope.item)
                            .then(function() {
                                load_children();
                            })
                        ;
                    }
                    return false;
                };
                $scope.edit = function() {
                    console.log("edit:::");
                    console.log($scope.editItem);
                    $scope.editItem && $scope.editItem($scope.item);
                    return false;
                };
            }
        };
    })
    .directive("tree", function () {
        var link = function($scope, element, attributes, controllers) {
            $scope.itemAdapter = $scope.adapter || function(item) {
                    console.log("in tree .adapter");
                    return item;
                };
            $scope.tree_class = function() {
                var classes = ["tree"];
                return classes;
            }
        };
        return {
            scope: {
                root: "=root",
                adapter: "=",
                icon: "=",
                folderOpen: "=",
                folderClose: "=",
                nodeClick: "=",
                childrenLoader: "=",
                addItem: "=",
                removeItem: "=",
                editItem: "="
            },
            require: [],
            restrict: "E",
            // templateUrl: "directive/tree/tree.html",
            template: require("./view/tree.html"),
            link: link
        }
    })
;

module.exports = tree;
