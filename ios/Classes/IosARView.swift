import Flutter
import UIKit
import Foundation
import ARKit
import Combine

class IosARView: NSObject, FlutterPlatformView, ARSCNViewDelegate, UIGestureRecognizerDelegate, ARSessionDelegate {
    //private var _view: UIView
    
    let sceneView: ARSCNView
    
    let params: Dictionary<String, Array<String>>
    let registrar: FlutterPluginRegistrar
    
    /// A serial queue for thread safety when modifying the SceneKit node graph.
    let updateQueue = DispatchQueue(label: Bundle.main.bundleIdentifier! +
        ".serialSceneKitQueue")
    
    /// Convenience accessor for the session owned by ARSCNView.
    var session: ARSession {
        return sceneView.session
    }

    init(
        frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?,
        registrar: FlutterPluginRegistrar
    ) {
        //_view = UIView()
        self.registrar = registrar
        self.sceneView = ARSCNView(frame: frame)
        self.params = args as! Dictionary<String, Array<String>>
        
        super.init()
        // iOS views can be created here
        //createNativeView(view: _view)
        
        self.sceneView.delegate = self
        self.sceneView.session.delegate = self
        
    }

    func view() -> UIView {
        resetTracking()
        //return _view
        return self.sceneView
    }
    
    /// Creates a new AR configuration to run on the `session`.
    /// - Tag: ARReferenceImage-Loading
    func resetTracking() {
        
        var imageSet: Set<ARReferenceImage> = []
        
        let fileUrls = params["triggerImagePaths"]
        
        for fileUrl in fileUrls ?? [""] {
            let asset = registrar.lookupKey(forAsset: fileUrl)
            guard let path = Bundle.main.path(forResource: asset, ofType: nil) else { return }
            let imageUrl = URL(fileURLWithPath: path)
            
            guard let image = CIImage(contentsOf: imageUrl) else { return }
            guard let ciImage = convertCIImageToCGImage(inputImage: image) else { return }
            
            let refimage = ARReferenceImage(ciImage, orientation: CGImagePropertyOrientation.up, physicalWidth: 0.2)
            imageSet.insert(refimage)
        }
        
        
        /*guard let referenceImages = ARReferenceImage.referenceImages(inGroupNamed: "AR Resources", bundle: nil) else {
            fatalError("Missing expected asset catalog resources.")
        }*/
        
        let referenceImages = imageSet
        
        let configuration = ARWorldTrackingConfiguration()
        configuration.detectionImages = referenceImages
        session.run(configuration, options: [.resetTracking, .removeExistingAnchors])
    }
    
    func convertCIImageToCGImage(inputImage: CIImage) -> CGImage! {
        let context = CIContext(options: nil)
        return context.createCGImage(inputImage, from: inputImage.extent)
    }

    func createNativeView(view _view: UIView){
        /*_view.backgroundColor = UIColor.blue
        let nativeLabel = UILabel()
        nativeLabel.text = "Native texto from evil iOS"
        nativeLabel.textColor = UIColor.white
        nativeLabel.textAlignment = .center
        nativeLabel.frame = CGRect(x: 0, y: 0, width: 180, height: 48.0)
        _view.addSubview(nativeLabel)*/
    }
    
}
