import UIKit
import WebKit

class BrowserViewController: UIViewController {
    // MARK: - Properties
    var webView: WKWebView!
    var addressBar: UITextField!
    var toolbarView: UIView!
    var backButton: UIButton!
    var forwardButton: UIButton!
    var reloadButton: UIButton!
    var credentialsButton: UIButton!
    var variablesButton: UIButton!
    var historyButton: UIButton!
    var favoritesButton: UIButton!
    var devToolsButton: UIButton!
    var menuButton: UIButton!
    var activityIndicator: UIActivityIndicatorView!
    var currentSession: BrowsingSession!
    var credentialsManager = CredentialsManager.shared
    var favoriteURLs = [
        "https://alliedpilots.org",
        "https://integ.alliedpilots.org",
        "https://expense.integ.alliedpilots.org"
    ]
    
    // Dev tools properties
    var isDevToolsVisible = false
    var devToolsView: UIView?
    
    // Current zoom factor (1.0 = 100%)
    var currentZoomFactor: Double = 1.0
    var minZoomFactor: Double = 0.5
    var maxZoomFactor: Double = 3.0
    var zoomIncrement: Double = 0.25
    
    // History tracking
    var browsingSessions: [BrowsingSession] = []
    
    // MARK: - Lifecycle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        print("BrowserViewController viewDidLoad - Starting setup")
        
        // Set extremely bright background color for immediate visibility
        view.backgroundColor = UIColor.systemPink
        
        // Add a visibility test label
        let visibilityLabel = UILabel()
        visibilityLabel.translatesAutoresizingMaskIntoConstraints = false
        visibilityLabel.text = "WebKit Browser is Running"
        visibilityLabel.font = UIFont.boldSystemFont(ofSize: 24)
        visibilityLabel.textColor = .white
        visibilityLabel.textAlignment = .center
        visibilityLabel.backgroundColor = .black
        visibilityLabel.layer.cornerRadius = 10
        visibilityLabel.layer.masksToBounds = true
        view.addSubview(visibilityLabel)
        
        NSLayoutConstraint.activate([
            visibilityLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            visibilityLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            visibilityLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            visibilityLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            visibilityLabel.heightAnchor.constraint(equalToConstant: 50)
        ])
        
        setupUIComponents()
        setupConstraints()
        
        // Restore previous session if available, otherwise load a default URL
        loadSavedSession()
        
        print("BrowserViewController viewDidLoad - Setup complete")
        
