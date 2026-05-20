import java.util.Properties

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val thingSmartAppKey = localProperties.getProperty("THING_SMART_APPKEY", "")
val thingSmartSecret = localProperties.getProperty("THING_SMART_SECRET", "")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.taojing.androidtest"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.taojing.androidtest"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

        manifestPlaceholders["THING_SMART_APPKEY"] = thingSmartAppKey
        manifestPlaceholders["THING_SMART_SECRET"] = thingSmartSecret
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    lint {
        abortOnError = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf(
                "lib/*/libc++_shared.so",
                "lib/*/libgnustl_shared.so",
                "lib/*/libv8android.so",
                "lib/*/libv8wrapper.so",
                "lib/*/libopenh264.so",
                "lib/*/libyuv.so",
                "lib/*/liblog.so",
                "lib/*/libbytehook.so",
                "lib/*/libxdl.so",
                "lib/*/libsqlcipher.so"
            )
        }
        resources {
            excludes += listOf(
                "AndroidManifest.xml",
                "**/module-info.class",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST"
            )
        }
    }
}

configurations.configureEach {
    exclude(group = "com.thingclips.smart", module = "thingplugin-annotation")
    exclude(group = "com.thingclips.smart", module = "thingsmart-modularCampAnno")
    resolutionStrategy {
        force("com.google.code.findbugs:jsr305:1.3.9")
        force("com.squareup.okhttp3:okhttp-jvm:5.0.0-alpha.11")
        force("com.squareup.okhttp3:okhttp-java-net-cookiejar:5.0.0-alpha.11")
        force("com.squareup.okhttp3:okhttp-urlconnection:5.0.0-alpha.11")
        force("com.squareup.okio:okio-jvm:3.2.0")
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.aar") })
    implementation(files("libs/pip-stub.jar"))

    // View-based UI
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.coil)

    // Existing Compose dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Tuya Smart App SDK
    implementation("com.alibaba:fastjson:1.1.67.android")
    implementation("com.thingclips.smart:thingsmart:7.5.6")

    // Tuya BizBundle (BOM manages versions)
    implementation(enforcedPlatform("com.thingclips.smart:thingsmart-BizBundlesBom:7.5.5"))
    implementation("com.thingclips.smart:thingsmart-bizbundle-panel")
    implementation("com.thingclips.smart:thingsmart-bizbundle-basekit") {
        exclude(group = "com.thingclips.smart", module = "thingplugin-annotation")
    }
    implementation("com.thingclips.smart:thingsmart-bizbundle-homekit")
    implementation("com.thingclips.smart:thingsmart-bizbundle-bizkit")
    implementation("com.thingclips.smart:thingsmart-bizbundle-devicekit")
    implementation("com.thingclips.smart:thingsmart-bizbundle-family")
    implementation("com.thingclips.smart:thingsmart-bizbundle-panelmore")
    implementation("com.thingclips.smart:thingsmart-bizbundle-miniapp")
    implementation("com.gzl.smart.gzlminiapp:miniapp_smart:3.34.0")
    implementation("com.thingclips.smart:thingsmart-bizbundle-mediakit")
    implementation("com.thingclips.smart:thingsmart-ipcsdk:6.11.1")
    implementation("com.thingclips.smart:thingsmart-bizbundle-mapkit")
    implementation("com.thingclips.smart:thingsmart-bizbundle-device_activator")
    implementation("com.thingclips.smart:thingsmart-bizbundle-qrcode_mlkit")
    implementation("com.amap.api:search:7.9.0")
    implementation("com.amap.api:3dmap:8.1.0")
    implementation("com.thingclips.smart:thingsmart-react-native-amap:5.5.0")
    implementation("com.thingclips.smart:thingsmart-bizbundle-scene")
    implementation("com.thingclips.smart:thingsmart-bizbundle-p2pkit")
    implementation("com.thingclips.smart:thingsmart-bizbundle-ipckit")

    // Theme library
    implementation("com.thingclips.smart:thingsmart-theme-open:2.0.6")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
