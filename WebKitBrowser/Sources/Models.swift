import Foundation
import WebKit

// MARK: - BrowsingSession
public struct BrowsingSession {
    var url: URL
    var title: String
    var lastVisited: Date
    
    init(url: URL, title: String, lastVisited: Date = Date()) {
        self.url = url
        self.title = title
        self.lastVisited = lastVisited
    }
}

// MARK: - Protocol Definitions
public protocol HistoryListViewControllerDelegate: AnyObject {
    func didSelectHistoryItem(_ session: BrowsingSession)
}

public protocol VariableSelectionDelegate: AnyObject {
    func didSelectVariable(name: String, value: String)
}
