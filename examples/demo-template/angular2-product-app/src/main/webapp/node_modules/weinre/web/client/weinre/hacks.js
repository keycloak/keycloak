/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// a place for hacks

;(function(){

var version = navigator.appVersion.match(/^.*Chrome\/(\d+)\..*$/)
if (!version) return

version = parseInt(version[1])
if (version <= 26) return

setTimeout(fixToolbarItem, 1000)

function fixToolbarItem() {
    var toolbarItems = document.querySelectorAll(".toolbar-item.toggleable")

    for (var i=0; i<toolbarItems.length; i++) {
        var style = toolbarItems[i].style
        if (style.display != "none") {
            toolbarItems[i].style.display = "inline-block"
        }
    }
}

})();

// __proto__ inheritance support; Weinre client classes only
(function () {

    var hasProto = "__proto__" in Object.prototype;

    if (!hasProto) {
        Object.defineProperty(Object.prototype, '__proto__', {set : function(value){
            var tmpFunc = new Function();
            tmpFunc.prototype = Object.create(value);

            function copyProperties(a, b) {
                var d;
                for (var i in b) {
                    d = Object.getOwnPropertyDescriptor(b, i);

                    if (d &&(d.get || d.set)) {
                        Object.defineProperty(a, i, d);
                    } else {
                        a[i] = b[i];
                    }
                }
                return a;
            }

            function findProtoOwner(root, proto, depth){

                if (depth > 1) return;

                var props = Object.getOwnPropertyNames(root),
                    prop,
                    d;

                for (var i = props.length; i--;) {
                    prop = props[i];

                    d = Object.getOwnPropertyDescriptor(root, prop);

                    if (d && (d.get || d.set))  continue;

                    if (typeof(root[prop]) === 'function' && root[prop].prototype === proto) {
                        return root[prop];
                    }

                    if (root[prop] && root[prop].prototype instanceof WebInspector.Object) {
                        var owner = findProtoOwner(root[prop], proto, depth+1);

                        if (owner) return owner;
                    }
                }
            }

            copyProperties(tmpFunc.prototype, this);

            // hack to be able to redefine object prototype; we can't do it another way
            // because we have prototype as 'this' here only
            var owner = findProtoOwner (window.WebInspector, this, 0);

            if (owner) {
                owner.prototype = new tmpFunc();
                owner.prototype.proto = value;
            }

        }, get : function(){
            return this.proto;
        }});
    }
})();

//if (!Object.prototype.__defineGetter__) {
//    Object.prototype.__defineGetter__ = function (key, fn) {
//        Object.defineProperty(this, key, {get: fn});
//    }
//}
//
//if (!Object.prototype.__defineSetter__) {
//    Object.prototype.__defineSetter__ = function (key, fn) {
//        Object.defineProperty(this, key, {set: fn, configurable: true, writable : true});
//    }
//}

// hack for not existing scrollIntoViewIfNeeded
if (!HTMLElement.prototype.scrollIntoViewIfNeeded){
    HTMLElement.prototype.scrollIntoViewIfNeeded = function(alignWithTop) {
		return this.scrollIntoView(alignWithTop);
	};
}

// hack for not existing setBaseAndExtent
if (!Selection.prototype.setBaseAndExtent){
    Selection.prototype.setBaseAndExtent = Selection.prototype.selectAllChildren;
}