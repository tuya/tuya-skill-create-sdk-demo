#import <Foundation/Foundation.h>

FOUNDATION_EXTERN NSString * const CurrentHomeDidChangeNotification;
FOUNDATION_EXTERN NSString * const SharedDeviceListDidChangeNotification;

@interface CurrentHomeManager : NSObject

+ (instancetype)sharedInstance;

@property (nonatomic, assign, readonly) long long currentHomeId;
@property (nonatomic, copy,   readonly) NSString  *currentHomeName;

- (void)initializeWithCompletion:(void(^)(long long homeId))completion;
- (void)switchHomeWithHomeId:(long long)homeId;
- (void)cleanup;

@end
