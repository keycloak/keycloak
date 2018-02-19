/**
 * @summary     pfEmpty for DataTables
 * @description A collection of API methods providing functionality to hide and show elements when DataTables is empty.
 * This ensures DataTables meets the Patternfly design pattern with a toolbar and empty state.
 *
 * When DataTable is redrawn and data length is zero, controls under the toolbar-pf-actions and toolbar-pf-results
 * classes are disabled; including the filter drop down, filter input, action buttons, kebab, find, etc. (You may
 * re-enable specific controls via the DataTables "draw.dt" event.) In addition, the DataTables empty table header and
 * row are hidden while the empty state (i.e., blank slate) layout is shown.
 *
 * The toolbar and empty state layouts are expected to contain the classes as shown in the example below.
 *
 * Example:
 *
 * <!-- NOTE: Some configuration may be omitted for clarity -->
 * <div class="row toolbar-pf table-view-pf-toolbar" id="toolbar1">
 *   <div class="col-sm-12">
 *     <form class="toolbar-pf-actions">
 *       ...
 *     </form>
 *     <div class="row toolbar-pf-results">
 *       ...
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
 * <div class="blank-slate-pf table-view-pf-empty hidden" id="emptyState1">
 *   <div class="blank-slate-pf-icon">
 *     <span class="pficon pficon pficon-add-circle-o"></span>
 *   </div>
 *   <h1>Empty State Title</h1>
 *   ...
 * </div>
 * <script>
 * // NOTE: Some properties may be omitted for clarity
 * $(document).ready(function() {
 *   var dt = $("#table1").DataTable({
 *     columns: [
 *       { data: null, ... },
 *       { data: "engine" },
 *       { data: "browser" }
 *     ],
 *     data: null,
 *     dom: "t",
 *     pfConfig: {
 *       emptyStateSelector: "#emptyState1",
 *       ...
 *       toolbarSelector: "#toolbar1"
 *     }
 *   });
 *   dt.table().pfEmpty.updateState(); // Optional API to force update
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
  var ACTIONS_SELECTOR = ".toolbar-pf-actions"; // Toolbar actions
  var RESULTS_SELECTOR = ".toolbar-pf-results"; // Toolbar results row

  DataTable.pfEmpty = {};

  /**
   * Initialize
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  DataTable.pfEmpty.init = function (dt) {
    var ctx = dt.settings()[0];
    var opts = (ctx.oInit.pfConfig) ? ctx.oInit.pfConfig : {};

    ctx._pfEmpty = {};
    ctx._pfEmpty.emptyState = $(opts.emptyStateSelector); // Empty state (Blank slate)
    ctx._pfEmpty.isEmptyState = false; // Flag indicating DatTable entered an empty state
    ctx._pfEmpty.pagination = $(opts.paginationSelector); // Pagination
    ctx._pfEmpty.tbody = $("tbody", dt.table().container()); // Table body
    ctx._pfEmpty.thead = $("thead", dt.table().container()); // Table head
    ctx._pfEmpty.toolbarActions = $(ACTIONS_SELECTOR, opts.toolbarSelector); // Toolbar actions
    ctx._pfEmpty.toolbarResults = $(RESULTS_SELECTOR, opts.toolbarSelector); // Toolbar results row

    // Update table on DataTables draw event
    dt.on("draw.dt", function () {
      updateState(dt);
    });

    // Initialize
    updateState(dt);
  };

  // Local functions

  /**
   * Disable and hide elements when DataTables has no data
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function updateEmptyState (dt) {
    var ctx = dt.settings()[0];

    // Show blank slate
    if (ctx._pfEmpty.emptyState !== undefined && ctx._pfEmpty.emptyState.length !== 0) {
      ctx._pfEmpty.emptyState.removeClass("hidden");
    }
    // Hide zero records message
    if (ctx._pfEmpty.tbody !== undefined && ctx._pfEmpty.tbody.length !== 0) {
      ctx._pfEmpty.tbody.addClass("hidden");
    }
    // Hide column headers
    if (ctx._pfEmpty.thead !== undefined && ctx._pfEmpty.thead.length !== 0) {
      ctx._pfEmpty.thead.addClass("hidden");
    }
    // Disable all buttons
    if (ctx._pfEmpty.toolbarActions !== undefined && ctx._pfEmpty.toolbarActions.length !== 0) {
      $("button", ctx._pfEmpty.toolbarActions).prop("disabled", true);
    }
    // Disable all inputs
    if (ctx._pfEmpty.toolbarActions !== undefined && ctx._pfEmpty.toolbarActions.length !== 0) {
      $("input", ctx._pfEmpty.toolbarActions).prop("disabled", true);
    }
    // Hide results container
    if (ctx._pfEmpty.toolbarResults !== undefined && ctx._pfEmpty.toolbarResults.length !== 0) {
      ctx._pfEmpty.toolbarResults.children().addClass("hidden");
    }
    // Hide pagination
    if (ctx._pfEmpty.pagination !== undefined && ctx._pfEmpty.pagination.length !== 0) {
      ctx._pfEmpty.pagination.addClass("hidden");
    }
    // Enable on empty
    if (ctx._pfEmpty.enableOnEmpty !== undefined) {
      $(ctx._pfEmpty.enableOnEmpty).prop("disabled", false);
    }
    // Enable on empty
    if (ctx._pfEmpty.enableOnEmpty !== undefined) {
      $(ctx._pfEmpty.enableOnEmpty).prop("disabled", false);
    }
  }

  /**
   * Enable and show elements when DataTables has data
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function updateNonEmptyState (dt) {
    var ctx = dt.settings()[0];

    // Hide blank slate
    if (ctx._pfEmpty.emptyState !== undefined && ctx._pfEmpty.emptyState.length !== 0) {
      ctx._pfEmpty.emptyState.addClass("hidden");
    }
    // Show table body
    if (ctx._pfEmpty.tbody !== undefined && ctx._pfEmpty.tbody.length !== 0) {
      ctx._pfEmpty.tbody.removeClass("hidden");
    }
    // Show column headers
    if (ctx._pfEmpty.thead !== undefined && ctx._pfEmpty.thead.length !== 0) {
      ctx._pfEmpty.thead.removeClass("hidden");
    }
    // Enable all buttons
    if (ctx._pfEmpty.toolbarActions !== undefined && ctx._pfEmpty.toolbarActions.length !== 0) {
      $("button", ctx._pfEmpty.toolbarActions).prop("disabled", false);
    }
    // Enable all inputs
    if (ctx._pfEmpty.toolbarActions !== undefined && ctx._pfEmpty.toolbarActions.length !== 0) {
      $("input", ctx._pfEmpty.toolbarActions).prop("disabled", false);
    }
    // Show results container
    if (ctx._pfEmpty.toolbarResults !== undefined && ctx._pfEmpty.toolbarResults.length !== 0) {
      ctx._pfEmpty.toolbarResults.children().removeClass("hidden");
    }
    // Show pagination
    if (ctx._pfEmpty.pagination !== undefined && ctx._pfEmpty.pagination.length !== 0) {
      ctx._pfEmpty.pagination.removeClass("hidden");
    }
  }

  /**
   * Update elements upon empty DataTable state
   *
   * @param {DataTable.Api} dt DataTable
   * @private
   */
  function updateState (dt) {
    var ctx = dt.settings()[0];

    // Don't enable or show elements unless DataTable was empty
    if (dt.data().length === 0) {
      ctx._pfEmpty.isEmptyState = true;
      updateEmptyState(dt);
    } else if (ctx._pfEmpty.isEmptyState === true) {
      ctx._pfEmpty.isEmptyState = false;
      updateNonEmptyState(dt);
    }
  }

  // DataTables API

  /**
   * Update state upon empty or non-empty DataTable
   *
   * Example: dt.table().pfEmpty.updateState();
   */
  DataTable.Api.register("pfEmpty.updateState()", function () {
    return this.iterator("table", function (ctx) {
      updateState(new DataTable.Api(ctx));
    });
  });

  // DataTables creation
  $(document).on("init.dt", function (e, ctx, json) {
    if (e.namespace !== "dt") {
      return;
    }
    DataTable.pfEmpty.init(new DataTable.Api(ctx));
  });
  return DataTable.pfEmpty;
}));
