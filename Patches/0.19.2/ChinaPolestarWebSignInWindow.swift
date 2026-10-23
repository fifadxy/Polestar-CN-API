import AppKit
import Foundation
import WebKit

// Official China WEBSITE sign-in only. No verified website-to-C3 token grant exists.
// Do not inspect passwords, cookies, verification codes, page JS or tokens.
@MainActor
final class ChinaPolestarWebSignInWindow: NSObject, WKUIDelegate, WKNavigationDelegate, NSWindowDelegate {
    static let shared = ChinaPolestarWebSignInWindow()
    static let officialLoginURL = URL(string: "https://www.polestar.cn/zh-cn/auth/login/")!

    private var window: NSWindow?
    private var webView: WKWebView?

    func open() {
        if let window {
            window.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
            return
        }
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

    func webView(_ webView: WKWebView,
                 createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction,
                 windowFeatures: WKWindowFeatures) -> WKWebView? {
        guard webView === self.webView, navigationAction.targetFrame == nil,
              let url = navigationAction.request.url else { return nil }
        if url.scheme?.lowercased() == "https" {
            webView.load(navigationAction.request)
        } else if url.scheme?.lowercased() != "javascript" {
            NSWorkspace.shared.open(url)
        }
        return nil
    }

    func webView(_ webView: WKWebView,
                 didFailProvisionalNavigation navigation: WKNavigation!,
                 withError error: Error) {
        guard (error as NSError).code != NSURLErrorCancelled, let window else { return }
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
