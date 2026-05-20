#import "HomeViewController.h"
#import "CurrentHomeManager.h"
#import <ThingSmartHomeKit/ThingSmartKit.h>
#import <ThingSmartBizCore/ThingSmartBizCore.h>
#import <ThingModuleServices/ThingSmartHomeDataProtocol.h>
#import <ThingModuleServices/ThingActivatorProtocol.h>
#import <ThingModuleServices/ThingFamilyProtocol.h>
#import <ThingModuleServices/ThingPanelProtocol.h>

static NSString * const kDeviceCellId = @"DeviceListCell";

// ---------------------------------------------------------------------------
#pragma mark - DeviceListCell
// ---------------------------------------------------------------------------

@interface DeviceListCell : UITableViewCell
- (void)configureWithDevice:(ThingSmartDeviceModel *)device;
@end

@implementation DeviceListCell {
    UILabel *_nameLabel;
    UILabel *_badgeLabel;
}

- (instancetype)initWithStyle:(UITableViewCellStyle)style
              reuseIdentifier:(NSString *)reuseIdentifier {
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (!self) return nil;

    _nameLabel = [[UILabel alloc] init];
    _nameLabel.font = [UIFont systemFontOfSize:16];
    _nameLabel.translatesAutoresizingMaskIntoConstraints = NO;
    [self.contentView addSubview:_nameLabel];

    _badgeLabel = [[UILabel alloc] init];
    _badgeLabel.text = @"离线";
    _badgeLabel.font = [UIFont systemFontOfSize:12];
    _badgeLabel.textColor = UIColor.secondaryLabelColor;
    _badgeLabel.translatesAutoresizingMaskIntoConstraints = NO;
    [self.contentView addSubview:_badgeLabel];

    [NSLayoutConstraint activateConstraints:@[
        [_nameLabel.leadingAnchor constraintEqualToAnchor:self.contentView.leadingAnchor constant:16],
        [_nameLabel.centerYAnchor constraintEqualToAnchor:self.contentView.centerYAnchor],
        [_nameLabel.trailingAnchor constraintLessThanOrEqualToAnchor:_badgeLabel.leadingAnchor constant:-8],
        [_badgeLabel.trailingAnchor constraintEqualToAnchor:self.contentView.trailingAnchor constant:-16],
        [_badgeLabel.centerYAnchor constraintEqualToAnchor:self.contentView.centerYAnchor],
    ]];
    return self;
}

- (void)configureWithDevice:(ThingSmartDeviceModel *)device {
    _nameLabel.text = device.name;
    BOOL online = device.isOnline;
    _badgeLabel.hidden = online;
    _nameLabel.textColor = online ? UIColor.labelColor : UIColor.tertiaryLabelColor;
}

@end

// ---------------------------------------------------------------------------
#pragma mark - HomeViewController
// ---------------------------------------------------------------------------

@interface HomeViewController () <UITableViewDataSource,
                                  UITableViewDelegate,
                                  ThingSmartHomeDelegate,
                                  ThingSmartHomeDataProtocol>
@property (nonatomic, strong) UITableView   *tableView;
@property (nonatomic, strong) UIView        *emptyView;
@property (nonatomic, strong) UILabel       *emptyLabel;
@property (nonatomic, strong) UIButton      *createHomeBtn;
@property (nonatomic, strong) NSMutableArray<ThingSmartDeviceModel *> *myDevices;
@property (nonatomic, strong) NSMutableArray<ThingSmartDeviceModel *> *sharedDevices;
@property (nonatomic, strong) ThingSmartHome *currentHome;
@end

