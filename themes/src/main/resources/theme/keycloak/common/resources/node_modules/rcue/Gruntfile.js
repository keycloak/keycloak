/*global module,require*/
var lrSnippet = require('connect-livereload')({
  port: 35730
});

module.exports = function (grunt) {
  'use strict';

  // load all grunt tasks
  require('matchdep').filterDev('grunt-*').forEach(grunt.loadNpmTasks);

  // configurable paths
  var projectConfig = {
    dist: 'dist',
    src: ''
  };

  try {
    projectConfig.src = require('./bower.json').appPath || projectConfig.src;
  } catch (e) {}

  grunt.initConfig({
    clean: {
      build: '<%= config.dist %>'
    },
    config: projectConfig,
    connect: {
      server: {
        options: {
          hostname: '0.0.0.0',
          livereload: true,
          base: [
            projectConfig.dist,
            projectConfig.src,
            projectConfig.src + 'tests'
          ],
          port: 9001
        }
      }
    },
    run: {
      options: {
      },
      bundleInstall: {
        cmd: 'bundle',
        args: [
          'install'
        ]
      }
    },
    copy: {
      main: {
        files: [
          // copy Bootstrap font files
          {expand: true, cwd: 'node_modules/bootstrap/dist/fonts/', src: ['*'], dest: 'dist/fonts/'},
          // copy Font Awesome font files
          {expand: true, cwd: 'node_modules/font-awesome/fonts/', src: ['*'], dest: 'dist/fonts/'},
          // copy PatternFly font files
          {expand: true, cwd: 'node_modules/patternfly/dist/fonts/', src: ['**'], dest: 'dist/fonts/'},
          // copy PatternFly less files
          {expand: true, cwd: 'node_modules/patternfly/less/', src: ['**'], dest: 'less/lib/patternfly/'},
          // copy PatternFly images
          {expand: true, cwd: 'node_modules/patternfly/dist/img/', src: ['**'], dest: 'dist/img'},
        ],
      },
    },
    csscount: {
      production: {
        src: [
          'dist/css/rcue*.min.css'
        ],
        options: {
          maxSelectors: 4096
        }
      }
    },
    cssmin: {
      production: {
        files: [{
          expand: true,
          cwd: 'dist/css',
          src: ['rcue*.css', '!*.min.css'],
          dest: 'dist/css',
          ext: '.min.css',
        }],
        options: {
          sourceMap: true
        }
      }
    },
    jekyll: {
      options: {
        src: 'node_modules/patternfly/tests/pages',
        bundleExec: 'true'
      },
      tests: {
        options: {
          config: '_config.yml',
          dest: 'tests'
        }
      }
    },
    less: {
      rcue: {
        files: {
          "dist/css/rcue.css": "less/rcue.less"
        },
        options: {
          paths: [
            "less/",
            "node_modules/"
          ],
          strictMath: true,
          sourceMap: true,
          outputSourceFiles: true,
          sourceMapFilename: 'dist/css/rcue.css.map',
          sourceMapURL: 'rcue.css.map'
        }
      },
      rcueAdditions: {
        files: {
          "dist/css/rcue-additions.css": "less/rcue-additions.less"
        },
        options: {
          paths: [
            "less/",
            "node_modules/"
          ],
          strictMath: true,
          sourceMap: true,
          outputSourceFiles: true,
          sourceMapFilename: 'dist/css/rcue-additions.css.map',
          sourceMapURL: 'rcue-additions.css.map'
        }
      }
    },
    watch: {
      copy: {
        files: 'node_modules/**/*',
        tasks: ['copy']
      },
      jekyll: {
        files: ['_config.yml', 'node_modules/patternfly/tests/pages/**/*'],
        tasks: ['jekyll']
      },
      less: {
        files: ['less/*.less', 'node_modules/patternfly/dist/less/*.less'],
        tasks: ['less'],
      },
      css: {
        files: ['dist/css/rcue*.css', 'dist/css/!*.min.css'],
        tasks: ['cssmin', 'csscount']
      },
      livereload: {
        files: ['dist/css/*.css', 'tests/*.html']
      },
      options: {
        livereload: 35730
      }
    }
  });

  grunt.registerTask('build', [
    'run:bundleInstall',
    'copy',
    'jekyll',
    'less',
    'cssmin',
    'csscount'
  ]);

  grunt.registerTask('server', [
    'connect:server',
    'watch'
  ]);

  grunt.registerTask('default', ['build']);
};
