$(document).ready(function(){
// Bootstrap js
	bootstrapTooltip();
	bootstrapTokenfield();
// Useful js
	fieldsetCollapseExpand();
	selectTableRow();
	associateRole();
// Provisional js
	enableDisableButton();
    advancedSearch();
	chosen();
});


// Enabling tooltip
bootstrapTooltip = function() {
	$('.tooltipTrigger').tooltip({
		placement: 'top'
	});
	$('.tooltipRightTrigger').tooltip({
		placement: 'right'
	});
	$('.tooltipBottomTrigger').tooltip({
		placement: 'bottom'
	});
};

// Enabling tokenfield
bootstrapTokenfield = function() {
    $('.tokenfield').tokenfield();
};


// Collapse and Expand Fieldset
fieldsetCollapseExpand = function() {
	$('legend').click(function() {
		$(this).toggleClass('collapsed');
		$(this).find('.toggle-icons').toggleClass('icon-collapse').toggleClass('icon-expand');
		$(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
        $(this).parent().find('.form-group').toggleClass('hidden');
	});
};

// Select Table Row
selectTableRow = function() {
    $('tbody.selectable-row td').click(function() {
        $(this).parent().toggleClass('selected');
    });
};

// Associate Role 
associateRole = function() {
    $('.token-cell button').click(function() {
        $(this).addClass('hidden');
        $(this).parent().find('select').removeClass('hidden');    
        return false;
    });    
};

// Enable Disable Button
enableDisableButton = function() {
    $('tbody td').click(function() {
        $('button.remove').toggleClass('disabled');        
    });
};

// Show / Hide Advanced Search
advancedSearch = function() {
    $('.advanced-search-link').click(function() {
        $(this).parent().find('.tooltip-box').toggleClass('hidden');
        return false;
    });
};

// Provisional Chosen
chosen = function() {
    $('.chosen').select2();    
};



