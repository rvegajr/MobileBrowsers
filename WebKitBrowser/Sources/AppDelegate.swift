import UIKit

@main
@objc class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    @objc func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        print("AppDelegate: didFinishLaunchingWithOptions called")
        
        // This is for iOS < 13 support, or for apps without SceneDelegate
        if #available(iOS 13.0, *) {
            // Use SceneDelegate for iOS 13+
            print("AppDelegate: Using scene-based lifecycle")
        } else {
            // Create window manually for iOS < 13
            print("AppDelegate: Using window-based lifecycle (iOS < 13)")
            window = UIWindow(frame: UIScreen.main.bounds)
            window?.backgroundColor = .systemRed // Very visible background color
            
            let browserViewController = BrowserViewController()
            window?.rootViewController = browserViewController
            window?.makeKeyAndVisible()
            
            print("AppDelegate: Created and displayed window with BrowserViewController")
        }
        
        return true
    }

    // MARK: UISceneSession Lifecycle

    @available(iOS 13.0, *)
    @objc func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        print("AppDelegate: configurationForConnecting scene session called")
        print("AppDelegate: Creating scene configuration with SceneDelegate class")
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    @available(iOS 13.0, *)
    @objc func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
        print("AppDelegate: didDiscardSceneSessions called")
    }
}
