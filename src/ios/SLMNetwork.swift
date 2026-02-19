import Foundation
import Network

@objc(SLMNetwork) class SLMNetwork: CDVPlugin {

    private var pathMonitorRef: Any?
    private var monitorQueue: DispatchQueue?
    private var monitorCallbackId: String?

    // MARK: - getConnectionInfo

    @objc(getConnectionInfo:)
    func getConnectionInfo(command: CDVInvokedUrlCommand) {
        if #available(iOS 12.0, *) {
            let monitor = NWPathMonitor()
            let queue = DispatchQueue(label: "com.slm.network.oneshot")

            monitor.pathUpdateHandler = { path in
                monitor.cancel()

                let info = self.buildNetworkInfo(path)
                let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: info)
                self.commandDelegate.send(result, callbackId: command.callbackId)
            }

            monitor.start(queue: queue)
        } else {
            let info: [String: Any] = [
                "type": "unknown",
                "isConnected": true,
                "isExpensive": false,
                "details": [
                    "isConstrained": false,
                    "supportsDNS": true,
                    "supportsIPv4": true,
                    "supportsIPv6": true
                ]
            ]
            let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: info)
            commandDelegate.send(result, callbackId: command.callbackId)
        }
    }

    // MARK: - startMonitoring

    @objc(startMonitoring:)
    func startMonitoring(command: CDVInvokedUrlCommand) {
        if #available(iOS 12.0, *) {
            // Detener monitor anterior si existe
            if let existing = pathMonitorRef as? NWPathMonitor {
                existing.cancel()
            }

            monitorCallbackId = command.callbackId
            let monitor = NWPathMonitor()
            let queue = DispatchQueue(label: "com.slm.network.monitor")

            monitor.pathUpdateHandler = { [weak self] path in
                guard let self = self, let callbackId = self.monitorCallbackId else { return }

                let info = self.buildNetworkInfo(path)
                let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: info)
                result?.setKeepCallbackAs(true)
                self.commandDelegate.send(result, callbackId: callbackId)
            }

            monitor.start(queue: queue)
            self.pathMonitorRef = monitor
            self.monitorQueue = queue
        } else {
            let result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Network monitoring requires iOS 12.0+")
            commandDelegate.send(result, callbackId: command.callbackId)
        }
    }

    // MARK: - stopMonitoring

    @objc(stopMonitoring:)
    func stopMonitoring(command: CDVInvokedUrlCommand) {
        if #available(iOS 12.0, *) {
            if let monitor = pathMonitorRef as? NWPathMonitor {
                monitor.cancel()
            }
        }
        pathMonitorRef = nil
        monitorQueue = nil
        monitorCallbackId = nil

        let info: [String: Any] = ["stopped": true]
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: info)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    // MARK: - Helpers

    @available(iOS 12.0, *)
    private func buildNetworkInfo(_ path: NWPath) -> [String: Any] {
        var type = "none"
        var isConnected = false

        if path.status == .satisfied {
            isConnected = true
            if path.usesInterfaceType(.wifi) {
                type = "wifi"
            } else if path.usesInterfaceType(.cellular) {
                type = "cellular"
            } else if path.usesInterfaceType(.wiredEthernet) {
                type = "ethernet"
            } else {
                type = "other"
            }
        }

        let details: [String: Any] = [
            "isConstrained": path.isConstrained,
            "supportsDNS": path.supportsDNS,
            "supportsIPv4": path.supportsIPv4,
            "supportsIPv6": path.supportsIPv6
        ]

        return [
            "type": type,
            "isConnected": isConnected,
            "isExpensive": path.isExpensive,
            "details": details
        ]
    }

    override func onReset() {
        if #available(iOS 12.0, *) {
            if let monitor = pathMonitorRef as? NWPathMonitor {
                monitor.cancel()
            }
        }
        pathMonitorRef = nil
        monitorQueue = nil
        monitorCallbackId = nil
    }
}
