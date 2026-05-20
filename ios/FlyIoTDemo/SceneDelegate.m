#import "SceneDelegate.h"
#import "LaunchViewController.h"
#import "AuthEntryViewController.h"
#import "ProfileViewController.h"
#import "HomeViewController.h"
#import "CurrentHomeManager.h"
#import "FlyAccountNotifications.h"
#import <ThingSmartBizCore/ThingSmartBizCore.h>
#import <ThingModuleServices/ThingThemeManagerProtocol.h>
#import <ThingSmartBusinessExtensionKit/ThingSmartBusinessExtensionKit.h>

@implementation SceneDelegate

- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session options:(UISceneConnectionOptions *)connectionOptions {
    if (![scene isKindOfClass:[UIWindowScene class]]) return;
    UIWindowScene *ws = (UIWindowScene *)scene;

    self.window = [[UIWindow alloc] initWithWindowScene:ws];
    self.window.rootViewController = [[LaunchViewController alloc] init];
    [self.window makeKeyAndVisible];

    // BizBundle must be initialized after makeKeyAndVisible to avoid lock contention
    [ThingSmartBusinessExtensionConfig setupConfig];
    [[ThingSmartBizCore sharedInstance] registerRouteWithHandler:^BOOL(NSString *url, NSDictionary *raw) {
        return NO;
    }];

    [self applyAppTheme];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(userDidLogin)
                                                 name:FlyUserDidLoginNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(userDidLogout)
                                                 name:FlyUserDidLogoutNotification
                                               object:nil];
}

- (void)applyAppTheme {
    id<ThingThemeManagerProtocol> impl =
        [[ThingSmartBizCore sharedInstance] serviceOfProtocol:@protocol(ThingThemeManagerProtocol)];
    if (!impl) return;

    UIColor *brandBlue = [UIColor colorWithRed:22.0/255.0 green:119.0/255.0 blue:255.0/255.0 alpha:1.0];

    [impl configNormalModuleWithThemeColor:brandBlue
                           backgroundColor:nil
                              warningColor:nil
                                 tipsColor:nil
                                guideColor:nil
                         navigationBarColor:nil
                        tabBarSelectedColor:brandBlue
                            alertMaskAlpha:nil];

    if (@available(iOS 13.0, *)) {
        [impl configDarkModuleWithThemeColor:brandBlue
                             backgroundColor:nil
                                warningColor:nil
                                   tipsColor:nil
                                  guideColor:nil
                           navigationBarColor:nil
                          tabBarSelectedColor:brandBlue
                              alertMaskAlpha:nil];
    }
}

- (void)userDidLogin {
    [[ThingSmartBizCore sharedInstance] updateConfig];
    [self showMainTabBar];
    [[CurrentHomeManager sharedInstance] initializeWithCompletion:nil];
}

- (void)showMainTabBar {
    HomeViewController    *homeVC   = [[HomeViewController alloc] init];
    UINavigationController *homeNav = [[UINavigationController alloc] initWithRootViewController:homeVC];
    homeNav.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"首页"
                                                       image:[UIImage systemImageNamed:@"house"]
                                                         tag:0];

    ProfileViewController  *profileVC  = [[ProfileViewController alloc] init];
    UINavigationController *profileNav = [[UINavigationController alloc] initWithRootViewController:profileVC];
    profileNav.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"我的"
                                                          image:[UIImage systemImageNamed:@"person"]
                                                            tag:1];

    UITabBarController *tab = [[UITabBarController alloc] init];
    tab.viewControllers     = @[homeNav, profileNav];

    [UIView transitionWithView:self.window
                      duration:0.3
                       options:UIViewAnimationOptionTransitionCrossDissolve
                    animations:^{ self.window.rootViewController = tab; }
                    completion:nil];
}

- (void)userDidLogout {
    [[CurrentHomeManager sharedInstance] cleanup];

    AuthEntryViewController *authVC = [[AuthEntryViewController alloc] init];
    UINavigationController  *nav    = [[UINavigationController alloc] initWithRootViewController:authVC];
    [UIView transitionWithView:self.window
                      duration:0.3
                       options:UIViewAnimationOptionTransitionCrossDissolve
                    animations:^{ self.window.rootViewController = nav; }
                    completion:nil];
}

- (void)sceneDidDisconnect:(UIScene *)scene {}
- (void)sceneDidBecomeActive:(UIScene *)scene {}
- (void)sceneWillResignActive:(UIScene *)scene {}
- (void)sceneWillEnterForeground:(UIScene *)scene {}
- (void)sceneDidEnterBackground:(UIScene *)scene {}

@end
