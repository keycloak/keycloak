'use strict';


ApplicationConfiguration.registerModule('dibSvc');

(function() {
  angular.module('dibSvc').service('locationService', function($window) {
  	var emailAddress = "";
    return {
      setEmailAddress: function(value) {
        return emailAddress = value;
      },
      getAuthUrl: function() {
        var domain, domainList;
        domainList = emailAddress.split("@");
        domain = domainList[1];
        if (domain == null) {
        	if (domainList[0] == null)
          	{
          		domain = domainList[0];
          	}
        }
        return $window.domainMap[domain];
      }
    };
  });

}).call(this);

(function() {
  angular.module('dibSvc').service('messageService', function() {
  	var messageText = '';
    return {
      setText: function(value) {
        return messageText = value;
      },
      getText: function() {
      	return messageText;
      },
      showMessage: function() {
        return messageText.length() > 0;
      }
    };
  });

}).call(this);


