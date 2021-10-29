/**
 * @summary     pfFilter for DataTables
 * @description A collection of API methods providing simple filter functionality for DataTables. This ensures
 * DataTables meets the Patternfly design pattern with a toolbar.
 *
 * To apply a filter, the user must press enter in the given filter input. The user may apply a filter to a different
 * column via the given filter drop down. After a filter has been applied, the filter results text, active filter
 * controls, and a clear all control are shown.
 *
 * The toolbar and empty state layouts are expected to contain the classes as shown in the example below.
 *
 * Example:
 *
 * <!-- NOTE: Some configuration may be omitted for clarity -->
 * <div class="row toolbar-pf table-view-pf-toolbar" id="toolbar1">
 *   <div class="col-sm-12">
 *     <form class="toolbar-pf-actions">
 *       <div class="form-group toolbar-pf-filter">
 *         <label class="sr-only" for="filter">Rendering Engine</label>
 *         <div class="input-group">
 *           <div class="input-group-btn">
 *             <button type="button" class="btn btn-default dropdown-toggle" id="filter" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Rendering Engine <span class="caret"></span></button>
 *             <ul class="dropdown-menu">
 *               <li><a href="#" id="filter1">Rendering Engine</a></li>
 *               <li><a href="#" id="filter2">Browser</a></li>
 *             </ul>
 *           </div>
 *           <input type="text" class="form-control" placeholder="Filter By Rendering Engine..." autocomplete="off" id="filterInput">
 *         </div>
 *       </div>
 *       ...
 *     </form>
 *     <div class="row toolbar-pf-results">
 *       <div class="col-sm-9">
 *         <div class="hidden">
 *           <h5>0 Results</h5>
 *           <p>Active filters:</p>
 *           <ul class="list-inline"></ul>
 *           <p><a href="#">Clear All Filters</a></p>
 *         </div>
 *       </div>
 *       <div class="col-sm-3 table-view-pf-select-results">
 *         <strong>0</strong> of <strong>0</strong> selected
 *       </div>
 *     </div>
 *   </div>
 * </div>
 * <table class="table table-striped table-bordered table-hover" id="table1">
 *   <thead>
 *     <tr>
 *       <th><input type="checkbox" name="selectAll"></th>
 *       <th>Rendering Engine</th>
 *       <th>Browser</th>
 *     </tr>
 *   </thead>
 * </table>
 * ...
 * <script>
 * // NOTE: Some properties may be omitted for clarity
 * $(document).ready(function() {
 *   var dt = $("#table1").DataTable({
 *     columns: [
 *       { data: null, ... },
 *       { data: "engine" },
 *       { data: "browser" }
 *     ],
 *     data: [
 *       { engine: "Gecko", browser: "Firefox" }
 *       { engine: "Trident", browser: "Mozilla" }
 *     ],
 *     dom: "t",
 *     pfConfig: {
 *       ...
 *       filterCaseInsensitive: true,
 *       filterCols: [
 *         null,
 *         {
 *           default: true,
 *           optionSelector: "#filter1",
 *           placeholder: "Filter By Rendering Engine..."
 *         }, {
 *           optionSelector: "#filter2",
 *           placeholder: "Filter By Browser..."
 *         }
 *       ],
 *       toolbarSelector: "#toolbar1"
 *     }
 *   });
 *   // Optional API to clear filters
 *   dt.table().pfFilter.clearFilters();
 *
 *   // Optional API to add filter
 *   dt.table().pfFilter.addFilter({
 *     column: 2,
 *     name: "Browser",
 *     value: "Firefox"
 *   });
 * });
 * </script>
 *
 * Note: This functionality requires the following Javascript library files to be loaded:
 *
 * https://cdn.datatables.net/select/1.2.0/js/dataTables.select.min.js
 */
