#import "PhoneAuthViewModel.h"
#import "FlyAccountNotifications.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>

@implementation PhoneAuthViewModel

- (void)sendCodeToPhone:(NSString *)phone countryCode:(NSString *)cc type:(NSInteger)type {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] sendVerifyCodeWithUserName:phone
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

- (void)loginByPassword:(NSString *)phone countryCode:(NSString *)cc password:(NSString *)pwd {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] loginByPhone:cc
                                      phoneNumber:phone
                                         password:pwd
                                          success:^{
        [self loading:NO];
        [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLoginNotification object:nil];
        if (self.onAuthSuccess) self.onAuthSuccess(PhoneAuthModePasswordLogin);
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)loginByCode:(NSString *)phone countryCode:(NSString *)cc code:(NSString *)code {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] loginWithMobile:phone
                                        countryCode:cc
                                               code:code
                                            success:^{
        [self loading:NO];
        [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLoginNotification object:nil];
        if (self.onAuthSuccess) self.onAuthSuccess(PhoneAuthModeCodeLogin);
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)registerPhone:(NSString *)phone countryCode:(NSString *)cc password:(NSString *)pwd code:(NSString *)code {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] registerByPhone:cc
                                         phoneNumber:phone
                                            password:pwd
                                               code:code
                                            success:^{
        [self loading:NO];
        [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLoginNotification object:nil];
        if (self.onAuthSuccess) self.onAuthSuccess(PhoneAuthModeRegister);
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)resetPassword:(NSString *)phone countryCode:(NSString *)cc newPassword:(NSString *)pwd code:(NSString *)code {
    [self loading:YES];
    [[ThingSmartUser sharedInstance] resetPasswordByPhone:cc
                                              phoneNumber:phone
                                              newPassword:pwd
                                                     code:code
                                                 success:^{
        [self loading:NO];
        if (self.onAuthSuccess) self.onAuthSuccess(PhoneAuthModeResetPassword);
    } failure:^(NSError *error) {
        [self loading:NO];
        if (self.onError) self.onError(error.localizedDescription);
    }];
}

- (void)loading:(BOOL)loading {
    if (self.onLoading) self.onLoading(loading);
}

@end
