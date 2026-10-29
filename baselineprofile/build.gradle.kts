plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.androidx.baselineprofile)
}

android {
  namespace = "com.opensplit.baselineprofile"
  compileSdk = 37

  defaultConfig {
    minSdk = 28
    targetSdk = 36
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  targetProjectPath = ":app"
}

baselineProfile {
  useConnectedDevices = true
}

dependencies {
  implementation(libs.androidx.junit)
  implementation(libs.androidx.benchmark.macro.junit4)
  implementation(libs.androidx.uiautomator)
}
