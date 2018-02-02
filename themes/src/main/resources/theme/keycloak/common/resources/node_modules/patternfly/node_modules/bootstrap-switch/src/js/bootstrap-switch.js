import jquery from 'jquery'

const $ = jquery || window.jQuery || window.$

class BootstrapSwitch {
  constructor (element, options = {}) {
    this.$element = $(element)
    this.options = $.extend(
      {},
      $.fn.bootstrapSwitch.defaults,
      this._getElementOptions(),
      options
    )
    this.prevOptions = {}
    this.$wrapper = $('<div>', {
      class: () => {
        const classes = []
        classes.push(this.options.state ? 'on' : 'off')
        if (this.options.size) {
          classes.push(this.options.size)
        }
        if (this.options.disabled) {
          classes.push('disabled')
        }
        if (this.options.readonly) {
          classes.push('readonly')
        }
        if (this.options.indeterminate) {
          classes.push('indeterminate')
        }
        if (this.options.inverse) {
          classes.push('inverse')
        }
        if (this.$element.attr('id')) {
          classes.push(`id-${this.$element.attr('id')}`)
        }
        return classes
          .map(this._getClass.bind(this))
          .concat([this.options.baseClass], this._getClasses(this.options.wrapperClass))
          .join(' ')
      }
    })
    this.$container = $('<div>', { class: this._getClass('container') })
    this.$on = $('<span>', {
      html: this.options.onText,
      class: `${this._getClass('handle-on')} ${this._getClass(this.options.onColor)}`
    })
    this.$off = $('<span>', {
      html: this.options.offText,
      class: `${this._getClass('handle-off')} ${this._getClass(this.options.offColor)}`
    })
    this.$label = $('<span>', {
      html: this.options.labelText,
      class: this._getClass('label')
    })

    this.$element.on('init.bootstrapSwitch', this.options.onInit.bind(this, element))
    this.$element.on('switchChange.bootstrapSwitch', (...args) => {
      if (this.options.onSwitchChange.apply(element, args) === false) {
        if (this.$element.is(':radio')) {
          $(`[name="${this.$element.attr('name')}"]`).trigger('previousState.bootstrapSwitch', true)
        } else {
          this.$element.trigger('previousState.bootstrapSwitch', true)
        }
      }
    })

    this.$container = this.$element.wrap(this.$container).parent()
    this.$wrapper = this.$container.wrap(this.$wrapper).parent()
    this.$element
      .before(this.options.inverse ? this.$off : this.$on)
      .before(this.$label)
      .before(this.options.inverse ? this.$on : this.$off)

    if (this.options.indeterminate) {
      this.$element.prop('indeterminate', true)
    }

    this._init()
    this._elementHandlers()
    this._handleHandlers()
    this._labelHandlers()
    this._formHandler()
    this._externalLabelHandler()
    this.$element.trigger('init.bootstrapSwitch', this.options.state)
  }

  setPrevOptions () {
    this.prevOptions = { ...this.options }
  }

  state (value, skip) {
    if (typeof value === 'undefined') { return this.options.state }
    if (
      (this.options.disabled || this.options.readonly) ||
      (this.options.state && !this.options.radioAllOff && this.$element.is(':radio'))
    ) { return this.$element }
    if (this.$element.is(':radio')) {
      $(`[name="${this.$element.attr('name')}"]`).trigger('setPreviousOptions.bootstrapSwitch')
    } else {
      this.$element.trigger('setPreviousOptions.bootstrapSwitch')
    }
    if (this.options.indeterminate) {
      this.indeterminate(false)
    }
    this.$element
      .prop('checked', Boolean(value))
      .trigger('change.bootstrapSwitch', skip)
    return this.$element
  }

  toggleState (skip) {
    if (this.options.disabled || this.options.readonly) { return this.$element }
    if (this.options.indeterminate) {
      this.indeterminate(false)
      return this.state(true)
    } else {
      return this.$element.prop('checked', !this.options.state).trigger('change.bootstrapSwitch', skip)
    }
  }

  size (value) {
    if (typeof value === 'undefined') { return this.options.size }
    if (this.options.size != null) {
      this.$wrapper.removeClass(this._getClass(this.options.size))
    }
    if (value) {
      this.$wrapper.addClass(this._getClass(value))
    }
    this._width()
    this._containerPosition()
    this.options.size = value
    return this.$element
  }

  animate (value) {
    if (typeof value === 'undefined') { return this.options.animate }
    if (this.options.animate === Boolean(value)) { return this.$element }
    return this.toggleAnimate()
  }