@implementation HomeViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = UIColor.systemBackgroundColor;
    self.myDevices     = [NSMutableArray array];
    self.sharedDevices = [NSMutableArray array];

    [[ThingSmartBizCore sharedInstance] registerService:@protocol(ThingSmartHomeDataProtocol)
                                           withInstance:self];

    [self buildNavBar];
    [self buildTableView];
    [self buildEmptyView];
    [self buildLongPressGesture];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onHomeChanged:)
                                                 name:CurrentHomeDidChangeNotification
                                               object:nil];
    [self onHomeChanged:nil];
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    self.currentHome.delegate = nil;
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    UINavigationBarAppearance *ap = [[UINavigationBarAppearance alloc] init];
    [ap configureWithOpaqueBackground];
    self.navigationController.navigationBar.standardAppearance   = ap;
    self.navigationController.navigationBar.scrollEdgeAppearance = ap;
    self.navigationController.navigationBar.tintColor =
        [UIColor colorWithRed:22.0/255.0 green:119.0/255.0 blue:255.0/255.0 alpha:1.0];
}

#pragma mark - Build UI

- (void)buildNavBar {
    UIBarButtonItem *addBtn =
        [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAdd
                                                      target:self
                                                      action:@selector(tapAddDevice)];
    UIBarButtonItem *switchBtn =
        [[UIBarButtonItem alloc] initWithTitle:@"切换"
                                         style:UIBarButtonItemStylePlain
                                        target:self
                                        action:@selector(tapSwitch)];
    self.navigationItem.rightBarButtonItems = @[addBtn, switchBtn];
}

- (void)buildTableView {
    self.tableView = [[UITableView alloc] initWithFrame:CGRectZero style:UITableViewStylePlain];
    self.tableView.dataSource = self;
    self.tableView.delegate   = self;
    self.tableView.rowHeight  = 60;
    self.tableView.hidden     = YES;
    self.tableView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.tableView registerClass:[DeviceListCell class]
           forCellReuseIdentifier:kDeviceCellId];
    [self.view addSubview:self.tableView];
    [NSLayoutConstraint activateConstraints:@[
        [self.tableView.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor],
        [self.tableView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.tableView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.tableView.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
    ]];

    UIRefreshControl *refresh = [[UIRefreshControl alloc] init];
    [refresh addTarget:self action:@selector(pullToRefresh) forControlEvents:UIControlEventValueChanged];
    self.tableView.refreshControl = refresh;
}

- (void)buildEmptyView {
    self.emptyView = [[UIView alloc] init];
    self.emptyView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.emptyView];
    [NSLayoutConstraint activateConstraints:@[
        [self.emptyView.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor],
        [self.emptyView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.emptyView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.emptyView.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
    ]];

    self.emptyLabel = [[UILabel alloc] init];
    self.emptyLabel.font          = [UIFont systemFontOfSize:17 weight:UIFontWeightMedium];
    self.emptyLabel.textColor     = UIColor.secondaryLabelColor;
    self.emptyLabel.textAlignment = NSTextAlignmentCenter;
    self.emptyLabel.translatesAutoresizingMaskIntoConstraints = NO;
    [self.emptyView addSubview:self.emptyLabel];

    self.createHomeBtn = [UIButton buttonWithType:UIButtonTypeSystem];
    [self.createHomeBtn setTitle:@"创建家庭" forState:UIControlStateNormal];
    self.createHomeBtn.backgroundColor    = [UIColor colorWithRed:22.0/255.0 green:119.0/255.0 blue:255.0/255.0 alpha:1.0];
    [self.createHomeBtn setTitleColor:UIColor.whiteColor forState:UIControlStateNormal];
    self.createHomeBtn.titleLabel.font    = [UIFont systemFontOfSize:16 weight:UIFontWeightMedium];
    self.createHomeBtn.layer.cornerRadius = 10;
    self.createHomeBtn.translatesAutoresizingMaskIntoConstraints = NO;
    [self.createHomeBtn addTarget:self action:@selector(tapCreateHome) forControlEvents:UIControlEventTouchUpInside];
    [self.emptyView addSubview:self.createHomeBtn];

    [NSLayoutConstraint activateConstraints:@[
        [self.emptyLabel.centerXAnchor constraintEqualToAnchor:self.emptyView.centerXAnchor],
        [self.emptyLabel.centerYAnchor constraintEqualToAnchor:self.emptyView.centerYAnchor constant:-40],
        [self.createHomeBtn.centerXAnchor constraintEqualToAnchor:self.emptyView.centerXAnchor],
        [self.createHomeBtn.topAnchor constraintEqualToAnchor:self.emptyLabel.bottomAnchor constant:24],
        [self.createHomeBtn.widthAnchor constraintEqualToConstant:160],
        [self.createHomeBtn.heightAnchor constraintEqualToConstant:48],
    ]];
}

