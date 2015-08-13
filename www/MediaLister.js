var exec = require('cordova/exec');

function clone(source) {
  var cloned = Object.create(source);
  for (var key in source ) {
    if (source.hasOwnProperty(key)) {
      var value = source[key];
      if (Object.prototype.toString.call(value) === '[object Object]') {
        cloned[key] = clone(value);
      } else {
        cloned[key] = value;
      }
    }
  }
  return cloned;
}

exports.readLibrary = function(options, success, error) {
  if (typeof options === 'function') {
    error = success;
    success = options;
    options = {};
  }
  var originalOptions = options;
  options = clone(originalOptions);

  if (options.thumbnail && !options.thumbnailSize) {
    if (typeof options.thumbnail === 'boolean') {
      options.thumbnailSize = {width: 400, height: 400};
    } else if (typeof options.thumbnail === 'number') {
      options.thumbnailSize = {
        width: options.thumbnail,
        height: options.thumbnail
      };
    } else if (typeof options.thumbnail === 'object') {
      var size = options.thumbnailSize = options.thumbnail;
      size.width = size.width || size.height || 400;
      size.height = size.height || size.width || 400;
    }
    options.thumbnail = true;
  }

  var originalSuccess = success;
  success = function (entries) {
    var nextOptions = clone(options);
    nextOptions.offset = (option.offset || 0) + entries.length;
    originalSuccess({entries: entries, nextOptions: nextOptions});
  };

  exec(success, error, "MediaLister", "readLibrary", [options]);
};