  toggleAnimate () {
    this.options.animate = !this.options.animate
    this.$wrapper.toggleClass(this._getClass('animate'))
    return this.$element
  }

  disabled (value) {
    if (typeof value === 'undefined') { return this.options.disabled }
    if (this.options.disabled === Boolean(value)) { return this.$element }
    return this.toggleDisabled()
  }

  toggleDisabled () {
    this.options.disabled = !this.options.disabled
    this.$element.prop('disabled', this.options.disabled)
    this.$wrapper.toggleClass(this._getClass('disabled'))
    return this.$element
  }

  readonly (value) {
    if (typeof value === 'undefined') { return this.options.readonly }
    if (this.options.readonly === Boolean(value)) { return this.$element }
    return this.toggleReadonly()
  }

  toggleReadonly () {
    this.options.readonly = !this.options.readonly
    this.$element.prop('readonly', this.options.readonly)
    this.$wrapper.toggleClass(this._getClass('readonly'))
    return this.$element
  }

  indeterminate (value) {
    if (typeof value === 'undefined') { return this.options.indeterminate }
    if (this.options.indeterminate === Boolean(value)) { return this.$element }
    return this.toggleIndeterminate()
  }

  toggleIndeterminate () {
    this.options.indeterminate = !this.options.indeterminate
    this.$element.prop('indeterminate', this.options.indeterminate)
    this.$wrapper.toggleClass(this._getClass('indeterminate'))
    this._containerPosition()
    return this.$element
  }

  inverse (value) {
    if (typeof value === 'undefined') { return this.options.inverse }
    if (this.options.inverse === Boolean(value)) { return this.$element }
    return this.toggleInverse()
  }

  toggleInverse () {
    this.$wrapper.toggleClass(this._getClass('inverse'))
    const $on = this.$on.clone(true)
    const $off = this.$off.clone(true)
    this.$on.replaceWith($off)
    this.$off.replaceWith($on)
    this.$on = $off
    this.$off = $on
    this.options.inverse = !this.options.inverse
    return this.$element
  }

  onColor (value) {
    if (typeof value === 'undefined') { return this.options.onColor }
    if (this.options.onColor) {
      this.$on.removeClass(this._getClass(this.options.onColor))
    }
    this.$on.addClass(this._getClass(value))
    this.options.onColor = value
    return this.$element
  }

  offColor (value) {
    if (typeof value === 'undefined') { return this.options.offColor }
    if (this.options.offColor) {
      this.$off.removeClass(this._getClass(this.options.offColor))
    }
    this.$off.addClass(this._getClass(value))
    this.options.offColor = value
    return this.$element
  }

  onText (value) {
    if (typeof value === 'undefined') { return this.options.onText }
    this.$on.html(value)
    this._width()
    this._containerPosition()
    this.options.onText = value
    return this.$element
  }

  offText (value) {
    if (typeof value === 'undefined') { return this.options.offText }
    this.$off.html(value)
    this._width()
    this._containerPosition()
    this.options.offText = value
    return this.$element
  }

  labelText (value) {
    if (typeof value === 'undefined') { return this.options.labelText }
    this.$label.html(value)
    this._width()
    this.options.labelText = value
    return this.$element
  }

  handleWidth (value) {
    if (typeof value === 'undefined') { return this.options.handleWidth }
    this.options.handleWidth = value
    this._width()
    this._containerPosition()
    return this.$element
  }

  labelWidth (value) {
    if (typeof value === 'undefined') { return this.options.labelWidth }
    this.options.labelWidth = value
    this._width()
    this._containerPosition()
    return this.$element
  }

  baseClass (value) {
    return this.options.baseClass
  }

  wrapperClass (value) {
    if (typeof value === 'undefined') { return this.options.wrapperClass }
    if (!value) {
      value = $.fn.bootstrapSwitch.defaults.wrapperClass
    }
    this.$wrapper.removeClass(this._getClasses(this.options.wrapperClass).join(' '))
    this.$wrapper.addClass(this._getClasses(value).join(' '))
    this.options.wrapperClass = value
    return this.$element
  }

  radioAllOff (value) {
    if (typeof value === 'undefined') { return this.options.radioAllOff }
    const val = Boolean(value)
    if (this.options.radioAllOff === val) { return this.$element }
    this.options.radioAllOff = val
    return this.$element
  }

  onInit (value) {
    if (typeof value === 'undefined') { return this.options.onInit }
    if (!value) {
      value = $.fn.bootstrapSwitch.defaults.onInit
    }
    this.options.onInit = value
    return this.$element
  }

