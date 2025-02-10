import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.instrumentationPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix$instrumentationPrefix${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.customtracking"
}

dependencies {
    api(Dependencies.Otel.api)
    implementation(project(":integration:agent:api"))
    implementation(project(":common:otel"))
    implementation(Dependencies.SessionReplay.commonLogger)
}