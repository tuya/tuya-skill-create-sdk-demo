#import "EmailAuthViewController.h"
#import "EmailAuthViewModel.h"

static NSString * const kEmailCC = @"86";

@interface EmailAuthViewController ()
@property (nonatomic, strong) EmailAuthViewModel *vm;
@property (nonatomic, strong) UISegmentedControl *seg;
@property (nonatomic, strong) UITextField *emailField;
@property (nonatomic, strong) UIView *codeRow;
@property (nonatomic, strong) UITextField *codeField;
@property (nonatomic, strong) UITextField *passwordField;
@property (nonatomic, strong) UIButton *submitBtn;
@property (nonatomic, strong) UIActivityIndicatorView *spinner;
@end

@implementation EmailAuthViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.title = @"邮箱";
    self.view.backgroundColor = UIColor.systemBackgroundColor;
    self.vm = [[EmailAuthViewModel alloc] init];
    [self bindViewModel];
    [self buildUI];
    [self applyMode:EmailAuthModePasswordLogin];
}

- (void)bindViewModel {
    __weak typeof(self) w = self;
    self.vm.onLoading = ^(BOOL loading) {
        dispatch_async(dispatch_get_main_queue(), ^{
            loading ? [w.spinner startAnimating] : [w.spinner stopAnimating];
            w.submitBtn.enabled = !loading;
        });
    };
    self.vm.onCodeSent = ^{
        dispatch_async(dispatch_get_main_queue(), ^{
            [w showTip:@"验证码已发送，请查收"];
        });
    };
    self.vm.onAuthSuccess = ^(EmailAuthMode mode) {
        if (mode == EmailAuthModeResetPassword) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [w showTip:@"密码已重置，请重新登录"];
                [w.navigationController popViewControllerAnimated:YES];
            });
        }
    };
    self.vm.onError = ^(NSString *msg) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [w showAlert:@"错误" message:msg];
        });
    };
}

