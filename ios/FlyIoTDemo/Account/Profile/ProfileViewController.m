#import "ProfileViewController.h"
#import "ProfileViewModel.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>
#import <ThingSmartBizCore/ThingSmartBizCore.h>
#import <ThingModuleServices/ThingFamilyProtocol.h>

@interface ProfileViewController ()
@property (nonatomic, strong) ProfileViewModel *vm;
@property (nonatomic, strong) UILabel *accountLabel;
@property (nonatomic, strong) UILabel *nicknameLabel;
@property (nonatomic, strong) UILabel *uidLabel;
@property (nonatomic, strong) UIActivityIndicatorView *spinner;
@end

@implementation ProfileViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.title = @"个人中心";
    self.view.backgroundColor = UIColor.systemBackgroundColor;
    self.vm = [[ProfileViewModel alloc] init];
    [self bindViewModel];
    [self buildUI];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    UINavigationBarAppearance *ap = [[UINavigationBarAppearance alloc] init];
    [ap configureWithOpaqueBackground];
    self.navigationController.navigationBar.standardAppearance   = ap;
    self.navigationController.navigationBar.scrollEdgeAppearance = ap;
    self.navigationController.navigationBar.tintColor =
        [UIColor colorWithRed:22.0/255.0 green:119.0/255.0 blue:255.0/255.0 alpha:1.0];
    [self.vm loadUserInfo];
}

- (void)bindViewModel {
    __weak typeof(self) w = self;
    self.vm.onLoading = ^(BOOL loading) {
        dispatch_async(dispatch_get_main_queue(), ^{
            loading ? [w.spinner startAnimating] : [w.spinner stopAnimating];
        });
    };
    self.vm.onUserInfoRefreshed = ^{
        dispatch_async(dispatch_get_main_queue(), ^{
            [w refreshLabels];
        });
    };
    self.vm.onSuccess = ^(NSString *msg) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [w showTip:msg];
        });
    };
    self.vm.onError = ^(NSString *msg) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [w showAlert:@"错误" message:msg];
        });
    };
    self.vm.onLoggedOut = ^{
        // Navigation handled via FlyUserDidLogoutNotification in SceneDelegate
    };
}

