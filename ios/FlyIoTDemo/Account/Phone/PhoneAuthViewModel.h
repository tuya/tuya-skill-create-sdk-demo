#import <Foundation/Foundation.h>

typedef NS_ENUM(NSInteger, PhoneAuthMode) {
    PhoneAuthModePasswordLogin = 0,
    PhoneAuthModeCodeLogin,
    PhoneAuthModeRegister,
    PhoneAuthModeResetPassword,
};

@interface PhoneAuthViewModel : NSObject

@property (nonatomic, copy) void (^onLoading)(BOOL loading);
@property (nonatomic, copy) void (^onCodeSent)(void);
@property (nonatomic, copy) void (^onAuthSuccess)(PhoneAuthMode mode);
@property (nonatomic, copy) void (^onError)(NSString *message);

- (void)sendCodeToPhone:(NSString *)phone countryCode:(NSString *)cc type:(NSInteger)type;
- (void)loginByPassword:(NSString *)phone countryCode:(NSString *)cc password:(NSString *)pwd;
- (void)loginByCode:(NSString *)phone countryCode:(NSString *)cc code:(NSString *)code;
- (void)registerPhone:(NSString *)phone countryCode:(NSString *)cc password:(NSString *)pwd code:(NSString *)code;
- (void)resetPassword:(NSString *)phone countryCode:(NSString *)cc newPassword:(NSString *)pwd code:(NSString *)code;

@end
