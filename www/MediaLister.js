var exec = require('cordova/exec');

exports.readLibrary = function(options, success, error) {
  if (typeof options === 'function') {
    error = success;
    success = options;
    options = {};
  }
  exec(success, error, "MediaLister", "readLibrary", [options]);
};