        if #available(iOS 13.0, *) {
            // Do nothing, context menu is handled by WKUIDelegate
        } else {
            setupLegacyContextMenu()
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        print("BrowserViewController viewWillAppear")
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        print("BrowserViewController viewDidAppear - View frame: \(view.frame)")
        print("AddressBar frame: \(addressBar.frame)")
        print("ToolbarView frame: \(toolbarView.frame)")
        print("WebView frame: \(webView.frame)")
        
        // Set up application lifecycle observers to save state when app enters background
        NotificationCenter.default.addObserver(self, 
                                              selector: #selector(saveCurrentSession), 
                                              name: UIApplication.willResignActiveNotification, 
                                              object: nil)
        
        NotificationCenter.default.addObserver(self,
                                              selector: #selector(saveCurrentSession),
                                              name: UIApplication.didEnterBackgroundNotification,
                                              object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    // MARK: - UI Setup
    
    private func setupUIComponents() {
        print("BrowserViewController: Setting up UI components")
        
        // Create WebView with configuration
        let webConfiguration = WKWebViewConfiguration()
        let preferences = WKPreferences()
        preferences.javaScriptEnabled = true
        webConfiguration.preferences = preferences
        
        webView = WKWebView(frame: .zero, configuration: webConfiguration)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.allowsBackForwardNavigationGestures = true
        webView.allowsLinkPreview = true
        
        if #available(iOS 16.4, *) {
            webView.isInspectable = true
        }
        
        view.addSubview(webView)
        print("BrowserViewController: WebView created and added to view")
        
        // Create address bar with a bright background for visibility
        addressBar = UITextField()
        addressBar.translatesAutoresizingMaskIntoConstraints = false
        addressBar.backgroundColor = .systemBackground
        addressBar.layer.cornerRadius = 8
        addressBar.layer.borderWidth = 1
        addressBar.layer.borderColor = UIColor.systemGray4.cgColor
        addressBar.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 8, height: 20))
        addressBar.leftViewMode = .always
        addressBar.rightView = UIView(frame: CGRect(x: 0, y: 0, width: 8, height: 20))
        addressBar.rightViewMode = .always
        addressBar.delegate = self
        addressBar.returnKeyType = .go
        addressBar.clearButtonMode = .whileEditing
        addressBar.autocapitalizationType = .none
        addressBar.autocorrectionType = .no
        addressBar.spellCheckingType = .no
        addressBar.keyboardType = .URL
        view.addSubview(addressBar)
        print("BrowserViewController: Address bar created and added to view")
        
        // Create activity indicator
        activityIndicator = UIActivityIndicatorView(style: .medium)
        activityIndicator.hidesWhenStopped = true
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(activityIndicator)
        
        // Create toolbar with semi-transparent background
        toolbarView = UIView()
        toolbarView.translatesAutoresizingMaskIntoConstraints = false
        toolbarView.backgroundColor = .systemBackground.withAlphaComponent(0.95)
        view.addSubview(toolbarView)
        print("BrowserViewController: Toolbar created and added to view")
        
        setupToolbar()
        
        print("BrowserViewController: All toolbar buttons created and added")
    }
    
    private func createToolbarButton(systemName: String, size: CGSize, config: UIImage.SymbolConfiguration) -> UIButton {
        let button = UIButton(type: .system)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setImage(UIImage(systemName: systemName, withConfiguration: config), for: .normal)
        button.tintColor = .systemBlue
        button.widthAnchor.constraint(equalToConstant: size.width).isActive = true
        button.heightAnchor.constraint(equalToConstant: size.height).isActive = true
        return button
    }
    
    private func setupToolbar() {
        print("BrowserViewController: Creating toolbar buttons")
        
        // Button size and configuration
        let buttonSize = CGSize(width: 44, height: 44)
        let buttonConfig = UIImage.SymbolConfiguration(pointSize: 22, weight: .regular)
        
        // Create back button
        backButton = createToolbarButton(systemName: "chevron.left", size: buttonSize, config: buttonConfig)
        backButton.addTarget(self, action: #selector(goBack), for: .touchUpInside)
        toolbarView.addSubview(backButton)
        
        // Create forward button
        forwardButton = createToolbarButton(systemName: "chevron.right", size: buttonSize, config: buttonConfig)
        forwardButton.addTarget(self, action: #selector(goForward), for: .touchUpInside)
        toolbarView.addSubview(forwardButton)
        
        // Create reload button
        reloadButton = createToolbarButton(systemName: "arrow.clockwise", size: buttonSize, config: buttonConfig)
        reloadButton.addTarget(self, action: #selector(refresh), for: .touchUpInside)
        toolbarView.addSubview(reloadButton)
        
        // Dev tools button
        devToolsButton = createToolbarButton(systemName: "curlybraces", size: buttonSize, config: buttonConfig)
        devToolsButton.addTarget(self, action: #selector(toggleDevTools), for: .touchUpInside)
        toolbarView.addSubview(devToolsButton)
        
        // Favorites button
        favoritesButton = createToolbarButton(systemName: "star", size: buttonSize, config: buttonConfig)
        favoritesButton.addTarget(self, action: #selector(showFavorites), for: .touchUpInside)
        toolbarView.addSubview(favoritesButton)
        
        // History button
        historyButton = createToolbarButton(systemName: "clock", size: buttonSize, config: buttonConfig)
        historyButton.addTarget(self, action: #selector(showHistory), for: .touchUpInside)
        toolbarView.addSubview(historyButton)
        
        // Variables button
        variablesButton = createToolbarButton(systemName: "list.bullet", size: buttonSize, config: buttonConfig)
        variablesButton.addTarget(self, action: #selector(showVariables), for: .touchUpInside)
        toolbarView.addSubview(variablesButton)
        
        // Credentials button
        credentialsButton = createToolbarButton(systemName: "key", size: buttonSize, config: buttonConfig)
        credentialsButton.addTarget(self, action: #selector(toggleCredentials), for: .touchUpInside)
        toolbarView.addSubview(credentialsButton)
        
        // Menu button (ellipsis) - NEW
        menuButton = createToolbarButton(systemName: "ellipsis", size: buttonSize, config: buttonConfig)
        menuButton.addTarget(self, action: #selector(showMenu), for: .touchUpInside)
        toolbarView.addSubview(menuButton)
        
        // Setup button constraints
        let spacing: CGFloat = 10
        NSLayoutConstraint.activate([
            // Back button at the left edge
            backButton.leadingAnchor.constraint(equalTo: toolbarView.leadingAnchor, constant: spacing),
            backButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
            
            // Forward button next to back
            forwardButton.leadingAnchor.constraint(equalTo: backButton.trailingAnchor, constant: spacing),
            forwardButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
            
            // Reload button next to forward
            reloadButton.leadingAnchor.constraint(equalTo: forwardButton.trailingAnchor, constant: spacing),
            reloadButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
            
            // Dev tools button next to reload
            devToolsButton.leadingAnchor.constraint(equalTo: reloadButton.trailingAnchor, constant: spacing),
            devToolsButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
            
            // Favorites button next to dev tools
            favoritesButton.leadingAnchor.constraint(equalTo: devToolsButton.trailingAnchor, constant: spacing),
            favoritesButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
            
            // History button next to favorites
            historyButton.leadingAnchor.constraint(equalTo: favoritesButton.trailingAnchor, constant: spacing),
            historyButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
            
            // Variables button next to history
            variablesButton.leadingAnchor.constraint(equalTo: historyButton.trailingAnchor, constant: spacing),
            variablesButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
            
            // Credentials button next to variables
            credentialsButton.leadingAnchor.constraint(equalTo: variablesButton.trailingAnchor, constant: spacing),
            credentialsButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
            
            // Menu button at the right edge
            menuButton.leadingAnchor.constraint(equalTo: credentialsButton.trailingAnchor, constant: spacing),
            menuButton.trailingAnchor.constraint(equalTo: toolbarView.trailingAnchor, constant: -spacing),
            menuButton.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
        ])
        
        // Disable navigation buttons initially (will be updated when web view loads)
        backButton.isEnabled = false
        forwardButton.isEnabled = false
    }
    
    private func setupConstraints() {
        print("BrowserViewController: Setting up constraints")
        
        let toolbarHeight: CGFloat = 44
        let spacing: CGFloat = 8
        
        NSLayoutConstraint.activate([
            // Address bar constraints
            addressBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: spacing),
            addressBar.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: spacing),
            addressBar.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -spacing),
            addressBar.heightAnchor.constraint(equalToConstant: 36),
            
            // Activity indicator constraints
            activityIndicator.centerYAnchor.constraint(equalTo: addressBar.centerYAnchor),
            activityIndicator.trailingAnchor.constraint(equalTo: addressBar.trailingAnchor, constant: -8),
            
            // Toolbar constraints
            toolbarView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            toolbarView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            toolbarView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            toolbarView.heightAnchor.constraint(equalToConstant: toolbarHeight),
            
            // WebView constraints
            webView.topAnchor.constraint(equalTo: addressBar.bottomAnchor, constant: spacing),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: toolbarView.topAnchor),
        ])
        
        print("BrowserViewController: Constraints setup completed")
    }
    
    // MARK: - Actions
    
    @objc private func showFavorites() {
        let alert = UIAlertController(title: "Favorites", message: nil, preferredStyle: .actionSheet)
        
        for url in favoriteURLs {
            alert.addAction(UIAlertAction(title: url, style: .default) { [weak self] _ in
                if let url = URL(string: url) {
                    self?.loadURL(url)
                }
            })
        }
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        // For iPad support
        if let popover = alert.popoverPresentationController {
            popover.sourceView = favoritesButton
            popover.sourceRect = favoritesButton.bounds
        }
        
        present(alert, animated: true)
    }
    
    // MARK: - Navigation Actions
    
    @objc func goBack() {
        if webView.canGoBack {
            webView.goBack()
        }
    }
    
    @objc func goForward() {
        if webView.canGoForward {
            webView.goForward()
        }
    }
    
    @objc func refresh() {
        webView.reload()
    }
    
    @objc func toggleCredentials() {
        let alertController = UIAlertController(title: "Credentials", message: nil, preferredStyle: .actionSheet)
        
        let viewAction = UIAlertAction(title: "View Saved Credentials", style: .default) { [weak self] _ in
            self?.viewCredentials()
        }
        
        let saveAction = UIAlertAction(title: "Save Credentials for this site", style: .default) { [weak self] _ in
            self?.saveCredentials()
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel)
        
        alertController.addAction(viewAction)
        alertController.addAction(saveAction)
        alertController.addAction(cancelAction)
        
        // For iPad
        if let popoverController = alertController.popoverPresentationController {
            popoverController.sourceView = credentialsButton
            popoverController.sourceRect = credentialsButton.bounds
        }
        
        present(alertController, animated: true)
    }
    
    // MARK: - Helper Methods
    
    func loadURL(_ url: URL) {
        let request = URLRequest(url: url)
        print("Loading URL: \(url.absoluteString)")
        webView.load(request)
        addressBar.text = url.absoluteString
        
        // Start the loading indicator
        activityIndicator.startAnimating()
    }
    
    func updateNavigationButtons(canGoBack: Bool, canGoForward: Bool) {
        backButton.isEnabled = canGoBack
        forwardButton.isEnabled = canGoForward
    }
    
    // MARK: - Development Tools
    
    @objc func toggleDevTools() {
        isDevToolsVisible.toggle()
        
        if isDevToolsVisible {
            // Create dev tools view
            devToolsView = UIView()
            if let devToolsView = devToolsView {
                devToolsView.backgroundColor = UIColor.systemGray6
                devToolsView.translatesAutoresizingMaskIntoConstraints = false
                view.addSubview(devToolsView)
                
                // Add a text view to show HTML source with selection enabled
                let textView = UITextView()
                textView.translatesAutoresizingMaskIntoConstraints = false
                textView.font = UIFont.monospacedSystemFont(ofSize: 12, weight: .regular)
                textView.isEditable = false
                textView.isSelectable = true
                textView.backgroundColor = UIColor.systemBackground
                textView.autocorrectionType = .no
                textView.autocapitalizationType = .none
                textView.spellCheckingType = .no
                textView.keyboardType = .default
                textView.returnKeyType = .done
                textView.tag = 100 // Tag for referencing later
                devToolsView.addSubview(textView)
                
                // Add a toolbar with buttons
                let toolbar = UIToolbar()
                toolbar.translatesAutoresizingMaskIntoConstraints = false
                toolbar.barTintColor = UIColor.systemGray5
                devToolsView.addSubview(toolbar)
                
                // Create toolbar buttons
                let closeButton = UIBarButtonItem(barButtonSystemItem: .close, target: self, action: #selector(toggleDevTools))
                let flexSpace = UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil)
                let selectAllButton = UIBarButtonItem(title: "Select All", style: .plain, target: self, action: #selector(selectAllSourceCode))
                let refreshButton = UIBarButtonItem(barButtonSystemItem: .refresh, target: self, action: #selector(refreshDevTools))
                toolbar.items = [closeButton, flexSpace, selectAllButton, refreshButton]
                
                // Configure layout
                NSLayoutConstraint.activate([
                    devToolsView.leftAnchor.constraint(equalTo: view.leftAnchor),
                    devToolsView.rightAnchor.constraint(equalTo: view.rightAnchor),
                    devToolsView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
                    devToolsView.heightAnchor.constraint(equalToConstant: 300), // Taller for better view
                    
                    toolbar.topAnchor.constraint(equalTo: devToolsView.topAnchor),
                    toolbar.leadingAnchor.constraint(equalTo: devToolsView.leadingAnchor),
                    toolbar.trailingAnchor.constraint(equalTo: devToolsView.trailingAnchor),
                    toolbar.heightAnchor.constraint(equalToConstant: 44),
                    
                    textView.topAnchor.constraint(equalTo: toolbar.bottomAnchor),
                    textView.leadingAnchor.constraint(equalTo: devToolsView.leadingAnchor, constant: 8),
                    textView.trailingAnchor.constraint(equalTo: devToolsView.trailingAnchor, constant: -8),
                    textView.bottomAnchor.constraint(equalTo: devToolsView.bottomAnchor, constant: -8)
                ])
                
                // Get HTML content from the web view
                refreshDevTools()
            }
        } else {
            // Hide dev tools
            devToolsView?.removeFromSuperview()
            devToolsView = nil
        }
    }
    
    @objc func selectAllSourceCode() {
        if let devToolsView = devToolsView, 
           let textView = devToolsView.viewWithTag(100) as? UITextView {
            textView.selectAll(nil)
        }
    }
    
    @objc func refreshDevTools() {
        if let devToolsView = devToolsView,
           let textView = devToolsView.viewWithTag(100) as? UITextView {
            // Show loading indicator
            textView.text = "Loading HTML source..."
            
            // Get HTML content from the web view
            webView.evaluateJavaScript("document.documentElement.outerHTML") { [weak self] (result, error) in
                DispatchQueue.main.async {
                    if let html = result as? String {
                        // Format the HTML with indentation for better readability
                        let formattedHTML = self?.formatHTML(html) ?? html
                        textView.text = formattedHTML
                    } else if let error = error {
                        textView.text = "Error loading HTML: \(error.localizedDescription)"
                    }
                }
            }
        }
    }
    
    private func formatHTML(_ html: String) -> String {
        // Simple HTML formatting - you could use a more sophisticated formatter if needed
        var indentLevel = 0
        var formattedHTML = ""
        var isInTag = false
        var isInQuotes = false
        let indentString = "  " // Two spaces for each level
        
        for char in html {
            if char == "<" && !isInQuotes {
                isInTag = true
                if html.dropFirst(html.distance(from: html.startIndex, to: html.firstIndex(of: char)!)).starts(with: "</") {
                    // Closing tag, decrease indent before adding
                    indentLevel = max(0, indentLevel - 1)
                }
                // Add newline and indent before starting a new tag
                formattedHTML += "\n" + String(repeating: indentString, count: indentLevel)
            }
            
            if char == "\"" {
                isInQuotes = !isInQuotes
            }
            
            formattedHTML.append(char)
            
            if char == ">" && !isInQuotes {
                isInTag = false
                // If it's not a self-closing tag or a closing tag, increase indent
                if !formattedHTML.hasSuffix("/>") && !formattedHTML.contains("</") {
                    indentLevel += 1
                }
            }
        }
        
        return formattedHTML
    }
    
    // MARK: - Credential Management
    
    func saveCredentials(username: String, password: String, for domain: String) {
        credentialsManager.saveCredentials(username, password: password, for: domain)
    }
    
    func loadCredentials(for domain: String) -> Credentials? {
        return credentialsManager.loadCredentials(for: domain)
    }
    
    func autoFillCredentials(for domain: String) {
        if let credentials = loadCredentials(for: domain) {
            // JavaScript to autofill credentials
            let jsScript = """
            function fillCredentials(username, password) {
                const usernameFields = document.querySelectorAll('input[type="text"], input[type="email"], input[name="username"], input[name="email"]');
                const passwordFields = document.querySelectorAll('input[type="password"]');
                
                if (usernameFields.length > 0) {
                    usernameFields[0].value = username;
                }
                
                if (passwordFields.length > 0) {
                    passwordFields[0].value = password;
                }
                
                return { usernameField: usernameFields.length > 0, passwordField: passwordFields.length > 0 };
            }
            fillCredentials("\(credentials.username)", "\(credentials.password)");
            """
            
            webView.evaluateJavaScript(jsScript) { (result, error) in
                if let error = error {
                    print("Error autofilling credentials: \(error)")
                }
            }
        }
    }
    
    // MARK: - Session Management
    
    @objc func saveCurrentSession() {
        guard let currentURL = webView.url else { return }
        
        // Create or update the current session
        if currentSession == nil {
            currentSession = BrowsingSession(url: currentURL, title: webView.title ?? "Untitled")
        } else {
            currentSession?.url = currentURL
            currentSession?.title = webView.title ?? "Untitled"
            currentSession?.lastVisited = Date()
        }
        
        // Save to UserDefaults
        saveSessionToDefaults()
        
        print("Saved current session: \(currentURL)")
    }
    
    private func saveSessionToDefaults() {
        guard let currentSession = currentSession else { return }
        
        // Save the current session to UserDefaults
        let sessionData: [String: Any] = [
            "url": currentSession.url.absoluteString,
            "title": currentSession.title,
            "lastVisited": currentSession.lastVisited
        ]
        
        UserDefaults.standard.set(sessionData, forKey: "lastBrowsingSession")
        UserDefaults.standard.synchronize()
    }
    
    func loadSavedSession() {
        if let sessionData = UserDefaults.standard.dictionary(forKey: "lastBrowsingSession"),
           let urlString = sessionData["url"] as? String,
           let url = URL(string: urlString) {
            
            let title = sessionData["title"] as? String ?? "Untitled"
            let lastVisited = sessionData["lastVisited"] as? Date ?? Date()
            
            // Create a session object
            currentSession = BrowsingSession(url: url, title: title, lastVisited: lastVisited)
            
            // Load the URL
            print("Restoring previous session: \(url)")
            loadURL(url)
        } else {
            // Load default URL if no saved session exists
            if let defaultURL = URL(string: "https://www.google.com") {
                print("Loading default URL: \(defaultURL)")
                loadURL(defaultURL)
            }
        }
    }
    
    // MARK: - Credential Management
    
    private func saveCredentials() {
        let alertController = UIAlertController(title: "Save Credentials", message: "Enter your credentials for this site", preferredStyle: .alert)
        
        alertController.addTextField { textField in
            textField.placeholder = "Username"
        }
        
        alertController.addTextField { textField in
            textField.placeholder = "Password"
            textField.isSecureTextEntry = true
        }
        
        let saveAction = UIAlertAction(title: "Save", style: .default) { [weak self] _ in
            guard let username = alertController.textFields?[0].text,
                  let password = alertController.textFields?[1].text,
                  let url = self?.webView.url?.host else { return }
            
            self?.credentialsManager.saveCredentials(username, password: password, for: url)
            
            let confirmationAlert = UIAlertController(title: "Success", message: "Credentials saved for \(url)", preferredStyle: .alert)
            confirmationAlert.addAction(UIAlertAction(title: "OK", style: .default))
            self?.present(confirmationAlert, animated: true)
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel)
        
        alertController.addAction(saveAction)
        alertController.addAction(cancelAction)
        
        present(alertController, animated: true)
    }
    
    private func viewCredentials() {
        guard let url = webView.url?.host else { return }
        
        if let credentials = credentialsManager.loadCredentials(for: url) {
            let detailAlert = UIAlertController(
                title: url,
                message: "Username: \(credentials.username)\nPassword: \(credentials.password)",
                preferredStyle: .alert
            )
            detailAlert.addAction(UIAlertAction(title: "OK", style: .default))
            present(detailAlert, animated: true)
        } else {
            let alertController = UIAlertController(
                title: "No Saved Credentials",
                message: "No credentials saved for \(url)",
                preferredStyle: .alert
            )
            alertController.addAction(UIAlertAction(title: "OK", style: .default))
            present(alertController, animated: true)
        }
    }
    
    // MARK: - Context Menu Support - for iOS 13 and later
    @available(iOS 13.0, *)
    func webView(_ webView: WKWebView, contextMenuConfigurationForElement elementInfo: WKContextMenuElementInfo, completionHandler: @escaping (UIContextMenuConfiguration?) -> Void) {
        // Create context menu configuration
        let configuration = UIContextMenuConfiguration(identifier: nil, previewProvider: nil) { _ in
            // Create menu actions
            var actions: [UIMenuElement] = []
            
            // Create variable insertion actions
            let variableNames = VariablesManager.shared.getAllVariableNames()
            
            if !variableNames.isEmpty {
                // Create variable menu actions
                let variableActions = variableNames.map { name -> UIAction in
                    return UIAction(title: "Insert \(name)") { [weak self] _ in
                        guard let value = VariablesManager.shared.getValue(for: name) else { return }
                        
                        // Insert variable value into active element
                        let script = """
                        (function() {
                            var activeElement = document.activeElement;
                            if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA')) {
                                var startPos = activeElement.selectionStart;
                                var endPos = activeElement.selectionEnd;
                                var text = activeElement.value;
                                var newText = text.substring(0, startPos) + "\(value)" + text.substring(endPos);
                                activeElement.value = newText;
                                activeElement.selectionStart = activeElement.selectionEnd = startPos + \(value.count);
                                var event = new Event('input', { bubbles: true });
                                activeElement.dispatchEvent(event);
                                return true;
                            }
                            return false;
                        })();
                        """
                        
                        webView.evaluateJavaScript(script) { (result, error) in
                            if let error = error {
                                print("Error inserting variable: \(error)")
                            }
                        }
                    }
                }
                
                // Create variable menu
                if #available(iOS 14.0, *) {
                    let variableMenu = UIMenu(title: "Insert Variable", children: variableActions)
                    actions.append(variableMenu)
                } else {
                    // For iOS 13, add actions directly
                    actions.append(contentsOf: variableActions)
                }
            }
            
            // Add standard context menu actions
            actions.append(UIAction(title: "Copy") { _ in
                webView.copy(nil)
            })
            
            actions.append(UIAction(title: "Paste") { _ in
                webView.paste(nil)
            })
            
            actions.append(UIAction(title: "Select All") { _ in
                webView.selectAll(nil)
            })
            
            return UIMenu(title: "", children: actions)
        }
        
        completionHandler(configuration)
    }
    
    // For iOS 12 and earlier, use long press gesture
    private func setupLegacyContextMenu() {
        let longPressGesture = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
        webView.addGestureRecognizer(longPressGesture)
    }
    
    @objc private func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        if gesture.state == .began {
            let point = gesture.location(in: webView)
            
            // Create a simple context menu for iOS 12 and earlier
            let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
            
            // Add variable insertion options
            let variableNames = VariablesManager.shared.getAllVariableNames()
            for name in variableNames {
                if let value = VariablesManager.shared.getValue(for: name) {
                    alert.addAction(UIAlertAction(title: "Insert \(name)", style: .default) { [weak self] _ in
                        let script = """
                        (function() {
                            var activeElement = document.activeElement;
                            if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA')) {
                                var startPos = activeElement.selectionStart;
                                var endPos = activeElement.selectionEnd;
                                var text = activeElement.value;
                                var newText = text.substring(0, startPos) + "\(value)" + text.substring(endPos);
                                activeElement.value = newText;
                                activeElement.selectionStart = activeElement.selectionEnd = startPos + \(value.count);
                                var event = new Event('input', { bubbles: true });
                                activeElement.dispatchEvent(event);
                                return true;
                            }
                            return false;
                        })();
                        """
                        
                        self?.webView.evaluateJavaScript(script, completionHandler: nil)
                    })
                }
            }
            
            // Standard actions
            alert.addAction(UIAlertAction(title: "Copy", style: .default) { [weak self] _ in
                self?.webView.copy(nil)
            })
            
            alert.addAction(UIAlertAction(title: "Paste", style: .default) { [weak self] _ in
                self?.webView.paste(nil)
            })
            
            alert.addAction(UIAlertAction(title: "Select All", style: .default) { [weak self] _ in
                self?.webView.selectAll(nil)
            })
            
            alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
            
            // For iPad support
            if let popoverController = alert.popoverPresentationController {
                popoverController.sourceView = webView
                popoverController.sourceRect = CGRect(x: point.x, y: point.y, width: 1, height: 1)
            }
            
            present(alert, animated: true)
        }
    }
    
    // MARK: - Variable Insertion
    
    private func inspectElement(at point: CGPoint) {
        // JavaScript to inspect element at point
        let js = """
        (function() {
            var element = document.elementFromPoint(\(point.x), \(point.y));
            return {
                tagName: element.tagName,
                id: element.id,
                className: element.className,
                type: element.type,
                name: element.name,
                value: element.value
            };
        })();
        """
        
        webView.evaluateJavaScript(js) { (result, error) in
            if let error = error {
                print("Error inspecting element: \(error.localizedDescription)")
                return
            }
            
            if let elementInfo = result as? [String: Any] {
                print("Element info: \(elementInfo)")
                
                // Show element info in alert
                let message = """
                Tag: \(elementInfo["tagName"] as? String ?? "Unknown")
                ID: \(elementInfo["id"] as? String ?? "None")
                Class: \(elementInfo["className"] as? String ?? "None")
                Type: \(elementInfo["type"] as? String ?? "None")
                Name: \(elementInfo["name"] as? String ?? "None")
                """
                
                let alert = UIAlertController(title: "Element Info", message: message, preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .default))
                self.present(alert, animated: true)
            }
        }
    }
    
    private func insertVariable(name: String, value: String) {
        // JavaScript to insert text at current active element
        let escapedValue = value.replacingOccurrences(of: "\\", with: "\\\\")
                              .replacingOccurrences(of: "\"", with: "\\\"")
                              .replacingOccurrences(of: "\n", with: "\\n")
        
        let js = """
        (function() {
            var activeElement = document.activeElement;
            if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA')) {
                var startPos = activeElement.selectionStart;
                var endPos = activeElement.selectionEnd;
                var text = activeElement.value;
                var newText = text.substring(0, startPos) + "\(escapedValue)" + text.substring(endPos);
                activeElement.value = newText;
                activeElement.selectionStart = activeElement.selectionEnd = startPos + \(value.count);
                var event = new Event('input', { bubbles: true });
                activeElement.dispatchEvent(event);
                return true;
            }
            return false;
        })();
        """
        
        webView.evaluateJavaScript(js) { (result, error) in
            if let error = error {
                print("Error inserting variable: \(error)")
            } else if let success = result as? Bool, !success {
                print("No suitable input field found for variable insertion")
            }
        }
    }
    
    private func showVariableManager() {
        let variableManagerVC = VariableManagerViewController()
        let navController = UINavigationController(rootViewController: variableManagerVC)
        navController.modalPresentationStyle = .formSheet
        
        present(navController, animated: true)
    }
    
    // MARK: - History Actions
    
    @objc private func showHistory() {
        // Create history view controller
        let historyVC = HistoryListViewController()
        historyVC.delegate = self
        
        // Present in a navigation controller
        let navController = UINavigationController(rootViewController: historyVC)
        navController.modalPresentationStyle = .formSheet
        
        present(navController, animated: true)
    }
    
    // MARK: - Variable Management
    
    @objc private func showVariables() {
        // Create variables view controller
        let variablesVC = VariableManagerViewController()
        variablesVC.delegate = self
        
        // Present in a navigation controller
        let navController = UINavigationController(rootViewController: variablesVC)
        navController.modalPresentationStyle = .formSheet
        
        present(navController, animated: true)
    }
    
    // MARK: - Helper Methods
    
    private func updateNavigationButtons() {
        backButton.isEnabled = webView.canGoBack
        forwardButton.isEnabled = webView.canGoForward
    }
    
    @objc private func showMenu() {
        let alertController = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        // Add options to the menu
        let viewSourceAction = UIAlertAction(title: "View Page Source", style: .default) { [weak self] _ in
            self?.toggleDevTools()
        }
        
        let saveCredentialsAction = UIAlertAction(title: "Save Credentials", style: .default) { [weak self] _ in
            self?.saveCredentials()
        }
        
        let viewCredentialsAction = UIAlertAction(title: "View Credentials", style: .default) { [weak self] _ in
            self?.viewCredentials()
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel)
        
        // Add actions to the alert controller
        alertController.addAction(viewSourceAction)
        alertController.addAction(saveCredentialsAction)
        alertController.addAction(viewCredentialsAction)
        alertController.addAction(cancelAction)
        
        // For iPad compatibility
        if let popoverController = alertController.popoverPresentationController {
            popoverController.sourceView = menuButton
            popoverController.sourceRect = menuButton.bounds
        }
        
        present(alertController, animated: true)
    }
}

// MARK: - WKNavigationDelegate
extension BrowserViewController: WKNavigationDelegate {
    @objc func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        print("Started loading: \(webView.url?.absoluteString ?? "No URL")")
        activityIndicator.startAnimating()
    }
    
    @objc func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        activityIndicator.stopAnimating()
        updateNavigationButtons()
        
        // Update current session
        if let url = webView.url {
            webView.evaluateJavaScript("document.title") { [weak self] (result, error) in
                let title = (result as? String) ?? url.host ?? "Untitled"
                self?.currentSession = BrowsingSession(url: url, title: title, lastVisited: Date())
                self?.saveCurrentSession()
            }
        }
    }
    
    @objc func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        activityIndicator.stopAnimating()
        updateNavigationButtons()
    }
    
    @objc func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        activityIndicator.stopAnimating()
        updateNavigationButtons()
    }
}

