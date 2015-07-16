var exec = require('cordova/exec');

exports.readLibrary = function(options, success, error) {
  if (typeof options === 'function') {
    error = success;
    success = options;
    options = {};
  }

  if (options.thumbnail) {
    if (typeof options.thumbnail === 'boolean') {
      options.thumbnailSize = {width: 400, height: 400};
    } else if (typeof options.thumbnail === 'number') {
      options.thumbnailSize = {width: options.thumbnail, height: options.thumbnail};
    } else if (typeof options.thumbnail === 'object') {
      options.thumbnailSize = options.thumbnail;
      options.thumbnailSize.width = options.thumbnailSize.width || options.thumbnailSize.height || 400;
      options.thumbnailSize.height = options.thumbnailSize.height || options.thumbnailSize.width || 400;
    }
    options.thumbnail = true;
  }

  var originalSuccess = success;
  success = function (results) {
    var lastResult = results[results.length];
    options.addedBefore = lastResult.dateAdded;
    originalSuccess(results, options);
  };

  exec(success, error, "MediaLister", "readLibrary", [options]);
};
