# Summy Enchantment 1.21.1 构建与发布

## 准备

- 使用 Java 21，分别在 `fabric/` 与 `forge/` 使用各自的 Gradle Wrapper。Fabric 的 Loom、Yarn、Loader 和 Fabric API 版本由 `gradle.properties` 固定。
- 默认通过 Maven 仓库解析工具链和常规依赖。已有旧版离线缓存时，可显式传入 `--offline -PseLocalCache`，复用工程的 `.gradle/se-local-maven`，以及 Fabric 的 `.gradle/se-local` 和 `.gradle/se-fabric-api-nested`。离线环境还需已缓存的 Gradle 和 Minecraft 依赖。
- 将对应版本的官方兼容 JAR 放入独立目录，用绝对路径传 `-PcompatJarsDir`。Fabric 需要 `spear-backport-fabric-1.8.0-1.21.1.jar`。NeoForge 需要 `spear-backport-neoforge-1.8.0-1.21.1.jar`、`kubejs-neoforge-2101.7.2-build.381.jar` 和 `rhino-2101.2.7-build.81.jar`。构建会校验文件的 SHA-256。

## 构建与自检

在对应工程执行 `.\gradlew.bat clean build "-PcompatJarsDir=<绝对路径>"` 生成完整候选。开发自检使用单独的实例目录，在其 `saves/SeDevCheckWorld` 放置专用世界副本，并用 `-PseTestRunDir=<绝对路径>` 指向该实例。测试用第三方模组按场景放入实例 `mods/`，运行后核对真实加载列表。

- 核心自检：两端运行 `runClient -PcoreDevcheck -PdevcheckMode=core`。
- Spears 专项：两端运行 `runClient -PspearDevcheck`。Gradle 从兼容输入目录加载 Spears。
- NeoForge KubeJS 专项：把 `tools/kubejs-selftest/` 的内容复制到测试实例 `kubejs/`，运行 `verifyKubeJsSelfTest -PkubejsDevcheck`。
- NeoForge 神化专项：使用完整开发构建，按场景准备测试模组，运行 `runClient -Pdevcheck -PdevcheckMode=anvilcompat`。
- 服务端：两端运行 `runServer`，使用独立实例并确认到达 `Done`。对于正式包，另用生产加载器实例验证。

客户端必须出现本轮 `[SE-DEVCHECK]` 汇总，失败数为 0。第三方联动还要确认该模组确实加载并执行命中、扣费等实际入口。核对注入错误、混入冲突和服务端日志。

## 校验与归档

从源码仓库根目录显式指定本轮完整候选：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-release.ps1 `
  -FabricJar 'fabric\build\libs\summy-enchantment-1.4.3-fix+1.21.1.jar' `
  -ForgeJar 'forge\build\libs\summy-enchantment-1.4.3-fix+1.21.1-neoforge.jar'
```

脚本检查版本、图标、许可证、平台兼容入口及包内隔离并输出 SHA-256。核验通过后将两份候选按加载器分别放进统一 `JAR/1.21.1/` 的发布目录，材料放入各自的 `_release-<版本>/`。同名异哈希的旧包先进入冲突归档。发布说明和测试记录使用实际版本与矩阵。两份候选完成审阅后可共用标签 `v1.4.3-fix+1.21.1`。
