/**
 * System configuration for Angular samples
 * Adjust as necessary for your application needs.
 */
(function (global) {
  System.config({
    paths: {
      // paths serve as alias
      'npm:': resourceUrl + '/node_modules/'
    },
    // map tells the System loader where to look for things
    map: {
      // our app is within the app folder
      'app': resourceUrl + '/app',
      'resources': resourceUrl,

      'react': 'npm:react/umd/react.development.js',
      'react-dom': 'npm:react-dom/umd/react-dom.development.js',
      'react-router-dom': 'npm:react-router-dom/umd/react-router-dom.js',
      
      //'@patternfly/patternfly': 'npm:@patternfly/patternfly',
      '@patternfly/patternfly': 'npm:@patternfly/react-core/dist/umd/@patternfly/patternfly',
      '@patternfly/react-core': 'npm:@patternfly/react-core/dist/umd/index.js',
      '@patternfly/react-styles': 'npm:@patternfly/react-styles/dist/umd/index.js',
      '@patternfly/react-icons': 'npm:@patternfly/react-icons/dist/umd/index.js',
      '@patternfly/react-tokens': 'npm:@patternfly/react-tokens/dist/umd/index.js',
      'emotion': 'npm:emotion/dist/emotion.umd.min.js',
      'camel-case': 'npm:camel-case/camel-case.js',
      'upper-case': 'npm:upper-case/upper-case.js',
      'no-case': 'npm:no-case/no-case.js',
      'lower-case': 'npm:lower-case/lower-case.js',
      'prop-types': 'npm:prop-types/prop-types.min.js',
      'exenv': 'npm:exenv/index.js',
      'focus-trap': 'npm:focus-trap/dist/focus-trap.min.js',
      'focus-trap-react': 'npm:focus-trap-react/dist/focus-trap-react.js',
      '@tippy.js/react': 'npm:@tippy.js/react/dist/Tippy.min.js',
      'tippy.js': 'npm:tippy.js/dist/tippy.min.js',
      
      'moment': 'npm:moment/min/moment-with-locales.min.js',
      
      'axios': 'npm:axios/dist/axios.min.js',
      
      'history': 'npm:history/umd/history.min.js',
    },
    
    bundles: {
        "npm:rxjs-system-bundle/Rx.system.min.js": [
          "rxjs",
          "rxjs/*",
          "rxjs/operator/*",
          "rxjs/observable/*",
          "rxjs/scheduler/*",
          "rxjs/symbol/*",
          "rxjs/add/operator/*",
          "rxjs/add/observable/*",
          "rxjs/util/*"
        ]
      },
      
    // packages tells the System loader how to load when no filename and/or no extension
    packages: {
      app: {
        defaultExtension: 'js',
        meta: {
          './*.js': {
          }
        }
      },
      rxjs: {
        defaultExtension: false
      },
      'npm:@patternfly/react-core/dist/umd/components': {
          main: './index.js',
          defaultExtension: 'js',
          map: {
              './Alert': './Alert/index.js',
              './AboutModal': './AboutModal/index.js',
              './ApplicationLauncher': './ApplicationLauncher/index.js',
              './Avatar': './Avatar/index.js',
              './Backdrop': './Backdrop/index.js',
              './BackgroundImage': './BackgroundImage/index.js',
              './Badge': './Badge/index.js',
              './Brand': './Brand/index.js',
              './Breadcrumb': './Breadcrumb/index.js',
              './Button': './Button/index.js',
              './Card': './Card/index.js',
              './Checkbox': './Checkbox/index.js',
              './ChipGroup': './ChipGroup/index.js',
              './ContextSelector': './ContextSelector/index.js',
              './DataList': './DataList/index.js',
              './Dropdown': './Dropdown/index.js',
              './EmptyState': './EmptyState/index.js',
              './Form': './Form/index.js',
              './FormSelect': './FormSelect/index.js',
              './InputGroup': './InputGroup/index.js',
              './Label': './Label/index.js',
              './List': './List/index.js',
              './LoginPage': './LoginPage/index.js',
              './Modal': './Modal/index.js',
              './Nav': './Nav/index.js',
              './Page': './Page/index.js',
              './Popover': './Popover/index.js',
              './Progress': './Progress/index.js',
              './Pagination': './Pagination/index.js',
              './Radio': './Radio/index.js',
              './Select': './Select/index.js',
              './Switch': './Switch/index.js',
              './Tabs': './Tabs/index.js',
              './Text': './Text/index.js',
              './TextArea': './TextArea/index.js',
              './TextInput': './TextInput/index.js',
              './Title': './Title/index.js',
              './Tooltip': './Tooltip/index.js',
              './Wizard': './Wizard/index.js',
              './Bullseye': './Bullseye/index.js',
              './Gallery': './Gallery/index.js',
              './Grid': './Grid/index.js',
              './Level': './Level/index.js',
              './Split': './Split/index.js',
              './Stack': './Stack/index.js',
              './Toolbar': './Toolbar/index.js',
          }
      },
      
      'npm:@patternfly/react-core/dist/umd/styles': {
          main: './index.js',
          defaultExtension: 'js',
      },
      'npm:@patternfly/react-core/dist/umd/helpers': {
          main: './index.js',
          defaultExtension: 'js',
      },
      'npm:@patternfly/react-core/dist/umd/layouts': {
          main: './index.js',
          defaultExtension: 'js',
          map: {
              './Bullseye': './Bullseye/index.js',
              './Gallery': './Gallery/index.js',
              './Level': './Level/index.js',
              './Grid': './Grid/index.js',
              './Stack': './Stack/index.js',
              './Split': './Split/index.js',
              './Toolbar': './Toolbar/index.js',
          }
      },
      'npm:@patternfly/react-core/dist/umd/@patternfly/patternfly/utilities/Accessibility': {
          defaultExtension: 'js',
          map: {
             './accessibility.css': './accessibility.css.js'
          }
      },
      'npm:@patternfly/react-icons/dist/umd': {
          main: './index.js',
          defaultExtension: 'js',
      },
      'npm:@patternfly/react-styles/dist/umd': {
          defaultExtension: 'js',
      },
      'npm:no-case/vendor': {
          defaultExtension: 'js',
      },
    }
  });
})(this);