- (void)buildLongPressGesture {
    UILongPressGestureRecognizer *lp =
        [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(handleLongPress:)];
    [self.tableView addGestureRecognizer:lp];
}

#pragma mark - Notifications

- (void)onHomeChanged:(NSNotification *)note {
    CurrentHomeManager *mgr = [CurrentHomeManager sharedInstance];
    long long homeId = mgr.currentHomeId;

    self.title = (homeId == 0) ? @"首页" : mgr.currentHomeName;
    for (UIBarButtonItem *item in self.navigationItem.rightBarButtonItems) {
        item.enabled = (homeId != 0);
    }

    if (homeId == 0) {
        self.currentHome.delegate = nil;
        self.currentHome = nil;
        [self.myDevices removeAllObjects];
        [self.sharedDevices removeAllObjects];
        [self showEmptyState:@"暂无家庭" showCreateBtn:YES];
    } else {
        [self loadDeviceListWithHomeId:homeId];
    }
}

#pragma mark - Data Loading

- (void)loadDeviceListWithHomeId:(long long)homeId {
    self.currentHome.delegate = nil;

    ThingSmartHome *home = [ThingSmartHome homeWithHomeId:homeId];
    home.delegate    = self;
    self.currentHome = home;

    [home getHomeDataWithSuccess:^(ThingSmartHomeModel *homeModel) {
        [self separateDevices:home.deviceList];
        [self.tableView reloadData];
        [self updateViewState];
        [self.tableView.refreshControl endRefreshing];
    } failure:^(NSError *error) {
        [self.tableView.refreshControl endRefreshing];
        NSLog(@"[FLY] load home data error: %@", error);
    }];
}

- (void)separateDevices:(NSArray<ThingSmartDeviceModel *> *)devices {
    NSMutableArray *mine   = [NSMutableArray array];
    NSMutableArray *shared = [NSMutableArray array];
    for (ThingSmartDeviceModel *d in devices) {
        if (d.isShare) [shared addObject:d];
        else           [mine   addObject:d];
    }
    self.myDevices     = mine;
    self.sharedDevices = shared;
}

- (void)updateViewState {
    BOOL noDevices = (self.myDevices.count == 0 && self.sharedDevices.count == 0);
    if (noDevices) {
        [self showEmptyState:@"暂无设备" showCreateBtn:NO];
    } else {
        self.emptyView.hidden = YES;
        self.tableView.hidden = NO;
    }
}

- (void)showEmptyState:(NSString *)message showCreateBtn:(BOOL)showBtn {
    self.emptyLabel.text      = message;
    self.createHomeBtn.hidden = !showBtn;
    self.emptyView.hidden     = NO;
    self.tableView.hidden     = YES;
}

- (void)pullToRefresh {
    long long homeId = [CurrentHomeManager sharedInstance].currentHomeId;
    if (homeId == 0) { [self.tableView.refreshControl endRefreshing]; return; }
    [self loadDeviceListWithHomeId:homeId];
}

#pragma mark - UITableViewDataSource

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView { return 2; }

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return section == 0 ? (NSInteger)self.myDevices.count : (NSInteger)self.sharedDevices.count;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    if (section == 0 && self.myDevices.count > 0)     return @"我的设备";
    if (section == 1 && self.sharedDevices.count > 0) return @"共享设备";
    return nil;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DeviceListCell *cell = [tableView dequeueReusableCellWithIdentifier:kDeviceCellId forIndexPath:indexPath];
    [cell configureWithDevice:[self deviceAtIndexPath:indexPath]];
    return cell;
}

#pragma mark - UITableViewDelegate — tap

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    [self openPanelForDevice:[self deviceAtIndexPath:indexPath]];
}

