module.exports = function (grunt) {

  grunt.initConfig({

    // Import package manifest
    pkg: grunt.file.readJSON("bootstrap-touchspin.jquery.json"),

    // Banner definitions
    meta: {
      banner: "/*\n" +
        " *  <%= pkg.title || pkg.name %> - v<%= pkg.version %>\n" +
        " *  <%= pkg.description %>\n" +
        " *  <%= pkg.homepage %>\n" +
        " *\n" +
        " *  Made by <%= pkg.author.name %>\n" +
        " *  Under <%= pkg.licenses[0].type %> License\n" +
        " */\n"
    },

    // Concat definitions
    concat: {
      js: {
        src: ["src/jquery.bootstrap-touchspin.js"],
        dest: "dist/jquery.bootstrap-touchspin.js"
      },
      css: {
        src: ["src/jquery.bootstrap-touchspin.css"],
        dest: "dist/jquery.bootstrap-touchspin.css"
      },
      options: {
        banner: "<%= meta.banner %>"
      }
    },

    // Lint definitions
    jshint: {
      files: ["src/jquery.bootstrap-touchspin.js"],
      options: {
        jshintrc: ".jshintrc"
      }
    },

    // Minify definitions
    uglify: {
      js: {
        src: ["dist/jquery.bootstrap-touchspin.js"],
        dest: "dist/jquery.bootstrap-touchspin.min.js"
      },
      options: {
        banner: "<%= meta.banner %>"
      }
    },

    cssmin: {
      css: {
        src: ["dist/jquery.bootstrap-touchspin.css"],
        dest: "dist/jquery.bootstrap-touchspin.min.css"
      },
      options: {
        banner: "<%= meta.banner %>"
      }
    }
  });

  grunt.loadNpmTasks("grunt-contrib-concat");
  grunt.loadNpmTasks("grunt-contrib-jshint");
  grunt.loadNpmTasks("grunt-contrib-uglify");
  grunt.loadNpmTasks("grunt-contrib-cssmin");

  grunt.registerTask("default", ["jshint", "concat", "uglify", "cssmin"]);
  grunt.registerTask("travis", ["jshint"]);

};
