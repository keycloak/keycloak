// a helper directive for injecting formatters and parsers
angular.module('ui.select2').directive('injectTransformers', [ function () {
  return {
    restrict: 'A',
    require: 'ngModel',
    priority: -1,
    link: function (scope, element, attr, ngModel) {
      var local = scope.$eval(attr.injectTransformers);

      if (!angular.isObject(local) || !angular.isFunction(local.fromModel) || !angular.isFunction(local.fromElement)) {
          throw "The injectTransformers directive must be bound to an object with two functions (`fromModel` and `fromElement`)";
      }

      ngModel.$parsers.push(local.fromElement);
      ngModel.$formatters.push(local.fromModel);
    }
  };
}]);

/*global describe, beforeEach, module, inject, it, spyOn, expect, $ */
describe('uiSelect2', function () {
  'use strict';

  var scope, $compile, options, $timeout;
  beforeEach(module('ui.select2'));
  beforeEach(inject(function (_$rootScope_, _$compile_, _$window_, _$timeout_) {
    scope = _$rootScope_.$new();
    $compile = _$compile_;
    $timeout = _$timeout_;
    scope.options = {
      query: function (query) {
        var data = {
          results: [{ id: 1, text: 'first' }]
        };
        query.callback(data);
      }
    };

    scope.transformers = {
      fromModel: function (modelValue) {
        if (!modelValue) {
          return modelValue;
        }

        if (angular.isArray(modelValue)) {
          return modelValue.map(function (val) {
            val.text += " - I've been formatted";
            return val;
          });
        }

        if (angular.isObject(modelValue)) {
          modelValue.text += " - I've been formatted";
          return modelValue;
        }

        return modelValue + " - I've been formatted";
      },
      fromElement: function (elementValue) {
        var suffix = " - I've been formatted";

        if (!elementValue) {
          return elementValue;
        }

        if (angular.isArray(elementValue)) {
          return elementValue.map(function (val) {
            val.text += val.text.slice(0, val.text.indexOf(" - I've been formatted"));
            return val;
          });
        }

        if (angular.isObject(elementValue)) {
          elementValue.text = elementValue.text.slice(0, elementValue.text.indexOf(suffix));
          return elementValue;
        }

        if (elementValue) {
          return elementValue.slice(0, elementValue.indexOf(suffix));
        }

        return undefined;
      }
    };
  }));

  /**
   * Compile a template synchronously
   * @param  {String} template The string to compile
   * @return {Object}          A reference to the compiled template
   */
  function compile(template) {
    var element = $compile(template)(scope);
    scope.$apply();
    $timeout.flush();
    return element;
  }

  describe('with a <select> element', function () {
    describe('compiling this directive', function () {
      it('should throw an error if we have no model defined', function () {
        expect(function(){
          compile('<select type="text" ui-select2></select>');
        }).toThrow();
      });
      it('should create proper DOM structure', function () {
        var element = compile('<select ui-select2 ng-model="foo"></select>');
        expect(element.siblings().is('div.select2-container')).toBe(true);
      });
      it('should not modify the model if there is no initial value', function(){
        //TODO 
      });
    });
    describe('when model is changed programmatically', function(){
      describe('for single select', function(){
        it('should set select2 to the value', function(){
          scope.foo = 'First';
          var element = compile('<select ui-select2 ng-model="foo"><option>First</option><option>Second</option></select>');
          expect(element.select2('val')).toBe('First');
          scope.$apply('foo = "Second"');
          expect(element.select2('val')).toBe('Second');
        });
        it('should handle falsey values', function(){
          scope.foo = 'First';
          var element = compile('<select ui-select2="{allowClear:true}" ng-model="foo"><option>First</option><option>Second</option></select>');
          expect(element.select2('val')).toBe('First');
          scope.$apply('foo = false');
          expect(element.select2('val')).toBe(null);
          scope.$apply('foo = "Second"');
          scope.$apply('foo = null');
          expect(element.select2('val')).toBe(null);
          scope.$apply('foo = "Second"');
          scope.$apply('foo = undefined');
          expect(element.select2('val')).toBe(null);
        });
      });
      describe('for multiple select', function(){
        it('should set select2 to multiple value', function(){
          scope.foo = ['First'];
          var element = compile('<select ui-select2="{allowClear:true}" multiple ng-model="foo"><option>First</option><option>Second</option><option>Third</option></select>');
          expect(element.select2('val')).toEqual(['First']);
          scope.$apply('foo = ["Second"]');
          expect(element.select2('val')).toEqual(['Second']);
          scope.$apply('foo = ["Second","Third"]');
          expect(element.select2('val')).toEqual(['Second','Third']);
        });
        it('should handle falsey values', function(){
          scope.foo = ['First'];
          var element = compile('<select ui-select2="{allowClear:true}" multiple ng-model="foo"><option>First</option><option>Second</option><option>Third</option></select>');
          expect(element.val()).toEqual(['First']);
          scope.$apply('foo = ["Second"]');
          scope.$apply('foo = false');
          expect(element.select2('val')).toEqual([]);
          scope.$apply('foo = ["Second"]');
          scope.$apply('foo = null');
          expect(element.select2('val')).toEqual([]);
          scope.$apply('foo = ["Second"]');
          scope.$apply('foo = undefined');
          expect(element.select2('val')).toEqual([]);
        });
      });
    });
    it('should observe the disabled attribute', function () {
      var element = compile('<select ui-select2 ng-model="foo" ng-disabled="disabled"></select>');
      expect(element.siblings().hasClass('select2-container-disabled')).toBe(false);
      scope.$apply('disabled = true');
      expect(element.siblings().hasClass('select2-container-disabled')).toBe(true);
      scope.$apply('disabled = false');
      expect(element.siblings().hasClass('select2-container-disabled')).toBe(false);
    });
    it('should observe the multiple attribute', function () {
      var element = $compile('<select ui-select2 ng-model="foo" ng-multiple="multiple"></select>')(scope);

      expect(element.siblings().hasClass('select2-container-multi')).toBe(false);
      scope.$apply('multiple = true');
      expect(element.siblings().hasClass('select2-container-multi')).toBe(true);
      scope.$apply('multiple = false');
      expect(element.siblings().hasClass('select2-container-multi')).toBe(false);
    });
    it('should observe an option with ng-repeat for changes', function(){
      scope.items = ['first', 'second', 'third'];
      scope.foo = 'fourth';
      var element = compile('<select ui-select2 ng-model="foo"><option ng-repeat="item in items">{{item}}</option></select>');
      expect(element.select2('val')).toBe(null);
      scope.$apply('foo="fourth";items=["fourth"]');
      $timeout.flush();
      expect(element.select2('val')).toBe('fourth');
    });
  });
  describe('with an <input> element', function () {
    describe('compiling this directive', function () {
      it('should throw an error if we have no model defined', function () {
        expect(function() {
          compile('<input ui-select2/>');
        }).toThrow();
      });
      it('should create proper DOM structure', function () {
        var element = compile('<input ui-select2="options" ng-model="foo"/>');
        expect(element.siblings().is('div.select2-container')).toBe(true);
      });
      it('should not modify the model if there is no initial value', function(){
        //TODO 
      });
    });
    describe('when model is changed programmatically', function(){
      describe('for single-select', function(){
        it('should call select2(data, ...) for objects', function(){
          var element = compile('<input ng-model="foo" ui-select2="options">');
          spyOn($.fn, 'select2');
          scope.$apply('foo={ id: 1, text: "first" }');
          expect(element.select2).toHaveBeenCalledWith('data', { id: 1, text: "first" });
        });
        it('should call select2(val, ...) for strings', function(){
          var element = compile('<input ng-model="foo" ui-select2="options">');
          spyOn($.fn, 'select2');
          scope.$apply('foo="first"');
          expect(element.select2).toHaveBeenCalledWith('val', 'first');
        });
      });
      describe('for multi-select', function(){
        it('should call select2(data, ...) for arrays', function(){
          var element = compile('<input ng-model="foo" multiple ui-select2="options">');
          spyOn($.fn, 'select2');
          scope.$apply('foo=[{ id: 1, text: "first" },{ id: 2, text: "second" }]');
          expect(element.select2).toHaveBeenCalledWith('data', [{ id: 1, text: "first" },{ id: 2, text: "second" }]);
        });
        it('should call select2(data, []) for falsey values', function(){
          var element = compile('<input ng-model="foo" multiple ui-select2="options">');
          spyOn($.fn, 'select2');
          scope.$apply('foo=[]');
          expect(element.select2).toHaveBeenCalledWith('data', []);
        });
        xit('should call select2(val, ...) for strings', function(){
          var element = compile('<input ng-model="foo" multiple ui-select2="options">');
          spyOn($.fn, 'select2');
          scope.$apply('foo="first,second"');
          expect(element.select2).toHaveBeenCalledWith('val', 'first,second');
        });
      });
    });
    describe('consumers of ngModel should correctly use $viewValue', function() {
      it('should use any formatters if present (select - single select)', function(){
        scope.foo = 'First';
        var element = compile('<select ui-select2 ng-model="foo" inject-transformers="transformers"><option>First - I\'ve been formatted</option><option>Second - I\'ve been formatted</option></select>');
        expect(element.select2('val')).toBe('First - I\'ve been formatted');
        scope.$apply('foo = "Second"');
        expect(element.select2('val')).toBe('Second - I\'ve been formatted');
      });

      // isMultiple && falsey
      it('should use any formatters if present (input multi select - falsey value)', function() {
        // need special function to hit this case
        // old code checked modelValue... can't just pass undefined to model value because view value will be the same
        scope.transformers.fromModel = function(modelValue) {
          if (modelValue === "magic") {
            return undefined;
          }

          return modelValue;
        };

        var element = compile('<input ng-model="foo" multiple ui-select2="options" inject-transformers="transformers">');
        spyOn($.fn, 'select2');
        scope.$apply('foo="magic"');
        expect(element.select2).toHaveBeenCalledWith('data', []);
      });
      // isMultiple && isArray
      it('should use any formatters if present (input multi select)', function() {
        var element = compile('<input ng-model="foo" multiple ui-select2="options" inject-transformers="transformers">');
        spyOn($.fn, 'select2');
        scope.$apply('foo=[{ id: 1, text: "first" },{ id: 2, text: "second" }]');
        expect(element.select2).toHaveBeenCalledWith('data', [{ id: 1, text: "first - I've been formatted" },{ id: 2, text: "second - I've been formatted" }]);
      });
      // isMultiple...
      xit('should use any formatters if present (input multi select - non array)', function() {
        var element = compile('<input ng-model="foo" multiple ui-select2="options" inject-transformers="transformers">');
        spyOn($.fn, 'select2');
        scope.$apply('foo={ id: 1, text: "first" }');
        expect(element.select2).toHaveBeenCalledWith('val', { id: 1, text: "first - I've been formatted" });
      });

      // !isMultiple
      it('should use any formatters if present (input - single select - object)', function() {
        var element = compile('<input ng-model="foo" ui-select2="options" inject-transformers="transformers">');
        spyOn($.fn, 'select2');
        scope.$apply('foo={ id: 1, text: "first" }');
        expect(element.select2).toHaveBeenCalledWith('data', { id: 1, text: "first - I've been formatted" });
      });
      it('should use any formatters if present (input - single select - non object)', function() {
        var element = compile('<input ng-model="foo" ui-select2="options" inject-transformers="transformers">');
        spyOn($.fn, 'select2');
        scope.$apply('foo="first"');
        expect(element.select2).toHaveBeenCalledWith('val', "first - I've been formatted");
      });

      it('should not set the default value using scope.$eval', function() {
        // testing directive instantiation - change order of test
        spyOn($.fn, 'select2');
        spyOn($.fn, 'val');
        scope.$apply('foo=[{ id: 1, text: "first" },{ id: 2, text: "second" }]');

        var element = compile('<input ng-model="foo" multiple ui-select2="options" inject-transformers="transformers">');
        expect(element.val).not.toHaveBeenCalledWith([{ id: 1, text: "first" },{ id: 2, text: "second" }]);
      });
      it('should expect a default value to be set with a call to the render method', function() {
        // this should monitor the events after init, when the timeout callback executes
        var opts = angular.copy(scope.options);
        opts.multiple = true;

        scope.$apply('foo=[{ id: 1, text: "first" },{ id: 2, text: "second" }]');

        spyOn($.fn, 'select2');
        var element = compile('<input ng-model="foo" multiple ui-select2="options" inject-transformers="transformers">');
        
        // select 2 init
        expect(element.select2).toHaveBeenCalledWith(opts);

        // callback setting
        expect(element.select2).toHaveBeenCalledWith('data', [{ id: 1, text: "first - I've been formatted" },{ id: 2, text: "second - I've been formatted" }]);

        // retieve data
        expect(element.select2).toHaveBeenCalledWith('data');
      });

    });
    it('should set the model when the user selects an item', function(){
      var element = compile('<input ng-model="foo" multiple ui-select2="options">');
      // TODO: programmactically select an option
      // expect(scope.foo).toBe(/*  selected val  */) ;
    });

    it('updated the view when model changes with complex object', function(){
      scope.foo = [{'id': '0', 'text': '0'}];
      scope.options['multiple'] = true;
      var element = compile('<input ng-model="foo" ui-select2="options">');
      scope.$digest();

      scope.foo.push({'id': '1', 'text': '1'});
      scope.$digest();

      expect(element.select2('data')).toEqual(
        [{'id': '0', 'text': '0'}, {'id': '1', 'text': '1'}]);
    });


    describe('simple_tags', function() {

      beforeEach(function() {
        scope.options['multiple'] = true;
        scope.options['simple_tags'] = true;
        scope.options['tags'] = [];
      });

      it('Initialize the select2 view based on list of strings.', function() {
        scope.foo = ['tag1', 'tag2'];

        var element = compile('<input ng-model="foo" ui-select2="options">');
        scope.$digest();

        expect(element.select2('data')).toEqual([
          {'id': 'tag1', 'text': 'tag1'},
          {'id': 'tag2', 'text': 'tag2'}
          ]);
      });

      it(
      'When list is empty select2 view model is also initialized as empty',
      function() {
        scope.foo = [];

        var element = compile('<input ng-model="foo" ui-select2="options">');
        scope.$digest();

        expect(element.select2('data')).toEqual([]);
      });

      it(
      'Updating the model with a string will update the select2 view model.',
      function() {
        scope.foo = [];
        var element = compile('<input ng-model="foo" ui-select2="options">');
        scope.$digest();

        scope.foo.push('tag1');
        scope.$digest();

        expect(element.select2('data')).toEqual([
          {'id': 'tag1', 'text': 'tag1'}
          ]);
      });

      it(
      'Updating the select2 model will update AngularJS model with a string.',
      function() {
        scope.foo = [];
        var element = compile('<input ng-model="foo" ui-select2="options">');
        scope.$digest();

        element.select2('data', [
          {'id':'tag1', 'text': 'tag1'},
          {'id':'tag2', 'text': 'tag2'}
        ]);
        element.trigger('change');

        expect(scope.foo).toEqual(['tag1', 'tag2']);
      });

    });

  });
});