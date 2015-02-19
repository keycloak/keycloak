'use strict';

ApplicationConfiguration.registerModule('dibCtrl');

(function() {
	angular.module('dibCtrl').controller('LoginController', [
		function() {
			this.text = "Login Controller";
		}
	]);
}).call(this);

(function() {
  angular.module('dibCtrl').controller('EmailFormController', [
    'locationService', '$cookies', 'messageService', function(locationService, $cookies, messageService) {
      this.emailAddress = $cookies.dibHrdEmailAddress;
      console.log("EmailAddress " + this.emailAddress);
      console.log("rememberMe " + this.rememberMe);
      this.onEmailAddressChange = function(value) {
        locationService.setEmailAddress(this.emailAddress);
        return $cookies.dibHrdEmailAddress = this.emailAddress;
      };
      this.emailSignInClick = function() {
        locationService.setEmailAddress(this.emailAddress);
        var authUrl = locationService.getAuthUrl();

        if (typeof(authUrl) !== 'undefined')
        {
            window.location = authUrl;
        }
        else
        {
        	messageService.setText('Unable to find SSO provider for '+emailAddress);
        }
        return false;
      };
    }
  ]);

}).call(this);

(function() {
	angular.module('dibCtrl').controller('MessageController', ['messageService',
		function(messageService) {
			this.messageText = messageService.getText();
			this.showText = messageService.showMessage;
			this.refreshText = function() {
				this.messageText = messageService.getText();
				this.showText = messageService.showMessage();
			}
		}
	]);
}).call(this);
