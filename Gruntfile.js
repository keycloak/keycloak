module.exports = function (grunt) {

  grunt.loadNpmTasks('grunt-contrib-copy');

  grunt.initConfig({
    // …
    copy: {
      files: {
        cwd: './node_modules/@patternfly/patternfly/assets/',
        src: '**/*',
        dest: 'public/assets/',
        expand: true
      }
    }
  });

  grunt.registerTask('default', ['copy']);

};