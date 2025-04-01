import Foundation

/// Model representing a history entry
struct HistoryEntry: Codable, Equatable, Identifiable {
    let id: UUID
    let url: URL
    let title: String
    let timestamp: Date
    let iconData: Data?
    
    init(url: URL, title: String, timestamp: Date = Date(), iconData: Data? = nil) {
        self.id = UUID()
        self.url = url
        self.title = title
        self.timestamp = timestamp
        self.iconData = iconData
    }
    
    static func == (lhs: HistoryEntry, rhs: HistoryEntry) -> Bool {
        return lhs.url.absoluteString == rhs.url.absoluteString
    }
}

/// Manager class for browser history
class HistoryManager {
    
    // MARK: - Properties
    
    static let shared = HistoryManager()
    
    private let historyFileURL: URL
    private(set) var historyEntries: [HistoryEntry] = []
    
    // Maximum number of history entries to keep
    private let maxHistoryEntries = 1000
    
    // MARK: - Initialization
    
    private init() {
        // Get Documents directory
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        
        // Create a URL for the history file
        historyFileURL = documentsDirectory.appendingPathComponent("browser_history.json")
        
        // Load existing history data
        loadHistory()
    }
    
    // MARK: - History Management
    
    /// Add a new entry to the history
    func addEntry(url: URL, title: String, iconData: Data? = nil) {
        // Create a new history entry
        let newEntry = HistoryEntry(url: url, title: title, iconData: iconData)
        
        // Remove any existing entries with the same URL
        historyEntries.removeAll { $0.url.absoluteString == url.absoluteString }
        
        // Add the new entry at the beginning
        historyEntries.insert(newEntry, at: 0)
        
        // Trim the history if needed
        if historyEntries.count > maxHistoryEntries {
            historyEntries = Array(historyEntries.prefix(maxHistoryEntries))
        }
        
        // Save the updated history
        saveHistory()
    }
    
    /// Get all history entries
    func getEntries() -> [HistoryEntry] {
        return historyEntries
    }
    
    /// Get history entries filtered by date
    func getEntries(since: Date? = nil, limit: Int? = nil) -> [HistoryEntry] {
        var filteredEntries = historyEntries
        
        // Apply date filter if provided
        if let since = since {
            filteredEntries = filteredEntries.filter { $0.timestamp >= since }
        }
        
        // Apply limit if provided
        if let limit = limit, limit < filteredEntries.count {
            filteredEntries = Array(filteredEntries.prefix(limit))
        }
        
        return filteredEntries
    }
    
    /// Clear all history entries
    func clearHistory() {
        historyEntries.removeAll()
        saveHistory()
    }
    
    /// Delete a specific entry
    func deleteEntry(with url: URL) {
        historyEntries.removeAll { $0.url.absoluteString == url.absoluteString }
        saveHistory()
    }
    
    // MARK: - Persistence
    
    /// Load history from disk
    private func loadHistory() {
        do {
            // Check if file exists
            if FileManager.default.fileExists(atPath: historyFileURL.path) {
                let data = try Data(contentsOf: historyFileURL)
                historyEntries = try JSONDecoder().decode([HistoryEntry].self, from: data)
                print("Loaded \(historyEntries.count) history entries")
            } else {
                print("No history file found, starting with empty history")
            }
        } catch {
            print("Error loading history: \(error.localizedDescription)")
            // If loading fails, start with an empty history
            historyEntries = []
        }
    }
    
    /// Save history to disk
    private func saveHistory() {
        do {
            let data = try JSONEncoder().encode(historyEntries)
            try data.write(to: historyFileURL)
            print("Saved \(historyEntries.count) history entries")
        } catch {
            print("Error saving history: \(error.localizedDescription)")
        }
    }
}