- (void)buildUI {
    self.seg = [[UISegmentedControl alloc] initWithItems:@[@"密码登录", @"验证码登录", @"注册", @"重置密码"]];
    self.seg.selectedSegmentIndex = 0;
    [self.seg addTarget:self action:@selector(segChanged) forControlEvents:UIControlEventValueChanged];

    self.emailField = [self field:@"邮箱地址" keyboard:UIKeyboardTypeEmailAddress secure:NO];

    self.codeField = [self field:@"验证码" keyboard:UIKeyboardTypeNumberPad secure:NO];
    UIButton *sendBtn = [UIButton buttonWithType:UIButtonTypeSystem];
    [sendBtn setTitle:@"发送验证码" forState:UIControlStateNormal];
    sendBtn.titleLabel.font = [UIFont systemFontOfSize:14];
    [sendBtn setContentHuggingPriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    [sendBtn addTarget:self action:@selector(tapSend) forControlEvents:UIControlEventTouchUpInside];
    UIStackView *codeStack = [[UIStackView alloc] initWithArrangedSubviews:@[self.codeField, sendBtn]];
    codeStack.axis = UILayoutConstraintAxisHorizontal;
    codeStack.spacing = 8;
    self.codeRow = codeStack;

    self.passwordField = [self field:@"密码" keyboard:UIKeyboardTypeDefault secure:YES];

    self.submitBtn = [UIButton buttonWithType:UIButtonTypeSystem];
    self.submitBtn.translatesAutoresizingMaskIntoConstraints = NO;
    self.submitBtn.backgroundColor = UIColor.systemBlueColor;
    [self.submitBtn setTitleColor:UIColor.whiteColor forState:UIControlStateNormal];
    self.submitBtn.titleLabel.font = [UIFont systemFontOfSize:17 weight:UIFontWeightSemibold];
    self.submitBtn.layer.cornerRadius = 10;
    [self.submitBtn addTarget:self action:@selector(tapSubmit) forControlEvents:UIControlEventTouchUpInside];
    [self.submitBtn.heightAnchor constraintEqualToConstant:50].active = YES;

    self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleMedium];
    self.spinner.hidesWhenStopped = YES;

    UIStackView *form = [[UIStackView alloc] initWithArrangedSubviews:@[
        self.seg, self.emailField, self.codeRow, self.passwordField, self.submitBtn, self.spinner
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

- (UITextField *)field:(NSString *)placeholder keyboard:(UIKeyboardType)type secure:(BOOL)secure {
    UITextField *tf = [[UITextField alloc] init];
    tf.placeholder = placeholder;
    tf.keyboardType = type;
    tf.secureTextEntry = secure;
    tf.borderStyle = UITextBorderStyleRoundedRect;
    tf.autocapitalizationType = UITextAutocapitalizationTypeNone;
    tf.autocorrectionType = UITextAutocorrectionTypeNo;
    [tf.heightAnchor constraintEqualToConstant:44].active = YES;
    return tf;
}

- (void)applyMode:(EmailAuthMode)mode {
    switch (mode) {
        case EmailAuthModePasswordLogin:
            self.codeRow.hidden = YES;
            self.passwordField.hidden = NO;
            self.passwordField.placeholder = @"密码";
            [self.submitBtn setTitle:@"登录" forState:UIControlStateNormal];
            break;
        case EmailAuthModeCodeLogin:
            self.codeRow.hidden = NO;
            self.passwordField.hidden = YES;
            [self.submitBtn setTitle:@"登录" forState:UIControlStateNormal];
            break;
        case EmailAuthModeRegister:
            self.codeRow.hidden = NO;
            self.passwordField.hidden = NO;
            self.passwordField.placeholder = @"密码（至少 6 位）";
            [self.submitBtn setTitle:@"注册" forState:UIControlStateNormal];
            break;
        case EmailAuthModeResetPassword:
            self.codeRow.hidden = NO;
            self.passwordField.hidden = NO;
            self.passwordField.placeholder = @"新密码";
            [self.submitBtn setTitle:@"重置密码" forState:UIControlStateNormal];
            break;
    }
}

- (void)segChanged {
    [self applyMode:(EmailAuthMode)self.seg.selectedSegmentIndex];
}

- (void)tapSend {
    NSString *email = self.emailField.text;
    if (email.length == 0) { [self showAlert:@"提示" message:@"请输入邮箱地址"]; return; }
    EmailAuthMode mode = (EmailAuthMode)self.seg.selectedSegmentIndex;
    NSInteger type = (mode == EmailAuthModeRegister) ? 1 : (mode == EmailAuthModeCodeLogin) ? 2 : 3;
    [self.vm sendCodeToEmail:email countryCode:kEmailCC type:type];
}

- (void)tapSubmit {
    NSString *email = self.emailField.text;
    NSString *pwd   = self.passwordField.text;
    NSString *code  = self.codeField.text;
    if (email.length == 0) { [self showAlert:@"提示" message:@"请输入邮箱地址"]; return; }
    EmailAuthMode mode = (EmailAuthMode)self.seg.selectedSegmentIndex;
    switch (mode) {
        case EmailAuthModePasswordLogin:
            if (pwd.length == 0) { [self showAlert:@"提示" message:@"请输入密码"]; return; }
            [self.vm loginByPassword:email countryCode:kEmailCC password:pwd];
            break;
        case EmailAuthModeCodeLogin:
            if (code.length == 0) { [self showAlert:@"提示" message:@"请输入验证码"]; return; }
            [self.vm loginByCode:email countryCode:kEmailCC code:code];
            break;
        case EmailAuthModeRegister:
            if (code.length == 0 || pwd.length == 0) { [self showAlert:@"提示" message:@"请填写验证码和密码"]; return; }
            [self.vm registerEmail:email countryCode:kEmailCC password:pwd code:code];
            break;
        case EmailAuthModeResetPassword:
            if (code.length == 0 || pwd.length == 0) { [self showAlert:@"提示" message:@"请填写验证码和新密码"]; return; }
            [self.vm resetPassword:email countryCode:kEmailCC newPassword:pwd code:code];
            break;
    }
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
