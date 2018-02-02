var $ = window.jQuery

var $window = $(window)
var sectionTop = $('.top').outerHeight() + 20
var $createDestroy = $('#switch-create-destroy')

function capitalize (string) {
  return string.charAt(0).toUpperCase() + string.slice(1)
}

window.hljs.initHighlightingOnLoad()
$(function () {
  $('a[href*=\'#\']').on('click', function (event) {
    event.preventDefault()
    var $target = $($(this).attr('href').slice('#'))
    if ($target.length) {
      $window.scrollTop($target.offset().top - sectionTop)
    }
  })
  $('input[type="checkbox"], input[type="radio"]')
    .not('[data-switch-no-init]')
    .bootstrapSwitch()

  $('[data-switch-get]').on('click', function () {
    var type = $(this).data('switch-get')
    window.alert($('#switch-' + type).bootstrapSwitch(type))
  })
  $('[data-switch-set]').on('click', function () {
    var type
    type = $(this).data('switch-set')
    $('#switch-' + type).bootstrapSwitch(type, $(this).data('switch-value'))
  })
  $('[data-switch-toggle]').on('click', function () {
    var type = $(this).data('switch-toggle')
    $('#switch-' + type).bootstrapSwitch('toggle' + capitalize(type))
  })
  $('[data-switch-set-value]').on('input', function (event) {
    var type, value
    event.preventDefault()
    type = $(this).data('switch-set-value')
    value = $.trim($(this).val())
    if ($(this).data('value') === value) {
      return
    }
    $('#switch-' + type).bootstrapSwitch(type, value)
  })
  $('[data-switch-create-destroy]').on('click', function () {
    var isSwitch
    isSwitch = $createDestroy.data('bootstrap-switch')
    $createDestroy.bootstrapSwitch((isSwitch ? 'destroy' : null))
    $(this).button((isSwitch ? 'reset' : 'destroy'))
  })
  $('#confirm').bootstrapSwitch({
    size: 'large',
    onSwitchChange: function (event, state) {
      event.preventDefault()
      return console.log(state, event.isDefaultPrevented())
    }
  })
})
