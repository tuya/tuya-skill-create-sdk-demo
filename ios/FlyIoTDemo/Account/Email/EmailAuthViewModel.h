#import <Foundation/Foundation.h>

typedef NS_ENUM(NSInteger, EmailAuthMode) {
    EmailAuthModePasswordLogin = 0,
    EmailAuthModeCodeLogin,
    EmailAuthModeRegister,
    EmailAuthModeResetPassword,
};

@interface EmailAuthViewModel : NSObject

@property (nonatomic, copy) void (^onLoading)(BOOL loading);
@property (nonatomic, copy) void (^onCodeSent)(void);
@property (nonatomic, copy) void (^onAuthSuccess)(EmailAuthMode mode);
@property (nonatomic, copy) void (^onError)(NSString *message);

- (void)sendCodeToEmail:(NSString *)email countryCode:(NSString *)cc type:(NSInteger)type;
- (void)loginByPassword:(NSString *)email countryCode:(NSString *)cc password:(NSString *)pwd;
- (void)loginByCode:(NSString *)email countryCode:(NSString *)cc code:(NSString *)code;
- (void)registerEmail:(NSString *)email countryCode:(NSString *)cc password:(NSString *)pwd code:(NSString *)code;
- (void)resetPassword:(NSString *)email countryCode:(NSString *)cc newPassword:(NSString *)pwd code:(NSString *)code;

@end
