[![npm version](https://badge.fury.io/js/ng-file-upload.svg)](http://badge.fury.io/js/ng-file-upload)
[![Downloads](http://img.shields.io/npm/dm/ng-file-upload.svg)](https://npmjs.org/package/ng-file-upload)
[![Issue Stats](http://issuestats.com/github/danialfarid/ng-file-upload/badge/pr)](http://issuestats.com/github/danialfarid/ng-file-upload)
[![Issue Stats](http://issuestats.com/github/danialfarid/ng-file-upload/badge/issue)](http://issuestats.com/github/danialfarid/ng-file-upload)<br/>
[![PayPayl donate button](https://img.shields.io/badge/paypal-donate-yellow.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=danial%2efarid%40gmail%2ecom&lc=CA&item_name=ng%2dfile%2dupload&item_number=ng%2dfile%2dupload&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted)
[![Gratipay donate button](https://img.shields.io/gratipay/danialfarid.svg?label=donate)](https://gratipay.com/ng-file-upload/)

ng-file-upload
===================

Lightweight Angular directive to upload files.

**See the <a href="https://angular-file-upload.appspot.com/" target="_blank">DEMO</a> page.** Reference docs [here](https://github.com/danialfarid/ng-file-upload/blob/master/README.md#full-reference)

**Migration notes**: [version 3.0.x](https://github.com/danialfarid/ng-file-upload/releases/tag/3.0.0) [version 3.1.x](https://github.com/danialfarid/ng-file-upload/releases/tag/3.1.0) [version 3.2.x](https://github.com/danialfarid/ng-file-upload/releases/tag/3.2.3) [version 4.x.x](https://github.com/danialfarid/ng-file-upload/releases/tag/4.0.0) [version 5.x.x](https://github.com/danialfarid/ng-file-upload/releases/tag/5.0.0) [version 6.x.x](https://github.com/danialfarid/ng-file-upload/releases/tag/6.0.0) [version 6.2.x](https://github.com/danialfarid/ng-file-upload/releases/tag/6.2.0) [version 7.0.x](https://github.com/danialfarid/ng-file-upload/releases/tag/7.0.0) [version 7.2.x](https://github.com/danialfarid/ng-file-upload/releases/tag/7.2.0) [version 8.0.x](https://github.com/danialfarid/ng-file-upload/releases/tag/8.0.1) [version 9.0.x](https://github.com/danialfarid/ng-file-upload/releases/tag/9.0.0) [version 10.0.x](https://github.com/danialfarid/ng-file-upload/releases/tag/10.0.0) [version 11.0.x](https://github.com/danialfarid/ng-file-upload/releases/tag/11.0.0) [version 12.0.x](https://github.com/danialfarid/ng-file-upload/releases/tag/12.0.0) [version 12.1.x](https://github.com/danialfarid/ng-file-upload/releases/tag/12.1.0) [version 12.2.x](https://github.com/danialfarid/ng-file-upload/releases/tag/12.2.3)



Ask questions on [StackOverflow](http://stackoverflow.com/) under the [ng-file-upload](http://stackoverflow.com/tags/ng-file-upload/) tag.<br/>
For bug report or feature request please search through existing [issues](https://github.com/danialfarid/ng-file-upload/issues) first then open a new one  [here](https://github.com/danialfarid/ng-file-upload/issues/new). For faster response provide steps to reproduce/versions with a jsfiddle link. If you need support for your company contact [me](mailto:danial.farid@gmail.com).<br/>
If you like this plugin give it a thumbs up at [ngmodules](http://ngmodules.org/modules/ng-file-upload) or get me a <a target="_blank" href="https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=danial%2efarid%40gmail%2ecom&lc=CA&item_name=ng%2dfile%2dupload&item_number=ng%2dfile%2dupload&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted">cup of tea <img src="https://angular-file-upload.appspot.com/img/tea.png" width="40" height="24" title="Icon made by Freepik.com"></a>. Contributions are welcomed.


Table of Content:
* [Features](#features)
* [Install](#install) ([Manual](#manual), [Bower](#bower), [NuGet](#nuget), [NPM](#npm))
* [Usage](#usage)
* [Old Browsers](#old_browsers)
* [Server Side](#server)
  * [Samples](#server) ([Java](#java), [Spring](#spring), [Node.js](#node), [Rails](#rails), [PHP](#php), [.Net](#net))
  * [CORS](#cors)
  * [Amazon S3 Upload](#s3)

##<a name="features"></a> Features
* file upload progress, cancel/abort
* file drag and drop (html5 only) 
* image paste from clipboard and drag and drop from browser pages (html5 only).
* image resize and center crop (native) and user controlled crop through [ngImgCrop](https://github.com/alexk111/ngImgCrop). See [crop sample](http://jsfiddle.net/danialfarid/xxo3sk41/590/) (html5 only)
* orientation fix for jpeg image files with exif orientation data
* resumable uploads: pause/resume upload (html5 only) 
* native validation support for file type/size, image width/height/aspect ratio, video/audio duration, and `ng-required` with pluggable custom sync or async validations.
* show thumbnail or preview of selected images/audio/videos
* supports CORS and direct upload of file's binary data using `Upload.$http()`
* plenty of sample server side code, available on nuget
* on demand flash [FileAPI](https://github.com/mailru/FileAPI) shim loading no extra load for html5 browsers.
* HTML5 FileReader.readAsDataURL shim for IE8-9
* available on [npm](https://www.npmjs.com/package/ng-file-upload), [bower](https://libraries.io/bower/ng-file-upload), [meteor](https://atmospherejs.com/danialf/ng-file-upload), [nuget](https://www.nuget.org/packages/angular-file-upload)

##<a name="install"></a> Install

* <a name="manual"></a>**Manual**: download latest from [here](https://github.com/danialfarid/ng-file-upload-bower/releases/latest)
* <a name="bower"></a>**Bower**:
  * `bower install ng-file-upload-shim --save`(for non html5 suppport)
  * `bower install ng-file-upload --save`
* <a name="nuget"></a>**NuGet**: `PM> Install-Package angular-file-upload` (thanks to [Georgios Diamantopoulos](https://github.com/georgiosd))
* <a name="npm"></a>**NPM**: `npm install ng-file-upload`
```html
<script src="angular(.min).js"></script>
<script src="ng-file-upload-shim(.min).js"></script> <!-- for no html5 browsers support -->
<script src="ng-file-upload(.min).js"></script>
```

##<a name="usage"></a> Usage

###Samples:
* Upload with form submit and validations: [http://jsfiddle.net/danialfarid/maqbzv15/1118/](http://jsfiddle.net/danialfarid/maqbzv15/1118/)
* Upload multiple files one by one on file select:
[http://jsfiddle.net/danialfarid/2vq88rfs/136/](http://jsfiddle.net/danialfarid/2vq88rfs/136/)
* Upload multiple files in one request on file select (html5 only):
[http://jsfiddle.net/danialfarid/huhjo9jm/5/](http://jsfiddle.net/danialfarid/huhjo9jm/5/)
* Upload single file on file select:
[http://jsfiddle.net/danialfarid/0mz6ff9o/135/](http://jsfiddle.net/danialfarid/0mz6ff9o/135/)
* Drop and upload with $watch:
[http://jsfiddle.net/danialfarid/s8kc7wg0/400/](http://jsfiddle.net/danialfarid/s8kc7wg0/400/)
* Image Crop and Upload
[http://jsfiddle.net/danialfarid/xxo3sk41/590/](http://jsfiddle.net/danialfarid/xxo3sk41/590/)
```html
<script src="angular.min.js"></script>
<!-- shim is needed to support non-HTML5 FormData browsers (IE8-9)-->
<script src="ng-file-upload-shim.min.js"></script>
<script src="ng-file-upload.min.js"></script>

Upload on form submit or button click
<form ng-app="fileUpload" ng-controller="MyCtrl" name="form">
  Single Image with validations
  <div class="button" ngf-select ng-model="file" name="file" ngf-pattern="'image/*'"
    ngf-accept="'image/*'" ngf-max-size="20MB" ngf-min-height="100" 
    ngf-resize="{width: 100, height: 100}">Select</div>
  Multiple files
  <div class="button" ngf-select ng-model="files" ngf-multiple="true">Select</div>
  Drop files: <div ngf-drop ng-model="files" class="drop-box">Drop</div>
  <button type="submit" ng-click="submit()">submit</button>
</form>

Upload right away after file selection:
<div class="button" ngf-select="upload($file)">Upload on file select</div>
<div class="button" ngf-select="uploadFiles($files)" multiple="multiple">Upload on file select</div>
  Drop File:
<div ngf-drop="uploadFiles($files)" class="drop-box"
  ngf-drag-over-class="'dragover'" ngf-multiple="true" 
  ngf-pattern="'image/*,application/pdf'">Drop Images or PDFs files here</div>
<div ngf-no-file-drop>File Drag/Drop is not supported for this browser</div>

Image thumbnail: <img ngf-thumbnail="file || '/thumb.jpg'">
Audio preview: <audio controls ngf-src="file"></audio>
Video preview: <video controls ngf-src="file"></video>
```
Javascript code:
```js
//inject directives and services.
var app = angular.module('fileUpload', ['ngFileUpload']);

app.controller('MyCtrl', ['$scope', 'Upload', function ($scope, Upload) {
    // upload later on form submit or something similar
    $scope.submit = function() {
      if ($scope.form.file.$valid && $scope.file) {
        $scope.upload($scope.file);
      }
    };

    // upload on file select or drop
    $scope.upload = function (file) {
        Upload.upload({
            url: 'upload/url',
            data: {file: file, 'username': $scope.username}
        }).then(function (resp) {
            console.log('Success ' + resp.config.data.file.name + 'uploaded. Response: ' + resp.data);
        }, function (resp) {
            console.log('Error status: ' + resp.status);
        }, function (evt) {
            var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
            console.log('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
        });
    };
    // for multiple files:
    $scope.uploadFiles = function (files) {
      if (files && files.length) {
        for (var i = 0; i < files.length; i++) {
          Upload.upload({..., data: {file: files[i]}, ...})...;
        }
        // or send them all together for HTML5 browsers:
        Upload.upload({..., data: {file: files}, ...})...;
      }
    }
}]);
```

### Full reference

#### File select and drop

At least one of the `ngf-select` or `ngf-drop` are mandatory for the plugin to link to the element.
`ngf-select` only attributes are marked with * and `ngf-drop` only attributes are marked with +.

```html
<div|button|input type="file"|ngf-select|ngf-drop...
  ngf-select="" or "upload($files, ...)" // called when files are selected or cleared
  ngf-drop="" or "upload($files, ...)" // called when files being dropped
    // You can use ng-model or ngf-change instead of specifying function for ngf-drop and ngf-select
    // function parameters are the same as ngf-change
  ngf-change="upload($files, $file, $newFiles, $duplicateFiles, $invalidFiles, $event)"
    // called when files are selected, dropped, or cleared
  ng-model="myFiles" // binds the valid selected/dropped file or files to the scope model
    // could be an array or single file depending on ngf-multiple and ngf-keep values.
  ngf-model-options="{updateOn: 'change click drop dropUrl paste', allowInvalid: false, debounce: 0}"
    // updateOn could be used to disable resetting on click, or updating on paste, browser image drop, etc. 
    // allowInvalid default is false could allow invalid files in the model
    // debouncing will postpone model update (miliseconds). See angular ng-model-options for more details.
  ngf-model-invalid="invalidFile(s)" // binds the invalid selected/dropped file or files to this model.
  ngf-before-model-change="beforeChange($files, ...)" // called after file select/drop and before 
    // model change, validation and resize is processed
  ng-disabled="boolean" // disables this element
  ngf-select-disabled="boolean" // default false, disables file select on this element
  ngf-drop-disabled="boolean" // default false, disables file drop on this element
  ngf-multiple="boolean" // default false, allows selecting multiple files
  ngf-keep="true|false|'distinct'" // default false, keep the previous ng-model files and 
    // append the new files. "'distinct'" removes duplicate files
    // $newFiles and $duplicateFiles are set in ngf-change/select/drop functions.
  ngf-fix-orientation="boolean" //default false, would rotate the jpeg image files that have
    // exif orientation data. See #745. Could be a boolean function like shouldFixOrientation($file) 
    // to decide wethere to fix that file or not.
  
  *ngf-capture="'camera'" or "'other'" // allows mobile devices to capture using camera
  *ngf-accept="'image/*'" // standard HTML accept attr, browser specific select popup window
  
  +ngf-allow-dir="boolean" // default true, allow dropping files only for Chrome webkit browser
  +ngf-include-dir="boolean" //default false, include directories in the dropped file array. 
    //You can detect if they are directory or not by checking the type === 'directory'.
  +ngf-drag-over-class="{pattern: 'image/*', accept:'acceptClass', reject:'rejectClass', delay:100}" 
                    or "'myDragOverClass'" or "calcDragOverClass($event)"
    // default "dragover". drag over css class behaviour. could be a string, a function 
    // returning class name or a json object.
    // accept/reject class only works in Chrome, validating only the file mime type.
    // if pattern is not specified ngf-pattern will be used. See following docs for more info.
  +ngf-drag="drag($isDragging, $class, $event)" // function called on drag over/leave events.
    // $isDragging: boolean true if is dragging over(dragover), false if drag has left (dragleave)
    // $class is the class that is being set for the element calculated by ngf-drag-over-class
  +ngf-drop-available="dropSupported" // set the value of scope model to true or false based on file
                                     // drag&drop support for this browser
  +ngf-stop-propagation="boolean" // default false, whether to propagate drag/drop events.
  +ngf-hide-on-drop-not-available="boolean" // default false, hides element if file drag&drop is not
  +ngf-enable-firefox-paste="boolean" // *experimental* default false, enable firefox image paste by making element contenteditable

  ngf-resize="{width: 100, height: 100, quality: .8, type: 'image/jpeg', 
               ratio: '1:2', centerCrop: true, pattern='.jpg', restoreExif: false}" 
               or resizeOptions() // a function returning a promise which resolves into the options.
    // resizes the image to the given width/height or ratio. Quality is optional between 0.1 and 1.0), 
    // type is optional convert it to the given image type format.
    // centerCrop true will center crop the image if it does not fit within the given width/height or ratio. 
    // centerCrop false (default) will not crop the image and will fit it within the given width/height or ratio 
    // so the resulting image width (or height) could be less than given width (or height).
    // pattern is to resize only the files that their name or type matches the pattern similar to ngf-pattern.
    // restoreExif boolean default true, will restore exif info on the resized image.
  ngf-resize-if="$width > 1000 || $height > 1000" or "resizeCondition($file, $width, $height)"
    // apply ngf-resize only if this function returns true. To filter specific images to be resized.
  ngf-validate-after-resize="boolean" // default false, if true all validation will be run after 
    // the images are being resized, so any validation error before resize will be ignored.
          
  //validations:
  ngf-max-files="10" // maximum number of files allowed to be selected or dropped, validate error name: maxFiles
  ngf-pattern="'.pdf,.jpg,video/*,!.jog'" // comma separated wildcard to filter file names and types allowed
              // you can exclude specific files by ! at the beginning.
              // validate error name: pattern
  ngf-min-size, ngf-max-size, ngf-max-total-size="100" in bytes or "'10KB'" or "'10MB'" or "'10GB'"
              // validate as form.file.$error.maxSize=true and file.$error='maxSize'
              // ngf-max-total-size is for multiple file select and validating the total size of all files.
  ngf-min-height, ngf-max-height, ngf-min-width, ngf-max-width="1000" in pixels only images
              // validate error names: minHeight, maxHeight, minWidth, maxWidth
  ngf-ratio="8:10,1.6" // list of comma separated valid aspect ratio of images in float or 2:3 format
              // validate error name: ratio
  ngf-min-ratio, ngf-max-ratio="8:10" // min or max allowed aspect ratio for the image.
  ngf-dimensions="$width > 1000 || $height > 1000" or "validateDimension($file, $width, $height)"
              // validate the image dimensions, validate error name: dimensions
  ngf-min-duration, ngf-max-duration="100.5" in seconds or "'10s'" or "'10m'" or "'10h'" only audio, video
              // validate error name: maxDuration
  ngf-duration="$duration > 1000" or "validateDuration($file, $duration)"
              // validate the media duration, validate error name: duration

  ngf-validate="{size: {min: 10, max: '20MB'}, width: {min: 100, max:10000}, height: {min: 100, max: 300}
                ratio: '2x1', duration: {min: '10s', max: '5m'}, pattern: '.jpg'}"
                shorthand form for above validations in one place.
  ngf-validate-fn="validate($file)" // custom validation function, return boolean or string containing the error.
              // validate error name: validateFn
  ngf-validate-async-fn="validate($file)" // custom validation function, return a promise that resolve to
              // boolean or string containing the error. validate error name: validateAsyncFn
  ngf-validate-force="boolean" // default false, if true file.$error will be set if the dimension or duration
              // values for validations cannot be calculated for example image load error or unsupported video by the browser.
              // by default it would assume the file is valid if the duration or dimension cannot be calculated by the browser.
  ngf-ignore-invalid="'pattern maxSize'" // ignore the files that fail the specified validations. They will 
              // just be ignored and will not show up in ngf-model-invalid or make the form invalid.
              // space separated list of validate error names.
  ngf-run-all-validations="boolean" // default false. Runs all the specified validate directives. By default
              // once a validation fails for a file it would stop running other validations for that file.

>Upload/Drop</div>

<div|... ngf-no-file-drop>File Drag/drop is not supported</div>

// filter to convert the file to base64 data url.
<a href="file | ngfDataUrl">image</a>
```

#### File preview
```html
<img|audio|video|div
  *ngf-src="file" //To preview the selected file, sets src attribute to the file data url.
  *ngf-background="file" //sets background-image style to the file data url.
  ngf-resize="{width: 20, height: 20, quality: 0.9}" // only for image resizes the image before setting it
             // as src or background image. quality is optional.
  ngf-no-object-url="true or false" // see #887 to force base64 url generation instead of object url. Default false
>

<div|span|...
 *ngf-thumbnail="file" //Generates a thumbnail version of the image file
 ngf-size="{width: 20, height: 20, quality: 0.9}" the image will be resized to this size
        // if not specified will be resized to this element`s client width and height.
 ngf-as-background="boolean" //if true it will set the background image style instead of src attribute.
>
```

#### Upload service:
```js
var upload = Upload.upload({
  *url: 'server/upload/url', // upload.php script, node.js route, or servlet url
  /*
  Specify the file and optional data to be sent to the server.
  Each field including nested objects will be sent as a form data multipart.
  Samples: {pic: file, username: username}
    {files: files, otherInfo: {id: id, person: person,...}} multiple files (html5)
    {profiles: {[{pic: file1, username: username1}, {pic: file2, username: username2}]} nested array multiple files (html5)
    {file: file, info: Upload.json({id: id, name: name, ...})} send fields as json string
    {file: file, info: Upload.jsonBlob({id: id, name: name, ...})} send fields as json blob, 'application/json' content_type
    {picFile: Upload.rename(file, 'profile.jpg'), title: title} send file with picFile key and profile.jpg file name*/
  *data: {key: file, otherInfo: uploadInfo},
  /*
  This is to accommodate server implementations expecting nested data object keys in .key or [key] format.
  Example: data: {rec: {name: 'N', pic: file}} sent as: rec[name] -> N, rec[pic] -> file  
     data: {rec: {name: 'N', pic: file}}, objectKey: '.k' sent as: rec.name -> N, rec.pic -> file */  
  objectKey: '[k]' or '.k' // default is '[k]'
  /*
  This is to accommodate server implementations expecting array data object keys in '[i]' or '[]' or 
  ''(multiple entries with same key) format.
  Example: data: {rec: [file[0], file[1], ...]} sent as: rec[0] -> file[0], rec[1] -> file[1],...  
    data: {rec: {rec: [f[0], f[1], ...], arrayKey: '[]'} sent as: rec[] -> f[0], rec[] -> f[1],...*/  
  arrayKey: '[i]' or '[]' or '.i' or '' //default is '[i]'
  method: 'POST' or 'PUT'(html5), default POST,
  headers: {'Authorization': 'xxx'}, // only for html5
  withCredentials: boolean,
  /*
  See resumable upload guide below the code for more details (html5 only) */
  resumeSizeUrl: '/uploaded/size/url?file=' + file.name // uploaded file size so far on the server.
  resumeSizeResponseReader: function(data) {return data.size;} // reads the uploaded file size from resumeSizeUrl GET response
  resumeSize: function() {return promise;} // function that returns a prommise which will be
                                            // resolved to the upload file size on the server.
  resumeChunkSize: 10000 or '10KB' or '10MB' // upload in chunks of specified size
  disableProgress: boolean // default false, experimental as hotfix for potential library conflicts with other plugins
  ... and all other angular $http() options could be used here.
})

// returns a promise
upload.then(function(resp) {
  // file is uploaded successfully
  console.log('file ' + resp.config.data.file.name + 'is uploaded successfully. Response: ' + resp.data);
}, function(resp) {
  // handle error
}, function(evt) {
  // progress notify
  console.log('progress: ' + parseInt(100.0 * evt.loaded / evt.total) + '% file :'+ evt.config.data.file.name);
});
upload.catch(errorCallback);
upload.finally(callback, notifyCallback);

/* access or attach event listeners to the underlying XMLHttpRequest */
upload.xhr(function(xhr){
  xhr.upload.addEventListener(...)
});

/* cancel/abort the upload in progress. */
upload.abort();

/* 
alternative way of uploading, send the file binary with the file's content-type.
Could be used to upload files to CouchDB, imgur, etc... html5 FileReader is needed.
This is equivalent to angular $http() but allow you to listen to the progress event for HTML5 browsers.*/
Upload.http({
  url: '/server/upload/url',
  headers : {
    'Content-Type': file.type
  },
  data: file
})

/* Set the default values for ngf-select and ngf-drop directives*/
Upload.setDefaults({ngfMinSize: 20000, ngfMaxSize:20000000, ...})

// These two defaults could be decreased if you experience out of memory issues 
// or could be increased if your app needs to show many images on the page.
// Each image in ngf-src, ngf-background or ngf-thumbnail is stored and referenced as a blob url
// and will only be released if the max value of the followings is reached.
Upload.defaults.blobUrlsMaxMemory = 268435456 // default max total size of files stored in blob urls.
Upload.defaults.blobUrlsMaxQueueSize = 200 // default max number of blob urls stored by this application.

/* Convert a single file or array of files to a single or array of 
base64 data url representation of the file(s).
Could be used to send file in base64 format inside json to the databases */
Upload.base64DataUrl(files).then(function(urls){...});

/* Convert the file to blob url object or base64 data url based on boolean disallowObjectUrl value */
Upload.dataUrl(file, boolean).then(function(url){...});

/* Get image file dimensions*/
Upload.imageDimensions(file).then(function(dimensions){console.log(dimensions.width, dimensions.height);});

/* Get audio/video duration*/
Upload.mediaDuration(file).then(function(durationInSeconds){...});

/* Resizes an image. Returns a promise */
// options: width, height, quality, type, ratio, centerCrop, resizeIf, restoreExif
//resizeIf(width, height) returns boolean. See ngf-resize directive for more details of options.
Upload.resize(file, options).then(function(resizedFile){...});

/* returns boolean showing if image resize is supported by this browser*/
Upload.isResizeSupported()
/* returns boolean showing if resumable upload is supported by this browser*/
Upload.isResumeSupported()

/* returns a file which will be uploaded with the newName instead of original file name */
Upload.rename(file, newName)
/* converts the object to a Blob object with application/json content type 
for jsob byte streaming support #359 (html5 only)*/
Upload.jsonBlob(obj)
/* converts the value to json to send data as json string. Same as angular.toJson(obj) */
Upload.json(obj)
/* converts a dataUrl to Blob object.*/
var blob = upload.dataUrltoBlob(dataurl, name);
/* returns true if there is an upload in progress. Can be used to prompt user before closing browser tab */
Upload.isUploadInProgress() boolean
/* downloads and converts a given url to Blob object which could be added to files model */
Upload.urlToBlob(url).then(function(blob) {...});
/* returns boolean to check if the object is file and could be used as file in Upload.upload()/http() */
Upload.isFile(obj);
/* fixes the exif orientation of the jpeg image file*/
Upload.applyExifRotation(file).then(...)
```
**ng-model**
The model value will be a single file instead of an array if all of the followings are true:
  * `ngf-multiple` is not set or is resolved to false.
  * `multiple` attribute is not set on the element
  * `ngf-keep` is not set or is resolved to false.

**validation**
When any of the validation directives specified the form validation will take place and
you can access the value of the validation using `myForm.myFileInputName.$error.<validate error name>`
for example `form.file.$error.pattern`.
If multiple file selection is allowed you can specify `ngf-model-invalid="invalidFiles"` to assing the invalid files to 
a model and find the error of each individual file with `file.$error` and description of it with `file.$errorParam`.
You can use angular ngf-model-options to allow invalid files to be set to the ng-model  `ngf-model-options="{allowInvalid: true}"`.

**Upload multiple files**: Only for HTML5 FormData browsers (not IE8-9) you have an array of files or more than one file in your `data` to 
send them all in one request . Non-html5 browsers due to flash limitation will upload each file one by one in a separate request. 
You should iterate over the files and send them one by one for a cross browser solution.

**drag and drop styling**: For file drag and drop, `ngf-drag-over-class` could be used to style the drop zone. 
It can be a function that returns a class name based on the $event. Default is "dragover" string.
Only in chrome It could be a json object `{accept: 'a', 'reject': 'r', pattern: 'image/*', delay: 10}` that specify the 
class name for the accepted or rejected drag overs. The `pattern` specified or `ngf-pattern` will be used to validate the file's `mime-type` 
since that is the only property of the file that is reported by the browser on drag. So you cannot validate 
the file name/extension, size or other validations on drag. There is also some limitation on some file types which are not reported by Chrome.
`delay` default is 100, and is used to fix css3 transition issues from dragging over/out/over [#277](https://github.com/danialfarid/angular-file-upload/issues/277).

**Upload.setDefaults()**:
If you have many file selects or drops you can set the default values for the directives by calling `Upload.setDefaults(options)`. `options` would be a json object with directive names in camelcase and their default values.

**Resumable Uploads:**
The plugin supports resumable uploads for large files. 
On your server you need to keep track of what files are being uploaded and how much of the file is uploaded.
 * `url` upload endpoint need to reassemble the file chunks by appending uploading content to the end of the file or correct chunk position if it already exists.
 * `resumeSizeUrl` server endpoint to return uploaded file size so far on the server to be able to resume the upload from 
 where it is ended. It should return zero if the file has not been uploaded yet. <br/>A GET request will be made to that 
 url for each upload to determine if part of the file is already uploaded or not. You need a unique way of identifying the file
  on the server so you can pass the file name or generated id for the file as a request parameter.<br/>
 By default it will assume that the response 
 content is an integer or a json object with `size` integer property. If you return other formats from the endpoint you can specify 
 `resumeSizeResponseReader` function to return the size value from the response. Alternatively instead of `resumeSizeUrl` you can use 
 `resumeSize` function which returns a promise that resolves to the size of the uploaded file so far.
 Make sure when the file is fully uploaded without any error/abort this endpoint returns zero for the file size 
 if you want to let the user to upload the same file again. Or optionally you could have a restart endpoint to 
 set that back to zero to allow re-uploading the same file.
 * `resumeChunkSize` optionally you can specify this to upload the file in chunks to the server. This will allow uploading to GAE or other servers that have 
 file size limitation and trying to upload the whole request before passing it for internal processing.<br/>
 If this option is set the requests will have the following extra fields: 
 `_chunkSize`, `_currentChunkSize`, `_chunkNumber` (zero starting), and `_totalSize` to help the server to write the uploaded chunk to 
 the correct position.
 Uploading in chunks could slow down the overall upload time specially if the chunk size is too small.
 When you provide `resumeChunkSize` option one of the `resumeSizeUrl` or `resumeSize` is mandatory to know how much of the file is uploaded so far.
 



##<a name="old_browsers"></a> Old browsers

For browsers not supporting HTML5 FormData (IE8, IE9, ...) [FileAPI](https://github.com/mailru/FileAPI) module is used.
**Note**: You need Flash installed on your browser since `FileAPI` uses Flash to upload files.

These two files  **`FileAPI.min.js`, `FileAPI.flash.swf`** will be loaded by the module on demand (no need to be included in the html) if the browser does not supports HTML5 FormData to avoid extra load for HTML5 browsers.
You can place these two files beside `angular-file-upload-shim(.min).js` on your server to be loaded automatically from the same path or you can specify the path to those files if they are in a different path using the following script:
```html
<script>
    //optional need to be loaded before angular-file-upload-shim(.min).js
    FileAPI = {
        //only one of jsPath or jsUrl.
        jsPath: '/js/FileAPI.min.js/folder/',
        jsUrl: 'yourcdn.com/js/FileAPI.min.js',

        //only one of staticPath or flashUrl.
        staticPath: '/flash/FileAPI.flash.swf/folder/',
        flashUrl: 'yourcdn.com/js/FileAPI.flash.swf',

        //forceLoad: true, html5: false //to debug flash in HTML5 browsers
        //noContentTimeout: 10000 (see #528)
    }
</script>
<script src="angular-file-upload-shim.min.js"></script>...
```
**Old browsers known issues**:
* Because of a Flash limitation/bug if the server doesn't send any response body the status code of the response will be always `204 'No Content'`. So if you have access to your server upload code at least return a character in the response for the status code to work properly.
* Custom headers will not work due to a Flash limitation [#111](https://github.com/danialfarid/ng-file-upload/issues/111) [#224](https://github.com/danialfarid/ng-file-upload/issues/224) [#129](https://github.com/danialfarid/ng-file-upload/issues/129)
* Due to Flash bug [#92](https://github.com/danialfarid/ng-file-upload/issues/92) Server HTTP error code 400 will be returned as 200 to the client. So avoid returning 400 on your server side for upload response otherwise it will be treated as a success response on the client side.
* In case of an error response (http code >= 400) the custom error message returned from the server may not be available. For some error codes flash just provide a generic error message and ignores the response text. [#310](https://github.com/danialfarid/ng-file-upload/issues/310)
* Older browsers won't allow `PUT` requests. [#261](https://github.com/danialfarid/ng-file-upload/issues/261)

##<a name="server"></a>Server Side

* <a name="java"></a>**Java**
You can find the sample server code in Java/GAE [here](https://github.com/danialfarid/ng-file-upload/blob/master/demo/src/main/java/com/df/angularfileupload/)
* <a name="spring"></a>**Spring MVC**
[Wiki Sample](https://github.com/danialfarid/ng-file-upload/wiki/spring-mvc-example) provided by [zouroto](https://github.com/zouroto)
* <a name="node"></a>**Node.js**
[Wiki Sample](https://github.com/danialfarid/ng-file-upload/wiki/node.js-example) provided by [chovy](https://github.com/chovy).
[Another wiki](https://github.com/danialfarid/ng-file-upload/wiki/Node-example) using Express 4.0 and the Multiparty provided by [Jonathan White](https://github.com/JonathanZWhite)
* <a name="rails"></a>**Rails**
  * [Wiki Sample](https://github.com/danialfarid/ng-file-upload/wiki/Rails-Example) provided by [guptapriyank](https://github.com/guptapriyank).
  * [Blog post](http://www.coshx.com/blog/2015/07/10/file-attachments-in-angular/)
provided by [Coshx Labs](http://www.coshx.com/).
  * **Rails progress event**: If your server is Rails and Apache you may need to modify server configurations for the server to support upload progress. See [#207](https://github.com/danialfarid/ng-file-upload/issues/207)
* <a name="php"></a>**PHP**
[Wiki Sample] (https://github.com/danialfarid/ng-file-upload/wiki/PHP-Example) and related issue [only one file in $_FILES when uploading multiple files] (https://github.com/danialfarid/ng-file-upload/issues/475)
* <a name="net"></a>**.Net**
  * [Demo](https://github.com/stewartm83/angular-fileupload-sample) showing how to use ng-file-upload with Asp.Net Web Api.
  * Sample client and server code [demo/C#] (https://github.com/danialfarid/ng-file-upload/tree/master/demo/C%23) provided by [AtomStar](https://github.com/AtomStar)

##<a name="cors"></a>CORS
To support CORS upload your server needs to allow cross domain requests. You can achieve that by having a filter or interceptor on your upload file server to add CORS headers to the response similar to this:
([sample java code](https://github.com/danialfarid/ng-file-upload/blob/master/demo/src/main/java/com/df/angularfileupload/CORSFilter.java))
```java
httpResp.setHeader("Access-Control-Allow-Methods", "POST, PUT, OPTIONS");
httpResp.setHeader("Access-Control-Allow-Origin", "your.other.server.com");
httpResp.setHeader("Access-Control-Allow-Headers", "Content-Type"));
```
For non-HTML5 IE8-9 browsers you would also need a `crossdomain.xml` file at the root of your server to allow CORS for flash:
<a name="crossdomain"></a>([sample xml](https://angular-file-upload.appspot.com/crossdomain.xml))
```xml
<cross-domain-policy>
  <site-control permitted-cross-domain-policies="all"/>
  <allow-access-from domain="angular-file-upload.appspot.com"/>
  <allow-http-request-headers-from domain="*" headers="*" secure="false"/>
</cross-domain-policy>
```

#### <a name="s3"></a>Amazon AWS S3 Upload
For Amazon authentication version 4 [see this comment](https://github.com/danialfarid/ng-file-upload/issues/1128#issuecomment-196203268) 

The <a href="https://angular-file-upload.appspot.com/" target="_blank">demo</a> page has an option to upload to S3.
Here is a sample config options:
```js
Upload.upload({
    url: 'https://angular-file-upload.s3.amazonaws.com/', //S3 upload url including bucket name
    method: 'POST',
    data: {
        key: file.name, // the key to store the file on S3, could be file name or customized
        AWSAccessKeyId: <YOUR AWS AccessKey Id>,
        acl: 'private', // sets the access to the uploaded file in the bucket: private, public-read, ...
        policy: $scope.policy, // base64-encoded json policy (see article below)
        signature: $scope.signature, // base64-encoded signature based on policy string (see article below)
        "Content-Type": file.type != '' ? file.type : 'application/octet-stream', // content type of the file (NotEmpty)
        filename: file.name, // this is needed for Flash polyfill IE8-9
        file: file
    }
});
```
[This article](http://aws.amazon.com/articles/1434/) explains more about these fields and provides instructions on how to generate the policy and signature using a server side tool.
These two values are generated from the json policy document which looks like this:
```js
{
    "expiration": "2020-01-01T00:00:00Z",
    "conditions": [
        {"bucket": "angular-file-upload"},
        ["starts-with", "$key", ""],
        {"acl": "private"},
        ["starts-with", "$Content-Type", ""],
        ["starts-with", "$filename", ""],
        ["content-length-range", 0, 524288000]
    ]
}
```
The [demo](https://angular-file-upload.appspot.com/) page provide a helper tool to generate the policy and signature from you from the json policy document. **Note**: Please use https protocol to access demo page if you are using this tool to generate signature and policy to protect your aws secret key which should never be shared.

Make sure that you provide upload and CORS post to your bucket at AWS -> S3 -> bucket name -> Properties -> Edit bucket policy and Edit CORS Configuration. Samples of these two files:
```js
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UploadFile",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::xxxx:user/xxx"
      },
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::angular-file-upload/*"
    },
    {
      "Sid": "crossdomainAccess",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::angular-file-upload/crossdomain.xml"
    }
  ]
}
```
```xml
<?xml version="1.0" encoding="UTF-8"?>
<CORSConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
    <CORSRule>
        <AllowedOrigin>http://angular-file-upload.appspot.com</AllowedOrigin>
        <AllowedMethod>POST</AllowedMethod>
        <AllowedMethod>GET</AllowedMethod>
        <AllowedMethod>HEAD</AllowedMethod>
        <MaxAgeSeconds>3000</MaxAgeSeconds>
        <AllowedHeader>*</AllowedHeader>
    </CORSRule>
</CORSConfiguration>
```

For IE8-9 flash polyfill you need to have a <a href='#crossdomain'>crossdomain.xml</a> file at the root of you S3 bucket. Make sure the content-type of crossdomain.xml is text/xml and you provide read access to this file in your bucket policy.


You can also have a look at [https://github.com/nukulb/s3-angular-file-upload](https://github.com/nukulb/s3-angular-file-upload) for another example with [this](https://github.com/danialfarid/ng-file-upload/issues/814#issuecomment-112198426) fix.




