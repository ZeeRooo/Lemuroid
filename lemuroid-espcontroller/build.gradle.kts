plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

dependencies {
    implementation(deps.libs.rxJava2)
    implementation(deps.libs.rxAndroid2)
    implementation(deps.libs.libretrodroid)
}
