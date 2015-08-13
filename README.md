# cordova-plugin-media-lister

Simple plugin providing a common interface to list files in the media library.

## Features

* Easy media library query
* Android support (iOS coming soon)
* Pagination
* On the fly thumbnail generation (images only)

## Installation

```sh
$ cordova plugin add https://github.com/claudetech/cordova-plugin-media-lister.git
```

## Usage

```javascript
mediaLister.readLibrary({thumbnail: true, limit: 40, mediaTypes: ['image']}, function (result) {
    console.log(results.entries);
    // [{
    //     "id": 4238,
    //     "dateModified": 1433332510,
    //     "title": "DSC_0003",
    //     "height": 2160,
    //     "width": 3840,
    //     "path": "/storage/emulated/0/DCIM/100ANDRO/DSC_0003.JPG",
    //     "dateAdded": 1433332510,
    //     "mediaType": "image",
    //     "thumbnailPath": "/data/data/com.my.app/cache/12207-400x400.jpg",
    //     "mimeType": "image/jpeg",
    //     "size": "1811261"
    // }]
    console.log(result.nextOptions); // suport for pagination
    // {
    //     limit: 40,
    //     thumbnail: true,
    //     mediaTypes: ['image'],
    //     offset: 8
    // }
}, function (err) {
    console.log(err);
});
```

## TODO

* Limit thumbnail cache size

## License

This plugin is under the MIT license.
See [LICENSE](./LICENSE) for more info.