- (void)openPanelForDevice:(ThingSmartDeviceModel *)device {
    long long homeId = [CurrentHomeManager sharedInstance].currentHomeId;

    id<ThingFamilyProtocol> familyImpl =
        [[ThingSmartBizCore sharedInstance] serviceOfProtocol:@protocol(ThingFamilyProtocol)];
    if ([familyImpl respondsToSelector:@selector(updateCurrentFamilyId:)]) {
        [familyImpl updateCurrentFamilyId:homeId];
    }

    id<ThingPanelProtocol> panelImpl =
        [[ThingSmartBizCore sharedInstance] serviceOfProtocol:@protocol(ThingPanelProtocol)];
    if (!panelImpl) {
        NSLog(@"[FLY] ThingPanelProtocol service not found — check ThingSmartPanelBizBundle");
        return;
    }

    if ([panelImpl respondsToSelector:@selector(gotoPanelViewControllerWithDevice:group:initialProps:contextProps:completion:)]) {
        [panelImpl gotoPanelViewControllerWithDevice:device
                                              group:nil
                                       initialProps:nil
                                       contextProps:nil
                                         completion:^(NSError * _Nullable error) {
            if (error) NSLog(@"[FLY] panel error: %@", error);
        }];
        return;
    }

    if ([panelImpl respondsToSelector:@selector(getPanelViewControllerWithDeviceModel:groupModel:initialProps:contextProps:completionHandler:)]) {
        [panelImpl getPanelViewControllerWithDeviceModel:device
                                             groupModel:nil
                                           initialProps:nil
                                           contextProps:nil
                                      completionHandler:^(UIViewController *vc, NSError *error) {
            if (error || !vc) { NSLog(@"[FLY] panel load error: %@", error); return; }
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.navigationController pushViewController:vc animated:YES];
            });
        }];
    }
}

#pragma mark - UITableViewDelegate — swipe

- (UISwipeActionsConfiguration *)tableView:(UITableView *)tableView
trailingSwipeActionsConfigurationForRowAtIndexPath:(NSIndexPath *)indexPath {
    ThingSmartDeviceModel *device = [self deviceAtIndexPath:indexPath];

    if (indexPath.section == 0) {
        UIContextualAction *del = [UIContextualAction
            contextualActionWithStyle:UIContextualActionStyleDestructive
                                title:@"删除"
                              handler:^(UIContextualAction *a, UIView *v, void (^done)(BOOL)) {
            [self confirmRemoveDevice:device completion:done];
        }];
        return [UISwipeActionsConfiguration configurationWithActions:@[del]];
    } else {
        UIContextualAction *cancel = [UIContextualAction
            contextualActionWithStyle:UIContextualActionStyleDestructive
                                title:@"取消共享"
                              handler:^(UIContextualAction *a, UIView *v, void (^done)(BOOL)) {
            [self confirmCancelShare:device completion:done];
        }];
        cancel.backgroundColor = UIColor.systemOrangeColor;
        return [UISwipeActionsConfiguration configurationWithActions:@[cancel]];
    }
}

#pragma mark - Long Press — rename

- (void)handleLongPress:(UILongPressGestureRecognizer *)gesture {
    if (gesture.state != UIGestureRecognizerStateBegan) return;
    CGPoint point = [gesture locationInView:self.tableView];
    NSIndexPath *ip = [self.tableView indexPathForRowAtPoint:point];
    if (!ip) return;

    ThingSmartDeviceModel *device = [self deviceAtIndexPath:ip];
    if (device.isShare) return;

    UIAlertController *alert =
        [UIAlertController alertControllerWithTitle:@"修改设备名称"
                                            message:nil
                                     preferredStyle:UIAlertControllerStyleAlert];
    [alert addTextFieldWithConfigurationHandler:^(UITextField *tf) {
        tf.text = device.name;
        tf.clearButtonMode = UITextFieldViewModeWhileEditing;
    }];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil]];
    [alert addAction:[UIAlertAction actionWithTitle:@"确定"
                                             style:UIAlertActionStyleDefault
                                           handler:^(UIAlertAction *a) {
        NSString *newName = alert.textFields.firstObject.text;
        if (newName.length == 0) return;
        [[ThingSmartDevice deviceWithDeviceId:device.devId]
            updateName:newName
               success:^{
            device.name = newName;
            [self.tableView reloadData];
        } failure:^(NSError *error) {
            NSLog(@"[FLY] rename error: %@", error);
        }];
    }]];
    [self presentViewController:alert animated:YES completion:nil];
}

