import Foundation
import Security

// Credentials model
public struct Credentials: Codable {
    public let username: String
    public let password: String
    
    public init(username: String, password: String) {
        self.username = username
        self.password = password
    }
}

open class CredentialsManager {
    // Singleton instance
    public static let shared = CredentialsManager()
    
    private let serviceIdentifier = "com.noctusoft.webkitbrowser"
    
    // Protected initializer to allow subclassing in tests
    public init() {}
    
    open func saveCredentials(_ username: String, password: String, for domain: String) {
        // Create credentials data
        let credentials = Credentials(username: username, password: password)
        guard let credentialsData = try? JSONEncoder().encode(credentials) else { return }
        
        // Prepare query dictionary
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceIdentifier,
            kSecAttrAccount as String: domain,
            kSecValueData as String: credentialsData
        ]
        
        // Delete any existing credentials for this domain
        SecItemDelete(query as CFDictionary)
        
        // Add new credentials
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            print("Error saving credentials: \(status)")
            return
        }
    }
    
    open func loadCredentials(for domain: String) -> Credentials? {
        // Prepare query dictionary
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceIdentifier,
            kSecAttrAccount as String: domain,
            kSecReturnData as String: true
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let credentialsData = result as? Data,
              let credentials = try? JSONDecoder().decode(Credentials.self, from: credentialsData) else {
            return nil
        }
        
        return credentials
    }
    
    open func deleteCredentials(for domain: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceIdentifier,
            kSecAttrAccount as String: domain
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess else {
            print("Error deleting credentials: \(status)")
            return
        }
    }
    
    open func getAllDomains() -> [String] {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceIdentifier,
            kSecReturnAttributes as String: true,
            kSecMatchLimit as String: kSecMatchLimitAll
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let items = result as? [[String: Any]] else {
            return []
        }
        
        return items.compactMap { $0[kSecAttrAccount as String] as? String }
    }
}
