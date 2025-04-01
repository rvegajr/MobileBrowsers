import UIKit

@objc class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    @objc func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        print("SceneDelegate: scene willConnectTo called")
        
        guard let windowScene = (scene as? UIWindowScene) else {
            print("SceneDelegate: Failed to cast scene to windowScene")
            return
        }
        
        print("SceneDelegate: Successfully got windowScene")
        
        // Create window with bright background for immediate visibility
        window = UIWindow(windowScene: windowScene)
        window?.backgroundColor = .systemYellow // Bright yellow for visibility
        
        print("SceneDelegate: Created window with bright yellow background")
        
        // Create and set root view controller
        let browserViewController = BrowserViewController()
        window?.rootViewController = browserViewController
        
        print("SceneDelegate: Set BrowserViewController as root")
        
        // Make the window visible
        window?.makeKeyAndVisible()
        
        print("SceneDelegate: Made window key and visible")
        print("SceneDelegate: Window frame: \(String(describing: window?.frame))")
    }

    @objc func sceneDidDisconnect(_ scene: UIScene) {
        print("SceneDelegate: sceneDidDisconnect called")
    }

    @objc func sceneDidBecomeActive(_ scene: UIScene) {
        print("SceneDelegate: sceneDidBecomeActive called")
    }

    @objc func sceneWillResignActive(_ scene: UIScene) {
        print("SceneDelegate: sceneWillResignActive called")
    }

    @objc func sceneWillEnterForeground(_ scene: UIScene) {
        print("SceneDelegate: sceneWillEnterForeground called")
    }

    @objc func sceneDidEnterBackground(_ scene: UIScene) {
        print("SceneDelegate: sceneDidEnterBackground called")
    }
}
