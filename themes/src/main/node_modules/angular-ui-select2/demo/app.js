
var app = angular.module('angular-ui-select2-demo', ['ui.select2']);

app.controller('MainCtrl', function ($scope, $element) {

  var states = [
    { text: 'Alaskan/Hawaiian Time Zone', children: [
      { id: 'AK', text: 'Alaska' },
      { id: 'HI', text: 'Hawaii' }
    ]},
    { text: 'Pacific Time Zone', children: [
      { id: 'CA', text: 'California' },
      { id: 'NV', text: 'Nevada' },
      { id: 'OR', text: 'Oregon' },
      { id: 'WA', text: 'Washington' }
    ]},
    { text: 'Mountain Time Zone', children: [
      { id: 'AZ', text: 'Arizona' },
      { id: 'CO', text: 'Colorado' },
      { id: 'ID', text: 'Idaho' },
      { id: 'MT', text: 'Montana' },
      { id: 'NE', text: 'Nebraska' },
      { id: 'NM', text: 'New Mexico' },
      { id: 'ND', text: 'North Dakota' },
      { id: 'UT', text: 'Utah' },
      { id: 'WY', text: 'Wyoming' }
    ]},
    { text: 'Central Time Zone', children: [
      { id: 'AL', text: 'Alabama' },
      { id: 'AR', text: 'Arkansas' },
      { id: 'IL', text: 'Illinois' },
      { id: 'IA', text: 'Iowa' },
      { id: 'KS', text: 'Kansas' },
      { id: 'KY', text: 'Kentucky' },
      { id: 'LA', text: 'Louisiana' },
      { id: 'MN', text: 'Minnesota' },
      { id: 'MS', text: 'Mississippi' },
      { id: 'MO', text: 'Missouri' },
      { id: 'OK', text: 'Oklahoma' },
      { id: 'SD', text: 'South Dakota' },
      { id: 'TX', text: 'Texas' },
      { id: 'TN', text: 'Tennessee' },
      { id: 'WI', text: 'Wisconsin' }
    ]},
    { text: 'Eastern Time Zone', children: [
      { id: 'CT', text: 'Connecticut' },
      { id: 'DE', text: 'Delaware' },
      { id: 'FL', text: 'Florida' },
      { id: 'GA', text: 'Georgia' },
      { id: 'IN', text: 'Indiana' },
      { id: 'ME', text: 'Maine' },
      { id: 'MD', text: 'Maryland' },
      { id: 'MA', text: 'Massachusetts' },
      { id: 'MI', text: 'Michigan' },
      { id: 'NH', text: 'New Hampshire' },
      { id: 'NJ', text: 'New Jersey' },
      { id: 'NY', text: 'New York' },
      { id: 'NC', text: 'North Carolina' },
      { id: 'OH', text: 'Ohio' },
      { id: 'PA', text: 'Pennsylvania' },
      { id: 'RI', text: 'Rhode Island' },
      { id: 'SC', text: 'South Carolina' },
      { id: 'VT', text: 'Vermont' },
      { id: 'VA', text: 'Virginia' },
      { id: 'WV', text: 'West Virginia' }
    ]}
  ];

  function findState(id) {
    for (var i=0; i<states.length; i++) {
      for (var j=0; j<states[i].children.length; j++) {
        if (states[i].children[j].id == id) {
          return states[i].children[j];
        }
      }
    }
  }

  $scope.multi2Value = ['CO', 'WA'];

  $scope.multi = {
    multiple: true,
    query: function (query) {
      query.callback({ results: states });
    },
    initSelection: function(element, callback) {
      var val = $(element).select2('val'),
        results = [];
      for (var i=0; i<val.length; i++) {
        results.push(findState(val[i]));
      }
      callback(results);
    }
  };

  $scope.placeholders = {
    placeholder: "Select a State"
  };

  $scope.array = {
    data: [{id:0,text:'enhancement'},{id:1,text:'bug'},{id:2,text:'duplicate'},{id:3,text:'invalid'},{id:4,text:'wontfix'}]
  };

  $scope.arrayAsync = {
    query: function (query) {
      query.callback({ results: states });
    },
    initSelection: function(element, callback) {
      var val = $(element).select2('val');
      return callback(findState(val));
    }
  };

});