  onSwitchChange (value) {
    if (typeof value === 'undefined') {
      return this.options.onSwitchChange
    }
    if (!value) {
      value = $.fn.bootstrapSwitch.defaults.onSwitchChange
    }
    this.options.onSwitchChange = value
    return this.$element
  }

  destroy () {
    const $form = this.$element.closest('form')
    if ($form.length) {
      $form.off('reset.bootstrapSwitch').removeData('bootstrap-switch')
    }
    this.$container
      .children()
      .not(this.$element)
      .remove()
    this.$element
      .unwrap()
      .unwrap()
      .off('.bootstrapSwitch')
      .removeData('bootstrap-switch')
    return this.$element
  }

  _getElementOptions () {
    return {
      state: this.$element.is(':checked'),
      size: this.$element.data('size'),
      animate: this.$element.data('animate'),
      disabled: this.$element.is(':disabled'),
      readonly: this.$element.is('[readonly]'),
      indeterminate: this.$element.data('indeterminate'),
      inverse: this.$element.data('inverse'),
      radioAllOff: this.$element.data('radio-all-off'),
      onColor: this.$element.data('on-color'),
      offColor: this.$element.data('off-color'),
      onText: this.$element.data('on-text'),
      offText: this.$element.data('off-text'),
      labelText: this.$element.data('label-text'),
      handleWidth: this.$element.data('handle-width'),
      labelWidth: this.$element.data('label-width'),
      baseClass: this.$element.data('base-class'),
      wrapperClass: this.$element.data('wrapper-class')
    }
  }

  _width () {
    const $handles = this.$on
      .add(this.$off)
      .add(this.$label)
      .css('width', '')
    const handleWidth = this.options.handleWidth === 'auto'
      ? Math.round(Math.max(this.$on.width(), this.$off.width()))
      : this.options.handleWidth
    $handles.width(handleWidth)
    this.$label.width((index, width) => {
      if (this.options.labelWidth !== 'auto') { return this.options.labelWidth }
      if (width < handleWidth) { return handleWidth }
      return width
    })
    this._handleWidth = this.$on.outerWidth()
    this._labelWidth = this.$label.outerWidth()
    this.$container.width((this._handleWidth * 2) + this._labelWidth)
    return this.$wrapper.width(this._handleWidth + this._labelWidth)
  }

  _containerPosition (state = this.options.state, callback) {
    this.$container.css('margin-left', () => {
      const values = [0, `-${this._handleWidth}px`]
      if (this.options.indeterminate) {
        return `-${this._handleWidth / 2}px`
      }
      if (state) {
        if (this.options.inverse) {
          return values[1]
        } else {
          return values[0]
        }
      } else {
        if (this.options.inverse) {
          return values[0]
        } else {
          return values[1]
        }
      }
    })
  }

  _init () {
    const init = () => {
      this.setPrevOptions()
      this._width()
      this._containerPosition()
      setTimeout(() => {
        if (this.options.animate) {
          return this.$wrapper.addClass(this._getClass('animate'))
        }
      }, 50)
    }
    if (this.$wrapper.is(':visible')) {
      init()
      return
    }
    const initInterval = window.setInterval(() => {
      if (this.$wrapper.is(':visible')) {
        init()
        return window.clearInterval(initInterval)
      }
    }, 50)
  }

  _elementHandlers () {
    return this.$element.on({
      'setPreviousOptions.bootstrapSwitch': this.setPrevOptions.bind(this),

      'previousState.bootstrapSwitch': () => {
        this.options = this.prevOptions
        if (this.options.indeterminate) {
          this.$wrapper.addClass(this._getClass('indeterminate'))
        }
        this.$element
          .prop('checked', this.options.state)
          .trigger('change.bootstrapSwitch', true)
      },

      'change.bootstrapSwitch': (event, skip) => {
        event.preventDefault()
        event.stopImmediatePropagation()
        const state = this.$element.is(':checked')
        this._containerPosition(state)
        if (state === this.options.state) {
          return
        }
        this.options.state = state
        this.$wrapper
          .toggleClass(this._getClass('off'))
          .toggleClass(this._getClass('on'))
        if (!skip) {
          if (this.$element.is(':radio')) {
            $(`[name="${this.$element.attr('name')}"]`)
              .not(this.$element)
              .prop('checked', false)
              .trigger('change.bootstrapSwitch', true)
          }
          this.$element.trigger('switchChange.bootstrapSwitch', [state])
        }
      },

      'focus.bootstrapSwitch': event => {
        event.preventDefault()
        this.$wrapper.addClass(this._getClass('focused'))
      },

      'blur.bootstrapSwitch': event => {
        event.preventDefault()
        this.$wrapper.removeClass(this._getClass('focused'))
      },

      'keydown.bootstrapSwitch': event => {
        if (!event.which || this.options.disabled || this.options.readonly) {
          return
        }
        if (event.which === 37 || event.which === 39) {
          event.preventDefault()
          event.stopImmediatePropagation()
          this.state(event.which === 39)
        }
      }
    })
  }

