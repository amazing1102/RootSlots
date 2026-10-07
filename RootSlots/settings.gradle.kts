// CI(GitHub Actions 等海外环境):官方源优先——阿里云镜像对海外网络不稳定,
// 且部分插件(如 KSP 2.0.21-1.0.25)镜像未收录,曾导致 CI 解析失败;
// 国内本地则相反:镜像优先以加速,官方源兜底,行为与此前保持一致。
pluginManagement {
    val onCi = System.getenv("CI") == "true"
    repositories {
        if (onCi) {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        if (!onCi) {
            google {
                content {
                    includeGroupByRegex("com\\.android.*")
                    includeGroupByRegex("com\\.google.*")
                    includeGroupByRegex("androidx.*")
                }
            }
            mavenCentral()
            gradlePluginPortal()
        }
    }
}
dependencyResolutionManagement {
    val onCi = System.getenv("CI") == "true"
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (onCi) {
            google()
            mavenCentral()
        }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        if (!onCi) {
            google()
            mavenCentral()
        }
    }
}
rootProject.name = "RootSlots"
include(":app")
