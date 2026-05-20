#import "ProfileViewModel.h"
#import "FlyAccountNotifications.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>

@implementation ProfileViewModel

- (void)loadUserInfo {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] updateUserInfo:^{
        [self loading:NO];
        if (self.onUserInfoRefreshed) self.onUserInfoRefreshed();
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)updateNickname:(NSString *)nickname {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] updateNickname:nickname success:^{
        [self loading:NO];
        if (self.onSuccess) self.onSuccess(@"昵称更新成功");
        if (self.onUserInfoRefreshed) self.onUserInfoRefreshed();
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)logout {
    BOOL isAnonymous = ([ThingSmartUser sharedInstance].phoneNumber.length == 0 &&
                        [ThingSmartUser sharedInstance].email.length == 0);
    if (isAnonymous) {
        [self loading:YES];
        [[ThingSmartUser sharedInstance] deleteAnonymousAccountWithSuccess:^{
            [self loading:NO];
            [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLogoutNotification object:nil];
            if (self.onLoggedOut) self.onLoggedOut();
        } failure:^(NSError *error) {
            [self loading:NO];
            if (self.onError) self.onError(error.localizedDescription);
        }];
    } else {
        [self loading:YES];
        [[ThingSmartUser sharedInstance] loginOut:^{
            [self loading:NO];
            [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLogoutNotification object:nil];
            if (self.onLoggedOut) self.onLoggedOut();
        } failure:^(NSError *error) {
            [self loading:NO];
            if (self.onError) self.onError(error.localizedDescription);
        }];
    }
}

- (void)cancelAccount {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] cancelAccount:^{
        [self loading:NO];
        [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLogoutNotification object:nil];
        if (self.onLoggedOut) self.onLoggedOut();
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)loading:(BOOL)loading {
    if (self.onLoading) self.onLoading(loading);
}

@end