// MARK: - WKUIDelegate
extension BrowserViewController: WKUIDelegate {
}

// MARK: - UITextFieldDelegate
extension BrowserViewController: UITextFieldDelegate {
    @objc func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if textField == addressBar, let text = textField.text, !text.isEmpty {
            var urlString = text
            if !urlString.hasPrefix("http://") && !urlString.hasPrefix("https://") {
                urlString = "https://" + urlString
            }
            
            if let url = URL(string: urlString) {
                loadURL(url)
            }
        }
        textField.resignFirstResponder()
        return true
    }
}

// MARK: - HistoryListViewControllerDelegate
extension BrowserViewController: HistoryListViewControllerDelegate {
    func didSelectHistoryItem(_ session: BrowsingSession) {
        loadURL(session.url)
        dismiss(animated: true)
    }
}

// MARK: - VariableSelectionDelegate
extension BrowserViewController: VariableSelectionDelegate {
    func didSelectVariable(name: String, value: String) {
        let script = """
        (function() {
            var activeElement = document.activeElement;
            if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA')) {
                var start = activeElement.selectionStart;
                var end = activeElement.selectionEnd;
                var text = activeElement.value;
                var before = text.substring(0, start);
                var after = text.substring(end);
                activeElement.value = before + "\(value)" + after;
                var newPos = start + \(value.count);
                activeElement.setSelectionRange(newPos, newPos);
                activeElement.dispatchEvent(new Event('input', { bubbles: true }));
                return true;
            }
            return false;
        })();
        """
        
        webView.evaluateJavaScript(script) { (result, error) in
            if let error = error {
                print("Error inserting variable: \(error)")
            } else if let success = result as? Bool, !success {
                print("No suitable input field found for variable insertion")
            }
        }
    }
}

