#import "AuthEntryViewController.h"
#import "PhoneAuthViewController.h"
#import "EmailAuthViewController.h"
#import "FlyAccountNotifications.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>

@implementation AuthEntryViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.title = @"登录";
    self.view.backgroundColor = UIColor.systemBackgroundColor;
    [self setupUI];
}

- (void)setupUI {
    UILabel *titleLabel = [[UILabel alloc] init];
    titleLabel.text = @"FlyIoT Demo";
    titleLabel.font = [UIFont boldSystemFontOfSize:30];
    titleLabel.textAlignment = NSTextAlignmentCenter;

    UIButton *phoneBtn = [self makeBtn:@"手机号登录 / 注册" sel:@selector(tapPhone) primary:YES];
    UIButton *emailBtn = [self makeBtn:@"邮箱登录 / 注册" sel:@selector(tapEmail) primary:YES];
    UIButton *anonBtn  = [self makeBtn:@"匿名体验" sel:@selector(tapAnon) primary:NO];

    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[titleLabel, phoneBtn, emailBtn, anonBtn]];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.spacing = 20;
    stack.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:stack];

    [NSLayoutConstraint activateConstraints:@[
        [stack.centerXAnchor constraintEqualToAnchor:self.view.centerXAnchor],
        [stack.centerYAnchor constraintEqualToAnchor:self.view.centerYAnchor constant:-30],
        [stack.widthAnchor constraintEqualToAnchor:self.view.widthAnchor constant:-48],
    ]];
}

- (UIButton *)makeBtn:(NSString *)title sel:(SEL)sel primary:(BOOL)primary {
    UIButton *btn = [UIButton buttonWithType:UIButtonTypeSystem];
    btn.translatesAutoresizingMaskIntoConstraints = NO;
    [btn setTitle:title forState:UIControlStateNormal];
    btn.backgroundColor = primary ? UIColor.systemBlueColor : UIColor.systemGray5Color;
    [btn setTitleColor:primary ? UIColor.whiteColor : UIColor.labelColor forState:UIControlStateNormal];
    btn.titleLabel.font = [UIFont systemFontOfSize:16 weight:UIFontWeightMedium];
    btn.layer.cornerRadius = 10;
    [btn addTarget:self action:sel forControlEvents:UIControlEventTouchUpInside];
    [btn.heightAnchor constraintEqualToConstant:50].active = YES;
    return btn;
}

- (void)tapPhone {
    [self.navigationController pushViewController:[[PhoneAuthViewController alloc] init] animated:YES];
}

- (void)tapEmail {
    [self.navigationController pushViewController:[[EmailAuthViewController alloc] init] animated:YES];
}

- (void)tapAnon {
    [[ThingSmartUser sharedInstance] registerAnonymousWithCountryCode:@"86" success:^{
        dispatch_async(dispatch_get_main_queue(), ^{
            [[NSNotificationCenter defaultCenter] postNotificationName:FlyUserDidLoginNotification object:nil];
        });
    } failure:^(NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"错误"
                                                                           message:error.localizedDescription
                                                                    preferredStyle:UIAlertControllerStyleAlert];
            [alert addAction:[UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:nil]];
            [self presentViewController:alert animated:YES completion:nil];
        });
    }];
}

@end
