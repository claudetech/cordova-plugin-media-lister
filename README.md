# cordova-plugin-media-lister

Simple plugin providing a common interface to list files in media library.

## Usage

```javascript
mediaLister.readLibrary(function (results) {
    console.log(results);
    // [{
    //     "id": 4238,
    //     "dateModified": 1433332510,
    //     "title": "DSC_0003",
    //     "height": 2160,
    //     "width": 3840,
    //     "path": "/storage/emulated/0/DCIM/100ANDRO/DSC_0003.JPG",
    //     "dateAdded": 1433332510,
    //     "mediaType": "image",
    //     "mimeType": "image/jpeg",
    //     "size": "1811261"
    // }]
}, function (err) {
    console.log(err);
});
```
