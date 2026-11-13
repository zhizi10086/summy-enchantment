# Summy Enchantment

[更新日志](CHANGELOG.md) · [发布流程](docs/release-process.md) · [1.4.2+1.21.1 Release 说明](docs/releases/1.4.2+1.21.1.md) · [许可证](LICENSE) · [第三方构建依赖](THIRD_PARTY_NOTICES.md)

Minecraft **1.21.1** 的附魔与机制改造模组：重做锋利与力量、铁砧改为消耗经验点数，
新增 32 个附魔（含锋利Plus）、11 组配置页（99 项）与可自定义的粒子特效；可选兼容矛
[Backported Spears](https://www.mcmod.cn/class/23213.html)。作者：**Summy**。

本仓库同时维护 Fabric 与 NeoForge 两份独立源码。Fabric 使用 Yarn 映射，NeoForge 使用
Mojang 官方映射；两树功能一致。

| 目录 | 平台 | 版本 |
| --- | --- | --- |
| [`fabric/`](fabric/README.md) | Fabric Loader 0.20.1 + Fabric API 0.116.18+1.21.1 | `1.4.2+1.21.1` |
| [`forge/`](forge/README.md) | NeoForge 21.1.265 | `1.4.2+1.21.1-neoforge` |

## 兼容

- KubeJS 仅作为 NeoForge 可选兼容；Minecraft 1.21.1 Fabric 当前没有 KubeJS 上游版本。
- Backported Spears、SR、SRA、BAT、Rhino 和 KubeJS 都不会打包进 SE JAR。
- Fabric 需要 Fabric API；NeoForge 无额外必需前置。
- 客户端与服务端必须使用相同 SE 版本，网络协议为 9。

## 构建

两个工程都需要 **JDK 21**。

```powershell
cd fabric
.\gradlew.bat clean build

cd ..\forge
.\gradlew.bat clean build verifyMixinTargets
```

各加载器目录的 README 提供开发、自检和兼容测试命令。

## 许可

MIT，见 [LICENSE](LICENSE)。

兼容策略与独立实现记录见 [docs/compatibility.md](docs/compatibility.md)。
