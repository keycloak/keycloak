'use strict';

describe('ui-select tests', function () {
  var scope, $rootScope, $compile, $timeout, $injector, $q, uisRepeatParser;

  var Key = {
    Enter: 13,
    Tab: 9,
    Up: 38,
    Down: 40,
    Left: 37,
    Right: 39,
    Backspace: 8,
    Delete: 46,
    Escape: 27
  };

  function isNil(value) {
    return angular.isUndefined(value) || value === null;
  }

  //create a directive that wraps ui-select
  angular.module('wrapperDirective', ['ui.select']);
  angular.module('wrapperDirective').directive('wrapperUiSelect', function () {
    return {
      restrict: 'EA',
      template: '<ui-select> \
            <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
            <ui-select-choices repeat="person in people | filter: $select.search"> \
              <div ng-bind-html="person.name | highlight: $select.search"></div> \
            </ui-select-choices> \
          </ui-select>',
      require: 'ngModel',
      scope: true,

      link: function (scope, element, attrs, ctrl) {

      }
    };

  });

  /* Create a directive that can be applied to the ui-select instance to test
   * the effects of Angular's validation process on the control.
   *
   * Does not currently work with single property binding. Looks at the
   * selected object or objects for a "valid" property. If all selected objects
   * have a "valid" property that is truthy, the validator passes.
   */
  angular.module('testValidator', []);
  angular.module('testValidator').directive('testValidator', function () {
    return {
      restrict: 'A',
      require: 'ngModel',
      link: function (scope, element, attrs, ngModel) {
        ngModel.$validators.testValidator = function (modelValue, viewValue) {
          if (isNil(modelValue)) {
            return true;
          } else if (angular.isArray(modelValue)) {
            var allValid = true, idx = modelValue.length;
            while (idx-- > 0 && allValid) {
              allValid = allValid && modelValue[idx].valid;
            }
            return allValid;
          } else {
            return !!modelValue.valid;
          }
        }
      }
    }
  });

  beforeEach(module('ngSanitize', 'ui.select', 'wrapperDirective', 'testValidator'));

  beforeEach(function () {
    module(function ($provide) {
      $provide.factory('uisOffset', function () {
        return function (el) {
          return { top: 100, left: 200, width: 300, height: 400 };
        };
      });
    });
  });

  beforeEach(inject(function (_$rootScope_, _$compile_, _$timeout_, _$injector_, _$q_, _uisRepeatParser_) {
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();
    $compile = _$compile_;
    $timeout = _$timeout_;
    $injector = _$injector_;
    $q = _$q_;
    uisRepeatParser = _uisRepeatParser_;
    scope.selection = {};

    scope.getGroupLabel = function (person) {
      return person.age % 2 ? 'even' : 'odd';
    };

    scope.filterInvertOrder = function (groups) {
      return groups.sort(function (groupA, groupB) {
        return groupA.name.toLocaleLowerCase() < groupB.name.toLocaleLowerCase();
      });
    };


    scope.people = [
      { name: 'Adam', email: 'adam@email.com', group: 'Foo', age: 12 },
      { name: 'Amalie', email: 'amalie@email.com', group: 'Foo', age: 12 },
      { name: 'Estefanía', email: 'estefanía@email.com', group: 'Foo', age: 21 },
      { name: 'Adrian', email: 'adrian@email.com', group: 'Foo', age: 21 },
      { name: 'Wladimir', email: 'wladimir@email.com', group: 'Foo', age: 30 },
      { name: 'Samantha', email: 'samantha@email.com', group: 'bar', age: 30 },
      { name: 'Nicole', email: 'nicole@email.com', group: 'bar', age: 43 },
      { name: 'Natasha', email: 'natasha@email.com', group: 'Baz', age: 54 }
    ];

    scope.peopleObj = {
      '1': { name: 'Adam', email: 'adam@email.com', age: 12, country: 'United States' },
      '2': { name: 'Amalie', email: 'amalie@email.com', age: 12, country: 'Argentina' },
      '3': { name: 'Estefanía', email: 'estefania@email.com', age: 21, country: 'Argentina' },
      '4': { name: 'Adrian', email: 'adrian@email.com', age: 21, country: 'Ecuador' },
      '5': { name: 'Wladimir', email: 'wladimir@email.com', age: 30, country: 'Ecuador' },
      '6': { name: 'Samantha', email: 'samantha@email.com', age: 30, country: 'United States' },
      '7': { name: 'Nicole', email: 'nicole@email.com', age: 43, country: 'Colombia' },
      '8': { name: 'Natasha', email: 'natasha@email.com', age: 54, country: 'Ecuador' },
      '9': { name: 'Michael', email: 'michael@email.com', age: 15, country: 'Colombia' },
      '10': { name: 'Nicolás', email: 'nicolas@email.com', age: 43, country: 'Colombia' }
    };

    scope.someObject = {};
    scope.someObject.people = [
      { name: 'Adam', email: 'adam@email.com', group: 'Foo', age: 12 },
      { name: 'Amalie', email: 'amalie@email.com', group: 'Foo', age: 12 },
      { name: 'Estefanía', email: 'estefanía@email.com', group: 'Foo', age: 21 },
      { name: 'Adrian', email: 'adrian@email.com', group: 'Foo', age: 21 },
      { name: 'Wladimir', email: 'wladimir@email.com', group: 'Foo', age: 30 },
      { name: 'Samantha', email: 'samantha@email.com', group: 'bar', age: 30 },
      { name: 'Nicole', email: 'nicole@email.com', group: 'bar', age: 43 },
      { name: 'Natasha', email: 'natasha@email.com', group: 'Baz', age: 54 }
    ];
  }));


  // DSL (domain-specific language)

  function compileTemplate(template) {
    var el = $compile(angular.element(template))(scope);
    scope.$digest();
    return el;
  }

  function createUiSelect(attrs) {
    var attrsHtml = '',
      matchAttrsHtml = '',
      choicesAttrsHtml = ''
    if (attrs !== undefined) {
      if (attrs.disabled !== undefined) { attrsHtml += ' ng-disabled="' + attrs.disabled + '"'; }
      if (attrs.required !== undefined) { attrsHtml += ' ng-required="' + attrs.required + '"'; }
      if (attrs.theme !== undefined) { attrsHtml += ' theme="' + attrs.theme + '"'; }
      if (attrs.tabindex !== undefined) { attrsHtml += ' tabindex="' + attrs.tabindex + '"'; }
      if (attrs.tagging !== undefined) { attrsHtml += ' tagging="' + attrs.tagging + '"'; }
      if (attrs.taggingTokens !== undefined) { attrsHtml += ' tagging-tokens="' + attrs.taggingTokens + '"'; }
      if (attrs.title !== undefined) { attrsHtml += ' title="' + attrs.title + '"'; }
      if (attrs.appendToBody !== undefined) { attrsHtml += ' append-to-body="' + attrs.appendToBody + '"'; }
      if (attrs.allowClear !== undefined) { matchAttrsHtml += ' allow-clear="' + attrs.allowClear + '"'; }
      if (attrs.inputId !== undefined) { attrsHtml += ' input-id="' + attrs.inputId + '"'; }
      if (attrs.ngClass !== undefined) { attrsHtml += ' ng-class="' + attrs.ngClass + '"'; }
      if (attrs.resetSearchInput !== undefined) { attrsHtml += ' reset-search-input="' + attrs.resetSearchInput + '"'; }
      if (attrs.closeOnSelect !== undefined) { attrsHtml += ' close-on-select="' + attrs.closeOnSelect + '"'; }
      if (attrs.spinnerEnabled !== undefined) { attrsHtml += ' spinner-enabled="' + attrs.spinnerEnabled + '"'; }
      if (attrs.spinnerClass !== undefined) { attrsHtml += ' spinner-class="' + attrs.spinnerClass + '"'; }
      if (attrs.refresh !== undefined) { choicesAttrsHtml += ' refresh="' + attrs.refresh + '"'; }
      if (attrs.refreshDelay !== undefined) { choicesAttrsHtml += ' refresh-delay="' + attrs.refreshDelay + '"'; }
      if (attrs.backspaceReset !== undefined) { attrsHtml += ' backspace-reset="' + attrs.backspaceReset + '"'; }
      if (attrs.uiDisableChoice !== undefined) { choicesAttrsHtml += ' ui-disable-choice="' + attrs.uiDisableChoice + '"'; }
      if (attrs.removeSelected !== undefined) { attrsHtml += ' remove-selected="' + attrs.removeSelected + '"'; }
    }

    return compileTemplate(
      '<ui-select ng-model="selection.selected"' + attrsHtml + '> \
        <ui-select-match placeholder="Pick one..."' + matchAttrsHtml + '>{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"'+ choicesAttrsHtml + '"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
  }

  function getMatchLabel(el) {
    return $(el).find('.ui-select-match > span:first > span[ng-transclude]:not(.ng-hide)').text();
  }

  function clickItem(el, text) {

    if (!isDropdownOpened(el)) {
      openDropdown(el);
    }

    $(el).find('.ui-select-choices-row div:contains("' + text + '")').click();
    scope.$digest();
  }

  function clickMatch(el) {
    $(el).find('.ui-select-match > span:first').click();
    scope.$digest();
  }

  function isDropdownOpened(el) {
    // Does not work with jQuery 2.*, have to use jQuery 1.11.*
    // This will be fixed in AngularJS 1.3
    // See issue with unit-testing directive using karma https://github.com/angular/angular.js/issues/4640#issuecomment-35002427
    return el.scope().$select.open && el.hasClass('open');
  }

  function triggerKeydown(element, keyCode) {
    var e = jQuery.Event("keydown");
    e.which = keyCode;
    e.keyCode = keyCode;
    element.trigger(e);
  }
  function triggerPaste(element, text, isClipboardEvent) {
    var e = jQuery.Event("paste");
    if (isClipboardEvent) {
      e.clipboardData = {
        getData: function () {
          return text;
        }
      };
    } else {
      e.originalEvent = {
        clipboardData: {
          getData: function () {
            return text;
          }
        }
      };
    }
    element.trigger(e);
  }

  function setSearchText(el, text) {
    el.scope().$select.search = text;
    scope.$digest();
    $timeout.flush();
  }

  function openDropdown(el) {
    var $select = el.scope().$select;
    $select.open = true;
    scope.$digest();
  }

  function closeDropdown(el) {
    var $select = el.scope().$select;
    $select.open = false;
    scope.$digest();
  }

  function showChoicesForSearch(el, search) {
    setSearchText(el, search);
    el.scope().$select.searchInput.trigger('keyup');
    scope.$digest();
  }

  it('should initialize selected choices with an array if choices source is undefined', function () {
    var el = createUiSelect(),
      ctrl = el.scope().$select;

    ctrl.setItemsFn(); // setPlainItems
    expect(ctrl.items).toEqual([]);
  });

  // Tests
  //uisRepeatParser

  it('should parse simple repeat syntax', function () {

    var locals = {};
    locals.people = [{ name: 'Wladimir' }, { name: 'Samantha' }];
    locals.person = locals.people[0];

    var parserResult = uisRepeatParser.parse('person in people');
    expect(parserResult.itemName).toBe('person');
    expect(parserResult.modelMapper(locals)).toBe(locals.person);
    expect(parserResult.source(locals)).toBe(locals.people);

    var ngExp = parserResult.repeatExpression(false);
    expect(ngExp).toBe('person in $select.items');

    var ngExpGrouped = parserResult.repeatExpression(true);
    expect(ngExpGrouped).toBe('person in $group.items');

  });

  it('should parse simple repeat syntax', function () {

    var locals = {};
    locals.people = [{ name: 'Wladimir' }, { name: 'Samantha' }];
    locals.person = locals.people[0];

    var parserResult = uisRepeatParser.parse('person.name as person in people');
    expect(parserResult.itemName).toBe('person');
    expect(parserResult.modelMapper(locals)).toBe(locals.person.name);
    expect(parserResult.source(locals)).toBe(locals.people);

  });

  it('should parse simple property binding repeat syntax', function () {

    var locals = {};
    locals.people = [{ name: 'Wladimir' }, { name: 'Samantha' }];
    locals.person = locals.people[0];

    var parserResult = uisRepeatParser.parse('person.name as person in people');
    expect(parserResult.itemName).toBe('person');
    expect(parserResult.modelMapper(locals)).toBe(locals.person.name);
    expect(parserResult.source(locals)).toBe(locals.people);

  });

  it('should parse simple property binding repeat syntax with a basic filter', function () {

    var locals = {};
    locals.people = [{ name: 'Wladimir' }, { name: 'Samantha' }];
    locals.person = locals.people[1];

    var parserResult = uisRepeatParser.parse('person.name as person in people | filter: { name: \'Samantha\' }');
    expect(parserResult.itemName).toBe('person');
    expect(parserResult.modelMapper(locals)).toBe(locals.person.name);
    expect(parserResult.source(locals)).toEqual([locals.person]);

  });

  it('should parse simple property binding repeat syntax with track by', function () {

    var locals = {};
    locals.people = [{ name: 'Wladimir' }, { name: 'Samantha' }];
    locals.person = locals.people[0];

    var parserResult = uisRepeatParser.parse('person.name as person in people track by person.name');
    expect(parserResult.itemName).toBe('person');
    expect(parserResult.modelMapper(locals)).toBe(locals.person.name);
    expect(parserResult.source(locals)).toBe(locals.people);

  });

  it('should parse (key, value) repeat syntax', function () {

    var locals = {};
    locals.people = { 'WC': { name: 'Wladimir' }, 'SH': { name: 'Samantha' } };
    locals.person = locals.people[0];

    var parserResult = uisRepeatParser.parse('(key,person) in people');
    expect(parserResult.itemName).toBe('person');
    expect(parserResult.keyName).toBe('key');
    expect(parserResult.modelMapper(locals)).toBe(locals.person);
    expect(parserResult.source(locals)).toBe(locals.people);

    var ngExp = parserResult.repeatExpression(false);
    expect(ngExp).toBe('person in $select.items');

    var ngExpGrouped = parserResult.repeatExpression(true);
    expect(ngExpGrouped).toBe('person in $group.items');

  });

  it('should parse simple property binding with (key, value) repeat syntax', function () {

    var locals = {};
    locals.people = { 'WC': { name: 'Wladimir' }, 'SH': { name: 'Samantha' } };
    locals.person = locals.people['WC'];

    var parserResult = uisRepeatParser.parse('person.name as (key, person) in people');
    expect(parserResult.itemName).toBe('person');
    expect(parserResult.keyName).toBe('key');
    expect(parserResult.modelMapper(locals)).toBe(locals.person.name);
    expect(parserResult.source(locals)).toBe(locals.people);

  });

  it('should should accept a "collection expresion" only if its not (key, value) repeat syntax', function () {

    var locals = {};
    locals.people = { 'WC': { name: 'Wladimir' }, 'SH': { name: 'Samantha' } };
    locals.person = locals.people['WC'];

    var parserResult = uisRepeatParser.parse('person.name as person in (peopleNothing || people)');
    expect(parserResult.itemName).toBe('person');
    expect(parserResult.modelMapper(locals)).toBe(locals.person.name);
    // expect(parserResult.source(locals)).toBe(locals.people);

  });

  it('should should throw if "collection expresion" used and (key, value) repeat syntax', function () {

    var locals = {};
    locals.people = { 'WC': { name: 'Wladimir' }, 'SH': { name: 'Samantha' } };
    locals.person = locals.people['WC'];

    function errorFunctionWrapper() {
      uisRepeatParser.parse('person.name as (key,person) in (people | someFilter)');
    }

    expect(errorFunctionWrapper).toThrow();

  });

  it('should not leak memory', function () {
    var cacheLenght = Object.keys(angular.element.cache).length;
    createUiSelect().remove();
    scope.$destroy();
    expect(Object.keys(angular.element.cache).length).toBe(cacheLenght);
  });

  it('should compile child directives', function () {
    var el = createUiSelect();

    var searchEl = $(el).find('.ui-select-search');
    expect(searchEl.length).toEqual(1);

    var matchEl = $(el).find('.ui-select-match');
    expect(matchEl.length).toEqual(1);

    var choicesContentEl = $(el).find('.ui-select-choices-content');
    expect(choicesContentEl.length).toEqual(1);

    var choicesContainerEl = $(el).find('.ui-select-choices');
    expect(choicesContainerEl.length).toEqual(1);

    openDropdown(el);
    var choicesEls = $(el).find('.ui-select-choices-row');
    expect(choicesEls.length).toEqual(8);
  });

  it('should correctly render initial state', function () {
    scope.selection.selected = scope.people[0];

    var el = createUiSelect();

    expect(getMatchLabel(el)).toEqual('Adam');
  });

  it('should merge both ng-class attributes defined on ui-select and its templates', function () {
    var el = createUiSelect({
      ngClass: "{class: expression}"
    });

    expect($(el).attr('ng-class')).toEqual("{class: expression, open: $select.open}");
  });

  it('should correctly render initial state with track by feature', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search track by person.name"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    scope.selection.selected = { name: 'Samantha', email: 'something different than array source', group: 'bar', age: 30 };
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
  });

  it('should correctly render initial state with track by $index', function () {

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people track by $index"> \
          {{person.email}} \
        </ui-select-choices> \
      </ui-select>'
    );

    openDropdown(el);

    var generatedId = el.scope().$select.generatedId;
    expect($(el).find('[id="ui-select-choices-row-' + generatedId + '-0"]').length).toEqual(1);
  });

  it('should utilize wrapper directive ng-model', function () {
    var el = compileTemplate('<wrapper-ui-select ng-model="selection.selected"/>');
    scope.selection.selected = { name: 'Samantha', email: 'something different than array source', group: 'bar', age: 30 };
    scope.$digest();
    expect($(el).find('.ui-select-container > .ui-select-match > span:first > span[ng-transclude]:not(.ng-hide)').text()).toEqual('Samantha');
  });

  it('should display the choices when activated', function () {
    var el = createUiSelect();

    expect(isDropdownOpened(el)).toEqual(false);

    clickMatch(el);

    expect(isDropdownOpened(el)).toEqual(true);
  });

  it('should select an item', function () {
    var el = createUiSelect();

    clickItem(el, 'Samantha');

    expect(getMatchLabel(el)).toEqual('Samantha');
  });

  it('should select an item (controller)', function () {
    var el = createUiSelect();

    el.scope().$select.select(scope.people[1]);
    scope.$digest();

    expect(getMatchLabel(el)).toEqual('Amalie');
  });

  it('should not select a non existing item', function () {
    var el = createUiSelect();

    clickItem(el, "I don't exist");

    expect(getMatchLabel(el)).toEqual('');
  });

  it('should close the choices when an item is selected', function () {
    var el = createUiSelect();

    clickMatch(el);

    expect(isDropdownOpened(el)).toEqual(true);

    clickItem(el, 'Samantha');

    expect(isDropdownOpened(el)).toEqual(false);
  });


  it('should open/close dropdown when clicking caret icon', function () {

    var el = createUiSelect({ theme: 'select2' });
    var searchInput = el.find('.ui-select-search');
    var $select = el.scope().$select;

    expect($select.open).toEqual(false);

    el.find(".ui-select-toggle").click();
    expect($select.open).toEqual(true);


    el.find(".ui-select-toggle").click();
    expect($select.open).toEqual(false);
  });

  it('should clear selection', function () {
    scope.selection.selected = scope.people[0];

    var el = createUiSelect({ theme: 'select2', allowClear: 'true' });
    var $select = el.scope().$select;

    // allowClear should be true.
    expect($select.allowClear).toEqual(true);

    // Trigger clear.
    el.find('.select2-search-choice-close').click();
    expect(scope.selection.selected).toEqual(null);

    // If there is no selection the X icon should be gone.
    expect(el.find('.select2-search-choice-close').length).toEqual(0);
  });

  it('should toggle allow-clear directive', function () {
    scope.selection.selected = scope.people[0];
    scope.isClearAllowed = false;

    var el = createUiSelect({ theme: 'select2', allowClear: '{{isClearAllowed}}' });
    var $select = el.scope().$select;

    expect($select.allowClear).toEqual(false);
    expect(el.find('.select2-search-choice-close').length).toEqual(0);

    // Turn clear on
    scope.isClearAllowed = true;
    scope.$digest();

    expect($select.allowClear).toEqual(true);
    expect(el.find('.select2-search-choice-close').length).toEqual(1);
  });

  it('should clear selection (with object as source)', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected" theme="select2"> \
        <ui-select-match placeholder="Pick one..." allow-clear="true">{{$select.selected.value.name}}</ui-select-match> \
        <ui-select-choices repeat="person.value.name as (key,person) in peopleObj | filter: $select.search"> \
          <div ng-bind-html="person.value.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.value.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    var $select = el.scope().$select;

    clickItem(el, 'Samantha');
    expect(scope.selection.selected).toEqual('Samantha');

    // allowClear should be true.
    expect($select.allowClear).toEqual(true);

    // Trigger clear.
    el.find('.select2-search-choice-close').click();
    expect(scope.selection.selected).toEqual(null);

    // If there is no selection the X icon should be gone.
    expect(el.find('.select2-search-choice-close').length).toEqual(0);
  });

  it('should pass tabindex to focusser', function () {
    var el = createUiSelect({ tabindex: 5 });

    expect($(el).find('.ui-select-focusser').attr('tabindex')).toEqual('5');
    expect($(el).attr('tabindex')).toEqual(undefined);
  });

  it('should pass tabindex to focusser when tabindex is an expression', function () {
    scope.tabValue = 22;
    var el = createUiSelect({ tabindex: '{{tabValue + 10}}' });

    expect($(el).find('.ui-select-focusser').attr('tabindex')).toEqual('32');
    expect($(el).attr('tabindex')).toEqual(undefined);
  });

  it('should not give focusser a tabindex when ui-select does not have one', function () {
    var el = createUiSelect();

    expect($(el).find('.ui-select-focusser').attr('tabindex')).toEqual(undefined);
    expect($(el).attr('tabindex')).toEqual(undefined);
  });

  it('should be disabled if the attribute says so', function () {
    var el1 = createUiSelect({ disabled: true });
    expect(el1.scope().$select.disabled).toEqual(true);
    clickMatch(el1);
    expect(isDropdownOpened(el1)).toEqual(false);

    var el2 = createUiSelect({ disabled: false });
    expect(el2.scope().$select.disabled).toEqual(false);
    clickMatch(el2);
    expect(isDropdownOpened(el2)).toEqual(true);

    var el3 = createUiSelect();
    expect(el3.scope().$select.disabled).toBeFalsy();
    clickMatch(el3);
    expect(isDropdownOpened(el3)).toEqual(true);
  });

  it('should allow decline tags when tagging function returns null', function () {
    scope.taggingFunc = function (name) {
      return null;
    };

    var el = createUiSelect({ tagging: 'taggingFunc' });
    clickMatch(el);

    showChoicesForSearch(el, 'idontexist');
    $(el).scope().$select.activeIndex = 0;
    $(el).scope().$select.select('idontexist');

    expect($(el).scope().$select.selected).not.toBeDefined();
  });

  it('should allow tagging if the attribute says so', function () {
    var el = createUiSelect({ tagging: true });
    clickMatch(el);

    $(el).scope().$select.select("I don't exist");

    expect($(el).scope().$select.selected).toEqual("I don't exist");
  });

  it('should format new items using the tagging function when the attribute is a function', function () {
    scope.taggingFunc = function (name) {
      return {
        name: name,
        email: name + '@email.com',
        group: 'Foo',
        age: 12
      };
    };

    var el = createUiSelect({ tagging: 'taggingFunc' });
    clickMatch(el);

    $(el).scope().$select.search = 'idontexist';
    $(el).scope().$select.activeIndex = 0;
    $(el).scope().$select.select('idontexist');

    expect($(el).scope().$select.selected).toEqual({
      name: 'idontexist',
      email: 'idontexist@email.com',
      group: 'Foo',
      age: 12
    });
  });

  // See when an item that evaluates to false (such as "false" or "no") is selected, the placeholder is shown https://github.com/angular-ui/ui-select/pull/32
  it('should not display the placeholder when item evaluates to false', function () {
    scope.items = ['false'];

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match>{{$select.selected}}</ui-select-match> \
        <ui-select-choices repeat="item in items | filter: $select.search"> \
          <div ng-bind-html="item | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    expect(el.scope().$select.selected).toEqual(undefined);

    clickItem(el, 'false');

    expect(el.scope().$select.selected).toEqual('false');
    expect(getMatchLabel(el)).toEqual('false');
  });

  it('should close an opened select when another one is opened', function () {
    var el1 = createUiSelect();
    var el2 = createUiSelect();
    el1.appendTo(document.body);
    el2.appendTo(document.body);

    expect(isDropdownOpened(el1)).toEqual(false);
    expect(isDropdownOpened(el2)).toEqual(false);
    clickMatch(el1);
    expect(isDropdownOpened(el1)).toEqual(true);
    expect(isDropdownOpened(el2)).toEqual(false);
    clickMatch(el2);
    expect(isDropdownOpened(el1)).toEqual(false);
    expect(isDropdownOpened(el2)).toEqual(true);

    el1.remove();
    el2.remove();
  });

  it('should bind model correctly (with object as source)', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.value.name}}</ui-select-match> \
        <ui-select-choices repeat="person.value as (key,person) in peopleObj | filter: $select.search"> \
          <div ng-bind-html="person.value.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.value.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    // scope.selection.selected = 'Samantha';

    clickItem(el, 'Samantha');
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
    expect(scope.selection.selected).toBe(scope.peopleObj[6]);

  });

  it('should bind model correctly (with object as source) using a single property', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.value.name}}</ui-select-match> \
        <ui-select-choices repeat="person.value.name as (key,person) in peopleObj | filter: $select.search"> \
          <div ng-bind-html="person.value.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.value.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    // scope.selection.selected = 'Samantha';

    clickItem(el, 'Samantha');
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
    expect(scope.selection.selected).toBe('Samantha');

  });

  it('should update choices when original source changes (with object as source)', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.value.name}}</ui-select-match> \
        <ui-select-choices repeat="person.value.name as (key,person) in peopleObj | filter: $select.search"> \
          <div ng-bind-html="person.value.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.value.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    scope.$digest();

    openDropdown(el);
    var choicesEls = $(el).find('.ui-select-choices-row');
    expect(choicesEls.length).toEqual(10);

    scope.peopleObj['11'] = { name: 'Camila', email: 'camila@email.com', age: 1, country: 'Ecuador' };
    scope.$digest();

    choicesEls = $(el).find('.ui-select-choices-row');
    expect(choicesEls.length).toEqual(11);

  });

  it('should bind model correctly (with object as source) using the key of collection', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.value.name}}</ui-select-match> \
        <ui-select-choices repeat="person.key as (key,person) in peopleObj | filter: $select.search"> \
          <div ng-bind-html="person.value.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.value.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    // scope.selection.selected = 'Samantha';

    clickItem(el, 'Samantha');
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
    expect(scope.selection.selected).toBe('6');

  });

  it('should correctly render initial state (with object as source) differentiating between falsy values', function () {
    scope.items = [{
      label: '-- None Selected --',
      value: ''
    }, {
      label: 'Yes',
      value: true
    }, {
      label: 'No',
      value: false
    }];

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match>{{ $select.selected.label }}</ui-select-match> \
        <ui-select-choices repeat="item.value as item in items track by item.value">{{ item.label }}</ui-select-choices> \
      </ui-select>'
    );

    scope.selection.selected = '';
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('-- None Selected --');
  });

  describe('backspace reset option', function () {
    it('should undefined model when pressing BACKSPACE key if backspaceReset=true', function () {
      var el = createUiSelect();
      var focusserInput = el.find('.ui-select-focusser');

      clickItem(el, 'Samantha');
      triggerKeydown(focusserInput, Key.Backspace);
      expect(scope.selection.selected).toBeUndefined();
    });

    it('should NOT reset model when pressing BACKSPACE key if backspaceReset=false', function () {
      var el = createUiSelect({ backspaceReset: false });
      var focusserInput = el.find('.ui-select-focusser');

      clickItem(el, 'Samantha');
      triggerKeydown(focusserInput, Key.Backspace);
      expect(scope.selection.selected).toBe(scope.people[5]);
    });
  });

  describe('disabled options', function () {
    function createUiSelect(attrs) {
      var attrsDisabled = '';
      if (attrs !== undefined) {
        if (attrs.disabled !== undefined) {
          attrsDisabled = ' ui-disable-choice="' + attrs.disabled + '"';
        } else {
          attrsDisabled = '';
        }
      }

      return compileTemplate(
        '<ui-select ng-model="selection.selected"> \
          <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
          <ui-select-choices repeat="person in people | filter: $select.search"' + attrsDisabled + '> \
            <div ng-bind-html="person.name | highlight: $select.search"></div> \
            <div ng-bind-html="person.email | highlight: $select.search"></div> \
          </ui-select-choices> \
        </ui-select>'
      );
    }

    function disablePerson(opts) {
      opts = opts || {};

      var key = opts.key || 'people',
        disableAttr = opts.disableAttr || 'disabled',
        disableBool = opts.disableBool === undefined ? true : opts.disableBool,
        matchAttr = opts.match || 'name',
        matchVal = opts.matchVal || 'Wladimir';

      scope['_' + key] = angular.copy(scope[key]);
      scope[key].map(function (model) {
        if (model[matchAttr] == matchVal) {
          model[disableAttr] = disableBool;
        }
        return model;
      });
    }

    function resetScope(opts) {
      opts = opts || {};
      var key = opts.key || 'people';
      scope[key] = angular.copy(scope['_' + key]);
    }

    describe('without disabling expression', function () {
      beforeEach(function () {
        disablePerson();
        this.el = createUiSelect();
      });

      it('should not allow disabled options to be selected', function () {
        clickItem(this.el, 'Wladimir');

        expect(getMatchLabel(this.el)).toEqual('Wladimir');
      });

      it('should set a disabled class on the option', function () {
        var option = $(this.el).find('.ui-select-choices-row div:contains("Wladimir")');
        var container = option.closest('.ui-select-choices-row');

        expect(container.hasClass('disabled')).toBeFalsy();
      });
    });

    describe('disable on truthy property', function () {
      beforeEach(function () {
        disablePerson({
          disableAttr: 'inactive',
          disableBool: true
        });
        this.el = createUiSelect({
          disabled: 'person.inactive'
        });
      });

      it('should allow the user to define the selected option', function () {
        expect($(this.el).find('.ui-select-choices').attr('ui-disable-choice')).toBe('person.inactive');
      });

      it('should not allow disabled options to be selected', function () {
        clickItem(this.el, 'Wladimir');

        expect(getMatchLabel(this.el)).not.toEqual('Wladimir');
      });

      it('should set a disabled class on the option', function () {

        openDropdown(this.el);

        var option = $(this.el).find('.ui-select-choices-row div:contains("Wladimir")');
        var container = option.closest('.ui-select-choices-row');

        expect(container.hasClass('disabled')).toBeTruthy();

      });
    });

    describe('disable on inverse property check', function () {
      beforeEach(function () {
        disablePerson({
          disableAttr: 'active',
          disableBool: false
        });
        this.el = createUiSelect({
          disabled: '!person.active'
        });
      });

      it('should allow the user to define the selected option', function () {
        expect($(this.el).find('.ui-select-choices').attr('ui-disable-choice')).toBe('!person.active');
      });

      it('should not allow disabled options to be selected', function () {
        clickItem(this.el, 'Wladimir');

        expect(getMatchLabel(this.el)).not.toEqual('Wladimir');
      });

      it('should set a disabled class on the option', function () {
        openDropdown(this.el);

        var option = $(this.el).find('.ui-select-choices-row div:contains("Wladimir")');
        var container = option.closest('.ui-select-choices-row');

        expect(container.hasClass('disabled')).toBeTruthy();
      });
    });

    describe('disable on expression', function () {
      beforeEach(function () {
        disablePerson({
          disableAttr: 'status',
          disableBool: 'inactive'
        });
        this.el = createUiSelect({
          disabled: "person.status == 'inactive'"
        });
      });

      it('should allow the user to define the selected option', function () {
        expect($(this.el).find('.ui-select-choices').attr('ui-disable-choice')).toBe("person.status == 'inactive'");
      });

      it('should not allow disabled options to be selected', function () {
        clickItem(this.el, 'Wladimir');

        expect(getMatchLabel(this.el)).not.toEqual('Wladimir');
      });

      it('should set a disabled class on the option', function () {
        openDropdown(this.el);

        var option = $(this.el).find('.ui-select-choices-row div:contains("Wladimir")');
        var container = option.closest('.ui-select-choices-row');

        expect(container.hasClass('disabled')).toBeTruthy();
      });
    });

    afterEach(function () {
      resetScope();
    });
  });

  describe('choices group', function () {
    function getGroupLabel(item) {
      return item.parent('.ui-select-choices-group').find('.ui-select-choices-group-label');
    }
    function createUiSelect() {
      return compileTemplate(
        '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices group-by="\'group\'" repeat="person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
      );
    }

    it('should create items group', function () {
      var el = createUiSelect();
      expect(el.find('.ui-select-choices-group').length).toBe(3);
    });

    it('should show label before each group', function () {
      var el = createUiSelect();
      expect(el.find('.ui-select-choices-group .ui-select-choices-group-label').map(function () {
        return this.textContent;
      }).toArray()).toEqual(['Foo', 'bar', 'Baz']);
    });

    it('should hide empty groups', function () {
      var el = createUiSelect();
      el.scope().$select.search = 'd';
      scope.$digest();

      expect(el.find('.ui-select-choices-group .ui-select-choices-group-label').map(function () {
        return this.textContent;
      }).toArray()).toEqual(['Foo']);
    });

    it('should change activeItem through groups', function () {
      var el = createUiSelect();
      el.scope().$select.search = 't';
      scope.$digest();
      openDropdown(el);
      var choices = el.find('.ui-select-choices-row');

      expect(choices.eq(0)).toHaveClass('active');
      expect(getGroupLabel(choices.eq(0)).text()).toBe('Foo');

      triggerKeydown(el.find('input'), 40 /*Down*/);
      scope.$digest();
      expect(choices.eq(1)).toHaveClass('active');
      expect(getGroupLabel(choices.eq(1)).text()).toBe('bar');
    });
  });

  describe('choices group by function', function () {
    function createUiSelect() {
      return compileTemplate(
        '<ui-select ng-model="selection.selected"> \
      <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
      <ui-select-choices group-by="getGroupLabel" repeat="person in people | filter: $select.search"> \
        <div ng-bind-html="person.name | highlight: $select.search"></div> \
      </ui-select-choices> \
    </ui-select>'
      );
    }
    it("should extract group value through function", function () {
      var el = createUiSelect();
      expect(el.find('.ui-select-choices-group .ui-select-choices-group-label').map(function () {
        return this.textContent;
      }).toArray()).toEqual(['odd', 'even']);
    });
  });

  describe('choices group filter function', function () {
    function createUiSelect() {
      return compileTemplate('\
        <ui-select ng-model="selection.selected"> \
          <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
          <ui-select-choices group-by="\'group\'" group-filter="filterInvertOrder"  repeat="person in people | filter: $select.search"> \
            <div ng-bind-html="person.name | highlight: $select.search"></div> \
          </ui-select-choices> \
        </ui-select>'
      );
    }
    it("should sort groups using filter", function () {
      var el = createUiSelect();
      expect(el.find('.ui-select-choices-group .ui-select-choices-group-label').map(function () {
        return this.textContent;
      }).toArray()).toEqual(["Foo", "Baz", "bar"]);
    });
  });

  describe('choices group filter array', function () {
    function createUiSelect() {
      return compileTemplate('\
        <ui-select ng-model="selection.selected"> \
          <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
          <ui-select-choices group-by="\'group\'" group-filter="[\'Foo\']" \
              repeat="person in people | filter: $select.search"> \
            <div ng-bind-html="person.name | highlight: $select.search"></div> \
          </ui-select-choices> \
        </ui-select>'
      );
    }
    it("should sort groups using filter", function () {
      var el = createUiSelect();
      expect(el.find('.ui-select-choices-group .ui-select-choices-group-label').map(function () {
        return this.textContent;
      }).toArray()).toEqual(["Foo"]);
    });
  });

  it('should format the model correctly using alias', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person as person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    clickItem(el, 'Samantha');
    expect(scope.selection.selected).toBe(scope.people[5]);
  });

  it('should parse the model correctly using alias', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person as person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    scope.selection.selected = scope.people[5];
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
  });

  it('should format the model correctly using property of alias', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    clickItem(el, 'Samantha');
    expect(scope.selection.selected).toBe('Samantha');
  });

  it('should parse the model correctly using property of alias', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    scope.selection.selected = 'Samantha';
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
  });

  it('should parse the model correctly using property of alias with async choices data', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in peopleAsync | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    $timeout(function () {
      scope.peopleAsync = scope.people;
    });

    scope.selection.selected = 'Samantha';
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('');

    $timeout.flush(); //After choices populated (async), it should show match correctly
    expect(getMatchLabel(el)).toEqual('Samantha');

  });

  //TODO Is this really something we should expect?
  it('should parse the model correctly using property of alias but passed whole object', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    scope.selection.selected = scope.people[5];
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
  });

  it('should format the model correctly without alias', function () {
    var el = createUiSelect();
    clickItem(el, 'Samantha');
    expect(scope.selection.selected).toBe(scope.people[5]);
  });

  it('should parse the model correctly without alias', function () {
    var el = createUiSelect();
    scope.selection.selected = scope.people[5];
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
  });

  it('should display choices correctly with child array', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in someObject.people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    scope.selection.selected = scope.people[5];
    scope.$digest();
    expect(getMatchLabel(el)).toEqual('Samantha');
  });

  it('should format the model correctly using property of alias and when using child array for choices', function () {
    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in someObject.people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );
    clickItem(el, 'Samantha');
    expect(scope.selection.selected).toBe('Samantha');
  });

  it('should invoke select callback on select', function () {

    scope.onSelectFn = function ($item, $model, $label) {
      scope.$item = $item;
      scope.$model = $model;
    };
    var el = compileTemplate(
      '<ui-select on-select="onSelectFn($item, $model)" ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    expect(scope.$item).toBeFalsy();
    expect(scope.$model).toBeFalsy();

    clickItem(el, 'Samantha');
    $timeout.flush();


    expect(scope.selection.selected).toBe('Samantha');

    expect(scope.$item).toEqual(scope.people[5]);
    expect(scope.$model).toEqual('Samantha');

  });

  it('should set $item & $model correctly when invoking callback on select and no single prop. binding', function () {

    scope.onSelectFn = function ($item, $model, $label) {
      scope.$item = $item;
      scope.$model = $model;
    };

    var el = compileTemplate(
      '<ui-select on-select="onSelectFn($item, $model)" ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    expect(scope.$item).toBeFalsy();
    expect(scope.$model).toBeFalsy();

    clickItem(el, 'Samantha');
    expect(scope.$item).toEqual(scope.$model);

  });

  it('should invoke remove callback on remove', function () {

    scope.onRemoveFn = function ($item, $model, $label) {
      scope.$item = $item;
      scope.$model = $model;
    };

    var el = compileTemplate(
      '<ui-select multiple on-remove="onRemoveFn($item, $model)" ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in people | filter: $select.search"> \
          <div ng-bind-html="person.name" | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    expect(scope.$item).toBeFalsy();
    expect(scope.$model).toBeFalsy();

    clickItem(el, 'Samantha');
    clickItem(el, 'Adrian');
    el.find('.ui-select-match-item').first().find('.ui-select-match-close').click();
    $timeout.flush();

    expect(scope.$item).toBe(scope.people[5]);
    expect(scope.$model).toBe('Samantha');

  });

  it('should set $item & $model correctly when invoking callback on remove and no single prop. binding', function () {

    scope.onRemoveFn = function ($item, $model, $label) {
      scope.$item = $item;
      scope.$model = $model;
    };

    var el = compileTemplate(
      '<ui-select multiple on-remove="onRemoveFn($item, $model)" ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div ng-bind-html="person.name" | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    expect(scope.$item).toBeFalsy();
    expect(scope.$model).toBeFalsy();

    clickItem(el, 'Samantha');
    clickItem(el, 'Adrian');
    el.find('.ui-select-match-item').first().find('.ui-select-match-close').click();
    $timeout.flush();

    expect(scope.$item).toBe(scope.people[5]);
    expect(scope.$model).toBe(scope.$item);
  });

  it('should call open-close callback with isOpen state as first argument on open and on close', function () {

    var el = compileTemplate(
      '<ui-select uis-open-close="onOpenCloseFn(isOpen)" ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    scope.onOpenCloseFn = function () { };
    spyOn(scope, 'onOpenCloseFn');

    openDropdown(el);
    $timeout.flush();
    expect(scope.onOpenCloseFn).toHaveBeenCalledWith(true);

    closeDropdown(el);
    $timeout.flush();
    expect(scope.onOpenCloseFn).toHaveBeenCalledWith(false);

    expect(scope.onOpenCloseFn.calls.count()).toBe(2);
  });

  it('should allow creating tag in single select mode with tagging enabled', function () {

    scope.taggingFunc = function (name) {
      return name;
    };

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected" tagging="taggingFunc" tagging-label="false"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div ng-bind-html="person.name" | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    clickMatch(el);

    var searchInput = el.find('.ui-select-search');

    setSearchText(el, 'idontexist');

    triggerKeydown(searchInput, Key.Enter);

    expect($(el).scope().$select.selected).toEqual('idontexist');
  });

  it('should allow creating tag on ENTER in multiple select mode with tagging enabled, no labels', function () {

    scope.taggingFunc = function (name) {
      return name;
    };

    var el = compileTemplate(
      '<ui-select multiple ng-model="selection.selected" tagging="taggingFunc" tagging-label="false"> \
          <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
          <ui-select-choices repeat="person in people | filter: $select.search"> \
            <div ng-bind-html="person.name" | highlight: $select.search"></div> \
            <div ng-bind-html="person.email | highlight: $select.search"></div> \
          </ui-select-choices> \
        </ui-select>'
    );

    var searchInput = el.find('.ui-select-search');

    setSearchText(el, 'idontexist');

    triggerKeydown(searchInput, Key.Enter);

    expect($(el).scope().$select.selected).toEqual(['idontexist']);
  });

  it('should allow selecting an item (click) in single select mode with tagging enabled', function () {

    scope.taggingFunc = function (name) {
      return name;
    };

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected" tagging="taggingFunc" tagging-label="false"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div ng-bind-html="person.name" | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    clickMatch(el);
    setSearchText(el, 'Sam');
    clickItem(el, 'Samantha');

    expect(scope.selection.selected).toBe(scope.people[5]);
    expect(getMatchLabel(el)).toEqual('Samantha');
  });


  it('should remove a choice when multiple and remove-selected is not given (default is true)', function () {

    var el = compileTemplate(
      '<ui-select multiple ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div class="person-name" ng-bind-html="person.name" | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    clickItem(el, 'Samantha');
    clickItem(el, 'Adrian');

    openDropdown(el);

    var choicesEls = $(el).find('.ui-select-choices-row');
    expect(choicesEls.length).toEqual(6);

    ['Adam', 'Amalie', 'Estefanía', 'Wladimir', 'Nicole', 'Natasha'].forEach(function (name, index) {
      expect($(choicesEls[index]).hasClass('disabled')).toBeFalsy();
      expect($(choicesEls[index]).find('.person-name').text()).toEqual(name);
    });
  });

  it('should not remove a pre-selected choice when not multiple and remove-selected is not given (default is true)', function () {
    scope.selection.selected = scope.people[5]; // Samantha

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div class="person-name" ng-bind-html="person.name" | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    expect(getMatchLabel(el)).toEqual("Samantha");
    openDropdown(el);

    var choicesEls = $(el).find('.ui-select-choices-row');
    expect(choicesEls.length).toEqual(8);

    ['Adam', 'Amalie', 'Estefanía', 'Adrian', 'Wladimir', 'Samantha', 'Nicole', 'Natasha'].forEach(function (name, index) {
      expect($(choicesEls[index]).hasClass('disabled')).toBeFalsy();
      expect($(choicesEls[index]).find('.person-name').text()).toEqual(name);
    });
  });

  it('should disable a choice instead of removing it when remove-selected is false', function () {

    var el = compileTemplate(
      '<ui-select multiple remove-selected="false" ng-model="selection.selected"> \
        <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div ng-bind-html="person.name" | highlight: $select.search"></div> \
          <div ng-bind-html="person.email | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    clickItem(el, 'Samantha');
    clickItem(el, 'Adrian');

    openDropdown(el);

    var choicesEls = $(el).find('.ui-select-choices-row');
    expect(choicesEls.length).toEqual(8);
    [false, false, false, true /* Adrian */, false, true /* Samantha */, false, false].forEach(function (bool, index) {
      expect($(choicesEls[index]).hasClass('disabled')).toEqual(bool);
    });
  });

  it('should append/transclude content (with correct scope) that users add at <match> tag', function () {

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match> \
          <span ng-if="$select.selected.name!==\'Wladimir\'">{{$select.selected.name}}</span>\
          <span ng-if="$select.selected.name===\'Wladimir\'">{{$select.selected.name | uppercase}}</span>\
        </ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
        </ui-select-choices> \
      </ui-select>'
    );

    clickItem(el, 'Samantha');
    expect(getMatchLabel(el).trim()).toEqual('Samantha');

    clickItem(el, 'Wladimir');
    expect(getMatchLabel(el).trim()).not.toEqual('Wladimir');
    expect(getMatchLabel(el).trim()).toEqual('WLADIMIR');

  });
  it('should append/transclude content (with correct scope) that users add at <choices> tag', function () {

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match> \
        </ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-if="person.name==\'Wladimir\'"> \
            <span class="only-once">I should appear only once</span>\
          </div> \
        </ui-select-choices> \
      </ui-select>'
    );

    openDropdown(el);
    expect($(el).find('.only-once').length).toEqual(1);


  });

  it('should call refresh function when search text changes', function () {

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match> \
        </ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search" \
          refresh="fetchFromServer($select.search)" refresh-delay="0"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-if="person.name==\'Wladimir\'"> \
            <span class="only-once">I should appear only once</span>\
          </div> \
        </ui-select-choices> \
      </ui-select>'
    );

    scope.fetchFromServer = function () { };

    spyOn(scope, 'fetchFromServer');

    el.scope().$select.search = 'r';
    scope.$digest();
    $timeout.flush();

    expect(scope.fetchFromServer).toHaveBeenCalledWith('r');

  });

  it('should call refresh function respecting minimum input length option', function () {

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match> \
        </ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search" \
          refresh="fetchFromServer($select.search)" refresh-delay="0" minimum-input-length="3"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-if="person.name==\'Wladimir\'"> \
            <span class="only-once">I should appear only once</span>\
          </div> \
        </ui-select-choices> \
      </ui-select>'
    );

    scope.fetchFromServer = function () { };

    spyOn(scope, 'fetchFromServer');

    el.scope().$select.search = 'r';
    scope.$digest();
    $timeout.flush();
    expect(scope.fetchFromServer).not.toHaveBeenCalledWith('r');

    el.scope().$select.search = 'red';
    scope.$digest();
    $timeout.flush();
    expect(scope.fetchFromServer).toHaveBeenCalledWith('red');
  });


  it('should call refresh function respecting minimum input length option with given refresh-delay', function () {

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match> \
        </ui-select-match> \
        <ui-select-choices repeat="person in people | filter: $select.search" \
          refresh="fetchFromServer($select.search)" refresh-delay="1" minimum-input-length="3"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-if="person.name==\'Wladimir\'"> \
            <span class="only-once">I should appear only once</span>\
          </div> \
        </ui-select-choices> \
      </ui-select>'
    );

    scope.fetchFromServer = function () { };

    spyOn(scope, 'fetchFromServer');

    el.scope().$select.search = 'redd';
    scope.$digest();
    $timeout.flush();
    expect(scope.fetchFromServer).toHaveBeenCalledWith('redd');


    el.scope().$select.search = 'red';
    scope.$digest();
    el.scope().$select.search = 're';
    scope.$digest();
    el.scope().$select.search = 'r';
    scope.$digest();
    $timeout.flush();
    expect(scope.fetchFromServer).not.toHaveBeenCalledWith('r');


  });


  it('should format view value correctly when using single property binding and refresh function', function () {

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected"> \
        <ui-select-match>{{$select.selected.name}}</ui-select-match> \
        <ui-select-choices repeat="person.name as person in people | filter: $select.search" \
          refresh="fetchFromServer($select.search)" refresh-delay="0"> \
          <div ng-bind-html="person.name | highlight: $select.search"></div> \
          <div ng-if="person.name==\'Wladimir\'"> \
            <span class="only-once">I should appear only once</span>\
          </div> \
        </ui-select-choices> \
      </ui-select>'
    );

    scope.fetchFromServer = function (searching) {

      if (searching == 's')
        return scope.people;

      if (searching == 'o') {
        scope.people = []; //To simulate cases were previously selected item isnt in the list anymore
      }

    };

    setSearchText(el, 'r');
    clickItem(el, 'Samantha');
    expect(getMatchLabel(el)).toBe('Samantha');

    setSearchText(el, 'o');
    expect(getMatchLabel(el)).toBe('Samantha');

  });

  it('should retain an invalid view value after refreshing items', function () {
    scope.taggingFunc = function (name) {
      return {
        name: name,
        email: name + '@email.com',
        valid: name === "iamvalid"
      };
    };

    var el = compileTemplate(
      '<ui-select ng-model="selection.selected" tagging="taggingFunc" tagging-label="false" test-validator> \
          <ui-select-match placeholder="Pick one...">{{$select.selected.email}}</ui-select-match> \
          <ui-select-choices repeat="person in people | filter: $select.search"> \
            <div ng-bind-html="person.name" | highlight: $select.search"></div> \
            <div ng-bind-html="person.email | highlight: $select.search"></div> \
          </ui-select-choices> \
        </ui-select>'
    );

    clickMatch(el);
    var searchInput = el.find('.ui-select-search');

    setSearchText(el, 'iamvalid');
    triggerKeydown(searchInput, Key.Tab);

    //model value defined because it's valid, view value defined as expected
    var validTag = scope.taggingFunc("iamvalid");
    expect(scope.selection.selected).toEqual(validTag);
    expect($(el).scope().$select.selected).toEqual(validTag);

    clickMatch(el);
    setSearchText(el, 'notvalid');
    triggerKeydown(searchInput, Key.Tab);

    //model value undefined because it's invalid, view value STILL defined as expected
    expect(scope.selection.selected).toEqual(undefined);
    expect($(el).scope().$select.selected).toEqual(scope.taggingFunc("notvalid"));
  });

  describe('search-enabled option', function () {

    var el;

    function setupSelectComponent(searchEnabled, theme) {
      el = compileTemplate(
        '<ui-select ng-model="selection.selected" theme="' + theme + '" search-enabled="' + searchEnabled + '"> \
          <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
          <ui-select-choices repeat="person in people | filter: $select.search"> \
            <div ng-bind-html="person.name | highlight: $select.search"></div> \
            <div ng-bind-html="person.email | highlight: $select.search"></div> \
          </ui-select-choices> \
        </ui-select>'
      );
    }

    describe('selectize theme', function () {

      it('should show search input when true', function () {
        setupSelectComponent(true, 'selectize');
        expect($(el).find('.ui-select-search')).not.toHaveClass('ui-select-search-hidden');
      });

      it('should hide search input when false', function () {
        setupSelectComponent(false, 'selectize');
        expect($(el).find('.ui-select-search')).toHaveClass('ui-select-search-hidden');
      });

    });

    describe('select2 theme', function () {

      it('should show search input when true', function () {
        setupSelectComponent('true', 'select2');
        expect($(el).find('.search-container')).not.toHaveClass('ui-select-search-hidden');
      });

      it('should hide search input when false', function () {
        setupSelectComponent('false', 'select2');
        expect($(el).find('.search-container')).toHaveClass('ui-select-search-hidden');
      });

    });

    describe('bootstrap theme', function () {

      it('should show search input when true', function () {
        setupSelectComponent('true', 'bootstrap');
        clickMatch(el);
        expect($(el).find('.ui-select-search')).not.toHaveClass('ui-select-search-hidden');
      });

      it('should hide search input when false', function () {
        setupSelectComponent('false', 'bootstrap');
        clickMatch(el);
        expect($(el).find('.ui-select-search')).toHaveClass('ui-select-search-hidden');
      });

    });

  });


  describe('multi selection', function () {

    function createUiSelectMultiple(attrs) {
      var attrsHtml = '',
        choicesAttrsHtml = '',
        matchesAttrsHtml = '';
      if (attrs !== undefined) {
        if (attrs.disabled !== undefined) { attrsHtml += ' ng-disabled="' + attrs.disabled + '"'; }
        if (attrs.required !== undefined) { attrsHtml += ' ng-required="' + attrs.required + '"'; }
        if (attrs.tabindex !== undefined) { attrsHtml += ' tabindex="' + attrs.tabindex + '"'; }
        if (attrs.closeOnSelect !== undefined) { attrsHtml += ' close-on-select="' + attrs.closeOnSelect + '"'; }
        if (attrs.tagging !== undefined) { attrsHtml += ' tagging="' + attrs.tagging + '"'; }
        if (attrs.taggingTokens !== undefined) { attrsHtml += ' tagging-tokens="' + attrs.taggingTokens + '"'; }
        if (attrs.taggingLabel !== undefined) { attrsHtml += ' tagging-label="' + attrs.taggingLabel + '"'; }
        if (attrs.inputId !== undefined) { attrsHtml += ' input-id="' + attrs.inputId + '"'; }
        if (attrs.groupBy !== undefined) { choicesAttrsHtml += ' group-by="' + attrs.groupBy + '"'; }
        if (attrs.uiDisableChoice !== undefined) { choicesAttrsHtml += ' ui-disable-choice="' + attrs.uiDisableChoice + '"'; }
        if (attrs.lockChoice !== undefined) { matchesAttrsHtml += ' ui-lock-choice="' + attrs.lockChoice + '"'; }
        if (attrs.removeSelected !== undefined) { attrsHtml += ' remove-selected="' + attrs.removeSelected + '"'; }
        if (attrs.resetSearchInput !== undefined) { attrsHtml += ' reset-search-input="' + attrs.resetSearchInput + '"'; }
        if (attrs.limit !== undefined) { attrsHtml += ' limit="' + attrs.limit + '"'; }
        if (attrs.onSelect !== undefined) { attrsHtml += ' on-select="' + attrs.onSelect + '"'; }
        if (attrs.removeSelected !== undefined) { attrsHtml += ' remove-selected="' + attrs.removeSelected + '"'; }
      }

      return compileTemplate(
        '<ui-select multiple ng-model="selection.selectedMultiple"' + attrsHtml + ' theme="bootstrap" style="width: 800px;"> \
                <ui-select-match "' + matchesAttrsHtml + ' placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
                <ui-select-choices repeat="person in people | filter: $select.search"' + choicesAttrsHtml + '> \
                  <div ng-bind-html="person.name | highlight: $select.search"></div> \
                  <div ng-bind-html="person.email | highlight: $select.search"></div> \
                </ui-select-choices> \
            </ui-select>'
      );
    }

    it('should initialize selected choices with an empty array when choices source is undefined', function () {
      var el = createUiSelectMultiple({ groupBy: "'age'" }),
        ctrl = el.scope().$select;

      ctrl.setItemsFn(); // updateGroups
      expect(ctrl.items).toEqual([]);
    });

    it('should render initial state', function () {
      var el = createUiSelectMultiple();
      expect(el).toHaveClass('ui-select-multiple');
      expect(el.scope().$select.selected.length).toBe(0);
      expect(el.find('.ui-select-match-item').length).toBe(0);
    });

    it('should render intial state with data-multiple attribute', function () {
      // ensure match template has been loaded by having more than one selection
      scope.selection.selectedMultiple = [scope.people[0], scope.people[1]];

      var el = compileTemplate(
        '<ui-select data-multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
            <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
            <ui-select-choices repeat="person in people | filter: $select.search"> \
              <div ng-bind-html="person.name | highlight: $select.search"></div> \
              <div ng-bind-html="person.email | highlight: $select.search"></div> \
            </ui-select-choices> \
        </ui-select>'
      );

      expect(el).toHaveClass('ui-select-multiple');
      expect(el.scope().$select.selected.length).toBe(2);
      expect(el.find('.ui-select-match-item').length).toBe(2);
    });

    it('should render intial state with x-multiple attribute', function () {
      // ensure match template has been loaded by having more than one selection
      scope.selection.selectedMultiple = [scope.people[0], scope.people[1]];

      var el = compileTemplate(
        '<ui-select x-multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
            <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
            <ui-select-choices repeat="person in people | filter: $select.search"> \
              <div ng-bind-html="person.name | highlight: $select.search"></div> \
              <div ng-bind-html="person.email | highlight: $select.search"></div> \
            </ui-select-choices> \
        </ui-select>'
      );

      expect(el).toHaveClass('ui-select-multiple');
      expect(el.scope().$select.selected.length).toBe(2);
      expect(el.find('.ui-select-match-item').length).toBe(2);
    });

    it('should set model as an empty array if ngModel isnt defined after an item is selected', function () {

      // scope.selection.selectedMultiple = [];
      var el = createUiSelectMultiple();
      expect(scope.selection.selectedMultiple instanceof Array).toBe(false);
      clickItem(el, 'Samantha');
      expect(scope.selection.selectedMultiple instanceof Array).toBe(true);
    });

    it('should render initial selected items', function () {
      scope.selection.selectedMultiple = [scope.people[4], scope.people[5]]; //Wladimir & Samantha
      var el = createUiSelectMultiple();
      expect(el.scope().$select.selected.length).toBe(2);
      expect(el.find('.ui-select-match-item').length).toBe(2);
    });

    it('should remove item by pressing X icon', function () {
      scope.selection.selectedMultiple = [scope.people[4], scope.people[5]]; //Wladimir & Samantha
      var el = createUiSelectMultiple();
      expect(el.scope().$select.selected.length).toBe(2);
      el.find('.ui-select-match-item').first().find('.ui-select-match-close').click();
      expect(el.scope().$select.selected.length).toBe(1);
      // $timeout.flush();
    });

    it('should pass tabindex to searchInput', function () {
      var el = createUiSelectMultiple({ tabindex: 5 });
      var searchInput = el.find('.ui-select-search');

      expect(searchInput.attr('tabindex')).toEqual('5');
      expect($(el).attr('tabindex')).toEqual(undefined);
    });

    it('should pass tabindex to searchInput when tabindex is an expression', function () {
      scope.tabValue = 22;
      var el = createUiSelectMultiple({ tabindex: '{{tabValue + 10}}' });
      var searchInput = el.find('.ui-select-search');

      expect(searchInput.attr('tabindex')).toEqual('32');
      expect($(el).attr('tabindex')).toEqual(undefined);
    });

    it('should not give searchInput a tabindex when ui-select does not have one', function () {
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(searchInput.attr('tabindex')).toEqual(undefined);
      expect($(el).attr('tabindex')).toEqual(undefined);
    });

    it('should update size of search input after removing an item', function () {
      scope.selection.selectedMultiple = [scope.people[4], scope.people[5]]; //Wladimir & Samantha
      var el = createUiSelectMultiple();

      spyOn(el.scope().$select, 'sizeSearchInput');

      var searchInput = el.find('.ui-select-search');
      var oldWidth = searchInput.css('width');

      el.find('.ui-select-match-item').first().find('.ui-select-match-close').click();
      expect(el.scope().$select.sizeSearchInput).toHaveBeenCalled();

    });

    it('should update size of search input use container width', function () {
      scope.selection.selectedMultiple = [scope.people[4], scope.people[5]]; //Wladimir & Samantha
      var el = createUiSelectMultiple({
        appendToBody: true
      });

      angular.element(document.body).css("width", "100%");
      angular.element(document.body).css("height", "100%");
      angular.element(document.body).append(el);

      spyOn(el.scope().$select, 'sizeSearchInput');

      var searchInput = el.find('.ui-select-search');
      el.find('.ui-select-match-item').first().find('.ui-select-match-close').click();

      expect(el.scope().$select.sizeSearchInput).toHaveBeenCalled();

      $timeout.flush();

      var newWidth = searchInput[0].clientWidth + searchInput[0].offsetLeft;
      var containerWidth = el[0].clientWidth;
      expect(containerWidth - newWidth).toBeLessThan(10);

    });
    it('should move to last match when pressing BACKSPACE key from search', function () {

      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Backspace);
      expect(isDropdownOpened(el)).toEqual(false);
      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(el.scope().$select.selected.length - 1);

    });

    it('should remove highlighted match when pressing BACKSPACE key from search and decrease activeMatchIndex', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5], scope.people[6]]; //Wladimir, Samantha & Nicole
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Backspace);
      expect(el.scope().$select.selected).toEqual([scope.people[4], scope.people[6]]); //Wladimir & Nicole

      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(0);

    });

    it('should remove highlighted match when pressing DELETE key from search and keep same activeMatchIndex', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5], scope.people[6]]; //Wladimir, Samantha & Nicole
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Delete);
      expect(el.scope().$select.selected).toEqual([scope.people[4], scope.people[6]]); //Wladimir & Nicole

      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(1);

    });

    it('should NOT remove highlighted match when pressing BACKSPACE key on a locked choice', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5], scope.people[6]]; //Wladimir, Samantha & Nicole
      var el = createUiSelectMultiple({ lockChoice: "$item.name == '" + scope.people[6].name + "'" });
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Backspace);
      expect(el.scope().$select.selected).toEqual([scope.people[4], scope.people[5], scope.people[6]]); //Wladimir, Samantha & Nicole

      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(scope.selection.selectedMultiple.length - 1);

    });

    it('should NOT remove highlighted match when pressing DELETE key on a locked choice', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5], scope.people[6]]; //Wladimir, Samantha & Nicole
      var el = createUiSelectMultiple({ lockChoice: "$item.name == '" + scope.people[6].name + "'" });
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Delete);
      expect(el.scope().$select.selected).toEqual([scope.people[4], scope.people[5], scope.people[6]]); //Wladimir, Samantha & Nicole

      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(scope.selection.selectedMultiple.length - 1);

    });


    it('should move to last match when pressing LEFT key from search', function () {

      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Left);
      expect(isDropdownOpened(el)).toEqual(false);
      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(el.scope().$select.selected.length - 1);

    });

    it('should move between matches when pressing LEFT key from search', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5], scope.people[6]]; //Wladimir, Samantha & Nicole
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Left);
      expect(isDropdownOpened(el)).toEqual(false);
      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(el.scope().$select.selected.length - 2);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Left);
      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(0);

    });

    it('should decrease $selectMultiple.activeMatchIndex when pressing LEFT key', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5], scope.people[6]]; //Wladimir, Samantha & Nicole
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      el.scope().$selectMultiple.activeMatchIndex = 3;
      triggerKeydown(searchInput, Key.Left);
      triggerKeydown(searchInput, Key.Left);
      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(1);

    });

    it('should increase $selectMultiple.activeMatchIndex when pressing RIGHT key', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5], scope.people[6]]; //Wladimir, Samantha & Nicole
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      el.scope().$selectMultiple.activeMatchIndex = 0;
      triggerKeydown(searchInput, Key.Right);
      triggerKeydown(searchInput, Key.Right);
      expect(el.scope().$selectMultiple.activeMatchIndex).toBe(2);

    });

    it('should open dropdown when pressing DOWN key', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5]]; //Wladimir & Samantha
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Down);
      expect(isDropdownOpened(el)).toEqual(true);

    });

    it('should search/open dropdown when writing to search input', function () {

      scope.selection.selectedMultiple = [scope.people[5]]; //Wladimir & Samantha
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      el.scope().$select.search = 'r';
      scope.$digest();
      expect(isDropdownOpened(el)).toEqual(true);

    });

    it('should add selected match to selection array', function () {

      scope.selection.selectedMultiple = [scope.people[5]]; //Samantha
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      clickItem(el, 'Wladimir');
      expect(scope.selection.selectedMultiple).toEqual([scope.people[5], scope.people[4]]); //Samantha & Wladimir

    });

    it('should close dropdown after selecting', function () {

      scope.selection.selectedMultiple = [scope.people[5]]; //Samantha
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Down);
      expect(isDropdownOpened(el)).toEqual(true);

      clickItem(el, 'Wladimir');

      expect(isDropdownOpened(el)).toEqual(false);

    });

    it('should not close dropdown after selecting if closeOnSelect=false', function () {

      scope.selection.selectedMultiple = [scope.people[5]]; //Samantha
      var el = createUiSelectMultiple({ closeOnSelect: false });
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Down);
      expect(isDropdownOpened(el)).toEqual(true);

      clickItem(el, 'Wladimir');

      expect(isDropdownOpened(el)).toEqual(true);

    });

    it('should closes dropdown when pressing ESC key from search input', function () {

      scope.selection.selectedMultiple = [scope.people[4], scope.people[5], scope.people[6]]; //Wladimir, Samantha & Nicole
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      expect(isDropdownOpened(el)).toEqual(false);
      triggerKeydown(searchInput, Key.Down);
      expect(isDropdownOpened(el)).toEqual(true);
      triggerKeydown(searchInput, Key.Escape);
      expect(isDropdownOpened(el)).toEqual(false);

    });

    it('should select highlighted match when pressing ENTER key from dropdown', function () {

      scope.selection.selectedMultiple = [scope.people[5]]; //Samantha
      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      triggerKeydown(searchInput, Key.Down);
      triggerKeydown(searchInput, Key.Enter);
      expect(scope.selection.selectedMultiple.length).toEqual(2);

    });

    it('should stop the propagation when pressing ENTER key from dropdown', function () {

      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');
      spyOn(jQuery.Event.prototype, 'preventDefault');
      spyOn(jQuery.Event.prototype, 'stopPropagation');

      triggerKeydown(searchInput, Key.Down);
      triggerKeydown(searchInput, Key.Enter);
      expect(jQuery.Event.prototype.preventDefault).toHaveBeenCalled();
      expect(jQuery.Event.prototype.stopPropagation).toHaveBeenCalled();

    });

    it('should stop the propagation when pressing ESC key from dropdown', function () {

      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');
      spyOn(jQuery.Event.prototype, 'preventDefault');
      spyOn(jQuery.Event.prototype, 'stopPropagation');

      triggerKeydown(searchInput, Key.Down);
      triggerKeydown(searchInput, Key.Escape);
      expect(jQuery.Event.prototype.preventDefault).toHaveBeenCalled();
      expect(jQuery.Event.prototype.stopPropagation).toHaveBeenCalled();

    });

    it('should increase $select.activeIndex when pressing DOWN key from dropdown', function () {

      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      triggerKeydown(searchInput, Key.Down); //Open dropdown

      el.scope().$select.activeIndex = 0;
      triggerKeydown(searchInput, Key.Down);
      triggerKeydown(searchInput, Key.Down);
      expect(el.scope().$select.activeIndex).toBe(2);

    });

    it('should decrease $select.activeIndex when pressing UP key from dropdown', function () {

      var el = createUiSelectMultiple();
      var searchInput = el.find('.ui-select-search');

      triggerKeydown(searchInput, Key.Down); //Open dropdown

      el.scope().$select.activeIndex = 5;
      triggerKeydown(searchInput, Key.Up);
      triggerKeydown(searchInput, Key.Up);
      expect(el.scope().$select.activeIndex).toBe(3);

    });

    it('should render initial selected items', function () {
      scope.selection.selectedMultiple = [scope.people[4], scope.people[5]]; //Wladimir & Samantha
      var el = createUiSelectMultiple();
      expect(el.scope().$select.selected.length).toBe(2);
      expect(el.find('.ui-select-match-item').length).toBe(2);
    });

    it('should parse the items correctly using single property binding', function () {

      scope.selection.selectedMultiple = ['wladimir@email.com', 'samantha@email.com'];

      var el = compileTemplate(
        '<ui-select multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select>'
      );

      expect(el.scope().$select.selected).toEqual([scope.people[4], scope.people[5]]);

    });

    it('should add selected match to selection array using single property binding', function () {

      scope.selection.selectedMultiple = ['wladimir@email.com', 'samantha@email.com'];

      var el = compileTemplate(
        '<ui-select multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select>'
      );

      var searchInput = el.find('.ui-select-search');

      clickItem(el, 'Natasha');

      expect(el.scope().$select.selected).toEqual([scope.people[4], scope.people[5], scope.people[7]]);
      scope.selection.selectedMultiple = ['wladimir@email.com', 'samantha@email.com', 'natasha@email.com'];

    });

    it('should format view value correctly when using single property binding and refresh function', function () {

      scope.selection.selectedMultiple = ['wladimir@email.com', 'samantha@email.com'];

      var el = compileTemplate(
        '<ui-select multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search" \
                refresh="fetchFromServer($select.search)" refresh-delay="0"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select>'
      );

      var searchInput = el.find('.ui-select-search');

      scope.fetchFromServer = function (searching) {

        if (searching == 'n')
          return scope.people;

        if (searching == 'o') {
          scope.people = []; //To simulate cases were previously selected item isnt in the list anymore
        }

      };

      setSearchText(el, 'n');
      clickItem(el, 'Nicole');

      expect(el.find('.ui-select-match-item [uis-transclude-append]:not(.ng-hide)').text())
        .toBe("Wladimir <wladimir@email.com>Samantha <samantha@email.com>Nicole <nicole@email.com>");

      setSearchText(el, 'o');

      expect(el.find('.ui-select-match-item [uis-transclude-append]:not(.ng-hide)').text())
        .toBe("Wladimir <wladimir@email.com>Samantha <samantha@email.com>Nicole <nicole@email.com>");

    });

    it('should watch changes for $select.selected and update formatted value correctly', function () {

      scope.selection.selectedMultiple = ['wladimir@email.com', 'samantha@email.com'];

      var el = compileTemplate(
        '<ui-select multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
                  <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
                  <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                    <div ng-bind-html="person.name | highlight: $select.search"></div> \
                    <div ng-bind-html="person.email | highlight: $select.search"></div> \
                  </ui-select-choices> \
              </ui-select> \
              '
      );

      var el2 = compileTemplate('<span class="resultDiv" ng-bind="selection.selectedMultiple"></span>');

      expect(el.find('.ui-select-match-item [uis-transclude-append]:not(.ng-hide)').text())
        .toBe("Wladimir <wladimir@email.com>Samantha <samantha@email.com>");

      clickItem(el, 'Nicole');

      expect(el.find('.ui-select-match-item [uis-transclude-append]:not(.ng-hide)').text())
        .toBe("Wladimir <wladimir@email.com>Samantha <samantha@email.com>Nicole <nicole@email.com>");

      expect(scope.selection.selectedMultiple.length).toBe(3);

    });

    it('should watch changes for $select.selected and refresh choices correctly', function () {

      scope.selection.selectedMultiple = ['wladimir@email.com', 'samantha@email.com'];

      var el = compileTemplate(
        '<ui-select multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
                  <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
                  <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                    <div ng-bind-html="person.name | highlight: $select.search"></div> \
                    <div ng-bind-html="person.email | highlight: $select.search"></div> \
                  </ui-select-choices> \
              </ui-select> \
              '
      );
      scope.selection.selectedMultiple.splice(0, 1); // Remove Wladimir from selection

      var searchInput = el.find('.ui-select-search');
      triggerKeydown(searchInput, Key.Down); //Open dropdown

      expect(el.find('.ui-select-choices-content').text())
        .toContain("wladimir@email.com");

    });

    it('should ensure the multiple selection limit is respected', function () {

      scope.selection.selectedMultiple = ['wladimir@email.com'];

      var el = compileTemplate(
        '<ui-select multiple limit="2" ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
                  <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
                  <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                    <div ng-bind-html="person.name | highlight: $select.search"></div> \
                    <div ng-bind-html="person.email | highlight: $select.search"></div> \
                  </ui-select-choices> \
              </ui-select> \
              '
      );

      var el2 = compileTemplate('<span class="resultDiv" ng-bind="selection.selectedMultiple"></span>');

      expect(el.find('.ui-select-match-item [uis-transclude-append]:not(.ng-hide)').text())
        .toBe("Wladimir <wladimir@email.com>");

      clickItem(el, 'Samantha');
      expect(el.find('.ui-select-match-item [uis-transclude-append]:not(.ng-hide)').text())
        .toBe("Wladimir <wladimir@email.com>Samantha <samantha@email.com>");

      clickItem(el, 'Nicole');

      expect(el.find('.ui-select-match-item [uis-transclude-append]:not(.ng-hide)').text())
        .toBe("Wladimir <wladimir@email.com>Samantha <samantha@email.com>");

      expect(scope.selection.selectedMultiple.length).toBe(2);

    });

    it('should change viewvalue only once when updating modelvalue', function () {

      scope.selection.selectedMultiple = ['wladimir@email.com', 'samantha@email.com'];

      var el = compileTemplate(
        '<ui-select ng-change="onlyOnce()" multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select> \
          '
      );

      scope.counter = 0;
      scope.onlyOnce = function () {
        scope.counter++;
      };

      clickItem(el, 'Nicole');

      expect(scope.counter).toBe(1);

    });

    it('should retain an invalid view value after refreshing items', function () {
      scope.taggingFunc = function (name) {
        return {
          name: name,
          email: name + '@email.com',
          valid: name === "iamvalid"
        };
      };

      var el = compileTemplate(
        '<ui-select multiple ng-model="selection.selectedMultiple" tagging="taggingFunc" tagging-label="false" test-validator> \
            <ui-select-match placeholder="Pick one...">{{$select.selected.email}}</ui-select-match> \
            <ui-select-choices repeat="person in people | filter: $select.search"> \
              <div ng-bind-html="person.name" | highlight: $select.search"></div> \
              <div ng-bind-html="person.email | highlight: $select.search"></div> \
            </ui-select-choices> \
          </ui-select>'
      );

      clickMatch(el);
      var searchInput = el.find('.ui-select-search');

      setSearchText(el, 'iamvalid');
      triggerKeydown(searchInput, Key.Tab);

      //model value defined because it's valid, view value defined as expected
      var validTag = scope.taggingFunc("iamvalid");
      expect(scope.selection.selectedMultiple).toEqual([jasmine.objectContaining(validTag)]);
      expect($(el).scope().$select.selected).toEqual([jasmine.objectContaining(validTag)]);

      clickMatch(el);
      setSearchText(el, 'notvalid');
      triggerKeydown(searchInput, Key.Tab);

      //model value undefined because it's invalid, view value STILL defined as expected
      var invalidTag = scope.taggingFunc("notvalid");
      expect(scope.selection.selected).toEqual(undefined);
      expect($(el).scope().$select.selected).toEqual([jasmine.objectContaining(validTag), jasmine.objectContaining(invalidTag)]);
    });

    it('should run $formatters when changing model directly', function () {

      scope.selection.selectedMultiple = ['wladimir@email.com', 'samantha@email.com'];

      var el = compileTemplate(
        '<ui-select multiple ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select> \
          '
      );

      // var el2 = compileTemplate('<span class="resultDiv" ng-bind="selection.selectedMultiple"></span>');

      scope.selection.selectedMultiple.push("nicole@email.com");

      scope.$digest();
      scope.$digest(); //2nd $digest needed when using angular 1.3.0-rc.1+, might be related with the fact that the value is an array

      expect(el.find('.ui-select-match-item [uis-transclude-append]:not(.ng-hide)').text())
        .toBe("Wladimir <wladimir@email.com>Samantha <samantha@email.com>Nicole <nicole@email.com>");

    });

    it('should support multiple="multiple" attribute', function () {

      var el = compileTemplate(
        '<ui-select multiple="multiple" ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select> \
          '
      );

      expect(el.scope().$select.multiple).toBe(true);
    });

    it('should preserve the model if tagging is enabled on select multiple', function () {
      scope.selection.selectedMultiple = ["I am not on the list of choices"];

      var el = compileTemplate(
        '<ui-select multiple="multiple" tagging ng-model="selection.selectedMultiple" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select> \
          '
      );

      scope.$digest();

      expect(scope.selection.selectedMultiple)
        .toEqual(["I am not on the list of choices"]);
    });

    it('should not call tagging function needlessly', function () {
      scope.slowTaggingFunc = function (name) {
        // for (var i = 0; i < 100000000; i++);
        return { name: name };
      };
      spyOn(scope, 'slowTaggingFunc').and.callThrough();

      var el = createUiSelectMultiple({ tagging: 'slowTaggingFunc' });

      showChoicesForSearch(el, 'Foo');
      expect(el.find('.ui-select-choices-row-inner').size()).toBe(6);

      showChoicesForSearch(el, 'a');
      expect(el.find('.ui-select-choices-row-inner').size()).toBe(9);

      expect(scope.slowTaggingFunc.calls.count()).toBe(2);
      expect(scope.slowTaggingFunc.calls.count()).not.toBe(15);
    });

    it('should allow decline tags when tagging function returns null in multiple select mode', function () {
      scope.taggingFunc = function (name) {
        if (name == 'idontexist') return null;
        return {
          name: name,
          email: name + '@email.com',
          group: 'Foo',
          age: 12
        };
      };

      var el = createUiSelectMultiple({ tagging: 'taggingFunc' });

      showChoicesForSearch(el, 'amalie');
      expect(el.find('.ui-select-choices-row-inner').size()).toBe(2);
      expect(el.scope().$select.items[0]).toEqual(jasmine.objectContaining({ name: 'amalie', isTag: true }));
      expect(el.scope().$select.items[1]).toEqual(jasmine.objectContaining({ name: 'Amalie' }));

      showChoicesForSearch(el, 'idoexist');
      expect(el.find('.ui-select-choices-row-inner').size()).toBe(1);
      expect(el.find('.ui-select-choices-row-inner').is(':contains(idoexist@email.com)')).toBeTruthy();

      showChoicesForSearch(el, 'idontexist');
      expect(el.find('.ui-select-choices-row-inner').size()).toBe(0);
    });

    it('should allow creating tag in multi select mode with tagging and group-by enabled', function () {
      scope.taggingFunc = function (name) {
        return {
          name: name,
          email: name + '@email.com',
          group: 'Foo',
          age: 12
        };
      };

      var el = createUiSelectMultiple({ tagging: 'taggingFunc', groupBy: "'age'" });

      showChoicesForSearch(el, 'amal');
      expect(el.find('.ui-select-choices-row-inner').size()).toBe(2);
      expect(el.scope().$select.items[0]).toEqual(jasmine.objectContaining({ name: 'amal', email: 'amal@email.com', isTag: true }));
      expect(el.scope().$select.items[1]).toEqual(jasmine.objectContaining({ name: 'Amalie', email: 'amalie@email.com' }));
    });


    it('should have tolerance for undefined values', function () {

      scope.modelValue = undefined;

      var el = compileTemplate(
        '<ui-select multiple ng-model="modelValue" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select> \
          '
      );

      expect($(el).scope().$select.selected).toEqual([]);
    });

    it('should have tolerance for null values', function () {

      scope.modelValue = null;

      var el = compileTemplate(
        '<ui-select multiple ng-model="modelValue" theme="bootstrap" style="width: 800px;"> \
              <ui-select-match placeholder="Pick one...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match> \
              <ui-select-choices repeat="person.email as person in people | filter: $select.search"> \
                <div ng-bind-html="person.name | highlight: $select.search"></div> \
                <div ng-bind-html="person.email | highlight: $select.search"></div> \
              </ui-select-choices> \
          </ui-select> \
          '
      );

      expect($(el).scope().$select.selected).toEqual([]);
    });

    it('should allow paste tag from clipboard', function () {
      scope.taggingFunc = function (name) {
        return {
          name: name,
          email: name + '@email.com',
          group: 'Foo',
          age: 12
        };
      };

      var el = createUiSelectMultiple({ tagging: 'taggingFunc', taggingTokens: ",|ENTER" });
      clickMatch(el);
      triggerPaste(el.find('input'), 'tag1');

      expect($(el).scope().$select.selected.length).toBe(1);
      expect($(el).scope().$select.selected[0].name).toBe('tag1');
    });

    it('should allow paste tag from clipboard for generic ClipboardEvent', function () {
      scope.taggingFunc = function (name) {
        return {
          name: name,
          email: name + '@email.com',
          group: 'Foo',
          age: 12
        };
      };

      var el = createUiSelectMultiple({ tagging: 'taggingFunc', taggingTokens: ",|ENTER" });
      clickMatch(el);
      triggerPaste(el.find('input'), 'tag1', true);

      expect($(el).scope().$select.selected.length).toBe(1);
      expect($(el).scope().$select.selected[0].name).toBe('tag1');
    });

    it('should allow paste multiple tags', function () {
      scope.taggingFunc = function (name) {
        return {
          name: name,
          email: name + '@email.com',
          group: 'Foo',
          age: 12
        };
      };

      var el = createUiSelectMultiple({ tagging: 'taggingFunc', taggingTokens: ",|ENTER" });
      clickMatch(el);
      triggerPaste(el.find('input'), ',tag1,tag2,tag3,,tag5,');

      expect($(el).scope().$select.selected.length).toBe(5);
    });

    it('should allow paste multiple tags with generic ClipboardEvent', function () {
      scope.taggingFunc = function (name) {
        return {
          name: name,
          email: name + '@email.com',
          group: 'Foo',
          age: 12
        };
      };

      var el = createUiSelectMultiple({ tagging: 'taggingFunc', taggingTokens: ",|ENTER" });
      clickMatch(el);
      triggerPaste(el.find('input'), ',tag1,tag2,tag3,,tag5,', true);

      expect($(el).scope().$select.selected.length).toBe(5);
    });

    it('should split pastes on ENTER (and with undefined tagging function)', function () {
      var el = createUiSelectMultiple({ tagging: true, taggingTokens: "ENTER|," });
      clickMatch(el);
      triggerPaste(el.find('input'), "tag1\ntag2\ntag3");

      expect($(el).scope().$select.selected.length).toBe(3);
    });

    it('should split pastes on TAB', function () {
      var el = createUiSelectMultiple({ tagging: true, taggingTokens: "TAB|," });
      clickMatch(el);
      triggerPaste(el.find('input'), "tag1\ttag2\ttag3");

      expect($(el).scope().$select.selected.length).toBe(3);
    });

    it('should split pastes on tagging token that is not the first token', function () {
      var el = createUiSelectMultiple({ tagging: true, taggingTokens: ",|ENTER|TAB" });
      clickMatch(el);
      triggerPaste(el.find('input'), "tag1\ntag2\ntag3\ntag4");

      expect($(el).scope().$select.selected).toEqual(['tag1', 'tag2', 'tag3', 'tag4']);
    });

    it('should split pastes only on first tagging token found in paste string', function () {
      var el = createUiSelectMultiple({ tagging: true, taggingTokens: ",|ENTER|TAB" });
      clickMatch(el);
      triggerPaste(el.find('input'), "tag1\ntag2\ntag3\ttag4");

      expect($(el).scope().$select.selected).toEqual(['tag1', 'tag2', 'tag3\ttag4']);
    });

    it('should allow paste with tagging-tokens and tagging-label=="false"', function () {
      var el = createUiSelectMultiple({ tagging: true, taggingLabel: false, taggingTokens: "," });
      clickMatch(el);
      triggerPaste(el.find('input'), 'tag1');

      expect($(el).scope().$select.selected).toEqual(['tag1']);
    });

    it('should add an id to the search input field', function () {
      var el = createUiSelectMultiple({ inputId: 'inid' });
      var searchEl = $(el).find('input.ui-select-search');
      expect(searchEl.length).toEqual(1);
      expect(searchEl[0].id).toEqual('inid');
    });

    it('should properly identify as empty if required', function () {
      var el = createUiSelectMultiple({ required: true });
      expect(el.hasClass('ng-empty')).toBeTruthy();
    });

    it('should properly identify as not empty if required', function () {
      var el = createUiSelectMultiple({ required: true });
      clickItem(el, 'Nicole');
      clickItem(el, 'Samantha');
      expect(el.hasClass('ng-not-empty')).toBeTruthy();
    });

    it('should be able to re-select the item with removeselected set to false', function () {
      scope.selection.selectedMultiple = [scope.people[4], scope.people[5]]; //Wladimir & Samantha
      var el = createUiSelectMultiple({ removeSelected: true });
      expect(el.scope().$select.selected.length).toBe(2);
      el.find('.ui-select-match-item').first().find('.ui-select-match-close').click();
      expect(el.scope().$select.selected.length).toBe(1);
      clickItem(el, 'Wladimir');
      expect(el.scope().$select.selected.length).toBe(2);
    });

    it('should set only 1 item in the selected items when limit = 1', function () {
      var el = createUiSelectMultiple({ limit: 1 });
      clickItem(el, 'Wladimir');
      clickItem(el, 'Natasha');
      expect(el.scope().$select.selected.length).toEqual(1);
    });

    it('should only have 1 item selected and onSelect function should only be handled once.', function () {
      scope.onSelectFn = function ($item, $model) {
        scope.$item = $item;
        scope.$model = $model;
      };
      var el = createUiSelectMultiple({ limit: 1, onSelect: 'onSelectFn($item, $model)' });

      expect(scope.$item).toBeFalsy();
      expect(scope.$model).toBeFalsy();

      clickItem(el, 'Samantha');
      $timeout.flush();
      clickItem(el, 'Natasha');
      $timeout.flush();
      expect(scope.selection.selectedMultiple[0].name).toBe('Samantha');
      expect(scope.$model.name).toEqual('Samantha');
      expect(el.scope().$select.selected.length).toEqual(1);
    });

    it('should only have 2 items selected and onSelect function should be handeld.', function () {
      scope.onSelectFn = function ($item, $model) {
        scope.$item = $item;
        scope.$model = $model;
      };
      var el = createUiSelectMultiple({ onSelect: 'onSelectFn($item, $model)' });

      expect(scope.$item).toBeFalsy();
      expect(scope.$model).toBeFalsy();

      clickItem(el, 'Samantha');
      $timeout.flush();
      expect(scope.$model.name).toEqual('Samantha');
      clickItem(el, 'Natasha');
      $timeout.flush();
      expect(scope.$model.name).toEqual('Natasha');
      expect(el.scope().$select.selected.length).toEqual(2);
    });

    describe('Test key down key up and activeIndex should skip disabled choice for uiMultipleSelect', function () {
      it('should ignored disabled items going up', function () {
        var el = createUiSelect({ uiDisableChoice: "person.age == 12" });
        openDropdown(el);
        var searchInput = el.find('.ui-select-search');
        expect(el.scope().$select.activeIndex).toBe(0);
        triggerKeydown(searchInput, Key.Down);
        expect(el.scope().$select.activeIndex).toBe(2);
        triggerKeydown(searchInput, Key.Up);
        expect(el.scope().$select.activeIndex).toBe(0);
      });

      it('should ignored disabled items going up with tagging on', function () {
        var el = createUiSelectMultiple({ uiDisableChoice: "person.age == 12", tagging: true });
        openDropdown(el);
        var searchInput = el.find('.ui-select-search');
        expect(el.scope().$select.activeIndex).toBe(-1);
        triggerKeydown(searchInput, Key.Down);
        expect(el.scope().$select.activeIndex).toBe(2);
        triggerKeydown(searchInput, Key.Up);
        expect(el.scope().$select.activeIndex).toBe(-1);
      });

      it('should ignored disabled items going down', function () {
        var el = createUiSelectMultiple({ uiDisableChoice: "person.age == 12" });
        openDropdown(el);
        var searchInput = el.find('.ui-select-search');
        expect(el.scope().$select.activeIndex).toBe(0);
        triggerKeydown(searchInput, Key.Down);
        triggerKeydown(searchInput, Key.Enter);
        expect(el.scope().$select.activeIndex).toBe(2);
      });

      it('should ignored disabled items going down with tagging on', function () {
        var el = createUiSelectMultiple({ uiDisableChoice: "person.age == 12", tagging: true });
        openDropdown(el);
        var searchInput = el.find('.ui-select-search');
        expect(el.scope().$select.activeIndex).toBe(-1);
        triggerKeydown(searchInput, Key.Down);
        expect(el.scope().$select.activeIndex).toBe(2);
        triggerKeydown(searchInput, Key.Up);
        expect(el.scope().$select.activeIndex).toBe(-1);
      });

      it('should ignore disabled items, going down with remove-selected on false', function () {
        var el = createUiSelectMultiple({ uiDisableChoice: "person.age == 12", removeSelected: false });
        openDropdown(el);
        var searchInput = el.find('.ui-select-search');
        expect(el.scope().$select.activeIndex).toBe(0);
        triggerKeydown(searchInput, Key.Down);
        triggerKeydown(searchInput, Key.Enter);
        expect(el.scope().$select.activeIndex).toBe(2);
      });
    });

    describe('resetSearchInput option multiple', function () {
      it('should be true by default', function () {
        expect(createUiSelectMultiple().scope().$select.resetSearchInput).toBe(true);
      });

      it('should be false when set.', function () {
        expect(createUiSelectMultiple({ resetSearchInput: false }).scope().$select.resetSearchInput).toBe(false);
      });
    });


    describe('Reset the search value', function () {
      it('should clear the search input when resetSearchInput is true', function () {
        var el = createUiSelectMultiple();
        $(el).scope().$select.search = 'idontexist';
        $(el).scope().$select.select('idontexist');
        expect($(el).scope().$select.search).toEqual('');
      });

      it('should not clear the search input when resetSearchInput is false', function () {
        var el = createUiSelectMultiple({ resetSearchInput: false });
        $(el).scope().$select.search = 'idontexist';
        $(el).scope().$select.select('idontexist');
        expect($(el).scope().$select.search).toEqual('idontexist');
      });

      it('should clear the search input when resetSearchInput is default set', function () {
        var el = createUiSelectMultiple();
        $(el).scope().$select.search = 'idontexist';
        $(el).scope().$select.select('idontexist');
        expect($(el).scope().$select.search).toEqual('');
      });
    });
  });

  it('should add an id to the search input field', function () {
    var el = createUiSelect({ inputId: 'inid' });
    var searchEl = $(el).find('input.ui-select-search');
    expect(searchEl.length).toEqual(1);
    expect(searchEl[0].id).toEqual('inid');
  });

  describe('default configuration via uiSelectConfig', function () {

    describe('searchEnabled option', function () {

      function setupWithoutAttr() {
        return compileTemplate(
          '<ui-select ng-model="selection.selected"> \
            <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
            <ui-select-choices repeat="person in people | filter: $select.search"> \
              <div ng-bind-html="person.name | highlight: $select.search"></div> \
              <div ng-bind-html="person.email | highlight: $select.search"></div> \
            </ui-select-choices> \
          </ui-select>'
        );
      }

      function setupWithAttr(searchEnabled) {
        return compileTemplate(
          '<ui-select ng-model="selection.selected" search-enabled="' + searchEnabled + '"> \
            <ui-select-match placeholder="Pick one...">{{$select.selected.name}}</ui-select-match> \
            <ui-select-choices repeat="person in people | filter: $select.search"> \
              <div ng-bind-html="person.name | highlight: $select.search"></div> \
              <div ng-bind-html="person.email | highlight: $select.search"></div> \
            </ui-select-choices> \
          </ui-select>'
        );
      }

      it('should be true by default', function () {
        var el = setupWithoutAttr();
        expect(el.scope().$select.searchEnabled).toBe(true);
      });

      it('should disable search if default set to false', function () {
        var uiSelectConfig = $injector.get('uiSelectConfig');
        uiSelectConfig.searchEnabled = false;

        var el = setupWithoutAttr();
        expect(el.scope().$select.searchEnabled).not.toBe(true);
      });

      it('should be overridden by inline option search-enabled=true', function () {
        var uiSelectConfig = $injector.get('uiSelectConfig');
        uiSelectConfig.searchEnabled = false;

        var el = setupWithAttr(true);
        expect(el.scope().$select.searchEnabled).toBe(true);
      });

      it('should be overridden by inline option search-enabled=false', function () {
        var uiSelectConfig = $injector.get('uiSelectConfig');
        uiSelectConfig.searchEnabled = true;

        var el = setupWithAttr(false);
        expect(el.scope().$select.searchEnabled).not.toBe(true);
      });
    });
  });

  describe('resetSearchInput option', function () {
    it('should be true by default', function () {
      expect(createUiSelect().scope().$select.resetSearchInput).toBe(true);
    });

    it('should be overridden by inline option reset-search-input=false', function () {
      expect(createUiSelect({ resetSearchInput: false }).scope().$select.resetSearchInput).toBe(false);
    });

    describe('Reset the search value', function () {
      it('should clear the search input when resetSearchInput is true', function () {
        var control = createUiSelect();
        setSearchText(control, 'idontexist');
        clickMatch(control);
        expect(control.scope().$select.search).toEqual('');
      });

      it('should not clear the search input', function () {
        var control = createUiSelect({ resetSearchInput: false });
        setSearchText(control, 'idontexist');
        clickMatch(control);
        expect(control.scope().$select.search).toEqual('idontexist');
      });

      it('should clear the search input when resetSearchInput is true and closeOnSelect is true', function () {
        var control = createUiSelect({ closeOnSelect: true });
        setSearchText(control, 'idontexist');
        clickMatch(control);
        expect(control.scope().$select.search).toEqual('');
      });

      it('should clear the search input when resetSearchInput is true and closeOnSelect is false', function () {
        var control = createUiSelect({ closeOnSelect: false });
        setSearchText(control, 'idontexist');
        clickMatch(control);
        expect(control.scope().$select.search).toEqual('');
      });

      it('should not clear the search input when resetSearchInput is false and closeOnSelect is false', function () {
        var control = createUiSelect({ resetSearchInput: false, closeOnSelect: false });
        setSearchText(control, 'idontexist');
        clickMatch(control);
        expect(control.scope().$select.search).toEqual('idontexist');
      });

      it('should not clear the search input when resetSearchInput is false and closeOnSelect is true', function () {
        var control = createUiSelect({ resetSearchInput: false, closeOnSelect: true });
        setSearchText(control, 'idontexist');
        clickMatch(control);
        expect(control.scope().$select.search).toEqual('idontexist');
      });
    });
  });

  describe('accessibility', function () {
    it('should have baseTitle in scope', function () {
      expect(createUiSelect().scope().$select.baseTitle).toBe('Select box');
      expect(createUiSelect().scope().$select.focusserTitle).toBe('Select box focus');
      expect(createUiSelect({ title: 'Choose a person' }).scope().$select.baseTitle).toBe('Choose a person');
      expect(createUiSelect({ title: 'Choose a person' }).scope().$select.focusserTitle).toBe('Choose a person focus');
    });

    it('should have aria-label on all input and button elements', function () {
      checkTheme();
      checkTheme('select2');
      checkTheme('selectize');
      checkTheme('bootstrap');

      function checkTheme(theme) {
        var el = createUiSelect({ theme: theme });
        checkElements(el.find('input'));
        checkElements(el.find('button'));

        function checkElements(els) {
          for (var i = 0; i < els.length; i++) {
            expect(els[i].attributes['aria-label']).toBeTruthy();
          }
        }
      }
    });
  });

  describe('select with the append to body option', function () {
    var body;

    beforeEach(inject(function ($document) {
      body = $document.find('body')[0];
    }));

    it('should only be moved to the body when the appendToBody option is true', function () {
      var el = createUiSelect({ appendToBody: false });
      openDropdown(el);
      expect(el.parent()[0]).not.toBe(body);
    });

    it('should be moved to the body when the appendToBody is true in uiSelectConfig', inject(function (uiSelectConfig) {
      uiSelectConfig.appendToBody = true;
      var el = createUiSelect();
      openDropdown(el);
      expect(el.parent()[0]).toBe(body);
    }));

    it('should be moved to the body when opened', function () {
      var el = createUiSelect({ appendToBody: true });
      openDropdown(el);
      expect(el.parent()[0]).toBe(body);
      closeDropdown(el);
      expect(el.parent()[0]).not.toBe(body);
    });

    it('should remove itself from the body when the scope is destroyed', function () {
      var el = createUiSelect({ appendToBody: true });
      openDropdown(el);
      expect(el.parent()[0]).toBe(body);
      el.scope().$destroy();
      expect(el.parent()[0]).not.toBe(body);
    });

    it('should have specific position and dimensions', function () {
      var el = createUiSelect({ appendToBody: true });
      var originalPosition = el.css('position');
      var originalTop = el.css('top');
      var originalLeft = el.css('left');
      var originalWidth = el.css('width');
      openDropdown(el);
      expect(el.css('position')).toBe('absolute');
      expect(el.css('top')).toBe('100px');
      expect(el.css('left')).toBe('200px');
      expect(el.css('width')).toBe('300px');
      closeDropdown(el);
      expect(el.css('position')).toBe(originalPosition);
      expect(el.css('top')).toBe(originalTop);
      expect(el.css('left')).toBe(originalLeft);
      expect(el.css('width')).toBe(originalWidth);
    });
  });

  describe('highlight filter', function () {
    var highlight;

    beforeEach(function () {
      highlight = $injector.get('highlightFilter');
    });

    it('returns the item if there is no match', function () {
      var query = 'January';
      var item = 'December';

      expect(highlight(item, query)).toBe('December');
    });

    it('wraps search strings matches in ui-select-highlight class', function () {
      var query = 'er';
      var item = 'December';

      expect(highlight(item, query)).toBe('Decemb<span class="ui-select-highlight">er</span>');
    });

    it('properly highlights numeric items', function () {
      var query = '15';
      var item = 2015;

      expect(highlight(item, query)).toBe('20<span class="ui-select-highlight">15</span>');
    });

    it('properly works with numeric queries', function () {
      var query = 15;
      var item = 2015;

      expect(highlight(item, query)).toBe('20<span class="ui-select-highlight">15</span>');
    });
  });

  describe('Test Spinner for promises', function () {
    var deferred;

    function getFromServer() {
      deferred = $q.defer();
      return deferred.promise;
    }
    it('should have a default value of false', function () {
      var control = createUiSelect();
      expect(control.scope().$select.spinnerEnabled).toEqual(false);
    });

    it('should have a set a value of true', function () {
      var control = createUiSelect({ spinnerEnabled: true });
      expect(control.scope().$select.spinnerEnabled).toEqual(true);
    });

    it('should have a default value of glyphicon-refresh ui-select-spin', function () {
      var control = createUiSelect();
      expect(control.scope().$select.spinnerClass).toEqual('glyphicon glyphicon-refresh ui-select-spin');
    });

    it('should have set a custom class value of randomclass', function () {
      var control = createUiSelect({ spinnerClass: 'randomclass' });
      expect(control.scope().$select.spinnerClass).toEqual('randomclass');
    });

    it('should not display spinner when disabled', function () {
      scope.getFromServer = getFromServer;
      var el = createUiSelect({ theme: 'bootstrap', refresh: "getFromServer($select.search)", refreshDelay: 0 });
      openDropdown(el);
      var spinner = el.find('.ui-select-refreshing');
      expect(spinner.hasClass('ng-hide')).toBe(true);
      setSearchText(el, 'a');
      expect(spinner.hasClass('ng-hide')).toBe(true);
      deferred.resolve();
      scope.$digest();
      expect(spinner.hasClass('ng-hide')).toBe(true);
    });

    it('should display spinner when enabled', function () {
      scope.getFromServer = getFromServer;
      var el = createUiSelect({ spinnerEnabled: true, theme: 'bootstrap', refresh: "getFromServer($select.search)", refreshDelay: 0 });
      openDropdown(el);
      var spinner = el.find('.ui-select-refreshing');
      expect(spinner.hasClass('ng-hide')).toBe(true);
      setSearchText(el, 'a');
      expect(spinner.hasClass('ng-hide')).toBe(false);
      deferred.resolve();
      scope.$digest();
      expect(spinner.hasClass('ng-hide')).toBe(true);
    });

    it('should not display spinner when enabled', function () {
      var el = createUiSelect({ spinnerEnabled: true, theme: 'bootstrap', spinnerClass: 'randomclass' });
      openDropdown(el);
      var spinner = el.find('.ui-select-refreshing');
      setSearchText(el, 'a');
      expect(el.scope().$select.spinnerClass).toBe('randomclass');
    });
  });

  describe('With refresh on active', function () {
    it('should refresh when is activated', function () {
      scope.fetchFromServer = function () { };
      var el = createUiSelect({ refresh: "fetchFromServer($select.search)", refreshDelay: 0 });
      spyOn(scope, 'fetchFromServer');
      expect(el.scope().$select.open).toEqual(false);
      el.scope().$select.activate();
      $timeout.flush();
      expect(el.scope().$select.open).toEqual(true);
      expect(scope.fetchFromServer.calls.any()).toEqual(true);
    });


    it('should refresh when open is set to true', function () {
      scope.fetchFromServer = function () { };
      var el = createUiSelect({ refresh: "fetchFromServer($select.search)", refreshDelay: 0 });
      spyOn(scope, 'fetchFromServer');
      expect(el.scope().$select.open).toEqual(false);
      openDropdown(el);
      $timeout.flush();
      expect(el.scope().$select.open).toEqual(true);
      expect(scope.fetchFromServer.calls.any()).toEqual(true);
    });
  });
  describe('Test key down key up and activeIndex should skip disabled choice', function () {
    it('should ignore disabled items, going down', function () {
      var el = createUiSelect({ uiDisableChoice: "person.age == 12" });
      openDropdown(el);
      var searchInput = el.find('.ui-select-search');
      expect(el.scope().$select.activeIndex).toBe(0);
      triggerKeydown(searchInput, Key.Down);
      triggerKeydown(searchInput, Key.Enter);
      expect(el.scope().$select.activeIndex).toBe(2);
    });

    it('should ignore disabled items, going up', function () {
      var el = createUiSelect({ uiDisableChoice: "person.age == 12" });
      openDropdown(el);
      var searchInput = el.find('.ui-select-search');
      expect(el.scope().$select.activeIndex).toBe(0);
      triggerKeydown(searchInput, Key.Down);
      expect(el.scope().$select.activeIndex).toBe(2);
      triggerKeydown(searchInput, Key.Up);
      expect(el.scope().$select.activeIndex).toBe(0);
    });

    it('should ignored disabled items going up with tagging on', function () {
      var el = createUiSelect({ uiDisableChoice: "person.age == 12", tagging: true });
      openDropdown(el);
      var searchInput = el.find('.ui-select-search');
      expect(el.scope().$select.activeIndex).toBe(-1);
      triggerKeydown(searchInput, Key.Down);
      expect(el.scope().$select.activeIndex).toBe(2);
      triggerKeydown(searchInput, Key.Up);
      expect(el.scope().$select.activeIndex).toBe(-1);
    });

    it('should ignored disabled items in the down direction with tagging on', function () {
      var el = createUiSelect({ uiDisableChoice: "person.age == 12", tagging: true });
      openDropdown(el);
      var searchInput = el.find('.ui-select-search');
      expect(el.scope().$select.activeIndex).toBe(-1);
      triggerKeydown(searchInput, Key.Down);
      triggerKeydown(searchInput, Key.Enter);
      expect(el.scope().$select.activeIndex).toBe(2);
    });

    it('should ignored disabled items going up with tagging on and custom tag', function () {
      var el = createUiSelect({ uiDisableChoice: "person.age == 12", tagging: true, taggingLabel: 'custom tag' });
      openDropdown(el);
      var searchInput = el.find('.ui-select-search');
      expect(el.scope().$select.activeIndex).toBe(-1);
      triggerKeydown(searchInput, Key.Down);
      triggerKeydown(searchInput, Key.Enter);
      expect(el.scope().$select.activeIndex).toBe(2);
    });

    it('should ignore disabled items, going down with remove-selected on false', function () {
      var el = createUiSelect({ uiDisableChoice: "person.age == 12", removeSelected: false });
      openDropdown(el);
      var searchInput = el.find('.ui-select-search');
      expect(el.scope().$select.activeIndex).toBe(0);
      triggerKeydown(searchInput, Key.Down);
      triggerKeydown(searchInput, Key.Enter);
      expect(el.scope().$select.activeIndex).toBe(2);
    });
  });
});
