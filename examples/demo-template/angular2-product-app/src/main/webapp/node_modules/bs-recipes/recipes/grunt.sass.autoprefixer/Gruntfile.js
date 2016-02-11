module.exports = function (grunt) {
    grunt.initConfig({
        dirs: {
            css:  "app/css",
            scss: "app/scss"
        },
        watch: {
            options: {
                spawn: false
            },
            sass: {
                files: '<%= dirs.scss %>/**/*.scss',
                tasks: ['sass', 'autoprefixer', 'bsReload:css']
            },
            html: {
                files: 'app/*.html',
                tasks: ['bsReload:all']
            }
        },
        sass: {
            dev: {
                files: {
                    '<%= dirs.css %>/main.css': '<%= dirs.scss %>/main.scss'
                }
            }
        },
        autoprefixer: {
            options: {
                browsers: ['last 5 versions', 'ie 8']
            },
            css: {
                src: '<%= dirs.css %>/main.css',
                dest: '<%= dirs.css %>/main.css'
            }
        },
        browserSync: {
            dev: {
                options: {
                    server: "./app",
                    background: true
                }
            }
        },
        bsReload: {
            css: {
                reload: "main.css"
            },
            all: {
                reload: true
            }
        }
    });

    // load npm tasks
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-autoprefixer');
    grunt.loadNpmTasks('grunt-browser-sync');
    grunt.loadNpmTasks('grunt-contrib-watch');

    // define default task
    grunt.registerTask('default', ['browserSync', 'watch']);
};