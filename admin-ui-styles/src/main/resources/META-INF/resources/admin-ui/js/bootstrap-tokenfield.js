/* ============================================================
 * bootstrap-tokenfield.js v0.9.5
 * ============================================================
 *
 * Copyright 2013 Sliptree
 * ============================================================ */


!function ($) {

  "use strict"; // jshint ;_;


 /* TOKENFIELD PUBLIC CLASS DEFINITION
  * ============================== */

  var Tokenfield = function (element, options) {
    var _self = this

    this.$element = $(element)
    // Extend options
    this.options = $.extend({}, $.fn.tokenfield.defaults, { tokens: this.$element.val() }, options)    
    // Store original input width
    var elWidth = this.$element.width();
    // Move original input out of the way
    this.$element.css({
      'position': 'absolute',
      'left': '-10000px'
    }).prop('tabindex', -1);

    // Create a wrapper
    this.$wrapper = $('<div class="tokenfield form-control" />')
    if (this.$element.hasClass('input-lg')) this.$wrapper.addClass('input-lg')
    if (this.$element.hasClass('input-sm')) this.$wrapper.addClass('input-sm')

    // Create a new input
    this.$input = $('<input type="text" class="token-input" />')
                    .appendTo( this.$wrapper )
                    .prop('placeholder',  this.$element.prop('placeholder') )

    // Set up a copy helper to handle copy & paste
    this.$copyHelper = $('<input type="text" />').css({
      'position': 'absolute',
      'left': '-10000px'
    }).prop('tabindex', -1).prependTo( this.$wrapper )
    
    // If the input is in inline form, set width explicitly
    if (this.$element.parents('.form-inline').length) this.$wrapper.width( elWidth )

    // Set tokenfield disabled, if original input is disabled
    if (this.$element.prop('disabled')) {
      this.disable();
    }

    // Set up mirror for input auto-sizing
    this.$mirror = $('<span style="position:absolute; top:-999px; left:0; white-space:pre;"/>');
    this.$input.css('min-width', this.options.minWidth + 'px')
    $.each([
        'fontFamily', 
        'fontSize', 
        'fontWeight', 
        'fontStyle', 
        'letterSpacing', 
        'textTransform', 
        'wordSpacing', 
        'textIndent'
    ], function (i, val) {
        _self.$mirror[0].style[val] = _self.$input.css(val);
    });
    this.$mirror.appendTo( 'body' )

    // Insert tokenfield to HTML
    this.$wrapper.insertBefore( this.$element )
    this.$element.prependTo( this.$wrapper )
    
    this.setTokens(this.options.tokens)

    // Start listening to events
    this.listen()

    // Initialize autocomplete, if necessary
    if (this.options.autocomplete.source) {
      var autocompleteOptions = $.extend({}, this.options.autocomplete, {
        minLength: this.options.showAutocompleteOnFocus ? 0 : null,
        position: { my: "left top", at: "left bottom", of: this.$wrapper }
      })
      this.$input.autocomplete( autocompleteOptions )
    }
  }

  Tokenfield.prototype = {

    constructor: Tokenfield

  , createToken: function (attrs) {
      if (typeof attrs === 'string') {
        attrs = { value: attrs, label: attrs }
      }
      
      var _self = this
        , value = $.trim(attrs.value)
        , label = attrs.label.length ? $.trim(attrs.label) : value

      if (!value.length || !label.length || value.length < this.options.minLength) return

      if (!this.options.allowDuplicates && $.grep(this.getTokens(), function (token) {
        return token.value === value
      }).length) return

      // Allow changing token data before creating it
      var e = $.Event('beforeCreateToken')
      e.token = {
        value: value,
        label: label
      }
      this.$element.trigger(e)

      var token = $('<div class="token" />')
            .attr('data-value', e.token.value)
            .append('<span class="token-label" />')
            .append('<a href="#" class="close" tabindex="-1">&times;</a>')

      this.$input.before( token )
      this.$input.css('width', this.options.minWidth + 'px')

      var tokenLabel = token.find('.token-label')
        , closeButton = token.find('.close')

      // Determine maximum possible token label width
      if (!this.maxTokenWidth) {
        this.maxTokenWidth =
          this.$wrapper.width() - closeButton.outerWidth() - 
          parseInt(closeButton.css('margin-left'), 10) -
          parseInt(closeButton.css('margin-right'), 10) -
          parseInt(token.css('border-left-width'), 10) -
          parseInt(token.css('border-right-width'), 10) -
          parseInt(token.css('padding-left'), 10) -
          parseInt(token.css('padding-right'), 10)
          parseInt(tokenLabel.css('border-left-width'), 10) -
          parseInt(tokenLabel.css('border-right-width'), 10) -
          parseInt(tokenLabel.css('padding-left'), 10) -
          parseInt(tokenLabel.css('padding-right'), 10)
          parseInt(tokenLabel.css('margin-left'), 10) -
          parseInt(tokenLabel.css('margin-right'), 10)
      }

      tokenLabel
        .text(e.token.label)
        .css('max-width', this.maxTokenWidth)

      // Listen to events
      token
        .on('mousedown',  function (e) {
          if (_self.disabled) return false;
          _self.preventDeactivation = true
        })
        .on('click',    function (e) {
          if (_self.disabled) return false;
          _self.preventDeactivation = false

          if (e.ctrlKey || e.metaKey) {
            e.preventDefault()
            return _self.toggle( token )
          }
          
          _self.activate( token, e.shiftKey, e.shiftKey )          
        })
        .on('dblclick', function (e) {
          if (_self.disabled) return false;
          _self.edit( token )
        })

      closeButton
          .on('click',  $.proxy(this.remove, this))

      var afterE = $.Event('afterCreateToken')
      afterE.token = e.token
      afterE.relatedTarget = token
      this.$element.trigger(afterE)

      this.$element.val( this.getTokensList() ).trigger('change')

      this.update()

      return this.$input.get(0)
    }    

  , setTokens: function (tokens, add) {
      if (!tokens) return

      if (!add) this.$wrapper.find('.token').remove()

      if (typeof tokens === 'string') {
        tokens = tokens.split(',')
      }

      var _self = this
      $.each(tokens, function (i, token) {
        _self.createToken(token)
      })

      return this.$input.get(0)
    }

  , getTokens: function(active) {
      var tokens = []
        , activeClass = active ? '.active' : '' // get active tokens only
      this.$wrapper.find( '.token' + activeClass ).each( function() {
        tokens.push({
          value: $(this).data('value') || $(this).find('.token-label').text(),
          label: $(this).find('.token-label').text()
        })
      })
      return tokens
  }

  , getTokensList: function(active) {
      return $.map( this.getTokens(active), function (token) {
        return token.value
      }).join(', ')
  }

  , getInput: function() {
    return this.$input.val()
  }

  , listen: function () {
      var _self = this

      this.$wrapper
        .on('click',    $.proxy(this.focusInput, this))

      this.$input
        .on('focus',    $.proxy(this.focus, this))
        .on('blur',     $.proxy(this.blur, this))
        .on('paste',    $.proxy(this.paste, this))
        .on('keydown',  $.proxy(this.keydown, this))
        .on('keypress', $.proxy(this.keypress, this))
        .on('keyup',    $.proxy(this.keyup, this))

      this.$copyHelper
        .on('focus',    $.proxy(this.focus, this))
        .on('blur',     $.proxy(this.blur, this))        
        .on('keydown',  $.proxy(this.keydown, this))
        .on('keyup',    $.proxy(this.keyup, this))

      // Secondary listeners for input width calculation
      this.$input.on('keydown, keypress, keyup',  $.proxy(this.update, this))

      this.$input
        .on('autocompletecreate', function() {
          // Set minimum autocomplete menu width
          var minWidth = _self.$wrapper.width() +
              parseInt(_self.$wrapper.css('padding-right'), 10) +
              parseInt(_self.$wrapper.css('border-right-width'), 10)
          $(this).data('uiAutocomplete').menu.element.css( 'min-width', minWidth + 'px' )
        })
        .on('autocompleteopen', function() {
          _self.$input.data('autocomplete-open', true)
        })
        .on('autocompleteclose', function() {
          _self.$input.data('autocomplete-open', false)
        })
        .on('autocompleteselect', function (e, ui) {
          _self.$input.val('')
          _self.createToken( ui.item )
          return false
        })

      // Listen to window resize
      $(window).on('resize', $.proxy(this.update, this ))

    }

  , keydown: function (e) {

      if (!this.focused) return

      switch(e.keyCode) {
        case 8: // backspace
          if (!this.$input.is(':focus')) break
          this.lastInputValue = this.$input.val()
          break

        case 37: // left arrow
          if (this.$input.is(':focus')) {
            if (this.$input.val().length > 0) break

            var prev = this.$input.prevAll('.token:first')

            if (!prev.length) break

            this.preventInputFocus = true

            this.activate( prev )
            e.preventDefault()

          } else {
            this.prev( e.shiftKey )
            e.preventDefault()
          }
          break

        case 38: // up arrow
          if (!e.shiftKey) return

          if (this.$input.is(':focus')) {
            if (this.$input.val().length > 0) break

            var prev = this.$input.prev('.token:last')
            if (!prev.length) return

            this.activate( prev )
          }

          var _self = this
          this.firstActiveToken.nextAll('.token').each(function() {
            _self.deactivate( $(this) )
          })

          this.activate( this.$wrapper.find('.token:first'), true, true )
          e.preventDefault()
          break

        case 39: // right arrow
          if (this.$input.is(':focus')) {
            if (this.$input.val().length > 0) break
            
            var next = this.$input.next('.token:first')

            if (!next.length) break

            this.preventInputFocus = true

            this.activate( next )
            e.preventDefault()              

          } else {
            this.next( e.shiftKey )
            e.preventDefault()
          }
          break

        case 40: // down arrow
          if (!e.shiftKey) return

          if (this.$input.is(':focus')) {
            if (this.$input.val().length > 0) break

            var next = this.$input.next('.token:first')
            if (!next.length) return

            this.activate( next )
          }

          var _self = this
          this.firstActiveToken.prevAll('.token').each(function() {
            _self.deactivate( $(this) )
          })          

          this.activate( this.$wrapper.find('.token:last'), true, true )
          e.preventDefault()
          break        

        case 65: // a (to handle ctrl + a)
          if (this.$input.val().length > 0 || !(e.ctrlKey || e.metaKey)) break
          this.activateAll()
          e.preventDefault()
          break

        case 9: // tab
        case 13: // enter
          if (this.$input.data('autocomplete-open')) break
          if (this.$input.is(':focus') && this.$input.val().length || this.$input.data('edit')) {
            this.createTokensFromInput(e)
          }
          if (e.keyCode === 13) {
            if (!this.$element.is(':focus') || this.$wrapper.find('.token.active').length !== 1) break
            this.edit( this.$wrapper.find('.token.active') )
          }
      }

      this.lastKeyDown = e.keyCode
    }

  , keypress: function(e) {
      this.lastKeyPressCode = e.keyCode
      this.lastKeyPressCharCode = e.charCode
    }

  , keyup: function (e) {
      this.preventInputFocus = false

      if (!this.focused) return

      switch(e.keyCode) {
        case 8: // backspace
          if (this.$input.is(':focus')) {
            if (this.$input.val().length || this.lastInputValue.length && this.lastKeyDown === 8) break
            this.activate( this.$input.prevAll('.token:first') )
          } else {
            this.remove(e)
          }
          break

        case 46: // delete
          this.remove(e, 'next')
          break

        case 188: // comma, hopefully (can also be angle bracket, so we need to check for keyPress code)
          if (this.lastKeyPressCharCode !== 44 || !this.$input.is(':focus') || !this.$input.val()) break
          this.createTokensFromInput(e)
      }
      this.lastKeyUp = e.keyCode
    }

  , focus: function (e) {
      this.focused = true
      this.$wrapper.addClass('focus')

      if (this.$input.is(':focus')) {
        this.$wrapper.find('.active').removeClass('active')
        this.firstActiveToken = null

        if (this.options.showAutocompleteOnFocus && this.$input.data('uiAutocomplete')) {
          this.search()
        }
      }
    }

  , blur: function (e) {
      this.focused = false
      this.$wrapper.removeClass('focus')

      if (!this.preventDeactivation && !this.$element.is(':focus')) {
        this.$wrapper.find('.active').removeClass('active')
        this.firstActiveToken = null
      }

      if (this.$input.data('edit') && !this.$input.is(':focus') || this.options.createTokensOnBlur) {
        this.createTokensFromInput(e)
      }
      
      this.preventDeactivation = false
    }

  , paste: function (e) {
      var _self = this
      
      // Add tokens to existing ones
      setTimeout(function () {
        _self.createTokensFromInput(e)
      }, 1)
    }

  , createTokensFromInput: function (e) {
      if (this.$input.val().length < this.options.minLength) return

      var tokensBefore = this.getTokensList()
      this.setTokens( this.$input.val(), true )
      if (tokensBefore == this.getTokensList() && this.$input.val().length) return // No tokens were added, do nothing

      this.$input.val('')

      if (this.$input.data( 'edit' )) {
        this.$input
          .appendTo( this.$wrapper )
          .data( 'edit', false )
          .css( 'width', this.options.minWidth + 'px' )

        if (!this.preventInputFocus) {
          var _self = this
          setTimeout(function () {
            _self.$input.focus()
          }, 1)
        }

        this.$wrapper.css( 'width', this.$wrapper.data('prev-width') )
      }

      e.preventDefault()
      e.stopPropagation()
    }  

  , next: function (add) {
      if (add) {
        var firstActive = this.$wrapper.find('.active:first')
          , deactivate = firstActive && this.firstActiveToken ? firstActive.index() < this.firstActiveToken.index() : false

        if (deactivate) return this.deactivate( firstActive )
      }

      var active = this.$wrapper.find('.active:last')
        , next = active.nextAll('.token:first')

      if (!next.length) {
        this.$input.focus()
        return
      }

      this.activate(next, add)
    }

  , prev: function (add) {

      if (add) {
        var lastActive = this.$wrapper.find('.active:last')
          , deactivate = lastActive && this.firstActiveToken ? lastActive.index() > this.firstActiveToken.index() : false

        if (deactivate) return this.deactivate( lastActive )
      }

      var active = this.$wrapper.find('.active:first')
        , prev = active.prevAll('.token:first')

      if (!prev.length) {
        prev = this.$wrapper.find('.token:first')
      }

      if (!prev.length && !add) {
        this.$input.focus()
        return
      }

      this.activate( prev, add )
    }

  , activate: function (token, add, multi, remember) {

      if (!token) return

      if (this.$wrapper.find('.token.active').length === this.$wrapper.find('.token').length) return

      if (typeof remember === 'undefined') var remember = true

      if (multi) var add = true

      this.$copyHelper.focus()

      if (!add) {
        this.$wrapper.find('.active').removeClass('active')
        if (remember) {
          this.firstActiveToken = token 
        } else {
          delete this.firstActiveToken
        }
      }

      if (multi && this.firstActiveToken) {
        // Determine first active token and the current tokens indicies
        // Account for the 1 hidden textarea by subtracting 1 from both
        var i = this.firstActiveToken.index() - 2
          , a = token.index() - 2
          , _self = this

        this.$wrapper.find('.token').slice( Math.min(i, a) + 1, Math.max(i, a) ).each( function() {
          _self.activate( $(this), true )
        })
      }

      token.addClass('active')
      this.preventDeactivation = true
      this.$copyHelper.val( this.getTokensList( true ) ).select()
    }

  , activateAll: function() {
      var _self = this

      this.$wrapper.find('.token').each( function (i) {
        _self.activate($(this), i !== 0, false, false)
      })
    }

  , deactivate: function(token) {
      if (!token) return

      token.removeClass('active')
      this.$copyHelper.val( this.getTokensList( true ) ).select()
    }

  , toggle: function(token) {
      if (!token) return

      token.toggleClass('active')
      this.$copyHelper.val( this.getTokensList( true ) ).select()
    }

  , edit: function (token) {
      if (!token) return

      var value = token.data('value')
        , label = token.find('.token-label').text()

      // Allow changing input value before editing
      var e = $.Event('beforeEditToken')
      e.token = {
        value: value,
        label: label
      }
      this.$element.trigger(e)

      token.find('.token-label').text(e.token.value)
      var tokenWidth = token.outerWidth()

      this.$wrapper
        .data( 'prev-width', this.$wrapper.width() )
        .width( this.$wrapper.width() )

      token.replaceWith( this.$input )

      this.$input.val( e.token.value )
                .select()
                .data( 'edit', true )
                .width( tokenWidth )
    }

  , remove: function (e, direction) {
      if (this.$input.is(':focus') || this.disabled) return

      var token = (e.type === 'click') ? $(e.target).closest('.token') : this.$wrapper.find('.token.active')
      
      if (e.type !== 'click') {
        if (!direction) var direction = 'prev'
        this[direction]()
      }
      token.remove()

      this.$element.val( this.getTokensList() ).trigger('removeToken').trigger('change')

      if (!this.$wrapper.find('.token').length || e.type === 'click') this.$input.focus()

      this.$input.css('width', this.options.minWidth + 'px')
      this.update()

      e.preventDefault()
      e.stopPropagation()
    }

  , update: function () {
      var value = this.$input.val()

      if (this.$input.data('edit')) {
        if (!value) {
          value = this.$input.prop("placeholder")
        }
        if (value === this.$mirror.text()) return

        this.$mirror.text(value)

        this.$input.width(this.$mirror.width() + 10)
      }
      else {
        this.$input.css( 'width', this.options.minWidth + 'px' )
        this.$input.width( this.$wrapper.offset().left + this.$wrapper.width() - this.$input.offset().left + 5 )
      }
    }

  , focusInput: function (e) {
      if ($(e.target).closest('.token').length) return
      this.$input.focus()
    }

  , search: function () {
      this.$input.autocomplete('search')
    }

  , disable: function () {
      this.disabled = true;
      this.$input.prop('disabled', true);
      this.$wrapper.addClass('disabled');
    }

  , enable: function () {
      this.disabled = false;
      this.$input.prop('disabled', false);
      this.$wrapper.removeClass('disabled');
    }

  }


 /* TOKENFIELD PLUGIN DEFINITION
  * ======================== */

  var old = $.fn.tokenfield

  $.fn.tokenfield = function (option, param) {
    this.each(function () {
      var $this = $(this)
        , data = $this.data('bs.tokenfield')
        , options = typeof option == 'object' && option

      if (typeof option === 'string' && data && data[option]) {
        return data[option](param)
      } else {
        if (!data) $this.data('bs.tokenfield', (data = new Tokenfield(this, options)))
      }
    })
    return this;
  }

  $.fn.tokenfield.defaults = {
    minWidth: 60,
    minLength: 0,
    allowDuplicates: false,
    autocomplete: {},
    showAutocompleteOnFocus: false,
    createTokensOnBlur: false
  }  

  $.fn.tokenfield.Constructor = Tokenfield


 /* TOKENFIELD NO CONFLICT
  * ================== */

  $.fn.tokenfield.noConflict = function () {
    $.fn.tokenfield = old
    return this
  }

}(window.jQuery);