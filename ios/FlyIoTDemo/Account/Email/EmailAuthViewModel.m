#import "EmailAuthViewModel.h"
#import "FlyAccountNotifications.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>

@implementation EmailAuthViewModel

- (void)sendCodeToEmail:(NSString *)email countryCode:(NSString *)cc type:(NSInteger)type {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] sendVerifyCodeWithUserName:email
                                                        region:nil
                                                   countryCode:cc
                                                          type:type
                                                       success:^{
        [self loading:NO];
        if (self.onCodeSent) self.onCodeSent();
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)loginByPassword:(NSString *)email countryCode:(NSString *)cc password:(NSString *)pwd {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] loginByEmail:cc
                                            email:email
                                         password:pwd
                                          success:^{
        [self loading:NO];
        [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLoginNotification object:nil];
        if (self.onAuthSuccess) self.onAuthSuccess(EmailAuthModePasswordLogin);
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)loginByCode:(NSString *)email countryCode:(NSString *)cc code:(NSString *)code {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] loginWithEmail:email
                                        countryCode:cc
                                               code:code
                                            success:^{
        [self loading:NO];
        [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLoginNotification object:nil];
        if (self.onAuthSuccess) self.onAuthSuccess(EmailAuthModeCodeLogin);
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)registerEmail:(NSString *)email countryCode:(NSString *)cc password:(NSString *)pwd code:(NSString *)code {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] registerByEmail:cc
                                               email:email
                                            password:pwd
                                               code:code
                                            success:^{
        [self loading:NO];
        [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLoginNotification object:nil];
        if (self.onAuthSuccess) self.onAuthSuccess(EmailAuthModeRegister);
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)resetPassword:(NSString *)email countryCode:(NSString *)cc newPassword:(NSString *)pwd code:(NSString *)code {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] resetPasswordByEmail:cc
                                                    email:email
                                              newPassword:pwd
                                                     code:code
                                                 success:^{
        [self loading:NO];
        if (self.onAuthSuccess) self.onAuthSuccess(EmailAuthModeResetPassword);
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)loading:(BOOL)loading {
    if (self.onLoading) self.onLoading(loading);
}

@end