// MARK: - Source View Controller
class SourceViewController: UIViewController {
    private var htmlSource: String
    private var textView: UITextView!
    
    init(htmlSource: String) {
        self.htmlSource = htmlSource
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        // Create text view
        textView = UITextView()
        textView.translatesAutoresizingMaskIntoConstraints = false
        textView.text = htmlSource
        textView.font = UIFont.monospacedSystemFont(ofSize: 12, weight: .regular)
        textView.isEditable = false
        view.addSubview(textView)
        
        // Create toolbar
        let toolbar = UIToolbar()
        toolbar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(toolbar)
        
        // Create a close button
        let closeButton = UIBarButtonItem(title: "Close", style: .done, target: self, action: #selector(dismissView))
        
        // Create a copy all button
        let copyButton = UIBarButtonItem(title: "Copy All", style: .plain, target: self, action: #selector(copyAllHtml))
        
        // Create a flexible space to push items to sides
        let flexSpace = UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil)
        
        // Add the buttons to the toolbar
        toolbar.items = [closeButton, flexSpace, copyButton]
        
        // Add constraints for toolbar
        NSLayoutConstraint.activate([
            toolbar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            toolbar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            toolbar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            toolbar.heightAnchor.constraint(equalToConstant: 44),
            
            textView.topAnchor.constraint(equalTo: toolbar.bottomAnchor),
            textView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            textView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            textView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    @objc private func dismissView() {
        dismiss(animated: true)
    }
    
    @objc private func copyAllHtml() {
        UIPasteboard.general.string = htmlSource
        
        // Show a brief confirmation toast without causing lifecycle issues
        let alert = UIAlertController(title: nil, message: "HTML copied to clipboard", preferredStyle: .alert)
        present(alert, animated: true)
        
        // Dismiss the alert after a short delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            if alert.presentingViewController != nil {
                alert.dismiss(animated: true)
            }
        }
    }
}
