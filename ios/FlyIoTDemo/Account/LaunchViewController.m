#import "LaunchViewController.h"
#import "AuthEntryViewController.h"
#import "SceneDelegate.h"
#import "CurrentHomeManager.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>

@implementation LaunchViewController

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([ThingSmartUser sharedInstance].isLogin) {
            UIWindowScene *ws = (UIWindowScene *)self.view.window.windowScene;
            SceneDelegate  *sd = (SceneDelegate *)ws.delegate;
            [sd showMainTabBar];
            [[CurrentHomeManager sharedInstance] initializeWithCompletion:nil];
        } else {
            self.view.window.rootViewController =
                [[UINavigationController alloc] initWithRootViewController:
                    [[AuthEntryViewController alloc] init]];
        }
    });
}

@end
