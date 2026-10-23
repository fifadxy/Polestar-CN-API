import AppKit
import Foundation
import WebKit

@MainActor
final class ChinaPolestarChallengeWindow: NSObject, WKScriptMessageHandler, WKNavigationDelegate, NSWindowDelegate {
    static let shared = ChinaPolestarChallengeWindow()
    static let challengeURL = URL(
        string: "https://account.app.polestar-auto.com/account-components/auth-slider/index.html"
    )!

    private var window: NSWindow?
    private var webView: WKWebView?
    private var completion: ((ChinaPolestarChallengePayload?) -> Void)?

    func open(completion: @escaping (ChinaPolestarChallengePayload?) -> Void) {
        finish(nil, closeWindow: false)
        self.completion = completion

        let controller = WKUserContentController()
        controller.add(self, name: "chromium")
        controller.addUserScript(WKUserScript(
            source: """
            window.chromium = window.chromium || {};
            window.chromium.postMessage = function(message) {
                window.webkit.messageHandlers.chromium.postMessage(String(message));
            };
            """,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: false
        ))

        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .nonPersistent()
        configuration.userContentController = controller

        let web = WKWebView(
            frame: NSRect(x: 0, y: 0, width: 520, height: 620),
            configuration: configuration
        )
        web.autoresizingMask = [.width, .height]
        web.navigationDelegate = self

        let win = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 520, height: 620),
            styleMask: [.titled, .closable],
            backing: .buffered,
            defer: false
        )
        win.title = "Polestar China · 安全验证"
        win.isReleasedWhenClosed = false
        win.delegate = self
        win.contentView = web
        window = win
        webView = web
        win.center()
        win.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
        web.load(URLRequest(url: Self.challengeURL))
    }

    func userContentController(_ userContentController: WKUserContentController,
                               didReceive message: WKScriptMessage) {
        guard message.name == "chromium",
              let result = Self.challengePayload(from: message.body)
        else { return }
        finish(result, closeWindow: true)
    }

    private static func challengePayload(from body: Any) -> ChinaPolestarChallengePayload? {
        if let dictionary = body as? [String: Any],
           let result = challengePayload(from: dictionary) {
            return result
        }
        guard let raw = body as? String else { return nil }

        var candidates = [raw, raw.removingPercentEncoding ?? raw]
        if let components = URLComponents(string: raw),
           let value = components.queryItems?.first(where: { $0.name == "data" })?.value {
            candidates.append(value)
            candidates.append(value.removingPercentEncoding ?? value)
        }

        for candidate in candidates {
            var text = candidate
            if let range = text.range(of: "data=") {
                text = String(text[range.upperBound...].split(separator: "&", maxSplits: 1)[0])
                text = text.removingPercentEncoding ?? text
            }
            guard let data = text.data(using: .utf8),
                  let object = try? JSONSerialization.jsonObject(with: data)
            else { continue }
            if let dictionary = object as? [String: Any],
               let result = challengePayload(from: dictionary) {
                return result
            }
        }
        return nil
    }

    private static func challengePayload(
        from dictionary: [String: Any]
    ) -> ChinaPolestarChallengePayload? {
        if let nested = dictionary["data"] as? [String: Any],
           let result = challengePayload(from: nested) {
            return result
        }
        if let nestedString = dictionary["data"] as? String,
           let data = nestedString.data(using: .utf8),
           let object = try? JSONSerialization.jsonObject(with: data),
           let nested = object as? [String: Any],
           let result = challengePayload(from: nested) {
            return result
        }
        guard let sessionID = dictionary["sessionId"] as? String, !sessionID.isEmpty,
              let signature = dictionary["sig"] as? String, !signature.isEmpty,
              let token = dictionary["token"] as? String, !token.isEmpty
        else { return nil }
        return ChinaPolestarChallengePayload(
            sessionID: sessionID,
            signature: signature,
            token: token
        )
    }

    private func finish(
        _ result: ChinaPolestarChallengePayload?,
        closeWindow: Bool
    ) {
        let callback = completion
        completion = nil
        if let webView {
            webView.stopLoading()
            webView.configuration.userContentController.removeScriptMessageHandler(forName: "chromium")
            webView.navigationDelegate = nil
        }
        webView = nil
        if closeWindow {
            window?.delegate = nil
            window?.close()
        }
        window = nil
        callback?(result)
    }

    func windowWillClose(_ notification: Notification) {
        finish(nil, closeWindow: false)
    }
}
