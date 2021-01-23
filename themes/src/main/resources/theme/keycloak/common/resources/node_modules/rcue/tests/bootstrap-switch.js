(function() {
  var $confirm;

  $confirm = null;

  $(function() {
    var $createDestroy, $window, sectionTop;
    $window = $(window);
    sectionTop = $(".top").outerHeight() + 20;
    $createDestroy = $("#switch-create-destroy");
    // hljs.initHighlightingOnLoad();
    $("a[href*=\"#\"]").on("click", function(event) {
      var $target;
      event.preventDefault();
      $target = $($(this).attr("href").slice("#"));
      if ($target.length) {
        return $window.scrollTop($target.offset().top - sectionTop);
      }
    });
    $("input[type=\"checkbox\"], input[type=\"radio\"]").not("[data-switch-no-init]").bootstrapSwitch();
    $("[data-switch-get]").on("click", function() {
      var type;
      type = $(this).data("switch-get");
      return alert($("#switch-" + type).bootstrapSwitch(type));
    });
    $("[data-switch-set]").on("click", function() {
      var type;
      type = $(this).data("switch-set");
      return $("#switch-" + type).bootstrapSwitch(type, $(this).data("switch-value"));
    });
    $("[data-switch-toggle]").on("click", function() {
      var type;
      type = $(this).data("switch-toggle");
      return $("#switch-" + type).bootstrapSwitch("toggle" + type.charAt(0).toUpperCase() + type.slice(1));
    });
    $("[data-switch-set-value]").on("input", function(event) {
      var type, value;
      event.preventDefault();
      type = $(this).data("switch-set-value");
      value = $.trim($(this).val());
      if ($(this).data("value") === value) {
        return;
      }
      return $("#switch-" + type).bootstrapSwitch(type, value);
    });
    $("[data-switch-create-destroy]").on("click", function() {
      var isSwitch;
      isSwitch = $createDestroy.data("bootstrap-switch");
      $createDestroy.bootstrapSwitch((isSwitch ? "destroy" : null));
      return $(this).button((isSwitch ? "reset" : "destroy"));
    });
    return $confirm = $("#confirm").bootstrapSwitch({
      size: "large",
      onSwitchChange: function(event, state) {
        event.preventDefault();
        return console.log(state, event.isDefaultPrevented());
      }
    });
  });

}).call(this);
