/* see test.html for example matchHeight usage */

/* testing page code only, you wont need this! */

(function() {

    $(function() {
        bindTestOptions();
    });

    var bindTestOptions = function() {
        resetTestOptions();
        $('.option').change(resetTestOptions);
    };

    var resetTestOptions = function() {
        // update test options
        $('.option').each(function() {
            var $this = $(this);
            $('body').toggleClass($this.val(), $this.prop('checked'));
        });

        // update byRow option
        var byRow = $('body').hasClass('test-rows');
        $.each($.fn.matchHeight._groups, function() {
            this.options.byRow = byRow;
        });

        // update all heights
        $.fn.matchHeight._update();
    };

})();