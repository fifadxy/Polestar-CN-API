import AppKit
import Combine
import Foundation
import WebKit

// Official China WEBSITE sign-in only. No verified website-to-C3 token grant exists.
// Do not inspect passwords, cookies, verification codes, page JS or tokens.
@MainActor
final class ChinaPolestarWebSignInWindow: NSObject, ObservableObject, WKUIDelegate, WKNavigationDelegate, NSWindowDelegate {
    static let shared = ChinaPolestarWebSignInWindow()
    static let officialLoginURL = URL(string: "https://polestarid.polestar.cn")!

    private var window: NSWindow?
    private var webView: WKWebView?

    @Published private(set) var lastStep = "尚未打开网页"
    @Published private(set) var lastSafeHost = "—"
    @Published private(set) var lastHTTPStatus: Int?

    /// No URL path, query string, callback code, cookies, tokens, or device identifiers.
    var redactedReport: String {
        [
            "patch=0.19.5",
            "web_source=polestar_id_china_diagnostic",
            "web_state=" + lastStep,
            "last_host=" + lastSafeHost,
            "last_http_status=" + (lastHTTPStatus.map { String($0) } ?? "unknown"),
            "web_to_vehicle_grant=ADVANCED_DIAGNOSTIC_ONLY",
            "vehicle_api_auth=NOT_VERIFIED",
            "legacy_sms_api=DISABLED"
        ].joined(separator: "\n")
    }

    private func updateSafeHost(_ url: URL?) {
        guard let host = url?.host?.lowercased() else { return }
        let allowed = ["polestar.cn", "polestar-auto.com", "polestar.com", "volvocars.com.cn"]
        lastSafeHost = allowed.contains {
            host == $0 || host.hasSuffix("." + $0)
        } ? host : "other-https-host"
    }

    func open() {
        if let window {
            window.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
            return
        }
        lastStep = "正在打开中国区官网"
        lastHTTPStatus = nil
        lastSafeHost = "www.polestar.cn"
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .default()
        let web = WKWebView(frame: NSRect(x: 0, y: 0, width: 760, height: 820), configuration: config)
        web.autoresizingMask = [.width, .height]
        web.navigationDelegate = self
        web.uiDelegate = self
        let win = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 760, height: 820),
            styleMask: [.titled, .closable, .resizable, .miniaturizable],
            backing: .buffered,
            defer: false
        )
        win.title = "Polestar China · Official Website Login"
        win.isReleasedWhenClosed = false
        win.delegate = self
        win.contentView = web
        window = win
        webView = web
        win.center()
        win.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
        web.load(URLRequest(url: Self.officialLoginURL))
    }

    // Response metadata only: never retain the URL that could contain an OAuth code.
    func webView(_ webView: WKWebView,
                 decidePolicyFor navigationResponse: WKNavigationResponse,
                 decisionHandler: @escaping @MainActor @Sendable (WKNavigationResponsePolicy) -> Void) {
        guard webView === self.webView else {
            decisionHandler(.cancel)
            return
        }
        updateSafeHost(navigationResponse.response.url)
        if let response = navigationResponse.response as? HTTPURLResponse {
            lastHTTPStatus = response.statusCode
        }
        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        guard webView === self.webView else { return }
        updateSafeHost(webView.url)
        lastStep = "网页已加载；是否登录成功请在官方页面确认"
    }

    func webView(_ webView: WKWebView,
                 createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction,
                 windowFeatures: WKWindowFeatures) -> WKWebView? {
        guard webView === self.webView, navigationAction.targetFrame == nil,
              let url = navigationAction.request.url else { return nil }
        if url.scheme?.lowercased() == "https" {
            webView.load(navigationAction.request)
        } else if ["mailto", "tel"].contains(url.scheme?.lowercased() ?? "") {
            NSWorkspace.shared.open(url)
        }
        return nil
    }

    func webView(_ webView: WKWebView,
                 didFailProvisionalNavigation navigation: WKNavigation!,
                 withError error: Error) {
        guard (error as NSError).code != NSURLErrorCancelled, let window else { return }
        lastStep = "网页加载失败；可使用默认浏览器"
        let alert = NSAlert()
        alert.alertStyle = .warning
        alert.messageText = "中国区登录网页无法加载"
        alert.informativeText = "检查网络，或改用默认浏览器打开极星中国官网。"
        alert.addButton(withTitle: "在浏览器打开")
        alert.addButton(withTitle: "关闭")
        alert.beginSheetModal(for: window) { response in
            if response == .alertFirstButtonReturn {
                NSWorkspace.shared.open(Self.officialLoginURL)
            }
        }
    }

    func windowWillClose(_ notification: Notification) {
        guard let closing = notification.object as? NSWindow, closing === window else { return }
        webView?.stopLoading()
        webView?.navigationDelegate = nil
        webView?.uiDelegate = nil
        webView = nil
        window = nil
    }
}
