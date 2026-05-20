#import <Foundation/Foundation.h>

@interface ProfileViewModel : NSObject

@property (nonatomic, copy) void (^onLoading)(BOOL loading);
@property (nonatomic, copy) void (^onUserInfoRefreshed)(void);
@property (nonatomic, copy) void (^onSuccess)(NSString *message);
@property (nonatomic, copy) void (^onError)(NSString *message);
@property (nonatomic, copy) void (^onLoggedOut)(void);

- (void)loadUserInfo;
- (void)updateNickname:(NSString *)nickname;
- (void)logout;
- (void)cancelAccount;

@end
