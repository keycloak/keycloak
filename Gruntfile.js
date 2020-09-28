const path = require("path");

module.exports = function (grunt) {
  grunt.loadNpmTasks("grunt-contrib-copy");

  grunt.initConfig({
    // …
    copy: {
      main: {
        cwd: "./node_modules/@patternfly/patternfly/assets/",
        src: ["**/*", '!**/fontawesome/**'],
        dest: "public/assets/",
        expand: true,
      },
      sso: {
        expand: true,
        cwd: "./public/",
        src: "rh-sso-*",
        dest: "public",
        rename: function (dest, matchedSrcPath) {
          return path.join(dest, matchedSrcPath.substring("rh-sso-".length));
        },
      },
    },
  });

  grunt.registerTask("default", ["copy:main"]);
  grunt.registerTask("switch-rh-sso", ["copy:sso"]);
};
