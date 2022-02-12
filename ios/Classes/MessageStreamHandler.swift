//
//  MessageStreamHandler.swift
//  augmented_images
//
//  Created by Nikolaus Schauersberger on 11.02.22.
//

import Foundation

class MessageStreamHandler : NSObject, FlutterStreamHandler{
    var sink: FlutterEventSink?
    
    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        sink = events
        return nil
    }
    
    func send(channel: String, event: String, data: Any) {
        let respose = ["channel" : channel, "event": event, "body": ["data" : data]] as [String : Any]
        sink!(respose)
    }
    
    
    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        sink = nil
        return nil
    }
}
