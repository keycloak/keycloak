module.exports = {
  website: {
    assets: "./book",
    js: [
      "toggle.js"
    ],
    css: [
      "toggle.css"
    ],
    html: {
      "html:start": function() {
        return "<!-- Start book "+this.options.title+" -->"
      },
      "html:end": function() {
        return "<!-- End of book "+this.options.title+" -->"
      },

      "head:start": "<!-- head:start -->",
      "head:end": "<!-- head:end -->",

      "body:start": "<!-- body:start -->",
      "body:end": "<!-- body:end -->"
    }
  },
  hooks: {
    // For all the hooks, this represent the current generator

    // This is called before the book is generated
    "init": function() {
      //console.log("init!");
    },

    // This is called after the book generation
    "finish": function() {
      //console.log("finish!");
    }

  }
};
