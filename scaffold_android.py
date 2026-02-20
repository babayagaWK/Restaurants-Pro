import os
import shutil

BASE_DIR = r"c:\Users\Administrator\Desktop\Food POS\android"
APP_DIR = os.path.join(BASE_DIR, "app")
SRC_MAIN = os.path.join(APP_DIR, "src", "main")
JAVA_DIR = os.path.join(SRC_MAIN, "java", "com", "example", "restaurantpos")
RES_DIR = os.path.join(SRC_MAIN, "res")

os.makedirs(JAVA_DIR, exist_ok=True)
os.makedirs(os.path.join(RES_DIR, "values"), exist_ok=True)

with open(os.path.join(BASE_DIR, "settings.gradle.kts"), "w") as f:
    f.write("""
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "KitchenPOS"
include(":app")
""")

with open(os.path.join(BASE_DIR, "build.gradle.kts"), "w") as f:
    f.write("""
buildscript {
    ext {
        set("compose_ui_version", "1.5.1")
    }
}
plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}
""")

with open(os.path.join(BASE_DIR, "gradle.properties"), "w") as f:
    f.write("""
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
""")

with open(os.path.join(APP_DIR, "build.gradle.kts"), "w") as f:
    f.write("""
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.restaurantpos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.restaurantpos"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.7.2")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    
    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
}
""")

with open(os.path.join(SRC_MAIN, "AndroidManifest.xml"), "w") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:roundIcon="@android:drawable/sym_def_app_icon"
        android:supportsRtl="true"
        android:theme="@style/Theme.KitchenPOS"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.KitchenPOS">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""")

with open(os.path.join(RES_DIR, "values", "strings.xml"), "w") as f:
    f.write("""<resources>
    <string name="app_name">Kitchen KDS</string>
</resources>
""")

with open(os.path.join(RES_DIR, "values", "themes.xml"), "w") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.KitchenPOS" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
""")

os.makedirs(os.path.join(RES_DIR, "xml"), exist_ok=True)
with open(os.path.join(RES_DIR, "xml", "backup_rules.xml"), "w") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?><full-backup-content><include domain="sharedpref" path="."/></full-backup-content>""")
with open(os.path.join(RES_DIR, "xml", "data_extraction_rules.xml"), "w") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?><data-extraction-rules><cloud-backup><include domain="sharedpref" path="."/></cloud-backup></data-extraction-rules>""")

with open(os.path.join(JAVA_DIR, "MainActivity.kt"), "w") as f:
    f.write("""package com.example.restaurantpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.restaurantpos.data.api.PosApiService
import com.example.restaurantpos.data.repository.OrderRepository
import com.example.restaurantpos.ui.kitchen.KitchenScreen
import com.example.restaurantpos.ui.kitchen.KitchenViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // TODO: Replace with your actual backend URL URL like "https://yourusername.pythonanywhere.com/"
        val retrofit = Retrofit.Builder()
            .baseUrl("https://kds-pos.com/") // Default dummy URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        val apiService = retrofit.create(PosApiService::class.java)
        val repository = OrderRepository(apiService)
        
        setContent {
            val viewModel: KitchenViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return KitchenViewModel(repository) as T
                    }
                }
            )
            KitchenScreen(viewModel = viewModel)
        }
    }
}
""")

def move_if_exists(src_rel, tgt_rel):
    src = os.path.join(BASE_DIR, src_rel)
    tgt = os.path.join(JAVA_DIR, tgt_rel)
    os.makedirs(os.path.dirname(tgt), exist_ok=True)
    if os.path.exists(src):
        shutil.copy(src, tgt)
        os.remove(src)

move_if_exists(r"data\api\PosApiService.kt", r"data\api\PosApiService.kt")
move_if_exists(r"data\model\Models.kt", r"data\model\Models.kt")
move_if_exists(r"data\repository\OrderRepository.kt", r"data\repository\OrderRepository.kt")
move_if_exists(r"ui\kitchen\KitchenViewModel.kt", r"ui\kitchen\KitchenViewModel.kt")
move_if_exists(r"ui\kitchen\KitchenScreen.kt", r"ui\kitchen\KitchenScreen.kt")

try:
    shutil.rmtree(os.path.join(BASE_DIR, "data"))
    shutil.rmtree(os.path.join(BASE_DIR, "ui"))
except:
    pass

print("Android project structured successfully!")
