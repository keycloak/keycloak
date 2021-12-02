// Util: PatternFly Vertical Navigation
// Must have navbar-toggle in navbar-pf-vertical for expand/collapse
(function ($) {
  'use strict';

  $.fn.setupVerticalNavigation = function (handleItemSelections, ignoreDrawer) {

    var navElement = $('.nav-pf-vertical'),
      bodyContentElement = $('.container-pf-nav-pf-vertical'),
      toggleNavBarButton = $('.navbar-toggle'),
      handleResize = true,
      explicitCollapse = false,
      subDesktop = false,
      hoverDelay = 500,
      hideDelay = hoverDelay + 200,

      inMobileState = function () {
        return bodyContentElement.hasClass('hidden-nav');
      },

      forceResize = function (delay) {
        setTimeout(function () {
          $(window).trigger('resize');
        }, delay);
      },

      showSecondaryMenu = function () {
        if (inMobileState() || !subDesktop) {
          navElement.addClass('secondary-visible-pf');
          bodyContentElement.addClass('secondary-visible-pf');
        }

        // Dispatch a resize event when showing the secondary menu in non-subdesktop state to
        // allow content to adjust to the secondary menu sizing
        if (!subDesktop) {
          forceResize(100);
        }
      },

      hideSecondaryMenu = function () {
        navElement.removeClass('secondary-visible-pf');
        bodyContentElement.removeClass('secondary-visible-pf');

        if (navElement.find('.secondary-nav-item-pf.is-hover').length <= 1) {
          navElement.removeClass('hover-secondary-nav-pf');
        }

        navElement.find('.mobile-nav-item-pf').each(function (index, item) {
          $(item).removeClass('mobile-nav-item-pf');
        });

        navElement.find('.is-hover').each(function (index, item) {
          $(item).removeClass('is-hover');
        });
      },

      hideTertiaryMenu = function () {
        navElement.removeClass('tertiary-visible-pf');
        bodyContentElement.removeClass('tertiary-visible-pf');

        if (navElement.find('.tertiary-nav-item-pf.is-hover').length <= 1) {
          navElement.removeClass('hover-tertiary-nav-pf');
        }

        navElement.find('.mobile-nav-item-pf').each(function (index, item) {
          $(item).removeClass('mobile-nav-item-pf');
        });

        navElement.find('.is-hover').each(function (index, item) {
          $(item).removeClass('is-hover');
        });
      },

      setActiveItem = function (item) {
        // remove all .active
        $('.nav-pf-vertical .list-group-item.active').removeClass('active');

        // add .active to item and its parents
        item.addClass('active').parents('.list-group-item').addClass('active');
      },

      updateSecondaryMenuDisplayAfterSelection = function () {
        if (inMobileState()) {
          navElement.removeClass('show-mobile-nav');
          hideSecondaryMenu();
          navElement.find('.mobile-nav-item-pf').each(function (index, item) {
            $(item).removeClass('mobile-nav-item-pf');
          });
        } else {
          showSecondaryMenu();
        }
      },

      updateSecondaryCollapsedState = function (setCollapsed, collapsedItem) {
        if (setCollapsed) {
          collapsedItem.addClass('collapsed');
          navElement.addClass('collapsed-secondary-nav-pf');
          bodyContentElement.addClass('collapsed-secondary-nav-pf');
        } else {
          if (collapsedItem) {
            collapsedItem.removeClass('collapsed');
          } else {
            // Remove any collapsed secondary menus
            navElement.find('[data-toggle="collapse-secondary-nav"]').each(function (index, element) {
              var $e = $(element);
              $e.removeClass('collapsed');
            });
          }
          navElement.removeClass('collapsed-secondary-nav-pf');
          bodyContentElement.removeClass('collapsed-secondary-nav-pf');
        }
      },

      updateTertiaryCollapsedState = function (setCollapsed, collapsedItem) {
        if (setCollapsed) {
          collapsedItem.addClass('collapsed');
          navElement.addClass('collapsed-tertiary-nav-pf');
          bodyContentElement.addClass('collapsed-tertiary-nav-pf');
          updateSecondaryCollapsedState(false);
        } else {
          if (collapsedItem) {
            collapsedItem.removeClass('collapsed');
          } else {
            // Remove any collapsed tertiary menus
            navElement.find('[data-toggle="collapse-tertiary-nav"]').each(function (index, element) {
              var $e = $(element);
              $e.removeClass('collapsed');
            });
          }
          navElement.removeClass('collapsed-tertiary-nav-pf');
          bodyContentElement.removeClass('collapsed-tertiary-nav-pf');
        }
      },

      updateMobileMenu = function (selected, secondaryItem) {
        $(document).find('.list-group-item.mobile-nav-item-pf').each(function (index, item) {
          $(item).removeClass('mobile-nav-item-pf');
        });
        $(document).find('.list-group-item.mobile-secondary-item-pf').each(function (index, item) {
          $(item).removeClass('mobile-secondary-item-pf');
        });
        if (selected) {
          selected.addClass('mobile-nav-item-pf');
          if (secondaryItem) {
            secondaryItem.addClass('mobile-secondary-item-pf');
            navElement.removeClass('show-mobile-secondary');
            navElement.addClass('show-mobile-tertiary');
          } else {
            navElement.addClass('show-mobile-secondary');
            navElement.removeClass('show-mobile-tertiary');
          }
        } else {
          navElement.removeClass('show-mobile-secondary');
          navElement.removeClass('show-mobile-tertiary');
        }
      },

      enterMobileState = function () {
        if (!navElement.hasClass('hidden')) {
          //Set the nav to being hidden
          navElement.addClass('hidden');
          navElement.removeClass('collapsed');

          //Set the body class to the correct state
          bodyContentElement.removeClass('collapsed-nav');
          bodyContentElement.addClass('hidden-nav');

          // Reset the collapsed states
          updateSecondaryCollapsedState(false);
          updateTertiaryCollapsedState(false);

          explicitCollapse = false;
        }
      },

      exitMobileState = function () {
        // Always remove the hidden & peek class
        navElement.removeClass('hidden show-mobile-nav');

        // Set the body class back to the default
        bodyContentElement.removeClass('hidden-nav');
      },

      checkNavState = function () {
        var width = $(window).width(), makeSecondaryVisible;
        if (!handleResize) {
          return;
        }
        // Check to see if we need to enter/exit the mobile state
        if (width < $.pfBreakpoints.tablet && !explicitCollapse) {
          enterMobileState();
        } else if (navElement.hasClass('hidden')) {
          exitMobileState();
        }

        // Check to see if we need to enter/exit the sub desktop state
        if (width < $.pfBreakpoints.desktop) {
          if (!subDesktop) {
            // Collapse the navigation bars when entering sub desktop mode
            navElement.addClass('collapsed');
            bodyContentElement.addClass('collapsed-nav');
          }
          if (width >= $.pfBreakpoints.tablet) {
            hideSecondaryMenu();
          }
          subDesktop = true;
        } else {
          makeSecondaryVisible = subDesktop && (navElement.find('.secondary-nav-item-pf.active').length > 0);
          subDesktop = false;
          if (makeSecondaryVisible) {

            showSecondaryMenu();
          }
        }

        if (explicitCollapse) {
          navElement.addClass('collapsed');
          bodyContentElement.addClass('collapsed-nav');
        } else {
          navElement.removeClass('collapsed');
          bodyContentElement.removeClass('collapsed-nav');
        }
      },

      collapseMenu = function () {
        //Make sure this is expanded
        navElement.addClass('collapsed');
        //Set the body class to the correct state
        bodyContentElement.addClass('collapsed-nav');

        if (subDesktop) {
          hideSecondaryMenu();
        }

        explicitCollapse = true;
      },

      enableTransitions = function () {
        // enable transitions only when toggleNavBarButton is clicked or window is resized
        $('html').addClass('transitions');
      },

      expandMenu = function () {
        //Make sure this is expanded
        navElement.removeClass('collapsed');
        //Set the body class to the correct state
        bodyContentElement.removeClass('collapsed-nav');

        explicitCollapse = false;

        // Dispatch a resize event when showing the expanding then menu to
        // allow content to adjust to the menu sizing
        if (!subDesktop) {
          forceResize(100);
        }
      },

      bindMenuBehavior = function () {
        toggleNavBarButton.on('click', function (e) {
          var $drawer;

          enableTransitions();

          if (inMobileState()) {
            // Toggle the mobile nav
            if (navElement.hasClass('show-mobile-nav')) {
              navElement.removeClass('show-mobile-nav');
            } else {
              // Always start at the primary menu
              updateMobileMenu();
              navElement.addClass('show-mobile-nav');

              // If the notification drawer is shown, hide it
              if (!ignoreDrawer) {
                $drawer = $('.drawer-pf');
                if ($drawer.length) {
                  $('.drawer-pf-trigger').removeClass('open');
                  $drawer.addClass('hide');
                }
              }
            }
          } else if (navElement.hasClass('collapsed')) {
            window.localStorage.setItem('patternfly-navigation-primary', 'expanded');
            expandMenu();
          } else {
            window.localStorage.setItem('patternfly-navigation-primary', 'collapsed');
            collapseMenu();
          }
        });
      },

      forceHideSecondaryMenu = function () {
        navElement.addClass('force-hide-secondary-nav-pf');
        setTimeout(function () {
          navElement.removeClass('force-hide-secondary-nav-pf');
        }, 500);
      },

      bindMenuItemsBehavior = function (handleSelection) {
        $(document).find('.nav-pf-vertical .list-group-item').each(function (index, item) {
          var onClickFn,
            $item = $(item),
            $nav = $item.closest('[class*="nav-pf-"]');

          if ($nav.hasClass('nav-pf-vertical')) {
            // Set main nav active item on click or show secondary nav if it has a secondary nav bar and we are in the mobile state
            onClickFn = function (event) {
              var $this = $(this), $secondaryItem, $tertiaryItem, $activeItem;

              if (!$this.hasClass('secondary-nav-item-pf')) {
                hideSecondaryMenu();
                if (inMobileState()) {
                  updateMobileMenu();
                  navElement.removeClass('show-mobile-nav');
                }
                if (handleSelection) {
                  setActiveItem($this);
                  // Don't process the click on the item
                  event.stopImmediatePropagation();
                }
              } else if (inMobileState()) {
                updateMobileMenu($this);
              } else if (handleSelection) {
                $activeItem = $secondaryItem = $item.find('.nav-pf-secondary-nav > .list-group > .list-group-item').eq(0);

                if ($secondaryItem.hasClass('tertiary-nav-item-pf')) {
                  $activeItem = $secondaryItem.find('.nav-pf-tertiary-nav > .list-group > .list-group-item').eq(0);
                }

                setActiveItem($activeItem);
                event.stopImmediatePropagation();
              }
            };

          } else if ($nav.hasClass('nav-pf-secondary-nav')) {
            // Set secondary nav active item on click or show tertiary nav if it has a tertiary nav bar and we are in the mobile state
            onClickFn = function (event) {
              var $this = $(this), $tertiaryItem, $primaryItem;
              if (!$this.hasClass('tertiary-nav-item-pf')) {
                if (inMobileState()) {
                  updateMobileMenu();
                  navElement.removeClass('show-mobile-nav');
                }
                updateSecondaryMenuDisplayAfterSelection();
                if (handleSelection) {
                  setActiveItem($item);
                  hideSecondaryMenu();
                  event.stopImmediatePropagation();
                }
              } else if (inMobileState()) {
                $primaryItem = $item.parents('.list-group-item');
                updateMobileMenu($this, $primaryItem);
                event.stopImmediatePropagation();
              } else if (handleSelection) {
                $tertiaryItem = $item.find('.nav-pf-tertiary-nav > .list-group > .list-group-item').eq(0);
                setActiveItem($tertiaryItem);
                event.stopImmediatePropagation();
              }
            };

          } else if ($nav.hasClass('nav-pf-tertiary-nav')) {
            // Set tertiary nav active item on click
            onClickFn = function (event) {
              if (inMobileState()) {
                updateMobileMenu();
                navElement.removeClass('show-mobile-nav');
              }
              updateSecondaryMenuDisplayAfterSelection();
              if (handleSelection) {
                setActiveItem($item);
                hideTertiaryMenu();
                hideSecondaryMenu();
                event.stopImmediatePropagation();
              }
            };
          }

          // register event handler
          $item.on('click.pf.secondarynav.data-api', onClickFn);
        });

        $(document).find('.secondary-nav-item-pf').each(function (index, secondaryItem) {
          var $secondaryItem = $(secondaryItem);

          // Collapse the secondary nav bar when the toggle is clicked
          $secondaryItem.on('click.pf.secondarynav.data-api', '[data-toggle="collapse-secondary-nav"]', function (e) {
            var $this = $(this);
            if (inMobileState()) {
              updateMobileMenu();
              e.stopImmediatePropagation();
            } else {
              if ($this.hasClass('collapsed')) {
                window.localStorage.setItem('patternfly-navigation-secondary', 'expanded');
                window.localStorage.setItem('patternfly-navigation-tertiary', 'expanded');
                updateSecondaryCollapsedState(false, $this);
                forceHideSecondaryMenu();
              } else {
                window.localStorage.setItem('patternfly-navigation-secondary', 'collapsed');
                updateSecondaryCollapsedState(true, $this);
              }
            }
            navElement.removeClass('hover-secondary-nav-pf');
            if (handleSelection) {
              // Don't process the click on the parent item
              e.stopImmediatePropagation();
            }
          });

          $secondaryItem.find('.tertiary-nav-item-pf').each(function (index, primaryItem) {
            var $primaryItem = $(primaryItem);
            // Collapse the tertiary nav bar when the toggle is clicked
            $primaryItem.on('click.pf.tertiarynav.data-api', '[data-toggle="collapse-tertiary-nav"]', function (e) {
              var $this = $(this);
              if (inMobileState()) {
                updateMobileMenu($secondaryItem);
                e.stopImmediatePropagation();
              } else {
                if ($this.hasClass('collapsed')) {
                  window.localStorage.setItem('patternfly-navigation-secondary', 'expanded');
                  window.localStorage.setItem('patternfly-navigation-tertiary', 'expanded');
                  updateTertiaryCollapsedState(false, $this);
                  forceHideSecondaryMenu();
                } else {
                  window.localStorage.setItem('patternfly-navigation-tertiary', 'collapsed');
                  updateTertiaryCollapsedState(true, $this);
                }
              }
              navElement.removeClass('hover-secondary-nav-pf');
              navElement.removeClass('hover-tertiary-nav-pf');
              if (handleSelection) {
                // Don't process the click on the parent item
                e.stopImmediatePropagation();
              }
            });
          });
        });

        // Show secondary nav bar on hover of secondary nav items
        $(document).on('mouseenter.pf.tertiarynav.data-api', '.secondary-nav-item-pf', function (e) {
          var $this = $(this);
          if (!inMobileState()) {
            if ($this[0].navUnHoverTimeout !== undefined) {
              clearTimeout($this[0].navUnHoverTimeout);
              $this[0].navUnHoverTimeout = undefined;
            } else if ($this[0].navHoverTimeout === undefined) {
              $this[0].navHoverTimeout = setTimeout(function () {
                navElement.addClass('hover-secondary-nav-pf');
                $this.addClass('is-hover');
                $this[0].navHoverTimeout = undefined;
              }, hoverDelay);
            }
          }
        });

        $(document).on('mouseleave.pf.tertiarynav.data-api', '.secondary-nav-item-pf', function (e) {
          var $this = $(this);
          if ($this[0].navHoverTimeout !== undefined) {
            clearTimeout($this[0].navHoverTimeout);
            $this[0].navHoverTimeout = undefined;
          } else if ($this[0].navUnHoverTimeout === undefined &&
              navElement.find('.secondary-nav-item-pf.is-hover').length > 0) {
            $this[0].navUnHoverTimeout = setTimeout(function () {
              if (navElement.find('.secondary-nav-item-pf.is-hover').length <= 1) {
                navElement.removeClass('hover-secondary-nav-pf');
              }
              $this.removeClass('is-hover');
              $this[0].navUnHoverTimeout = undefined;
            }, hideDelay);
          }
        });

        // Show tertiary nav bar on hover of secondary nav items
        $(document).on('mouseover.pf.tertiarynav.data-api', '.tertiary-nav-item-pf', function (e) {
          var $this = $(this);
          if (!inMobileState()) {
            if ($this[0].navUnHoverTimeout !== undefined) {
              clearTimeout($this[0].navUnHoverTimeout);
              $this[0].navUnHoverTimeout = undefined;
            } else if ($this[0].navHoverTimeout === undefined) {
              $this[0].navHoverTimeout = setTimeout(function () {
                navElement.addClass('hover-tertiary-nav-pf');
                $this.addClass('is-hover');
                $this[0].navHoverTimeout = undefined;
              }, hoverDelay);
            }
          }
        });
        $(document).on('mouseout.pf.tertiarynav.data-api', '.tertiary-nav-item-pf', function (e) {
          var $this = $(this);
          if ($this[0].navHoverTimeout !== undefined) {
            clearTimeout($this[0].navHoverTimeout);
            $this[0].navHoverTimeout = undefined;
          } else if ($this[0].navUnHoverTimeout === undefined) {
            $this[0].navUnHoverTimeout = setTimeout(function () {
              if (navElement.find('.tertiary-nav-item-pf.is-hover').length <= 1) {
                navElement.removeClass('hover-tertiary-nav-pf');
              }
              $this.removeClass('is-hover');
              $this[0].navUnHoverTimeout = undefined;
            }, hideDelay);
          }
        });
      },

      loadFromLocalStorage = function () {
        if (inMobileState()) {
          return;
        }

        if (window.localStorage.getItem('patternfly-navigation-primary') === 'collapsed') {
          collapseMenu();
        }

        if ($('.nav-pf-vertical.nav-pf-vertical-collapsible-menus').length > 0) {
          if (window.localStorage.getItem('patternfly-navigation-secondary') === 'collapsed') {
            updateSecondaryCollapsedState(true, $('.secondary-nav-item-pf.active [data-toggle=collapse-secondary-nav]'));
          }

          if (window.localStorage.getItem('patternfly-navigation-tertiary') === 'collapsed') {
            updateTertiaryCollapsedState(true, $('.tertiary-nav-item-pf.active [data-toggle=collapse-tertiary-nav]'));
          }
        }
      },

      setTooltips = function () {
        var tooltipOptions = {
          container: 'body',
          placement: 'bottom',
          delay: { 'show': '500', 'hide': '200' },
          template: '<div class="nav-pf-vertical-tooltip tooltip" role="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner"></div></div>'
        };
        $('.nav-pf-vertical [data-toggle="tooltip"]').tooltip(tooltipOptions);

        $('.nav-pf-vertical').on("show.bs.tooltip", function (e) {
          return $(this).hasClass("collapsed");
        });
      },

      init = function (handleItemSelections) {
        // Hide the nav menus during initialization
        navElement.addClass('hide-nav-pf');
        bodyContentElement.addClass('hide-nav-pf');

        //Set correct state on load
        checkNavState();

        // Bind Top level hamburger menu with menu behavior;
        bindMenuBehavior();

        // Bind menu items
        bindMenuItemsBehavior(handleItemSelections);

        //Set tooltips
        setTooltips();

        loadFromLocalStorage();

        // Show the nav menus
        navElement.removeClass('hide-nav-pf');
        bodyContentElement.removeClass('hide-nav-pf');
        forceResize(250);
      },

      self = {
        hideMenu: function () {
          handleResize = false;
          enterMobileState();
        },
        showMenu: function () {
          handleResize = true;
          exitMobileState();
        },
        isVisible: function () {
          return handleResize;
        }
      };

    if (!$.fn.setupVerticalNavigation.self) {
      $.fn.setupVerticalNavigation.self = self;
      //Listen for the window resize event and collapse/hide as needed
      $(window).on('resize', function () {
        checkNavState();
        enableTransitions();
      });

      init(handleItemSelections);
    }
    return $.fn.setupVerticalNavigation.self;
  };
}(jQuery));
