$(document).ready(function(){
	feedback();
});

// Show Feedback
feedback = function() {
	$('.btn-primary').click(function() {
		$('.feedback').addClass('show');
        return false;
	});
	
	$('.login .btn-primary').click(function() {
        $('#username').addClass('error').focus();		
        $('#password').addClass('error');		
        $('#one-time-pswd').addClass('error');		
	});
	
	$('.register .btn-primary').click(function() {
        $('#email').addClass('error').focus();		
	});
};