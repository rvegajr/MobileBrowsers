import UIKit
import WebKit

class HistoryListViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    
    // MARK: - Properties
    
    private var tableView: UITableView!
    private var historyEntries: [HistoryEntry] = []
    private var filteredEntries: [HistoryEntry] = []
    private var searchController: UISearchController!
    
    weak var delegate: HistoryListViewControllerDelegate?
    
    // MARK: - Lifecycle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Set up UI
        setupUI()
        
        // Set up navigation bar
        title = "Browsing History"
        navigationItem.rightBarButtonItem = UIBarButtonItem(
            title: "Clear All",
            style: .plain,
            target: self,
            action: #selector(clearAllHistory)
        )
        
        // Load history
        reloadHistory()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        reloadHistory()
    }
    
    // MARK: - UI Setup
    
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        // Create search controller
        searchController = UISearchController(searchResultsController: nil)
        searchController.searchResultsUpdater = self
        searchController.obscuresBackgroundDuringPresentation = false
        searchController.searchBar.placeholder = "Search History"
        navigationItem.searchController = searchController
        definesPresentationContext = true
        
        // Create table view
        tableView = UITableView(frame: view.bounds, style: .plain)
        tableView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "HistoryCell")
        view.addSubview(tableView)
        
        // Add pull to refresh
        let refreshControl = UIRefreshControl()
        refreshControl.addTarget(self, action: #selector(reloadHistory), for: .valueChanged)
        tableView.refreshControl = refreshControl
    }
    
    // MARK: - Data Loading
    
    @objc private func reloadHistory() {
        // Get all history entries
        historyEntries = HistoryManager.shared.getEntries()
        
        // Apply filter if search is active
        filterHistoryEntries()
        
        // Reload table
        tableView.reloadData()
        tableView.refreshControl?.endRefreshing()
    }
    
    private func filterHistoryEntries() {
        guard let searchText = searchController.searchBar.text, !searchText.isEmpty else {
            filteredEntries = historyEntries
            return
        }
        
        // Filter by URL or title containing search text
        filteredEntries = historyEntries.filter { entry in
            return entry.url.absoluteString.lowercased().contains(searchText.lowercased()) ||
                   entry.title.lowercased().contains(searchText.lowercased())
        }
    }
    
    // MARK: - Actions
    
    @objc private func clearAllHistory() {
        let alert = UIAlertController(
            title: "Clear History",
            message: "Are you sure you want to clear all browsing history?",
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        alert.addAction(UIAlertAction(title: "Clear All", style: .destructive) { _ in
            HistoryManager.shared.clearHistory()
            self.reloadHistory()
        })
        
        present(alert, animated: true)
    }
    
    // MARK: - UITableViewDataSource
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return filteredEntries.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "HistoryCell", for: indexPath)
        
        // Get history entry
        let entry = filteredEntries[indexPath.row]
        
        // Configure cell
        if #available(iOS 14.0, *) {
            var content = cell.defaultContentConfiguration()
            content.text = entry.title
            content.secondaryText = entry.url.absoluteString
            
            // Format date
            let dateFormatter = DateFormatter()
            dateFormatter.dateStyle = .medium
            dateFormatter.timeStyle = .short
            content.secondaryTextProperties.color = .gray
            
            // Set cell content
            cell.contentConfiguration = content
        } else {
            // Fallback for iOS 13 (should not be needed with deployment target of 14.0, but adding for safety)
            cell.textLabel?.text = entry.title
            cell.detailTextLabel?.text = entry.url.absoluteString
            cell.detailTextLabel?.textColor = .gray
        }
        
        return cell
    }
    
    // MARK: - UITableViewDelegate
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        // Get selected entry
        let entry = filteredEntries[indexPath.row]
        
        // Convert HistoryEntry to BrowsingSession
        let session = BrowsingSession(url: entry.url, title: entry.title, lastVisited: entry.timestamp)
        
        // Notify delegate
        delegate?.didSelectHistoryItem(session)
        
        tableView.deselectRow(at: indexPath, animated: true)
    }
    
    func tableView(_ tableView: UITableView, trailingSwipeActionsConfigurationForRowAt indexPath: IndexPath) -> UISwipeActionsConfiguration? {
        // Create delete action
        let deleteAction = UIContextualAction(style: .destructive, title: "Delete") { [weak self] (_, _, completionHandler) in
            guard let self = self else {
                completionHandler(false)
                return
            }
            
            // Get entry to delete
            let entry = self.filteredEntries[indexPath.row]
            
            // Delete from history manager
            HistoryManager.shared.deleteEntry(with: entry.url)
            
            // Reload data
            self.reloadHistory()
            
            completionHandler(true)
        }
        
        // Create configuration with actions
        let configuration = UISwipeActionsConfiguration(actions: [deleteAction])
        return configuration
    }
}

// MARK: - UISearchResultsUpdating

extension HistoryListViewController: UISearchResultsUpdating {
    func updateSearchResults(for searchController: UISearchController) {
        filterHistoryEntries()
        tableView.reloadData()
    }
}
