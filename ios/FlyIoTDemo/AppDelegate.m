#import "AppDelegate.h"
#import "AppKey.h"
#import "FlyAccountNotifications.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>
#import <ThingSmartBusinessExtensionKit/ThingSmartBusinessExtensionKit.h>
#import <ThingSmartBizCore/ThingSmartBizCore.h>

@interface AppDelegate ()
@property (nonatomic, assign) BOOL isRedirectingToLogin;
@end

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    [[ThingSmartSDK sharedInstance] startWithAppKey:APP_KEY secretKey:APP_SECRET_KEY];

#ifdef DEBUG
    [[ThingSmartSDK sharedInstance] setDebugMode:YES];
#endif

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(userSessionInvalid)
                                                 name:ThingSmartUserNotificationUserSessionInvalid
                                               object:nil];
    return YES;
}

- (void)userSessionInvalid {
    if (self.isRedirectingToLogin) return;
    self.isRedirectingToLogin = YES;
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLogoutNotification object:nil];
        self.isRedirectingToLogin = NO;
    });
}

- (UISceneConfiguration *)application:(UIApplication *)application configurationForConnectingSceneSession:(UISceneSession *)connectingSceneSession options:(UISceneConnectionOptions *)options {
    return [[UISceneConfiguration alloc] initWithName:@"Default Configuration" sessionRole:connectingSceneSession.role];
}

- (void)application:(UIApplication *)application didDiscardSceneSessions:(NSSet<UISceneSession *> *)sceneSessions {
}

@end
