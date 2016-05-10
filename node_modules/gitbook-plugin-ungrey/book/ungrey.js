require(['gitbook', 'jQuery'], function(gitbook, $) {
    gitbook.events.bind('page.change', function() {
        // Find summary items that have an inactive title (span) and subitems (ul)
        $('ul.summary li>span+ul').prev().css({
            // Make the span fully opaque and show as clickable
            opacity: 1,
            cursor: 'pointer'
        }).click(function() {
            // When the span is clicked, click the first link inside this chapter instead
            $(this).parent().find('a')[0].click();
        })
    });
});