#pragma mark - Device Operations

- (void)confirmRemoveDevice:(ThingSmartDeviceModel *)device completion:(void (^)(BOOL))completion {
    NSString *msg = [NSString stringWithFormat:@"确定移除「%@」？", device.name];
    UIAlertController *alert =
        [UIAlertController alertControllerWithTitle:@"移除设备" message:msg preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:^(UIAlertAction *a) { completion(NO); }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"移除"
                                             style:UIAlertActionStyleDestructive
                                           handler:^(UIAlertAction *a) {
        [[ThingSmartDevice deviceWithDeviceId:device.devId]
            remove:^{ completion(YES); }
           failure:^(NSError *error) {
            completion(NO);
            NSLog(@"[FLY] remove device error: %@", error);
        }];
    }]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)confirmCancelShare:(ThingSmartDeviceModel *)device completion:(void (^)(BOOL))completion {
    NSString *msg = [NSString stringWithFormat:@"确定取消「%@」的共享？", device.name];
    UIAlertController *alert =
        [UIAlertController alertControllerWithTitle:@"取消共享" message:msg preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:^(UIAlertAction *a) { completion(NO); }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"确定"
                                             style:UIAlertActionStyleDestructive
                                           handler:^(UIAlertAction *a) {
        [[ThingSmartHomeDeviceShare new]
            removeReceiveDeviceShareWithDevId:device.devId
                                      success:^{ completion(YES); }
                                      failure:^(NSError *error) {
            completion(NO);
            NSLog(@"[FLY] cancel share error: %@", error);
        }];
    }]];
    [self presentViewController:alert animated:YES completion:nil];
}

#pragma mark - ThingSmartHomeDelegate

- (void)home:(ThingSmartHome *)home didAddDeivice:(ThingSmartDeviceModel *)device {
    if (device.isShare) [self.sharedDevices addObject:device];
    else                [self.myDevices     addObject:device];
    [self.tableView reloadData];
    [self updateViewState];
}

- (void)home:(ThingSmartHome *)home didRemoveDeivice:(NSString *)devId {
    [self.myDevices filterUsingPredicate:[NSPredicate predicateWithFormat:@"devId != %@", devId]];
    [self.sharedDevices filterUsingPredicate:[NSPredicate predicateWithFormat:@"devId != %@", devId]];
    [self.tableView reloadData];
    [self updateViewState];
}

- (void)home:(ThingSmartHome *)home deviceInfoUpdate:(ThingSmartDeviceModel *)device {
    NSIndexPath *ip = [self indexPathForDevId:device.devId];
    if (ip) [self.tableView reloadRowsAtIndexPaths:@[ip] withRowAnimation:UITableViewRowAnimationNone];
}

- (void)home:(ThingSmartHome *)home device:(ThingSmartDeviceModel *)device dpsUpdate:(NSDictionary *)dps {
    NSIndexPath *ip = [self indexPathForDevId:device.devId];
    if (ip) [self.tableView reloadRowsAtIndexPaths:@[ip] withRowAnimation:UITableViewRowAnimationNone];
}

- (void)homeDidUpdateSharedInfo:(ThingSmartHome *)home {
    long long homeId = [CurrentHomeManager sharedInstance].currentHomeId;
    if (homeId > 0) [self loadDeviceListWithHomeId:homeId];
}

#pragma mark - ThingSmartHomeDataProtocol

- (ThingSmartHome *)getCurrentHome {
    long long homeId = [CurrentHomeManager sharedInstance].currentHomeId;
    return [ThingSmartHome homeWithHomeId:homeId];
}

#pragma mark - Add Device