  _handleHandlers () {
    this.$on.on('click.bootstrapSwitch', event => {
      event.preventDefault()
      event.stopPropagation()
      this.state(false)
      return this.$element.trigger('focus.bootstrapSwitch')
    })
    return this.$off.on('click.bootstrapSwitch', event => {
      event.preventDefault()
      event.stopPropagation()
      this.state(true)
      return this.$element.trigger('focus.bootstrapSwitch')
    })
  }

  _labelHandlers () {
    const handlers = {
      click (event) { event.stopPropagation() },

      'mousedown.bootstrapSwitch touchstart.bootstrapSwitch': event => {
        if (this._dragStart || this.options.disabled || this.options.readonly) {
          return
        }
        event.preventDefault()
        event.stopPropagation()
        this._dragStart = (event.pageX || event.originalEvent.touches[0].pageX) - parseInt(this.$container.css('margin-left'), 10)
        if (this.options.animate) {
          this.$wrapper.removeClass(this._getClass('animate'))
        }
        this.$element.trigger('focus.bootstrapSwitch')
      },

      'mousemove.bootstrapSwitch touchmove.bootstrapSwitch': event => {
        if (this._dragStart == null) { return }
        const difference = (event.pageX || event.originalEvent.touches[0].pageX) - this._dragStart
        event.preventDefault()
        if (difference < -this._handleWidth || difference > 0) { return }
        this._dragEnd = difference
        this.$container.css('margin-left', `${this._dragEnd}px`)
      },

      'mouseup.bootstrapSwitch touchend.bootstrapSwitch': event => {
        if (!this._dragStart) { return }
        event.preventDefault()
        if (this.options.animate) {
          this.$wrapper.addClass(this._getClass('animate'))
        }
        if (this._dragEnd) {
          const state = this._dragEnd > -(this._handleWidth / 2)
          this._dragEnd = false
          this.state(this.options.inverse ? !state : state)
        } else {
          this.state(!this.options.state)
        }
        this._dragStart = false
      },

      'mouseleave.bootstrapSwitch': () => {
        this.$label.trigger('mouseup.bootstrapSwitch')
      }
    }
    this.$label.on(handlers)
  }

  _externalLabelHandler () {
    const $externalLabel = this.$element.closest('label')
    $externalLabel.on('click', event => {
      event.preventDefault()
      event.stopImmediatePropagation()
      if (event.target === $externalLabel[0]) {
        this.toggleState()
      }
    })
  }

  _formHandler () {
    const $form = this.$element.closest('form')
    if ($form.data('bootstrap-switch')) {
      return
    }
    $form
      .on('reset.bootstrapSwitch', () => {
        window.setTimeout(() => {
          $form.find('input')
            .filter(function () { return $(this).data('bootstrap-switch') })
            .each(function () { return $(this).bootstrapSwitch('state', this.checked) })
        }, 1)
      })
      .data('bootstrap-switch', true)
  }

  _getClass (name) {
    return `${this.options.baseClass}-${name}`
  }

  _getClasses (classes) {
    if (!$.isArray(classes)) {
      return [this._getClass(classes)]
    }
    return classes.map(this._getClass.bind(this))
  }
}

$.fn.bootstrapSwitch = function (option, ...args) {
  function reducer (ret, next) {
    const $this = $(next)
    const existingData = $this.data('bootstrap-switch')
    const data = existingData || new BootstrapSwitch(next, option)
    if (!existingData) {
      $this.data('bootstrap-switch', data)
    }
    if (typeof option === 'string') {
      return data[option].apply(data, args)
    }
    return ret
  }
  return Array.prototype.reduce.call(this, reducer, this)
}
$.fn.bootstrapSwitch.Constructor = BootstrapSwitch
$.fn.bootstrapSwitch.defaults = {
  state: true,
  size: null,
  animate: true,
  disabled: false,
  readonly: false,
  indeterminate: false,
  inverse: false,
  radioAllOff: false,
  onColor: 'primary',
  offColor: 'default',
  onText: 'ON',
  offText: 'OFF',
  labelText: '&nbsp',
  handleWidth: 'auto',
  labelWidth: 'auto',
  baseClass: 'bootstrap-switch',
  wrapperClass: 'wrapper',
  onInit: () => {},
  onSwitchChange: () => {}
}
