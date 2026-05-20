#import "CurrentHomeManager.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>

NSString * const CurrentHomeDidChangeNotification      = @"CurrentHomeDidChangeNotification";
NSString * const SharedDeviceListDidChangeNotification = @"SharedDeviceListDidChangeNotification";

@interface CurrentHomeManager () <ThingSmartHomeManagerDelegate>
@property (nonatomic, strong) ThingSmartHomeManager *homeManager;
@property (nonatomic, assign, readwrite) long long currentHomeId;
@property (nonatomic, copy,   readwrite) NSString  *currentHomeName;
@end

@implementation CurrentHomeManager

+ (instancetype)sharedInstance {
    static CurrentHomeManager *instance;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{ instance = [[self alloc] init]; });
    return instance;
}

#pragma mark - Public

- (void)initializeWithCompletion:(void(^)(long long homeId))completion {
    self.homeManager          = [ThingSmartHomeManager new];
    self.homeManager.delegate = self;

    [self.homeManager getHomeListWithSuccess:^(NSArray<ThingSmartHomeModel *> *homes) {
        if (homes.count == 0) {
            self.currentHomeId   = 0;
            self.currentHomeName = nil;
            [self broadcastChange];
            if (completion) completion(0);
            return;
        }

        long long storedId = [self storedHomeId];
        ThingSmartHomeModel *target = nil;
        for (ThingSmartHomeModel *home in homes) {
            if (home.homeId == storedId) { target = home; break; }
        }
        if (!target) target = homes.firstObject;

        self.currentHomeId   = target.homeId;
        self.currentHomeName = target.name;
        [self persistHomeId:target.homeId];
        [self broadcastChange];
        if (completion) completion(self.currentHomeId);

        ThingSmartHome *home = [ThingSmartHome homeWithHomeId:target.homeId];
        [home getHomeDataWithSuccess:^(ThingSmartHomeModel *homeModel) {
            if (![homeModel.name isEqualToString:self.currentHomeName]) {
                self.currentHomeName = homeModel.name;
                dispatch_async(dispatch_get_main_queue(), ^{ [self broadcastChange]; });
            }
        } failure:^(NSError *error) {}];
    } failure:^(NSError *error) {
        if (completion) completion(0);
    }];
}

- (void)switchHomeWithHomeId:(long long)homeId {
    self.currentHomeId = homeId;
    [self persistHomeId:homeId];

    ThingSmartHome *home = [ThingSmartHome homeWithHomeId:homeId];
    [home getHomeDataWithSuccess:^(ThingSmartHomeModel *homeModel) {
        self.currentHomeName = homeModel.name;
        dispatch_async(dispatch_get_main_queue(), ^{ [self broadcastChange]; });
    } failure:^(NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{ [self broadcastChange]; });
    }];
}

- (void)cleanup {
    self.homeManager.delegate = nil;
    self.homeManager          = nil;
    [self clearStoredHomeId];
    self.currentHomeId   = 0;
    self.currentHomeName = nil;
}

#pragma mark - ThingSmartHomeManagerDelegate

- (void)homeManager:(ThingSmartHomeManager *)manager didAddHome:(ThingSmartHomeModel *)home {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSString *msg = [NSString stringWithFormat:@"您已被加入家庭「%@」，是否切换到该家庭？", home.name];
        UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"家庭邀请"
                                                                       message:msg
                                                                preferredStyle:UIAlertControllerStyleAlert];
        __weak typeof(self) w = self;
        [alert addAction:[UIAlertAction actionWithTitle:@"切换"
                                                  style:UIAlertActionStyleDefault
                                                handler:^(UIAlertAction *a) {
            [w switchHomeWithHomeId:home.homeId];
        }]];
        [alert addAction:[UIAlertAction actionWithTitle:@"留在当前家庭"
                                                  style:UIAlertActionStyleCancel
                                                handler:nil]];
        [[self topViewController] presentViewController:alert animated:YES completion:nil];
    });
}

- (void)homeManager:(ThingSmartHomeManager *)manager didRemoveHome:(long long)homeId {
    if (homeId != self.currentHomeId) return;

    [self.homeManager getHomeListWithSuccess:^(NSArray<ThingSmartHomeModel *> *homes) {
        dispatch_async(dispatch_get_main_queue(), ^{
            NSString *msg;
            if (homes.count > 0) {
                ThingSmartHomeModel *fallback = homes.firstObject;
                [self switchHomeWithHomeId:fallback.homeId];
                msg = [NSString stringWithFormat:@"您已被移出当前家庭，已自动切换到「%@」", fallback.name];
            } else {
                self.currentHomeId   = 0;
                self.currentHomeName = @"无家庭";
                [self clearStoredHomeId];
                [self broadcastChange];
                msg = @"您已被移出家庭，当前无可用家庭";
            }
            UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"家庭变更"
                                                                           message:msg
                                                                    preferredStyle:UIAlertControllerStyleAlert];
            [alert addAction:[UIAlertAction actionWithTitle:@"确定"
                                                      style:UIAlertActionStyleDefault
                                                    handler:nil]];
            [[self topViewController] presentViewController:alert animated:YES completion:nil];
        });
    } failure:^(NSError *error) {}];
}

- (void)serviceConnectedSuccess {
    if (self.currentHomeId == 0) return;
    ThingSmartHome *home = [ThingSmartHome homeWithHomeId:self.currentHomeId];
    [home getHomeDataWithSuccess:^(ThingSmartHomeModel *homeModel) {
        self.currentHomeName = homeModel.name;
        dispatch_async(dispatch_get_main_queue(), ^{ [self broadcastChange]; });
    } failure:^(NSError *error) {}];
}

#pragma mark - Helpers

- (void)broadcastChange {
    [[NSNotificationCenter defaultCenter] postNotificationName:CurrentHomeDidChangeNotification
                                                        object:nil
                                                      userInfo:@{@"homeId": @(self.currentHomeId)}];
}

- (NSString *)defaultsKey {
    NSString *uid = [ThingSmartUser sharedInstance].uid ?: @"anonymous";
    return [NSString stringWithFormat:@"current_home_id_%@", uid];
}

- (long long)storedHomeId {
    NSNumber *n = [[NSUserDefaults standardUserDefaults] objectForKey:[self defaultsKey]];
    return n ? n.longLongValue : 0;
}

- (void)persistHomeId:(long long)homeId {
    [[NSUserDefaults standardUserDefaults] setObject:@(homeId) forKey:[self defaultsKey]];
}

- (void)clearStoredHomeId {
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:[self defaultsKey]];
}

- (UIViewController *)topViewController {
    UIWindowScene *scene = nil;
    for (UIScene *s in [UIApplication sharedApplication].connectedScenes) {
        if ([s isKindOfClass:[UIWindowScene class]]) { scene = (UIWindowScene *)s; break; }
    }
    UIViewController *root = scene.windows.firstObject.rootViewController;
    return [self topViewControllerFrom:root];
}

- (UIViewController *)topViewControllerFrom:(UIViewController *)vc {
    if (vc.presentedViewController)
        return [self topViewControllerFrom:vc.presentedViewController];
    if ([vc isKindOfClass:[UITabBarController class]])
        return [self topViewControllerFrom:((UITabBarController *)vc).selectedViewController];
    if ([vc isKindOfClass:[UINavigationController class]])
        return [self topViewControllerFrom:((UINavigationController *)vc).visibleViewController];
    return vc;
}

@end