(function (factory) {
  "use strict";
  if (typeof define === "function" && define.amd ) {
    // AMD
    define (["jquery", "datatables.net"], function ($) {
      return factory ($, window, document);
    });
  } else if (typeof exports === "object") {
    // CommonJS
    module.exports = function (root, $) {
      if (!root) {
        root = window;
      }
      if (!$ || !$.fn.dataTable) {
        $ = require("datatables.net")(root, $).$;
      }
      return factory($, root, root.document);
    };
  } else {
    // Browser
    factory(jQuery, window, document);
  }
}(function ($, window, document, undefined) {
  "use strict";
  var DataTable = $.fn.dataTable;
  var ACTIVE_FILTER_CONTROLS_SELECTOR = ".list-inline"; // Active filter controls
  var CLEAR_FILTERS_SELECTOR = ".toolbar-pf-results a"; // Clear filters control
  var FILTER_SELECTOR = ".toolbar-pf-filter"; // Filter input
  var FILTER_BUTTON_SELECTOR = FILTER_SELECTOR + " button"; // Filter button
  var FILTER_INPUT_SELECTOR = FILTER_SELECTOR + " input"; // Filter input
  var FILTER_LABEL_SELECTOR = FILTER_SELECTOR + " label"; // Filter label
  var RESULTS_SELECTOR = ".toolbar-pf-results"; // Toolbar results row
  var FILTER_RESULTS_SELECTOR = RESULTS_SELECTOR + " h5"; // Toolbar filter results

  DataTable.pfFilter = {};

  /**
   * Initialize
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  DataTable.pfFilter.init = function (dt) {
    var i;
    var ctx = dt.settings()[0];
    var opts = (ctx.oInit.pfConfig) ? ctx.oInit.pfConfig : {};

    ctx._pfFilter = {};
    ctx._pfFilter.filterButton = $(FILTER_BUTTON_SELECTOR, opts.toolbarSelector); // Filter button
    ctx._pfFilter.filterCols = opts.filterCols; // Filter colums config
    ctx._pfFilter.filterLabel = $(FILTER_LABEL_SELECTOR, opts.toolbarSelector); // Filter label
    ctx._pfFilter.filterInput = $(FILTER_INPUT_SELECTOR, opts.toolbarSelector); // Filter input
    ctx._pfFilter.filters = []; // Applied filters array
    ctx._pfFilter.activeFilterControls = $(ACTIVE_FILTER_CONTROLS_SELECTOR, opts.toolbarSelector); // Active filter controls
    ctx._pfFilter.activeFilters = ctx._pfFilter.activeFilterControls.closest("div"); // Active filters container
    ctx._pfFilter.clearFilters = $(CLEAR_FILTERS_SELECTOR, opts.toolbarSelector); // Clear filters control
    ctx._pfFilter.results = $(RESULTS_SELECTOR, opts.toolbarSelector); // Toolbar results row
    ctx._pfFilter.filterCaseInsensitive = opts.filterCaseInsensitive; // Filter filter case insensitive
    ctx._pfFilter.filterResults = $(FILTER_RESULTS_SELECTOR, opts.toolbarSelector); // Toolbar filter results

    if (ctx._pfFilter.filterCols === undefined) {
      return;
    }

    // Set default filter properties
    for (i = 0; i < ctx._pfFilter.filterCols.length; i++) {
      if (ctx._pfFilter.filterCols[i] === null) {
        continue;
      }
      ctx._pfFilter.filterColumn = i; // Current filter column
      ctx._pfFilter.filterName = $(ctx._pfFilter.filterCols[i].optionSelector).text(); // Name of current filter
      if (ctx._pfFilter.filterCols[i].default === true) {
        break;
      }
    }

    // Handle click on filter menu to set current filter column and name
    for (i = 0; i < ctx._pfFilter.filterCols.length; i++) {
      handleFilterOption(dt, i); // Need to pass value of i as a function
    }

    // Handle actions when enter is pressed within filter input
    handleFilterInput(dt);

    // Handle actions when clear filters control is selected
    handleClearFilters(dt);

    // Simple filter
    $.fn.dataTable.ext.search.push(function (ctx, data, dataIndex) {
      var showThisRow = true;
      // Must match all filters
      if (ctx._pfFilter) {
        $.each(ctx._pfFilter.filters, function (index, filter) {
          if (ctx._pfFilter.filterCaseInsensitive !== undefined && ctx._pfFilter.filterCaseInsensitive === true) {
            if (data[filter.column].toLowerCase().indexOf(filter.value.toLowerCase()) === -1) {
              showThisRow = false;
            }
          } else {
            if (data[filter.column].indexOf(filter.value) === -1) {
              showThisRow = false;
            }
          }
        });
      }
      return showThisRow;
    });
  };

  // Local functions

  /**
   * Add active filter control
   *
   * @param {DataTable.Api} dt DataTable
   * @param {object} filter Properties associated with a new filter
   * @param {string} filter.column - Column associated with DataTable
   * @param {string} filter.name - Name of the filter
   * @param {string} filter.value - Value of the filter
   * @private
   */
  function addActiveFilterControl (dt, filter) {
    var ctx = dt.settings()[0];
    var i;

    // Append active filter control
    ctx._pfFilter.activeFilterControls.append('<li><span class="label label-info">' + filter.name + ': ' +
      filter.value + '<a href="#"><span class="pficon pficon-close"/></a></span></li>');

    // Handle click to clear active filter
    $("a", ctx._pfFilter.activeFilterControls).last().on("click", function (e) {
      // Find existing filter and remove
      for (i = 0; i < ctx._pfFilter.filters.length; i++) {
        if (ctx._pfFilter.filters[i].column === filter.column && ctx._pfFilter.filters[i].value === filter.value) {
          ctx._pfFilter.filters.splice(i, 1);
          $(this).parents("li").remove();
          break;
        }
      }
      if (ctx._pfFilter.filters.length === 0) {
        ctx._pfFilter.activeFilters.addClass("hidden"); // Hide
      }
      dt.draw();
      updateFilterResults(dt);
    });
    // Show active filters
    ctx._pfFilter.activeFilters.removeClass("hidden");
  }

  /**
   * Add filter
   *
   * @param {DataTable.Api} dt DataTable
   * @param {object} filter Properties associated with a new filter
   * @param {string} filter.column - Column associated with DataTable
   * @param {string} filter.name - Name of the filter
   * @param {string} filter.value - Value of the filter
   * @private
   */
  function addFilter (dt, filter) {
    var ctx = dt.settings()[0];
    var found = false;

    // Find existing entry
    $.grep(ctx._pfFilter.filters, function (f) {
      if (f.column === filter.column && f.value === filter.value) {
        found = true;
      }
    });

    // Add new filter
    if (!found) {
      ctx._pfFilter.filters.push(filter);
      dt.draw();
      addActiveFilterControl(dt, filter);
      updateFilterResults(dt);
    }
    ctx._pfFilter.filterInput.val(""); // Clear input
  }

  /**
   * Clear filters
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function clearFilters (dt) {
    var ctx = dt.settings()[0];
    ctx._pfFilter.filters.length = 0; // Reset filters
    ctx._pfFilter.activeFilterControls.html(""); // Remove active filter controls
    ctx._pfFilter.activeFilters.addClass("hidden"); // Hide active filters area
    dt.draw();
  }

  /**
   * Handle actions when clear filters control is selected
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function handleClearFilters (dt) {
    var ctx = dt.settings()[0];
    if (ctx._pfFilter.clearFilters === undefined || ctx._pfFilter.clearFilters.length === 0) {
      return;
    }
    ctx._pfFilter.clearFilters.on("click", function (e) {
      clearFilters(dt);
    });
  }

  /**
   * Handle actions when enter is pressed within filter input
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function handleFilterInput (dt) {
    var ctx = dt.settings()[0];
    if (ctx._pfFilter.filterInput === undefined || ctx._pfFilter.filterInput.length === 0) {
      return;
    }
    ctx._pfFilter.filterInput.on("keypress", function (e) {
      var keycode = (e.keyCode ? e.keyCode : e.which);
      if (keycode === 13) {
        e.preventDefault();
        if (this.value.trim().length > 0) {
          addFilter(dt, {
            column: ctx._pfFilter.filterColumn,
            name: ctx._pfFilter.filterName,
            value: this.value
          });
        }
        return false;
      }
      return true;
    });
  }

  /**
   * Handle actions when filter options are selected
   *
   * @param {DataTable.Api} dt DataTable
   * @param {number} i The column associated with this handler
   * @private
   */
  function handleFilterOption (dt, i) {
    var ctx = dt.settings()[0];
    if (ctx._pfFilter.filterCols[i] === null || ctx._pfFilter.filterCols[i].optionSelector === undefined) {
      return;
    }
    $(ctx._pfFilter.filterCols[i].optionSelector).on("click", function (e) {
      // Set input placeholder
      if (ctx._pfFilter.filterInput !== undefined && ctx._pfFilter.filterInput.length !== 0) {
        ctx._pfFilter.filterInput.get(0).placeholder = ctx._pfFilter.filterCols[i].placeholder;
      }
      // Set filter label
      if (ctx._pfFilter.filterLabel !== undefined && ctx._pfFilter.filterLabel.length !== 0) {
        ctx._pfFilter.filterLabel.html($(this).text());
      }
      // Set filter button
      if (ctx._pfFilter.filterButton !== undefined && ctx._pfFilter.filterButton.length !== 0) {
        ctx._pfFilter.filterButton.html($(this).text() + ' <span class="caret"></span>');
      }
      ctx._pfFilter.filterColumn = i; // Save filter column when applying filter
      ctx._pfFilter.filterName = $(this).text(); // Save filter name for active filter control
    });
  }

  /**
   * Update active filter results
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function updateFilterResults (dt) {
    var ctx = dt.settings()[0];
    var filteredRows = dt.rows({"page": "current", "search": "applied"}).flatten().length;
    if (ctx._pfFilter.filterResults === undefined || ctx._pfFilter.filterResults.length === 0) {
      return;
    }
    ctx._pfFilter.filterResults.html(filteredRows + " Results");
  }

  // DataTables API

  /**
   * Add filter
   *
   * Example: dt.table().pfFilter.addFilter({
   *   column: 2,
   *   name: "Browser",
   *   value: "Firefox"
   * });
   *
   * @param {object} filter Properties associated with a new filter
   * @param {string} filter.column - Column associated with DataTable
   * @param {string} filter.name - Name of the filter
   * @param {string} filter.value - Value of the filter
   */
  DataTable.Api.register("pfFilter.addFilter()", function (filter) {
    return this.iterator("table", function (ctx) {
      addFilter(new DataTable.Api(ctx), filter);
    });
  });

  /**
   * Clear filters
   *
   * Example: dt.table().pfFilter.clearFilters();
   *
   */
  DataTable.Api.register("pfFilter.clearFilters()", function () {
    return this.iterator("table", function (ctx) {
      clearFilters(new DataTable.Api(ctx));
    });
  });

  // DataTables creation
  $(document).on("init.dt", function (e, ctx, json) {
    if (e.namespace !== "dt") {
      return;
    }
    DataTable.pfFilter.init(new DataTable.Api(ctx));
  });
  return DataTable.pfFilter;
}));
