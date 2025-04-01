import UIKit

class VariableManagerViewController: UIViewController, UITableViewDelegate, UITableViewDataSource, UISearchResultsUpdating {
    
    // MARK: - Properties
    
    private var tableView: UITableView!
    private var variableNames: [String] = []
    weak var delegate: VariableSelectionDelegate?
    
    // MARK: - Lifecycle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Setup UI
        setupUI()
        
        // Set up navigation bar
        title = "Variables"
        navigationItem.rightBarButtonItem = UIBarButtonItem(
            barButtonSystemItem: .add,
            target: self,
            action: #selector(addVariable)
        )
        
        // Add close button
        navigationItem.leftBarButtonItem = UIBarButtonItem(
            barButtonSystemItem: .close,
            target: self,
            action: #selector(dismissView)
        )
        
        // Load variables
        reloadVariables()
    }
    
    // MARK: - UI Setup
    
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        // Create table view
        tableView = UITableView(frame: view.bounds, style: .insetGrouped)
        tableView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "VariableCell")
        view.addSubview(tableView)
        
        // Add search bar
        let searchController = UISearchController(searchResultsController: nil)
        searchController.searchResultsUpdater = self
        searchController.obscuresBackgroundDuringPresentation = false
        searchController.searchBar.placeholder = "Search Variables"
        navigationItem.searchController = searchController
        navigationItem.hidesSearchBarWhenScrolling = false
    }
    
    // MARK: - Actions
    
    @objc private func dismissView() {
        dismiss(animated: true)
    }
    
    @objc private func addVariable() {
        showVariableEditor(name: "", value: "", isEditing: false)
    }
    
    private func showVariableEditor(name: String, value: String, isEditing: Bool = false) {
        // Create alert controller
        let title = isEditing ? "Edit Variable" : "Add Variable"
        let alert = UIAlertController(title: title, message: nil, preferredStyle: .alert)
        
        // Add text fields
        alert.addTextField { textField in
            textField.placeholder = "Variable Name"
            textField.text = name
            textField.isEnabled = !isEditing // Can't change name when editing
            textField.autocapitalizationType = .none
            textField.autocorrectionType = .no
        }
        
        alert.addTextField { textField in
            textField.placeholder = "Variable Value"
            textField.text = value
            textField.autocapitalizationType = .none
            textField.autocorrectionType = .no
        }
        
        // Add cancel action
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        // Add save action
        let saveTitle = isEditing ? "Save" : "Add"
        alert.addAction(UIAlertAction(title: saveTitle, style: .default) { [weak self] _ in
            guard let self = self,
                  let nameField = alert.textFields?[0],
                  let valueField = alert.textFields?[1],
                  let newName = nameField.text?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !newName.isEmpty,
                  let newValue = valueField.text else {
                return
            }
            
            // Save the variable
            VariablesManager.shared.setValue(newValue, for: newName)
            
            // Reload table
            self.reloadVariables()
            
            // If this was a new variable, show quick insert option
            if !isEditing {
                self.offerQuickInsert(name: newName, value: newValue)
            }
        })
        
        // Present alert
        present(alert, animated: true)
    }
    
    private func offerQuickInsert(name: String, value: String) {
        let alert = UIAlertController(
            title: "Variable Added",
            message: "Would you like to insert this variable now?",
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Insert", style: .default) { [weak self] _ in
            self?.delegate?.didSelectVariable(name: name, value: value)
            self?.dismiss(animated: true)
        })
        
        alert.addAction(UIAlertAction(title: "Later", style: .cancel))
        
        present(alert, animated: true)
    }
    
    // MARK: - Data Management
    
    private func reloadVariables() {
        // Get all variable names
        variableNames = VariablesManager.shared.getAllVariableNames().sorted()
        
        // Reload table
        tableView.reloadData()
    }
    
    // MARK: - UITableViewDataSource
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return variableNames.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "VariableCell", for: indexPath)
        
        // Get variable name and value
        let name = variableNames[indexPath.row]
        let value = VariablesManager.shared.getValue(for: name) ?? ""
        
        // Configure cell
        var content = cell.defaultContentConfiguration()
        content.text = name
        content.secondaryText = value
        cell.contentConfiguration = content
        
        return cell
    }
    
    // MARK: - UITableViewDelegate
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        let name = variableNames[indexPath.row]
        let value = VariablesManager.shared.getValue(for: name) ?? ""
        
        let alert = UIAlertController(
            title: name,
            message: nil,
            preferredStyle: .actionSheet
        )
        
        // Add edit action
        alert.addAction(UIAlertAction(title: "Edit", style: .default) { [weak self] _ in
            self?.showVariableEditor(name: name, value: value, isEditing: true)
        })
        
        // Add insert action
        alert.addAction(UIAlertAction(title: "Insert", style: .default) { [weak self] _ in
            self?.delegate?.didSelectVariable(name: name, value: value)
            self?.dismiss(animated: true)
        })
        
        // Add copy action
        alert.addAction(UIAlertAction(title: "Copy Value", style: .default) { _ in
            UIPasteboard.general.string = value
        })
        
        // Add delete action
        alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { [weak self] _ in
            self?.deleteVariable(name: name)
        })
        
        // Add cancel action
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        // For iPad support
        if let popover = alert.popoverPresentationController {
            popover.sourceView = tableView.cellForRow(at: indexPath)
            popover.sourceRect = tableView.cellForRow(at: indexPath)?.bounds ?? .zero
        }
        
        present(alert, animated: true)
    }
    
    private func deleteVariable(name: String) {
        let alert = UIAlertController(
            title: "Delete Variable",
            message: "Are you sure you want to delete '\(name)'?",
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { [weak self] _ in
            VariablesManager.shared.removeVariable(named: name)
            self?.reloadVariables()
        })
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        present(alert, animated: true)
    }
    
    // MARK: - Search Results Updating
    
    func updateSearchResults(for searchController: UISearchController) {
        guard let searchText = searchController.searchBar.text?.lowercased() else { return }
        
        if searchText.isEmpty {
            variableNames = VariablesManager.shared.getAllVariableNames().sorted()
        } else {
            variableNames = VariablesManager.shared.getAllVariableNames().filter {
                $0.lowercased().contains(searchText) ||
                (VariablesManager.shared.getValue(for: $0)?.lowercased().contains(searchText) ?? false)
            }.sorted()
        }
        
        tableView.reloadData()
    }
}

// MARK: - Variable Selection Delegate
// Moved to Models.swift
