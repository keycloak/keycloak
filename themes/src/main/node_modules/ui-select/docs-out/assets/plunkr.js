angular.module('plunkr', [])
.factory('formPostData', ['$document', function ($document) {
  return function (url, newWindow, fields) {
    /**
     * If the form posts to target="_blank", pop-up blockers can cause it not to work.
     * If a user choses to bypass pop-up blocker one time and click the link, they will arrive at
     * a new default plnkr, not a plnkr with the desired template.  Given this undesired behavior,
     * some may still want to open the plnk in a new window by opting-in via ctrl+click.  The
     * newWindow param allows for this possibility.
     */
    var target = newWindow ? '_blank' : '_self';
    var form = angular.element('<form style="display: none;" method="post" action="' + url + '" target="' + target + '"></form>');
    angular.forEach(fields, function (value, name) {
      var input = angular.element('<input type="hidden" name="' + name + '">');
      input.attr('value', value);
      form.append(input);
    });
    $document.find('body').append(form);
    form[0].submit();
    form.remove();
  };
}])


.directive('plnkrOpener', ['$q', 'getExampleData', 'formPostData', function ($q, getExampleData, formPostData) {
  return {
    scope: {},
    bindToController: {
      'examplePath': '@'
    },
    controllerAs: 'plnkr',
    template: '<button ng-click="plnkr.open($event)" class="btn btn-info btn-sm plunk-btn"> <i class="glyphicon glyphicon-edit">&nbsp;</i> Edit in Plunker</button> ',
    controller: [function () {
      var ctrl = this;

      ctrl.prepareExampleData = function (examplePath) {
        if (ctrl.prepared) {
          return $q.when(ctrl.prepared);
        } else {
          return getExampleData(examplePath).then(function (data) {
            ctrl.prepared = data;
          });
        }
      };

      ctrl.open = function (clickEvent) {

        var newWindow = clickEvent.ctrlKey || clickEvent.metaKey;
        var postData = {
          'tags[0]': "angularjs",
          'tags[1]': "ui-select",
          'private': true
        };

        // Make sure the example data is available.
        // If an XHR must be made, this might break some pop-up blockers when
        // new window is requested
        ctrl.prepareExampleData(ctrl.examplePath)
          .then(function () {
            angular.forEach(ctrl.prepared, function (file) {
              postData['files[' + file.name + ']'] = file.content;
            });

            postData.description = "Angular ui-select http://github.com/angular-ui/ui-select/";

            formPostData('http://plnkr.co/edit/?p=preview', newWindow, postData);
          });
      };

      // Initialize the example data, so it's ready when clicking the open button.
      // Otherwise pop-up blockers will prevent a new window from opening
      ctrl.prepareExampleData(ctrl.examplePath);
    }]
  };
}])

.factory('getExampleData', ['$http', '$q', function ($http, $q) {
  return function (exampleFile) {
    // Load the manifest for the example
    var defaultFiles = {
      'demo.js': './assets/',
      'select.css': './dist',
      'select.js': './dist'
    };
    files = angular.copy(defaultFiles);
    files[exampleFile] = './';

    var filePromises = [];

    angular.forEach(files, function (folder, filename) {
      filePromises.push($http.get(folder + '/' + filename, { transformResponse: [], cache: true })
        .then(function (response) {

          var content = response.data;
          // Should only be one html (the example)
          if (filename.match(/.html$/)) {
            filename = "index.html";
            content = content.replace(/.\/assets\//g, './').replace(/.\/dist\//g, './');
          }

          return {
            name: filename,
            content: content
          };
        }));
    });

    return $q.all(filePromises);
  };
}]);
