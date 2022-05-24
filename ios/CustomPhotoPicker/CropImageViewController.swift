//
//  ViewController.swift
//  DemoPod
//
//  Created by dinh trung on 6/15/21.
//

import UIKit
import WeScan

public protocol CropImageViewControllerDelegate: NSObjectProtocol {
    
    /// Tells the delegate that the user scanned a document.
    ///
    /// - Parameters:
    ///   - scanner: The scanner controller object managing the scanning interface.
    ///   - results: The results of the user scanning with the camera.
    /// - Discussion: Your delegate's implementation of this method should dismiss the image scanner controller.
    func imageCropScannerController(_ path: ImageScannerResults)
    
    func imageCropScannerControllerCancel(_ path: String)
    
}

class CropImageViewController: UIViewController, ImageScannerControllerDelegate {
    
    private var submitButton: UIButton?
    public var uri: String?
    public var isFakeData: Bool?
    public var points: NSArray?
    public var resizeWidth: CGFloat?
    public var resizeHeight: CGFloat?
    public weak var cropImageViewDelegate: CropImageViewControllerDelegate?
    
    func imageScannerController(_ scanner: ImageScannerController, didFinishScanningWithResults results: ImageScannerResults) {
       // scanner.dismiss(animated: true);
        cropImageViewDelegate?.imageCropScannerController(results)
        scanner.dismiss(animated: true)
    }
    
    func imageScannerControllerDidCancel(_ scanner: ImageScannerController) {
        cropImageViewDelegate?.imageCropScannerControllerCancel("cancel")
        scanner.dismiss(animated: true);
    }
    
    func imageScannerController(_ scanner: ImageScannerController, didFailWithError error: Error) {
        print(error);
    }
    
    override func viewDidAppear(_ animated: Bool) {
        self.dismiss(animated: false);
        self.crop()
    }
    
    func checkDevice() -> Bool{
        switch UIScreen.main.nativeBounds.height {
        case 1136:
//            print("iPhone 5 or 5S or 5C")
            return false
        case 1334:
//            print("iPhone 6/6S/7/8")
            return false
        case 1920, 2208:
//            print("iPhone 6+/6S+/7+/8+")
            return false
        case 2436:
//            print("iPhone X/XS/11 Pro")
            return false
        case 2688:
//            print("iPhone XS Max/11 Pro Max")
            return false
        case 1792:
//            print("iPhone XR/ 11 ")
            return false
        case 2532,2340,2778:
//            print("iPhone 12/ 12Promax / 12Mini ")
            return false
        default:
            return true
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    func crop(){
        let imageUrl = NSURL(string: self.uri!)
        let image = RCTImageFromLocalAssetURL(imageUrl as! URL);
        let scannerViewController = ImageScannerController(image: image, delegate: nil, points: self.points, resizeWidth: self.resizeWidth, resizeHeight: self.resizeHeight)
        scannerViewController.imageScannerDelegate = self
        scannerViewController.modalTransitionStyle = .coverVertical
        if checkDevice() {
            scannerViewController.modalPresentationStyle = .overFullScreen
        }
        present(scannerViewController, animated: true)
    }
    
}


