/*!
 * CUI 2.6.0-rc.1
 *
 * © 2015 Dell Inc.
 * ALL RIGHTS RESERVED.
 *
 */
(function() {
  var base, cui, module,
    __slice = [].slice;

  window.__cui = {
    "version": "2.6.0",
    "major": "2",
    "minor": "6",
    "patch": "0",
    "release": "rc.1",
    "full": "2.6.0-rc.1",
    "releaseDate": "1/15/2015"
  };

  cui = angular.module('cui', ["cui.controls.alert", "cui.controls.badge", "cui.controls.breadcrumb", "cui.controls.button", "cui.controls.calendar", "cui.controls.checkbox", "cui.controls.collapse", "cui.controls.combobox", "cui.controls.dataGrid", "cui.controls.datePicker", "cui.controls.dropDownButton", "cui.controls.dropDownList", "cui.controls.icon", "cui.controls.masthead", "cui.controls.memo", "cui.controls.menu", "cui.controls.navigationList", "cui.controls.pane", "cui.controls.progressBar", "cui.controls.radio", "cui.controls.richTextEditor", "cui.controls.spinner", "cui.controls.splitButton", "cui.controls.table", "cui.controls.tabset", "cui.controls.textarea", "cui.controls.textbox", "cui.controls.time", "cui.controls.tip", "cui.controls.tooltip", "cui.controls.tree", "cui.controls.uiSelect", "cui.modules.aboutBox", "cui.modules.applicationFrame", "cui.modules.masterDetail", "cui.modules.startScreen", "cui.modules.wizard", "cui.modules.wizard", "cui.services.alertService", "cui.services.dataSource", "cui.services.dialog", "cui.services.loading", "cui.services.modal", "cui.services.modal", "cui.services.modal", "cui.services.modal", "cui.services.position", "cui.services.transition"]);

  angular.module('cui.base', ['cui.templates', 'smartTable.table', 'pascalprecht.translate', 'keyboard', 'dateParser', 'ui.select', 'ngSanitize']).value('baseTemplatePath', '__cui/').config(function($translateProvider) {
    $translateProvider.translations('en', {
      "CUI_LOADING_TEXT": "Loading, please wait...",
      "CUI_DATAGRID_NO_DATA": "No data entries exist",
      "CUI_DATAGRID_SEARCH_PLACEHOLDER": "Search",
      "CUI_ABOUTBOX_CLOSE_BUTTON": "Done",
      "CUI_ABOUTBOX_LICENSES_TITLE": "Licenses",
      "CUI_ABOUTBOX_LINCENSE_TH": "License",
      "CUI_ABOUTBOX_TYPE_TH": "Type",
      "CUI_ABOUTBOX_EXPIRES_TH": "Expires",
      "CUI_ABOUTBOX_SEATS_LICENSED_TH": "Seats Licensed",
      "CUI_ABOUTBOX_SEATS_USED_TH": "Seats Used",
      "CUI_ABOUTBOX_CONTACT_TITLE": "Contact Us",
      "CUI_ABOUTBOX_CONTACT_TEXT": "Dell listens to customers and delivers worldwide innovative technology, business solutions and services they trust and value. For more information, visit <a target='_blank' href='http://www.software.dell.com'>www.software.dell.com</a>.",
      "CUI_ABOUTBOX_TECHNICAL_TITLE": "Technical Support:",
      "CUI_ABOUTBOX_TECHNICAL_LINK": "<a href='https://support.software.dell.com/essentials/contact-technical-support' title='Contact Support' target='_blank'>Online Support</a>",
      "CUI_ABOUTBOX_PRODUCT_TITLE": "Product Questions and Sales:",
      "CUI_ABOUTBOX_PRODUCT_PHONE": "<a class='tel' href='tel:8003069329'>(800) 306 - 9329</a>",
      "CUI_ABOUTBOX_EMAIL_TITLE": "Email:",
      "CUI_ABOUTBOX_EMAIL_LINK": "<a href='mailto:info@software.dell.com'>info@software.dell.com</a>",
      "CUI_ABOUTBOX_APPLICATION_NAME": "Application Name in Language File",
      "CUI_ABOUTBOX_VERSION": "Version 2.0",
      "CUI_ABOUTBOX_PATENT_STRING": "Patent String from Language File",
      "CUI_ABOUTBOX_DELL_TRADEMARK": "Dell, and the Dell logo are trademarks of Dell Inc. Other trademarks and trade names may be used in this document to refer to either the entities claiming the marks and names or their products. Dell disclaims any proprietary interest in the marks and names of others.",
      "CUI_WIZARD_BACK": "Back",
      "CUI_WIZARD_NEXT": "Next",
      "CUI_WIZARD_SAVE_AND_NEXT": "Save & Next",
      "CUI_WIZARD_FINISH": "Finish",
      "CUI_WIZARD_CANCEL": "Cancel",
      "CUI_WIZARD_STEP": "Step {{current}} of {{total}}",
      "CUI_STARTSCREEN_DOMAIN_LABEL": "Domain",
      "CUI_STARTSCREEN_USERNAME_LABEL": "Username",
      "CUI_STARTSCREEN_PASSWORD_LABEL": "Password",
      "CUI_DELL_COPYRIGHT": "© 2014 Dell Inc. ALL RIGHTS RESERVED",
      "CUI_STARTSCREEN_SIGN_IN_LABEL": "Sign In",
      "CUI_STARTSCREEN_REMEMBER_ME": "Remember Me",
      "CUI_CALENDAR_TODAY": "Today",
      "CUI_CALENDAR_PREVIOUS_YEAR": "Skip to previous year",
      "CUI_CALENDAR_PREVIOUS_MONTH": "Skip to previous month",
      "CUI_CALENDAR_NEXT_MONTH": "Skip to next month",
      "CUI_CALENDAR_NEXT_YEAR": "Skip to next year",
      "CUI_TIME_HOUR_INCREMENT": "Increment {{hourIncrement}} hours",
      "CUI_TIME_HOUR_DECREMENT": "Decrement -{{hourIncrement}} hours",
      "CUI_TIME_MINUTE_INCREMENT": "Increment {{minuteIncrement}} minutes",
      "CUI_TIME_MINUTE_DECREMENT": "Decrement -{{minuteIncrement}} minutes",
      "CUI_TIME_SECOND_INCREMENT": "Increment {{secondIncrement}} seconds",
      "CUI_TIME_SECOND_DECREMENT": "Decrement -{{secondIncrement}} seconds",
      "CUI_TIME_MERIDIAN": "Toggle AM/PM"
    });
    return $translateProvider.preferredLanguage('en');
  });


  /**
    @ngdoc directive
    @name alert
   
    @description Alert messages are displayed in a modeless pane at the top of the page. They are triggered by a system event or in response to a user action. Alerts are not explicitly requested by the user.
  
  Messages and links for additional information should be displayed on the left. Action buttons should be displayed on the right.
  
  <h3>Events</h3>
  
    ** `cui:alert:dismiss`: listened for to close the alert nicely (with animation).
    ** `cui:alert:dismissed`: emitted after the alert is dismissed (after animation, right before `$destroy`ed and removed from the DOM).
  
    @module cui.controls
    @src controls/alert/alert.coffee
    @controlType presentational
   
    @restrict E
    @transclude Alert message
   
    @param {boolean=} dismissable Whether to show the button to manually close the alert. Defaults to `true`. If `false`, the alert can still be dismissed programatically (via `cui:alert:dismiss`).
   
    @param {string=} label The bold text providing the alert type preceding the further transcluded information.
   
  Default labels for each `cui-type`:
  
  ** `'danger'`: `'Critical Error'`
  ** `'warning'`: `'Warning'`
  ** `'unknown'`: `'There are multiple errors'`
  ** `'success'`: `'Success'`
  ** Other: `'Note'`
   
   
    @param {string=} icon What icon you want to show, see {link module:cui.controls:Icon}
  
  Default icons for each type:
   
  ** `'danger'`: `'exclamation-sign'`
  ** `'warning'`: `'warning-sign'`
  ** `'unknown'`: `'No icon'`
  ** `'success'`: `'ok-sign'`
  ** Other: `'ok-sign'`
   
    @param {string=} cuiType Either `unknown`, `warning`, `success`, or `critical`.  Defaults to `note` version.
   
    @param {number=} destroyAfter The time, in milliseconds, to wait before destroying the alert. (If not specified, the alert will exist until manually destroyed.)
   
    @example <h3>Basic alert</h3>
  <example name='alert'>
    <file name='index.html'>
      <cui-alert>This is a basic alert</cui-alert>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
    
        })
    </file>
  </example>
   
    @example
    <h3>Basic nondismissable alert</h3>
  <example name='alert'>
    <file name='index.html'>
      <cui-alert dismissable=false>This is a basic, nondismissable alert</cui-alert>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
    
        })
    </file>
  </example>
   
    @example
    <h3>Alert with custom label</h3>
  <example name='alertLabel'>
    <file name='index.html'>
      <cui-alert label='Achtung'>Das Label wird angepasst.</cui-alert>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
    
        })
    </file>
  </example>
   
    @example
    <h3>Alert types</h3>
  <example name='alertTypes'>
    <file name='index.html'>
      <cui-alert cui-type="danger">Maryvi is currently offline! Try the following possible solutions:
        <ul>
          <li>Try turning it off and on again</li>
          <li>Upgrade your memory to at least 16 gigabytes. <a href="">Order here</a></li>
        </ul>
      </cui-alert>
  
      <cui-alert cui-type="warning">The server is running near full capacity. Please upgrade soon. <a href="">More information</a></cui-alert>
  
      <cui-alert cui-type="unknown">There were multiple problems with the servers:
        <ol>
          <li><cui-icon icon="warning-sign" color="yellow"></cui-icon> Try turning it off and on again</li>
          <li><cui-icon icon="exclamation-sign" color="red"></cui-icon> Upgrade your memory to at least 16 gigabytes. <a href="">Order here</a></li>
        </ol>
      </cui-alert>
      
      <cui-alert>Please wait while we submit your information. This may take a minute...</cui-alert>
  
      <cui-alert cui-type="success">Your information was successfully processed. Thank you for choosing CUI!</cui-alert>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
    
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.alert', ['cui.base']);

  module.directive('cuiAlert', function(baseTemplatePath, $timeout, $q) {
    var TRANSITION_TIME, getDefaults;
    TRANSITION_TIME = 250;
    getDefaults = function(type) {
      switch (type) {
        case 'danger':
          return ['exclamation-sign', 'Critical Error'];
        case 'warning':
          return ['warning-sign', 'Warning'];
        case 'unknown':
          return [null, 'There are multiple issues'];
        case 'success':
          return ['ok-sign', 'Success'];
        default:
          return ['info-sign', 'Note'];
      }
    };
    return {
      templateUrl: "" + baseTemplatePath + "alert.html",
      restrict: 'EA',
      transclude: true,
      scope: {
        cuiType: '@',
        destroyAfter: '@',
        dismissable: '=?',
        icon: '@',
        label: '@'
      },
      link: function(scope, element, attrs) {
        var icon, label, _ref;
        scope.isClosed = false;
        if (scope.dismissable == null) {
          scope.dismissable = true;
        }
        scope._icon = scope.icon;
        scope._label = scope.label;
        _ref = getDefaults(scope.cuiType), icon = _ref[0], label = _ref[1];
        if (((scope.icon == null) || scope.icon === '') && (icon != null)) {
          element.attr('icon', '');
          scope._icon = icon;
        }
        if ((scope.label == null) || scope.label === '') {
          scope._label = label;
        }
        if (angular.isNumber(parseInt(scope.destroyAfter)) && scope.destroyAfter > 0) {
          $timeout((function() {
            return scope.close();
          }), scope.destroyAfter);
        }
        scope.$on('cui:collapse:expanded', function(e) {
          return e.stopPropagation();
        });
        scope.$on('cui:alert:dismiss', function() {
          return scope.close();
        });
        return scope.close = function() {
          var deferred;
          deferred = $q.defer();
          scope.$on('cui:collapse:collapsed', function(e) {
            e.stopPropagation();
            scope.$emit('cui:alert:dismissed');
            element.remove();
            return deferred.resolve();
          });
          scope.isClosed = true;
          return deferred.promise;
        };
      }
    };
  });


  /**
    @ngdoc directive
    @module cui.controls
    @name badge
    @src controls/badge/badge.coffee
    @description Badges typically show the status of a part of an application, or a count of objects.
   
    @restrict E
    @controlType presentational
    @transclude Badge Label
    @param {string=} cuiType One of
    ** `(undefined|invalid)` - Black
    ** `primary` - Blue
    ** `success` - Green
    ** `warning` - Yellow
    ** `danger` - Red
    ** `gray`
    ** `orange`
    ** `violet`
   
    @example
    <h3>Badges</h3>
  <example name='badge'>
    <file name='index.html'>
      <cui-badge>Default</cui-badge>
      <cui-badge cui-type='primary'>Primary</cui-badge>
      <cui-badge cui-type='success'>Success</cui-badge>
      <cui-badge cui-type='warning'>Warning</cui-badge>
      <cui-badge cui-type='danger'>Danger</cui-badge>
      <cui-badge cui-type='gray'>Gray</cui-badge>
      <cui-badge cui-type='orange'>Orange</cui-badge>
      <cui-badge cui-type='violet'>Violet</cui-badge>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
    
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.badge', ['cui.base']);

  module.directive('cuiBadge', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "badge.html",
      restrict: 'EA',
      transclude: true,
      scope: {
        cuiType: '@'
      }
    };
  });


  /**
    @ngdoc directive
    @module cui.controls
    @name breadcrumb
    @description Breadcrumbs or breadcrumb trail is a navigation aid used in user interfaces. It allows users to keep track of their locations within programs or documents. See [Masthead - Secondary Bar](http://ce.software.dell.com/cx/edg/masthead.html).
    @controlType navigation
    @restrict E
    @param {array} items An array of objects with the following structure:
   
        {
          icon: String, // icon to prepend before the label
          label: String, // required
          url: String
        }
    @example
    <h3>Basic Breadcrumb</h3>
  <example name='breadcrumb'>
    <file name='index.html'>
      <cui-breadcrumb items='items'></cui-breadcrumb>
      <p>Current path: <span ng-bind='location.path() || "/"'></span></p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $location) {
          $scope.items = [
            {
              icon: 'home',
              label: 'Home',
              url: './'
            },
            {
              label: 'Servers',
              url: '#servers'
            }
          ]
  
          $scope.location = $location
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.breadcrumb', ['cui.base']);

  module.directive('cuiBreadcrumb', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "breadcrumb.html",
      restrict: 'EA',
      transclude: false,
      scope: {
        items: '='
      }
    };
  });


  /**
    @ngdoc directive
    @name button
    @controlType actionable
   
    @module cui.controls
    @description Buttons allow for basic user interaction via click event handlers defined with `ng-click`.
   
    @src controls/button/button.coffee
    @restrict E
   
    @param {string=} cuiType The type (appearance) of the button. One of:
    ** `(undefined|invalid) - gray`
    ** `'primary' - Blue`
    ** `'success' - Green`
    ** `'warning' - Yellow`
    ** `'danger' - Red`
    ** `'gray'`
    ** `'orange'`
    ** `'violet'`
    ** `'transparent'` - Slightly opaque, the background color comes through the button.
    ** `'link'` - A standard link style in the sizing of a normal button.
    
  The color of a button in a disabled state is predefined and the `cui-type` is ignored. When the button is reenabled, the `cui-type` is respected.
   
    @param {string=} size The size of the button. One of:
    ** `'small'`
    ** `'medium' - default`
    ** `'large'`
   
    @param {expression=} ngClick The action to call from the `$scope`. See the Angular Documentation for [ng-click](https://docs.angularjs.org/api/ng/directive/ngClick).
   
    @param {expression=} ngDisabled Set to true to disable a button. See the Angular Documentation for [ng-disabled](https://docs.angularjs.org/api/ng/directive/ngDisabled).
   
    @param {string=} type The type of the button. Possible values are:
    ** `'submit'` - The button submits the form data to the server. This is the default if the attribute is not specified, or if the attribute is dynamically changed to an empty or invalid value.
    ** `'reset'` - The button resets all the controls to their initial values.
    ** `'button'` - The button has no default behavior. It can have client-side scripts associated with the element's events, which are triggered when the events occur.
    
    The default type is `'button'`.
   
    @example
    <h3>Button label</h3>
  <example name='button'>
    <file name='index.html'>
      <cui-button>Button Label</cui-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          // Nothing required here to have a label.
        })
    </file>
  </example>
   
    @example
    <h3>Button types</h3>
  <example name='buttonType'>
    <file name='index.html'>
      <cui-button>Default</cui-button>
      <cui-button cui-type='primary'>Primary</cui-button>
      <cui-button cui-type='success'>Success</cui-button>
      <cui-button cui-type='warning'>Warning</cui-button>
      <cui-button cui-type='danger'>Danger</cui-button>
      <cui-button cui-type='transparent'>Transparent</cui-button>
      <cui-button cui-type='link'>Link</cui-button>
  
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          // Nothing required here to have types.
        })
    </file>
  </example>
   
    @example
    <h3>Button sizes</h3>
  <example name='buttonSize'>
    <file name='index.html'>
      <cui-button size='small'>Small</cui-button>
      <cui-button size='medium'>Medium</cui-button>
      <cui-button size='large'>Large</cui-button>
  
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          // Nothing required here to have sizes.
        })
    </file>
  </example>
   
    @example
    <h3>Button enabled/disabled</h3>
  <example name='buttonDisabled'>
    <file name='index.html'>
      <cui-button cui-type='primary'>Primary, enabled</cui-button>
      <cui-button cui-type='primary' ng-disabled='true'>Primary, disabled</cui-button>
      
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          // Nothing to see here.
        })
    </file>
  </example>
   
    @example
    <h3>Buttons with icons</h3>
  <example name='buttonIcon'>
    <file name='index.html'>
      <cui-button cui-type='primary'>
        <cui-icon icon='plus'></cui-icon> Increment
      </cui-button>
      <cui-button cui-type='danger'>
        <cui-icon icon='remove-sign'></cui-icon> Delete Databases
      </cui-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          // Nothing is needed except to load CUI to add icons to a button.
        })
    </file>
  </example>
   
    @example
    <h3>Buttons with click handlers</h3>
  <example name='buttonClick'>
    <file name='index.html'>
      <cui-button ng-click='click()'>Default</cui-button>
      <cui-button ng-click='click("primary")' cui-type='primary'>Primary</cui-button>
      <cui-button ng-click='click("success")' cui-type='success'>Success</cui-button>
      <cui-button ng-click='click("warning")' cui-type='warning'>Warning</cui-button>
      <cui-button ng-click='click("danger")' cui-type='danger'>Danger</cui-button>
  
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.click = function(type) {
            type = type || 'default';
            alert("Hello! You clicked " + type + ".")
          }
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.button', ['cui.base']);

  module.directive('cuiButton', function(baseTemplatePath) {
    return {
      templateUrl: function(element, attrs) {
        if (attrs.cuiType !== 'link') {
          return "" + baseTemplatePath + "button.html";
        } else {
          return "" + baseTemplatePath + "button-link.html";
        }
      },
      restrict: 'EA',
      transclude: true,
      replace: true,
      scope: {
        cuiType: '@'
      },
      compile: function(element, attrs) {
        if (attrs.size == null) {
          attrs.size = 'medium';
        }
        if (attrs.cuiType == null) {
          attrs.cuiType = 'default';
        }
        if (attrs.type == null) {
          attrs.type = 'button';
        }
        if (attrs.cuiType === 'link') {
          element.addClass('cui-button-link');
        } else {
          element.addClass('cui-button');
        }
        element.addClass("cui-button-" + attrs.size);
        element.attr('type', attrs.type);
        return function(scope, element) {
          element.on('mouseup', function() {
            return element[0].blur();
          });
          if (scope.cuiType != null) {
            element.addClass("cui-button-type-" + scope.cuiType);
          }
          return scope.$watch('cuiType', function(newClass, oldClass) {
            if (oldClass !== newClass) {
              element.removeClass("cui-button-type-" + oldClass);
              if (newClass != null) {
                return element.addClass("cui-button-type-" + newClass);
              }
            }
          });
        };
      }
    };
  });


  /**
    @ngdoc directive
    @module cui.controls
    @name calendar
    @description The Calendar lets users choose a specific date. It is particularly useful for navigation (as in Outlook, for example).
  
  > The Calendar is primarily used with the Date Picker.
  
    @controlType input
    @restrict E
    @param {date|string} ngModel This accepts a javascript `date` object, or a string that can be parsed to a date.
    
    @example
    <h3>Calendar</h3>
  <example>
    <file name='index.html'>
      <cui-calendar ng-model='selectedDate'></cui-calendar>
      <br><br>
      <cui-button ng-click='selectedDate = "1/1/14"'>selectedDate = "1/1/14"</cui-button>
      <cui-button ng-click='setDate()'>selectedDate = "1/2/13"</cui-button>
      <cui-button ng-click='selectedDate = null'>selectedDate = null</cui-button>
      <br><br>
      <span ng-if='selectedDate'>
        <code>selectedDate: {{selectedDate}}</code> ({{readableDate}})
      </span>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $filter) {
          $scope.setDate = function() {
            $scope.selectedDate = new Date('1/2/13');
          }
  
          $scope.$watch('selectedDate', function(d) {
            $scope.readableDate = $filter('date')(new Date(d), 'fullDate');
          });
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.calendar', ['cui.base']);

  module.service('cuiCalendarUtils', function() {
    this.datesAreEqualToMonth = function(d1, d2) {
      return d1 && d2 && (d1.getYear() === d2.getYear()) && (d1.getMonth() === d2.getMonth());
    };
    this.datesAreEqualToDay = function(d1, d2) {
      return d1 && d2 && (d1.getYear() === d2.getYear()) && (d1.getMonth() === d2.getMonth()) && (d1.getDate() === d2.getDate());
    };
    this.generateMonth = function(date, selectedDate) {
      var d, dateIterator, month, startingDay, today, week, _i, _j;
      startingDay = (function() {
        var firstDayOfMonth, month, offset, ret, year;
        year = date.getFullYear();
        month = date.getMonth();
        firstDayOfMonth = new Date(year, month, 1);
        ret = new Date(firstDayOfMonth);
        offset = firstDayOfMonth.getDay();
        if (offset === 0) {
          offset = 7;
        }
        ret.setDate(ret.getDate() - offset);
        return ret;
      })();
      today = new Date();
      dateIterator = new Date(startingDay);
      month = [];
      for (_i = 0; _i <= 5; _i++) {
        week = [];
        for (_j = 0; _j <= 6; _j++) {
          d = new Date(dateIterator);
          week.push({
            date: d,
            isSelected: this.datesAreEqualToDay(d, selectedDate),
            isInMonth: this.datesAreEqualToMonth(d, date),
            today: this.datesAreEqualToDay(d, today)
          });
          dateIterator.setDate(dateIterator.getDate() + 1);
        }
        month.push(week);
      }
      return month;
    };
    this.nextMonth = function(date) {
      if (date.getMonth() === 11) {
        return new Date(date.getFullYear() + 1, 0);
      } else {
        return new Date(date.getFullYear(), date.getMonth() + 1);
      }
    };
    this.previousMonth = function(date) {
      if (date.getMonth() === 0) {
        return new Date(date.getFullYear() - 1, 11);
      } else {
        return new Date(date.getFullYear(), date.getMonth() - 1);
      }
    };
    this.nextYear = function(date) {
      var d;
      d = new Date(date);
      d.setFullYear(d.getFullYear() + 1);
      return d;
    };
    this.previousYear = function(date) {
      var d;
      d = new Date(date);
      d.setFullYear(d.getFullYear() - 1);
      return d;
    };
    return this;
  });

  module.directive('cuiCalendar', function(baseTemplatePath, cuiCalendarUtils) {
    return {
      templateUrl: "" + baseTemplatePath + "calendar.html",
      restrict: 'EA',
      require: ['cuiCalendar', '^ngModel'],
      scope: {
        dateFilter: '=?',
        modelCtrl: '=?'
      },
      controllerAs: 'calendarCtrl',
      controller: function($scope) {
        this.refreshView = function() {
          return $scope.month = cuiCalendarUtils.generateMonth($scope.monthDate, $scope.viewValueDate);
        };
        this.nextMonth = function() {
          $scope.monthDate = cuiCalendarUtils.nextMonth($scope.monthDate);
          return this.refreshView();
        };
        this.previousMonth = function() {
          $scope.monthDate = cuiCalendarUtils.previousMonth($scope.monthDate);
          return this.refreshView();
        };
        this.nextYear = function() {
          $scope.monthDate = cuiCalendarUtils.nextYear($scope.monthDate);
          return this.refreshView();
        };
        this.previousYear = function() {
          $scope.monthDate = cuiCalendarUtils.previousYear($scope.monthDate);
          return this.refreshView();
        };
        return this;
      },
      link: function(scope, element, attrs, _arg) {
        var calendarCtrl, getViewValueDate, modelCtrl;
        calendarCtrl = _arg[0], modelCtrl = _arg[1];
        scope.today = new Date();
        scope.setDate = function(date) {
          var currentDate;
          currentDate = modelCtrl.$viewValue;
          if (angular.isDate(currentDate)) {
            date.setHours(currentDate.getHours(), currentDate.getMinutes(), currentDate.getSeconds(), currentDate.getMilliseconds());
          }
          modelCtrl.$setViewValue(date);
          scope.viewValueDate = getViewValueDate();
          scope.monthDate = scope.viewValueDate;
          return calendarCtrl.refreshView();
        };
        getViewValueDate = function() {
          if (modelCtrl != null ? modelCtrl.$viewValue : void 0) {
            return new Date(modelCtrl != null ? modelCtrl.$viewValue : void 0);
          } else {
            return null;
          }
        };
        return modelCtrl.$render = function() {
          scope.viewValueDate = getViewValueDate();
          if (scope.viewValueDate || (scope.monthDate == null)) {
            scope.monthDate = scope.viewValueDate || scope.today;
          }
          return calendarCtrl.refreshView();
        };
      }
    };
  });


  /**
    @ngdoc directive
    @name checkbox
  
    @description Checkboxes let users select one or more options in a set of related options. A single check box lets users toggle an option off and on.
  
    @module cui.controls
    @src controls/checkbox/checkbox.coffee
    @controlType input
  
    @restrict E
    @transclude The label of the checkbox.
  
    @param {string} name - Name of the checkbox, or checkbox group inputs
    @param {object} ngModel - object or array on the scope where theresulting values are to be pushed to
    @param {string=} ngTrueValue - Individual only - Use if you want the true/checked value to be something other than true
    @param {string=} ngFalseValue - Individual only - Use if you want the false/unchecked value to be something other than false
  
    @param {object=} checkItems - Object/Array on the scope that should be used to generate a checkbox group
    @param {string=} checkValue - True value for checkbox within a group. Possible values: key on check-items, 'key', 'option', empty
    Resulting value depends on structure of check-items
    @param {string=} checkLabel - Label for checkbox within a group. Possible values: key on check-items, 'key', 'option', empty
    Resulting value depends on structure of check-items
    
    @example
    <h3>Basic checkbox</h3>
  <example name='basicCheckbox'>
    <file name='index.html'>
      <cui-checkbox ng-model='value' name='updates'>Sign up for updates</cui-checkbox>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
    
    @example
    <h3>Checkbox in form, individual</h3>
    <dl>
      <dt>ng-model</dt>
      <dd>Since this is not a group, we provide the model to store the selected value</dd>
      <dt>ng-disabled</dt>
      <dd>When true, will disabled the checkbox</dd>
      <dt>ng-required</dt>
      <dd>When true, will require the checkbox to be checked</dd>
    </dl>
  <example name='formCheckbox'>
    <file name='index.html'>
      <form novalidate name='checkboxExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <label>What would make a good movie?</label>
          <cui-checkbox ng-model="formData.movie.misslealiens"
                        name='movie'
                        ng-disabled='isDisabled'>
            Aliens with missles on their shoulders
          </cui-checkbox>
          <cui-checkbox ng-model="formData.movie.walkingsharks"
                        name='movie'
                        ng-disabled='isDisabled'>
            Walking, man eating sharks
          </cui-checkbox>
          <cui-checkbox ng-model="formData.movie.ninjamonkeys" name='movie' ng-disabled='isDisabled'>Killer monkey ninjas that come from underground</cui-checkbox>
        </div>
        <code>formData.movie: {{formData.movie}}</code>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
    
    @example
    <h3>Checkbox in form, individual, custom values</h3>
    <dl>
      <dt>ng-model</dt>
      <dd>Since this is not a group, we provide the model to store the selected value</dd>
      <dt>ng-true-value</dt>
      <dd>Designate a value other than true when selected</dd>
      <dt>ng-false-value</dt>
      <dd>Designate a value other than false when not selected</dd>
    </dl>
  <example name='formCustomCheckbox'>
    <file name='index.html'>
      <form novalidate name='checkboxExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <cui-checkbox ng-model="formData.approve"
                        name='approve'
                        ng-true-value='Yes'
                        ng-false-value='No'>
            Do you approve
          </cui-checkbox>
        </div>
        <code>formData.approve: {{formData.approve}}</code>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
    
    @example
    <h3>Checkbox group</h3>
    <dl>
      <dt>check-items</dt>
      <dd>Object or Array that contains the checkbox group value(s) and label(s)</dd>
      <dt>checkbox-label</dt>
      <dd>Key for the label of the checkbox (in check-items)</dd>
      <dt>checkbox-value</dt>
      <dd>Key for the true value of the checkbox (in check-items)</dd>
      <dd>Similiar to <code>ng-true-value</code></dd>
      <dd>If <code>check-items</code> is an object array, then this is required and if not provided, will default to the same value of <code>check-label</code></dd>
      <dt>ng-model</dt>
      <dd>Object to bind the group checked state to</dd>
      <dt>ng-required</dt>
      <dd>When true, will require at least one check within the group to be checked</dd>
      <dt>ng-disabled</dt>
      <dd>When true, will disabled the entire group</dd>
    </dl>
  <example name='formCustomCheckbox'>
    <file name='index.html'>
      <form novalidate name='checkboxExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <label>No <code>check-value</code> provided</label>
          <cui-checkbox-group name='colors' check-items='checkboxExample.colors'
                              ng-model="formData.colors"
                              check-label='color'
                              ng-disabled='isDisabled'
                              ng-required='isRequired'
                              cui-memo='checkboxExample.memos.colors'></cui-checkbox-group>
  
          <code>formData.colors: {{formData.colors}}</code>
        </div>
        <div class='cui-form-group'>
          <label><code>check-value</code> provided</label>
          <cui-checkbox-group name='colorsTwo' check-items='checkboxExample.colors'
                              ng-model="formData.othercolors"
                              check-label='color'
                              check-value='meaning'></cui-checkbox-group>
  
          <code>formData.othercolors: {{formData.othercolors}}</code>
        </div>
        <div class='cui-form-group'>
          <label><code>check-label</code> as option</label>
          <cui-checkbox-group name='roles' check-items='checkboxExample.roles'
                              ng-model="formData.roles"
                              check-label='option'></cui-checkbox-group>
          <code>formData.roles: {{formData.roles}}</code>
        </div>
        <div class='cui-form-group'>
          <label><code>check-label</code> default as key, <code>check-value</code> as option</label>
          <cui-checkbox-group name='tasks' check-items='checkboxExample.tasks'
                              ng-model="formData.tasks"
                              check-value='key'></cui-checkbox-group>
          <code>formData.tasks: {{formData.tasks}}</code>
        </div>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
    
    @example
    <h3>Checkbox group</h3>
  <h3><code>check-label</code> and <code>check-value</code> variations</h3>
  <p>
    <h4>Values for <code>check-label</code></h4>
    <p>There are a number of different values that can be provided for the check-label attribute. The resulting checkbox label depends on the data structure of check-items.</p>
    <ul>
      <li>If check-items is an array
        <ul>
          <li>"key": location of the element in the array, zero-based</li>
          <li>"option": value of the element in the array</li>
          <li>custom: n/a</li>
        </ul>
      </li>
    <li>If check-items is an object
      <ul>
        <li>"key": name of the key</li>
        <li>"option": value of the key</li>
        <li>custom: n/a</li>
      </ul>
    </li>
    <li>If check-items is an array of objects
      <ul>
        <li>"key": location of the element in the array, zero-based</li>
        <li>"option": [object, object], not recommended</li>
        <li>custom: value of the provided key within check-items</li>
      </ul>
    </li>
  </ul>
  At this time, it is not possible to use an object array
  </p>
  
  <p>
    <h4>Values for <code>check-value</code></h4>
    <p>There are a number of different values that can be provided for the check-value attribute. The resulting checkbox true value depends on the data structure of check-items.</p>
    <ul>
      <li>If check-items is an array
        <ul>
          <li>"key": location of the element in the array, zero-based</li>
          <li>"option": value of the element in the array</li>
          <li>custom: n/a</li>
        </ul>
      </li>
      <li>If check-items is an object
        <ul>
          <li>"key": name of the key</li>
          <li>"option": value of the key</li>
          <li>custom: n/a</li>
        </ul>
      </li>
      <li>If check-items is an array of objects
        <ul>
          <li>"key": location of the element in the array, zero-based</li>
          <li>"option": returns the entier object, not possible to initialize checked items</li>
          <li>custom: value of the provided key within check-items</li>
        </ul>
      </li>
    </ul>
    At this time, it is not possible to use an object array
  </p>
  
  <example name='formCustomCheckbox'>
    <file name='index.html'>
      <form novalidate name='checkboxExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <label>No <code>check-value</code> provided</label>
          <cui-checkbox-group name='colors' check-items='checkboxExample.colors'
                              ng-model="formData.colors"
                              check-label='color'
                              ng-disabled='isDisabled'
                              ng-required='isRequired'
                              cui-memo='checkboxExample.memos.colors'></cui-checkbox-group>
  
          <code>formData.colors: {{formData.colors}}</code>
        </div>
        <div class='cui-form-group'>
          <label><code>check-value</code> provided</label>
          <cui-checkbox-group name='colorsTwo' check-items='checkboxExample.colors'
                              ng-model="formData.othercolors"
                              check-label='color'
                              check-value='meaning'></cui-checkbox-group>
  
          <code>formData.othercolors: {{formData.othercolors}}</code>
        </div>
        <div class='cui-form-group'>
          <label><code>check-label</code> as option</label>
          <cui-checkbox-group name='roles' check-items='checkboxExample.roles'
                              ng-model="formData.roles"
                              check-label='option'></cui-checkbox-group>
          <code>formData.roles: {{formData.roles}}</code>
        </div>
        <div class='cui-form-group'>
          <label><code>check-label</code> default as key, <code>check-value</code> as option</label>
          <cui-checkbox-group name='tasks' check-items='checkboxExample.tasks'
                              ng-model="formData.tasks"
                              check-value='key'></cui-checkbox-group>
          <code>formData.tasks: {{formData.tasks}}</code>
        </div>
        <p><cui-button ng-click='toggleDisabled()'>{{isDisabled && 'Enable' || 'Disable'}} checks</cui-button></p>
        <p><cui-button ng-click='makeRequired()'>{{isRequired && 'Optional ' || 'Require'}} checks</cui-button></p>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
  
          $scope.formData = []
  
          $scope.formData.colors = []
          $scope.formData.othercolors = ["wealth"]
  
          $scope.formData.roles  = ["Admin"]
          $scope.formData.tasks  = ["design"]
  
          $scope.formData.approve = "Yes"
  
          $scope.isRequired = false;
          $scope.makeRequired = function() {
            $scope.isRequired = !$scope.isRequired
          }
  
          $scope.isDisabled = false;
          $scope.toggleDisabled = function() {
            $scope.isDisabled = !$scope.isDisabled
          }
  
          $scope.checkboxExample = [];
          $scope.checkboxExample = {
            memos: {
              colors: {
                required: 'This is required'
              }
            }
          };
          $scope.checkboxExample.colors = [
              {
                color: 'Red',
                meaning: 'anger'
              },
              {
                color: 'Green',
                meaning: 'wealth'
              },
              {
                color: 'Blue',
                meaning: 'sad'
              }
          ];
  
          $scope.checkboxExample.roles = [ "Admin", "Guest" ]
  
          $scope.checkboxExample.tasks = {
            design: 'Design',
            dev: 'Development'
          }
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.checkbox', ['cui.base']);

  module.directive('cuiCheckbox', function(baseTemplatePath, $log) {
    return {
      templateUrl: "" + baseTemplatePath + "checkbox.html",
      restrict: 'EA',
      require: '?ngModel',
      transclude: true,
      replace: true,
      scope: {
        ngModel: '=',
        ngRequired: '=',
        ngDisabled: '='
      },
      compile: function(element, attrs) {
        var check;
        check = element.find('input');
        if (attrs.name == null) {
          $log.cuiError({
            cuiErrorType: 'name',
            cuiErrorCtrl: 'cui-checkbox',
            cuiErrorElIdentity: attrs.name || attrs.id || 'UNKNOWN'
          });
        } else {
          check.attr('name', attrs.name);
        }
        if (attrs.ngTrueValue != null) {
          check.attr('ng-true-value', attrs.ngTrueValue);
          check.attr('ng-checked', "{{ngModel==='" + attrs.ngTrueValue + "'}}");
        }
        if (attrs.ngFalseValue != null) {
          check.attr('ng-false-value', attrs.ngFalseValue);
        }
        return function(scope, iElement, iAttrs, ngModelCtrl) {
          if (ngModelCtrl == null) {
            $log.cuiError({
              cuiErrorType: 'ngmodel',
              cuiErrorCtrl: 'cui-checkbox',
              cuiErrorElIdentity: iAttrs.name || iAttrs.id || 'UNKNOWN'
            });
            return;
          }
          return iElement.bind('keypress', function(event) {
            switch (event.which) {
              case 32:
                event.preventDefault();
                return document.activeElement.click();
            }
          });
        };
      }
    };
  });

  module.directive('cuiCheckboxGroup', function(baseTemplatePath, $log) {
    return {
      templateUrl: "" + baseTemplatePath + "checkboxgroup.html",
      restrict: 'EA',
      require: '?ngModel',
      replace: true,
      scope: {
        ngDisabled: '=',
        checkItems: '=',
        ngRequired: '=',
        ngModel: '='
      },
      compile: function(element, attrs) {
        var check, checkLabel, checkValue, cv, lbl, span;
        check = element.find('input');
        span = element.find('span');
        if (attrs.name == null) {
          $log.cuiError({
            cuiErrorType: 'name',
            cuiErrorCtrl: 'cui-checkbox-group',
            cuiErrorElIdentity: attrs.name || attrs.id || 'UNKNOWN'
          });
        } else {
          check.attr('name', attrs.name);
        }
        lbl = attrs.checkLabel;
        if (lbl != null) {
          if (lbl !== 'key' && lbl !== 'option') {
            checkLabel = 'option.' + attrs.checkLabel;
          } else {
            checkLabel = lbl;
          }
        } else {
          checkLabel = 'option';
        }
        span.attr('ng-bind', checkLabel);
        cv = attrs.checkValue;
        if (cv != null) {
          if (cv !== 'option' && cv !== 'key') {
            checkValue = 'option.' + cv;
          } else {
            checkValue = cv;
          }
        } else {
          checkValue = checkLabel;
        }
        check.attr('ng-checked', "ngModel.indexOf(" + checkValue + ") > -1");
        check.attr('ng-true-value', "{{" + checkValue + "}}");
        check.attr('ng-click', "updateGroup(" + checkValue + ")");
        return function(scope, iElement, iAttrs, ngModelCtrl) {
          if (iAttrs.ngModel == null) {
            $log.cuiError({
              cuiErrorType: 'ngmodel',
              cuiErrorCtrl: 'cui-checkbox-group',
              cuiErrorElIdentity: iAttrs.name || iAttrs.id || 'UNKNOWN'
            });
            return;
          }
          scope.updateGroup = function(value) {
            var idx;
            idx = scope.ngModel.indexOf(value);
            if (idx > -1) {
              scope.ngModel.splice(idx, 1);
            } else {
              scope.ngModel.push(value);
            }
            ngModelCtrl.$setViewValue(scope.ngModel);
            return ngModelCtrl.$render();
          };
          ngModelCtrl.$render = function(value) {
            if (scope.ngRequired === true) {
              if (scope.ngModel.length === 0) {
                return ngModelCtrl.$setValidity('required', false);
              } else {
                return ngModelCtrl.$setValidity('required', true);
              }
            }
          };
          scope.$watch('ngRequired', function(value) {
            return ngModelCtrl.$render();
          });
          return iElement.bind('keypress', function(event) {
            switch (event.which) {
              case 32:
                event.preventDefault();
                return document.activeElement.click();
            }
          });
        };
      }
    };
  });


  /*
  
    Internal directive --
  
    When cuiCollapse is 'true', the element will expand. False, it will collapse.
    If cuiCollapseSkipInitial is 'true', the element will not expand/collapse initially.
  
    Emits `cui:collapse:collapsed` when done collapsing, and `cui:collapse:expanded` when done expanding.
    (The events should be caught and `e.stopPropagation()` to keep it efficient-ish.)
  
    Elements using cuiCollapse should not have any border/padding/margin!
   */

  module = angular.module('cui.controls.collapse', ['cui.base']);

  module.directive('cuiCollapse', function(cuiTransition) {
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        var collapse, collapseDone, currentTransition, doTransition, expand, expandDone, initialAnimSkip;
        initialAnimSkip = attrs['cuiCollapseSkipInitial'] === 'true';
        currentTransition = void 0;
        doTransition = function(change) {
          var newTransition, newTransitionDone;
          newTransitionDone = function() {
            if (currentTransition === newTransition) {
              return currentTransition = void 0;
            }
          };
          newTransition = cuiTransition(element, change);
          if (currentTransition) {
            currentTransition.cancel();
          }
          currentTransition = newTransition;
          newTransition.then(newTransitionDone, newTransitionDone);
          return newTransition;
        };
        expand = function() {
          var scrollHeight;
          if (initialAnimSkip) {
            initialAnimSkip = false;
            return expandDone();
          } else {
            element.removeClass('cui-collapse').addClass('cui-collapsing');
            scrollHeight = element[0].scrollHeight;
            if (scrollHeight === 0) {
              return expandDone();
            }
            return doTransition({
              height: scrollHeight + 'px'
            }).then(expandDone);
          }
        };
        expandDone = function() {
          element.removeClass('cui-collapsing');
          element.addClass('cui-collapse cui-in');
          element.css({
            height: 'auto'
          });
          return scope.$emit('cui:collapse:expanded');
        };
        collapse = function() {
          var x;
          if (initialAnimSkip) {
            initialAnimSkip = false;
            collapseDone();
            return element.css({
              height: 0
            });
          } else {
            element.css({
              height: element[0].scrollHeight + 'px'
            });
            x = element[0].offsetWidth;
            element.removeClass('cui-collapse cui-in').addClass('cui-collapsing');
            return doTransition({
              height: 0 + 'px'
            }).then(collapseDone);
          }
        };
        collapseDone = function() {
          element.removeClass('cui-collapsing');
          element.addClass('cui-collapse');
          return scope.$emit('cui:collapse:collapsed');
        };
        return scope.$watch(attrs['cuiCollapse'], function(shouldCollapse) {
          if (shouldCollapse) {
            return collapse();
          } else {
            return expand();
          }
        });
      }
    };
  });


  /**
    @ngdoc directive
    @name combobox
  
    @description A text field with a drop down list containing possible values. The drop down list is filtered based on the text currently entered in the text field.
  
    @module cui.controls
    @src controls/comboBox/comboBox.coffee
    @controlType input
    @restrict E
  
    @param {string} ngModel - The text in the text field within the combo box.
  
    @param {array} items - An array of `item`s containing `label` properties. For example:
  
      [
        {
          label: 'Item 1'
        },
        {
          label: 'Item 2'
        },
        {
          label: 'Item 3'
        }
      ]
  
    @param {boolean=} ngDisabled - Set to true to disable the drop down button.
  
    @param {string=} placeholder - The placeholder text for the text field.
    
    @param {string} name - Required attribute that is used to set aria attributes.
  
    @example
    <h3>Basic combo box</h3>
  <example name='basicComboBox'>
    <file name='index.html'>
      <cui-combobox
        ng-model='name'
        items='names'
        cui-type='primary'
        placeholder='Enter a CUI dev'>
      </cui-combobox>
  
      Their role: {{getDesc(name) || 'Not a CUI developer'}}.
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.getDesc = function(name) {
            for(var i = 0; i < $scope.names.length; i++) {
              var el = $scope.names[i];
              if(el.label === name) {
                return el.description;
              }
            }
            return null;
          }
          $scope.names = [{
            label: 'Alexander Harding',
            description: 'CUI Intern developer'
          }, {
            label: 'Nick Radford',
            description: 'CUI Lead developer'
          }, {
            label: 'Anthony Schultz',
            description: 'CUI Developer'
          }, {
            label: 'James Krot',
            description: 'CUI Developer'
          }, {
            label: 'John Mancine',
            description: 'Lead Architect'
          }];
        })
    </file>
    <file name='styles.css'>
      body {
        height: 200px;
      }
    </file>
  </example>
  
    @example
    <h3>Disabled combo box</h3>
  <example name='disabledComboBox'>
    <file name='index.html'>
      <cui-combobox
        ng-disabled=true
        placeholder="I'm disabled!">
      </cui-combobox>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function(){});
    </file>
  </example>
  
    @example
    <h3>Combo box with external data</h3>
  <example name='countriesComboBox'>
    <file name='index.html'>
      <cui-combobox
        items='items'
        ng-disabled='disabled'
        ng-model='item'
        placeholder='Choose a country...'>
      </cui-combobox>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiDataSourceService){
          $scope.items = []
  
          countries = cuiDataSourceService('countries.json')
  
          countries.all().then(function(data){
            $scope.items = data
          }, function(err) {
            $scope.item = 'No countries found.';
            $scope.disabled = true;
          })
        });
    </file>
    <file name='styles.css'>
      body {
        height: 250px;
      }
    </file>
    <file name='countries.json'>
      [{"label":"Andorra"},{"label":"United Arab Emirates"},{"label":"Afghanistan"},{"label":"Antigua and Barbuda"},{"label":"Anguilla"},{"label":"Albania"},{"label":"Armenia"},{"label":"Angola"},{"label":"Antarctica"},{"label":"Argentina"},{"label":"American Samoa"},{"label":"Austria"},{"label":"Australia"},{"label":"Aruba"},{"label":"Åland"},{"label":"Azerbaijan"},{"label":"Bosnia and Herzegovina"},{"label":"Barbados"},{"label":"Bangladesh"},{"label":"Belgium"},{"label":"Burkina Faso"},{"label":"Bulgaria"},{"label":"Bahrain"},{"label":"Burundi"},{"label":"Benin"},{"label":"Saint Barthélemy"},{"label":"Bermuda"},{"label":"Brunei"},{"label":"Bolivia"},{"label":"Bonaire"},{"label":"Brazil"},{"label":"Bahamas"},{"label":"Bhutan"},{"label":"Bouvet Island"},{"label":"Botswana"},{"label":"Belarus"},{"label":"Belize"},{"label":"Canada"},{"label":"Cocos [Keeling] Islands"},{"label":"Democratic Republic of the Congo"},{"label":"Central African Republic"},{"label":"Republic of the Congo"},{"label":"Switzerland"},{"label":"Ivory Coast"},{"label":"Cook Islands"},{"label":"Chile"},{"label":"Cameroon"},{"label":"China"},{"label":"Colombia"},{"label":"Costa Rica"},{"label":"Cuba"},{"label":"Cape Verde"},{"label":"Curacao"},{"label":"Christmas Island"},{"label":"Cyprus"},{"label":"Czechia"},{"label":"Germany"},{"label":"Djibouti"},{"label":"Denmark"},{"label":"Dominica"},{"label":"Dominican Republic"},{"label":"Algeria"},{"label":"Ecuador"},{"label":"Estonia"},{"label":"Egypt"},{"label":"Western Sahara"},{"label":"Eritrea"},{"label":"Spain"},{"label":"Ethiopia"},{"label":"Finland"},{"label":"Fiji"},{"label":"Falkland Islands"},{"label":"Micronesia"},{"label":"Faroe Islands"},{"label":"France"},{"label":"Gabon"},{"label":"United Kingdom"},{"label":"Grenada"},{"label":"Georgia"},{"label":"French Guiana"},{"label":"Guernsey"},{"label":"Ghana"},{"label":"Gibraltar"},{"label":"Greenland"},{"label":"Gambia"},{"label":"Guinea"},{"label":"Guadeloupe"},{"label":"Equatorial Guinea"},{"label":"Greece"},{"label":"South Georgia and the South Sandwich Islands"},{"label":"Guatemala"},{"label":"Guam"},{"label":"Guinea-Bissau"},{"label":"Guyana"},{"label":"Hong Kong"},{"label":"Heard Island and McDonald Islands"},{"label":"Honduras"},{"label":"Croatia"},{"label":"Haiti"},{"label":"Hungary"},{"label":"Indonesia"},{"label":"Ireland"},{"label":"Israel"},{"label":"Isle of Man"},{"label":"India"},{"label":"British Indian Ocean Territory"},{"label":"Iraq"},{"label":"Iran"},{"label":"Iceland"},{"label":"Italy"},{"label":"Jersey"},{"label":"Jamaica"},{"label":"Jordan"},{"label":"Japan"},{"label":"Kenya"},{"label":"Kyrgyzstan"},{"label":"Cambodia"},{"label":"Kiribati"},{"label":"Comoros"},{"label":"Saint Kitts and Nevis"},{"label":"North Korea"},{"label":"South Korea"},{"label":"Kuwait"},{"label":"Cayman Islands"},{"label":"Kazakhstan"},{"label":"Laos"},{"label":"Lebanon"},{"label":"Saint Lucia"},{"label":"Liechtenstein"},{"label":"Sri Lanka"},{"label":"Liberia"},{"label":"Lesotho"},{"label":"Lithuania"},{"label":"Luxembourg"},{"label":"Latvia"},{"label":"Libya"},{"label":"Morocco"},{"label":"Monaco"},{"label":"Moldova"},{"label":"Montenegro"},{"label":"Saint Martin"},{"label":"Madagascar"},{"label":"Marshall Islands"},{"label":"Macedonia"},{"label":"Mali"},{"label":"Myanmar [Burma]"},{"label":"Mongolia"},{"label":"Macao"},{"label":"Northern Mariana Islands"},{"label":"Martinique"},{"label":"Mauritania"},{"label":"Montserrat"},{"label":"Malta"},{"label":"Mauritius"},{"label":"Maldives"},{"label":"Malawi"},{"label":"Mexico"},{"label":"Malaysia"},{"label":"Mozambique"},{"label":"Namibia"},{"label":"New Caledonia"},{"label":"Niger"},{"label":"Norfolk Island"},{"label":"Nigeria"},{"label":"Nicaragua"},{"label":"Netherlands"},{"label":"Norway"},{"label":"Nepal"},{"label":"Nauru"},{"label":"Niue"},{"label":"New Zealand"},{"label":"Oman"},{"label":"Panama"},{"label":"Peru"},{"label":"French Polynesia"},{"label":"Papua New Guinea"},{"label":"Philippines"},{"label":"Pakistan"},{"label":"Poland"},{"label":"Saint Pierre and Miquelon"},{"label":"Pitcairn Islands"},{"label":"Puerto Rico"},{"label":"Palestine"},{"label":"Portugal"},{"label":"Palau"},{"label":"Paraguay"},{"label":"Qatar"},{"label":"Réunion"},{"label":"Romania"},{"label":"Serbia"},{"label":"Russia"},{"label":"Rwanda"},{"label":"Saudi Arabia"},{"label":"Solomon Islands"},{"label":"Seychelles"},{"label":"Sudan"},{"label":"Sweden"},{"label":"Singapore"},{"label":"Saint Helena"},{"label":"Slovenia"},{"label":"Svalbard and Jan Mayen"},{"label":"Slovakia"},{"label":"Sierra Leone"},{"label":"San Marino"},{"label":"Senegal"},{"label":"Somalia"},{"label":"Suriname"},{"label":"South Sudan"},{"label":"São Tomé and Príncipe"},{"label":"El Salvador"},{"label":"Sint Maarten"},{"label":"Syria"},{"label":"Swaziland"},{"label":"Turks and Caicos Islands"},{"label":"Chad"},{"label":"French Southern Territories"},{"label":"Togo"},{"label":"Thailand"},{"label":"Tajikistan"},{"label":"Tokelau"},{"label":"East Timor"},{"label":"Turkmenistan"},{"label":"Tunisia"},{"label":"Tonga"},{"label":"Turkey"},{"label":"Trinidad and Tobago"},{"label":"Tuvalu"},{"label":"Taiwan"},{"label":"Tanzania"},{"label":"Ukraine"},{"label":"Uganda"},{"label":"U.S. Minor Outlying Islands"},{"label":"United States"},{"label":"Uruguay"},{"label":"Uzbekistan"},{"label":"Vatican City"},{"label":"Saint Vincent and the Grenadines"},{"label":"Venezuela"},{"label":"British Virgin Islands"},{"label":"U.S. Virgin Islands"},{"label":"Vietnam"},{"label":"Vanuatu"},{"label":"Wallis and Futuna"},{"label":"Samoa"},{"label":"Kosovo"},{"label":"Yemen"},{"label":"Mayotte"},{"label":"South Africa"},{"label":"Zambia"},{"label":"Zimbabwe"}]
    </file>
  </example>
   */

  module = angular.module('cui.controls.combobox', ['cui.base']);

  (function() {
    var func;
    func = function(baseTemplatePath, $filter, $log) {
      return {
        templateUrl: "" + baseTemplatePath + "combobox.html",
        restrict: 'EA',
        require: '?ngModel',
        scope: {
          name: '@',
          items: '=?',
          ngModel: '=?',
          ngDisabled: '='
        },
        compile: function(element, attrs) {
          var group, input, label;
          input = element.find('input');
          if (attrs.name != null) {
            input.attr('name', attrs.name);
            if (attrs.label != null) {
              label = "<label id='cui-label-" + attrs.name + "'>" + attrs.label + "</label>";
              element.prepend(label);
            } else {
              group = element.parent();
              label = group.find('label');
              label.attr('id', "cui-label-" + attrs.name);
            }
          } else {
            $log.cuiError({
              cuiErrorType: 'name',
              cuiErrorCtrl: 'cui-combobox',
              cuiErrorElIdentity: 'UNKNOWN'
            });
          }
          if (attrs.placeholder != null) {
            input.attr('placeholder', attrs.placeholder);
          }
          return function(scope, iElement, iAttrs, ngModelCtrl) {
            var filterItems, focusField, menu, scrollIntoView;
            if (iAttrs.ngModel == null) {
              $log.cuiError({
                cuiErrorType: 'ngmodel',
                cuiErrorCtrl: 'cui-combobox',
                cuiErrorElIdentity: iAttrs.name || iAttrs.id || 'UNKNOWN'
              });
              return;
            }
            if (scope.items == null) {
              scope.items = [];
            }
            scope.currentIndex = scope.selectedIndex || 0;
            scope.$watch('items', function(val) {
              if (val.length > 0) {
                return scope.selectedItems = scope.items;
              } else {
                return scope.selectedItems = [
                  {
                    label: 'No results found.'
                  }
                ];
              }
            });
            scrollIntoView = function() {
              var idx, scrollingElement;
              if (scope.currentIndex === -1) {
                idx = 0;
              } else {
                idx = scope.currentIndex;
              }
              element = iElement[0].querySelectorAll('.cui-menu-item')[idx];
              scrollingElement = iElement[0].querySelector('.cui-menu');
              if (element.offsetTop <= scrollingElement.scrollTop) {
                scrollingElement.scrollTop = element.offsetTop;
              }
              if (element.offsetTop + element.offsetHeight >= scrollingElement.scrollTop + scrollingElement.offsetHeight) {
                return scrollingElement.scrollTop = element.offsetTop + element.offsetHeight - scrollingElement.offsetHeight;
              }
            };
            filterItems = function(inputValue) {
              scope.selectedItems = $filter('filter')(scope.items, inputValue);
              scope.currentIndex = -1;
              if (scope.selectedItems.length === 0) {
                return scope.closeMenu();
              }
            };
            scope.select = function(index) {
              var inputValue, k, originalIndex, v, _ref, _ref1;
              if (index >= 0) {
                _ref = scope.items;
                for (k in _ref) {
                  v = _ref[k];
                  if (v.label === scope.selectedItems[index].label) {
                    originalIndex = k - 0;
                    scope.selectedIndex = originalIndex;
                    scope.currentIndex = originalIndex;
                  }
                }
                ngModelCtrl.$setViewValue(scope.items[originalIndex].label);
              } else {
                inputValue = iElement.find('input').val();
                _ref1 = scope.items;
                for (k in _ref1) {
                  v = _ref1[k];
                  if (v.label === inputValue) {
                    originalIndex = k - 0;
                  }
                }
                if (originalIndex) {
                  scope.selectedIndex = originalIndex;
                  scope.currentIndex = originalIndex;
                  ngModelCtrl.$setViewValue(scope.items[originalIndex].label);
                } else {
                  ngModelCtrl.$setViewValue(inputValue);
                }
              }
              return scope.closeMenu();
            };
            scope.toggleMenu = function(event) {
              scope.$broadcast('cui:toggle:');
              scrollIntoView();
              if (event) {
                event.preventDefault();
                event.stopPropagation();
              }
              return focusField();
            };
            scope.openMenu = function(event) {
              scope.$broadcast('cui:open:');
              scrollIntoView();
              if (event) {
                event.preventDefault();
                return event.stopPropagation();
              }
            };
            scope.closeMenu = function() {
              scope.selectedItems = scope.items;
              return scope.$broadcast('cui:close:');
            };
            focusField = function() {
              return iElement.find('input')[0].focus();
            };
            menu = iElement[0].querySelector('.cui-menu');
            ngModelCtrl.$formatters.push(function(inputValue) {
              if (inputValue && !angular.element(menu).hasClass('cui-menu-showing')) {
                scope.openMenu();
              }
              scope.selectedIndex = -1;
              filterItems(inputValue);
              if (ngModelCtrl.$dirty) {
                ngModelCtrl.$setViewValue(inputValue);
              }
              return inputValue;
            });
            ngModelCtrl.$parsers.unshift(function(inputValue) {
              var k, v, _ref;
              if (scope.currentIndex >= 0) {
                _ref = scope.items;
                for (k in _ref) {
                  v = _ref[k];
                  if (v.label === inputValue) {
                    scope.selectedIndex = k - 0;
                    scope.currentIndex = k - 0;
                  }
                }
              }
              return inputValue;
            });
            return iElement.bind('keydown keypress', function(event) {
              if (event.altKey && event.which === 40) {
                scope.openMenu();
                return;
              }
              if (event.altKey && event.which === 38) {
                scope.closeMenu();
                return;
              }
              if (scope.selectedItems.length > 0) {
                switch (event.which) {
                  case 40:
                    scope.$apply(function() {
                      if (scope.currentIndex < scope.selectedItems.length - 1) {
                        scope.currentIndex++;
                        return scrollIntoView();
                      }
                    });
                    return event.preventDefault();
                  case 38:
                    scope.$apply(function() {
                      if (scope.currentIndex > 0) {
                        scope.currentIndex--;
                        return scrollIntoView();
                      }
                    });
                    return event.preventDefault();
                  case 34:
                  case 35:
                    scope.$apply(function() {
                      scope.currentIndex = scope.selectedItems.length - 1;
                      return scrollIntoView();
                    });
                    return event.preventDefault();
                  case 33:
                  case 36:
                    scope.$apply(function() {
                      scope.currentIndex = 0;
                      return scrollIntoView();
                    });
                    return event.preventDefault();
                  case 13:
                    scope.$apply(function() {
                      return scope.select(scope.currentIndex);
                    });
                    return event.preventDefault();
                }
              }
            });
          };
        }
      };
    };
    module.directive('cuiCombobox', func);
    return module.directive('cuiComboBox', func);
  })();


  /**
    @ngdoc directive
    @module cui.controls
    @name dataGrid
    @description
  The data grid provides the ability to display extremely large amounts of data in a tabular format without having to send it all to the user at once.
  
  The data grid supports pagination, cell templates, and searching.
  
  <h3>Events</h3>
  <h4>Emits:</h4>
  
    ** `'cui:dataGrid:error'` when `$http` errors
  
  
  <h4>Listens for:</h4>
  
    ** `'cui:dataGrid:refresh'` to refresh the data grid
  
    @controlType tabular
  
    @restrict E
  
    @param {object} config This object has the following properties with the following default values, if literals (otherwise the `TYPE` it should be). All properties are doubly bound.
  
    {
      url: String,
      displayChecks: false,
      id: String,
      columns: [
        {
          label: String,
          map: String,
          className: String,
          cellTemplateUrl: String,
          cellTemplate: String,
          disableSort: Boolean
        }, ...
      ],
      rows: 'data',
      searchable: true,
      selection: true,
      pageSize: 15,
      currentPageNumber: 1,
      totalPages: function(config, response) {
        // Assume: response contains 'count' property (total number of entries)
        return Math.ceil(response.count / config.pageSize)
      },
      query: function(config),
      _query: function(config) {
        return {
          offset: config.pageSize * (config.currentPageNumber - 1),
          limit: config.pageSize,
          search: config.searchQuery
          sortCol: config.sortCol
          sortDir: config.sortDir
        }
      }
    }
  
   *# url
  url should be a location to a url that provides JSON data in the following form:
  
    {
      count: Number // The number of entries. Used for the default totalPages method for the paginator.
      data: [
        // (Items with properties which keys match the map value set in config.columns)
      ]
    }
  
  If the endpoint is called and returns an error (404, 500, etc), a `cui:dataGrid:error` event will be emitted with the response body as the first parameter.
  
   *# displayChecks
  Will create a column of Cui Checkboxes as the first column of the grid.
  TODO: where are the checks bound?
  
   *# id
  A key that can be used as a unique identifier for each row of data.
  
   *# columns
  Each entry must contain a:
  
   ** `map` specifying the key to look in the response of the server for.
  
  
  Additionally, each entry can contain a:
  
  
   ** `label` specifying the header label for the column.
   ** `className` the CSS class name(s) to add to the `td` (cell) or `th` (column header) elements. Using `'text-center'` (or left, right, justified) can make alignment easy.
   ** `cellTemplateUrl` specifying the location of an HTML partial to fetch and compile. This can be useful for things like buttons, icons, and forms in the column.
    Additionally, each partial has access the the `column` and `row` data.
    By adding the a scope onto the `column`, you can access it from within the cell (see example).
   ** `cellTemplate` same as `cellTemplateUrl` except inline.
  
   *# rows
  The location of the rows array in the response from the server as an angular expression.
  
  By default, it's `'data'`, or `response['data']`.
  
  It can be passed a string using the dot syntax, for example, `'data.rows'` would access `response['data']['rows']`. This allows full control of where the rows array is in the response from the server.
  
   *# searchable
  Whether to display a text box which will update `config.searchQuery`. Since `config` is being watched, this will trigger data to be fetched. The default `config.query` returns a search property which value is `config.searchQuery`. Lastly, `config.currentPageNumber` is reset to 1.
  
   *# selection
  Set to `false` to turn off single selection of rows.
  
   *# pageSize
  The size of the page in terms of entries for the default `config.totalPages` function.
  
   *# currentPageNumber
  The current page number, used by the default `config.query.offset`.
  
   *# sortCol and sortDir
  When the header is clicked and sorting is not disabled on the column clicked, a request (see `config._query`) will be made to the server, with `sortCol` and `sortDir`.
  
  `sortCol` is the column's `map` property (the ID).
  
  `sortAsc` is either 'asc' (ascending) 'desc' (descending).
  
  These requests to the server can be manipulated or globally disabled by using the  `config.query` function.
  
  <br /><br />
  <blockquote class="alert">In most cases, the properties below will not need to be modified.</blockquote>
  
   *# totalPages
  `totalPages` returns the total pages for the paginator. It can use values passed in from the `config` object and current `response` from your server.
  
  The default way this is implemented is for if your server returns the `count` of ENTRIES, and the `offset` is in terms of ENTRIES. If, for example, your server abstracts entries to pages, you can override this function with the following:
  
    function(config, response) {
      return response.count
    }
  
  Following suit, your `config.query` function would look like the following:
  
    function(config) {
      return {
        offset: config.currentPageNumber - 1
      }
    }
  
  See "query and _query" below for more information.
  
   *# query and _query
  Query is a function that allows you to customize the query sent to the server. The return object of `query` is extended into `_query`'s returned values -- therefore, you can overide anything in `_query`. However, since it is also available in the `config` object, you can rename parameters sent to the server.
  
  Say, for example, your server take the param `startAt` instead of `offset`. The following `config.query` function will make this possible:
  
    function(config) {
      return {
        offset: null,
        startAt: config._query(config).offset
      }
    }
  
  All we're doing is simply 'muting' `offset`, and creating the param `startAt` that does what `offset` used to do by default.
  
  `_query` CANNOT be overidden, and it is the only property than cannot. It is simply exposing the default behavior of the params to send to the server for your convenience. The following `query` function would work exactly the same:
  
    function(config) {
      return {
        offset: null,
        startAt: config.pageSize * (config.currentPageNumber - 1)
      }
    }
  
  
    @param {object=} response Provides access to the raw server's JSON response. May or may not exist. You should never modify this value.
    
    @param {object=} selected The currently selected row object. This object is tracked by ID.
  
    @param {array=} checked The array of currently checked row object. Each row object in the array is tracked by ID.
  
    @example
    <h3>CUI Data Grid</h3>
  
  <example name='dataGrid'>
    <file name='index.html'>
      {{ response.count }} total rows <cui-button ng-click="refresh()">Refresh</cui-button>
      <cui-data-grid config="settings" response="response"></cui-data-grid>
    </file>
    <file name='cellIconTmpl.html'>
      <cui-icon icon="{{row.online && 'ok' || 'warning-sign'}}" color="{{row.online && 'green' || 'red'}}"></cui-icon>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.delete = function(row) {
            console.log('DELETE ', row);
          }
  
          $scope.refresh = function() {
            $scope.$broadcast('cui:dataGrid:refresh');
          }
  
          $scope.settings = {
            url: "http://ce.software.dell.com/api/server",
            id: 'id',
            displayChecks: true,
            columns: [{
              label: 'ID',
              map: 'id',
              className: 'text-right'
            }, {
              label: 'Status',
              map: 'online',
              cellTemplateUrl: 'cellIconTmpl.html'
            }, {
              label: 'Type',
              map: 'type'
            }, {
              label: 'Location',
              map: 'location'
            }, {
              label: 'IP Address',
              map: 'ip'
            }, {
              label: 'Service Tag',
              map: 'servicetag'
            }, {
              label: '',
              appScope: $scope,
              cellTemplate: '<cui-button size="small" ng-click="column.appScope.delete(row)">console.log(row)</cui-button>',
              className: 'text-center',
              disableSort: true
            }]
          }
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.dataGrid', ['cui.base']);

  module.directive('cuiDataGrid', function(baseTemplatePath, cuiDataSourceService, $parse, cuiLoading) {
    return {
      templateUrl: "" + baseTemplatePath + "dataGrid.html",
      restrict: 'EA',
      transclude: false,
      scope: {
        config: '=',
        response: '=?',
        selected: '=?',
        checked: '=?'
      },
      controller: function($scope) {
        if ($scope.checked == null) {
          $scope.checked = [];
        }
        if ($scope.selected == null) {
          $scope.selected = null;
        }
        $scope.toggleChecked = function(row) {
          var checkedRow, index, _i, _len, _ref;
          _ref = $scope.checked;
          for (index = _i = 0, _len = _ref.length; _i < _len; index = ++_i) {
            checkedRow = _ref[index];
            if (checkedRow[$scope.config.id] === row[$scope.config.id]) {
              $scope.checked.splice(index, 1);
              return;
            }
          }
          return $scope.checked.push(row);
        };
        $scope.isChecked = function(row) {
          var checkedRow, _i, _len, _ref;
          _ref = $scope.checked;
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            checkedRow = _ref[_i];
            if (checkedRow[$scope.config.id] === row[$scope.config.id]) {
              return true;
            }
          }
          return false;
        };
        this.updateRowStatus = function(e, row) {

          /*
          Because ngClick is on the table row, we check the target to verify the intent of the user. If the target was a table cell(TD), we assume they meant to select the row. This is a problematic solution, clicking on any other target within a cell (like an icon), prevents the selection.
           */
          if (e.target.nodeName === 'TD' && $scope.config.selection) {
            if ($scope.selected && $scope.selected.id === row.id) {
              return $scope.selected = null;
            } else {
              return $scope.selected = row;
            }
          }
        };
        return this;
      },
      controllerAs: 'datagrid',
      link: function(scope, tElement, tAttrs) {
        var dataSource, getPaginationRange, queryWithDefaults, updatePaginator, updateQuery, _base, _base1, _base2, _base3, _base4, _ref, _ref1;
        if (scope.config != null) {
          if ((_base = scope.config).rows == null) {
            _base.rows = 'data';
          }
          if ((_base1 = scope.config).pageSize == null) {
            _base1.pageSize = 15;
          }
          if ((_base2 = scope.config).currentPageNumber == null) {
            _base2.currentPageNumber = 1;
          }
          if ((_base3 = scope.config).selection == null) {
            _base3.selection = true;
          }
          if ((_base4 = scope.config).totalPages == null) {
            _base4.totalPages = function(config, response) {
              return Math.ceil(response.count / config.pageSize);
            };
          }
          scope.config._query = function(config) {
            return {
              offset: config.pageSize * (config.currentPageNumber - 1),
              limit: config.pageSize,
              search: config.searchQuery ? config.searchQuery : null,
              sortCol: config.sortCol,
              sortDir: config.sortDir
            };
          };
        }
        scope.dataGetter = $parse((_ref = scope.config) != null ? _ref.rows : void 0);
        queryWithDefaults = function(config) {
          var defaults, userDef;
          defaults = config._query(config);
          userDef = typeof config.query === "function" ? config.query(config) : void 0;
          return angular.extend(defaults, userDef);
        };
        if ((_ref1 = scope.config) != null ? _ref1.url : void 0) {
          dataSource = cuiDataSourceService(scope.config.url);
          updateQuery = function() {
            return cuiLoading(dataSource.query(queryWithDefaults(scope.config)).then(function(response) {
              scope.response = response;
              scope.totalPages = scope.config.totalPages(scope.config, scope.response);
              return updatePaginator();
            }, function(err) {
              return scope.$emit('cui:dataGrid:error', err);
            }));
          };
          scope.$on('cui:dataGrid:refresh', function() {
            return updateQuery();
          });
          scope.$watchCollection('config', function(newConfig, oldConfig) {
            if (oldConfig != null) {
              if (newConfig.url !== oldConfig.url) {
                dataSource = cuiDataSourceService(newConfig.url);
              }
              return updateQuery();
            }
          });
        }

        /* SORTING */
        scope.toggleSort = function(column) {
          if (!column.disableSort) {
            if (column.map === scope.config.sortCol) {
              if (scope.config.sortDir === 'asc') {
                return scope.config.sortDir = 'desc';
              } else if (scope.config.sortDir === 'desc') {
                return scope.config.sortDir = 'asc';
              }
            } else {
              scope.config.sortCol = column.map;
              return scope.config.sortDir = 'desc';
            }
          }
        };

        /* SEARCHING */
        scope.$watch('config.searchQuery', function(newVal, oldVal) {
          var _ref2;
          if (newVal !== oldVal) {
            return (_ref2 = scope.config) != null ? _ref2.currentPageNumber = 1 : void 0;
          }
        });

        /* PAGINATION */
        updatePaginator = function() {
          var i, range;
          range = getPaginationRange(scope.config.currentPageNumber, scope.totalPages);
          if (range != null) {
            scope.pages = (function() {
              var _i, _ref2, _ref3, _results;
              _results = [];
              for (i = _i = _ref2 = range[0], _ref3 = range[1]; _ref2 <= _ref3 ? _i <= _ref3 : _i >= _ref3; i = _ref2 <= _ref3 ? ++_i : --_i) {
                _results.push({
                  number: i
                });
              }
              return _results;
            })();
            return scope.showPaginator = true;
          } else {
            return scope.showPaginator = false;
          }
        };
        return getPaginationRange = function(currentPage, totalPages) {
          if (totalPages === 0) {
            return null;
          } else if (totalPages < 8) {
            return [1, totalPages];
          } else if (currentPage < 5) {
            return [1, 8];
          } else if (currentPage > totalPages - 4) {
            return [totalPages - 7, totalPages];
          } else {
            return [currentPage - 3, currentPage + 3];
          }
        };
      }
    };
  });

  module.directive('cuiDataGridCell', function($compile, $http, $templateCache) {
    return {
      restrict: 'C',
      require: '^cuiDataGrid',
      link: function(scope, element, attrs) {
        scope.$watch('column.cellTemplate', function(value) {
          if (value) {
            element.html(value);
            return $compile(element.contents())(scope);
          }
        });
        return scope.$watch('column.cellTemplateUrl', function(value) {
          if (value) {
            $http.get(value, {
              cache: $templateCache
            }).success(function(response) {
              element.html(response);
              $compile(element.contents())(scope);
            });
          }
        });
      }
    };
  });


  /**
    @ngdoc directive
    @name datePicker
  
    @description The Date picker uses a Calendar and Textbox to make a date picking popup.
  
    @module cui.controls
    @src controls/datePicker/datePicker.coffee
    @controlType input
    
    @restrict A
  
    @param {undefined} cuiDatePicker The base directive. Any value is ignored.
  
  The Date Picker directive is intended to be used on text boxes like the CUI Text Box, or `input[type="text"]`.
    @param {date} ngModel This accepts a javascript `date` object, or `null`/`undefined`, in which case the no date will be selected, and the current month will be opened by default.
    @param {date#format} dateParser `dateParser` is a separate directive used to convert date strings into dates with validation.
  
  It takes custom and shorthand date formats, like Angular Date.
  
  For more information, visit the third party component Github page: https://github.com/dnasir/angular-dateParser
  
    @example
  <example name='datePicker'>
    <file name='index.html'>
      <cui-textbox cui-date-picker
                   ng-model="date"
                   date-parser='shortDate'
                   placeholder='m/d/yy'></cui-textbox>
      <p>date:  {{date}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {})
    </file>
    <file name='styles.css'>
      body {
        height: 600px;
      }
    </file>
  </example>
   */


  /**
    @ngdoc directive
    @name dateTimePicker
  
    @description The Date-time picker uses Calendar, Time and Textbox to make a date and time picking popup.
  
    @module cui.controls
    @src controls/datePicker/datePicker.coffee
    @controlType input
    
    @restrict A
  
    @param {undefined} cuiDateTimePicker The base directive. Any value is ignored.
  
  The Date Time Picker directive is intended to be used on text boxes like the CUI Text Box, or `input[type="text"]`.
    @param {date} ngModel This accepts a javascript `date` object, or `null`/`undefined`, in which case the no date will be selected, and the current month will be opened by default.
    @param {date#format} dateParser `dateParser` is a separate directive used to convert date strings into dates with validation.
    @param {boolean=} showSeconds If `'true'`, a toggle to adjust the time by each second will be added. Default false.
    @param {boolean=} showMeridian If `'false'`, time will be displayed in 24-hour format. Default true.
    @param {number=} hourIncrement The number of hours to skip when clicking the up/down arrows. Default is 1.
    @param {number=} minuteIncrement The number of minutes to skip when clicking the up/down arrows. Default is 15.
    @param {number=} secondIncrement The number of seconds to skip when clicking the up/down arrows. Default is 15.
  
  It takes custom and shorthand date formats, like Angular Date.
  
  For more information, visit the third party component Github page: https://github.com/dnasir/angular-dateParser
  
    @example
  <example name='dateTimePicker'>
    <file name='index.html'>
        <cui-textbox cui-date-time-picker
                     ng-model="date"
                     date-parser='short'
                     placeholder='m/d/yy h:mm a'></cui-textbox>
        <p>date:  {{date}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {})
    </file>
    <file name='styles.css'>
      body {
        height: 600px;
      }
    </file>
  </example>
  
    @example
    <h3>Date Time Picker with 24-hour mode, and seconds</h3>
  <example name='dateTimePicker'>
    <file name='index.html'>
        <cui-textbox cui-date-time-picker
                     ng-model="date"
                     date-parser='M/d/yy H:mm:ss'
                     show-seconds='true'
                     show-meridian='false'
                     placeholder='m/d/yy h:mm a'></cui-textbox>
        <p>date:  {{date}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {})
    </file>
    <file name='styles.css'>
      body {
        height: 600px;
      }
    </file>
  </example>
   */


  /**
    @ngdoc directive
    @module cui.controls
    @name timePicker
    @description The Time Picker implements the Time directive in a tooltip. It is meant to be used within forms.
  
    @controlType input
    @restrict A
    @param {undefined} cuiTimePicker The base directive. Any value is ignored.
  
  The Time Picker directive is intended to be used on text boxes like the CUI Text Box, or `input[type="text"]`.
    @param {date} ngModel This accepts a javascript `date` object, or `null`/`undefined`, in which case the time will be rounded down to the current hour.
    @param {date#format} dateParser `dateParser` is a separate directive used to convert date strings into dates with validation.
  
  It takes custom and shorthand date formats, like Angular Date.
  
  For more information, visit the third party component Github page: https://github.com/dnasir/angular-dateParser
    @param {boolean=} showSeconds If `'true'`, a toggle to adjust the time by each second will be added. Default false.
    @param {boolean=} showMeridian If `'false'`, time will be displayed in 24-hour format. Default true.
    @param {number=} hourIncrement The number of hours to skip when clicking the up/down arrows. Default is 1.
    @param {number=} minuteIncrement The number of minutes to skip when clicking the up/down arrows. Default is 15.
    @param {number=} secondIncrement The number of seconds to skip when clicking the up/down arrows. Default is 15.
  
    @example
    <h3>TIme Picker</h3>
  <example>
    <file name='index.html'>
      <cui-textbox cui-time-picker ng-model='time' date-parser='shortTime' placeholder='12:00 AM'></cui-textbox> {{time}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
    <file name='styles.css'>
    body {
      height: 160px;
    }
    </file>
  </example>
  
    @example
    <h3>24-hour Time Picker</h3>
  <example>
    <file name='index.html'>
      <cui-textbox cui-time-picker show-meridian='false' ng-model='time' date-parser='H:mm' placeholder='12:00'></cui-textbox> {{time}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
    <file name='styles.css'>
    body {
      height: 160px;
    }
    </file>
  </example>
  
    @example
    <h3>Time Picker with seconds</h3>
  <example>
    <file name='index.html'>
      <cui-textbox cui-time-picker show-seconds='true' ng-model='time' date-parser='mediumTime' placeholder='12:00:00 AM'></cui-textbox> {{time}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
    <file name='styles.css'>
    body {
      height: 160px;
    }
    </file>
  </example>
  
    @example
    <h3>Time Picker with custom increments</h3>
  <example>
    <file name='index.html'>
      <cui-textbox cui-time-picker minute-increment='1' ng-model='time' date-parser='shortTime' placeholder='12:00 AM'></cui-textbox> {{time}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
    <file name='styles.css'>
    body {
      height: 160px;
    }
    </file>
  </example>
   */

  module = angular.module('cui.controls.datePicker', ['cui.base']);

  (function() {
    var directive;
    directive = function(baseTemplatePath, $templateCache, $compile, cuiTip, $document) {
      return {
        restrict: 'A',
        require: 'ngModel',
        scope: {
          model: '=ngModel'
        },
        link: function(scope, element, attrs, modelCtrl) {
          var calendar, calendarTimeScope, classes, datePicker, datepickerClicked, handleDocumentClick, time, tip;
          scope.modelCtrl = modelCtrl;
          classes = ['drop-theme-cui-tooltip'];
          datePicker = angular.element('<span></span>');

          /*
            We need to link calendarTimeScope's `model` and our scope's `model` together
            calendarTimeScope.model <--> scope.model
          
            This allows us to watch where changes are coming from, and react accordingly.
          
            For example, when `calendarTimeScope.model` changes, we need to set validity on our `modelCtrl`.
           */
          calendarTimeScope = scope.$new({});
          calendarTimeScope.$watch('model', function(m) {
            scope.model = m;
            return modelCtrl.$setValidity('date', true);
          });
          scope.$watch('model', function(m) {
            return calendarTimeScope.model = m;
          });
          if ((attrs.cuiDateTimePicker != null) || (attrs.cuiDatePicker != null)) {
            classes.push('cui-calendar-tooltip');
            calendar = $templateCache.get("" + baseTemplatePath + "datePicker-calendar.html");
            calendar = $compile(calendar)(calendarTimeScope);
            datePicker.append(calendar);
          }
          if ((attrs.cuiDateTimePicker != null) || (attrs.cuiTimePicker != null)) {
            classes.push('cui-time-tooltip');
            time = angular.element($templateCache.get("" + baseTemplatePath + "datePicker-time.html"));
            time.attr('show-seconds', attrs.showSeconds);
            time.attr('show-meridian', attrs.showMeridian);
            time.attr('hour-increment', attrs.hourIncrement);
            time.attr('minute-increment', attrs.minuteIncrement);
            time.attr('second-increment', attrs.secondIncrement);
            time = $compile(time)(calendarTimeScope);
            datePicker.append(time);
          }
          tip = null;
          datepickerClicked = false;
          handleDocumentClick = function(e) {
            if (!datepickerClicked) {
              $document.off('mousedown', handleDocumentClick);
              if (tip != null) {
                tip.close();
              }
            }
            return datepickerClicked = false;
          };
          element.on('focus', function() {
            if (tip == null) {
              tip = cuiTip({
                target: element[0],
                content: datePicker[0],
                openOn: null,
                classes: classes.join(' '),
                type: 'tooltip',
                position: 'bottom left',
                tetherOptions: {
                  constraints: [
                    {
                      to: 'scrollParent'
                    }, {
                      to: 'window',
                      attachment: 'together',
                      pin: true
                    }
                  ]
                }
              });
            }
            $document.off('mousedown', handleDocumentClick);
            $document.on('mousedown', handleDocumentClick);
            return tip.open();
          });
          element.on('mousedown', function(e) {
            return datepickerClicked = true;
          });
          datePicker.on('mousedown', function(e) {
            return datepickerClicked = true;
          });
          element.on('keydown', function(e) {
            if (e.which === 9) {
              $document.off('mousedown', handleDocumentClick);
              return tip != null ? tip.close() : void 0;
            }
          });
          return scope.$on('$destroy', function() {
            $document.off('mousedown', handleDocumentClick);
            return tip != null ? tip.remove() : void 0;
          });
        }
      };
    };
    module.directive('cuiDatePicker', directive);
    module.directive('cuiDateTimePicker', directive);
    return module.directive('cuiTimePicker', directive);
  })();


  /**
    @ngdoc directive
    @name dropDownButton
  
    @description The Drop Down Button creates a button and menu combination. Because the Drop Down Button generates a Button, the same attributes, such as size and color, can be used. See the Button documentation for more information.
  
  The Drop Down Button should transclude a `<ul cui-keyboard-menu>`. The `cui-keyboard-menu` directive enables keyboard support on the menu. (new in 2.6.0-beta.2).
  
    @module cui.controls
    @src controls/dropDownButton/dropDownButton.coffee
    @controlType actionable
  
    @restrict E
    @transclude <ul cui-keyboard-menu> <li></li>... </ul>
  
    @param {string} Label Label that should be displayed in the button.
  
    @param {string=} menuAnchor The side of the button the menu should open on. `menu-anchor='[right] [left]'`. Default anchor position is `right` if `menuAnchor` is not set.
  
    @param {boolean=} ngDisabled Set to true to disable the drop down button.
  
    @param {string=} size
      The size of the button. One of
  
    ** `'small'`
    ** `'medium'` - default
    ** `'large'`
  
    @param {string=} cuiType
      The type of the button. See the Button.
  
    @param {string=} Icon
      The type of icon. See the Icon.
  
    @example
    <h3>Regular drop down button</h3>
    <p>Drop down button with label and color attributes.</p>
  <example name='regularDropDownButton'>
    <file name='index.html'>
      <cui-drop-down-button label='Drop down button' cui-type='primary' icon="dashboard">
        <ul cui-keyboard-menu>
          <li kb-focus='focus-1' ng-click='click("Example 1 - Item 1")'>Item 1</li>
          <li ng-click='click("Example 1 - Item 2")'>Item 2</li>
          <li ng-click='click("Example 1 - Item 3")'>Item 3</li>
        </ul>
      </cui-drop-down-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.click = function(text) {
            console.log(text);
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 150px;
      }
    </file>
  </example>
  
    @example
    <h3>Right aligned drop down button</h3>
    <p>Drop down button with the menu-anchor attribute set to right, which anchors the open menu position to the right, aligned with the right edge of the button. The default menu-anchor position is left.</p>
  <example name='rightDropDownButton'>
    <file name='index.html'>
      <cui-drop-down-button label='Drop down button anchor position' menu-anchor='right' style='float: right'>
        <ul cui-keyboard-menu>
          <li ng-click='click("Example 2 - Item 1")'>Item 1</li>
          <li ng-click='click("Example 2 - Item 2")'>Item 2</li>
          <li ng-click='click("Example 2 - Item 3")'>Item 3</li>
        </ul>
      </cui-drop-down-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.click = function(text) {
            alert(text);
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 150px;
      }
    </file>
  </example>
  
    @example
    <h3>Disabled drop down button</h3>
  <example name='disabledDropDownButton'>
    <file name='index.html'>
      <cui-drop-down-button label='Drop down button disabled' ng-disabled='true'>
        <ul cui-keyboard-menu>
          <li ng-click='click("Example 3 - Item 1")'>Item 1</li>
          <li ng-click='click("Example 3 - Item 2")'>Item 2</li>
          <li ng-click='click("Example 3 - Item 3")'>Item 3</li>
        </ul>
      </cui-drop-down-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.click = function(text) {
            alert(text);
          }
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.dropDownButton', ['cui.base']);

  module.directive('cuiDropDownButton', function(baseTemplatePath, cuiTip, $compile, $timeout) {
    return {
      templateUrl: "" + baseTemplatePath + "dropDownButton.html",
      restrict: 'E',
      transclude: true,
      replace: true,
      scope: {
        menuAnchor: '@',
        label: '@',
        cuiType: '@',
        ngDisabled: '=',
        icon: '@'
      },
      compile: function(element, attrs, transclude, $log) {
        var button;
        if (attrs.size == null) {
          attrs.size = 'medium';
        }
        if (attrs.menuAnchor == null) {
          attrs.menuAnchor = 'right';
        }
        button = element.find('cui-button');
        button.attr('size', attrs.size);
        button.attr('cui-type', attrs.cuiType);
        if (attrs.ngDisabled != null) {
          button.attr('ng-disabled', 'ngDisabled');
        }
        return function(scope, element, attrs) {
          element.on('click', function(e) {
            return e.stopPropagation();
          });
          return transclude(scope, function(content) {
            var contents, tipScope, ul;
            ul = null;
            angular.forEach(content, function(c) {
              if (c.nodeName === 'UL') {
                return ul = c;
              }
            });
            if (!ul) {
              console.log('CUI-DROP-DOWN-BUTTON requires nested UL element.');
              return;
            }
            tipScope = scope.$parent.$new();
            contents = $compile(ul.outerHTML)(tipScope)[0];
            return $timeout(function() {
              var _tip;
              _tip = cuiTip({
                target: element[0].children[0],
                content: contents,
                classes: 'drop-theme-cui-menu',
                position: "bottom " + attrs.menuAnchor,
                type: 'menu'
              });
              return tipScope._tip = _tip;
            });
          });
        };
      }
    };
  });

  module.directive('cuiKeyboardMenu', function() {
    return {
      compile: function(element) {
        element.attr('kb-list', '');
        return element.attr('kb-cyclic', '');
      }
    };
  });

  module.directive('li', function() {
    return {
      restrict: 'E',
      compile: function(element) {
        var _ref;
        if (!((_ref = element.parent()[0]) != null ? _ref.hasAttribute('cui-keyboard-menu') : void 0)) {
          return;
        }
        element.attr('kb-item', '');
        if (element.attr('ng-click')) {
          element.attr('kb-invoke', "_tip.close(); " + (element.attr('ng-click')));
          return element.removeAttr('ng-click');
        }
      }
    };
  });


  /**
    @ngdoc directive
    @name dropDownList
    @description The Drop Down List displays options to be selected in a form.
  
  
  > Please note that we are [currently aware](https://github.com/angular/angular.js/issues/6926) this control doesn't work in IE9. This is an issue with the browser or Angular. As an alternative, please do the following:
  ```html
  <select class="cui-drop-down-list" [ngModel, ngOptions, ...]>
    <option value="one">First option</option>
    <option value="two">Second option</option>
  </select>
  ```
  
    @deprecated 2.7.0
    @useInstead Please consider using <a href='../uiSelect'>UI Select</a> instead.<br>Although Drop Down List will be removed, the style .cui-drop-down-list will still be included.
  
    @module cui.controls
    @src controls/dropDownList/dropDownList.coffee
    @controlType input
  
    @restrict E
    @transclude <option> and <optgroup> DOM elements
    
    @param {string} ngModel The `ng-model` is the property on the scope the selected value should be stored into. [See Angular Docs for ngModel](http://docs.angularjs.org/api/ng.directive:ngModel).
    
    @param {string=} ngOptions `ng-options` can be passed in as an Angular comprehension to create the options for the DropDownList. [See Angular Docs for select](http://docs.angularjs.org/api/ng.directive:select).
  
  ```html
    <cui-drop-down-list ng-options='opt.value as opt.label for opt in options'></cui-drop-down-list>
  ```
  ```javascript
    $scope.options = [
      {
        label: '-- Select the repeat frequency --',
        value: ''
      },
      {
        label: 'Daily',
        value: 'daily'
      },
      {
        label: 'Weekly',
        value: 'weekly'
      },
      {
        label: 'Monthly',
        value: 'monthly'
      }
    ]
  ```
  
    @example
    <h3>Transclude items</h3>
  <example name='transcludeDropDownList'>
    <file name='index.html'>
      <cui-drop-down-list ng-model='value' ng-init='value = ""'>
        <option value=''>-- Select --</option>
        <option value='clicker'>Click Here</option>
      </cui-drop-down-list>
  
      <p>{{value}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Transclude items with optgroups</h3>
  <example name='transcludeOptgroupDropDownList'>
    <file name='index.html'>
      <select class="cui-drop-down-list" ng-model='value' ng-init='value = ""'>
        <option value=''>-- Select --</option>
        <optgroup label='Foobar'>
          <option>Long option number 1</option>
          <option>Much longer options can go in here yay</option>
        </optgroup>
      </select>
  
      <p>{{value}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>With ngRepeat</h3>
  <example name='ngRepeatDropDownList'>
    <file name='index.html'>
      <select class="cui-drop-down-list" ng-model='value' ng-init='value = ""'>
        <option value=''>-- Please select an Option --</option>
        <option ng-repeat='opt in options' value='{{opt.value}}'>{{opt.label}}</option>
      </select>
  
      <p>{{value}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.options = [
            {
              label: 'Option 1',
              value: 'opt1'
            },
            {
              label: 'Option 2',
              value: 'opt2'
            }
          ]
        });
    </file>
  </example>
  
    @example
    <h3>With ngOptions</h3>
  <example name='ngOptionsDropDownList'>
    <file name='index.html'>
      <cui-drop-down-list ng-model='value' ng-options='opt.value as opt.label for opt in options'>
      </cui-drop-down-list>
  
      <p>Selected Value: {{value}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.options = [
            // {
            //   label: '-- Select --',
            //   value: ''
            // },
            {
              label: 'Option 1',
              value: 'opt1'
            },
            {
              label: 'Option 2',
              value: 'opt2'
            }
          ]
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.dropDownList', ['cui.base']);

  module.directive('cuiDropDownList', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "dropDownList.html",
      restrict: 'EA',
      transclude: true,
      replace: true
    };
  });

  module.directive('cuiDropDownList', function() {
    var div, isIeLessThan10;
    div = document.createElement('div');
    div.innerHTML = '<!--[if lt IE 10]><i></i><![endif]-->';
    isIeLessThan10 = div.getElementsByTagName('i').length === 1;
    return {
      restrict: 'C',
      link: function(scope, element, attrs) {

        /* istanbul ignore if */
        if (isIeLessThan10) {
          return element.addClass('cui-drop-down-list-ie');
        }
      }
    };
  });


  /**
    @ngdoc directive
    @name masthead
  
    @description The Masthead is the primary branding element in a web application.
    
  You can programmatically close the controls by broadcasting a `'cui:masthead:controls:close'` event.
  
    @module cui.controls
    @src controls/masthead/masthead.coffee
    @controlType foundational
  
    @restrict E
    @transclude
        <cui-button></cui-button>
        [...]
  
    @param {string=} applicationHref Url for the applicationName or applicationLogo.
  
    @param {string=} applicationLogo When applicationLogo is used, applicationName is overridden.
  
    @param {string} applicationName The label for the Masthead.
  
    @param {string=} applicationSubname The secondary label for the Masthead.
  
    @param {boolean=} showControls True or false to show controls.
  
    @example
  
  <example name='masthead'>
    <file name='index.html'>
      <cui-masthead application-name='Active System Manager' application-subname='Admin panel' show-controls=true>
        <!-- Masthead controls -->
        <cui-drop-down-button label='Drop down button' icon="dashboard">
          <ul cui-keyboard-menu>
            <li kb-focus='focus-1' ng-click='click("Example 1 - Item 1")'>Item 1</li>
            <li ng-click='click("Example 1 - Item 2")'>Item 2</li>
            <li ng-click='click("Example 1 - Item 3")'>Item 3</li>
          </ul>
        </cui-drop-down-button>
  
        <cui-button ng-click='showSettings()'>
          <cui-icon icon='cog'></cui-icon> Settings
        </cui-button>
  
        <cui-button ng-click='showAboutBox()'>
          <cui-icon icon='question-sign'></cui-icon> About Box
        </cui-button>
      </cui-masthead>
      
      <br>
      <cui-text-box ng-model='username'></cui-text-box>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          function closeControls() {
            $scope.$broadcast('cui:masthead:controls:close');
          }
  
          $scope.manageAccount = function() {
            alert("manage your users account");
            closeControls();
          }
  
          $scope.showSettings = function() {
            alert('Show application settings');
            closeControls();
          }
  
          $scope.showAboutBox = function() {
            alert('Show the About Box');
            closeControls();
          }
  
          $scope.username = 'PROD\\nradford'
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.masthead', ['cui.base']);

  module.directive('cuiMasthead', function(baseTemplatePath, $document) {
    var FIXED_CLASS;
    FIXED_CLASS = 'cui-fixed-masthead';
    return {
      templateUrl: "" + baseTemplatePath + "masthead.html",
      restrict: 'EA',
      transclude: true,
      scope: {
        fixed: '@',
        showNavigationDrawer: '@',
        name: '@applicationName',
        subname: '@applicationSubname',
        showControls: '=',
        applicationHref: '@',
        applicationLogo: '@'
      },
      controller: function($scope) {
        var handleClick;
        $scope.drawerShowing = false;
        $scope.toggleDrawer = function() {
          $scope.drawerShowing = !$scope.drawerShowing;
          if ($scope.drawerShowing) {
            $document.bind('click', handleClick);
          } else {
            $document.unbind('click', handleClick);
          }
        };
        handleClick = function() {
          $scope.drawerShowing = false;
          $document.unbind('click', handleClick);
          $scope.$apply();
        };
        return $scope.$on('cui:masthead:controls:close', function() {
          return $scope.drawerShowing = false;
        });
      },
      link: function(scope, element, attrs) {
        var drawerButton, mastheadControls;
        mastheadControls = angular.element(element[0].querySelector('.cui-masthead-controls'));
        drawerButton = angular.element(element[0].querySelector('.cui-drawer'));
        drawerButton.on('click', function(event) {
          event.preventDefault();
          event.stopPropagation();
          scope.$apply();
        });
        return scope.$watch('fixed', function(fixed) {
          var el;
          el = document.getElementsByTagName('body')[0];
          angular.element(el).removeClass(FIXED_CLASS);
          if (fixed) {
            return angular.element(el).addClass(FIXED_CLASS);
          }
        });
      }
    };
  });


  /**
    @ngdoc directive
    @name icon
    @description The Icon is a helper to use an icon from the CUI icon set.
  
    @module cui.controls
    @src controls/icon/icon.coffee
    @controlType presentational
    @restrict E
    
    @param {string} icon The name of the icon.
  
  If you need to use a custom icon set, we have the ability to register prefixes for the Icon:
  
  The CUI Icon works by taking the `icon` attribute value and prefixing it with `cui-icon-` as a class. For example (generalized),
  
  ```html
  <cui-icon icon="flag"></cui-icon>
  ```
  becomes
  ```html
  <i class="cui-icon-flag"></i>
  ```
  
  However, if you inject the cuiIcon service and call the function `registerPrefix` on that object:
  ```js
  cuiIcon.registerPrefix('custom-', 'custom-base-class');
  ```
  Then
  ```html
  <cui-icon icon="custom-weird-icon"></cui-icon>
  ```
  becomes
  ```html
  <i class="custom-weird-icon custom-base-class"></i>
  ```
  
  (All parameters after the first are recognized as base classes and added to the matched prefix.)
  
  This is especially useful for custom icons in CUI components that use CUI Icons, such as the Navigation List.
  Also, you're now free to use your own icon font without worry about conflicting with CUI's.
  
    @param {string=} color The color of the icon. One of:
    ** `(undefined|invalid)` - Inherit
    ** `gray`
    ** `blue`
    ** `yellow`
    ** `red`
    ** `green`
    ** `black`
    ** `orange`
    ** `violet`
    ** `white`
  
    @param {string=} size The size of the icon, e.g. `'100%'`, `'3em'` or `'14px'`.
  
    @example
    <h3>Inline icons</h3>
  <example name='inlineIcons'>
    <file name='index.html'>
      I took a <cui-icon icon='rocket'></cui-icon> from the <cui-icon icon='globe'></cui-icon> to the <cui-icon icon='moon'></cui-icon>.
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Icons with color and size</h3>
    <p>Since icons are scalable vector graphics, any sizes works well. The icons are, for all intents and purposes, an extent of alphanumeric characters.</p>
  <example name='colorSizeIcons'>
    <file name='index.html'>
      <div class='text-center'>
        <cui-icon icon='dell-halo' color='white' size='500px'></cui-icon>
      </div>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
    <file name='styles.css'>
      body {
        background-color: #0085c3;
      }
    </file>
  </example>
  
    @example
    <h3>Available icons</h3>
    <p>This example uses the <code>cuiDataSourceService</code> and some native angular filtering.</p>
  <example name='allIcons'>
    <file name='index.html'>
      <cui-text-box ng-model="filterText" placeholder="Filter icon..."></cui-text-box>
      <br /><br />
      <div ng-repeat='icon in icons | filter: filterText | orderBy:"toString()"' title='{{icon}}' class='icon-cell'>
        <cui-icon icon='{{icon}}' size='40px'></cui-icon><br/>
        <div>{{icon}}</div>
      </div>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui']).
        controller('AppCtrl', function($scope, cuiDataSourceService) {
          cuiDataSourceService('icons.json').all().then(function(data) {
            $scope.icons = data;
          })
        });
    </file>
    <file name='styles.css'>
      .icon-cell {
        width: 125px;
        height: 85px;
        display: inline-block;
        text-align:center;
        padding-top: 10px
      }
      .icon-cell > div {
        padding-top: 8px;
      }
      .icon-cell:hover {
        background: #f5f5f5;
      }
    </file>
    <file name='icons.json'>
      ["dell-halo","glass","music","search","envelope","heart","star","star-empty","user","film","th-large","th","th-list","ok","remove","zoom-in","zoom-out","off","signal","cog","trash","home","file","time","road","download-alt","download","upload","inbox","play-circle","repeat","refresh","list-alt","lock","flag","headphones","volume-off","volume-down","volume-up","qrcode","barcode","tag","tags","book","bookmark","print","camera","font","bold","italic","text-height","text-width","align-left","align-center","align-right","align-justify","list","indent-left","indent-right","facetime-video","picture","pencil","map-marker","adjust","tint","edit","share","check","move","step-backward","fast-backward","backward","play","pause","stop","forward","fast-forward","step-forward","eject","chevron-left","chevron-right","plus-sign","minus-sign","remove-sign","ok-sign","question-sign","info-sign","screenshot","remove-circle","ok-circle","ban-circle","arrow-left","arrow-right","arrow-up","arrow-down","share-alt","resize-full","resize-small","plus","minus","asterisk","exclamation-sign","gift","leaf","fire","eye-open","eye-close","warning-sign","plane","calendar","random","comment","magnet","chevron-up","chevron-down","retweet","shopping-cart","folder-close","folder-open","resize-vertical","resize-horizontal","bar-chart","twitter-sign","facebook-sign","camera-retro","key","cogs","comments","thumbs-up","thumbs-down","star-half","heart-empty","signout","linkedin-sign","pushpin","external-link","signin","trophy","github-sign","upload-alt","lemon","phone","check-empty","bookmark-empty","phone-sign","twitter","facebook","github","unlock","credit","rss","hdd","bullhorn","bell","certificate","hand-right","hand-left","hand-up","hand-down","circle-arrow-left","circle-arrow-right","circle-arrow-up","circle-arrow-down","globe","wrench","tasks","filter","briefcase","fullscreen","group","link","cloud","beaker","cut","copy","paper-clip","save","sign-blank","reorder","list-ul","list-ol","strikethrough","underline","table","magic","truck","pinterest","pinterest-sign","google-plus-sign","google-plus","money","caret-down","caret-up","caret-left","caret-right","columns","sort","sort-down","sort-up","envelope-alt","linkedin","undo","legal","dashboard","comment-alt","comments-alt","bolt","sitemap","umbrella","paste","lightbulb","exchange","cloud-download","cloud-upload","user-md","stethoscope","suitcase","bell-alt","coffee","food","file-alt","building","hospital","ambulance","medkit","fighter-jet","beer","h-sign","plus-sign-2","double-angle-left","double-angle-right","double-angle-up","double-angle-down","angle-left","angle-right","angle-up","angle-down","desktop","laptop","tablet","mobile","circle-blank","quote-left","quote-right","spinner","circle","reply","github-alt","folder-close-alt","folder-open-alt","expand-alt","collapse-alt","smile","frown","meh","gamepad","keyboard","flag-alt","flag-checkered","terminal","code","reply-all","star-half-full","location-arrow","crop","code-fork","unlink","question","info","exclamation","superscript","subscript","eraser","puzzle","microphone","microphone-off","shield","calendar-empty","fire-extinguisher","rocket","maxcdn","chevron-sign-left","chevron-sign-right","chevron-sign-up","chevron-sign-down","html5","css3","anchor","unlock-alt","bullseye","ellipsis-horizontal","ellipsis-vertical","rss-sign","play-sign","ticket","minus-sign-alt","check-minus","level-up","level-down","check-sign","edit-sign","external-link-sign","share-sign","compass","collapse","collapse-top","expand","euro","gbp","dollar","rupee","yen","renminbi","won","bitcoin","file-2","file-text","sort-by-alphabet","sort-by-alphabet-alt","sort-by-attributes","sort-by-attributes-alt","sort-by-order","sort-by-order-alt","thumbs-up-2","thumbs-down-2","youtube-sign","youtube","xing","xing-sign","youtube-play","dropbox","stackexchange","instagram","flickr","adn","bitbucket","bitbucket-sign","tumblr","tumblr-sign","long-arrow-down","long-arrow-up","long-arrow-left","long-arrow-right","apple","windows","android","linux","dribbble","skype","foursquare","trello","female","male","gittip","sun","moon","archive","bug","vk","weibo","renren"]
    </file>
  </example>
    @example
    <h3>CSS3 and SVG party!</h3>
  <example name='rainbowIcons'>
    <file name='index.html'>
      <div class='text-center'>
        <cui-icon icon='rocket' class='rainbow' size='250px'></cui-icon>
      </div>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
    <file name='styles.css'>
      .rainbow {
        animation: rainbow 2s;
        -webkit-animation: rainbow 2s;
  
        animation-iteration-count: infinite;
        -webkit-animation-iteration-count: infinite;
      }
  
      @keyframes rainbow {
        0%   {color: #ce1126;}
        25%  {color: #f2af00;}
        50%  {color: #0085c3;}
        75%  {color: #7ab800;}
        100% {color: #ce1126;}
      }
  
      @-webkit-keyframes rainbow {
        0%   {color: #ce1126;}
        25%  {color: #f2af00;}
        50%  {color: #0085c3;}
        75%  {color: #7ab800;}
        100% {color: #ce1126;}
      }
    </file>
  </example>
   */

  module = angular.module('cui.controls.icon', ['cui.base']);

  module.factory('cuiIcon', function() {
    var prefixes;
    prefixes = [];
    return {
      registerPrefix: function() {
        var classes, prefix;
        prefix = arguments[0], classes = 2 <= arguments.length ? __slice.call(arguments, 1) : [];
        return prefixes.push({
          name: prefix,
          classes: classes || []
        });
      },
      getIconClass: function(icon) {
        var prefix, _i, _len;
        if (!angular.isString(icon)) {
          return null;
        }
        for (_i = 0, _len = prefixes.length; _i < _len; _i++) {
          prefix = prefixes[_i];
          if (icon.indexOf(prefix.name) === 0) {
            return ("" + icon + " ") + prefix.classes.join(' ');
          }
        }
        return "cui-icon-" + icon;
      }
    };
  });

  module.directive('cuiIcon', function(baseTemplatePath, cuiIcon) {
    return {
      templateUrl: "" + baseTemplatePath + "icon.html",
      restrict: 'EA',
      scope: {
        icon: '@',
        color: '@',
        size: '@'
      },
      link: function(scope, element, attrs) {
        return scope.$watch('icon', function(icon, oldIcon) {
          element.find('i').removeClass(cuiIcon.getIconClass(oldIcon));
          return element.find('i').addClass(cuiIcon.getIconClass(icon));
        });
      }
    };
  });


  /**
    @ngdoc directive
    @name memo
  
    @description The Memo provides a way to show an error message, description or instruction
    on a form input control.
    
    @module cui.controls
    @src controls/memo/memo.coffee
    @controlType presentational
    
    @restrict A
  
    @param {object} cui-memo - An object that contains the message to be shown with
    the given input control, where the message key matches the error type.
  
    memos: {
      inputName: {
        required: 'This input is required',
        pattern: 'Provide a valid value'
      }
    }
  
    @example
  <p>Messages are shown on page load if an input does not meet the valid criteria set for it.</p>
  
  <p>Controls known to have issues:
    <ul>
      <li>Checkbox: required marker does not render</li>
      <li>Radio: required marker does not render</li>
      <li>Datepicker: Will not render with cui-memo</li>
      <li>Combo Box: Will not render with cui-memo</li>
    </ul>
  </p>
  <p>
    <blockquote>Angular 1.3, integrates a feature called ng-model-options which will allows us to show messages/feedback with finer control over the event that triggers the message display. For now, messages are shown at page load and on keyup.</blockquote>
  </p>
  
  <example name='memo'>
    <file name='index.html'>
      <form name='userdetails' class='cui-form-stacked' novalidate style='width: 50%'>
        <div class='cui-form-group'>
          <label for='firstname'>First Name</label>
          <cui-text-box type='text' id='firstname' name='firstname'
            ng-model='stackedform.user.firstname'
            cui-memo='stackedform.memos.firstname'
            ng-pattern='validators.chars'
            required></cui-text-box>
        </div>
  
        <div class='cui-form-group'>
          <label for='lastname'>Last Name</label>
          <cui-text-box type='text' id='lastname' name='lastname'
            ng-model='stackedform.user.lastname'
            cui-memo='stackedform.memos.lastname'
            ng-pattern='validators.chars'
            required></cui-text-box>
        </div>
  
        <div class='cui-form-group'>
          <label for='age'>Age</label>
          <cui-text-box type='number' id='age' name='age'
            step='1' min='0' max='120'
            ng-model='stackedform.user.age'
            cui-memo='stackedform.memos.age'
            required></cui-text-box>
        </div>
  
        <div class='cui-form-group'>
          <label for='gender'>Gender</label>
          <select class="cui-drop-down-list" id='gender' name='gender'
            ng-model='stackedform.user.gender'
            cui-memo='stackedform.memos.gender'
            required>
              <option value='male'>Male</option>
              <option value='female'>Female</option>
          </select>
        </div>
  
        <div class='cui-form-group'>
          <label for='relationship'>Marital Status</label>
          <cui-drop-down-list id='relationship' name='relationship'
            ng-model='stackedform.user.relationship'
            cui-memo='stackedform.memos.relationship'
            required>
            <option value='single'>Single</option>
            <option value='married'>Married</option>
          </cui-drop-down-list>
        </div>
  
        <div class='cui-form-group'>
          <label for='usercomment'>Comments</label>
          <cui-textarea id='usercomment' name='usercomment'
            ng-model='stackedform.user.comment'
            cui-memo='stackedform.memos.comment'
            maxlength=1000
            required></cui-textarea>
        </div>
  
        <cui-button ng-disabled='userdetails.$pristine' ng-click='reset()'>Reset</cui-button>
        <cui-button type='primary' ng-disabled='userdetails.$invalid'>Submit</cui-button>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.reset = function(user) {
            $scope.stackedform.user = {}
          };
          $scope.validators = { chars: /[A-Za-z]{3}/ }
          $scope.stackedform = {
            memos: {
              firstname: {
                required: 'Your first name is required',
                pattern: 'C\'mon that can\'t be your first name!'
              },
              lastname: {
                required: 'Your last name is also required',
                pattern: 'Really, that\'s your last name?'
              },
              age: {
                required: 'We won\'t tell',
                pattern: 'Whhaaa?'
              },
              gender: {
                required: 'Please select one'
              },
              relationship: {
                required: 'Not really our business, but tell us anyway'
              },
              comment: {
                required: 'Say what you will!'
              },
              awesome: {
                required: 'Bet you are!'
              }
            }
          }
          $scope.alignedform = {
            users: {},
            memos: {
              firstname: {
                required: 'Your first name is required',
                pattern: 'C\'mon that can\'t be your first name!'
              },
              lastname: {
                required: 'Your last name is also required',
                pattern: 'Really, that\'s your last name?'
              }
            }
          }
          $scope.inlineform = {
            users: {},
            memos: {
              name: {
                description: 'Please enter your first and last name',
                required: 'Missing a required field',
                pattern: 'First and Last names must have at least 3 characters',
                valid: 'You got it!'
              }
            }
          }
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.memo', ['cui.base']);

  module.directive('cuiMemo', function(baseTemplatePath, $compile) {
    return {
      restrict: 'A',
      require: 'ngModel',
      link: function(scope, iElement, iAttrs, ngModelCtrl) {
        var form, getMemos, memoElement, memos, parent;
        memos = scope.$eval(iAttrs.cuiMemo);
        memoElement = angular.element('<span class="input-memo"></span>');
        parent = angular.element(iElement[0].parentNode);
        if (!parent.hasClass('cui-form-group')) {
          while (!parent.hasClass('cui-form-group')) {
            parent = angular.element(parent[0].parentNode);
          }
        }
        parent.append(memoElement);
        getMemos = function(value) {
          var errorType, errorValue, memo, memoType, memoValue, _ref;
          for (memoType in memos) {
            memoValue = memos[memoType];
            _ref = ngModelCtrl.$error;
            for (errorType in _ref) {
              errorValue = _ref[errorType];
              if (errorValue === true) {
                if (memoType === errorType) {
                  memo = memoValue;
                }
              }
            }
          }
          if (ngModelCtrl.$dirty) {
            memoElement.text(memo != null ? memo : '');
          }
          return value;
        };
        ngModelCtrl.$formatters.push(getMemos);
        ngModelCtrl.$parsers.push(getMemos);
        form = iElement[0].form;
        if (iAttrs.required) {
          return angular.element(iElement[0].labels).append('<span class="cui-required"> *</span>');
        }
      }
    };
  });


  /**
    @ngdoc directive
    @name menu
  
    @description The CUI Menu manages the visible state of a menu and binds a click event to the toggle broadcast and is mainly intended to be consumed by other CUI controls and not directly implemented as a control.
  
  CUI Menu is a behavior set that is added to two CUI controls or elements. The control/element that is to trigger the opening and closing of the menu, will have an attribute of cui-menu-toggle. The element whos visual state of open or close should be affected will have an attribute cui-menu. The values of the 'cui-menu-toggle' and 'for' attributes must be the same.
  
  A CUI Menu cui-menu-toggle attribute can be set on almost any element. Likewise, with the cui-menu attribute. If you are going to use the CUI Menu control directly, please be sure to follow semantic HTML practices. For example, a cui-menu-toggle on a h1 would not be very semantic. Nor would setting the cui-menu on a span.
  
  CUI Menu consists of three seperate directives. Each applied to different elements that make up the CUI Menu.
  
   1. `cui-menu-toggle`
   2. `cui-menu`
   3. `cui-menu-item`
  
  
  __Description:__
  
  `cui-menu-toggle`: Applied to a CUI control or element that `cui-menu` is to respond to. Binds a click event to the control or element.
  
  `cui-menu`: Applied as a behavior to an element, such as a `div`, that will have its visiblity affected when the `cui-menu-toggle` click event is triggered.
  
  `cui-menu-item`: The items to be contained within the `cui-menu`.
  
  CUI Menu has two helper attributes, where the value of the attributes are to be the same.
  
  __Description:__
  
   1. `cui-menu-toggle='menu'`: Applied to a control or element that is to trigger the visibility of an associated cui-menu
   2. `for='menu'`: Applied to an HTML element that is to have its visiblity toggled by the `cui-menu-toggle`
  
    @module cui.controls
    @src controls/menu/menu.coffee
    @controlType actionable
  
    @restrict A
  
    @example
    <h3>Menu</h3>
  
  <example name='menu'>
    <file name='index.html'>
      <cui-button cui-menu-toggle='exampleOneMenu'>Example One Menu Trigger</cui-button>
      <div cui-menu='exampleOneMenu'>
        <cui-menu-item ng-click='click()'>Menu Item</cui-menu-item>
        <cui-menu-item ng-click='click()'>
          <cui-icon icon='heart'></cui-icon> Menu Item 2
        </cui-menu-item>
        <cui-menu-item>
          <div>Multi-Line Item:</div>
          <div>A litte more information</div>
        </cui-menu-item>
      </div>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.click = function() {
            console.log('clicked')
          }
        })
    </file>
    <file name='styles.css'>
      body {
        height: 150px;
      }
    </file>
  </example>
  
    @example
    <h3>Disabled Menu</h3>
  
  <example name='menu'>
    <file name='index.html'>
      <cui-button cui-menu-toggle="exampleThreeMenu" ng-disabled='isDisabled'>Example Three Menu</cui-button>
      <div cui-menu='exampleThreeMenu'>
        <cui-menu-item ng-click='click()'>Item One</cui-menu-item>
      </div>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.isDisabled = true;
  
          $scope.click = function() {
            console.log('clicked')
          }
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.menu', ['cui.base']);

  module.directive('cuiMenuToggle', function() {
    return {
      restrict: 'A',
      priority: 1,
      link: function(scope, element, attrs) {
        var menu;
        menu = attrs.cuiMenuToggle;
        return element.on('click', function(event) {
          scope.$broadcast("cui:toggle:" + menu);
          if (event) {
            event.preventDefault();
            return event.stopPropagation();
          }
        });
      }
    };
  });

  module.directive('cuiMenu', function(baseTemplatePath, $document) {
    var currentMenuOpen;
    currentMenuOpen = null;
    return {
      restrict: 'A',
      replace: true,
      compile: function(element, attrs) {
        var menu;
        element.addClass('cui-menu');
        menu = attrs.cuiMenu;
        return function(scope, element, attrs) {
          var closeMenu, openMenu;
          element.attr('aria-expanded', false);
          closeMenu = function() {
            element.removeClass('cui-menu-showing');
            element.attr('aria-expanded', false);
            $document.unbind('click', closeMenu);
            return element.unbind('click', closeMenu);
          };
          openMenu = function() {
            if (currentMenuOpen != null) {
              currentMenuOpen.removeClass('cui-menu-showing');
            }
            currentMenuOpen = element;
            element.addClass('cui-menu-showing');
            element.attr('aria-expanded', true);
            $document.bind('click', closeMenu);
            return element.bind('click', closeMenu);
          };
          scope.$on("cui:toggle:" + menu, function() {
            if (element.hasClass('cui-menu-showing')) {
              return closeMenu();
            } else {
              return openMenu();
            }
          });
          scope.$on("cui:close:" + menu, function() {
            if (element.hasClass('cui-menu-showing')) {
              return closeMenu();
            }
          });
          return scope.$on("cui:open:" + menu, function() {
            if (!element.hasClass('cui-menu-showing')) {
              return openMenu();
            }
          });
        };
      }
    };
  });

  module.directive('cuiMenuItem', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "menuItem.html",
      restrict: 'EA',
      priority: 5,
      transclude: true,
      replace: true
    };
  });


  /**
    @ngdoc directive
    @name navigationList
  
    @description The Navigation list is used for primary navigation on the
    page. It can have infinite levels, but only tabs up to five. We recommend
    only up to two levels for best practices.
    
    @module cui.controls
    @src controls/navigationList/navigationList.coffee
    @controlType navigation
  
    @restrict E
  
    @param {array} items An array of node objects. Each node must
    have a 'label' property, and optionally an array of nodes as the 'children'
    property. Additionally, each node can have an 'href' and an 'icon'
    (see cui.controls.icon) (we only recommend an icon on the toplevel nodes).
  
    [
      {
        label: 'Home',
        icon: 'house'
        children: [
          {
            label: 'A child'
          }
        ]
      }, {
        label: 'Logout',
        href: 'logout.php'
      }
    ]
  
    @param {string=} persistAs A label of the navigation list to keep in sessionStorage. Only the expanded/collapsed state is saved. If the name or ordering of the nodes significantly changes between the cache and new list, the state(s) are not kept.
  
  If persistAs is falsy, the state is not kept upon refreshing.
  
    @param {boolean=} watchLocation If true, the navigationList will watch for an angular $locationChangeStart and update the selected item if it finds a matching href. It will also check and select the appropriate node on page population (load)
  
    @example
    <h3>Basic navigation list</h3>
    <p>Close 'settings', open 'jobs', select something, and finally refresh the page to see persistance of 'jobs' open state and 'kernel' reselected (as well as menus containing being opened). This is due to location-change-start looking on page load and seeing the current page url match the href of 'kernel'.</p>
  <example name='navigationList'>
    <file name='index.html'>
      <cui-navigation-list items="treeFamily" persist-as="my-example" watch-location=true></cui-navigation-list>
    </file>
  
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.treeFamily = [{
            label: "Home",
            icon: "home"
          },{
            label: "Services and tasks to do for this step",
            icon: "tasks"
          },{
            label: "Profiles",
            icon: "user"
          },{
            label: "Templates",
            icon: "tags"
          },{
            label: "Devices",
            icon: "laptop"
          },{
            label: "Jobs",
            icon: "refresh",
            children: [{
              label: "Defrag"
            },{
              label: "Convert"
            }]
          },{
            label: "Settings",
            icon: "cog",
            children: [{
              label: "Users"
            },{
              label: "Appliance Management for the user portal"
            },{
              label: "Advanced",
              children: [{
                label: "Console"
              },{
                label: "Logs"
              },{
                label: "Kernel",
                href: "about:blank" // Since we're in an iframe, this is the url...
              }]
            }]
          }]
        })
    </file>
    <file name='styles.css'>
      .cui-navigation-list {
        width: 200px
      }
    </file>
  </example>
   */

  module = angular.module('cui.controls.navigationList', ['cui.base']);

  module.directive('cuiNavigationList', function(baseTemplatePath, $compile) {
    return {
      templateUrl: "" + baseTemplatePath + "navigationList.html",
      restrict: 'E',
      scope: {
        items: '=',
        persistAs: '@',
        watchLocation: '@'
      },
      controller: function($scope, $location) {
        var loadStates, mergeItems, nodes, selectNode;
        if ($scope.watchLocation) {
          $scope.$on('$locationChangeStart', function(o, next, curr) {
            var node, _i, _len;
            for (_i = 0, _len = nodes.length; _i < _len; _i++) {
              node = nodes[_i];
              if ((node.items.href != null) && next.indexOf(node.items.href) !== -1) {
                selectNode(node);
              }
            }
          });
        }
        nodes = $scope.nodes = [];
        this.addNode = function(node) {
          nodes.push(node);
          if ($scope.watchLocation) {
            if ((node.items.href != null) && $location.absUrl().indexOf(node.items.href) !== -1) {
              selectNode(node);
            }
          }
        };
        selectNode = function(node) {
          var parentScope;
          if ((node.items.href != null) || (node.items.children == null) || node.items.children.length === 0) {
            angular.forEach(nodes, function(n) {
              return n.items.active = false;
            });
            node.items.active = true;
            parentScope = node;
            while (!angular.isArray(parentScope.items)) {
              parentScope = parentScope.$parent.$parent;
              parentScope.items.visible = true;
            }
          }
        };
        this.selectNode = selectNode;
        this.saveStates = function() {
          if ($scope.persistAs) {
            return sessionStorage.setItem('cui-' + $scope.persistAs, angular.toJson($scope.items));
          }
        };
        loadStates = function() {
          var i, item, itemsOld, _i, _len, _ref;
          itemsOld = angular.fromJson(sessionStorage.getItem('cui-' + $scope.persistAs));
          if (($scope.items != null) && (itemsOld != null)) {
            _ref = $scope.items;
            for (i = _i = 0, _len = _ref.length; _i < _len; i = ++_i) {
              item = _ref[i];
              if (itemsOld[i] != null) {
                mergeItems($scope.items[i], itemsOld[i]);
              }
            }
          }
        };
        mergeItems = function(dest, src) {
          var child, i, _i, _len, _ref;
          if ((dest.label != null) && (src.label != null) && dest.label === src.label) {
            if ((dest.visible == null) && (src.visible != null)) {
              dest.visible = src.visible;
            }
            if ((dest.children != null) && (src.children != null)) {
              _ref = dest.children;
              for (i = _i = 0, _len = _ref.length; _i < _len; i = ++_i) {
                child = _ref[i];
                if (src.children[i] != null) {
                  mergeItems(dest.children[i], src.children[i]);
                }
              }
            }
          }
        };
        if ($scope.persistAs) {
          loadStates();
        }
        return this;
      }
    };
  });

  module.directive('cuiNavigationListItem', function(baseTemplatePath, $compile) {
    return {
      templateUrl: "" + baseTemplatePath + "navigationListItem.html",
      restrict: 'E',
      require: '^cuiNavigationList',
      scope: {
        items: '=',
        toplevel: '@'
      },
      compile: function(tElement, tAttr) {
        var compiledContents, contents;
        contents = tElement.contents().remove();
        compiledContents = void 0;
        return function(scope, iElement, iAttr, cuiNavigationListController) {
          if (!compiledContents) {
            compiledContents = $compile(contents);
          }
          compiledContents(scope, function(clone) {
            return iElement.append(clone);
          });
          cuiNavigationListController.addNode(scope);
          scope.collapsed = !scope.items.visible;
          scope.$watch('items.visible', function(v) {
            if (v) {
              scope.collapsed = false;
            }
            return cuiNavigationListController.saveStates();
          });
          scope.$on('cui:collapse:collapsed', function(e) {
            e.stopPropagation();
            return scope.collapsed = true;
          });
          scope.$on('cui:collapse:expanded', function(e) {
            return e.stopPropagation();
          });
          return scope.showTab = function() {
            if ((scope.items.children != null) && scope.items.children.length > 0) {
              scope.items.visible = !scope.items.visible;
            } else {
              scope.$emit('cui:navigationList:itemClick', scope.items);
            }
            return cuiNavigationListController.selectNode(scope);
          };
        };
      }
    };
  });


  /**
    @ngdoc directive
    @name pane
    @description The Pane groups related content on a page in an area on the page. Panes have a title, an optional icon, and are optionally collapsible.
  
    @module cui.controls
    @src controls/pane/pane.coffee
    @controlType presentational
  
    @directive cuiPane
    @restrict E
  
    @transclude The content of the Pane. Can include HTML and other CUI Components.
  
    @param {(boolean|expression)=} collapsed The collapsed state of the Pane.
    @param {boolean=} collapsible Sets whether the Pane's collapsed state can be toggled by the user.
    @param {string=} icon An [Icon](../icon) which is displayed next to the `cui-title`.
    @param {string} cuiTitle The title for the Pane.
  
  Note: This parameter was renamed from `title` to `cuiTitle` to prevent conflicts with the HTML5 `title` global attribute. Backwards compatibility will be removed in CUI 2.7.0.
  
    @example
    <h3>Set of Panes in a CUI CSS Grid</h3>
  <example>
    <file name='index.html'>
      <div class='cui-grid'>
        <cui-pane class='cui-unit cui-two-thirds' cui-title='Profile' icon='user'>
          <div ng-include="'form.html'"></div>
        </cui-pane>
        <div class='cui-unit cui-one-third'>
          <cui-pane cui-title='Help' class='help-pane' collapsible=false collapsed=false>
            <p class='light'>This pane is styled by applying a class on the &lt;cui-pane&gt;. Background-color is inherited from the parent element.</p>
          </cui-pane>
          <cui-pane cui-title='New Features' icon='lightbulb' collapsed=true>
            <img src='http://lorempixel.com/g/800/600/technics' width='100%'>
            
            <p class='light'>Lorem ipsum Cillum consequat sit quis dolore consequat et eiusmod magna sed in Duis ullamco magna aute occaecat in dolore et laborum laboris ut est sint reprehenderit qui in cillum.</p>
          </cui-pane>
        </div>
      </div>
    </file>
  
    <file name='form.html'>
          <form name='userdetails' class='cui-form-stacked' novalidate>
          <div class='cui-form-group'>
            <label for='firstname'>First Name</label>
            <cui-text-box type='text' id='firstname' name='firstname'
              ng-model='stackedform.user.firstname'
              cui-memo='stackedform.memos.firstname'
              ng-pattern='validators.chars'
              required></cui-text-box>
          </div>
  
          <div class='cui-form-group'>
            <label for='lastname'>Last Name</label>
            <cui-text-box type='text' id='lastname' name='lastname'
              ng-model='stackedform.user.lastname'
              cui-memo='stackedform.memos.lastname'
              ng-pattern='validators.chars'
              required></cui-text-box>
          </div>
  
          <div class='cui-form-group'>
            <label for='age'>Age</label>
            <cui-text-box type='number' id='age' name='age'
              step='1' min='0' max='120'
              ng-model='stackedform.user.age'
              cui-memo='stackedform.memos.age'
              required></cui-text-box>
          </div>
  
          <div class='cui-form-group'>
            <label for='gender'>Gender</label>
            <select class="cui-drop-down-list" id='gender' name='gender'
              ng-model='stackedform.user.gender'
              cui-memo='stackedform.memos.gender'
              required>
                <option value='male'>Male</option>
                <option value='female'>Female</option>
            </select>
          </div>
  
          <div class='cui-form-group'>
            <label for='relationship'>Marital Status</label>
            <cui-drop-down-list id='relationship' name='relationship'
              ng-model='stackedform.user.relationship'
              cui-memo='stackedform.memos.relationship'
              required>
              <option value='single'>Single</option>
              <option value='married'>Married</option>
            </cui-drop-down-list>
          </div>
  
          <div class='cui-form-group'>
            <label for='usercomment'>Comments</label>
            <cui-textarea id='usercomment' name='usercomment'
              ng-model='stackedform.user.comment'
              cui-memo='stackedform.memos.comment'
              maxlength=1000
              required></cui-textarea>
          </div>
  
          <cui-button ng-disabled='userdetails.$pristine' ng-click='reset()'>Reset</cui-button>
          <cui-button type='primary' ng-disabled='userdetails.$invalid'>Submit</cui-button>
        </form>
  
      </file>
  
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.$on('cui:pane:collapsed', function(e, s) {
            console.log('collapsed', s)
          })
          $scope.$on('cui:pane:expanded', function(e, s) {
            console.log('expanded', s)
          })
          $scope.reset = function(user) {
            $scope.stackedform.user = {}
          };
          $scope.validators = { chars: /[A-Za-z]{3}/ }
          $scope.stackedform = {
            memos: {
              firstname: {
                required: 'Your first name is required',
                pattern: 'C\'mon that can\'t be your first name!'
              },
              lastname: {
                required: 'Your last name is also required',
                pattern: 'Really, that\'s your last name?'
              },
              age: {
                required: 'We won\'t tell',
                pattern: 'Whhaaa?'
              },
              gender: {
                required: 'Please select one'
              },
              relationship: {
                required: 'Not really our business, but tell us anyway'
              },
              comment: {
                required: 'Say what you will!'
              },
              awesome: {
                required: 'Bet you are!'
              }
            }
          }
          $scope.alignedform = {
            users: {},
            memos: {
              firstname: {
                required: 'Your first name is required',
                pattern: 'C\'mon that can\'t be your first name!'
              },
              lastname: {
                required: 'Your last name is also required',
                pattern: 'Really, that\'s your last name?'
              }
            }
          }
          $scope.inlineform = {
            users: {},
            memos: {
              name: {
                description: 'Please enter your first and last name',
                required: 'Missing a required field',
                pattern: 'First and Last names must have at least 3 characters',
                valid: 'You got it!'
              }
            }
          }
  
        })
    </file>
  
    <file name='styles.css'>
      .light {
        color: #888;
      }
  
      .help-pane {
        background: #eee;
        color: #0085c3;
        border-color: #ce1126;
      }
  
    </file>
  
  </example>
  
  <example>
    <file name='index.html'>
      <cui-button ng-click='pane1 = !pane1'>Toggle Pane 1</cui-button>
      <cui-button ng-click='pane2 = !pane2'>Toggle Pane 2</cui-button>
      <cui-button ng-click='pane3 = !pane3'>Toggle Pane 3</cui-button>
      <cui-button ng-click='pane4 = !pane4'>Toggle Pane 4</cui-button>
      <cui-pane cui-title='Pane 1' collapsed='pane1'>
        Lorem ipsum Amet mollit labore qui nostrud laborum deserunt et dolore ut nostrud dolore in in id irure dolor ad cillum exercitation commodo adipisicing minim Ut laborum fugiat est deserunt fugiat ut est voluptate dolore velit aute commodo laboris dolore eiusmod pariatur aliquip id in laborum laboris cupidatat est occaecat Ut irure dolor aute nisi laborum ullamco ea incididunt ea fugiat nulla reprehenderit et nostrud minim eu reprehenderit pariatur minim aliqua Excepteur culpa incididunt enim magna qui quis deserunt dolor laborum labore qui in in commodo culpa cupidatat fugiat cillum eu irure mollit et reprehenderit in mollit id incididunt est anim fugiat in aliqua mollit tempor cillum commodo Ut veniam dolor aute dolor ullamco dolor ullamco dolore ex amet Excepteur magna veniam aliqua veniam labore sunt reprehenderit magna ad nulla irure dolor aliquip esse deserunt irure et proident ex ad deserunt laborum.
      </cui-pane>
      <cui-pane cui-title='Pane 2' collapsed='pane2'>
        Lorem ipsum Minim id non ullamco ad ullamco dolore officia ut dolore voluptate sit id consectetur aute elit aute aliqua veniam non sed sed deserunt veniam ullamco tempor id enim cillum minim occaecat proident id laboris magna occaecat laboris velit irure quis Ut Excepteur non incididunt reprehenderit ut in incididunt cillum pariatur ex sit proident reprehenderit dolore fugiat veniam nostrud id nulla non magna culpa sunt irure dolore eiusmod in veniam velit Duis magna proident aliqua ut reprehenderit cillum adipisicing amet culpa reprehenderit mollit irure Duis Duis est dolore dolore laboris esse incididunt in adipisicing esse sint minim amet aute Ut sunt pariatur ut elit ea voluptate ex sunt adipisicing aliqua laboris non sunt officia nisi tempor sed cillum quis ex commodo tempor elit aute non aliqua nisi Excepteur dolore deserunt et nulla labore veniam Duis ut fugiat sint mollit aliqua sed pariatur id enim sunt ea in adipisicing dolor do ut ut qui commodo eiusmod proident ea pariatur eiusmod proident nostrud officia est dolore irure labore qui do ut id quis ut minim elit quis sed esse ea et pariatur magna elit velit proident culpa nostrud consectetur labore quis eiusmod id consectetur incididunt fugiat qui ex consectetur do anim Excepteur minim mollit Excepteur consectetur proident id sint ullamco nulla occaecat dolor commodo ut do anim in velit dolor incididunt nostrud dolor qui eu pariatur et sunt in eiusmod ex dolor laboris esse est commodo dolore nostrud.
      </cui-pane>
      <cui-pane cui-title='Pane 3' collapsed='pane3'>
        Lorem ipsum Mollit pariatur est proident aliqua quis sunt ut proident quis magna magna in consectetur culpa exercitation minim ex sint magna minim do magna sed Ut nulla qui aute veniam ullamco nisi aute Duis nulla nostrud ea aliquip id dolore id incididunt sed laboris reprehenderit veniam laborum eu do ut reprehenderit incididunt occaecat minim Excepteur magna occaecat consequat irure magna dolor sint in aliqua magna consectetur nisi pariatur quis cillum ea consequat ad cupidatat magna cupidatat ad consectetur Excepteur nulla labore adipisicing sed est dolore occaecat dolor nostrud in consequat dolore sint in dolor proident nostrud fugiat deserunt dolore commodo in officia ad est aliquip qui veniam cupidatat in nostrud reprehenderit et dolore ullamco labore dolore in in aute labore incididunt anim anim dolore incididunt ullamco nostrud sint reprehenderit ea laboris consectetur elit pariatur irure officia nulla sit Duis incididunt aliquip aute eu qui dolore nisi ad eiusmod elit culpa dolor dolore do anim ullamco reprehenderit in quis irure cillum do Duis Duis in elit sit Duis irure dolor minim adipisicing in Excepteur fugiat aliqua Duis elit veniam tempor minim sint ea sint amet fugiat fugiat aliquip adipisicing occaecat Duis eu Duis eiusmod ea exercitation veniam officia non cillum laborum esse dolore ut ea occaecat ea aute officia tempor consequat ex id.
      </cui-pane>
      <cui-pane cui-title='Pane 4' collapsed='pane4'>
        Lorem ipsum Ea exercitation minim incididunt ullamco cillum dolore sint cupidatat fugiat dolore ex et pariatur id sint incididunt velit Excepteur labore officia Excepteur Duis in id incididunt et laborum laboris nulla mollit ex dolore eiusmod consequat cillum occaecat et commodo dolore est aute ea sed proident quis aute non anim in dolore et culpa adipisicing nisi cillum nulla incididunt esse voluptate nisi ex velit ullamco proident exercitation tempor aliqua sint enim mollit commodo magna in deserunt Duis do dolore exercitation proident Excepteur officia dolor Ut proident ullamco veniam Excepteur irure officia nostrud qui culpa in non non laborum dolore ullamco laborum non dolore sunt ex in proident velit eiusmod fugiat irure quis et ut laboris ex ullamco irure irure ad irure minim commodo eiusmod in anim dolor ex magna dolor aute cillum ut aliquip incididunt esse adipisicing aliquip cillum veniam et est ea eiusmod est irure eu aliqua eu cupidatat eiusmod fugiat deserunt reprehenderit aute in labore mollit officia est irure elit commodo ut dolor amet Duis consectetur quis irure enim adipisicing proident in fugiat id officia eiusmod dolor id adipisicing adipisicing Excepteur nostrud nostrud dolor fugiat deserunt sed ad magna officia do proident Ut anim nulla cillum ullamco quis sint pariatur Duis do nostrud magna minim tempor in ullamco nisi aute occaecat dolore minim consequat eiusmod officia mollit quis occaecat culpa occaecat in anim eiusmod labore.
      </cui-pane>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.pane1 = false;
          $scope.pane2 = false;
          $scope.pane3 = false;
          $scope.pane4 = false;
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.pane', ['cui.base']);

  module.directive('cuiPane', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "pane.html",
      restrict: 'EA',
      transclude: true,
      scope: {
        cuiTitle: '@',
        title: '@',
        collapsible: '=?',
        collapsed: '=?'
      },
      compile: function(element, attrs) {
        var paneEl;
        if (attrs.collapsed) {
          element.addClass('cui-pane-animate');
        }
        paneEl = element.find('.cui-pane');
        return function(scope, element, attrs) {
          var collapse, collapsedIsAssignable, expand, toggle;
          collapsedIsAssignable = function() {
            return attrs.collapsed !== 'true' && attrs.collapsed !== 'false';
          };
          scope._collapsed = scope.collapsed != null ? scope.collapsed : false;
          scope._collapsible = scope.collapsible != null ? scope.collapsible : true;
          scope.icon = attrs.icon || '';
          scope.collapseIcon = scope._collapsed ? 'angle-right' : 'angle-down';
          collapse = function(emit) {
            scope._collapsed = true;
            if (collapsedIsAssignable()) {
              scope.collapsed = true;
            }
            scope.collapseIcon = 'angle-right';
            if (emit) {
              return scope.$emit('cui:pane:collapsed', scope);
            }
          };
          expand = function(emit) {
            scope._collapsed = false;
            if (collapsedIsAssignable()) {
              scope.collapsed = false;
            }
            scope.collapseIcon = 'angle-down';
            if (emit) {
              return scope.$emit('cui:pane:expanded', scope);
            }
          };
          toggle = function(currentlyCollapsed, emit) {
            if (emit == null) {
              emit = true;
            }
            switch (currentlyCollapsed) {
              case true:
                return collapse(emit);
              case false:
                return expand(emit);
            }
          };
          scope.toggleCollapse = function() {
            if (!scope._collapsible) {
              return;
            }
            return toggle(!scope._collapsed);
          };
          scope.$watch('collapsed', function(collapsed) {
            return toggle(collapsed, collapsed !== scope._collapsed);
          });
          scope.$on('cui:pane:collapsed', function() {
            var addAnimate;
            addAnimate = function(e) {
              if (angular.element(e.target === paneEl)) {
                element.addClass('cui-pane-animate');
                return element.off('mouseout', addAnimate);
              }
            };
            return element.on('mouseout', addAnimate);
          });
          return scope.$on('cui:pane:expanded', function(e) {
            return element.removeClass('cui-pane-animate');
          });
        };
      }
    };
  });


  /**
    @ngdoc directive
    @name progressBar
  
    @description Display a progress bar for operations that take longer than approximately 5 seconds. Include a description of the task that is running. When possible, allow users to cancel the task by clicking a cancel button.
  
    @module cui.controls
    @src controls/progressBar/progressBar.coffee
    @controlType presentational
  
    @restrict E
  
    @param {number=} value - A value ranging from zero to 100 or `null`/`undefined`, in which case the progress bar turns into an indeterminate progress bar (striped).
    @param {string=} cuiType - The type of progress bar.
  
    @example
  <h3>Regular progress bars</h3>
  <example name='basicBrogressBar'>
    <file name='index.html'>
      <cui-progress-bar value=25></cui-progress-bar>
      <br />
      <cui-progress-bar value=35 cui-type="warning"></cui-progress-bar>
      <br />
      <cui-progress-bar value=45 cui-type="danger"></cui-progress-bar>
      <br />
      <cui-progress-bar value=55 cui-type="green"></cui-progress-bar>
      <br />
      <cui-progress-bar value=65 cui-type="black"></cui-progress-bar>
      <br />
      <cui-progress-bar value=75 cui-type="violet"></cui-progress-bar>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
    
        })
    </file>
  </example>
  
    @example
    <h3>Indeterminate progress bars</h3>
  <example name='indeterminateProgressBar'>
    <file name='index.html'>
      <cui-progress-bar></cui-progress-bar>
      <br />
      <cui-progress-bar cui-type="warning"></cui-progress-bar>
      <br />
      <cui-progress-bar cui-type="danger"></cui-progress-bar>
      <br />
      <cui-progress-bar cui-type="green"></cui-progress-bar>
      <br />
      <cui-progress-bar cui-type="black"></cui-progress-bar>
      <br />
      <cui-progress-bar cui-type="violet"></cui-progress-bar>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
    
        })
    </file>
  </example>
  
    @example
    <h3>Loading progress bar</h3>
  <example name='loadingProgressBar'>
    <file name='index.html'>
      <cui-progress-bar value='{{value}}'></cui-progress-bar>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $timeout) {
          $scope.value = 0;
          // Set new timeout for random period (between 0.5s and 1s)
          (function loop() {
            var rand = Math.round(Math.random() * 500) + 500;
            $timeout(function() {
              if($scope.value > 100) {
                $scope.value = 100;
              } else if($scope.value === 100) {
                $scope.value = 0;
              } else {
                $scope.value += Math.round(Math.random() * 25) + 5;
              }
              loop(); // Call encapsulated, labelled anonymous function recursively
            }, rand);
          }());
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.progressBar', ['cui.base']);

  module.directive('cuiProgressBar', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "progressBar.html",
      restrict: 'EA',
      scope: {
        value: '@',
        cuiType: '@'
      }
    };
  });


  /**
    @ngdoc directive
    @name radio
  
    @description Radio buttons let users choose one option in a set of related, mutually exclusive options.
  
  __Points to note__
  
  1. It is good practice to provide a <code>name</code> attribute to all form inputs, even though with AngularJS it is not required since they are bound via the <code>ng-model</code> attribute.
  
  2. When creating a group of cui-radio buttons individually, you can use <code>ng-required</code> or <code>required</code> attributes in order to require a selction. Using <code>ng-required</code> will allow you to provide an expression or function that sets the required state dynamically.
    
    @module cui.controls
    @src controls/radio/radio.coffee
    @controlType input
  
    @restrict E
    @transclude The label
  
    @param {string} ngModel Value of the radio button
    @param {string} value The name of the scope variable to provide the selected radio button's value.
    Radio buttons with the same name are grouped.
  
    @example
    <h3>Basic radio button set</h3>
  
  <example name='basicRadio'>
    <file name='index.html'>
      <h3>Favorite symphony orchestra</h3>
      <cui-radio ng-model='orchestra' name='orchestra' value='amsterdam'>Royal Concertgebouw Orchestra, Amsterdam</cui-radio><br>
      <cui-radio ng-model='orchestra' name='orchestra' value='berlin'>Berlin Philharmonic</cui-radio><br>
      <cui-radio ng-model='orchestra' name='orchestra' value='vienna'>Vienna Philharmonic</cui-radio><br>
      <cui-radio ng-model='orchestra' name='orchestra' value='london'>London Symphony Orchestra</cui-radio><br>
      <cui-radio ng-model='orchestra' name='orchestra' value='chicago'>Chicago Symphony Orchestra</cui-radio><br>
      <cui-radio ng-model='orchestra' name='orchestra' value='bavarian'>Bavarian Radio Symphony Orchestra</cui-radio>
  
      <p>Your choice: {{orchestra}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Radio buttons created individually</h3>
    <dl>
      <dt>ng-model</dt>
      <dd>Since this is not a group, we provide the model to store the selected value</dd>
      <dt>name</dt>
      <dd>Ties multiple radios to an ad-hoc group</dd>
      <dt>value</dt>
      <dd>Designates the value of the current selection</dd>
      <dt>ng-required</dt>
      <dd>Using <code>ng-required</code> as opposed to <code>required</code> allows the ability to dynamically control the required state</dd>
    </dl>
  
  <example name='individualRadio'>
    <file name='index.html'>
      <form novalidate name='radioExampleForm' class='cui-form-stacked'>
  
        <div class='cui-form-group'>
  
          <label>What would make a good movie?</label>
  
          <cui-radio ng-model="formData.movie"
                     name='movie'
                     value='Missle Aliens'
                     ng-disabled='isDisabled'
                     ng-required='isRequired'>
            Aliens with missles on their shoulders
          </cui-radio>
          <cui-radio ng-model="formData.movie"
                     name='movie'
                     value='Walking Sharks'
                     ng-disabled='isDisabled'
                     ng-required='isRequired'>
            Walking, man eating sharks
          </cui-radio>
          <cui-radio ng-model="formData.movie"
                     name='movie'
                     value='Ninja Monkeys'
                     ng-disabled='isDisabled'
                     ng-required='isRequired'>
            Killer monkey ninjas that come from underground
          </cui-radio>
  
        </div>
        <code>formData.movie: {{formData.movie}}</code>
  
        <cui-button ng-click='toggleDisabled()'>{{isDisabled && 'Enable' || 'Disable'}} radios</cui-button>
        <cui-button ng-click='makeRequired()'>{{isRequired && 'Optional ' || 'Require'}} radios</cui-button>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.isRequired = false;
          $scope.makeRequired = function() {
            $scope.isRequired = !$scope.isRequired
            $scope.requiredState = !$scope.requiredState
          }
  
          $scope.isDisabled = false;
          $scope.toggleDisabled = function() {
            $scope.isDisabled = !$scope.isDisabled
          }
        });
    </file>
  </example>
  
    @example
    <h3>Radio buttons, dynamically created as a group</h3>
    <dl>
      <dt>radio-items</dt>
      <dd>Object that contains the radio group value(s) and label(s)</dd>
      <dt>radio-model</dt>
      <dd>Selected value model</dd>
      <dt>radio-label</dt>
      <dd>Key for the label of the radio (in radio-items)</dd>
      <dt>radio-value</dt>
      <dd>Key for the value of the radio (in radio-items)</dd>
    </dl>
  
  <example name='groupRadio'>
    <file name='index.html'>
      <form novalidate name='radioExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
  
          <label>Select a color</label>
          <cui-radio-group name='colors' ng-disabled='isDisabled' ng-required='isRequired'
                           radio-items='radioExample.colors'
                           radio-model="formData.colors"
                           radio-label='color'></cui-radio-group>
  
        </div>
        
        <code>formData.colors: {{formData.colors}}</code>
  
        <cui-button ng-click='toggleDisabled()'>{{isDisabled && 'Enable' || 'Disable'}} radios</cui-button>
        <cui-button ng-click='makeRequired()'>{{isRequired && 'Optional ' || 'Require'}} radios</cui-button>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.formData = []
  
  
          $scope.isRequired = false;
          $scope.makeRequired = function() {
            $scope.isRequired = !$scope.isRequired
            $scope.requiredState = !$scope.requiredState
          }
  
          $scope.isDisabled = false;
          $scope.toggleDisabled = function() {
            $scope.isDisabled = !$scope.isDisabled
          }
  
          $scope.radioExample = [];
          $scope.radioExample.colors = [
              {
                color: 'Red',
                meaning: 'anger'
              },
              {
                color: 'Green',
                meaning: 'wealth'
              },
              {
                color: 'Blue',
                meaning: 'sad'
              }
          ];
        });
    </file>
  </example>
  
    @example
    <h3>Simple radio group</h3>
    <p>If <code>radio-value</code> is not defined, the value will fall back to <code>radio-label</code> if it is defined. If neither are defined, then it is assumed an array was given for <code>radio-items</code> and each array element will be treated as the label and the value.</p>
  <example name='simpleRadioGroup'>
    <file name='index.html'>
      <form novalidate name='radioExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <cui-radio-group name='roles'
                                  radio-items='radioExample.roles'
                                  radio-model="formData.roles"></cui-radio-group>
  
          <code>formData.roles: {{formData.roles}}</code>
        </div>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.formData = []
          $scope.radioExample = [];
          $scope.radioExample.roles = ['Designer', 'Developer', 'Administrator'];
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.radio', ['cui.base']);

  module.directive('cuiRadio', function(baseTemplatePath, $log) {
    return {
      templateUrl: "" + baseTemplatePath + "radio.html",
      restrict: 'EA',
      require: '?ngModel',
      replace: true,
      transclude: true,
      scope: {
        name: '@',
        value: '@',
        ngModel: '=',
        ngRequired: '=',
        ngDisabled: '='
      },
      compile: function(element, attrs) {
        var radio;
        radio = element.find('input');
        if (attrs.name == null) {
          $log.cuiError({
            cuiErrorType: 'name',
            cuiErrorCtrl: 'cui-radio',
            cuiErrorElIdentity: attrs.name || attrs.id || 'UNKNOWN'
          });
        } else {
          radio.attr('name', attrs.name);
        }
        return function(scope, iElement, iAttrs, ngModelCtrl) {
          if (ngModelCtrl == null) {
            $log.cuiError({
              cuiErrorType: 'ngmodel',
              cuiErrorCtrl: 'cui-radio',
              cuiErrorElIdentity: iAttrs.name || iAttrs.id || 'UNKNOWN'
            });
            return;
          }
          return iElement.bind('keypress', function(event) {
            switch (event.which) {
              case 32:
                event.preventDefault();
                return document.activeElement.click();
            }
          });
        };
      }
    };
  });

  module.directive('cuiRadioGroup', function(baseTemplatePath, $log) {
    return {
      templateUrl: "" + baseTemplatePath + "radiogroup.html",
      restrict: 'EA',
      require: '?ngModel',
      replace: true,
      scope: {
        name: '@',
        ngRequired: '=',
        ngDisabled: '=',
        radioModel: '=',
        radioItems: '='
      },
      compile: function(element, attrs) {
        var radio;
        radio = element.find('input');
        if (attrs.name == null) {
          $log.cuiError({
            cuiErrorType: 'name',
            cuiErrorCtrl: 'cui-radio-group',
            cuiErrorElIdentity: attrs.name || attrs.id || 'UNKNOWN'
          });
        } else {
          radio.attr('name', attrs.name);
        }
        return function(scope, iElement, iAttrs, ngModelCtrl) {
          if (iAttrs.radioModel == null) {
            $log.cuiError({
              cuiErrorType: 'radiomodel',
              cuiErrorCtrl: 'cui-radio-group',
              cuiErrorElIdentity: iAttrs.name || iAttrs.id || 'UNKNOWN'
            });
            return;
          }
          scope.radiolabel = iAttrs.radioLabel != null ? 'option.' + iAttrs.radioLabel : 'option';
          if (iAttrs.radioValue || iAttrs.radioLabel) {
            return scope.radiovalue = 'option.' + (iAttrs.radioValue || iAttrs.radioLabel);
          } else {
            return scope.radiovalue = 'option';
          }
        };
      }
    };
  });


  /**
    @ngdoc directive
    @name richTextEditor
  
    @description The Rich text editor provides a in page rich text editor
  
  This is v2 of the Cui Rich Text Editor. More features will be coming in future iterations and current features will become more advanced.
  
  This version of the editor allows for easier configuration and sets up the api to allow for user defined tools, which is a feature that is on the roadmap. We've added a color pallete for font color and background color, cleaned up the api considerably and have laid the groundwork to allow extraction of the HTML content and, in the future, the ability to extract plain text wherein doing so will preserve line breaks for block level elements.
    
    @module cui.controls
    @src controls/richTextEditor/richTextEditor.coffee
    @controlType input
  
    @restrict E
  
    @param {object=} config This object has the following properties with the following default properties
    
    {
      content: '', # This is where to set and retrieve the content of the editor
      editor: {
        id: 'cuiEditor',    # A unique id for the editor
        editMode: 'richtext', # sets the mode of the editor ['richtext' || 'html']
        isEditable: true,
        dimensions: {
          width: 800,
          height: 400
        }
      },
      tools: { # leave empty or set individula tools to false if you do now want to provide them
        bold:  ,
        italic: ,
        underlive: ,
        strikethrough: ,
        increasefont: ,
        decreasefont: ,
        forecolor: ,
        hilitecolor: ,
        justifyleft: ,
        justifycenter: ,
        justifyright: ,
        clear: ,
        heading: ,
        insertorderedlist: ,
        insertunorderedlist: ,
        indent: ,
        outdent: ,
        html:
  
      }
    }
    
    @example
  <example name='richTextEditor'>
    <file name='index.html'>
      <cui-rich-text-editor config="settings" ng-model='settings.content'></cui-rich-text-editor>
      <div><cui-button ng-click='getContent()'>Output Content to console</cui-button></div>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.enableEditor = true
          $scope.settings = {
            content: '<h1>Heading 1</h1><p style="font-weight:bold">This is some content.</p>',
            editor: {
              id: null,
              editMode: 'richtext',
              isEditable: true,
              dimensions: {
                width: 800,
                height: 300
              }
            },
            tools: {}
          }
  
          $scope.getContent = function() {
            console.log('--------------------------------------------')
            console.log('Editor Mode:', $scope.settings.editor.editMode)
            console.log('Button contentAsText:', $scope.settings.content)
            console.log('--------------------------------------------')
          }
        })
    </file>
    <file name='app.css'>
    body {
      height: 400px;
    }
    </file>
  </example>
   */

  module = angular.module('cui.controls.richTextEditor', ['cui.base']);

  module.directive('cuiRichTextEditor', function($rootScope, $compile, $sce, $document, $log, baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "richTextEditor.html",
      restrict: 'EA',
      transclude: true,
      replace: true,
      require: '?ngModel',
      scope: {
        config: '=',
        ngModel: '='
      },
      link: function(scope, el, attrs, ngModel) {
        if (ngModel == null) {
          $log.cuiError({
            cuiErrorType: 'ngmodel',
            cuiErrorCtrl: 'cui-rich-text-editor',
            cuiErrorElIdentity: attrs.name || attrs.id || 'UNKNOWN'
          });
          return;
        }
        scope.cuiRte = {
          editor: {
            id: 'cuiEditor',
            editMode: 'richtext',
            allowImages: true,
            fontSizeType: 'px',
            dimensions: {
              width: 800,
              height: 400
            }
          },
          tools: {
            bold: {
              type: 'button',
              label: '<cui-icon icon="bold"></cui-icon>',
              action: 'bold'
            },
            italic: {
              type: 'button',
              label: '<cui-icon icon="italic"></cui-icon>',
              action: 'italic'
            },
            underline: {
              type: 'button',
              label: '<cui-icon icon="underline"></cui-icon>',
              action: 'underline'
            },
            strikethrough: {
              type: 'button',
              label: '<cui-icon icon="strikethrough"></cui-icon>',
              action: 'strikethrough'
            },
            increasefont: {
              type: 'button',
              label: '<cui-icon icon="font"></cui-icon><cui-icon icon="plus"></cui-icon>',
              action: 'fontsize',
              options: function() {
                return Math.abs($document[0].queryCommandValue('fontsize')) + 1;
              }
            },
            decreasefont: {
              type: 'button',
              label: '<cui-icon icon="font"></cui-icon><cui-icon icon="minus"></cui-icon>',
              action: 'fontsize',
              options: function() {
                return Math.abs($document[0].queryCommandValue('fontsize')) - 1;
              }
            },
            forecolor: {
              type: 'menu',
              label: 'Font Color',
              action: 'forecolor',
              "class": 'colormenu',
              menuTemplate: '<div cui-color-picker class="colorslist"></div>'
            },
            hilitecolor: {
              type: 'menu',
              label: 'BG Color',
              action: 'hilitecolor',
              "class": 'colormenu',
              menuTemplate: '<div cui-color-picker class="colorslist"></div>'
            },
            justifyleft: {
              type: 'button',
              label: '<cui-icon icon="align-left"></cui-icon>',
              action: 'justifyLeft',
              options: false
            },
            justifycenter: {
              type: 'button',
              label: '<cui-icon icon="align-center"></cui-icon>',
              action: 'justifyCenter',
              options: false
            },
            justifyright: {
              type: 'button',
              label: '<cui-icon icon="align-right"></cui-icon>',
              action: 'justifyRight',
              options: false
            },
            clear: {
              type: 'button',
              label: '<cui-icon icon="remove"></cui-icon>',
              action: 'removeFormat'
            },
            heading: {
              type: 'menu',
              label: 'Heading',
              "class": 'headingmenu',
              menuTemplate: '<cui-menu-item ng-click=\'command("formatBlock", "<H1>")\'>Heading 1</cui-menu-item>\n<cui-menu-item ng-click=\'command("formatBlock", "<H2>")\'>Heading 2</cui-menu-item>\n<cui-menu-item ng-click=\'command("formatBlock", "<H3>")\'>Heading 3</cui-menu-item>\n<cui-menu-item ng-click=\'command("formatBlock", "<H4>")\'>Heading 4</cui-menu-item>\n<cui-menu-item ng-click=\'command("formatBlock", "<H5>")\'>Heading 5</cui-menu-item>\n<cui-menu-item ng-click=\'command("formatBlock", "<H6>")\'>Heading 6</cui-menu-item>'
            },
            insertunorderedlist: {
              type: 'button',
              label: '<cui-icon icon="list"></cui-icon>',
              action: 'InsertOrderedList'
            },
            insertorderedlist: {
              type: 'button',
              label: '<cui-icon icon="list-ol"></cui-icon>',
              action: 'InsertUnorderedList'
            },
            indent: {
              type: 'button',
              label: '<cui-icon icon="indent-right"></cui-icon>',
              action: 'indent'
            },
            outdent: {
              type: 'button',
              label: '<cui-icon icon="indent-left"></cui-icon>',
              action: 'outdent'
            },
            html: {
              type: 'button',
              label: 'HTML',
              action: 'editmode'
            }
          },
          contenteditable: true,
          api: {
            command: function(cmd, opts) {
              var editor, html;
              editor = document.getElementById(this.editorID);
              if (cmd === 'editmode') {
                this.config.editor.editMode = this.config.editor.editMode === 'html' ? 'richtext' : 'html';
                return ngModel.$render();
              } else {
                if (typeof opts === 'function') {
                  opts = opts();
                } else {
                  opts = opts;
                }
                document.execCommand('styleWithCSS', true);
                document.execCommand(cmd, false, opts);
                html = angular.element(editor).html();
                return ngModel.$setViewValue(html);
              }
            },
            setEditorId: function() {
              var count;
              count = document.querySelectorAll('[contenteditable]').length || '';
              if (scope.config.editor.id != null) {
                return this.editorID = scope.config.editor.id + count;
              } else {
                return this.editorID = scope.cuiRte.editor.id + count;
              }
            },
            editor: function() {
              var editor, editorOptions, htmlStore;
              editorOptions = angular.extend({}, scope.cuiRte.editor, this.config.editor);
              editor = angular.element("<div id='" + this.editorID + "' class='editor'></div>");
              editor.attr('contenteditable', this.config.editor.isEditable);
              editor.attr('editMode', this.config.editor.editMode);
              editor.css({
                'width': "" + editorOptions.dimensions.width + "px",
                'height': "" + editorOptions.dimensions.height + "px"
              });
              editor.attr('ng-model', 'ngModel');
              htmlStore = angular.element("<textarea tabindex='0' ng-model='ngModel' id='" + this.editorID + "-htmlstore' style='display:none'></textarea>");
              $compile(htmlStore)(scope);
              el.append(htmlStore);
              ngModel.$render = (function(_this) {
                return function() {
                  if (_this.config.editor.editMode === 'richtext') {
                    editor.html(ngModel.$viewValue);
                  } else {
                    editor.text(ngModel.$viewValue);
                  }
                };
              })(this);
              editor.bind('keyup', (function(_this) {
                return function(e) {
                  return scope.$apply(function() {
                    var value;
                    if (_this.config.editor.editMode === 'richtext') {
                      value = angular.element(editor).html();
                      return ngModel.$setViewValue(value);
                    } else {
                      value = angular.element(editor).text();
                      return ngModel.$setViewValue(value);
                    }
                  });
                };
              })(this));
              return editor;
            },
            toolbar: function() {
              var action, childScope, opts, setToolTemplate, tool, toolSet, toolbar, toolel;
              toolbar = angular.element('<div class="cui-editor-toolbar"></div>');
              toolSet = angular.extend({}, scope.cuiRte.tools, this.config.tools);
              setToolTemplate = function(tool) {
                var template;
                if (toolSet[tool].type === 'button') {
                  template = "<cui-button size='small'\n  ng-click='command(action, opts)'\n  " + (toolSet[tool].action !== 'editmode' ? "ng-disabled='htmlMode()'" : "") + ">" + toolSet[tool].label + "\n</cui-button>";
                } else {
                  template = "<cui-button size='small' ng-disabled='htmlMode()' cui-menu-toggle=\"\">\n  " + toolSet[tool].label + " <cui-icon icon='caret-down'></cui-icon>\n</cui-button><div cui-menu class='" + toolSet[tool]["class"] + "'>\n  " + toolSet[tool].menuTemplate + "\n</div>";
                }
                return template;
              };
              for (tool in toolSet) {
                if (this.config.tools[tool] !== false) {
                  toolel = angular.element(setToolTemplate(tool));
                  action = toolSet[tool].action;
                  opts = toolSet[tool].options ? toolSet[tool].options : null;
                  childScope = angular.extend(scope.$new(true), scope.cuiRte.api, {
                    action: action,
                    opts: opts
                  }, {
                    htmlMode: function() {
                      if (this.config.editor.editMode !== 'richtext') {
                        return true;
                      }
                    }
                  });
                  toolbar.append($compile(toolel)(childScope));
                }
              }
              return toolbar.bind('mousedown', function(e) {
                return e.preventDefault();
              });
            },
            init: function() {
              this.config = scope.config;
              this.editorID = this.setEditorId();
              el.append(this.editor());
              return el.prepend(this.toolbar());
            }
          }
        };
        return scope.cuiRte.api.init();
      }
    };
  });

  module.directive('cuiColorPicker', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "colorpicker.html",
      restrict: 'A',
      controller: function($scope) {
        return $scope.colorList = [
          {
            'color': '#0085C3',
            'name': 'Dell Blue'
          }, {
            'color': '#9ad3fd',
            'name': 'Dell Blue 2'
          }, {
            'color': '#6E2585',
            'name': 'Dell Purple'
          }, {
            'color': '#7AB800',
            'name': 'Dell Green'
          }, {
            'color': '#F2AF00',
            'name': 'Dell Yellow'
          }, {
            'color': '#FF7700',
            'name': 'Dell Orange'
          }, {
            'color': '#CE1126',
            'name': 'Dell Red'
          }, {
            'color': '#f2f2f2',
            'name': 'Dell Light Gray'
          }, {
            'color': '#e4e4e4',
            'name': 'Dell Gray'
          }, {
            'color': '#282828',
            'name': 'Dell Dark Gray'
          }, {
            'color': '#000000',
            'name': 'Dell Black'
          }
        ];
      }
    };
  });


  /**
    @ngdoc directive
    @name spinner
    
    @description The Spinner loads a loading spinner based on size and color specifications.
    
    @module cui.controls
    @src controls/spinner/spinner.coffee
    @controlType presentational
    
    @restrict E
    
    @param {string=} size CSS property for height and width. Can be in terms of `px`, `%`, `em`, etc.
    @param {string=} color Sets the color for the spinner. Valid colors:
  
    ** `'blue'`
    ** `'white'`
    ** `'black'`
    
    @example
    <h3>Inline Spinner</h3>
  <example name='inlineSpinner'>
    <file name='index.html'>
      <h2>
        ♪
        <em>Like a record, baby, right round round round</em>
        ♫
        <cui-spinner></cui-spinner>
      </h2>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {})
    </file>
  </example>
  
    @example
    <h3>Other spinners on different backgrounds</h3>
  <example name='otherSpinners'>
    <file name='index.html'>
      <div class='blue'>
        <cui-spinner size='32px' color='white'></cui-spinner>
      </div>
      <cui-spinner size='32px' color='black'></cui-spinner>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {})
    </file>
    <file name='styles.css'>
      .blue {
        background-color: #0085c3;
        padding: 25px;
        margin-right: 25px;
        display: inline-block;
      }
    </file>
  </example>
   */

  module = angular.module('cui.controls.spinner', ['cui.base']);

  module.directive('cuiSpinner', function() {
    return {
      restrict: 'EA',
      scope: {
        size: '@',
        color: '@'
      },
      link: function(scope, element) {
        if (scope.color == null) {
          scope.color = 'blue';
        }
        element.addClass('cui-spinner');
        element.addClass("cui-spinner-color-" + scope.color);
        if (scope.size != null) {
          return element.css({
            height: scope.size,
            width: scope.size
          });
        }
      }
    };
  });


  /**
    @ngdoc directive
    @name splitButton
  
    @description The Split button provides a combination of a Button and a Menu. Because the Drop down button
    generates other controls, the same attributes, such as size and color can be set.
  
    @module cui.controls
    @controlType actionable
  
    @restrict E
  
    @transclude <ul cui-keyboard-menu>
                  <li
                  [ng-click=""]>
                  </li>
                  [..]
                </ul>
  
    @param {string=} cuiType The type of the button. See the Button.
  
    @param {expression=} defaultAction Function that should respond when the default action is clicked.
  
    @param {string=} icon The type of icon. See the Icon.
  
    @param {string} label The label of the button, describes the default action.
  
    @param {string=} menuAnchor Define how the menu should anchor itself and in which direction the menu should open towards. `menu-anchor='[right] [left]'`. Default anchor position is `right` if `menuAnchor` is not set.
  
    @param {boolean=} ngDisabled Set to true to disable the drop down button.
  
    @param {string=} size
      The size of the button. One of
  
    ** `'small'`
    ** `'medium'` - default
    ** `'large'`
  
    @example
    <h3>Regular split button</h3>
    <p>Split button with label and color attributes.</p>
  <example name='regularSplitButton'>
    <file name='index.html'>
      <cui-split-button label='Split button' cui-type='primary' icon="dashboard">
        <ul cui-keyboard-menu>
          <li ng-click='click("Example 1 - Item 1")'>Item 1</li>
          <li ng-click='click("Example 1 - Item 2")'>Item 2</li>
          <li ng-click='click("Example 1 - Item 3")'>Item 3</li>
        </ul>
      </cui-split-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.click = function(text) {
            alert(text);
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 150px;
      }
    </file>
  </example>
  
    @example
    <h3>Right aligned split button</h3>
    <p>Split button with the menu-anchor attribute set to right, which anchors the open menu position to the right, aligned with the right edge of the button. The default menu-anchor position is left.</p>
  <example name='rightSplitButton'>
    <file name='index.html'>
      <cui-split-button label='Split button anchor position' menu-anchor='right' style='float: right'>
        <ul cui-keyboard-menu>
          <li ng-click='click("Example 2 - Item 1")'>Item 1</li>
          <li ng-click='click("Example 2 - Item 2")'>Item 2</li>
          <li ng-click='click("Example 2 - Item 3")'>Item 3</li>
        </ul>
      </cui-split-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.click = function(text) {
            alert(text);
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 150px;
      }
    </file>
  </example>
  
    @example
    <h3>Disabled split button</h3>
  <example name='disabledSplitButton'>
    <file name='index.html'>
      <cui-split-button label='Split button disabled' ng-disabled='true'>
        <ul cui-keyboard-menu>
          <li ng-click='click("Example 3 - Item 1")'>Item 1</li>
          <li ng-click='click("Example 3 - Item 2")'>Item 2</li>
          <li ng-click='click("Example 3 - Item 3")'>Item 3</li>
        </ul>
      </cui-split-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.click = function(text) {
            alert(text);
          }
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.splitButton', ['cui.base']);

  module.directive('cuiSplitButton', function(baseTemplatePath, cuiTip, $compile, $timeout) {
    return {
      templateUrl: "" + baseTemplatePath + "splitButton.html",
      restrict: 'EA',
      transclude: true,
      replace: true,
      scope: {
        defaultAction: '=',
        menuAnchor: '@',
        label: '@',
        cuiType: '@',
        ngDisabled: '=',
        icon: '@'
      },
      compile: function(element, attrs, transclude) {
        var button;
        if (attrs.size == null) {
          attrs.size = 'medium';
        }
        if (attrs.menuAnchor == null) {
          attrs.menuAnchor = 'left';
        }
        button = element.find('cui-button');
        button.attr('size', attrs.size);
        if (attrs.ngDisabled != null) {
          button.attr('ng-disabled', 'ngDisabled');
        }
        return function(scope, element) {
          return transclude(scope, function(content) {
            var contents, tipScope, ul;
            ul = null;
            angular.forEach(content, function(c) {
              if (c.nodeName === 'UL') {
                return ul = c;
              }
            });
            if (!ul) {
              console.log("CUI-SPLIT-BUTTON requires nested UL element.");
              return;
            }
            tipScope = scope.$parent.$new();
            contents = $compile(ul.outerHTML)(tipScope)[0];
            return $timeout(function() {
              var _tip;
              _tip = cuiTip({
                target: element[0].children[0].children[1],
                content: contents,
                classes: 'drop-theme-cui-menu',
                position: "bottom " + attrs.menuAnchor,
                type: 'menu'
              });
              return tipScope._tip = _tip;
            });
          });
        };
      }
    };
  });


  /**
    @ngdoc directive
    @name table
  
    @description Tables display much of the content in enterprise applications. For more information, please see the documentation of Smart-Table, the angular module that we wrap.
    
    @module cui.controls
    @src controls/table/table.coffee
    @controlType tabular
  
    @restrict E
  
    @param {object} rows - The actual data placed on the table. See the [Smart-Table documentation](http://lorenzofox3.github.io/smart-table-website/) for more information.
    @param {object=} columns - The configuration/settings for each of the columns. See the [Smart-Table documentation](http://lorenzofox3.github.io/smart-table-website/) for more information.
    @param {object=} config - The global settings for the table. See the [Smart-Table documentation](http://lorenzofox3.github.io/smart-table-website/) for more information.
    
    @example
    <h3>Static table</h3>
  <example name='table'>
    <file name='index.html'>
      <cui-table rows="rowCollection" columns="columnCollection"></cui-table>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiDataSourceService) {
          // Data from http://nssdc.gsfc.nasa.gov/planetary/factsheet/marsfact.html
          $scope.columnCollection = [{
            label: '',
            cellTemplateUrl: 'custom.html'
          }, {
            label: 'Mars',
            map: 'mars'
          }, {
            label: 'Earth',
            map: 'earth'
          }, {
            label: 'Ratio (Mars/Earth)',
            map: 'ratio'
          }]
          cuiDataSourceService('data.json').all().then(function(data) {
            $scope.rowCollection = data
          });
        })
    </file>
    <file name='custom.html'>
      <strong>{{dataRow.prop.name}}</strong> <span ng-if='dataRow.prop.unit'>({{dataRow.prop.unit}})</span>
    </file>
    <file name='data.json'>
      [{"prop":{"name":"Mass","unit":"10^24 kg"},"mars":0.64174,"earth":5.9726,"ratio":0.107},{"prop":{"name":"Volume","unit":"10^10 km^3"},"mars":16.318,"earth":108.321,"ratio":0.151},{"prop":{"name":"Equatorial Radius","unit":"km"},"mars":3396.2,"earth":6378.1,"ratio":0.532},{"prop":{"name":"Polar radius","unit":"km"},"mars":3376.2,"earth":6356.8,"ratio":0.531},{"prop":{"name":"Volumetric mean radius","unit":"km"},"mars":3389.5,"earth":6371,"ratio":0.532},{"prop":{"name":"Core radius","unit":"km"},"mars":1700,"earth":3485,"ratio":0.488},{"prop":{"name":"Ellipticity","unit":"Flattening"},"mars":0.00589,"earth":0.00335,"ratio":1.76},{"prop":{"name":"Mean density","unit":"km/m^3"},"mars":3933,"earth":5514,"ratio":0.713},{"prop":{"name":"Surface gravity","unit":"m/s^2"},"mars":3.71,"earth":9.8,"ratio":0.379},{"prop":{"name":"Surface acceleration","unit":"m/s^2"},"mars":3.69,"earth":9.78,"ratio":0.377},{"prop":{"name":"Escape velocity","unit":"km/s"},"mars":5.03,"earth":11.19,"ratio":0.45},{"prop":{"name":"GM","unit":"x 10^6 km^3/s^2"},"mars":0.04283,"earth":0.3986,"ratio":0.107},{"prop":{"name":"Bond albedo","unit":""},"mars":0.25,"earth":0.306,"ratio":0.817},{"prop":{"name":"Visual geometric albedo","unit":""},"mars":0.17,"earth":0.367,"ratio":0.463},{"prop":{"name":"Visual magnitude","unit":"V(1,0)"},"mars":-1.52,"earth":-3.86,"ratio":null},{"prop":{"name":"Solar irradiance","unit":"W/m^2"},"mars":589.2,"earth":1267.6,"ratio":0.431},{"prop":{"name":"Black-body temperature","unit":"K"},"mars":210.1,"earth":254.3,"ratio":0.826},{"prop":{"name":"Topographic range","unit":"km"},"mars":30,"earth":20,"ratio":1.5},{"prop":{"name":"Moment of inertia","unit":"I/MR^2"},"mars":0.366,"earth":0.3308,"ratio":1.106},{"prop":{"name":"J_2","unit":"x 10^-6"},"mars":1960.45,"earth":1082.63,"ratio":1.811},{"prop":{"name":"Number of natural satellites","unit":""},"mars":2,"earth":1,"ratio":null},{"prop":{"name":"Planetary ring system","unit":""},"mars":false,"earth":false,"ratio":null}]
    </file>
  </example>
  
    @example
    <h3>Generated table</h3>
  <example name='staticTable'>
    <file name='index.html'>
      <cui-table rows="rowCollection"
                 columns="columnCollection"
                 config="globalConfig">
      </cui-table>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          var servers = ['PowerEdge R320', 'PowerEdge R210 II', 'PowerEdge R420', 'PowerEdge R910', 'PowerEdge R720xd'];
  
          var ram = [2, 4, 8, 16, 32, 64, 128];
  
          function genIP() {
            return '192.168.' +
              (Math.floor(Math.random() * 3) + 100) + '.' +
              Math.floor(Math.random() * 256)
          }
  
          function genServiceTag() {
            ret = '';
            for(i = 0; i < 8; i++)
              ret += String.fromCharCode(65 + Math.floor(Math.random() * 8));
            return ret;
          }
  
          function createRandomItem() {
            var
              isOnline = Math.random() > .1,
              icon =  isOnline ? 'ok-sign' : 'warning-sign',
              iconColor = isOnline ? 'green' : 'red',
              ip = genIP(),
              serviceTag = genServiceTag();
  
            return{
              ip: ip,
              icon: icon,
              iconColor: iconColor,
              serviceTag: serviceTag,
              model: servers[ Math.floor(Math.random() * servers.length) ],
              memory: ram[ Math.floor(Math.random() * ram.length) ] + ' GB'
            };
          }
  
          $scope.rowCollection = [];
          for (var j = 0; j < 200; j++) {
            $scope.rowCollection.push(createRandomItem());
          }
  
          $scope.columnCollection = [
            {label: 'Status', cellTemplateUrl: 'cellIcon.html'},
            {label: 'IP Address', map: 'ip'},
            {label: 'Service Tag', map: 'serviceTag'},
            {label: 'Model', map: 'model'},
            {label: 'Memory', map: 'memory'}
          ];
  
          $scope.globalConfig = {
            selectionMode: 'multiple',
            isPaginationEnabled: true,
            isGlobalSearchActivated: true,
            itemsByPage: 12,
            maxSize: 8
          };
        })
    </file>
    <file name='cellIcon.html'>
      <cui-icon icon='{{dataRow.icon}}' color='{{dataRow.iconColor}}'></cui-icon>
    </file>
  </example>
   */

  module = angular.module('cui.controls.table', ['cui.base']);

  module.directive('cuiTable', function(baseTemplatePath, $timeout) {
    return {
      templateUrl: "" + baseTemplatePath + "table.html",
      restrict: 'EA',
      scope: {
        columnCollection: '=columns',
        dataCollection: '=rows',
        globalConfig: '=config'
      },
      link: function(scope, element, attrs) {
        return $timeout(function() {
          var cols, searchTd, _ref;
          if ((_ref = scope.globalConfig) != null ? _ref.isGlobalSearchActivated : void 0) {
            searchTd = angular.element(element[0].querySelector('.smart-table-global-search'));
            cols = searchTd.attr('column-span');
            return searchTd.attr('colspan', cols);
          }
        }, 0);
      }
    };
  });


  /**
    @ngdoc directive
    @name tabset
  
    @description A Tabset presents tabbed data
    
    @module cui.controls
    @src controls/tabset/tabset.coffee
    @controlType navigation
  
    @restrict E
    @transclude A set of Tabs
  
    @example
    <h3>A basic tabset</h3>
  <example name='tabset'>
    <file name='index.html'>
      <!-- Yo dawg, I heard you liked tabsets, so I made a tabset to view the code of your tabsets. -->
      <cui-button ng-click='tabs[1].active = true'>Make second tab active</cui-button>
  
      <cui-tabset>
        <cui-tab label='{{tabs[0].label}}'
                icon='{{tabs[0].icon}}'
                active='tabs[0].active'>
          <h1>Manage Account Settings</h1>
          <cui-text-box ng-model='data'></cui-text-box>
          User entered: {{data}}
        </cui-tab>
        <cui-tab label='{{tabs[1].label}}'
                icon='{{tabs[1].icon}}'
                badge='tabs[1].badge'
                active='tabs[1].active'>
          <cui-text-box ng-model='data2'></cui-text-box>
          User entered: {{data2}}
        </cui-tab>
        <cui-tab label='{{tabs[2].label}}'
                 icon='{{tabs[2].icon}}'
                 badge='tabs[2].badge'
                 active='tabs[2].active'>
          Error Console
        </cui-tab>
      </cui-tabset>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.tabs = [
            {
              label: 'Accounts',
              icon: 'user',
              content: 'Manage User Settings'
            },
            {
              label: 'Optional Settings',
              icon: 'user',
              content: 'Non User Settings'
            },
            {
              label: 'Error Console',
              icon: 'bug',
              badge: {
                type: 'danger',
                label: '3'
              },
              content: 'Error content'
            }
          ]
  
          $scope.click = function() {
            alert('see!')
          }
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.tabset', ['cui.base']);

  module.directive('cuiTabset', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "tabset.html",
      restrict: 'EA',
      transclude: true,
      scope: {},
      controller: function($scope) {
        var tabs;
        tabs = $scope.tabs = [];
        this.addTab = function(tab) {
          if (tabs.length === 0 || tab.active === true) {
            this.select(tab);
          }
          return tabs.push(tab);
        };
        this.select = function(tab) {
          angular.forEach(tabs, function(t) {
            return t.active = false;
          });
          return tab.active = true;
        };
        $scope.addTab = this.addTab;
        $scope.select = this.select;
        return this;
      }
    };
  });


  /**
    @ngdoc directive
    @name tab
  
    @description A single tab (with content). Can only be used inside of a Tabset.
    
    @module cui.controls
    @src controls/tabset/tabset.coffee
    @controlType actionable
    
    @restrict E
    @transclude The tab content area.
  
    @param {string} label The label for the tab
    @param {boolean=} active Whether this tab is active. It is watched: When it becomes `true`, this tab will turn selected (and other tabs' `active` properties will become `false`).
    @param {expression=} tabShown Called when the tab is clicked, or shown
    @param {string=} icon An optional icon to prefix the tab's label with
    @param {object=} badge The badge is appended to the label of the tab. An object with the following form:
  
  ```javascript
  {
    type: 'danger', // Any cuiType -- 'danger', 'primary', 'warning', etc.
    label: '3' // Contents of the badge
  }
  ```
   */

  module.directive('cuiTab', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "tab.html",
      restrict: 'EA',
      require: '^cuiTabset',
      replace: true,
      transclude: true,
      scope: {
        label: '@',
        active: '=?',
        tabShown: '&',
        icon: '@',
        badge: '='
      },
      link: function(scope, element, attrs, cuiTabsetController) {
        cuiTabsetController.addTab(scope);
        scope.$watch('active', function(newActive, oldActive) {
          if (oldActive !== newActive && newActive) {
            return cuiTabsetController.select(scope);
          }
        });
        return scope.showTab = function() {
          cuiTabsetController.select(scope);
          return scope.tabShown();
        };
      }
    };
  });


  /**
    @ngdoc directive
    @name textarea
  
    @description The Textarea is a form control that will allow for long value inputs from a user.
  
  If the Textarea's `ng-model` contains a value, it will be overwritten by the transcluded content and the value of ng-model will be set to the value of the transcluded content.
    
    @module cui.controls
    @src controls/textarea/textarea.coffee
    @controlType input
    @restrict E
  
    @param {string=} placeholder The HTML5 placeholder attribute for the input.
  
    @param {string} ngModel The name of a property on the scope, which is bound to the value of the Textarea
  
    @param {string=} resize Controls the manner in which the user may resize the Textarea. Options are: none|both|vertical|horizontal
    The default in Firefox, Chrome and Safari is 'both'. Not respected in IE11 and older IE browsers.
  
    @param {boolean=} showCharactersRemaining Displays a remaining characters counter under the
    Textarea that will count down the characters remaining before the maxlength is reached. Set to true by default
    if a maxlength is set.
  
    @param {number=} maxlength Sets the maximum allowed characters that can be entered into a Textarea control
  
    @example
    <h3>Basic textarea</h3>
    <cui-alert dismissable=false>Except for very simple cases, form elements should be used inside of a form.</cui-alert>
  <example name='textarea'>
    <file name='index.html'>
      <cui-textarea name='default'
                    placeholder='Place Text Here'
                    ng-model='value'
                    resize='horizontal'>
      </cui-textarea>
  
      The textbox value: {{value}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Basic textarea in a form</h3>
  <example name='formTextarea'>
    <file name='index.html'>
      <form name='textareaExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <label for='default'>Default</label>
          <cui-textarea name='default'
                        id='default'
                        placeholder='Place Text Here'
                        ng-model='textareaExample.default'
                        resize='horizontal'>
          </cui-textarea>
        </div>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Basic textarea in a form with transclusion</h3>
  <example name='formTextarea'>
    <file name='index.html'>
      <form name='textareaExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <label for='transcluded'>Transcluded Text as value</label>
          <cui-textarea name='transcluded'
                        id='transcluded'
                        ng-model='textareaExample.transcludedText'
                        ng-maxlength='100'>
            Hello World! I have been transcluded.
          </cui-textarea>
        </div>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>No character remaining counter, but a ten character limit</h3>
  <example name='formTextarea'>
    <file name='index.html'>
      <form name='textareaExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <label for='characterLimit'>No character remaining counter, but a 10 char limit</label>
          <cui-textarea name='characterLimit'
                        id='characterLimit'
                        ng-model='textareaExample.characterLimit'
                        ng-maxlength='10'
                        show-characters-remaining='false'>
          </cui-textarea>
        </div>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Disabled textarea</h3>
  <example name='formTextarea'>
    <file name='index.html'>
      <form name='textareaExampleForm' class='cui-form-stacked'>
        <div class='cui-form-group'>
          <label for='disabled'>Disabled</label>
          <cui-textarea name='disabled'
                        id='disabled'
                        ng-model='textareaExample.disabled'
                        ng-disabled='true'
                        ng-maxlength='10'
                        show-characters-remaining='false'>
            Disabled
          </cui-textarea>
        </div>
      </form>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
   */

  module = angular.module('cui.controls.textarea', ['cui.base']);

  module.directive('cuiTextarea', function(baseTemplatePath, $compile, $log) {
    return {
      templateUrl: "" + baseTemplatePath + "textarea.html",
      restrict: 'E',
      transclude: true,
      replace: true,
      require: '?ngModel',
      scope: {
        ngModel: '='
      },
      controller: function($scope, $element, $attrs) {
        var genCharactersRemaining, maxlength, updateCharactersRemaining, value;
        if ($attrs.ngMaxlength != null) {
          maxlength = $attrs.ngMaxlength;
          value = function() {
            return $scope.ngModel;
          };
          genCharactersRemaining = function() {
            var current, _ref, _ref1;
            current = ((_ref = value()) != null ? _ref.trim().length : void 0) + ((_ref1 = value()) != null ? _ref1.split('\n').length : void 0) - 1;
            if (isNaN(current)) {
              current = 0;
            }
            return "" + (maxlength - current) + " characters remaining";
          };
          updateCharactersRemaining = function() {
            return $scope.charactersRemaining = genCharactersRemaining();
          };
          updateCharactersRemaining();
          return $scope.$watch('ngModel', function() {
            return updateCharactersRemaining();
          });
        }
      },
      compile: function(element, attrs, transcludeFn) {
        element.wrap('<div class="cui-textarea-wrapper"></div>');
        if (attrs.ngMaxlength != null) {
          if (attrs.showCharactersRemaining == null) {
            attrs.showCharactersRemaining = true;
          }
        }
        if (attrs.resize != null) {
          element.css('resize', attrs.resize);
        }
        return function(scope, element, attrs, ngModelCtrl) {
          var counter;
          if (ngModelCtrl == null) {
            $log.cuiError({
              cuiErrorType: 'ngmodel',
              cuiErrorCtrl: 'cui-textarea',
              cuiErrorElIdentity: attrs.name || attrs.id || 'UNKNOWN'
            });
            return;
          }
          if ((attrs.ngMaxlength != null) && attrs.showCharactersRemaining === true) {
            counter = angular.element($compile('<div class="characters-remaining">{{charactersRemaining}}</div>')(scope));
            element.after(counter);
          }
          return transcludeFn(scope.$new(), function(clone, innerScope) {
            var ta, transcludedContent;
            transcludedContent = clone.text().trim();
            if (transcludedContent && attrs.ngModel) {
              return scope.ngModel = transcludedContent;
            } else {
              ta = element.find('textarea');
              return ta.val(transcludedContent);
            }
          });
        };
      }
    };
  });


  /**
    @ngdoc directive
    @name textbox
  
    @description The Textbox is the basic form input element.
    
    @module cui.controls
    @src controls/textbox/textbox.coffee
    @controlType input
  
    @restrict E
  
    @param {string} ngModel The name of a property on the scope, which is bound to the value of the input.
                                This is a required attribute.
  
    @param {string=} type The HTML type attribute for the input. If not defined, it will default to a type of 'text'.
  
    @param {string} name The HTML name attribute for the input.
  
    @param {string=} placeholder The HTML5 placeholder attribute for the input
  
    @param {regex=} ngPattern Regex pattern to test the input value against.
  
  ```javascript
  $scope.validators = {
    zip: /^\d\d\d\d\d$/
  };
  ```
    
    @example
    <h3>Basic text box</h3>
    <p>
      Generates a basic text input. You can use common HTML and HTML5 attributes, such as placeholder and types(number, email, etc). See <a href='http://docs.angularjs.org/api/ng/directive/input' title='AngularJS input directive'>AngularJS input directives</a> for more information.
    </p>
  <example name='textbox'>
    <file name='index.html'>
      <cui-textbox ng-model='user.username'
                    placeholder='Username'>
      </cui-textbox>
      <cui-textbox ng-model='user.password'
                    placeholder='Password'
                    type='password'>
      </cui-textbox>
  
      <p>
        Output: <br />
        <code>Username: {{user.username}}</code> <br />
        <code>Password: {{user.password}}</code>
      </p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Disabled text box</h3>
  
  <example name='textbox'>
    <file name='index.html'>
      <cui-textbox ng-disabled=true
                    placeholder='I am disabled'
                    ng-model='value'>
      </cui-textbox>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Validation in text box</h3>
  
  <example name='validationTextbox'>
    <file name='index.html'>
      <form name='myForm'>
        <cui-textbox type='text'
                      name='chars'
                      ng-model='value'
                      ng-pattern='fiveCharacters'
                      placeholder='Enter any 5 a-z chars'>
        </cui-textbox>
      </form>
  
      <p>
        Input validation status:<br/>
        <code>value = {{value}}</code><br/>
        <code>myForm.chars.$valid = {{myForm.chars.$valid}}</code><br/>
        <code>myForm.chars.$error = {{myForm.chars.$error}}</code><br/>
      </p>
  
      <cui-textbox type='text'
                    name='chars'
                    ng-model='value'
                    ng-pattern='fiveCharacters'
                    placeholder='Enter any 5 chars'
                    required>
      </cui-textbox>
      <p>
        Input validation status:<br/>
        <code>value = {{value}}</code><br/>
        <code>myForm.chars.$valid = {{myForm.chars.$valid}}</code><br/>
        <code>myForm.chars.$error = {{myForm.chars.$error}}</code><br/>
      </p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.fiveCharacters = /[a-z]{5}/;
        });
    </file>
  </example>
  
    @example
    <h3>Numeric text box</h3>
    <p>With the advent of HTML5 and browser vendors quickly integrating new specifications for elements, the most practical way in which to create a numeric text box is to implement a cui-textbox control with its type attribute set to number.</p>
    <p>We have taken some steps to create a consistent user interface, as well as attempt to mitigate browser differences as they relate to how they invalidate various user input.</p>
    <cui-alert cui-type='warning' dismissable=false>
      There are known issues as it relates to how numeric inputs are validated in different browsers.
      (See <a href='https://github.com/angular/angular.js/issues/6928'>forms validation refactoring #6928</a>.)
    </cui-alert>
  <example name='textbox'>
    <file name='index.html'>
      <cui-textbox type='number'
                    ng-model='value'
                    placeholder='Enter a number'
                    ng-disabled='disabled'>
      </cui-textbox>
      <cui-button ng-click='disabled = !disabled'>Toggle disabled</cui-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
   */

  module = angular.module('cui.controls.textbox', ['cui.base']);

  (function() {
    var func;
    func = function(baseTemplatePath, $compile, $log) {
      return {
        templateUrl: "" + baseTemplatePath + "textbox.html",
        restrict: 'E',
        transclude: true,
        require: '?ngModel',
        replace: true,
        link: function(scope, iElement, iAttrs, ngModelCtrl) {
          var isOverMax, isUnderMin, spinnerControlTpl, spinnerScope, wrapperEl;
          if (ngModelCtrl == null) {
            $log.cuiError({
              cuiErrorType: 'ngmodel',
              cuiErrorCtrl: 'cui-textbox',
              cuiErrorElIdentity: iAttrs.name || iAttrs.id || 'UNKNOWN'
            });
            return;
          }
          if (iAttrs.type === 'number') {
            spinnerControlTpl = '<span class="cui-spinner-wrap">\n    <span class=\'cui-icon-wrapper\' ng-click="step(1)" >\n      <cui-icon icon="caret-up"></cui-icon>\n    </span>\n    <span class=\'cui-icon-wrapper\' ng-click="step(-1)">\n      <cui-icon icon="caret-down"></cui-icon>\n    </span>\n</span>';
            wrapperEl = angular.element('<div class="cui-numeric-textbox"></div>');
            iElement.wrap(wrapperEl);
            spinnerScope = scope.$new();
            spinnerScope.step = function(direction) {
              var delta, inBounds, interval, value;
              if (iElement.attr('disabled') != null) {
                return;
              }
              interval = Math.abs(parseFloat(iAttrs.step)) || 1;
              delta = interval * direction;
              value = (+ngModelCtrl.$viewValue || 0) + delta;
              if (!(isOverMax(value) || isUnderMin(value))) {
                inBounds = true;
              }
              if (inBounds) {
                ngModelCtrl.$setViewValue(+value);
                return ngModelCtrl.$render();
              } else {

              }
            };
            iElement.after($compile(spinnerControlTpl)(spinnerScope));
            isUnderMin = function(value) {
              return angular.isDefined(iAttrs.min) && +value < iAttrs.min;
            };
            isOverMax = function(value) {
              return angular.isDefined(iAttrs.max) && +value > iAttrs.max;
            };
            return ngModelCtrl.$parsers.push(function(value) {
              var currentPrecision, precision, _ref, _ref1, _ref2;
              if (value === void 0 || value === null) {
                ngModelCtrl.$setValidity('number', false);
                return void 0;
              } else {
                precision = ((_ref = iAttrs.step) != null ? (_ref1 = _ref.toString().split('.')[1]) != null ? _ref1.length : void 0 : void 0) || 0;
                currentPrecision = (value != null ? (_ref2 = value.toString().split('.')[1]) != null ? _ref2.length : void 0 : void 0) || 0;
                if (currentPrecision > precision) {
                  value = +value.toFixed(precision);
                  ngModelCtrl.$setViewValue(value);
                }
                return value;
              }
            });
          } else if (iAttrs.type == null) {
            return iElement.attr('type', 'text');
          }
        }
      };
    };
    module.directive('cuiTextBox', func);
    return module.directive('cuiTextbox', func);
  })();


  /**
    @ngdoc directive
    @name time
    @module cui.controls
    @description The Time directive allows adjusting the hour, minute, and (optionally) second of a time.
  
  ***For most use cases, please see the Time Picker, which implements this inside of a tip.***
  
    @controlType input
    @restrict E
    @param {date} ngModel This accepts a javascript `date` object, or `null`/`undefined`, in which case the time will be rounded down to the current hour.
    @param {boolean=} showSeconds If `'true'`, a toggle to adhust the time by each second will be added. Default false.
    @param {boolean=} showMeridian If `'false'`, time will be displayed in 24-hour format. Default true.
    @param {number=} hourIncrement The number of hours to skip when clicking the up/down arrows. Default is 1.
    @param {number=} minuteIncrement The number of minutes to skip when clicking the up/down arrows. Default is 15.
    @param {number=} secondIncrement The number of seconds to skip when clicking the up/down arrows. Default is 15.
  
    @example
    <h3>Time picker</h3>
  <example>
    <file name='index.html'>
      <cui-time ng-model='time'></cui-time> {{time | date:'shortTime'}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.time = new Date(2013, 2, 1, 1, 10);
        });
    </file>
  </example>
  
    @example
    <h3>Time picker with seconds</h3>
  <example>
    <file name='index.html'>
      <cui-time ng-model='time' show-seconds='true'></cui-time> {{time | date:'mediumTime'}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Time picker with seconds and fine-grain control</h3>
  <example>
    <file name='index.html'>
      <cui-time ng-model='time'
                show-seconds='true'
                minute-increment='1'
                second-increment='1'></cui-time>
      {{time | date:'mediumTime'}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
  
    @example
    <h3>Time picker without AM/PM</h3>
  <example>
    <file name='index.html'>
      <cui-time ng-model='time' show-meridian='false'></cui-time> {{time | date:'H:mm'}}
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
  </example>
   */

  module = angular.module('cui.controls.time', ['cui.base']);

  module.directive('cuiTime', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "time.html",
      restrict: 'E',
      scope: {
        model: '=ngModel'
      },
      require: ['cuiTime', '^ngModel'],
      controllerAs: 'timeCtrl',
      controller: function($scope) {
        this.options = {
          hourIncrement: 1,
          minuteIncrement: 15,
          secondIncrement: 15
        };
        this.changeHour = function(increment) {
          if (!angular.isDate($scope.model)) {
            $scope.model = $scope.lastValidDate;
          }
          $scope.model.setHours($scope.model.getHours() + increment);
          return $scope.model = new Date($scope.model);
        };
        this.changeMinute = function(increment) {
          if (!angular.isDate($scope.model)) {
            $scope.model = $scope.lastValidDate;
          }
          $scope.model.setMinutes($scope.model.getMinutes() + increment);
          return $scope.model = new Date($scope.model);
        };
        this.changeSecond = function(increment) {
          if (!angular.isDate($scope.model)) {
            $scope.model = $scope.lastValidDate;
          }
          $scope.model.setSeconds($scope.model.getSeconds() + increment);
          return $scope.model = new Date($scope.model);
        };
        this.toggleMeridian = function() {
          if (!angular.isDate($scope.model)) {
            $scope.model = $scope.lastValidDate;
          }
          if ($scope.model.getHours() >= 12) {
            $scope.model.setHours($scope.model.getHours() - 12);
            return $scope.model = new Date($scope.model);
          } else {
            $scope.model.setHours($scope.model.getHours() + 12);
            return $scope.model = new Date($scope.model);
          }
        };
        return this;
      },
      link: function(scope, element, attrs, _arg) {
        var modelCtrl, timeCtrl;
        timeCtrl = _arg[0], modelCtrl = _arg[1];
        if (attrs.hourIncrement != null) {
          timeCtrl.options.hourIncrement = parseInt(attrs.hourIncrement);
        }
        if (attrs.minuteIncrement != null) {
          timeCtrl.options.minuteIncrement = parseInt(attrs.minuteIncrement);
        }
        if (attrs.secondIncrement != null) {
          timeCtrl.options.secondIncrement = parseInt(attrs.secondIncrement);
        }
        scope.showMeridian = attrs.showMeridian !== 'false';
        scope.showSeconds = attrs.showSeconds === 'true';
        scope.lastValidDate = new Date();
        scope.lastValidDate.setMinutes(0, 0, 0);
        return scope.$watch('model', function(m) {
          if (angular.isDate(m)) {
            scope.lastValidDate = m;
          }
          return scope.meridian = (m || scope.lastValidDate).getHours() >= 12 ? 'PM' : 'AM';
        });
      }
    };
  });


  /**
    @ngdoc directive
    @name tip
    @controlType actionable
   
    @module cui.controls
    @description Tips provide additional information about an item in the UI. For instance, a tip could be placed next to a Textbox to show the pattern requirements of the value.
  
  There are two different types of Tips, as illustrated by the examples below. The first is the `tooltip`, the second is a `hint`. Use the `tooltip` type for longer content, or HTML content which needs to be formated (multiple lines, headings, etc), and use the `hint` type for short content.
  
  Tips will do their best to stay within the viewport. This is shown in the first example, the "left top" and "right top" both have extremely long content, and as such, the Tip flips to the opposite side.
   
    @src controls/tip/tip.coffee
    @restrict A
  
    @param {Object} cuiTip The base directive. Can be passed an object of the following format:
  ```js
  {
    content: String,
    position: String,
    openOn: String,
    templateUrl: String,
    type: String
  }
  ```
  
  __Attributes listed will override properties in this object.__
  
  For instance, if you set `content: 'Hello there'`, but also set `cui-tip-content='No, goodbye'`, the content of the tip will be `'No, goodbye'`.
  
    @param {String=} cuiTipContent An HTML content string.
    @param {String=} cuiTipPosition The position the Tip should anchor at. One of:
  ```js
  'top left'
  'left top'
  'left middle'
  'left bottom'
  'bottom left'
  'bottom center'
  'bottom right'
  'right bottom'
  'right middle'
  'right top'
  'top right'
  'top center' // Default
  ```
  
  The syntax for position follows the pattern of `"vertical-position horizontal-position"`, where the `vertical-position` determines what side of the target you want the Tip to show on, and the `horizontal-position` determines which part of the Tip should align to the target.
  
  This syntax is taken from [Tether.io](http://tether.io).
    @param {String=} cuiTipOpenOn Specifies what event on the target element opens the Tip. One of:
  ```js
  'click'
  'hover' // Default
  'always'
  ```
  
    @param {String=} cuiTipTemplateUrl The URL of a template which should be loaded as the content. Use of this attribute will override the value of `cui-tip-content`.
  
    @param {String=} cuiTipType The type of Tip to be displayed. One of:
  ```js
  'tooltip' // Default
  'hint'
  ```
   
    @example
    <h3>Tooltip label</h3>
  <example name='Tooltip'>
    <file name='index.html'>
      <div class='centered'>
        <cui-button cui-tip cui-tip-position='top left'>Top Left</cui-button>
        <cui-button cui-tip>Top Center [default]</cui-button>
        <cui-button cui-tip cui-tip-position='top right'>Top Right</cui-button>
      </div>
      
      <div class='centered'>
          <cui-button cui-tip cui-tip-position='left top' cui-tip-content='<b>This content is long, and causes the Tip to flip sides.</b><br/>Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.'>Left Top</cui-button>
          <span>&nbsp;</span>
          <cui-button cui-tip cui-tip-position='right top' cui-tip-content='<b>This content is long, and causes the Tip to flip sides.</b><br/>Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>'>Right Top</cui-button>
      </div>
      
      <div class='centered'>
          <cui-button cui-tip cui-tip-position='left middle'>Left Middle</cui-button>
          <cui-button cui-type='transparent'>Hover over buttons to see tooltips</cui-button>
          <cui-button cui-tip cui-tip-position='right middle'>Right Middle</cui-button>
      </div>
  
      <div class='centered'>
          <cui-button cui-tip cui-tip-position='left bottom' cui-tip-content='Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>'>Left Bottom</cui-button>
          <cui-button cui-type='success' cui-tip cui-tip-template-url='tooltipTmpl.html'>Tip Templates</cui-button>
          <cui-button cui-tip cui-tip-position='right bottom' cui-tip-content='Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>Lorem ipsum Ullamco est nisi fugiat qui sint ut aliqua cupidatat ut non veniam.<br/>'>Right Bottom</cui-button>
      </div>
      
      <div class='centered'>
        <cui-button cui-tip cui-tip-position='bottom left'>Bottom Left</cui-button>
        <cui-button cui-tip cui-tip-position='bottom center'>Bottom Center</cui-button>
        <cui-button cui-tip cui-tip-position='bottom right'>Bottom Right</cui-button>
      </div>
    </file>
    <file name='tooltipTmpl.html'>
      <h1><cui-icon icon='check-sign' color='green' cui-tip cui-tip-content='Yo Dawg'></cui-icon> Success!</h1>
      <p>You can use a template inside of the content of a tooltip!</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          // Nothing required here to have a label.
        })
    </file>
    <file name='style.css'>
    body {
      min-height: 400px;
    }
  
    .centered {
      width: 600px;
      margin: 100px auto;
      text-align: center;
    }
  
    .centered > :first-child {
      float: left;
    }
  
    .centered > :last-child {
      float: right;
    }
    </file>
  </example>
  
   @example
    <h3>SVG</h3>
  <example name='Tooltip'>
    <file name='index.html'>
      <svg height='300' width='400'>
        <g>
          <circle cx='100' cy='100' r='30' cui-tip='tip'></circle>
        </g>
      </svg>
   </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          // Nothing required here to have a label.
          $scope.tip = {
            position: 'top left',
            content: 'Setting content in base config object'
          }
        })
    </file>
  </example>
  
    @example
    <h3>Updating content</h3>
  <example name='Tooltip'>
    <file name='index.html'>
      <cui-icon size='48px' icon='time' cui-tip cui-tip-type='hint' cui-tip-content='{{content}}' cui-tip-open-on='always' cui-tip-position='right middle'></cui-icon>
    </file>
    <file name='style.css'>
      body { height: 400px; }
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $timeout, $filter) {
          function setTime() {
            var time = $filter('date')(new Date(), 'mediumTime')
            $scope.content = "Current Time: " + time;
            $timeout(setTime, 1000)
          }
  
          setTime()
        })
    </file>
  </example>
   
    @example
    <h3>"Hint"</h3>
  <example name='Tooltip'>
    <file name='index.html'>
      <h1>Sign up now!</h1>
      <div>
        <cui-textbox placeholder='Email' ng-model='user.username'></cui-textbox> <cui-icon icon='question-sign' cui-tip cui-tip-content='Try to use a non-Yahoo email account.' cui-tip-type='hint' cui-tip-position='right middle'></cui-icon>
      </div>
      <div>
        <cui-textbox placeholder='Password' ng-model='user.password' type='password'></cui-textbox> <cui-icon icon='question-sign' cui-tip cui-tip-content='8 or more alphanumeric characters.' cui-tip-type='hint' cui-tip-position='right middle'></cui-icon>
      </div>
    </file>
    <file name='style.css'>
      body { height: 400px; }
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
        })
    </file>
  </example>
   */

  module = angular.module('cui.controls.tip', ['cui.base']);

  module.factory('cuiTip', function() {
    return function(config) {
      var _tip;
      _tip = new Drop(config);
      _tip.on('open', function(e) {
        var li;
        if (config.type === 'menu') {
          li = angular.element(_tip.drop).find('li');
          angular.forEach(li, function(l) {
            var el;
            el = angular.element(l);
            if (el.hasClass('kb-active')) {
              return l.focus();
            }
          });
          return li.on('mouseenter', function() {
            angular.forEach(li, function(l) {
              return l.blur();
            });
            this.focus();
            return this.setAttribute('tabindex', 0);
          });
        }
      });
      _tip.on('cui:tip:open', function() {
        return _tip.open();
      });
      _tip.on('cui:tip:close', function() {
        return _tip.close();
      });
      return _tip;
    };
  });

  module.factory('cuiTipCalculateCenterOffset', function() {
    return function(position, el, center) {
      var elHeight, elWidth, offset;
      if (center == null) {
        center = true;
      }
      if (!center) {
        return '0 0';
      }
      el = el[0];
      elWidth = el.getBoundingClientRect != null ? el.getBoundingClientRect().width : el.offsetWidth;
      elHeight = el.getBoundingClientRect != null ? el.getBoundingClientRect().height : el.offsetHeight;
      position = position.split(' ');
      offset = {
        x: 0,
        y: 0
      };
      switch (position[1]) {
        case 'right':
          offset.x = (elWidth - elWidth / 2) - 21;
          break;
        case 'left':
          offset.x = -elWidth / 2 + 21;
          break;
        case 'top':
          offset.y = -elHeight / 2 + 21;
          break;
        case 'bottom':
          offset.y = (elHeight - elHeight / 2) - 21;
      }
      return "" + offset.y + "px " + offset.x + "px";
    };
  });

  module.directive('cuiTip', function(baseTemplatePath, cuiTip, $timeout, $compile, $templateCache, cuiTipCalculateCenterOffset, kbFocus) {
    return {
      restrict: 'A',
      link: function(scope, el, attrs) {
        var config, defaults, newScope, tipConfig, tipContent, tipTmpl, type;
        defaults = {
          position: 'top center',
          centerArrow: true,
          openOn: 'hover',
          content: 'No Content',
          type: 'tooltip'
        };
        tipConfig = scope[attrs.cuiTip];
        type = attrs.cuiTipType || (tipConfig != null ? tipConfig.type : void 0) || defaults.type;
        if (type === 'menu') {
          angular.extend(defaults, {
            type: 'menu',
            centerArrow: false,
            position: 'bottom right',
            openOn: 'click'
          });
        }
        config = {
          position: attrs.cuiTipPosition || (tipConfig != null ? tipConfig.position : void 0) || defaults.position,
          centerArrow: attrs.cuiTipCenterArrow || (tipConfig != null ? tipConfig.centerArrow : void 0) || defaults.centerArrow,
          openOn: attrs.cuiTipOpenOn || (tipConfig != null ? tipConfig.openOn : void 0) || defaults.openOn,
          content: attrs.cuiTipContent || (tipConfig != null ? tipConfig.content : void 0) || defaults.content,
          templateUrl: attrs.cuiTipTemplateUrl || (tipConfig != null ? tipConfig.templateUrl : void 0) || void 0,
          type: type
        };
        config.classes = "drop-theme-cui-" + config.type;
        if (config.templateUrl != null) {
          newScope = scope.$new();
          newScope.templateUrl = config.templateUrl;
          tipTmpl = $templateCache.get("" + baseTemplatePath + "tip.html");
          tipContent = $compile(tipTmpl)(newScope);
          config.content = tipContent[0];
        }
        return $timeout(function() {
          var _tip;
          _tip = cuiTip({
            target: el[0],
            content: config.content,
            classes: config.classes,
            position: config.position,
            openOn: config.openOn,
            tetherOptions: {
              offset: cuiTipCalculateCenterOffset(config.position, el, config.centerArrow),
              constraints: [
                {
                  to: 'scrollParent',
                  attachment: 'together',
                  pin: ['left', 'right']
                }
              ]
            }
          });

          /* istanbul ignore next */
          _tip.on('open', function(e) {
            return scope.$broadcast('cui:tip:open');
          });

          /* istanbul ignore next */
          _tip.on('close', function(e) {
            return scope.$broadcast('cui:tip:close');
          });
          if (newScope != null) {
            newScope._tip = _tip;
          }

          /* istanbul ignore next */
          return attrs.$observe('cuiTipContent', function(n, o) {
            if (n != null) {
              _tip.content.innerHTML = n;
              return _tip.position();
            }
          });
        });
      }
    };
  });


  /**
    @ngdoc directive
    @name tooltip
  
    @description The Tooltip is an easy way to have text or icons have a styled tooltip window.
    
    @module cui.controls
    @src controls/tooltip/tooltip.coffee
    @controlType presentational
    @deprecated 2.7.0
    @useInstead Please consider using <a href='../tip'>Tip</a> instead.
    
    @restrict E
    @transclude The content that will show up in the tooltip window
    
    @param {string=} position Describes where to put the tool tip, this is added as a class to the tooltip, default is bottom.  EX: top, bottom, left, right
    
    @param {string=} text Text for the tooltip to be bound to. (This __or__ `icon` is required.)
    
    @param {string=} icon Icon for the tooltip instead of text. (This __or__ `text` is required.)
    
    @param {string=} size Sets the size of the icon.
    
    @param {string=} cuiStyle Sets the style attribute for the span or cui icon so you can style these inline.
  
  If you put a style attribute on the cui-tooltip itself it would affect the tooltip as well.
  
    @param {object=} trigger Accepts an object of the methods you want to call.
  
  ```javascript
  {
    hover: true,
    click: false
  }
  ```
  
  This is also the default hover being true and click being false.
    
    @example
    <h3>Tooltip bound to text</h3>
  <example name='textTooltip'>
    <file name='index.html'>
      <p>
        <cui-tooltip position="bottom" text="Bottom" cui-style="font-weight:bold">
          <strongTooltip header</strong> This is the bottom placement.
        </cui-tooltip> next there is a
      </p>
      <p>
        <cui-tooltip position="right" text="Right" cui-style="font-weight:bold">
          <span style="color: #CE1126">
            <cui-icon icon="exclamation-sign"></cui-icon> This is the right placement.
          </span>
        </cui-tooltip>, after that it is a
      </p>
      <p>
        <cui-tooltip position="left" text="Left" cui-style="font-weight:bold">
          This is the left placement.
        </cui-tooltip> and finally
      </p>
      <p>
        <cui-tooltip position="top" text="Top" cui-style="font-weight:bold">
          <strong>Tooltip header</strong>
          <br>
          This is the top placement.
        </cui-tooltip> is the last placement.
      </p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {})
    </file>
    <file name='styles.css'>
      body {
        text-align: center;
      }
    </file>
  </example>
    
    @example
    <h3>Tooltip bound to icon</h3>
    <cui-alert cui-type='warning' dismissable=false>
      <span>Be careful with padding on your icons. Because padding is factored into the width and height returned, the tooltip may become off-centered.</span>
      <p>Also, keep in mind that text transforms do not change the height and width of an element, so be careful when positioning right and bottom tooltips on text that has text-shadow or box-shadow.</p>
    </cui-alert>
  <example name='iconTooltip'>
    <file name='index.html'>
      <cui-tooltip position="left" icon='chevron-sign-left' trigger="{hover: false, click: true}" size="20px">
        This is the left placement.
      </cui-tooltip>
  
      <cui-tooltip position="top" icon='chevron-sign-up' trigger="{hover: false, click: true}" size="20px">
        <strong>Tooltip header</strong>
        <p>This is the top placement.</p>
      </cui-tooltip>
  
      <cui-tooltip position="bottom" icon='chevron-sign-down' trigger="{hover: false, click: true}" size="20px">
        <strong>Tooltip header</strong>
        <p>This is the bottom placement.</p>
      </cui-tooltip>
  
      <cui-tooltip position="right" icon='chevron-sign-right' trigger="{hover: false, click: true}" size="20px">
        <span style="color: #CE1126">
          <cui-icon icon="exclamation-sign"></cui-icon>
          This is the right placement.
        </span>
      </cui-tooltip>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {})
    </file>
    <file name='styles.css'>
      body {
        text-align: center;
        height: 300px;
        margin-top: 145px;
      }
    </file>
  </example>
    
    @example
    <h3>Tooltip manual triggering</h3>
  <example name='iconTooltip'>
    <file name='index.html'>
      <cui-button ng-click='toggleButton()'>Toggle Me</cui-button>
  
      <cui-tooltip id="toggleOnly"
                   position="top"
                   text="I only toggle via the button."
                   cui-style="font-weight:bold"
                   trigger="{hover: false, click: false}">
        <span style="color: #CE1126">
          <cui-icon icon="exclamation-sign"></cui-icon>
          This is the right placement.
        </span>
      </cui-tooltip>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.toggleButton = function() {
            // This creates an angular element which has JQuery Lite functions such as toggleClass.  Many other ways to do this such as Jquery etc.
            triggerElement = angular.element(document.querySelector("#toggleOnly .cui-tooltip-box"));
            triggerElement.toggleClass("on");
            // Publicly Accessible Function will reposition all the tooltips.
            $scope.$broadcast('repositionBox');
          }
        })
    </file>
    <file name='styles.css'>
      body {
        height: 110px;
        margin-top: 60px;
      }
    </file>
  </example>
   */

  module = angular.module('cui.controls.tooltip', ['cui.base']);

  module.directive('cuiTooltip', function(baseTemplatePath, cuiPositionService) {
    return {
      templateUrl: "" + baseTemplatePath + "tooltip.html",
      restrict: 'EA',
      transclude: true,
      scope: {
        position: '@',
        text: '@',
        icon: '@',
        size: '@',
        cuiStyle: '@',
        trigger: '=?'
      },
      link: function(scope, element) {
        var boxElement;
        if (scope.position == null) {
          scope.position = 'bottom';
        }
        boxElement = angular.element(element[0].querySelectorAll('.cui-tooltip-box'));
        if (!scope.trigger) {
          scope.trigger = {
            hover: true,
            click: false
          };
        }
        scope.toggleTooltip = function(triggerMethod) {
          if (triggerMethod === 'click' && scope.trigger.click === true) {
            scope.showTooltip = !scope.showTooltip;
          } else if (triggerMethod === 'hover' && scope.trigger.hover === true) {
            scope.showTooltip = !scope.showTooltip;
          } else {
            return;
          }
          if (scope.showTooltip) {
            boxElement.addClass('on');
          } else {
            boxElement.removeClass('on');
          }
          return scope.$broadcast('repositionBox');
        };
        return scope.$on('repositionBox', function() {
          var anchorElement, anchorPosition, attachedPosition, obj;
          anchorElement = scope.text ? element[0].querySelectorAll('.cui-tooltip-text') : element[0].querySelectorAll('.cui-tooltip-icon');
          anchorPosition = cuiPositionService.getPositionValue(anchorElement);
          attachedPosition = cuiPositionService.getPositionValue(boxElement);
          obj = cuiPositionService.getRelativePositionValue(anchorPosition, attachedPosition, scope.position, 10);
          return scope.positionStyle = {
            top: obj.top,
            left: obj.left
          };
        });
      }
    };
  });


  /**
    @ngdoc directive
    @name tree
    @description The tree is used for navigation in filesystems and general nested items.
    @module cui.controls
  
    @src controls/tree/tree.coffee
    @controlType navigation
  
    @restrict E
    
    @param {array} items An array containing node objects, each of which can contain a 'children' property of an array of nodes.
  ```javascript
  [
    {
      label: 'Home', // This
      id: 'home',    // and/or this required for each node
      icon: 'house',
      children: [
        {
          label: 'A child',
          id: 'child'
        }
      ]
    }, {
      label: 'Logout'
    }
  ]
  ```
  
    @param {string=} selectedName The selected id, or, if the node doesn't have an id, the label. This property can be set: If the value is invalid or empty, nothing in the tree is selected. If the object has no children (null/undefined property or empty array), it can be selected.
    @param {object=} selectedNode The selected node. Its use is accessing properties on the node that you may want when it changes (such as a function to call). It can only be read.
    
    @example
    <h3>Static tree</h3>
  
  <example name='staticTree'>
    <file name='index.html'>
      <cui-tree items="items"></cui-tree>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.items = [
            {
              label: 'Home', // This
              id: 'home',    // and/or this required for each node
              icon: 'house',
              children: [
                {
                  label: 'A child',
                  id: 'child'
                }
              ]
            }, {
              label: 'Logout'
            }
          ];
        });
    </file>
  </example>
  
    @example
    <h3>Interactive tree</h3>
  
  <example name='staticTree'>
    <file name='index.html'>
      <!-- The selectedName value can be set (case sensitive) -->
      <cui-text-box ng-model="selectedItem" placeholder="Node indentifier"></cui-text-box>
  
      <!-- The selectedNode value cannot -->
      The label: {{ node.label }}
  
  
      <br><br>
      <cui-tree items="items" selected-name="selectedItem" selected-node="node"></cui-tree>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          // Two different ways to execute a command when clicked...
  
          // This one watches for when a value is something
          $scope.$watch('selectedItem', function() {
            if($scope.selectedItem == 'childId2')
              alert("Clicked childId2!")
          })
  
          // This one is more general -- looks for a specific function on the node
          $scope.$watch('node', function() {
            if($scope.node && $scope.node.doSomething) $scope.node.doSomething()
          })
  
  
          $scope.items = [{
            label: 'label1',
            id: 'id1',
            children: [{
              label: 'childLabel1 ( doSomething() ) (no id)', // No id, fallback to label
              doSomething: function() {
                alert('Woah! Something!');
              },
              icon: 'file'
            },{
              label: 'childLabel2 (do something else)',
              id: 'childId2',
              icon: 'flag'
            },{
              label: 'childLabel3',
              id: 'childId3',
              icon: 'file'
            }]
          },{
            label: 'label2',
            id: 'id2'
          },{
            label: 'label3 (Open by default)',
            visible: true,
            id: 'id3',
            children: [{
              label: 'child1 (no id)',
              icon: 'file'
            },{
              label: 'child2 (no id)',
              icon: 'file'
            }]
          }];
        });
    </file>
  </example>
   */

  module = angular.module('cui.controls.tree', ['cui.base']);

  module.directive('cuiTree', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "tree.html",
      restrict: 'E',
      scope: {
        items: '=',
        selectedName: '=?',
        selectedNode: '=?'
      },
      controller: function($scope) {
        var nodes;
        nodes = $scope.nodes = [];
        this.addNode = function(node) {
          return nodes.push(node);
        };
        this.selectNode = function(node) {
          return $scope.selectedName = node.items.id || node.items.label;
        };
        return this;
      },
      link: function(scope, element, attrs, cuiTreeController) {
        return scope.$watch('selectedName', function() {
          scope.selectedNode = null;
          return angular.forEach(scope.nodes, function(n) {
            if (((n.items.id != null) && scope.selectedName === n.items.id) || ((n.items.label != null) && (n.items.id == null) && scope.selectedName === n.items.label)) {
              n.items.active = true;
              return scope.selectedNode = n.items;
            } else {
              return n.items.active = false;
            }
          });
        });
      }
    };
  });

  module.directive('cuiTreeItem', function(baseTemplatePath, $compile) {
    return {
      templateUrl: "" + baseTemplatePath + "treeItem.html",
      restrict: 'E',
      require: '^cuiTree',
      scope: {
        items: '='
      },
      compile: function(tElement, tAttr) {
        var compiledContents, contents;
        contents = tElement.contents().remove();
        compiledContents = void 0;
        return function(scope, iElement, iAttr, cuiTreeController) {
          if (!compiledContents) {
            compiledContents = $compile(contents);
          }
          compiledContents(scope, function(clone) {
            return iElement.append(clone);
          });
          cuiTreeController.addNode(scope);
          scope.toggleVisible = function() {
            return scope.items.visible = !scope.items.visible;
          };
          return scope.toggleActive = function() {
            return cuiTreeController.selectNode(scope);
          };
        };
      }
    };
  });


  /**
    @ngdoc directive
    @new true
    @module ui.select
    @name uiSelect
    @description The UI Select directive is a 3rd party dropdown/multi-select Angular component. It allows for very complex use-cases in applications.
  
  UI Select is tailored for an integrated look and feel with CUI.
  
  <h3>For more demos and documentation, please see <a href='https://github.com/angular-ui/ui-select'>the Github page</a>.</h3>
  
  Note: When using `ui-select`, do NOT use the `theme` attribute. CUI automatically changes the default for styling.
  
    @controlType input
    @restrict None
  
    @example
    <h3>UI Select</h3>
  <example>
    <file name='index.html'>
      <p>Selected: {{person.selected}}</p>
      <ui-select ng-model="person.selected" style="min-width: 300px;" ng-disabled='disabled'>
        <ui-select-match placeholder="Select a person in the list or search his name/age...">{{$select.selected.name}}</ui-select-match>
        <ui-select-choices repeat="person in people | propsFilter: {name: $select.search, age: $select.search}">
          <div ng-bind-html="person.name | highlight: $select.search"></div>
          <small>
            email: {{person.email}}
            age: <span ng-bind-html="''+person.age | highlight: $select.search"></span>
          </small>
        </ui-select-choices>
      </ui-select>
      <cui-checkbox name='disabledCheck' ng-model='disabled'>Disable ui-select</cui-checkbox>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.person = {};
          $scope.people = [
            { name: 'Adam',      email: 'adam@email.com',      age: 10 },
            { name: 'Amalie',    email: 'amalie@email.com',    age: 12 },
            { name: 'Wladimir',  email: 'wladimir@email.com',  age: 30 },
            { name: 'Samantha',  email: 'samantha@email.com',  age: 31 },
            { name: 'Estefanía', email: 'estefanía@email.com', age: 16 },
            { name: 'Natasha',   email: 'natasha@email.com',   age: 54 },
            { name: 'Nicole',    email: 'nicole@email.com',    age: 43 },
            { name: 'Adrian',    email: 'adrian@email.com',    age: 21 }
          ];
        })
        // AngularJS default filter with the following expression:
        // "person in people | filter: {name: $select.search, age: $select.search}"
        // performs a AND between 'name: $select.search' and 'age: $select.search'.
        // We want to perform a OR.
   
        .filter('propsFilter', function() {
          return function(items, props) {
            var out = [];
  
            if (angular.isArray(items)) {
              items.forEach(function(item) {
                var itemMatches = false;
  
                var keys = Object.keys(props);
                for (var i = 0; i < keys.length; i++) {
                  var prop = keys[i];
                  var text = props[prop].toLowerCase();
                  if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                    itemMatches = true;
                    break;
                  }
                }
  
                if (itemMatches) {
                  out.push(item);
                }
              });
            } else {
              // Let the output be the input untouched
              out = items;
            }
  
            return out;
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 350px;
      }
    </file>
  </example>
  
  
    @example
    <h3>UI Multiselect</h3>
  <example>
    <file name='index.html'>
      <ui-select multiple ng-model="demo.colors" style="width: 300px;">
        <ui-select-match placeholder="Select colors...">{{$item}}</ui-select-match>
        <ui-select-choices repeat="color in availableColors | filter:$select.search">
          {{color}}
        </ui-select-choices>
      </ui-select>
      <p>Selected: {{demo.colors}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.demo = {};
          $scope.demo.colors = ['Blue', 'Red'];
          $scope.availableColors = ['Red','Green','Blue','Yellow','Magenta','Maroon','Umbra','Turquoise'];
        })
        // AngularJS default filter with the following expression:
        // "person in people | filter: {name: $select.search, age: $select.search}"
        // performs a AND between 'name: $select.search' and 'age: $select.search'.
        // We want to perform a OR.
   
        .filter('propsFilter', function() {
          return function(items, props) {
            var out = [];
  
            if (angular.isArray(items)) {
              items.forEach(function(item) {
                var itemMatches = false;
  
                var keys = Object.keys(props);
                for (var i = 0; i < keys.length; i++) {
                  var prop = keys[i];
                  var text = props[prop].toLowerCase();
                  if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                    itemMatches = true;
                    break;
                  }
                }
  
                if (itemMatches) {
                  out.push(item);
                }
              });
            } else {
              // Let the output be the input untouched
              out = items;
            }
  
            return out;
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 350px;
      }
    </file>
  </example>
  
  
    @example
    <h3>UI Select (Address)</h3>
  <example>
    <file name='index.html'>
      <p>Selected: {{address.selected.formatted_address}}</p>
      <ui-select ng-model="address.selected"
                 reset-search-input="false"
                 style="width: 300px;">
        <ui-select-match placeholder="Enter an address...">{{$select.selected.formatted_address}}</ui-select-match>
        <ui-select-choices repeat="address in addresses track by $index"
                 refresh="refreshAddresses($select.search)"
                 refresh-delay="0">
          <div ng-bind-html="address.formatted_address | highlight: $select.search"></div>
        </ui-select-choices>
      </ui-select>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $http) {
          $scope.address = {};
          $scope.refreshAddresses = function(address) {
            var params = {address: address, sensor: false};
            return $http.get(
              'http://maps.googleapis.com/maps/api/geocode/json',
              {params: params}
            ).then(function(response) {
              $scope.addresses = response.data.results
            });
          };
        })
        // AngularJS default filter with the following expression:
        // "person in people | filter: {name: $select.search, age: $select.search}"
        // performs a AND between 'name: $select.search' and 'age: $select.search'.
        // We want to perform a OR.
   
        .filter('propsFilter', function() {
          return function(items, props) {
            var out = [];
  
            if (angular.isArray(items)) {
              items.forEach(function(item) {
                var itemMatches = false;
  
                var keys = Object.keys(props);
                for (var i = 0; i < keys.length; i++) {
                  var prop = keys[i];
                  var text = props[prop].toLowerCase();
                  if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                    itemMatches = true;
                    break;
                  }
                }
  
                if (itemMatches) {
                  out.push(item);
                }
              });
            } else {
              // Let the output be the input untouched
              out = items;
            }
  
            return out;
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 350px;
      }
    </file>
  </example>
  
  
    @example
    <h3>UI Select (Tagging)</h3>
  <example>
    <file name='index.html'>
      <ui-select multiple tagging tagging-label="(custom 'new' label)" ng-model="demo.colors" style="width: 300px;">
        <ui-select-match placeholder="Select colors...">{{$item}}</ui-select-match>
        <ui-select-choices repeat="color in availableColors | filter:$select.search">
          {{color}}
        </ui-select-choices>
      </ui-select>
      <p>Selected: {{demo.colors}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $http) {
          $scope.demo = {};
          $scope.demo.colors = ['Blue', 'Red'];
          $scope.availableColors = ['Red','Green','Blue','Yellow','Magenta','Maroon','Umbra','Turquoise'];
        })
        // AngularJS default filter with the following expression:
        // "person in people | filter: {name: $select.search, age: $select.search}"
        // performs a AND between 'name: $select.search' and 'age: $select.search'.
        // We want to perform a OR.
   
        .filter('propsFilter', function() {
          return function(items, props) {
            var out = [];
  
            if (angular.isArray(items)) {
              items.forEach(function(item) {
                var itemMatches = false;
  
                var keys = Object.keys(props);
                for (var i = 0; i < keys.length; i++) {
                  var prop = keys[i];
                  var text = props[prop].toLowerCase();
                  if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                    itemMatches = true;
                    break;
                  }
                }
  
                if (itemMatches) {
                  out.push(item);
                }
              });
            } else {
              // Let the output be the input untouched
              out = items;
            }
  
            return out;
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 350px;
      }
    </file>
  </example>
  
  
    @example
    <h3>UI Select (Advanced Tagging)</h3>
  <example>
    <file name='index.html'>
      <h3>Object Tags with Tokenization (Space, Forward Slash, Comma)</h3>
      <strong>Note that the SPACE character can't be used literally, use the keyword SPACE</strong>
      <ui-select multiple tagging="tagTransform" tagging-tokens="SPACE|,|/" ng-model="demo.selectedPeople" style="width: 800px;">
        <ui-select-match placeholder="Select person...">{{$item.name}} &lt;{{$item.email}}&gt;</ui-select-match>
        <ui-select-choices repeat="person in people | propsFilter: {name: $select.search, age: $select.search}">
          <div ng-if="person.isTag" ng-bind-html="person.name + ' ' + $select.taggingLabel | highlight: $select.search"></div>
          <div ng-if="!person.isTag" ng-bind-html="person.name| highlight: $select.search"></div>
          <small>
            email: {{person.email}}
            age: <span ng-bind-html="''+person.age | highlight: $select.search"></span>
          </small>
        </ui-select-choices>
      </ui-select>
      <p>Selected: {{demo.selectedPeople}}</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $http) {
          $scope.demo = {};
          
          $scope.tagTransform = function (newTag) {
            var item = {
                name: newTag,
                email: newTag.toLowerCase()+'@email.com',
                age: 'unknown',
                country: 'unknown'
            };
  
            return item;
          };
  
          $scope.people = [
            { name: 'Adam',      email: 'adam@email.com',      age: 12, country: 'United States' },
            { name: 'Amalie',    email: 'amalie@email.com',    age: 12, country: 'Argentina' },
            { name: 'Estefanía', email: 'estefania@email.com', age: 21, country: 'Argentina' },
            { name: 'Adrian',    email: 'adrian@email.com',    age: 21, country: 'Ecuador' },
            { name: 'Wladimir',  email: 'wladimir@email.com',  age: 30, country: 'Ecuador' },
            { name: 'Samantha',  email: 'samantha@email.com',  age: 30, country: 'United States' },
            { name: 'Nicole',    email: 'nicole@email.com',    age: 43, country: 'Colombia' },
            { name: 'Natasha',   email: 'natasha@email.com',   age: 54, country: 'Ecuador' },
            { name: 'Michael',   email: 'michael@email.com',   age: 15, country: 'Colombia' },
            { name: 'Nicolás',   email: 'nicolas@email.com',    age: 43, country: 'Colombia' }
          ];
        })
        // AngularJS default filter with the following expression:
        // "person in people | filter: {name: $select.search, age: $select.search}"
        // performs a AND between 'name: $select.search' and 'age: $select.search'.
        // We want to perform a OR.
   
        .filter('propsFilter', function() {
          return function(items, props) {
            var out = [];
  
            if (angular.isArray(items)) {
              items.forEach(function(item) {
                var itemMatches = false;
  
                var keys = Object.keys(props);
                for (var i = 0; i < keys.length; i++) {
                  var prop = keys[i];
                  var text = props[prop].toLowerCase();
                  if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                    itemMatches = true;
                    break;
                  }
                }
  
                if (itemMatches) {
                  out.push(item);
                }
              });
            } else {
              // Let the output be the input untouched
              out = items;
            }
  
            return out;
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 350px;
      }
    </file>
  </example>
   */

  module = angular.module('cui.controls.uiSelect', ['cui.base']);

  module.config(function(uiSelectConfig) {
    return uiSelectConfig.theme = '__cui';
  });


  /**
    @ngdoc service
    @name aboutBox
    @module cui.controls
    @src modules/aboutBox/aboutBox.coffee
    @controlType foundational
  
    @description The About Box displays general information about the Application, including legal notices, and support contact information.
  
    @example
    <h3>About box</h3>
  
  <example name='aboutBox'>
    <file name='index.html'>
      <cui-button ng-click='showAboutBox()' cui-type='primary'>Show About Box</cui-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiAboutBox) {
          var aboutBox = cuiAboutBox({
            applicationName: 'Change Auditor',
            licenses: 'licenses.json',
            thirdParty: 'thirdParty.json',
            version: '2.0.0',
            tabs: [{
              label: 'About',
              active: true,
              template: 'aboutBox.about'
            }]
          });
  
          aboutBox.modal.show();
  
          $scope.showAboutBox = aboutBox.modal.show;
        })
    </file>
    <file name='styles.css'>
      body {
        height: 800px;
      }
    </file>
    <file name='licenses.json'>
      [
        {
          "name": "ChangeAuditor for Exchange",
          "type": "Ongoing",
          "expires": "2015-04-12T05:00:00.000Z",
          "seatsLicensed": 10,
          "seatsUsed": 11,
          "invalid": true,
          "errorMessage": "Too many seats used."
        },
        {
          "name": "ChangeAuditor Express Support",
          "type": "Ongoing",
          "expires": "2019-12-20T06:00:00.000Z",
          "seatsLicensed": 1,
          "seatsUsed": 1
        },
        {
          "name": "ChangeAuditor for Abacus",
          "type": "Ongoing",
          "expires": "1592-03-14T05:00:00.000Z",
          "seatsLicensed": 3,
          "seatsUsed": 2,
          "invalid": true,
          "errorMessage": "License is expired. Please contact sales for support."
        }
      ]
    </file>
    <file name='thirdParty.json'>
      [
        {
          "name": "ABC Component",
          "version": "v4.0",
          "license": {
            "name": "Apache v2.0"
          }
        }, {
          "name": "EDDraw Office Viewer",
          "version": "5.3",
          "license": {
            "notice": "Copyright 2006 EDDraw. All rights reserved."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }, {
          "name": "Filler third party components",
          "version": "41",
          "license": {
            "notice": "To demonstrate overflow. Nothing to see here."
          }
        }
      ]
    </file>
  </example>
   */


  /**
  @ngdoc method
  @name aboutBox#
  @param {object} config An object with the following properties:
  
  ```javascript
  {
    applicationName: 'Change Auditor',
    licenses: 'api/licenses.json', // String for url, or object
    thirdParty: 'api/thirdParty.json', // String for url, or object
    patentString: '',
    visible: false, // Boolean, default false. If true, the about box will be shown once created
    tabs: [{
      label: 'About',
      active: true,
      template: 'aboutBox.about'
    }]
  }
  ```
  
  `applicationName`: The name of the application. Shown on the "About" page of the About Box.
   
  `version`: The version of the application. This is displayed below the `applicationName`.
   
  `licenses`: An array of objects describing the licenses for the application.
  
  Can be passed a JSON endpoint of the form `'"/api/licenses.json"'` (notice the ' followed by ", similar to the syntax for `ng-include`).
  
  If a string is passed $cui.services.dataSource will be used to fetch the JSON from the specified endpoint.
  
  The default template for the licenses tab expects objects in the array to have the following structure:
  
  ```javascript
  {
   "name": "Change Auditor",
   "type": "Ongoing",
   "expires": "2015-04-12",
   "seatsLicensed": 10,
   "seatsUsed": 11,
   "invalid": true // Optional, adds 'error' class to row and adds a title attribute to the row containing the `errorMessage`.
   "errorMessage": "Too many seats used, please contact sales for further help."
  }
  ```
  
  `thirdParty`: An array of objects describing the licenses for the Third Party components or libraries that the application uses.
  
  Can be passed a JSON endpoint of the form `'"/api/thirdParty.json"'` (notice the ' followed by ", similar to the syntax for `ng-include`).
  
  If a string is passed, $cui.services.dataSource will be used to fetch the JSON from the specified endpoint.
  
  The default template for the Third Party tab expects objects in the array to have the following structure:
  
  ```javascript
  {
   "name": "Bootstrap",
   "version": "3.0.3",
   "license": {
     "name": "Apache v2.0",
     "notice": "Custom notice" // Optional, only used if author of library requires a notice be placed in your application.
   }
  }
  ```
  
  `patentString`: The list of patents that apply to the application. If this is not included, the "Patents" line on the "About" tab will not be included.
   */

  module = angular.module('cui.modules.aboutBox', ['cui.base']);

  module.service('cuiAboutBox', function(cuiModal, baseTemplatePath, cuiDataSourceService) {
    return function(options) {
      var gtp, scope;
      if (options == null) {
        options = {};
      }
      gtp = function(path) {
        return "" + baseTemplatePath + path + ".html";
      };
      scope = cuiModal({
        type: 'dismiss',
        templateUrl: "" + baseTemplatePath + "aboutBox.html",
        visible: options.visible,
        applicationName: options.applicationName,
        version: options.version,
        patentString: options.patentString,
        className: 'cui-about-box'
      });
      if (angular.isString(options.licenses)) {
        cuiDataSourceService(options.licenses).all().then(function(licenses) {
          return scope.licensesArray = licenses;
        });
      } else {
        scope.licensesArray = options.licenses;
      }
      if (angular.isString(options.thirdParty)) {
        cuiDataSourceService(options.thirdParty).all().then(function(thirdParty) {
          return scope.thirdPartyArray = thirdParty;
        });
      } else {
        scope.thirdPartyArray = options.thirdParty;
      }
      scope.today = new Date();
      scope.tabs = [
        {
          label: 'About',
          active: null,
          template: options.aboutTmpl || gtp('aboutBox.about')
        }
      ];
      scope.$on('hide', function() {
        var index, tab, _ref, _results;
        _ref = scope.tabs;
        _results = [];
        for (index in _ref) {
          tab = _ref[index];
          if (index === '0') {
            _results.push(tab.active = true);
          } else {
            _results.push(tab.active = null);
          }
        }
        return _results;
      });
      if (options.licenses != null) {
        scope.tabs.push({
          label: 'Licenses',
          active: null,
          template: options.licensesTmpl || gtp('aboutBox.licenses')
        });
      }
      if (options.thirdParty != null) {
        scope.tabs.push({
          label: 'Third Party',
          active: null,
          template: options.thirdPartyTmpl || gtp('aboutBox.thirdParty')
        });
      }
      scope.tabs.push({
        label: 'Contact',
        active: null,
        template: options.contact || gtp('aboutBox.contact')
      });
      return scope;
    };
  });


  /**
    @ngdoc directive
    @name applicationFrame
  
    @description The Application Frame is a simple way to get the "DellBow" into your web application.
  
  The `cuiApplicationFrame` supports complex/multi transclusion. See the following examples:
  
  <div class='cui-grid'>
    <div class='cui-unit cui-half'>
  <pre>
  ```
  <cui-application-frame ... >
    <div cui-controls>
      <!-- Controls go here -->
    </div>
    <div cui-content>
      <!-- Content goes here -->
    </div>
  </cui-application-frame>
  ```
  </pre>
    </div>
    <div class='cui-unit cui-half'>
  <pre>
  ```
  <cui-application-frame ... >
      <!-- Content goes here -->
      <!-- No ability to add controls to masthead -->
  </cui-application-frame>
  ```
  </pre>
    </div>
  </div>
  
    @module cui.modules
    @src modules/applicationFrame/applicationFrame.coffee
  
    @controlType foundational
    @restrict E
  
    @param {string=} applicationHref Url for the applicationName or applicationLogo for the Masthead.
  
    @param {string=} applicationLogo When applicationLogo is used, applicationName is overridden for the Masthead.
  
    @param {string} applicationName The label for the Masthead.
    
  `applicationName` is also used as the `persistAs` value for the Navigation List.
  
    @param {string=} applicationSubname The secondary label for the Masthead.
  
    @param {boolean=} mastheadShowControls True or false setting which binds back to Masthead showControls.
  
    @param {boolean=} navigationPersist Default `true`. If `true`, the `applicationName` will be used for the `persistAs` value of the Navigation List. If `false`, no `persistAs` value will be set.
  
    @param {array=} navigationItems See the Navigation List's 'items' attribute for more information.
  
    @example
    <h3>Basic transclusion, no navigation list</h3>
    <p>Click 'pop out' to view the non-mobile version (with a larger width).</p>
  <example name='basicApplicationFrame'>
    <file name='index.html'>
      <cui-application-frame application-name='The Base for your App'>
        <h2>Hello, world!</h2>
        <cui-button cui-type='primary'>
          To the moon! <cui-icon icon='moon'></cui-icon>
        </cui-button>
      </cui-application-frame>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function() {});
    </file>
    <file name='styles.css'>
      body {
        height: 200px;
      }
    </file>
  </example>
  
    @example
    <h3>Complex transclusion with navigation list</h3>
    <p>Click 'pop out' to view the non-mobile version (with a larger width).</p>
  <example name='basicApplicationFrame'>
    <file name='index.html'>
      <cui-application-frame application-name='Example title' application-href='http://www.dell.com' application-subname='SubTitle' masthead-show-controls=true navigation-items='navigationItems'>
        <div cui-controls>
          <cui-drop-down-button label='Drop down button' cui-type='primary' icon="dashboard">
            <ul cui-keyboard-menu>
              <li kb-focus='focus-1' ng-click='click("Example 1 - Item 1")'>Item 1</li>
              <li ng-click='click("Example 1 - Item 2")'>Item 2</li>
              <li ng-click='click("Example 1 - Item 3")'>Item 3</li>
            </ul>
          </cui-drop-down-button>
  
          <cui-button ng-click='showSettings()'>
            <cui-icon icon='cog'></cui-icon> Settings
          </cui-button>
  
          <cui-button ng-click='showAboutBox()'>
            <cui-icon icon='question-sign'></cui-icon> About Box
          </cui-button>
        </div>
        <div cui-content>
          <h1>Hello there, {{username}}!</h1>
          <cui-text-box ng-model='username'></cui-text-box>
          <cui-button cui-type='primary'>Nested controls: Reloaded</cui-button>
        </div>
  
      </cui-application-frame>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.username = 'Michael Dell';
          
          function closeControls() {
            $scope.$broadcast('cui:masthead:controls:close');
          }
  
          $scope.manageAccount = function() {
            alert("manage your users account");
            closeControls();
          }
  
          $scope.showSettings = function() {
            alert('Show application settings');
            closeControls();
          }
  
          $scope.showAboutBox = function() {
            alert('Show the About Box');
            closeControls();
          }
  
          $scope.navigationItems = [
            {
              label: "Home",
              icon: "home"
            },
            {
              label: "Services",
              icon: "tasks"
            },
            {
              label: "Profiles",
              icon: "user"
            },
            {
              label: "Templates",
              icon: "tags"
            },
            {
              label: "Devices",
              icon: "laptop"
            },
            {
              label: "Jobs",
              icon: "refresh"
            },
            {
              label: "Settings",
              icon: "cog",
              children: [
                {
                  label: "Users"
                },
                {
                  label: "Environment"
                },
                {
                  label: "Credentials"
                },
                {
                  label: "Logs"
                },{
                  label: "Appliance Management"
                },{
                  label: "Backup and Restore"
                },{
                  label: "Polling Intervals"
                }
              ]
            }
          ]
        });
    </file>
    <file name='styles.css'>
      body {
        height: 600px;
      }
    </file>
  </example>
   */

  module = angular.module('cui.modules.applicationFrame', ['cui.base']);

  module.directive('cuiApplicationFrame', function(baseTemplatePath) {
    return {
      templateUrl: "" + baseTemplatePath + "applicationFrame.html",
      restrict: 'EA',
      transclude: true,
      replace: true,
      scope: {
        navigationItems: '=',
        navigationPersist: '=?',
        applicationName: '@',
        applicationSubname: '@',
        mastheadShowControls: '@',
        applicationHref: '@',
        applicationLogo: '@'
      },
      controller: function($scope) {
        this.$scope = $scope;
        this.multiTranscludeEl = null;
        return this;
      },
      link: function(scope) {
        var handleResize;
        scope.persistAs = scope.applicationName;
        if (scope.navigationPersist === false) {
          scope.persistAs = '';
        }
        scope.toggleNavigation = function() {
          return scope.navigationShowing = !scope.navigationShowing;
        };
        scope.swipeRight = function() {
          return scope.navigationShowing = true;
        };
        scope.swipeLeft = function() {
          return scope.navigationShowing = false;
        };
        handleResize = scope.handleResize = function() {
          scope.navigationShowing = false;
          return scope.$apply();
        };
        angular.element(window).on('resize', handleResize);
        return scope.$on('cui:navigationList:itemClick', function() {
          return scope.toggleNavigation();
        });
      }
    };
  });

  module.directive('cuiApplicationFrameTransclude', function() {
    var el;
    el = null;
    return {
      require: '^cuiApplicationFrame',
      link: function(scope, element, attrs, cuiApplicationFrameController, transclude) {
        var attach, fallback, selectAttr;
        selectAttr = function(attr) {
          return "[" + attr + "]";
        };
        fallback = function(clone) {
          var contentArea, controlsArea, currentAreaIsContentArea;
          contentArea = clone[0].querySelector(selectAttr('cui-content'));
          controlsArea = clone[0].querySelector(selectAttr('cui-controls'));
          currentAreaIsContentArea = attrs.cuiApplicationFrameTransclude === 'cui-content';
          if ((contentArea != null) || (controlsArea != null)) {
            return false;
          }
          if (currentAreaIsContentArea) {
            return 'fallbackAndTransclude';
          }
          return 'fallbackAndStop';
        };
        attach = function(clone) {
          var part, shouldFallback;
          shouldFallback = fallback(clone);
          switch (shouldFallback) {
            case false:
              part = angular.element(clone[0].querySelector(selectAttr(attrs.cuiApplicationFrameTransclude)));
              element.html('');
              return element.append(part);
            case 'fallbackAndTransclude':
              element.html('');
              return element.append(clone);
          }
        };
        if (cuiApplicationFrameController.multiTranscludeEl != null) {
          return attach(cuiApplicationFrameController.multiTranscludeEl);
        } else {
          return transclude(cuiApplicationFrameController.$scope.$parent, function(clone) {
            var wrap;
            wrap = angular.element('<span></span>');
            wrap.append(clone);
            cuiApplicationFrameController.multiTranscludeEl = wrap;
            return attach(wrap);
          });
        }
      }
    };
  });


  /**
    @ngdoc directive
    @module cui.controls
    @name masterDetail
  
    @description
  The Master Detail is suited for displaying a large number of data and provides the ability to select an individual item and view more information on that item.
  
  Unlike many of the CUI controls, there is actually no custom Angular code in the Master Detail view -- we provide CSS styling to help out, but all Angular functionality is up to you to implement and customize. Below, we have some Angular code implementing the Master Detail.
  
    @controlType tabular
  
    @restrict E
  
    @example
    <h3>Cui Master Detail with selection</h3>
  <example name='masterDetail'>
    <file name='index.html'>
      <cui-master-detail ng-class="{'cui-collapse-detail': masterdetail.collapsed}">
        <cui-master-view>
          <input class="cui-textbox" ng-model='dataGrid.searchQuery' placeholder='Search'>
          <cui-data-grid config='dataGrid' selected='selected'></cui-data-grid>
        </cui-master-view>
  
        <cui-detail-view>
          <div class='cui-collapse-detail-button' ng-click='masterdetail.collapsed = !masterdetail.collapsed'></div>
          <div class='cui-detail-header'>
            <cui-icon ng-if='selected' icon="{{selected.online && 'ok' || 'warning-sign'}}" color="{{selected.online && 'green' || 'red'}}"></cui-icon>
            <span class='cui-detail-light'>Server:</span> <strong>{{selected.ip || 'None'}}</strong> <span class='cui-detail-light'>Service Tag:</span> <strong>#{{selected.servicetag || 'None'}}</strong>
          </div>
          <div class='cui-detail-content'>
            <cui-button cui-type='primary' type='button'>A Button</cui-button>
            <h3>More information</h3>
            <table class='cui-detail-table'>
              <tr ng-repeat="(k,v) in selected">
                <td class='cui-half label'>{{k}}</td>
                <td class='cui-half value'>{{v}}</td>
              </tr>
            </table>
          </div>
        </cui-detail-view>
      </cui-master-detail>
    </file>
  
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.masterdetail = { collapsed: true };
          
          // // Use this if you want the detail view to open automatically upon clicking a row
          $scope.$watch('selected', function(newS, oldS) {
            if (!oldS && newS) {
              $scope.masterdetail.collapsed = false;
            } else if (oldS && !newS) {
              $scope.masterdetail.collapsed = true;
            } else if (oldS && newS && oldS.id !== newS.id) {
              $scope.masterdetail.collapsed = false;
            }
          });
          $scope.dataGrid = {
            id: 'id',
            pageSize: 15,
            searchable: false,
            url: "http://ce.software.dell.com/api/server",
            columns: [{
              label: 'Status',
              map: 'online',
              cellTemplate: '<cui-icon icon="{{row.online && \'ok\' || \'warning-sign\'}}" color="{{row.online && \'green\' || \'red\'}}"></cui-icon>'
            }, {
              label: 'Type',
              map: 'type'
            }, {
              label: 'Location',
              map: 'location'
            }]
          };
      });
    </file>
  </example>
  
    @example
    <h3>Cui Master Detail with checking</h3>
  <example name='masterDetail'>
    <file name='index.html'>
      <cui-master-detail ng-class="{'cui-collapse-detail': masterdetail.collapsed}">
        <cui-master-view>
          <input class="cui-textbox" ng-model='dataGrid.searchQuery' placeholder='Search'>
          <cui-data-grid config='dataGrid' checked='checked'></cui-data-grid>
        </cui-master-view>
  
        <cui-detail-view>
          <div class='cui-collapse-detail-button' ng-click='masterdetail.collapsed = !masterdetail.collapsed'></div>
          <div class='cui-detail-header'>
            <span class='cui-detail-light'>Servers selected:</span> <strong>{{checked.length}}</strong></span>
          </div>
          <div class='cui-detail-content'>
            <cui-button cui-type='primary' type='button'>A Button</cui-button>
            <h3>Server IDs checked:</h3>
            <ol>
              <li ng-repeat='server in checked'>{{server.id}}</li>
            </ol>
          </div>
        </cui-detail-view>
      </cui-master-detail>
    </file>
  
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.masterdetail = { collapsed: true };
          
          // // Use this if you want the detail view to open automatically upon clicking a row
          $scope.$watch('checked.length', function(l) {
            $scope.masterdetail.collapsed = !l;
          });
          $scope.dataGrid = {
            id: 'id',
            pageSize: 15,
            searchable: false,
            selection: false,
            displayChecks: true,
            url: "http://ce.software.dell.com/api/server",
            columns: [{
              label: 'Status',
              map: 'online',
              cellTemplate: '<cui-icon icon="{{row.online && \'ok\' || \'warning-sign\'}}" color="{{row.online && \'green\' || \'red\'}}"></cui-icon>'
            }, {
              label: 'Type',
              map: 'type'
            }]
          };
      });
    </file>
  </example>
   */

  module = angular.module('cui.modules.masterDetail', ['cui.base']);


  /**
    @ngdoc directive
    @name startScreen
  
    @description The start screen is a dell-branded login/splash page.
    
    @module cui.controls
    @src modules/startScreen/startScreen.coffee
    @directive cui-start-screen
    @controlType foundational
    @restrict E
  
    @transclude Content for the message displayed to the user. Can be shown with the showMessage param.
  
    @param {object=} config Object that you can set or read current start screen settings or variables. The object has the following properties:
  
   * * __applicationName__ &mdash; `{string}` &mdash;
   *   Text for the Application name. Default is Application Name
   * * __domain__ &mdash; `{string}` &mdash;
   *   Value for the domain field.
   * * __username__ &mdash; `{string}` &mdash;
   *   Value for username field.  Note that we do parse the username field for common domain values and put them in their correct place.
   * * __password__ &mdash; `{string}` &mdash;
   *   Value for password field.
   * * __copyright__ &mdash; `{string}` &mdash;
   *   Message field that displays at the bottom of the page. Default is &copy; 2014 Dell Inc. ALL RIGHTS RESERVED
   * * __domainPlaceholder__ &mdash; `{string}` &mdash;
   *   Placeholder text for domain field. Default is Domain
   * * __usernamePlaceholder__ &mdash; `{string}` &mdash;
   *   Placeholder text for username field. Default is Username
   * * __passwordPlaceholder__ &mdash; `{string}` &mdash;
   *   Placeholder text for password field. Default is Password
   * * __signInLabel__ &mdash; `{string}` &mdash;
   *   Text for Sign In button. Default is Sign In
   * * __isSpinning__ &mdash; `{boolean}` &mdash;
   *   If true login fields and sign in button is hidden and spinner is displayed. Default is false
   * * __showDomain__ &mdash; `{boolean}` &mdash;
   *   Displays the domain field. Default is false
   * * __showRememberMe__ &mdash; `{boolean}` &mdash;
   *   Displays the rememberMe field. Default is false
   * * __rememberMe__ &mdash; `{boolean}` &mdash;
   *   Defines if Remember me is checked or not. Default is false
   * * __rememberMeText__ &mdash; `{string}` &mdash;
   *   Text next to the remember me checkbox,  Default is Remember Me
   * * __usernameParser__ &mdash; `{function}` &mdash;
   *   A function that takes the username and returns an object with `username` and `domain` properties.
   *   By default, it parses '\' (example: PROD\first.last) and '&#64;' (example: first.last&#64;software.dell.com) into the username and domain.
   *   To disable parsing of the username, set this function to null or undefined.
  
    @param {boolean=} showMessage If true, the transcluded content will be shown. By default, the content will be hidden.
  
    @param {expression=} onSignIn A function passed the object `userData` as its first parameter containing `username`, `password`, `domain`, and `rememberMe` properties.
  ```html
  <cui-start-screen on-sign-in='signIn(userData)'></cui-start-screen>
  ```
  ```javascript
  scope.signIn = function(userData) {
    // userdata = {
    //   username: 'bob@dell.com',
    //   password: 'ExAmPlE',
    //   domain: 'Americas',
    //   rememberMe: false,
    //   parsedUsername: 'bob',
    //   parsedDomain: 'dell.com'
    // }
  }
  ```
  
  If a `promise` is returned in the function you provide, the `config.isSpinning` property will be set to `true` until the promise resolves or rejects.
  
    @example
    <h3>Basic example</h3>
  <example name='startScreen'>
    <file name='index.html'>
      <cui-start-screen
        config="settings"
        on-sign-in="signIn(userData)"
        show-message='true'>
        <span ng-switch='settings.isSpinning'>
          <span ng-switch-when='true'>
            Welcome, {{details.username}}! Please wait (indefinitely)...
          </span>
          <span ng-switch-default>
            <cui-icon icon='warning-sign' color='yellow'></cui-icon> This is a mocked login page. Login with any credentials to demo.
          </span>
        </span>
      </cui-start-screen>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $q) {
          // Example of Settings for Start Screen, to actually change anything from the defaults you must use config.
          $scope.settings = {
            showRememberMe: true,
            showDomain: true
          }
  
  
          $scope.signIn = function(userData) {
            $scope.details = userData;
  
            // You could return an $http promise here, for example
            return $q.defer().promise;
          }
          // Example of listening for the Broadcast of the sign in event.
          $scope.$on('cuiStartScreen:signIn', function(ev, data) {
            // data contains `domain`, `username`, and `password`.
            console.log($scope.settings);
          })
        })
    </file>
    <file name='styles.css'>
    html, body {
      height: 700px;
    }
    </file>
  </example>
   */

  module = angular.module('cui.modules.startScreen', ['cui.base', 'ngAnimate']);

  module.directive('cuiStartScreen', function(baseTemplatePath, $timeout) {
    return {
      templateUrl: "" + baseTemplatePath + "startScreen.html",
      restrict: 'EA',
      transclude: true,
      scope: {
        config: '=?',
        onSignIn: '&',
        showMessage: '='
      },
      controller: function($scope) {
        if ($scope.config == null) {
          $scope.config = {};
        }
        this.defaults = {
          applicationName: 'Application Name',
          showDomain: false,
          showRememberMe: false,
          rememberMe: false,
          usernameParser: function(username) {
            var temp;
            if ((username != null ? username.indexOf('\\') : void 0) > -1) {
              temp = username.split('\\');
              return {
                domain: temp[0],
                username: temp[1]
              };
            } else if ((username != null ? username.indexOf('@') : void 0) > -1) {
              temp = username.split('@');
              return {
                username: temp[0],
                domain: temp[1]
              };
            } else {
              return {
                username: null,
                domain: null
              };
            }
          }
        };
        $scope.config = angular.extend({}, this.defaults, $scope.config);
        return this;
      },
      link: function(scope, element, attrs) {
        var messageText;
        $timeout(function() {
          return element[0].querySelector('.cui-start-screen-username').focus();
        });
        messageText = angular.element(element[0].querySelector('.cui-start-screen-message'));
        scope.$watch('showMessage', function(show) {
          return messageText.css('display', show ? '' : 'none');
        });
        scope.$watch('config.username', function(username) {
          var _base, _ref;
          _ref = typeof (_base = scope.config).usernameParser === "function" ? _base.usernameParser(username) : void 0, scope.parsedUsername = _ref.username, scope.parsedDomain = _ref.domain;
          if (scope.parsedDomain) {
            return scope.config.domain = scope.parsedDomain;
          }
        });
        return scope.signIn = function() {
          var data, onSignInPromise;
          data = {
            username: scope.parsedUsername || scope.config.username,
            password: scope.config.password,
            domain: scope.config.domain,
            rememberMe: scope.config.rememberMe
          };
          scope.$emit('cui:startScreen:signIn', data);
          onSignInPromise = scope.onSignIn({
            userData: data
          });
          if ((onSignInPromise != null ? onSignInPromise['finally'] : void 0) != null) {
            scope.config.isSpinning = true;
            return onSignInPromise['finally'](function() {
              return scope.config.isSpinning = false;
            });
          }
        };
      }
    };
  });


  /**
    @ngdoc directive
    @name wizard
  
    @description A wizard accomplishes a single task with multiple steps that must be performed sequentially. It is recommended to display it within a modal.
  
  
  <h3>Design considerations</h3>
  
  The CUI Wizard can make development of process-based user interaction very easy. However, we recommend that you consider the following:
  
    ** Asynchronous operations (submitting data to the server) should be saved for the end, when the user clicks 'finish'. This makes it easier to show the user what steps are valid. If really needed, you can use the step's `onNext` property for server-side validation.
    ** Client-side validation should be used in-between steps, and each step's `invalid` property should be used where appropriate.
    ** Adding and removing steps to the DOM with `ng-if`, `ng-repeat`, `ng-switch`, etc is completely fine to allow 'forking' directions the wizard could go.
  
  
  <h3>Events</h3>
  
  <h4>Emits:</h4>
  
    ** `'cui:wizard:stepChanged'` when a step is changed.
  
  The event provides two objects, the first being the step the wizard is on now and the second being to step the wizard used to be on. Each object has the properties `index` for the position of the step in the wizard, and `title` for the step title.
  
  Please note that both the old and new indices can be the same (in the case that the current step is removed from the DOM, in which the next step replaces that one).
    
  ```javascript
  // 1st arg
  {
    title: 'Now on this step',
    index: 1
  }
  // 2nd arg
  {
    title: 'Came from this step',
    index: 0
  }
  ```
  
    ** `'cui:wizard:finished'` when the wizard is finished.
    ** `'cui:wizard:cancelled'` when the wizard is cancelled.
  
  
  <h4>Listens for:</h4>
  
  Please note that while these will throw if out-of-bounds or the title of the step isn't found, they **will** allow selecting steps that are invalid.
  
    ** `'cui:wizard:next'`: Goes to the next step.
    ** `'cui:wizard:previous'`: Goes to the previous step.
    ** `'cui:wizard:first'`: Goes to the first step.
    ** `'cui:wizard:last'`: Goes to the last step.
    ** `'cui:wizard:goto'`: Accepts an index or step title to navigate to.
    ** `'cui:wizard:cancel'`: Cancels (emits `'cui:wizard:cancelled'`).
    ** `'cui:wizard:finish'`: Finishes (emits `'cui:wizard:finished'`).
  
  
  <h3>Localization</h3>
  
  The following `$translate` properties are available to localize the wizard:
  
    ** `'CUI_WIZARD_BACK'`: Text for the back button. English default: `'Back'`.
    ** `'CUI_WIZARD_NEXT'`: Text for the next button. English default: `'Next'`.
    ** `'CUI_WIZARD_SAVE_AND_NEXT'`: Text for the next button when `onNext` is set on the current `cui-wizard-step`. English default: `'Save & Next'`.
    ** `'CUI_WIZARD_FINISH'`: Text for the finish button. English default: `'Finish'`.
    ** `'CUI_WIZARD_CANCEL'`: Text for the cancel button. English default: `'Cancel'`.
    ** `'CUI_WIZARD_STEP'`: Description for the current step location. Takes the variables `current` and `total`. English default: <span ng-non-bindable>`'Step {{current}} of {{total}}'`</span>.
  
    @module cui.modules
    @src modules/wizard/wizard.coffee
    @directive cui-wizard
    @controlType navigation
    @restrict E
  
    @transclude <cui-wizard-step
                 cui-title=""
                 [invalid="false"]
                 [on-next]
                 [on-enter]>
               </cui-wizard-step>
               [..]
  
    @param {boolean=} editMode Default `false`.
  
  If `false`, this will force the user to visit every step, even if all following steps are valid. Additionally, the user can only click 'finish' when on the last step (and it is valid).
  
  If `true`, the user can jump around unrestricted, providing all steps are valid. Additionally, if all steps up to the last are valid, the user can click 'finish' at any time.
    
    @param {expression=} onFinish Called (with `$event` if the button is clicked) when the wizard is finished.
    @param {expression=} onCancel Called (with `$event` if the button is clicked) when the wizard is cancelled.
    @param {string=} currentStep The title of the current step.
    @param {number=} currentStepIndex The index of the current step. (This should only be used in specific situations, or when debugging.)
    @example
    <h3>Basic wizard</h3>
    
  <example name='wizard'>
    <file name='index.html'>
      <cui-button ng-click='previous()'>Previous step</cui-button>
      <cui-button ng-click='next()'>Next step</cui-button>
      <cui-button ng-click='cancel()'>Cancel</cui-button>
      <cui-button ng-click='finish()'>Finish</cui-button>
      <cui-button ng-click='goto(0)'>1</cui-button>
      <cui-button ng-click='goto(1)'>2</cui-button>
      <cui-button ng-click='goto(2)'>3</cui-button>
  
      <cui-wizard edit-mode='editMode'>
        <cui-wizard-step cui-title="Welcome!" invalid='!choices.branch'>
          <h1>Start the interactive wizard</h1>
          <p>Click below to switch between methods of dynamically adding steps to the wizard.</p>
          <cui-button ng-click='choices.branch = "repeat"' ng-disabled='choices.branch === "repeat"' cui-type='primary'>Steps with ngRepeat, ngInclude</cui-button>
          <cui-button ng-click='choices.branch = "static"' ng-disabled='choices.branch === "static"' cui-type='primary'>Normal steps added conditionally</cui-button>
        </cui-wizard-step>
        <cui-wizard-step ng-repeat='step in steps' cui-title="{{step.title}}" ng-if='choices.branch === "repeat"'>
          <div ng-include='step.src'></div>
        </cui-wizard-step>
        <div ng-if='choices.branch === "static"'>
          <cui-wizard-step cui-title='Steps added conditionally'>
            <p>This is an example of adding steps conditionally.</p>
          </cui-wizard-step>
          <cui-wizard-step cui-title='More info'>
            <p>This is an example of adding steps conditionally, with some more information.</p>
          </cui-wizard-step>
        </div>
        <cui-wizard-step cui-title='Finish'>
          Finish your wizard.
        </cui-wizard-step>
      </cui-wizard>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope) {
          $scope.next = function() { $scope.$broadcast('cui:wizard:next') };
          $scope.previous = function() { $scope.$broadcast('cui:wizard:previous') };
          $scope.finish = function() { $scope.$broadcast('cui:wizard:finish') };
          $scope.cancel = function() { $scope.$broadcast('cui:wizard:cancel') };
          $scope.goto = function(i) { $scope.$broadcast('cui:wizard:goto', i) };
  
          $scope.$on('cui:wizard:finished', function() { $scope.editMode = true; });
  
  
          $scope.choices = {
            branch: ''
          }
          $scope.$on('cui:wizard:cancelled', function() {
            $scope.$broadcast('cui:wizard:first');
            $scope.choices.branch = '';
            $scope.editMode = false;
          });
  
          $scope.steps = [{
            title: 'ngRepeat',
            src: 'step-0.html'
          }, {
            title: 'ngRepeat 2',
            src: 'step-1.html'
          }, {
            title: 'ngRepeat 3',
            src: 'step-2.html'
          }];
        });
    </file>
    <file name='step-0.html'>
      <h2>Dynamic steps</h2>
      <p>This is the zeroth step. I am demonstrating the ability to <strong>dynamically</strong> add steps to your wizard.</p>
    </file>
    <file name='step-1.html'>
      <h2>ng-repeat, and more</h2>
      <p>While this example uses ng-repeat, the same functionality can be achieved with ng-if, ng-switch, and other Angular DOM modifiers.</p>
    </file>
    <file name='step-2.html'>
      <h2>Needs to modify DOM</h2>
      <p>Please note that something like ng-show/ng-hide would not work, since it doesn't actually remove the step from the DOM.</p>
    </file>
  </example>
  
    @example
    <h3>Wizard with asynchronous requests each step</h3>
    
  <example name='wizard'>
    <file name='index.html'>
      <cui-wizard>
        <cui-wizard-step cui-title="Introduction">
          <h1>Start the interactive wizard</h1>
          <p>Here we will demonstrate the onNext function.</p>
          <p><strong>We strongly recommend that you DO NOT use this pattern if at all possible; it's much more difficult to show if the step is valid or not to the user!</strong></p>
        </cui-wizard-step>
        <cui-wizard-step cui-title='First' on-next='first.fn()'>
          <p>A function that returns a Promise</p>
          <cui-radio ng-model='first.value' value='resolve'>Resolve</cui-radio> <cui-radio ng-model='first.value' value='reject'>Reject</cui-radio>
        </cui-wizard-step>
        <cui-wizard-step cui-title='Second' on-next='second.fn()' on-enter='stepEntered("Second")'>
          <p>A function that returns a Promise</p>
          <cui-radio ng-model='second.value' value='resolve'>Resolve</cui-radio> <cui-radio ng-model='second.value' value='reject'>Reject</cui-radio>
        </cui-wizard-step>
        <cui-wizard-step cui-title='Third' on-next='third.fn()'>
          <p>A function that returns a Promise</p>
          <cui-radio ng-model='third.value' value='resolve'>Resolve</cui-radio> <cui-radio ng-model='third.value' value='reject'>Reject</cui-radio>
        </cui-wizard-step>
        <cui-wizard-step cui-title='Finish'>
          Finish your wizard.
        </cui-wizard-step>
      </cui-wizard>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $q, $timeout) {
          $scope.first = { value: 'resolve' };
          $scope.second = { value: 'resolve' };
          $scope.third = { value: 'resolve' };
  
          var timeoutFn = function(number, ms) {
            return function() {
              var deferred = $q.defer();
              $timeout(function() {
                deferred[$scope[number].value]();
              }, ms);
              return deferred.promise;
            }
          }
          $scope.$on('cui:wizard:stepChanged', function(e, old, ne) {
            console.log(old, ne);
          });
  
          $scope.first.fn = timeoutFn('first', 1000);
          $scope.second.fn = timeoutFn('second', 2000);
          $scope.third.fn = timeoutFn('third', 3000);
  
          $scope.stepEntered = function(step) {
            console.log('Entered step ' + step)
          }
        });
    </file>
  </example>
  
    @example
    <h3>Wizard in a modal</h3>
  
  <example name='wizardInModal'>
    <file name='index.html'>
      <cui-button ng-click='open()' cui-type='primary'>Open wizard in modal</cui-button>
    </file>
    <file name='wizard.html'>
      <cui-wizard edit-mode='editMode'>
        <cui-wizard-step cui-title="Welcome, this is a really long title for a wizard step.">
          <h1>Getting Started</h1>
          <p>The purpose of this wizard is the configure the basic settings required to begin using Active System Manager.</p>
          <h2>Before you begin, please gather the following information:</h2>
          <ul>
            <li>Time zone where the virtual appliance that hosts Active System Manager is installed</li>
            <li>(Optional) IP addresses of up to two NTP servers</li>
            <li>(Optional): IP address, port, and credentials of a proxy server</li>
            <li>Local network share where the Active Ststem Manager license is stored</li>
          </ul>
          <p>After starting the wizard, you can leave and come back at any time, until you complete the wizard by clicking Finish on the final page. Clicking Finish restarts the Active System Manager virtual appliance and applies time zone settings.</p>
          <p>To change basic settings after initial setup is complete, click Settings in the left menu.</p>
        </cui-wizard-step>
        <cui-wizard-step cui-title="ToS" invalid='!wiz.acceptedTerms'>
          <h1>Terms of Service</h1>
          <p><strong>You must accept the following terms before continuing.</strong></p>
          <div ng-include='"terms.html"'></div>
          <cui-checkbox name='agreement' ng-model='wiz.acceptedTerms'>I agree to the terms in this document</cui-checkbox>
        </cui-wizard-step>
      <form name="form" ng-submit='next(form.$invalid)'>
        <cui-wizard-step cui-title="Register an account" invalid='form.$invalid'>
          <h1>Continuing</h1>
          <cui-textbox ng-model='user.email' type='email' name='email' placeholder='Your email' ng-required='true'></cui-textbox>
          <p>Password: <cui-textbox ng-model='user.password' type='password' name='password' ng-minlength="8" ng-required='true'></cui-textbox></p>
          <p>Press enter when done, or click next.</p>
          <input type="submit" style="position: absolute; left: -9999px; width: 1px; height: 1px;"/>
        </cui-wizard-step>
      </form>
        <cui-wizard-step cui-title="More steps 2">
          <p>Even more steps!!</p>
        </cui-wizard-step>
        <cui-wizard-step cui-title="More steps 3">
          <p>Even more steps!!</p>
        </cui-wizard-step>
      </cui-wizard>
    </file>
    <file name='terms.html'>
      <p class="para">These terms and conditions ("Terms") apply to all Products, Software and/or Services purchased by or on behalf of customer (the legal person who agrees to buy the Products, Software and/or Services from Dell and is also identified in Dell's quotation or invoice) ("Customer") direct from Dell India Private Limited having its registered office at DivyaShree Greens, Ground Floor, S.No 12/1, 12/2A, 13/1A, Chalangatta Village, VarthurHobli, Bangalore South - 560071, Karnataka, India ("Dell") for its internal use only , not for resale purposes and to the exclusion of all other terms and conditions. It is clarified that these Terms do not apply to Products, Software and Services purchased for resale by the Customer, and the terms set out on Dell's website at <a href="http://www.dell.com\ap">www.dell.com\ap</a> [select: Terms and Conditions for India Resellers] are applicable for such purchases.</p><p class="para">These Terms together with Dell's Order Documents and Service Documents (as defined below) form a legally binding contract between Customer and Dell for the purchase of Products, Software and Services <b>("the Agreement")</b> . </p><p class="para">If Customer and Dell have a separate agreement in place for the purchase of Products, Software or Services, that agreement, along with relevant Order Documents and Service Documents (if any) shall apply instead of these Terms.  The Customer acknowledges that it is aware of the contents of and agrees to be bound by these Terms. Neither Dell's acknowledgment of a purchase order nor it's failure to object to conflicting, different, or additional terms and conditions in a purchase order shall be deemed an acceptance of such terms and conditions or a waiver of the provisions hereof. The Agreement shall not be deemed to have come into existence until the Customer's order has been accepted by Dell either by way of the order confirmation and/or the invoice sent by Dell to Customer which describes the Products, Software and/or Services purchased by Customer under the Agreement. The Products / Software sold and/or Services rendered are subject to the Agreement to the exclusion of any other terms and conditions stipulated or referred to by Customer, unless expressly agreed and accepted by Dell in writing. The Agreement documents will apply in following order of priority: (1) Order Documents; (2) Service Documents and (3) these Terms.</p><p class="title_level1">2. Definitions</p><p class="para">(i)  <b>"Confidential Information"</b>  means collectively information of the disclosing party that is not generally known to the public, such as software, product plans, pricing, marketing and sales information, customer lists, "know-how," or trade secrets, which may be designated as confidential or which, under the circumstances surrounding disclosure, ought to be treated as confidential. </p><p class="para">(ii) <b>"Deliverables"</b>  means tangible and intangible materials including reports, studies, base cases, drawings, findings, manuals, procedures and recommendations prepared by Dell or its suppliers, partners, sub-contractors, licensors in the course of providing the Services. </p><p class="para">(iii) <b>"Dell-branded"</b>  means  information technology hardware, software and related products and services which are marked with the Dell logo or sold under the Dell brand and components thereof, excluding (1) Third Party Products and (2) any parts or components added after delivery of the Products or through Dell's custom factory integration service.</p><p class="para">(iv)  <b>"Intellectual Property Rights"</b>  means any patent, copyright, database right, moral right, design right, registered design, trade mark, service mark, domain name, metatag, utility model, unregistered design or, where relevant, any application for any such right, or other industrial or intellectual property right subsisting anywhere in the world and any intellectual property rights in know-how, documentation, and techniques associated with the Deliverables or Materials. </p><p class="para">(v) <b>"Materials"</b>  means all content and other items included with or as part of the Products, Services, Software, or Deliverables, such as text, graphics, logos, button icons, images, audio clips, information, data, photographs, graphs, videos, typefaces,  music, sounds, and software.</p><p class="para">(vi) <b>"Order Documents"</b>  means the quotation and/or the order confirmation and/or the invoice sent by Dell to Customer which describes the Products, Software and/or Services purchased by Customer under the Agreement as well as prices, payment terms and other provisions. </p><p class="para">(vii)<b>"Products"</b>  means computer hardware and related products supplied by Dell under the Agreement.</p><p class="para">(viii)  <b>"Services"</b>  means the services provided by Dell as described in any Service Documents.</p><p class="para">(ix) <b>"Service Descriptions"</b>  means descriptions of services found at <a  href="http://www.dell.com/Learn/us/en/19/solutions/service-contracts-for-smb-ple-pub?c=us&l=en&s=dhs&cs=19&delphi:gr=true" class="lnk" target="pop-up">www.dell.com/servicecontracts</a><a href="javascript:void(0)"><sup tn="servicecontracts">*</sup></a>.</p><p class="para">(x)  <b>"Service Documents"</b>  means the Service Descriptions available at <a  href="http://www.dell.com/Learn/us/en/19/solutions/service-contracts-for-smb-ple-pub?c=us&l=en&s=dhs&cs=19&delphi:gr=true" class="lnk" target="pop-up">www.Dell.com/ServiceContracts</a>, Statements of Work and any other mutually agreed documents describing Services, Software or Deliverables.</p><p class="para">(xi) <b>"Software"</b>  means any software, library, utility, tool, or other computer or program code, in object (binary) or source-code form, as well as related documentation, provided by Dell to Customer.  Software includes software (1) provided by Dell and locally installed on Customer's hardware and/or equipment or (2) made available by Dell and accessed by Customer through the internet or other remote means (such as websites, portals, and "cloud-based" solutions).</p><p class="para">(xii)<b>"Statement of Work"</b>  means any mutually agreed statement of work describing specific Services and/or Deliverables as agreed between Customer and Dell.</p><p class="para">(xiii)  <b>"Third Party Products"</b>  means any non-Dell-branded products, software, or services.</p><p class="title_level1">3.  Ordering</p><p class="para"><b>3.1</b>  Any quotes issued by Dell shall be valid for 15 days unless stated otherwise in the quote.</p><p class="para"><b>3.2</b>  Prices for the Products, Software and/or Services shall be stated in the Order Documents or Service Documents issued by Dell. Where deliveries occur in instalments or phases Dell may need to adjust prices for Products, Software or Services due to changes in exchange rates, taxes, duties, freight, levies and purchase costs. Quotes provided by Dell exclude value added tax and any other taxes, levies, and shipping charges unless expressly set out in the quote. Such charges are payable by Customer in addition to the prices quoted and may appear as separate items on the Order Documents.</p><p class="para"><b>3.3</b>  Payment for Products, Software or Services must be received by Dell as agreed in writing, within the time period noted on the Order Documents, or if not noted, within 30 days from the date of the invoice. Time for payment shall be of the essence.  Customer's payment terms are subject to credit checking by Dell. Dell shall be entitled to charge interest on any overdue amounts (computed from the due date to the date of actual payment) at a rate of the lesser of (a) one and half percent (1.5%) per month; or (b) maximum rate permitted by law. If any sum due from the Customer to Dell under the Agreement is not paid by the due date for payment then (without prejudice to any other right or remedy available to Dell), Dell shall be entitled to cancel or suspend its performance of the Agreement or any order including suspending deliveries of the Products and/or Software and suspending provision of the Services until arrangements as to payment or credit have been established which are satisfactory to Dell. Dell may invoice parts of an order separately. Unless credit terms have been expressly agreed by Dell or indicated in the Agreement documents, payment for the Products, Software or Services including applicable taxes shall be made in full before physical delivery of Products/Software or commencement of Services.</p><p class="para"><b>3.4</b>  All payments made or to be made by Customer to Dell under the Agreement shall be made free of any restriction or condition and without any deduction or withholding (except to the extent required by law) on account of any other amount, whether by way of set-off or otherwise.</p><p class="title_level1">4.  Changes to Products, Software or Services </p><p class="para"><b>4.1</b>  Changes in a Product, Software, or Service may occur after a Customer places an order but before Dell ships the Product or Software or performs the Service, and the Products, Software or Services the Customer receives might display minor differences from the Products, Software, or Services Customer orders but they will meet or exceed all material functionality and performance of the Products, Software or Services that were originally ordered. </p><p class="para"><b>4.2</b>   Dell may revise and/or discontinue a Product, Software, or Service at any time without notice as part of Dell's policy of on-going a Product, Software, or Service up-date and revision. Any revised or updated Product, Software, or Service will usually have the core functionality and performance of the Product, Software, or Service ordered unless otherwise intimated by Dell. The Customer accepts that Dell's policy may result in differences between the specification of a Product, Software, or Service delivered to the Customer and the specification of a Product, Software, or Service ordered. There may be occasions when Dell confirms orders but learns that it cannot supply the ordered Product, Software, or Service, either at all or in the quantities ordered such as when the a Product, Software, or Service no longer are being manufactured/provided or they otherwise become unavailable to Dell, or when Dell cannot source components for the configuration ordered, or when there is a pricing error etc. In such circumstances, Dell will contact the Customer to inform them about alternative Product, Software, or Service that might meet the Customer's needs. However, if the Customer does not wish to order alternative Product, Software, or Service, Dell will cancel the order for a Product, Software, or Service that it cannot supply and will refund the purchase/order price paid by the Customer. </p><p class="title_level1">5. Products</p><p class="para"><b>5.1</b>  Dell shall deliver the Products to Customer's location as set out in the Order Documents, and the purchase of the Products shall be subject to the terms as per the Agreement. Delivery dates are non-binding and time for delivery shall not be of the essence.  If no delivery dates are specified, delivery shall be within a reasonable time. Delivery of Products may be made in instalments. Where the Products are so delivered by instalments, each instalment shall be deemed to be the subject of a separate contract and no default or failure by Dell in respect of any one or more instalments shall vitiate the contract in respect of Products previously delivered or undelivered Products. Dell shall not be liable for any loss (including loss of profits), costs, damages, charges or expenses caused directly or indirectly by any delay in the delivery of the Products (even if caused by Dell's negligence), nor will any delay entitle Customer to terminate or rescind the Agreement.  Dell shall only be liable for any non-delivery of Products if Customer gives written notice to Dell within 7 days of the date when the Products would, in the ordinary course of events, have been delivered.</p><p class="para"><b>5.2</b>  The title to and risk in the Products shall pass to the Customer or its representative upon delivery of the Product to the Customer or its representative. Notwithstanding any such delivery, Dell shall be entitled to maintain an action against Customer for any unpaid price of the Products (without prejudice to any other right or remedy available to Dell either in law or under this Agreement).</p><p class="para"><b>5.3</b>  Customer shall inspect the Products to identify any missing, wrong or damaged Products or packaging, and notify Dell (as per the contact details stated in Dell's invoice) of any such issues which would be apparent on reasonable inspection and testing of the Products within 7 days of the date of delivery of the Products. If Customer does not comply with the notification requirements in this clause, Customer shall not be entitled to reject the Products; Dell shall have no liability for such defect or failure, and Customer shall be bound to pay for the Products as if they had been delivered in accordance with the Agreement. Based on the Customer's notification, Dell's representative will contact the Customer as regards the further course of action proposed to be adopted by Dell, which may at Dell's discretion, extend to repair or replacement of Products rejected in accordance with this clause. </p><p class="para"><b>5.4</b>  If for any reason Customer does not accept delivery of any of the Products when they are ready for delivery, or Dell is unable to deliver the Products on time because Customer has not provided appropriate instructions, documents, licences or authorisations then the Products will be deemed to have been delivered, risk passing to Customer (including for loss or damage caused by Dell's negligence) and Dell may:  <br />&nbsp;&nbsp;&nbsp;<b>5.4.1</b>   store the Products until actual delivery and Customer shall be liable for all related costs and expenses (including without limitation storage and insurance); or  <br />&nbsp;&nbsp;&nbsp;<b>5.4.2</b>   sell the Products at the best price readily obtainable and (after deduction of all reasonable storage and selling expenses) charge Customer for any shortfall below the Price for the Products. <br /></p><p class="para"><b>5.5</b>  Except as agreed to in writing between Customer and Dell, Third Party Products shall be exclusively subject to terms and conditions between the third party and Customer.</p><p class="title_level1">6. Services, Software provided by Dell in connection with the provision of the Services & Deliverables </p><p class="para">The below provisions in clauses 6, 7 and 8 shall be applicable if the Order Document reflects that the Customer has availed of Software and Services from Dell by paying the applicable charges for the same.</p><p class="para"><b>6.1</b>   Dell shall provide Services, Software, or Deliverables to Customer in accordance with the Service Documents and the other applicable terms of the Agreement. Dell may, at its option, propose to renew the Service and the software licence by sending Customer an invoice or, subject to prior notification, continuing to perform the Service or make the Software available to Customer.  Customer is deemed to have agreed to such renewal of the Service and software licence by paying such invoice by its due date or by continuing to order Services or use the Software. In case of such renewal, the Service Documents (or the Service Descriptions as the case may be (available at <a  href="http://www.dell.com/Learn/us/en/19/solutions/service-contracts-for-smb-ple-pub?c=us&l=en&s=dhs&cs=19&delphi:gr=true" class="lnk" target="pop-up">www.Dell.com/ServiceContracts</a>) and other relevant terms of the Agreement as on the date of payment of the invoice or continuation of performing the service or making available the Software, shall be applicable.</p><p class="para"><b>6.2</b>  All Intellectual Property Rights embodied in the Materials and Deliverables, including the methods by which the Services are performed and the processes that make up the Services, shall belong solely and exclusively to Dell, its suppliers or its licensors except as expressly granted in the Agreement. The Materials are protected by copyright laws and international copyright treaties, as well as other intellectual property laws and treaties. You may not modify, remove, delete, augment, add to, publish, transmit, adapt, translate, participate in the transfer or sale of, create derivative works from, or in any way exploit any of the Materials or Deliverables, in whole or in part.</p><p class="para"><b>6.3</b>  Subject to payment in full for the applicable Services, Dell grants Customer a non-exclusive, non-transferable, royalty-free right to use the Materials and Deliverables solely (1) in the country or countries in which Dell delivers the Services, (2) for its internal use, and (3) as necessary for Customer to enjoy the benefit of the Services as stated in the applicable Service Documents. </p><p class="para"><b>6.4</b>   Dell may cancel or suspend its performance of the Services or Customer's access or any user access to the Software provided by Dell in connection with the provision of the Services where Dell is required to do so (1) by law, (2) by order of a court of competent jurisdiction, or (3) when Dell has reasonable grounds to believe that Customer (or Customer's users) are involved in any fraudulent or other illegal activities in connection with the Agreement.</p><p class="para"><b>6.5</b>   It may be necessary for Dell to carry out scheduled or unscheduled repairs or maintenance, or remote patching or upgrading of the Software provided by Dell in connection with the provision of the Services and which is installed on Customer's computer system(s) <b>("Maintenance")</b> , which may temporarily degrade the quality of the Services or result in a partial or complete outage of the Software.  Any degradation or interruption in the Software or Services during such Maintenance shall not give rise to a refund or credit of any fees paid by Customer or any other liability on Dell. </p><p class="para"><b>6.6</b>   Customer agrees that the operation and availability of the systems used for accessing and interacting with the Software provided by Dell in connection with the provision of the Services (including telephone, computer networks and the internet) or to transmit information can be unpredictable and may from time to time interfere with or prevent access to, use or operation of the Software. Dell shall not be liable for any such interference with or prevention of Customer's access to, use or lack of operation of the Software. </p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiModal) {
          var wizardModal = cuiModal({
            type: 'bare',
            templateUrl: 'wizard.html',
            controller: 'WizardCtrl',
            visible: true,
            static: true
          })
          $scope.open = wizardModal.modal.show;
        })
        .controller('WizardCtrl', function($scope, cuiLoading, cuiDialog, $q, $timeout) {
          $scope.wiz = {
            acceptedTerms: false
          };
          $scope.editMode = false;
          $scope.$on('cui:wizard:finished', function() {
            var deferred = $q.defer();
            deferred.promise.then(function() {
              cuiDialog({
                title: 'Success',
                message: 'Done mock submitting data'
              })();
              $scope.editMode = true;
            });
            cuiLoading(deferred.promise)
            $timeout(function() {
              deferred.resolve();
            }, 1000);
          });
          $scope.next = function(invalid) {
            if (!invalid) {
              $scope.$broadcast('cui:wizard:next');
            }
          }
          $scope.$on('cui:wizard:cancelled', function() {
            $scope.modal.hide().then(function() {
              $scope.$broadcast('cui:wizard:first');
            });
          })
        });
    </file>
    <file name='styles.css'>
    body {
      height: 800px;
    }
    </file>
  </example>
   */

  module = angular.module('cui.modules.wizard', ['cui.base']);

  module.directive('cuiWizard', function(baseTemplatePath, $q, cuiLoading) {
    return {
      templateUrl: "" + baseTemplatePath + "wizard.html",
      restrict: 'E',
      require: '^?cuiModal',
      transclude: true,
      scope: {
        editMode: '=?',
        onFinish: '&',
        onCancel: '&',
        currentStep: '=?',
        currentStepIndex: '=?'
      },
      controller: function($scope, $element) {
        var canJumpToStep, runOnNext, steps;
        if ($scope.editMode == null) {
          $scope.editMode = false;
        }
        $scope.currentStepIndex = 0;
        steps = this.steps = $scope.steps = [];
        this.updateSteps = (function(_this) {
          return function() {
            var newStep, oldStep, scope, step, stepElements, _i, _len;
            oldStep = steps[$scope.currentStepIndex];
            stepElements = $element[0].querySelectorAll('cui-wizard-step');
            steps = _this.steps = $scope.steps = [];
            for (_i = 0, _len = stepElements.length; _i < _len; _i++) {
              step = stepElements[_i];
              scope = angular.element(step).isolateScope();
              if (scope != null) {
                steps.push(scope);
              }
            }
            if ($scope.currentStepIndex >= steps.length) {
              $scope.currentStepIndex = steps.length - 1;
              if ($scope.currentStepIndex === -1) {
                $scope.currentStepIndex = 0;
              }
            }
            newStep = steps[$scope.currentStepIndex];
            if (oldStep !== newStep) {
              _this.updateStepShown(newStep, oldStep, $scope.currentStepIndex);
            }
          };
        })(this);
        this.updateStepShown = function(newStep, oldStep, oldStepIndex) {
          if (newStep != null) {
            newStep.touched = true;
          }
          $scope.currentStep = (newStep != null ? newStep.cuiTitle : void 0) || (newStep != null ? newStep.title : void 0);
          if (newStep != null) {
            newStep.onEnter();
          }
          return $scope.$emit('cui:wizard:stepChanged', {
            index: $scope.currentStepIndex,
            title: (newStep != null ? newStep.cuiTitle : void 0) || (newStep != null ? newStep.title : void 0)
          }, {
            index: oldStepIndex,
            title: (oldStep != null ? oldStep.cuiTitle : void 0) || (oldStep != null ? oldStep.title : void 0)
          });
        };
        this.onStep = function(step) {
          return steps[$scope.currentStepIndex] === step;
        };
        canJumpToStep = function(index) {
          return $scope.editMode || index <= $scope.currentStepIndex + 1;
        };
        this.updateForwards = function() {
          var i, invalidStep, step, _i, _len;
          invalidStep = -1;
          for (i = _i = 0, _len = steps.length; _i < _len; i = ++_i) {
            step = steps[i];
            if (step.invalid && invalidStep === -1) {
              invalidStep = i;
            }
            if (i < $scope.currentStepIndex) {
              step.forward = true;
            } else if (invalidStep !== -1 && invalidStep !== i) {
              step.forward = false;
            } else {
              step.forward = true;
            }
          }
        };
        $scope.$watch('currentStepIndex', this.updateForwards);
        runOnNext = function(cb) {
          var onNext;
          onNext = steps[$scope.currentStepIndex].onNext;
          steps[$scope.currentStepIndex].onNextFailed = false;
          if (steps[$scope.currentStepIndex].onNextExists()) {
            onNext = onNext();
            cuiLoading(onNext);
            return $q.when(onNext).then(function() {
              return cb();
            }, function() {
              return steps[$scope.currentStepIndex].onNextFailed = true;
            });
          } else {
            return cb();
          }
        };
        $scope.isStepDisabled = function(index) {
          return !canJumpToStep(index) || !steps[index].forward;
        };
        $scope.canFinish = function() {
          var _ref, _ref1, _ref2;
          if ($scope.editMode) {
            return ((_ref = steps[steps.length - 1]) != null ? _ref.forward : void 0) && !((_ref1 = steps[steps.length - 1]) != null ? _ref1.invalid : void 0);
          } else {
            return !((_ref2 = steps[$scope.currentStepIndex]) != null ? _ref2.invalid : void 0) && $scope.currentStepIndex === steps.length - 1;
          }
        };
        $scope.selectStepIndex = (function(_this) {
          return function(index) {
            if (canJumpToStep(index) && steps[index].forward) {
              if (index > $scope.currentStepIndex) {
                return runOnNext(function() {
                  return _this.goto(index);
                });
              } else {
                return _this.goto(index);
              }
            }
          };
        })(this);
        this.goto = (function(_this) {
          return function(identifier) {
            var i, old, step, _i, _len;
            old = $scope.currentStepIndex;
            if (angular.isString(identifier)) {
              for (i = _i = 0, _len = steps.length; _i < _len; i = ++_i) {
                step = steps[i];
                if (step.title === identifier) {
                  $scope.currentStepIndex = i;
                  return;
                }
              }
              throw new Error("cuiWizard: Couldn't find step '" + identifier + "'");
            } else if (angular.isNumber(identifier)) {
              if (identifier < 0 || identifier >= steps.length) {
                throw new Error("cuiWizard: Step " + identifier + " not in index range 0-" + (steps.length - 1));
              }
              $scope.currentStepIndex = identifier;
            } else {
              throw new Error("cuiWizard: Identifier not valid");
            }
            return _this.updateStepShown(steps[$scope.currentStepIndex], steps[old], old);
          };
        })(this);
        $scope.$on('cui:wizard:goto', (function(_this) {
          return function(e, identifier) {
            return _this.goto(identifier);
          };
        })(this));
        $scope.goto = this.goto;
        this.previous = (function(_this) {
          return function() {
            if ($scope.currentStepIndex <= 0) {
              throw new Error('cuiWizard: At beginning of wizard steps');
            }
            $scope.currentStepIndex--;
            return _this.updateStepShown(steps[$scope.currentStepIndex], steps[$scope.currentStepIndex + 1], $scope.currentStepIndex + 1);
          };
        })(this);
        $scope.$on('cui:wizard:previous', this.previous);
        $scope.previous = this.previous;
        this.next = (function(_this) {
          return function() {
            if ($scope.currentStepIndex >= steps.length - 1) {
              throw new Error('cuiWizard: At end of wizard steps');
            }
            $scope.currentStepIndex++;
            return _this.updateStepShown(steps[$scope.currentStepIndex], steps[$scope.currentStepIndex - 1], $scope.currentStepIndex - 1);
          };
        })(this);
        $scope.$on('cui:wizard:next', this.next);
        $scope.next = (function(_this) {
          return function() {
            return runOnNext(_this.next);
          };
        })(this);
        this.first = (function(_this) {
          return function() {
            var old;
            old = $scope.currentStepIndex;
            $scope.currentStepIndex = 0;
            return _this.updateStepShown(steps[$scope.currentStepIndex], steps[old], old);
          };
        })(this);
        $scope.$on('cui:wizard:first', this.first);
        this.last = (function(_this) {
          return function() {
            var old;
            old = $scope.currentStepIndex;
            $scope.currentStepIndex = steps.length - 1;
            return _this.updateStepShown(steps[$scope.currentStepIndex], steps[old], old);
          };
        })(this);
        $scope.$on('cui:wizard:last', this.last);
        this.cancel = function(e) {
          $scope.onCancel({
            $event: e
          });
          return $scope.$emit('cui:wizard:cancelled', e);
        };
        $scope.$on('cui:wizard:cancel', this.cancel);
        $scope.cancel = this.cancel;
        this.finish = function(e) {
          $scope.onFinish({
            $event: e
          });
          return $scope.$emit('cui:wizard:finished', e);
        };
        $scope.$on('cui:wizard:finish', this.finish);
        $scope.finish = this.finish;
        return this;
      },
      link: function(scope, element, attrs, modalCtrl) {
        return modalCtrl != null ? modalCtrl.addClass('cui-wizard-modal-wrapper') : void 0;
      }
    };
  });


  /**
    @ngdoc directive
    @name wizardStep
  
    @description The wizard steps are transcluded by the wizard.
  
    @module cui.modules
    @src modules/wizard/wizard.coffee
    @directive cui-wizard-step
    @controlType navigation
    @restrict E
  
    @transclude The content of the step
  
    @param {string} cuiTitle The title of the step (to be displayed on the left hand side of the wizard).
  
  Note: This parameter was renamed from `title` to `cuiTitle` to prevent conflicts with the HTML5 `title` global attribute. Backwards compatibility will be removed in CUI 2.7.0.
  
    @param {boolean=} invalid Default `false`. If `true`, the user will not be allowed to continue past this step. For use cases with the wizard, check out the wizard examples.
  
    @param {expression=} onNext A function that will return a promise. It should resolve to allow the user to continue to the next step.
  
  __The `onNext` property should be used sparingly!__ Only use it where you must have an asynchronous request before allowing the user to continue to the next step. When you must use `onNext`, we recommend you use it with `invalid` for client-side validation before allowing the user to attempt to send data to the server.
  
    @param {expression=} onEnter A function to execute when the step is entered.
   */

  module.directive('cuiWizardStep', function(baseTemplatePath, $timeout) {
    return {
      templateUrl: "" + baseTemplatePath + "wizardStep.html",
      restrict: 'E',
      transclude: true,
      require: '^cuiWizard',
      scope: {
        title: '@',
        cuiTitle: '@',
        invalid: '=?',
        onNext: '&',
        onEnter: '&'
      },
      link: function(scope, element, attrs, cuiWizard) {
        if (scope.invalid == null) {
          scope.invalid = false;
        }
        scope.touched = false;
        scope.onNextExists = function() {
          return angular.isDefined(attrs.onNext);
        };
        scope.isCurrentStep = function() {
          return cuiWizard.onStep(scope);
        };
        cuiWizard.updateSteps();
        scope.$on('$destroy', function() {
          return $timeout(function() {
            return cuiWizard.updateSteps();
          });
        });
        return scope.$watch('invalid', function() {
          return cuiWizard.updateForwards();
        });
      }
    };
  });


  /**
    @ngdoc service
    @name alertService
    @module cui.services
    @src services/alertService/alertService.coffee
    @controlType presentational
  
    @description The alert service provides an easy way to add alerts to the application frame without touching any HTML.
  
  ```javascript
  angular.module('app', ['cui'])
    .controller('AppCtrl', function($scope, cuiAlertService) {
      cuiAlertService('Welcome to CUI!');
  });
  ```
  
  <h3>Specify alert type</h3>
  The alert service provides the following shorthand methods as properties of the cuiAlertService: `danger`, `warning`, `unknown`, and `success`.
  
  ```javascript
  cuiAlertService.success("Perfect, you're all good to go!");
  ```
  
  <h3>Specify alert location</h3>
  By default, the alert service pushes new alerts to the application frame. You can specify the ID of an element to push the alert(s) to, or flush them from.
  
  To do this, pass the ID as the second parameter as a `string`, or as the `location` property on the configuration object.
  
  ```javascript
  cuiAlertService('In a custom area', 'custom-area');
  ```
  ```html
  <div id='custom-area'></div>
  ```
  
  <h3>Further customization</h3>
  The alert service also accepts a configuration object:
  
  ```javascript
  cuiAlertService({
    label: 'Woah there',
    content: 'Something bad occurred.',
    type: 'danger',
    icon: 'flag',
    location: 'alert-service-el-id'
    destroyAfter: 2000,
    dismissable: false
  });
  ```
  
  <h3>Flushing alerts</h3>
  To remove all alerts, simply call `cuiAlertService.flush()`, with, optionally, the first param being the ID of the alert service element location.
  
  > If there are more than three alerts on the page, the alert service will automatically remove the oldest.
  
  @returns {promise} The service returns a `promise` that resolves with the parameter `id` (from the config object). The `id` can be of any type (object, function, string, number, boolean).
  
  ```javascript
  cuiAlertService({content: 'Hello, world!', id: 500}).then(function(id) {
    // Alert has been closed. Maybe do something?
  });
  ```
  
    @example
    <h3>Pushing alerts</h3>
    Lorem ipsum is included to demonstrate how even if not at the top of the page, the alerts stay pinned to the masthead (always in view).
  <example name='alertService'>
    <file name='index.html'>
      <cui-application-frame application-name="Alert Service"
                             application-subname="on Application Frame">
        <h1>Hello, world!</h1>
        <p>Content (HTML accepted):</p>
        <cui-textarea ng-model="content">Hello, there! Thanks for trying out the alert service.</cui-textarea>
        <cui-checkbox ng-model="expire">Expire after three seconds?</cui-checkbox>
        <cui-button ng-click="alert('primary')" cui-type="primary">Push default alert</cui-button>
        <cui-button ng-click="alert('danger')" cui-type="danger">Push danger alert</cui-button>
        <cui-button ng-click="alert('success')" cui-type="success">Push success alert</cui-button>
        <cui-button ng-click="alert('warning')" cui-type="warning">Push warning alert</cui-button>
        <cui-button ng-click="openModal()">Open modal</cui-button>
        <br><br>
        <cui-button ng-click="flush()">Remove all alerts</cui-button>
        
        <div ng-controller='LoremCtrl'>
          <p ng-repeat='lorem in ipsum'>{{lorem}}</p>
        </div>
      </cui-application-frame>
    </file>
    <file name='modal.html'>
      <div id='alert-service-modal'></div>
      <cui-button ng-click="alert('danger', _alertLoc)" cui-type="danger">Push danger alert</cui-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiAlertService, cuiModal) {
          var id = 0;
          $scope.alert = function(type, location) {
            cuiAlertService({
              content: $scope.content,
              location: location,
              id: id++,
              type: type,
              destroyAfter: $scope.expire ? 3000 : undefined
            }).then(function(id) {
              console.log(id);
            })
          }
          $scope.flush = function() {
            cuiAlertService.flush()
          }
          var m = cuiModal({
            templateUrl: 'modal.html',
            controller: 'ModalCtrl',
            scope: $scope
          });
          $scope.openModal = m.modal.show;
        })
        .controller('ModalCtrl', function($scope) {
          $scope._alertLoc = 'alert-service-modal';
        })
        .controller('LoremCtrl', function($scope, $http, cuiAlertService, cuiLoading) {
          cuiLoading($http.get('http://baconipsum.com/api/?type=meat-and-filler').then(function success(response) {
            $scope.ipsum = response.data;
          }, function failure(err) {
            cuiAlertService(err);
          }));
        });
    </file>
    <file name='styles.css'>
      body {
        height: 500px;
      }
    </file>
  </example>
   */

  module = angular.module('cui.services.alertService', ['cui.base']);

  module.factory('cuiAlertService', function($document, $timeout, $q, $rootScope, $sce, $compile, $templateCache, baseTemplatePath) {
    var alertService, getAlertsLocation, getDocument, setAlerts;
    getDocument = function(args, type) {
      var deferred;
      deferred = $q.defer();
      $timeout((function() {
        return setAlerts(args, type, deferred);
      }), 0);
      return deferred.promise;
    };
    alertService = function() {
      return getDocument(arguments);
    };
    alertService.danger = function() {
      return getDocument(arguments, 'danger');
    };
    alertService.warning = function() {
      return getDocument(arguments, 'warning');
    };
    alertService.unknown = function() {
      return getDocument(arguments, 'unknown');
    };
    alertService.success = function() {
      return getDocument(arguments, 'success');
    };
    alertService.flush = function(id) {
      var elLocation;
      elLocation = getAlertsLocation(id);
      return angular.forEach(elLocation.children(), function(child) {
        return angular.element(child).isolateScope().$broadcast('cui:alert:dismiss');
      });
    };
    getAlertsLocation = function(id) {
      var alertPlaceholder;
      if (id == null) {
        id = 'cui-application-frame-alert-service';
      }
      alertPlaceholder = angular.element(document.getElementById(id));
      if (alertPlaceholder.length !== 1) {
        throw new Error("Couldn't find alert service location with id '" + id + "'");
      }
      return alertPlaceholder;
    };
    setAlerts = function(args, type, deferred) {
      var alert, el, elContent, elLocation, scope;
      alert = {
        type: type
      };
      if (angular.isString(args[0])) {
        alert.content = args[0];
        if (angular.isObject(args[1])) {
          alert = angular.extend(alert, args[1]);
        } else if (angular.isString(args[1])) {
          alert.location = args[1];
          if (args[2] != null) {
            alert = angular.extend(alert, args[2]);
          }
        }
      } else if (angular.isObject(args[0])) {
        alert = args[0];
      }
      elContent = $templateCache.get("" + baseTemplatePath + "alertService.html");
      el = angular.element(elContent);
      scope = $rootScope.$new();
      scope.content = $sce.trustAsHtml(alert.content);
      scope.dismissable = alert.dismissable;
      scope.label = alert.label;
      scope.id = alert.id;
      scope.type = alert.type;
      scope.icon = alert.icon;
      scope.dismissable = alert.dismissable;
      scope.destroyAfter = alert.destroyAfter;
      el = $compile(el)(scope);
      elLocation = getAlertsLocation(alert.location);
      elLocation.append(el);
      if (elLocation.children().length > 3) {
        angular.element(elLocation.children()[0]).isolateScope().$broadcast('cui:alert:dismiss');
      }
      return scope.$on('cui:alert:dismissed', function() {
        return deferred.resolve(alert.id);
      });
    };
    return alertService;
  });


  /**
    @ngdoc service
    @name dataSourceService
    @src services/dataSource/dataSource.coffee
    @controlType tabular
    @description
  
  The DataSourceService is a thin wrapper around Angular's [ng.$http service](http://docs.angularjs.org/api/ng.$http). It provides a function which takes as it's first parameter an API endpoint upon which queries can be executed.
  
  ```javascript
  AppController = function($scope, cuiDataSourceService) {
    Server = cuiDataSourceService('/api/server');
  
    Server.all().then(function(servers) {
      // Do something with your servers
      $scope.servers = servers;
    }, function(err) {
      // An error occured
      $scope.errorMessage = err.message;
    })
  }
  ```
   
    @example
    <h3>Button enabled/disabled</h3>
  <example name='buttonDisabled'>
    <file name='index.html'>
    <cui-button ng-click='query("test.json")'>Call endpoint</cui-button>
    <cui-button ng-click='query("404File.json")'>Call incorrect endpoint</cui-button>
    <p>Response:</p>
    <code>{{response}}</code>
    </file>
    <file name='test.json'>
      {
        "hello": "world"
      }
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiDataSourceService) {
          $scope.query = function(endpoint) {
            cuiDataSourceService(endpoint).all().then(function success(data) {
              $scope.response = data;
            }, function error(err) {
              $scope.response = 'An error occurred.';
            });
          }
        })
    </file>
  </example>
   */


  /**
    @ngdoc method
    @name dataSourceService#
    @description
  
    @param {string} endpoint The endpoint through which the Data Source Service should communicate.
  
    @param {object=} config This object allows you to set default query parameters, headers for the request, and the `id` field of single object.
  
  Setting the `id` will ensure that any call to `save` on an object returned from your server (via `.get(id)`) will perform an HTTP PUT instead of a POST. Only use this if the unique identifier for the object is **not** `id`. Objects returned from the server need to have an id field.
  
  ```javascript
  {
    headers: {
      "x-dell-authentication-key": "94b2fff3e7fd9b9c391a2306"
    },
    params: {
      limit: 20
    },
    id: '_id'
  }
  ```
  
    @returns {DataSource}
  
  The DataSource is an object that represents the API endpoint. It exposes methods for you to perform CRUD operations on your REST API.
  
  <h2>Methods available on a DataSource object</h2>
  <hr>
  
  
  > All of the methods below except <b>create</b> return a <a href='http://www.html5rocks.com/en/tutorials/es6/promises/'>Promise</a>. Check out <a href='http://docs.angularjs.org/api/ng.$q'>ng.$q</a> for more information on promises.
  
  <h3>All</h3>
  
  Retrieve all records from the endpoint.
  
      Server.all().then(function(servers) {
        // Use the servers
      })
      // HTTP GET /api/servers
  
  <h3>Create</h3>
  
  Create a record which is later saved.
  
  <blockquote class='alert'>The create method does not return a promise.</blockquote>
  
      Server.create({
        name: 'PowerEdge 331x',
        location: 'AMER.US.WI.MADISON'
      })
  
      Server.save()
      // HTTP POST /api/servers
  
  <h3>Get</h3>
  
  Get a single item from your API by it's ID.
  
      Server.get(123).then(function(server) {
        // Do something with the server
        server.location = 'EMEA.RU.MOSCOW'
        server.save() // HTTP PUT /api/servers/123
      })
      // HTTP GET /api/servers/123
      
  Retrieving a single item via the get method provides a `save` and `delete` method on the returned object.
  
  <h3>Query</h3>
  
  Provide query parameters on the request to your API.
  
      Server.query({
        limit: 20,
        offset: 100
      }).then(function(servers) {
        // Use the servers
      })
      // HTTP GET /api/servers?limit=20&offset=100
  
  <h2>Methods on single record objects</h2>
  <hr>
  
  Items retrieved using the `get` method have a `save` and `delete` method on them.
  
  <h3>Delete</h3>
  
  The delete method sends an `HTTP DELETE` request to the API Endpoint.
  
      Server.get(123).then(function(server) {
        server.delete().then(function(success) { // HTTP DELETE /api/servers/123
          // The server return a successfully deleted.
        }, function(err) {
          // The server had an error.
        })
      })
  
  <h3>Save</h3>
  
  The save method either sends an `HTTP PUT` or `HTTP POST` depending on if the item object has an id set. (Remember: this id field is configurable by the base `cuiDataSourceService` method).
  
      Server.get(123).then(function(server) {
        server.location = 'AMER.US.TX.RR2'
        server.save().then(function(success) { // HTTP PUT /api/servers/123
          // The server data was saved.
        })
      })
  
  <br/>
  
      server = Server.create({
        name: 'PowerEdge 33x',
        location: 'EMEA.UK.IR.DUBLIN'
      })
  
      server.save()
      //  HTTP POST /api/servers
      //    {
      //      "name": "PowerEdge 33x",
      //      "location": "EMEA.UK.IR.DUBLIN"
      //    }
      //
   */

  module = angular.module('cui.services.dataSource', ['cui.base']);

  module.factory('cuiDataSourceService', function($q, $http, $injector) {
    return function(url, config) {
      var collectionMethods, individualMethods, object, options, performHTTP;
      if (config == null) {
        config = {};
      }
      object = {
        endpoint: url,
        options: {}
      };
      options = {
        headers: {}
      };
      if (config.headers) {
        angular.extend(options.headers, config.headers);
        delete config.headers;
      }
      object.options = angular.extend(options, config);
      performHTTP = function(request) {
        var deferred;
        deferred = $q.defer();
        $http(request).success(function(data, status, headers, config) {
          return deferred.resolve(data);
        }).error(function(err) {
          return deferred.reject(err);
        });
        return deferred.promise;
      };
      individualMethods = {
        save: function() {
          var key, request;
          request = {
            method: this[this.$dataSource.options.id || 'id'] ? 'PUT' : 'POST',
            url: this.$dataSource.endpoint,
            data: {},
            headers: this.$dataSource.options.headers
          };
          for (key in this.$dataSource._originalData) {
            request.data[key] = this[key];
          }
          return performHTTP(request);
        },
        "delete": function() {
          return performHTTP({
            method: 'DELETE',
            url: this.$dataSource.endpoint,
            headers: this.$dataSource.options.headers
          });
        }
      };
      collectionMethods = {
        all: function() {
          return this.query();
        },
        create: function(data) {
          return angular.extend({}, data, individualMethods, {
            $dataSource: {
              _originalData: data,
              options: this.options,
              endpoint: this.endpoint
            }
          });
        },
        get: function(id) {
          var Item, dataSourceService, deferred;
          deferred = $q.defer();
          dataSourceService = $injector.get('cuiDataSourceService');
          Item = dataSourceService("" + this.endpoint + "/" + id, this.options);
          Item.all().then(function(data) {
            return deferred.resolve(angular.extend({}, data, individualMethods, {
              $dataSource: {
                _originalData: data,
                options: Item.options,
                endpoint: Item.endpoint
              }
            }));
          }, function(err) {
            return deferred.reject(err);
          });
          return deferred.promise;
        },
        query: function(params, headers) {
          var request;
          request = {
            method: 'GET',
            url: this.endpoint
          };
          if (params || this.options.params) {
            request.params = angular.extend({}, this.options.params, params);
          }
          if (headers || this.options.headers) {
            request.headers = angular.extend({}, this.options.headers, headers);
          }
          return performHTTP(request);
        }
      };
      return angular.extend({}, object, collectionMethods);
    };
  });


  /**
    @ngdoc service
    @name dialog
  
    @module cui.controls
    @src services/dialog/dialog.coffee
    @controlType presentational
  
    @description The dialog service allows for an easy way to prompt the user. For more advanced usage, see the Modal Service.
  
    @example
    <h3>OK/cancel</h3>
  
  <example name='okcanceldialog'>
    <file name='index.html'>
      <cui-button ng-click="prompt()">Show ok/cancel dialog box</cui-button>
      <p ng-if='status'>Promise was {{status}}.</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiDialog) {
          var dialog = cuiDialog({
            title: 'Warning!',
            message: 'You will loose your work if you leave the page. Are you sure you want to leave?',
            icon: 'exclamation-sign',
            iconColor: 'red',
            buttons: 'okcancel'
          });
  
          $scope.prompt = function() {
            $scope.status = '';
            dialog().then(function() {
              $scope.status = 'resolved';
            }, function(val) {
              $scope.status = 'rejected as \'' + val + '\''
            });
          }
  
          $scope.prompt();
        });
    </file>
    <file name='styles.css'>
      body {
        height: 350px;
      }
    </file>
  </example>
  
    @example
    <h3>Custom dialog buttons</h3>
  In example, a dialog with the buttons 'Save', 'Don't Save' and 'Cancel' would be created the following way:
  
  <example name='customdialog'>
    <file name='index.html'>
      <cui-button ng-click="prompt()">Show save/don't save dialog box</cui-button>
      <p ng-if='status'>Promise was {{status}}.</p>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiDialog) {
          $scope.document = 'hello_world.txt';
  
          var dialog = cuiDialog({
            title: 'Unsaved changes',
            message: "Do you want to save '{{document}}'?",
            scope: $scope,
            icon: 'warning-sign',
            iconColor: 'yellow',
            keyboard: true,
            buttons: [{
              label: 'Save',
              promise: 'resolve',
              cuiType: 'primary',
              value: 'save',
              focus: true
            }, {
              label: 'Don\'t save',
              promise: 'reject',
              value: 'no_save'
            }, {
              label: 'Cancel',
              promise: 'reject',
              value: 'cancel'
            }]
          });
  
          $scope.prompt = function() {
            $scope.status = '';
            dialog().then(function() {
              $scope.status = 'resolved';
            }, function(val) {
              $scope.status = 'rejected as \'' + val + '\''
            });
          }
  
          $scope.prompt();
        });
    </file>
    <file name='styles.css'>
      body {
        height: 350px;
      }
    </file>
  </example>
   */


  /**
  @ngdoc method
  @name dialog#
  @param {object} config An object with the following properties:
  
  ```javascript
  {
    title: 'Dialog',
    scope: $scope, // When scope is child of. Otherwise, scope is child of root
    message: 'Do you want to save changes to "{{item}}"?', // item is on the scope
    icon: 'warning-sign',
    iconColor: 'yellow',
    keyboard: false, // If true and user presses <esc>, will reject with 'keyboard' value
    buttons: 'ok' // Default: 'ok'. Can be either string or array (see below)
  }
  ```
  
  The `button` property can be passed either a `String` or `Array`. The following `String` values are preset for ease of use:
  
   1. `okcancel`: OK (focused) resolves, Cancel rejects
   2. `yesno`: Yes (focused) resolves, No rejects
   3. `close`: Close (focused) resolves
   4. `ok`: OK (focused) resolves
  
  
  An `Array` passed to the `button` property takes `Object`s like the following:
  
  ```javascript
  {
    label: 'Save', // The button's label
    cuiType: 'primary', // The button's cuiType
    promise: 'resolve', // optional: defaults to 'resolve'. Can be the String 'resolve' OR 'reject', how the promise should be handled when clicked
    value: 'save', // optional: A value to pass through to the promise
    focus: true // If true, this button will be focused upon opening. Only one button in a dialog should have `focus: true`.
  }
  ```
  
  Additionally, the `scope` property can be used to pass in a scope to create a child from. If none is specified, the rootScope is used.
  
  The `scope` property along with the fact that the `message` string is compiled allows for custom properties (see the following example).
  
    @returns {function} Call this newly returned function with no parameters to create a new dialog to display to the user based on the previous config. It can be called as many times as needed.
  
  Once called, this will return a promise that will resolve or reject before the user is allowed to do anything else. If there are more than three buttons, an optional `value` property of each button will be passed to the promise as the first parameter.
   */

  module = angular.module('cui.services.dialog', ['cui.base']);

  module.service('cuiDialog', function(cuiModal, baseTemplatePath, $q) {
    return function(config) {
      var instance, scope;
      if (config == null) {
        config = {};
      }
      scope = cuiModal({
        type: 'confirm',
        scope: config.scope,
        template: '<cui-template content="message"></cui-template>',
        "static": true,
        keyboard: config.keyboard || false
      });
      instance = function() {
        var button, deferred, promise, _i, _j, _len, _len1, _ref, _ref1;
        deferred = $q.defer();
        promise = deferred.promise;
        scope.message = config.message;
        scope.title = {};
        scope.title.label = config.title || 'Unnamed Dialog';
        scope.title.icon = config.icon || 'info-sign';
        scope.title.iconColor = config.iconColor || 'blue';
        scope.buttons = [];
        if (angular.isArray(config.buttons)) {
          _ref = config.buttons;
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            button = _ref[_i];
            scope.buttons.push({
              label: button.label,
              cuiType: button.cuiType || 'transparent',
              value: button.value,
              promise: button.promise,
              focus: button.focus
            });
          }
        } else {
          scope.buttons = (function() {
            switch (config.buttons) {
              case 'yesno':
                return [
                  {
                    label: 'Yes',
                    promise: 'resolve',
                    cuiType: 'primary',
                    focus: true
                  }, {
                    label: 'No',
                    promise: 'reject',
                    cuiType: 'transparent'
                  }
                ];
              case 'okcancel':
                return [
                  {
                    label: 'OK',
                    promise: 'resolve',
                    cuiType: 'primary',
                    focus: true
                  }, {
                    label: 'Cancel',
                    promise: 'reject',
                    cuiType: 'transparent'
                  }
                ];
              case 'close':
                return [
                  {
                    label: 'Close',
                    promise: 'resolve',
                    cuiType: 'primary',
                    focus: true
                  }
                ];
              default:
                return [
                  {
                    label: 'OK',
                    promise: 'resolve',
                    cuiType: 'primary',
                    focus: true
                  }
                ];
            }
          })();
        }
        _ref1 = scope.buttons;
        for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
          button = _ref1[_j];
          if (button.promise === 'reject') {
            button.action = function() {
              scope.modal.hide();
              return deferred.reject.apply(null, arguments);
            };
          } else {
            button.action = function() {
              scope.modal.hide();
              return deferred.resolve.apply(null, arguments);
            };
          }
        }
        scope.$on('hide', function(e, message) {
          if (message === 'keyboard') {
            return deferred.reject('keyboard');
          }
        });
        scope.modal.show();
        return promise;
      };
      instance.hide = scope.modal.hide;
      instance.dismiss = scope.modal.dismiss;
      return instance;
    };
  });

  module.directive('cuiFocusWhen', function($timeout, $parse) {
    return {
      link: function(scope, element, attrs) {
        var model;
        model = $parse(attrs.cuiFocusWhen);
        return scope.$watch(model, function(value) {
          if (value === true) {
            $timeout(function() {
              element[0].focus();
            });
          }
        });
      }
    };
  });


  /**
    @ngdoc service
    @name loading
  
    @module cui.controls
    @src services/loading/loading.coffee
    @controlType presentational
  
    @description The Loading Service allows for an easy way to let the user know when promises are deferred.
  
  When a promise is pushed to the Loading Service, the loading modal appears over content on the webpage.
  
  Once **all** promises are either resolved or rejected, the loading modal is hidden.
  
  The `cuiLoading` wraps an object that might be a value or a (3rd party) then-able promise, so you can provide it promises from outside of Angular.
  
  <h3>What if I don't have a promise?</h3>
  
  You can always wrap anything with a promise. For example:
  
  ```javascript
  AppController = function(cuiLoading, $q) {
    var deferred = $q.defer()
  
    cuiLoading(deferred.promise);
  
    someAsyncTask(function callback(data) {
      // Doing something in the callback...
  
      // Resolve the promise (doesn't matter if it resolves or rejects for cuiLoading's sake)
      deferred.resolve();
    })
  
  }
  ```
  
  <h3>Spinner Colors</h3>
  
  By injecting the `cuiLoadingProvider` into a config block, you can configure the Loading Service Spinner color. For example:
  
  ```javascript
  angular.module('app', ['cui'])
    .config(function(cuiLoadingProvider) {
      cuiLoadingProvider.spinnerColor = 'white';
    });
  ```
  
  See the [Spinner documentation](../../directive/spinner) for available color names.
  
  > Note: This will change the Spinner color for ALL calls to the Loading Service.
  
    @example
    <h3>Timeout promises with cuiLoading (for demonstration):</h3>
  <example name='loading'>
    <file name='index.html'>
      <cui-textbox ng-model='time' placeholder='Time (in ms)'></cui-textbox>
      <cui-button cui-type='primary' ng-click='newTimeout(time)'>Push new timeout</cui-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, $timeout, cuiLoading) {
          var timeouts = [];
  
          $scope.newTimeout = function(time) {
            var t = $timeout(angular.noop, time);
            timeouts.push(t);
            cuiLoading(t);
          }
        });
    </file>
    <file name='styles.css'>
      body {
        height: 400px;
      }
    </file>
  </example>
  
  @example
    <h3>White Spinner:</h3>
  <example name='loading'>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .config(function(cuiLoadingProvider) {
          cuiLoadingProvider.spinnerColor = 'white'
        })
        .controller('AppCtrl', function($scope, $q, cuiLoading) {
          var deferred = $q.defer();
          cuiLoading(deferred.promise);
        });
    </file>
    <file name='styles.css'>
      body {
        height: 400px;
      }
    </file>
  </example>
   */


  /**
  @ngdoc method
  @name loading#
  @param {promise|array} obj A promise or array of promises to push to the Loading Service.
  
  The following is a basic use case using the `cuiDataSourceService`:
  
  ```javascript
  AppController = function(cuiLoading, cuiDataSourceService) {
    var endpoint = cuiDataSourceService('/api/servers')
    cuiLoadingService(endpoint.all().then(function(data) {
      // Display data to user
    }));
  }
  ```
   */

  module = angular.module('cui.services.loading', ['cui.base']);

  module.provider('cuiLoading', function() {
    this.spinnerColor = 'blue';
    this.$get = (function(_this) {
      return function(baseTemplatePath, cuiModal, cuiLoadingPromise, $timeout, $q) {
        var loadingScope, pushPromise;
        loadingScope = cuiModal({
          type: 'bare',
          templateUrl: "" + baseTemplatePath + "loading.html",
          className: 'cui-loading',
          backdrop: true,
          "static": true,
          keyboard: false
        });
        loadingScope.spinnerColor = _this.spinnerColor;
        pushPromise = cuiLoadingPromise(function() {
          return loadingScope.modal.hide();
        });
        return function(obj) {
          var timeoutPromise;
          pushPromise(obj);
          timeoutPromise = $timeout(function() {
            return loadingScope.modal.show();
          });
          return $q.all(angular.isArray(obj) ? obj : [obj])["finally"](function() {
            return $timeout.cancel(timeoutPromise);
          });
        };
      };
    })(this);
    return this;
  });

  module.factory('cuiLoadingPromise', function($q) {
    var promises;
    promises = [];
    return function(done) {
      var promiseUpdated, pushPromise;
      promiseUpdated = function(promise) {
        promises.splice(promises.indexOf(promise), 1);
        if (promises.length === 0) {
          return done();
        }
      };
      pushPromise = function(promise) {
        promise = $q.when(promise).then((function() {
          return promiseUpdated(promise);
        }), (function() {
          return promiseUpdated(promise);
        }));
        return promises.push(promise);
      };
      return function(obj) {
        if (angular.isArray(obj)) {
          return obj.forEach(function(promise) {
            return pushPromise(promise);
          });
        } else {
          return pushPromise(obj);
        }
      };
    };
  });


  /*
    PRIVATE
  
    Provides the location (context) of the modals in the DOM
   */

  module.factory('cuiModals', function() {
    var modals;
    modals = document.getElementById('cui-modals');
    if (modals == null) {
      modals = document.createElement('div');
      modals.id = 'cui-modals';
      document.body.appendChild(modals);
    }
    return modals;
  });


  /**
    @ngdoc service
    @name modal
  
    @module cui.controls
    @src services/modal/modal.coffee
    @controlType presentational
  
    @description A modal window is a graphical control element subordinate to an application's main window which creates a mode where the main window can't be used.
  
  For simple user prompts, see the Dialog Service.
  
  
    @example
  <example name='modal'>
    <file name='index.html'>
      <cui-button ng-click='open()'>Open the modal</cui-button>
    </file>
    <file name='app.js'>
      angular.module('app', ['cui'])
        .controller('AppCtrl', function($scope, cuiModal) {
          var modalScope = cuiModal({
            templateUrl: 'modal.html',
            controller: 'ModalController',
            visible: true
          })
  
          $scope.open = modalScope.modal.show;
  
          modalScope.$on('hide', function() {
            console.log('Modal was closed.');
          });
          modalScope.$on('show', function() {
            console.log('Modal was shown.');
          });
          modalScope.$on('dismiss', function() {
            console.log('Modal was dimissed.');
          });
        })
        .controller('ModalController', function($scope) {
          $scope.name = 'Alex';
          $scope.$on('hide', function() {
            console.log('Close detected from within ModalController.');
          });
          $scope.$on('show', function() {
            console.log('Show detected from within ModalController.');
          });
          $scope.$on('dismiss', function() {
            console.log('Dismiss detected from within ModalController.');
          });
        });
    </file>
    <file name='modal.html'>
      <header>My ({{name}}) first modal</header>
  
      <p>A little bit of information</p>
  
      <footer>
        <menu>
          <cui-button cui-type="primary" ng-click="modal.hide()">Close</cui-button>
          <cui-button cui-type="danger" ng-click="modal.dismiss()">Dismiss</cui-button>
          <cui-button>Another action</cui-button>
        </menu>
      </footer>
    </file>
    <file name='styles.css'>
      body {
        height: 400px;
      }
    </file>
  </example>
   */


  /**
  @ngdoc method
  @name modal#
  @param {object} config An object with the following properties:
  
  ```javascript
  {
    type: 'dismiss', // Default 'dismiss'. Learn more about the type property below
    templateUrl: 'modal.html', // OR `template`
    template: '<h1>Hello, world!</h1>', // OR `templateUrl`
    className: 'my-modal-larger', // A class that is applied the base `.cui-modal`. Note: For setting width, please use the css property `min-width`.
    controller: 'MyModalController', // Create a controller for the modal. None by default
    scope: $scope, // Default $rootScope. Create a child scope for the modal inheriting the scope passed in
    backdrop: true, // Default true. If true, the background will be slightly darker
    visible: true, // Default false. If true, the modal will be displayed when created
    static: true // Default false. If true, the backdrop will not hide the modal
    keyboard: true // Default true. If true, binds escape key to hide the modal on keyup. The 'hide' is event $broadcasted with message 'keyboard'.
  }
  ```
  
  The `type` property allows for initial controls on the modal. For now, the following types are supported:
  
   1. `'dismiss'`: Essentially `'bare'`, except there is a dismiss button in the top right corner of the modal.
   2. `'bare'`: A bare modal, with no customization.
  
  Please remember that `templateUrl` should be used in most cases (as opposed to `template`).
  
  If you do not want to have the HTML for the modal on another page, Angular supports mocking template locations the following way:
  
  ```html
  <script type='text/ng-template' id='confirmation.html'>
    <!-- Some HTML here -->
  </script>
  ```
  
  The `templateUrl` for this is the ID; in this case, `confirmation.html`.
  
    @returns {$scope} The scope of the modal.
  
  On this scope, there is a `modal` property, which contains the functions `show()`, `hide()`, and `dismiss()`. You can call these anywhere in your modal template. For example, a close button:
  
  ```html
  <cui-button ng-click="modal.hide()">Close modal</cui-button
  ```
  
  Please note that calling `dismiss()` is an irreversible action, and `show()` will not work after dismissal.
  
  The events `show`, `hide`, and `dismiss` are broadcasted on this returned scope. They are broadcasted *after* the 250ms animation in or out.
   */

  module = angular.module('cui.services.modal', ['cui.base', 'ngTouch']);

  module.constant('cuiModalValidTypes', ['bare', 'confirm', 'dismiss']);

  module.directive('cuiModal', function() {
    return {
      restrict: 'C',
      controller: function($element) {
        this.addClass = function(c) {
          return $element.addClass(c);
        };
        return this;
      }
    };
  });

  module.provider('cuiModal', function() {
    var ANIMATE_SPEED;
    ANIMATE_SPEED = 250;
    this.$get = function($rootScope, $compile, $document, $templateCache, baseTemplatePath, $log, $timeout, $controller, cuiModalValidTypes, cuiModalDismiss, cuiModals) {
      return function(options) {
        var afterHideAnimation, config, currentAnimation, defaultConfig, dom, el, scope;
        if (options == null) {
          options = {};
        }
        defaultConfig = {
          backdrop: true,
          visible: false,
          "static": false,
          keyboard: true,
          type: 'dismiss'
        };
        config = angular.extend({}, defaultConfig, options);
        if (cuiModalValidTypes.indexOf(config.type) === -1) {
          $log.cuiError({
            cuiErrorType: 'modal',
            cuiErrorInvalidType: config.type,
            cuiValidTypes: cuiModalValidTypes
          });
          config.type = 'bare';
        }
        config.classes = [];
        if (config.type != null) {
          config.classes.push("cui-modal-type-" + config.type);
        }
        if (config.className != null) {
          config.classes.push(config.className);
        }
        config.typeTemplateUrl = "" + baseTemplatePath + "modal-" + config.type + ".html";
        dom = $templateCache.get("" + baseTemplatePath + "modal.html");
        scope = angular.extend((config.scope || $rootScope).$new(), {
          modal: config
        });
        if (config.controller) {
          $controller(config.controller, {
            $scope: scope
          });
        }
        el = $compile(dom)(scope);
        currentAnimation = null;
        scope.modal.show = function() {
          if (scope.modal.visible) {
            cuiModals.removeChild(el[0]);
            cuiModals.appendChild(el[0]);
            return;
          }
          if (currentAnimation) {
            $timeout.cancel(currentAnimation);
            afterHideAnimation();
          }
          $document.find('html').addClass('cui-modal-no-scrollbars');
          el.addClass('shown');
          cuiModals.appendChild(el[0]);
          scope.modal.visible = true;
          scope.$broadcast('show');
        };
        if (scope.modal.visible === true) {
          scope.modal.visible = false;
          scope.modal.show();
        }
        afterHideAnimation = function(message, broadcastHide) {
          if (broadcastHide == null) {
            broadcastHide = true;
          }
          cuiModals.removeChild(el[0]);
          currentAnimation = null;
          if (broadcastHide) {
            if (message != null) {
              return scope.$broadcast('hide', message);
            } else {
              return scope.$broadcast('hide');
            }
          }
        };
        scope.modal.hide = function(message, broadcastHide) {
          if (!scope.modal.visible) {
            return;
          }
          el.removeClass('shown');
          currentAnimation = $timeout(function() {
            return afterHideAnimation(message, broadcastHide);
          }, ANIMATE_SPEED);
          $document.find('html').removeClass('cui-modal-no-scrollbars');
          scope.modal.visible = false;
          return currentAnimation;
        };
        scope.modal.dismiss = function() {
          scope.modal.hide(null, false);
          return $timeout(function() {
            scope.$broadcast('dismiss');
            scope.$destroy();
            return el = null;
          }, ANIMATE_SPEED);
        };
        return scope;
      };
    };
    return this;
  });


  /*
    PRIVATE
  
    This factory listens for escape keyup events, and then grabs the topmost modal,
    determines if it is dismissable, and dismisses it if so.
   */

  module.factory('cuiModalDismiss', function($document, cuiModals) {
    var modalCloseEvent;
    modalCloseEvent = function(e) {
      var lastModal, lastModalWrapper, modalOptions;
      if (e.which === 27) {
        lastModalWrapper = cuiModals.lastChild;
        if (lastModalWrapper) {
          lastModal = angular.element(lastModalWrapper.getElementsByClassName('cui-modal')[0]);
          modalOptions = lastModal.scope().modal;
          if (modalOptions.keyboard) {
            return modalOptions.hide('keyboard');
          }
        }
      }
    };
    $document.bind('keyup', modalCloseEvent);
    return modalCloseEvent;
  });

  module.directive('cuiTemplate', function($compile, $parse) {
    return {
      restrict: 'E',
      link: function(scope, element, attr) {
        scope.$watch(attr.content, (function() {
          element.html($parse(attr.content)(scope));
          $compile(element.contents())(scope);
        }), true);
      }
    };
  });


  /*
    @ns cui.services.position
    @name Position Service
    @example position.default.html
    @description The Position  A set of utility methods that can be use to retrieve position of DOM elements.  It is meant to be used where we need to absolute-position DOM elements in relation to other, existing elements.  So it will give you the relative position to the parent element.  Both elements need to be visible for this to work.
   */

  module = angular.module('cui.services.position', ['cui.base']);

  module.factory('cuiPositionService', function() {
    return {
      getPositionValue: function(unknownItem) {
        var element;
        if (angular.isString(unknownItem)) {
          element = document.querySelector(unknownItem).getBoundingClientRect();
        } else {
          element = unknownItem[0].getBoundingClientRect();
        }
        return element;
      },
      getRelativePositionValue: function(anchor, attached, position, offset) {
        var obj;
        if (offset == null) {
          offset = 0;
        }
        if (position === 'top') {
          obj = {
            top: 0 - attached.height - offset,
            left: (anchor.width / 2) - (attached.width / 2)
          };
        }
        if (position === 'bottom') {
          obj = {
            top: anchor.height + offset,
            left: (anchor.width / 2) - (attached.width / 2)
          };
        }
        if (position === 'left') {
          obj = {
            top: (anchor.height / 2) - (attached.height / 2),
            left: 0 - attached.width - offset
          };
        }
        if (position === 'right') {
          obj = {
            top: (anchor.height / 2) - (attached.height / 2),
            left: anchor.width + offset
          };
        }
        obj.top = "" + obj.top + "px";
        obj.left = "" + obj.left + "px";
        return obj;
      }
    };
  });


  /*
  cuiTransition service provides a consistent interface to trigger CSS 3 transitions and to be informed when they complete.
  @param  {DOMElement} element  The DOMElement that will be animated.
  @param  {string|object|function} trigger  The thing that will cause the transition to start:
  - As a string, it represents the css class to be added to the element.
  - As an object, it represents a hash of style attributes to be applied to the element.
  - As a function, it represents a function to be called that will cause the transition to occur.
  @return {Promise}  A promise that is resolved when the transition finishes.
   */

  module = angular.module('cui.services.transition', ['cui.base']);

  module.factory('cuiTransition', function($q, $timeout, $rootScope) {
    var animationEndEventNames, cuiTransition, findEndEventName, transElement, transitionEndEventNames;
    cuiTransition = function(element, trigger, options) {
      var deferred, endEventName, transitionEndHandler;
      options = options || {};
      deferred = $q.defer();
      endEventName = cuiTransition[(options.animation ? 'animationEndEventName' : 'transitionEndEventName')];
      transitionEndHandler = function(event) {
        if (event.target !== element[0]) {
          return;
        }
        return $rootScope.$apply(function() {
          element.unbind(endEventName, transitionEndHandler);
          return deferred.resolve(element);
        });
      };
      if (endEventName) {
        element.bind(endEventName, transitionEndHandler);
      }
      $timeout(function() {
        if (angular.isString(trigger)) {
          element.addClass(trigger);
        } else if (angular.isFunction(trigger)) {
          trigger(element);
        } else if (angular.isObject(trigger)) {
          element.css(trigger);
        }

        /* istanbul ignore if */
        if (!endEventName) {
          return deferred.resolve(element);
        }
      });
      deferred.promise.cancel = function() {
        if (endEventName) {
          element.unbind(endEventName, transitionEndHandler);
        }
        return deferred.reject('Transition cancelled');
      };
      return deferred.promise;
    };
    transElement = document.createElement('trans');
    transitionEndEventNames = {
      WebkitTransition: 'webkitTransitionEnd',
      MozTransition: 'transitionend',
      OTransition: 'oTransitionEnd',
      transition: 'transitionend'
    };
    animationEndEventNames = {
      WebkitTransition: 'webkitAnimationEnd',
      MozTransition: 'animationend',
      OTransition: 'oAnimationEnd',
      transition: 'animationend'
    };
    findEndEventName = function(endEventNames) {
      var name;
      for (name in endEventNames) {
        if (transElement.style[name] !== void 0) {
          return endEventNames[name];
        }
      }
    };
    cuiTransition.transitionEndEventName = findEndEventName(transitionEndEventNames);
    cuiTransition.animationEndEventName = findEndEventName(animationEndEventNames);
    return cuiTransition;
  });

  base = angular.module('cui.base');

  base.config(function($provide) {
    return $provide.decorator('$log', function($delegate) {
      var _log;
      _log = $delegate.log;
      $delegate.cuiError = function(error) {
        var message;
        if (error.cuiErrorType === 'ngmodel') {
          message = 'CUI ERROR: missing NG-MODEL attribute on cui control: ' + error.cuiErrorCtrl.toUpperCase() + ' - name/id: ' + error.cuiErrorElIdentity.toUpperCase();
        }
        if (error.cuiErrorType === 'radiomodel') {
          message = 'CUI ERROR: missing RADIO-MODEL attribute on cui control: ' + error.cuiErrorCtrl.toUpperCase() + ' - name/id: ' + error.cuiErrorElIdentity.toUpperCase();
        }
        if (error.cuiErrorType === 'name') {
          message = 'CUI ERROR: missing NAME attribute on cui control: ' + error.cuiErrorCtrl.toUpperCase() + ' - name/id: ' + error.cuiErrorElIdentity.toUpperCase();
        }
        if (error.cuiErrorType === 'modal') {
          message = "CUI ERROR: invalid TYPE attribute on cuiModal - '" + error.cuiErrorInvalidType + "' is not in list " + (error.cuiValidTypes.join(', '));
        }
        if (error.cuiErrorType === 'attr') {
          message = "CUI ERROR: missing attribute - '" + error.cuiErrorAttr + "' is a required attribute for '" + error.cuiErrorDirectiveName + "'";
        }
        if (message) {
          return $delegate.error(message);
        }
      };
      return $delegate;
    });
  });

}).call(this);

angular.module("cui.templates", []).run(["$templateCache", function($templateCache) {$templateCache.put("__cui/alert.html","<div cui-collapse=isClosed><div class=\"cui-alert cui-alert-{{cuiType}}\"><cui-icon class=cui-label-icon icon={{_icon}}></cui-icon><span class=cui-label ng-if=_label>{{_label}}</span> <span ng-transclude class=cui-message></span><cui-button ng-if=dismissable cui-type=transparent class=cui-close size=small ng-click=close()><cui-icon icon=remove></cui-icon></cui-button></div></div>");
$templateCache.put("__cui/badge.html","<div ng-transclude class=\"cui-badge cui-badge-type-{{cuiType}}\"></div>");
$templateCache.put("__cui/breadcrumb.html","<ul class=cui-breadcrumb><li ng-repeat=\"item in items\"><a ng-href={{item.url}}><cui-icon ng-if=item.icon icon={{item.icon}}></cui-icon>{{item.label}}</a><cui-icon class=cui-separator ng-if=!$last icon=angle-right></cui-icon></li></ul>");
$templateCache.put("__cui/button-link.html","<button ng-transclude></button>");
$templateCache.put("__cui/button.html","<button><div class=cui-button-overlay ng-transclude></div></button>");
$templateCache.put("__cui/calendar.html","<div class=cui-calendar><div class=cui-calendar-header><div class=cui-calendar-left><div class=cui-calendar-arrow ng-click=calendarCtrl.previousYear() ng-attr-title=\"{{ \'CUI_CALENDAR_PREVIOUS_YEAR\' | translate }}\"><cui-icon icon=double-angle-left></cui-icon></div><div class=cui-calendar-arrow ng-click=calendarCtrl.previousMonth() ng-attr-title=\"{{ \'CUI_CALENDAR_PREVIOUS_MONTH\' | translate }}\"><cui-icon icon=angle-left></cui-icon></div></div>{{ monthDate | date:\'MMMM yyyy\' }}<div class=cui-calendar-right><div class=cui-calendar-arrow ng-click=calendarCtrl.nextMonth() ng-attr-title=\"{{ \'CUI_CALENDAR_NEXT_MONTH\' | translate }}\"><cui-icon icon=angle-right></cui-icon></div><div class=cui-calendar-arrow ng-click=calendarCtrl.nextYear() ng-attr-title=\"{{ \'CUI_CALENDAR_NEXT_YEAR\' | translate }}\"><cui-icon icon=double-angle-right></cui-icon></div></div></div><div class=cui-calendar-month><div class=\"cui-calendar-day-of-week cui-calendar-week\" ng-repeat=\"week in month | limitTo:1\"><div class=cui-calendar-short ng-repeat=\"day in week\" ng-attr-title=\"{{ day.date | date:\'EEEE\' }}\">{{ day.date | date:\'EEE\' }}</div></div><div class=cui-calendar-week ng-repeat=\"week in month\"><div class=cui-calendar-day ng-repeat=\"day in week\" ng-click=setDate(day.date) ng-class=\"{&quot;cui-calendar-selected&quot;: day.isSelected,\n                      &quot;cui-calendar-not-in-month&quot;: !day.isInMonth,\n                      &quot;cui-calendar-today&quot;: day.today}\" ng-attr-title=\"{{ day.date | date:\'fullDate\' }}\">{{ day.date | date:\'d\' }}</div></div></div><div class=cui-calendar-footer ng-click=setDate(today) ng-attr-title=\"{{ \'CUI_CALENDAR_TODAY\' | translate }}\">{{ today | date:\'fullDate\' }}</div></div>");
$templateCache.put("__cui/checkbox.html","<label class=cui-checkbox role=checkbox><input type=checkbox ng-model=ngModel ng-required=ngRequired ng-disabled=\"ngDisabled\"> <span tabindex=0 ng-transclude></span></label>");
$templateCache.put("__cui/checkboxgroup.html","<div><label class=cui-checkbox ng-repeat=\"(key, option) in checkItems\"><input type=checkbox ng-model=ngModel ng-disabled=\"ngDisabled\"> <span tabindex=0 ng-bind=label></span></label></div>");
$templateCache.put("__cui/combobox.html","<div class=cui-combobox><input type=text class=cui-textbox ng-model=ngModel placeholder=\"\" ng-disabled=ngDisabled ng-click=openMenu($event) role=combobox aria-owns=cui-menu-{{name}} aria-labeledby=\"cui-label-{{name}}\"><cui-button cui-type=transparent ng-click=toggleMenu($event) ng-disabled=ngDisabled aria-controls=cui-menu-{{name}}><cui-icon icon=caret-down></cui-icon></cui-button><div id=cui-menu-{{name}} class=\"cui-menu-wrap cui-menu-anchor-left\"><div cui-menu><cui-menu-item ng-repeat=\"item in selectedItems\" ng-click=select($index) ng-class=\"{\'cui-combo-current\': $index === currentIndex, \'cui-combo-selected\': $index === selectedIndex }\">{{ item.label }}</cui-menu-item></div></div></div>");
$templateCache.put("__cui/dataGrid.html","<div class=\"cui-data-grid cui-table\"><table><thead><tr><td ng-if=\"config.searchable !== false\" colspan={{config.columns.length}}><cui-textbox ng-model=config.searchQuery placeholder=\"{{ \'CUI_DATAGRID_SEARCH_PLACEHOLDER\' | translate}}\"></cui-textbox></td></tr><tr class=cui-table-text-align><th ng-if=config.displayChecks></th><th ng-repeat=\"column in config.columns\" ng-click=toggleSort(column) class={{column.className}}><span ng-class=\"\n            { \'sort-ascent\': config.sortCol === column.map && config.sortDir === \'asc\',\n             \'sort-descent\': config.sortCol === column.map && config.sortDir === \'desc\'}\">{{column.label || column.map}}</span></th></tr></thead><tbody><tr ng-repeat=\"row in dataGetter(response)\" ng-class=\"{selected: selected[config.id] === row[config.id], checked: isChecked(row)}\" class=cui-table-text-align ng-click=\"datagrid.updateRowStatus($event, row)\"><td ng-if=config.displayChecks><cui-icon tabindex=0 icon=\"{{isChecked(row) && \'check\' || \'check-empty\'}}\" ng-click=toggleChecked(row) class=cui-data-grid-check></cui-icon></td><td ng-repeat=\"column in config.columns\" class=\"cui-data-grid-cell {{column.className}}\">{{ row[column.map] }}</td></tr></tbody><tfoot><tr ng-if=\"dataGetter(response).length === 0\"><td colspan=\"{{config.displayChecks && config.columns.length+1 || config.columns.length}}\" class=no-rows translate=CUI_DATAGRID_NO_DATA></td></tr><tr><td colspan=\"{{config.displayChecks && config.columns.length+1 || config.columns.length}}\"><div class=pagination ng-if=showPaginator><ul><li ng-if=\"pages[0].number !== 1\" ng-click=\"config.currentPageNumber = config.currentPageNumber - 1\">&lt;</li><li ng-repeat=\"page in pages\" ng-class=\"{active: config.currentPageNumber === page.number}\" ng-click=\"config.currentPageNumber = page.number\"><a>{{page.number}}</a></li><li ng-if=\"pages[pages.length-1].number !== totalPages\" ng-click=\"config.currentPageNumber = config.currentPageNumber + 1\">&gt;</li></ul></div></td></tr></tfoot></table></div>");
$templateCache.put("__cui/datePicker-calendar.html","<cui-calendar class=cui-date-picker-calendar ng-model=model></cui-calendar>");
$templateCache.put("__cui/datePicker-time.html","<cui-time ng-model=model></cui-time>");
$templateCache.put("__cui/dropDownButton.html","<div class=cui-drop-down-button><cui-button><cui-icon ng-if=icon class=cui-drop-down-button-icon icon={{icon}}></cui-icon>{{label}}<cui-icon icon=caret-down></cui-icon></cui-button></div>");
$templateCache.put("__cui/dropDownList.html","<select class=cui-drop-down-list ng-transclude></select>");
$templateCache.put("__cui/masthead.html","<header class=cui-masthead ng-class=\"{fixed: fixed}\"><cui-icon class=dell-logo icon=dell-halo color=white size=47px></cui-icon><div class=cui-masthead-break></div><h1 class=cui-application-title><a ng-href={{applicationHref}} class=cui-application-title-link ng-if=\"!applicationLogo && applicationHref\"><span class=cui-application-name>{{name}}</span></a> <span class=cui-application-name ng-if=\"!applicationLogo && !applicationHref\">{{name}}</span> <a ng-href={{applicationHref}} class=cui-application-title-link ng-if=\"applicationLogo && applicationHref\"><span class=cui-application-name><img class=cui-application-logo ng-src=applicationLogo></span></a> <span class=cui-application-name ng-if=\"applicationLogo && !applicationHref\"><img class=cui-application-logo ng-src=applicationLogo></span> <span class=cui-application-subname ng-if=subname>{{subname}}</span></h1><span ng-show=showControls><div class=cui-hidden-lg><span class=cui-navigation-drawer ng-show=showNavigationDrawer ng-click=$parent.toggleNavigation()><cui-icon icon=angle-right></cui-icon></span> <span class=cui-drawer ng-class=\"{&quot;cui-drawer-showing&quot;: drawerShowing}\" ng-show=showControls ng-click=toggleDrawer()><cui-icon icon=ellipsis-vertical></cui-icon></span></div><div ng-transclude class=\"cui-masthead-controls horizontal-list\" ng-class=\"{&quot;cui-masthead-drawer-showing&quot;: drawerShowing}\"></div></span></header>");
$templateCache.put("__cui/icon.html","<i class=\"cui-icon cui-icon-color-{{color}}\" ng-style=\"{&quot;font-size&quot;: size}\"></i>");
$templateCache.put("__cui/memo.html","<span>{{message}}</span>");
$templateCache.put("__cui/menu.html","<div class=cui-menu ng-transclude ng-class=\"{&quot;cui-menu-showing&quot;: isShowing}\"></div>");
$templateCache.put("__cui/menuItem.html","<div class=cui-menu-item ng-transclude></div>");
$templateCache.put("__cui/navigationList.html","<div class=cui-navigation-list role=menu><ul class=cui-navigation-group><li ng-repeat=\"child in items\" role=menuitem aria-haspopup=\"{{child.children.length > 0}}\"><cui-navigation-list-item items=child toplevel=true></cui-navigation-list-item></li></ul></div>");
$templateCache.put("__cui/navigationListItem.html","<a ng-click=showTab() ng-class=\"{ \'cui-navigation-toplevel\': toplevel,\r\n               \'cui-navigation-secondary\': !collapsed,\r\n               \'cui-navigation-active\': items.active }\" ng-href=\"{{items.href || &quot;#&quot;}}\" title={{items.label}} class=cui-navigation-link><cui-icon icon=caret-right ng-class=\"{ \'cui-navigation-open\': items.visible,\r\n                        \'cui-navigation-invisible\': !items.children.length }\" class=cui-navigation-collapse></cui-icon>&nbsp;<cui-icon icon={{items.icon}} ng-if=items.icon class=cui-navigation-icon></cui-icon>&nbsp;{{ items.label }}</a><div cui-collapse=!items.visible cui-collapse-skip-initial=true aria-hidden={{!items.visible}}><ul class=cui-navigation-group ng-class=\"{\'cui-navigation-secondary\': !collapsed,\r\n                 \'cui-navigation-collapsing\': items.visible,\r\n                \'cui-navigation-haschildren\': items.children.length > 0}\" role=menu><li ng-repeat=\"child in items.children\" role=menuitem aria-haspopup=\"{{child.children.length > 0}}\"><cui-navigation-list-item items=child></cui-navigation-list-item></li></ul></div>");
$templateCache.put("__cui/pane.html","<div class=cui-pane ng-class=\"{&quot;collapsed&quot;: _collapsed}\" ng-mouseleave=mouseLeft()><h2 class=cui-pane--title-bar ng-class=\"{&quot;collapsible&quot;: _collapsible}\" ng-click=toggleCollapse(true)><span class=cui-pane--title-bar--icon ng-if=icon><cui-icon icon={{icon}}></cui-icon></span> <span class=cui-pane--title-bar--title>{{cuiTitle || title}}</span> <span class=cui-pane--collapse-icon ng-if=_collapsible><cui-icon icon={{collapseIcon}}></cui-icon></span></h2><div cui-collapse=_collapsed cui-collapse-skip-initial=true><div class=cui-pane--content ng-transclude></div></div></div>");
$templateCache.put("__cui/progressBar.html","<div class=cui-progress-bar><progress max=100 title=\"{{value || &quot;Unknown&quot;}} percent complete\" ng-attr-value={{value}} class=cui-progress-bar-type-{{cuiType}}></progress></div>");
$templateCache.put("__cui/radio.html","<label class=cui-radio><input type=radio ng-model=ngModel value={{value}} ng-disabled=ngDisabled ng-required=\"ngRequired\"> <span tabindex=0 ng-transclude></span></label>");
$templateCache.put("__cui/radiogroup.html","<div><label class=cui-radio ng-repeat=\"(key, option) in radioItems\"><input type=radio ng-model=$parent.radioModel ng-value={{radiovalue}} ng-disabled=ngDisabled ng-required=\"ngRequired\"> <span tabindex=0 ng-bind={{radiolabel}}></span></label></div>");
$templateCache.put("__cui/colorpicker.html","<span ng-repeat=\"color in colorList\"><span class=coloroption style=\"background-color: {{color.color}}\" ng-click=\"command(action, color.color) \" title={{color.name}}></span></span>");
$templateCache.put("__cui/richTextEditor.html","<div class=cui-rich-text-editor></div>");
$templateCache.put("__cui/splitButton.html","<div class=cui-split-button><div class=cui-button-wrap><cui-button size={{size}} cui-type={{cuiType}} ng-click=defaultAction()><cui-icon icon={{icon}} ng-if=icon class=cui-split-button-icon></cui-icon>{{label}}</cui-button><cui-button class=cui-split-button-drop size={{size}} cui-type={{cuiType}}><cui-icon icon=caret-down></cui-icon></cui-button></div></div>");
$templateCache.put("__cui/table.html","<div class=cui-table><smart-table config=globalConfig columns=columnCollection rows=dataCollection></smart-table></div>");
$templateCache.put("__cui/tab.html","<div class=cui-tab-content ng-show=active ng-transclude></div>");
$templateCache.put("__cui/tabset.html","<div class=cui-tabset><ul class=\"cui-tabs cui-horizontal-list\"><cui-button class=cui-tab ng-repeat=\"tab in tabs\" ng-click=tab.showTab() cui-type=\"{{tab.active && &quot;blue&quot; || &quot;transparent&quot;}}\"><cui-icon ng-show=tab.icon icon={{tab.icon}}></cui-icon>&nbsp;{{tab.label}}&nbsp;<cui-badge ng-show=tab.badge cui-type={{tab.badge.type}}>{{tab.badge.label}}</cui-badge></cui-button></ul><div class=cui-tab-container ng-transclude></div></div>");
$templateCache.put("__cui/textarea.html","<textarea class=cui-textarea></textarea>");
$templateCache.put("__cui/textbox.html","<input class=cui-textbox>");
$templateCache.put("__cui/time.html","<div class=cui-time><div class=cui-time-hour><div class=cui-time-angle-up ng-attr-title=\"{{ \'CUI_TIME_HOUR_INCREMENT\' | translate:timeCtrl.options }}\" ng-click=timeCtrl.changeHour(timeCtrl.options.hourIncrement)><cui-icon icon=angle-up></cui-icon></div><div class=cui-time-hour-text ng-if=showMeridian>{{model || lastValidDate | date:\'h\'}}</div><div class=cui-time-hour-text ng-if=!showMeridian>{{model || lastValidDate | date:\'H\'}}</div><div class=cui-time-angle-down ng-attr-title=\"{{ \'CUI_TIME_HOUR_DECREMENT\' | translate:timeCtrl.options }}\" ng-click=timeCtrl.changeHour(-timeCtrl.options.hourIncrement)><cui-icon icon=angle-down></cui-icon></div></div><div class=cui-time-colon><spam>:</spam></div><div class=cui-time-minute><div class=cui-time-angle-up ng-attr-title=\"{{ \'CUI_TIME_MINUTE_INCREMENT\' | translate:timeCtrl.options }}\" ng-click=timeCtrl.changeMinute(timeCtrl.options.minuteIncrement)><cui-icon icon=angle-up></cui-icon></div><div class=cui-time-minute-text>{{model || lastValidDate | date:\'mm\'}}</div><div class=cui-time-angle-down ng-attr-title=\"{{ \'CUI_TIME_MINUTE_DECREMENT\' | translate:timeCtrl.options }}\" ng-click=timeCtrl.changeMinute(-timeCtrl.options.minuteIncrement)><cui-icon icon=angle-down></cui-icon></div></div><div class=cui-time-show-seconds ng-if=showSeconds><div class=cui-time-colon><spam>:</spam></div><div class=cui-time-second><div class=cui-time-angle-up ng-attr-title=\"{{ \'CUI_TIME_SECOND_INCREMENT\' | translate:timeCtrl.options }}\" ng-click=timeCtrl.changeSecond(timeCtrl.options.secondIncrement)><cui-icon icon=angle-up></cui-icon></div><div class=cui-time-second-text>{{model || lastValidDate | date:\'ss\'}}</div><div class=cui-time-angle-down ng-attr-title=\"{{ \'CUI_TIME_SECOND_DECREMENT\' | translate:timeCtrl.options }}\" ng-click=timeCtrl.changeSecond(-timeCtrl.options.secondIncrement)><cui-icon icon=angle-down></cui-icon></div></div></div><div class=cui-time-meridian ng-if=showMeridian ng-attr-title=\"{{ \'CUI_TIME_MERIDIAN\' | translate:timeCtrl.options }}\"><span ng-click=timeCtrl.toggleMeridian()>{{meridian}}</span></div></div>");
$templateCache.put("__cui/tip.html","<div><div ng-include=templateUrl></div></div>");
$templateCache.put("__cui/tooltip.html","<div class=cui-tooltip><span class=cui-tooltip-text style={{cuiStyle}} ng-click=\"toggleTooltip(\'click\')\" ng-mouseenter=\"toggleTooltip(\'hover\')\" ng-mouseleave=\"toggleTooltip(\'hover\')\" ng-if=text>{{text}}</span><cui-icon class=cui-tooltip-icon style={{cuiStyle}} ng-click=\"toggleTooltip(\'click\')\" ng-mouseenter=\"toggleTooltip(\'hover\')\" ng-mouseleave=\"toggleTooltip(\'hover\')\" ng-if=icon icon={{icon}} size={{size}}></cui-icon><div class=\"cui-tooltip-box {{position}}\" ng-style=positionStyle><div class=arrow></div><div class=cui-tooltip-content ng-transclude></div></div></div>");
$templateCache.put("__cui/tree.html","<div class=cui-tree><ul class=cui-tree-group><li ng-repeat=\"child in items\"><cui-tree-item items=child></cui-tree-item></li></ul></div>");
$templateCache.put("__cui/treeItem.html","<a ng-click=toggleVisible() ng-class=\"{\'cui-tree-hide\': !items.children.length}\" class=cui-tree-toggle><cui-icon icon=caret-right ng-class=\"{ \'cui-tree-open\': items.visible,\n            \'cui-tree-invisible\': !items.children.length }\" class=cui-tree-collapse></cui-icon></a><a ng-click=toggleActive() ng-class=\"{\'cui-tree-active\': items.active}\"><cui-icon icon=\"{{ items.icon }}\" ng-if=items.icon class=cui-tree-node-icon></cui-icon>{{ items.label }} {{id}}</a><ul class=cui-tree-group cui-collapse=!items.visible cui-collapse-skip-initial=true><li ng-repeat=\"child in items.children\"><cui-tree-item items=child></cui-tree-item></li></ul>");
$templateCache.put("__cui/choices.tpl.html","<ul class=\"ui-select-choices ui-select-choices-content cui-select-results\"><li class=ui-select-choices-group ng-class=\"{\'cui-select-result-with-children\': $select.choiceGrouped($group) }\"><div ng-show=$select.choiceGrouped($group) class=\"ui-select-choices-group-label cui-select-result-label\" ng-bind-html=$group.name></div><ul ng-class=\"{\'cui-select-result-sub\': $select.choiceGrouped($group), \'cui-select-result-single\': !$select.choiceGrouped($group) }\"><li class=ui-select-choices-row ng-class=\"{\'cui-select-highlighted\': $select.isActive(this), \'cui-select-disabled\': $select.isDisabled(this)}\"><div class=\"cui-select-result-label ui-select-choices-row-inner\"></div></li></ul></li></ul>");
$templateCache.put("__cui/match-multiple.tpl.html"," <span class=ui-select-match><li class=\"ui-select-match-item cui-select-search-choice\" ng-repeat=\"$item in $select.selected\" ng-class=\"{\'cui-select-search-choice-focus\':$select.activeMatchIndex === $index, \'cui-select-locked\':$select.isLocked(this, $index)}\"><span uis-transclude-append></span> <a href=javascript:; class=\"ui-select-match-close cui-select-search-choice-close\" ng-click=$select.removeChoice($index) tabindex=-1>x</a></li></span>");
$templateCache.put("__cui/match.tpl.html"," <a class=\"cui-select-choice ui-select-match\" ng-class=\"{\'cui-select-default\': $select.isEmpty()}\" ng-click=$select.activate()><span ng-show=$select.isEmpty() class=cui-select-chosen>{{$select.placeholder}}</span> <span ng-hide=$select.isEmpty() class=cui-select-chosen ng-transclude></span> <abbr ng-if=\"$select.allowClear && !$select.isEmpty()\" class=cui-select-search-choice-close ng-click=$select.select(undefined)></abbr> <span class=\"cui-select-arrow ui-select-toggle\" ng-click=$select.toggle($event)><b></b></span></a>");
$templateCache.put("__cui/select-multiple.tpl.html","<div class=\"ui-select-container ui-select-multiple cui-select cui-select-container cui-select-container-multi\" ng-class=\"{\'cui-select-container-active cui-select-dropdown-open open\': $select.open,\n                \'cui-select-container-disabled\': $select.disabled}\"><ul class=cui-select-choices><span class=ui-select-match></span><li class=cui-select-search-field><input type=text autocomplete=off autocorrect=off autocapitalize=off spellcheck class=\"cui-select-input ui-select-search\" placeholder={{$select.getPlaceholder()}} ng-disabled=$select.disabled ng-hide=$select.disabled ng-model=$select.search ng-click=$select.activate() style=\"width: 34px\"></li></ul><div class=\"cui-select-drop cui-select-with-searchbox cui-select-drop-active\" ng-class=\"{\'cui-select-display-none\': !$select.open}\"><div class=ui-select-choices></div></div></div>");
$templateCache.put("__cui/select.tpl.html","<div class=\"ui-select-container cui-select cui-select-container\" ng-class=\"{\'cui-select-container-active cui-select-dropdown-open open\': $select.open,\n                \'cui-select-container-disabled\': $select.disabled,\n                \'cui-select-container-active\': $select.focus, \n                \'cui-select-allowclear\': $select.allowClear && !$select.isEmpty()}\"><div class=ui-select-match></div><div class=\"cui-select-drop cui-select-with-searchbox cui-select-drop-active\" ng-class=\"{\'cui-select-display-none\': !$select.open}\"><div class=cui-select-search ng-show=$select.searchEnabled><input type=text autocomplete=off autocorrect=off autocapitalize=off spellcheck class=\"ui-select-search cui-select-input\" ng-model=$select.search></div><div class=ui-select-choices></div></div></div>");
$templateCache.put("__cui/aboutBox.about.html","<div class=cui-about-pane><div class=cui-about-pane-center><cui-icon color=blue icon=dell-halo></cui-icon><div class=cui-title-group><h1 class=cui-title>{{modal.applicationName}}</h1><span class=cui-version>{{modal.version}}</span></div><div class=cui-product-info><p ng-if=patentString>{{modal.patentString}}</p><p translate=CUI_ABOUTBOX_DELL_TRADEMARK></p><p>&copy; {{today | date:\"yyyy\"}} Dell Inc. ALL RIGHTS RESERVED.</p></div></div></div>");
$templateCache.put("__cui/aboutBox.contact.html","<div class=cui-contact-pane><h2 class=cui-title translate=CUI_ABOUTBOX_CONTACT_TITLE></h2><p translate=CUI_ABOUTBOX_CONTACT_TEXT></p><h3 translate=CUI_ABOUTBOX_TECHNICAL_TITLE></h3><div translate=CUI_ABOUTBOX_TECHNICAL_LINK></div><h3 translate=CUI_ABOUTBOX_PRODUCT_TITLE></h3><div translate=CUI_ABOUTBOX_PRODUCT_PHONE></div><h3 translate=CUI_ABOUTBOX_EMAIL_TITLE></h3><div translate=CUI_ABOUTBOX_EMAIL_LINK></div></div>");
$templateCache.put("__cui/aboutBox.html","<cui-tabset><cui-tab ng-repeat=\"tab in tabs\" label={{tab.label}} active=tab.active><div ng-include=tab.template></div></cui-tab></cui-tabset><cui-button class=cui-about-box-close cui-type=primary ng-click=modal.hide()>{{\'CUI_ABOUTBOX_CLOSE_BUTTON\' | translate}}</cui-button>");
$templateCache.put("__cui/aboutBox.licenses.html","<div class=cui-licenses-pane><h1 class=cui-title translate=CUI_ABOUTBOX_LICENSES_TITLE></h1><table class=cui-table><thead><tr><th translate=CUI_ABOUTBOX_LINCENSE_TH></th><th translate=CUI_ABOUTBOX_TYPE_TH></th><th translate=CUI_ABOUTBOX_EXPIRES_TH></th><th translate=CUI_ABOUTBOX_SEATS_LICENSED_TH></th><th translate=CUI_ABOUTBOX_SEATS_USED_TH></th></tr></thead><tbody><tr ng-repeat=\"license in licensesArray\" ng-class=\"{error: license.invalid}\" ng-attr-title={{license.errorMessage}}><td>{{license.name}}</td><td>{{license.type}}</td><td ng-class=\"{error: license.expires < today }\">{{license.expires | date:\'longDate\'}}</td><td>{{license.seatsLicensed}}</td><td ng-class>{{license.seatsUsed}}</td></tr></tbody></table></div>");
$templateCache.put("__cui/aboutBox.thirdParty.html","<div class=cui-third-party-pane><h1 class=cui-title>Third Party Components</h1><table class=cui-table><thead><tr><th>Component</th><th>Legal Notice</th></tr></thead><tbody><tr ng-repeat=\"component in thirdPartyArray\"><td>{{component.name}} {{component.version}}</td><td><div ng-if=component.license.name>This components is governed by the {{component.license.name}} license.</div><div ng-if=component.license.notice>{{component.license.notice}}</div><a href=http://www.quest.com/legal/third-party-licenses.aspx target=_blank>Third party licenses</a></td></tr></tbody></table></div>");
$templateCache.put("__cui/applicationFrame.html","<div class=cui-application-frame ng-class=\"{&quot;cui-navigation-showing&quot;: navigationShowing}\" ng-swipe-left=swipeLeft()><cui-masthead fixed=true show-navigation-drawer=\"{{navigationItems !== undefined}}\" show-controls=mastheadShowControls application-name={{applicationName}} application-subname={{applicationSubname}} application-href={{applicationHref}} application-logo={{applicationLogo}}><span cui-application-frame-transclude=cui-controls></span></cui-masthead><cui-navigation-list ng-if=\"navigationItems !== undefined\" items=navigationItems persist-as={{persistAs}} watch-location=true class=cui-left-navigation></cui-navigation-list><div class=cui-container ng-swipe-right=swipeRight() ng-click=swipeLeft() ng-class=\"{&quot;cui-navigation-none&quot;: navigationItems === undefined}\"><div id=cui-application-frame-alert-service></div><div class=cui-content cui-application-frame-transclude=cui-content></div></div></div>");
$templateCache.put("__cui/startScreen.html","<div class=\"cui-start-screen cui-theme-blue\"><div class=cui-start-screen-container><div class=cui-start-screen-row><div class=cui-start-screen-cell><div class=cui-start-screen-body><cui-icon icon=dell-halo class=cui-start-screen-dell-logo></cui-icon><h1 class=cui-start-screen-application-name>{{config.applicationName}}</h1><div class=cui-start-screen-message ng-transclude></div><form name=cuiStartScreen class=cui-start-screen-sign-in novalidate ng-submit=signIn()><div class=\"cui-start-screen-login-container cui-start-screen-fade\" ng-class=\"{\'cui-start-screen-login\': !config.isSpinning}\"><div class=cui-start-screen-inputs><label><div class=cui-start-screen-label>{{config.usernameLabel || \"CUI_STARTSCREEN_USERNAME_LABEL\" | translate}}:</div><cui-textbox name=cuiUsername class=cui-start-screen-username ng-model=config.username type=text placeholder={{config.usernamePlaceholder}} required></cui-textbox></label></div><div class=cui-start-screen-inputs><label><div class=cui-start-screen-label>{{config.passwordLabel || \"CUI_STARTSCREEN_PASSWORD_LABEL\" | translate}}:</div><cui-textbox name=cuiPassword class=cui-start-screen-password ng-model=config.password type=password placeholder={{config.passwordPlaceholder}} required></cui-textbox></label></div><div class=cui-start-screen-inputs ng-show=config.showDomain><label><div class=cui-start-screen-label>{{config.domainLabel || \"CUI_STARTSCREEN_DOMAIN_LABEL\" | translate}}:</div><cui-textbox name=cuiDomain class=cui-start-screen-domain ng-model=config.domain ng-disabled=parsedDomain placeholder={{config.domainPlaceholder}} ng-required=config.showDomain></cui-textbox></label></div><div class=cui-start-screen-inputs ng-show=config.showRememberMe><cui-checkbox class=cui-start-screen-remember-me name=rememberMe value=rememberMe ng-model=config.rememberMe>{{config.rememberMeText || \'CUI_STARTSCREEN_REMEMBER_ME\'| translate}}</cui-checkbox></div><div class=cui-start-screen-buttons><cui-button class=cui-start-screen-sign-in-button size=medium type=submit ng-disabled=cuiStartScreen.$invalid>{{config.signInLabel || \'CUI_STARTSCREEN_SIGN_IN_LABEL\' | translate}}</cui-button></div></div><cui-spinner class=\"cui-start-screen-spinner cui-start-screen-fade\" color=white ng-if=config.isSpinning></cui-spinner></form><div class=cui-start-screen-push></div></div></div></div></div><div class=cui-start-screen-copyright>{{config.copyright || \'CUI_DELL_COPYRIGHT\' | translate}}</div></div>");
$templateCache.put("__cui/wizard.html","<div class=cui-wizard><div class=cui-wizard-step-buttons><ul><li ng-repeat=\"step in steps\" ng-click=selectStepIndex($index) ng-attr-title=\"{{step.cuiTitle || step.title}}\" ng-class=\"{&quot;cui-wizard-active&quot;: currentStepIndex === $index,\n                     &quot;cui-wizard-disabled&quot;: isStepDisabled($index)}\"><cui-icon icon=angle-right size=20px ng-if=\"currentStepIndex === $index\" class=cui-wizard-angle></cui-icon><span class=cui-wizard-step-title>{{step.cuiTitle || step.title}}</span><div class=cui-wizard-check ng-if=\"(currentStepIndex > $index || editMode) && !step.invalid && !step.onNextFailed\">&#10003;</div><div class=cui-wizard-x ng-if=\"((currentStepIndex >= $index || editMode) && step.invalid) || step.onNextFailed\">&#x2717;</div></li></ul></div><div class=cui-wizard-content><div class=cui-wizard-transclude ng-transclude></div><div class=cui-wizard-navigation><span class=cui-wizard-step-count translate=CUI_WIZARD_STEP translate-values=\"{current: currentStepIndex + 1, total: steps.length}\"></span><div class=cui-wizard-nav-buttons><cui-button ng-click=previous($event) ng-disabled=\"currentStepIndex === 0\" class=\"cui-wizard-nav-button cui-wizard-nav-previous\">{{\'CUI_WIZARD_BACK\' | translate}}</cui-button><cui-button ng-click=next($event) cui-type=primary ng-disabled=\"steps[currentStepIndex].invalid || currentStepIndex === steps.length - 1\" class=\"cui-wizard-nav-button cui-wizard-nav-next\"><span ng-if=!steps[currentStepIndex].onNextExists()>{{\'CUI_WIZARD_NEXT\' | translate}}</span> <span ng-if=steps[currentStepIndex].onNextExists()>{{\'CUI_WIZARD_SAVE_AND_NEXT\' | translate}}</span></cui-button><cui-button ng-click=finish($event) cui-type=primary ng-disabled=!canFinish() class=\"cui-wizard-nav-button cui-wizard-nav-finish\">{{\'CUI_WIZARD_FINISH\' | translate}}</cui-button><div class=cui-wizard-button-divider></div><cui-button ng-click=cancel($event) class=\"cui-wizard-nav-button cui-wizard-nav-cancel\">{{\'CUI_WIZARD_CANCEL\' | translate}}</cui-button></div></div></div></div>");
$templateCache.put("__cui/wizardStep.html","<section class=cui-wizard-step ng-transclude ng-show=isCurrentStep()></section>");
$templateCache.put("__cui/alertService.html","<cui-alert dismissable=dismissable icon={{icon}} label={{label}} cui-type={{type}} destroy-after={{destroyAfter}} dismissable=dismissable><span ng-bind-html=content></span></cui-alert>");
$templateCache.put("__cui/loading.html","<cui-spinner color={{spinnerColor}} size=75px></cui-spinner><p translate=CUI_LOADING_TEXT></p>");
$templateCache.put("__cui/modal-bare.html","<div ng-include=modal.templateUrl></div>");
$templateCache.put("__cui/modal-confirm.html","<header><cui-icon icon={{title.icon}} color={{title.iconColor}}></cui-icon>&nbsp;{{title.label}}</header><div class=cui-modal-message><div ng-include=modal.templateUrl></div><cui-template content=modal.template></cui-template></div><footer><menu><cui-button ng-repeat=\"button in buttons\" ng-click=button.action(button.value) cui-type={{button.cuiType}} cui-focus-when={{button.focus}}>{{button.label}}</cui-button></menu></footer>");
$templateCache.put("__cui/modal-dismiss.html","<span class=cui-remove ng-click=modal.hide()><cui-icon icon=remove></cui-icon></span><div ng-include=modal.templateUrl></div><cui-template content=modal.template></cui-template>");
$templateCache.put("__cui/modal.html","<div class=cui-modal-wrapper><div ng-if=\"modal.backdrop && modal.visible\" class=cui-modal-backdrop ng-click=\"!modal.static && modal.hide()\"></div><div ng-class=modal.classes class=cui-modal ng-include=modal.typeTemplateUrl></div></div>");}]);