- (void)buildUI {
    self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleMedium];
    self.spinner.hidesWhenStopped = YES;
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithCustomView:self.spinner];

    self.accountLabel  = [self infoLabel];
    self.nicknameLabel = [self infoLabel];
    self.uidLabel      = [self infoLabel];

    UIStackView *infoStack = [[UIStackView alloc] initWithArrangedSubviews:@[
        [self rowWithKey:@"账号" valueLabel:self.accountLabel],
        [self rowWithKey:@"昵称" valueLabel:self.nicknameLabel],
        [self rowWithKey:@"UID"  valueLabel:self.uidLabel],
    ]];
    infoStack.axis = UILayoutConstraintAxisVertical;
    infoStack.spacing = 12;

    UIButton *familyBtn   = [self actionBtn:@"家庭管理"     sel:@selector(tapFamily)   destructive:NO];
    UIButton *refreshBtn  = [self actionBtn:@"刷新用户信息" sel:@selector(tapRefresh)  destructive:NO];
    UIButton *nicknameBtn = [self actionBtn:@"修改昵称"     sel:@selector(tapNickname) destructive:NO];
    UIButton *logoutBtn   = [self actionBtn:@"退出登录"     sel:@selector(tapLogout)   destructive:YES];
    UIButton *cancelBtn   = [self actionBtn:@"注销账号"     sel:@selector(tapCancel)   destructive:YES];

    UIStackView *form = [[UIStackView alloc] initWithArrangedSubviews:@[
        infoStack, familyBtn, refreshBtn, nicknameBtn, logoutBtn, cancelBtn
    ]];
    form.axis = UILayoutConstraintAxisVertical;
    form.spacing = 16;
    form.translatesAutoresizingMaskIntoConstraints = NO;

    UIScrollView *scroll = [[UIScrollView alloc] init];
    scroll.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:scroll];
    [scroll addSubview:form];

    UILayoutGuide *content = scroll.contentLayoutGuide;
    UILayoutGuide *frame  = scroll.frameLayoutGuide;
    [NSLayoutConstraint activateConstraints:@[
        [scroll.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor],
        [scroll.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [scroll.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [scroll.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
        [form.topAnchor constraintEqualToAnchor:content.topAnchor constant:24],
        [form.leadingAnchor constraintEqualToAnchor:content.leadingAnchor constant:24],
        [form.trailingAnchor constraintEqualToAnchor:content.trailingAnchor constant:-24],
        [form.bottomAnchor constraintEqualToAnchor:content.bottomAnchor constant:-24],
        [form.widthAnchor constraintEqualToAnchor:frame.widthAnchor constant:-48],
    ]];
}

- (UILabel *)infoLabel {
    UILabel *label = [[UILabel alloc] init];
    label.font = [UIFont systemFontOfSize:15];
    label.textColor = UIColor.secondaryLabelColor;
    label.numberOfLines = 1;
    return label;
}

- (UIStackView *)rowWithKey:(NSString *)key valueLabel:(UILabel *)valueLabel {
    UILabel *keyLabel = [[UILabel alloc] init];
    keyLabel.text = key;
    keyLabel.font = [UIFont systemFontOfSize:15 weight:UIFontWeightMedium];
    [keyLabel setContentHuggingPriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];

    UIStackView *row = [[UIStackView alloc] initWithArrangedSubviews:@[keyLabel, valueLabel]];
    row.axis = UILayoutConstraintAxisHorizontal;
    row.spacing = 12;
    return row;
}

- (UIButton *)actionBtn:(NSString *)title sel:(SEL)sel destructive:(BOOL)destructive {
    UIButton *btn = [UIButton buttonWithType:UIButtonTypeSystem];
    btn.translatesAutoresizingMaskIntoConstraints = NO;
    [btn setTitle:title forState:UIControlStateNormal];
    btn.backgroundColor = destructive ? UIColor.systemRedColor : UIColor.systemBlueColor;
    [btn setTitleColor:UIColor.whiteColor forState:UIControlStateNormal];
    btn.titleLabel.font = [UIFont systemFontOfSize:16 weight:UIFontWeightMedium];
    btn.layer.cornerRadius = 10;
    [btn addTarget:self action:sel forControlEvents:UIControlEventTouchUpInside];
    [btn.heightAnchor constraintEqualToConstant:50].active = YES;
    return btn;
}

- (void)refreshLabels {
    ThingSmartUser *user = [ThingSmartUser sharedInstance];
    NSString *account = user.phoneNumber.length > 0 ? user.phoneNumber :
                        user.email.length > 0       ? user.email       : @"（匿名用户）";
    self.accountLabel.text  = account;
    self.nicknameLabel.text = user.nickname.length > 0 ? user.nickname : @"—";
    self.uidLabel.text      = user.uid ?: @"—";
}

- (void)tapFamily {
    id<ThingFamilyProtocol> impl =
        [[ThingSmartBizCore sharedInstance] serviceOfProtocol:@protocol(ThingFamilyProtocol)];
    if (!impl) {
        [self showAlert:@"提示" message:@"家庭管理业务包未加载"];
        return;
    }
    [[ThingSmartHomeManager new] getHomeListWithSuccess:^(NSArray<ThingSmartHomeModel *> *homes) {
        if (homes.firstObject && [impl respondsToSelector:@selector(updateCurrentFamilyId:)]) {
            [impl updateCurrentFamilyId:homes.firstObject.homeId];
        }
        if ([impl respondsToSelector:@selector(gotoFamilyManagement)]) {
            [impl gotoFamilyManagement];
        }
    } failure:^(NSError *error) {
        if ([impl respondsToSelector:@selector(gotoFamilyManagement)]) {
            [impl gotoFamilyManagement];
        }
    }];
}

- (void)tapRefresh {
    [self.vm loadUserInfo];
}

- (void)tapNickname {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"修改昵称"
                                                                   message:nil
                                                            preferredStyle:UIAlertControllerStyleAlert];
    [alert addTextFieldWithConfigurationHandler:^(UITextField *tf) {
        tf.placeholder = @"新昵称";
        tf.text = [ThingSmartUser sharedInstance].nickname;
    }];
    UIAlertAction *confirm = [UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
        NSString *nickname = alert.textFields.firstObject.text;
        if (nickname.length > 0) [self.vm updateNickname:nickname];
    }];
    [alert addAction:confirm];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)tapLogout {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"退出登录"
                                                                   message:@"确定退出当前账号？"
                                                            preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"退出" style:UIAlertActionStyleDestructive handler:^(UIAlertAction *action) {
        [self.vm logout];
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)tapCancel {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"注销账号"
                                                                   message:@"注销后一周内数据将被永久删除，期间重新登录可取消。确定注销？"
                                                            preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"注销" style:UIAlertActionStyleDestructive handler:^(UIAlertAction *action) {
        [self.vm cancelAccount];
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)showTip:(NSString *)msg {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:nil message:msg preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)showAlert:(NSString *)title message:(NSString *)msg {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:title message:msg preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

@end
