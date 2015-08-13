import Foundation
import AssetsLibrary
import MobileCoreServices

// TODO: Rewrite in ObjC or Photo
@objc(HWPMediaLister) public class MediaLister: CDVPlugin{
    
    let library = ALAssetsLibrary()
    var success: Bool!
    var result:[[String: AnyObject]] = []
    var option: [String: AnyObject] = [:]
    
    public func readLibrary(command: CDVInvokedUrlCommand){
        var temp = command.arguments[0] as! [String: AnyObject]
        println(temp)
        temp["offset"] = 0
        loadMedia(temp)
        var pluginResult: CDVPluginResult! = nil
        if success == true {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsArray: result)
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
        }
        self.commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
    
    public func startLoad(thumbnail: Bool = true, limit: Int = 20, mediaTypes: [String] = ["image"], offset:Int = 0) -> [[String: AnyObject]]{
        option = ["thumbnail": thumbnail, "limit":limit , "mediaTypes": mediaTypes, "offset": offset]
        loadMedia(option)
        return result
    }

    private func loadMedia(option: [String: AnyObject]){
        success = false
        library.enumerateGroupsWithTypes(ALAssetsGroupSavedPhotos,usingBlock: {
            (group: ALAssetsGroup!, stop: UnsafeMutablePointer) in
            if group == nil{
                return
            }

            self.success = true

            if let filter = self.getFilter(option["mediaTypes"] as! [String]){
                group.setAssetsFilter(filter)
            } else {
                return
            }
            
            let num = group.numberOfAssets()
            let indexSet = self.getIndexSet(num, limit: option["limit"] as! Int, offset: option["offset"] as! Int)
            if indexSet == nil{
                return
            }
            
            group.enumerateAssetsAtIndexes(indexSet!, options: NSEnumerationOptions.Reverse){
                (asset:ALAsset!, id:Int , stop: UnsafeMutablePointer) in
                if asset != nil{
                    self.result.append(self.setDictionary(asset, id: id, option:option))
                }
            }
            }, failureBlock:{
                (myerror: NSError!) -> Void in
                println("error occurred: \(myerror.localizedDescription)")
            }

        )
        println(result)
    }
    
    // TODO: Add data of Location etc.
    private func setDictionary(asset: ALAsset, id: Int, option: [String: AnyObject]) -> [String: AnyObject]{
        var data: [String: AnyObject] = [:]
        data["id"] = id
        data["mediaType"] = setType(asset)
        var date: NSDate = asset.valueForProperty(ALAssetPropertyDate) as! NSDate
        data["dateAdded"] = date.timeIntervalSince1970
        data["path"] = asset.valueForProperty(ALAssetPropertyAssetURL)
        var rep = asset.defaultRepresentation()
        data["size"] = Int(rep.size())
        data["orientation"] = rep.metadata()["Orientation"]
        data["title"] = rep.filename()
        data["height"] = rep.dimensions().height
        data["wigth"] = rep.dimensions().width
        data["mimeType"] = UTTypeCopyPreferredTagWithClass(rep.UTI(), kUTTagClassMIMEType).takeUnretainedValue()
        if (option["thumbnail"] as! Bool) {
            data["thumbnailPath"] = saveThumbnail(asset, id: id)
        }
        return data
    }
    
    private func saveThumbnail(asset: ALAsset, id: Int) -> NSString{
        let thumbnail = asset.thumbnail().takeUnretainedValue()
        let image = UIImage(CGImage: thumbnail)
        let imageData = UIImageJPEGRepresentation(image, 0.8)
        
        let cacheDirPath: NSString = NSSearchPathForDirectoriesInDomains(.CachesDirectory, .UserDomainMask, true)[0] as! NSString
        let filePath = cacheDirPath.stringByAppendingPathComponent("\(id).jpeg")
        
        if imageData.writeToFile(filePath, atomically: true){
            return filePath
        } else {
            println("error occured: Cannot save thumbnail image")
            return ""
        }
    }
    
    private func setType(asset:ALAsset) -> String{
        let type = asset.valueForProperty(ALAssetPropertyType) as! String
        if type == ALAssetTypePhoto{
            return "image"
        } else if type == ALAssetTypeVideo {
            return "video"
        }
        return  ""
    }
    
    // TODO: Add music and playlist and audio
    private func getFilter(mediaTypes: [String]) -> ALAssetsFilter?{
        if contains(mediaTypes, "image"){
            if contains(mediaTypes, "video"){
                return ALAssetsFilter.allAssets()
            } else {
                return ALAssetsFilter.allPhotos()
            }
        } else if contains(mediaTypes, "video"){
            return ALAssetsFilter.allVideos()
        }
        return nil
    }
    
    private func getIndexSet(max: Int, limit:Int, offset: Int) -> NSIndexSet?{
        if offset >= max{
            return nil
        } else if offset + limit > max{
            return NSIndexSet(indexesInRange: NSMakeRange(0, max - offset))
        } else {
            return NSIndexSet(indexesInRange: NSMakeRange(max - offset - limit, limit))
        }
    }
}