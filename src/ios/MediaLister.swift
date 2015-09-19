import Foundation
import AssetsLibrary
import MobileCoreServices

// TODO: Rewrite in ObjC or Photo
@objc(HWPMediaLister) public class MediaLister: CDVPlugin{
    
    let library = ALAssetsLibrary()
    let priority = DISPATCH_QUEUE_PRIORITY_DEFAULT
    
    var command: CDVInvokedUrlCommand!
    var option: [String: AnyObject] = [:]
    
    var result:[[String: AnyObject]] = []
    var success: Bool!
    
    public func readLibrary(command: CDVInvokedUrlCommand){
        dispatch_async(dispatch_get_global_queue(priority, 0)){
            self.result = []
            self.command = command
            let temp = command.arguments[0] as! [String: AnyObject]
            self.option = self.initailizeOption(temp)
            self.loadMedia(self.option)
        }
    }
    
    public func startLoad(thumbnail: Bool = true, limit: Int = 20, mediaTypes: [String] = ["image"], offset:Int = 0) -> [[String: AnyObject]]{
        option = ["thumbnail": thumbnail, "limit":limit , "mediaTypes": mediaTypes, "offset": offset]
        loadMedia(option)
        return result
    }
    
    private func initailizeOption(option:[String: AnyObject]) -> [String: AnyObject]{
        var tempOption = option
        if tempOption["offset"] == nil{
            tempOption["offset"] = 0
        }
        if tempOption["limit"]  == nil{
            tempOption["limit"] = 20
        }
        if tempOption["thumbnail"] == nil{
            tempOption["thumbnail"] = true
        }
        if tempOption["mediaTypes"] == nil{
            tempOption["mdeiaTypes"] = ["image"]
        }
        return tempOption
    }

    private func sendResult(){
        dispatch_async(dispatch_get_main_queue()){
            var pluginResult: CDVPluginResult! = nil
            if self.success == true {
                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsArray: self.result)
            } else {
                pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
            }
            self.commandDelegate?.sendPluginResult(pluginResult, callbackId: self.command.callbackId)
        }
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
                self.sendResult()
            }, failureBlock:{
                (myerror: NSError!) -> Void in
                print("error occurred: \(myerror.localizedDescription)")
            }

        )
    }
    
    // TODO: Add data of Location etc.
    private func setDictionary(asset: ALAsset, id: Int, option: [String: AnyObject]) -> [String: AnyObject]{
        var data: [String: AnyObject] = [:]
        data["id"] = id
        data["mediaType"] = setType(asset)
        let date: NSDate = asset.valueForProperty(ALAssetPropertyDate) as! NSDate
        data["dateAdded"] = date.timeIntervalSince1970
        data["path"] = asset.valueForProperty(ALAssetPropertyAssetURL).absoluteString
        let rep = asset.defaultRepresentation()
        data["size"] = Int(rep.size())
        data["orientation"] = rep.metadata()["Orientation"]
        data["title"] = rep.filename()
        data["height"] = rep.dimensions().height
        data["wigth"] = rep.dimensions().width
        data["mimeType"] = UTTypeCopyPreferredTagWithClass(rep.UTI(), kUTTagClassMIMEType)!.takeUnretainedValue()
        if (option["thumbnail"] as! Bool) {
            data["thumbnailPath"] = saveThumbnail(asset, id: id)
        }
        return data
    }
    
    private func saveThumbnail(asset: ALAsset, id: Int) -> NSString{
        let thumbnail = asset.thumbnail().takeUnretainedValue()
        let image = UIImage(CGImage: thumbnail)
        let imageData = UIImageJPEGRepresentation(image, 0.8)
        
        let cacheDirPath: NSString = NSSearchPathForDirectoriesInDomains(.CachesDirectory, .UserDomainMask, true)[0] as? NSString
        let filePath = cacheDirPath.stringByAppendingPathComponent("\(id).jpeg")
        
        if imageData?.writeToFile(filePath, atomically: true){
            return filePath
        } else {
            print("error occured: Cannot save thumbnail image")
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
        if mediaTypes.contains("image"){
            if mediaTypes.contains(, "video"){
                return ALAssetsFilter.allAssets()
            } else {
                return ALAssetsFilter.allPhotos()
            }
        } else if mediaTypes.contains("video"){
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