- (void)tapAddDevice {
    id<ThingActivatorProtocol> impl =
        [[ThingSmartBizCore sharedInstance] serviceOfProtocol:@protocol(ThingActivatorProtocol)];
    if (!impl) { NSLog(@"[FLY] ThingActivatorProtocol not found"); return; }

    [impl activatorCompletion:ThingActivatorCompletionNodeNormal
                   customJump:NO
               completionBlock:^(NSArray * _Nullable deviceList) {
        NSLog(@"[FLY] activator completion, new devices: %@", deviceList);
        long long homeId = [CurrentHomeManager sharedInstance].currentHomeId;
        if (homeId > 0) [self loadDeviceListWithHomeId:homeId];
    }];

    if ([impl respondsToSelector:@selector(gotoCategoryViewController)]) {
        [impl gotoCategoryViewController];
    }
}

#pragma mark - Switch Home

- (void)tapSwitch {
    [[ThingSmartHomeManager new] getHomeListWithSuccess:^(NSArray<ThingSmartHomeModel *> *homes) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (homes.count == 0) { [self tapCreateHome]; return; }

            UIAlertController *sheet =
                [UIAlertController alertControllerWithTitle:@"选择家庭" message:nil preferredStyle:UIAlertControllerStyleActionSheet];
            long long currentId = [CurrentHomeManager sharedInstance].currentHomeId;
            for (ThingSmartHomeModel *home in homes) {
                NSString *title = (home.homeId == currentId)
                    ? [NSString stringWithFormat:@"✓ %@", home.name]
                    : home.name;
                [sheet addAction:[UIAlertAction actionWithTitle:title
                                                          style:UIAlertActionStyleDefault
                                                        handler:^(UIAlertAction *a) {
                    [[CurrentHomeManager sharedInstance] switchHomeWithHomeId:home.homeId];
                }]];
            }
            [sheet addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil]];
            [self presentViewController:sheet animated:YES completion:nil];
        });
    } failure:^(NSError *error) {}];
}

#pragma mark - Create Home

- (void)tapCreateHome {
    UIAlertController *alert =
        [UIAlertController alertControllerWithTitle:@"创建家庭" message:@"请输入家庭名称" preferredStyle:UIAlertControllerStyleAlert];
    [alert addTextFieldWithConfigurationHandler:^(UITextField *tf) {
        tf.placeholder = @"家庭名称（如：我的家）";
        tf.text        = @"我的家";
    }];
    [alert addAction:[UIAlertAction actionWithTitle:@"创建"
                                             style:UIAlertActionStyleDefault
                                           handler:^(UIAlertAction *a) {
        NSString *name = alert.textFields.firstObject.text;
        if (name.length == 0) name = @"我的家";
        [self doCreateHomeWithName:name];
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)doCreateHomeWithName:(NSString *)name {
    [[ThingSmartHomeManager new] addHomeWithName:name
                                        geoName:@""
                                          rooms:@[]
                                       latitude:0
                                      longitude:0
                                        success:^(long long homeId) {
        [[CurrentHomeManager sharedInstance] switchHomeWithHomeId:homeId];
    } failure:^(NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            UIAlertController *err =
                [UIAlertController alertControllerWithTitle:@"创建失败" message:error.localizedDescription preferredStyle:UIAlertControllerStyleAlert];
            [err addAction:[UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:nil]];
            [self presentViewController:err animated:YES completion:nil];
        });
    }];
}

#pragma mark - Helpers

- (ThingSmartDeviceModel *)deviceAtIndexPath:(NSIndexPath *)indexPath {
    return indexPath.section == 0 ? self.myDevices[indexPath.row] : self.sharedDevices[indexPath.row];
}

- (NSIndexPath *)indexPathForDevId:(NSString *)devId {
    for (NSInteger i = 0; i < (NSInteger)self.myDevices.count; i++) {
        if ([self.myDevices[i].devId isEqualToString:devId])
            return [NSIndexPath indexPathForRow:i inSection:0];
    }
    for (NSInteger i = 0; i < (NSInteger)self.sharedDevices.count; i++) {
        if ([self.sharedDevices[i].devId isEqualToString:devId])
            return [NSIndexPath indexPathForRow:i inSection:1];
    }
    return nil;
}

@end
