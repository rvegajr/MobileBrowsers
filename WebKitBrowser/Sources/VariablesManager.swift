import Foundation

/// Manager class for user-defined variables that can be inserted into web forms
class VariablesManager {
    
    // MARK: - Properties
    
    static let shared = VariablesManager()
    
    private var variables: [String: String] = [:]
    private let variablesFileName = "user_variables.plist"
    
    // MARK: - Initialization
    
    private init() {
        loadVariables()
        setupDefaultVariables()
    }
    
    // MARK: - Variable Management
    
    /// Set a variable value
    func setValue(_ value: String, for name: String) {
        variables[name] = value
        saveVariables()
    }
    
    /// Get a variable value by name
    func getValue(for name: String) -> String? {
        return variables[name]
    }
    
    /// Remove a variable
    func removeVariable(named name: String) {
        variables.removeValue(forKey: name)
        saveVariables()
    }
    
    /// Get all variable names
    func getAllVariableNames() -> [String] {
        return Array(variables.keys).sorted()
    }
    
    /// Get all variables
    func getAllVariables() -> [String: String] {
        return variables
    }
    
    // MARK: - Default Variables
    
    private func setupDefaultVariables() {
        // Only set default variables if none exist
        if variables.isEmpty {
            setValue("user123", for: "username")
            setValue("password123", for: "password")
            setValue("john.doe@example.com", for: "email")
            setValue("123-456-7890", for: "phone")
            print("Set up default variables")
        }
    }
    
    // MARK: - File Management
    
    private func loadVariables() {
        guard let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            print("Error: Could not access documents directory")
            return
        }
        
        let fileURL = documentsDirectory.appendingPathComponent(variablesFileName)
        
        if FileManager.default.fileExists(atPath: fileURL.path) {
            do {
                let data = try Data(contentsOf: fileURL)
                if let loadedVariables = try PropertyListSerialization.propertyList(from: data, options: [], format: nil) as? [String: String] {
                    variables = loadedVariables
                    print("Successfully loaded \(variables.count) variables")
                }
            } catch {
                print("Error loading variables: \(error)")
            }
        } else {
            print("No variables file found, starting with defaults")
        }
    }
    
    private func saveVariables() {
        guard let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            print("Error: Could not access documents directory")
            return
        }
        
        let fileURL = documentsDirectory.appendingPathComponent(variablesFileName)
        
        do {
            let data = try PropertyListSerialization.data(fromPropertyList: variables, format: .xml, options: 0)
            try data.write(to: fileURL)
            print("Successfully saved \(variables.count) variables")
        } catch {
            print("Error saving variables: \(error)")
        }
    }
}
