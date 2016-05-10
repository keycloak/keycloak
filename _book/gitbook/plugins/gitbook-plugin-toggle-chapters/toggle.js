require(["gitbook", "jQuery"], function(gitbook, $) {

  function expand(chapter) {
    chapter.show();
    if (chapter.parent().attr('class') != 'summary'
        && chapter.parent().attr('class') != 'book-summary'
      && chapter.length != 0
       ) {
         expand(chapter.parent());
       }
  }

  gitbook.events.bind("page.change", function() {
    $('li.chapter').children('ul.articles').hide();
    $chapter = $('li.chapter.active');
    $children = $chapter.children('ul.articles');
    $parent = $chapter.parent();
    $siblings = $chapter.siblings().children('ul.articles');

    expand($chapter);

    if ($children.length > 0) {
      $children.show();
    }
  });

});
