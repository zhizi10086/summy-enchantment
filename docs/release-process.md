# Summy Enchantment 发布流程

本流程沿用 Summy Reliquary 的发布门禁：构建、真实客户端自检、正式包检查、服务端冒烟、文档一致性、日志卫生和发布材料归档缺一不可。

## 1. 版本冻结

- 确认两树 `gradle.properties` 中的版本与目标发布版本一致。
- 确认 README、CHANGELOG、版本 Release 说明中的附魔数、配置数、协议与产物名一致。
- 确认本轮涉及的行为变化已经写入公开文档；内部文档不得进入发布材料。

## 2. 构建与注入校验

```powershell
cd fabric
.\gradlew.bat build

cd ..\forge
.\gradlew.bat build verifyMixinTargets
```

Forge `verifyMixinTargets` 必须全绿。警告不能包含缺失的 Mixin 目标、refmap 未命中或注入失败。

## 3. 真实客户端自检

```powershell
cd fabric
.\gradlew.bat runClient -Pdevcheck -PexternalModsDir="<官方模组 JAR 目录>"

cd ..\forge
.\gradlew.bat runClient -Pdevcheck -PexternalModsDir="<官方模组 JAR 目录>"
```

只使用专用世界 `SeDevCheckWorld`。必须看到 `[SE-DEVCHECK]` 最终汇总且失败数为 0，不能只以客户端启动成功作为通过。

## 4. 正式包检查

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-release.ps1
```

检查项：

- 两个正式 JAR 存在且文件名正确。
- 版本元数据与目标版本一致。
- 不含 `com/summy/enchantment/devcheck/**`。
- Fabric 包含 `fabric.mod.json`，Forge 包含 `META-INF/mods.toml`。
- 两个包都含 `LICENSE_summy-enchantment`。
- Forge 最终 `reobfJarJar` 包含嵌套 MixinExtras 及其许可证。
- 不包含 Celestial、Apotheosis、Majrusz 的包路径。
- 输出 SHA-256 供 Release 和平台版本页使用。

## 5. 服务端与部署冒烟

- 无 SR / 有 deobf SR + Curios 的 Forge 专用服务端各启动到 `Done`。
- 部署到目标实例后确认启动横幅、兼容检查、生命护盾属性自检与日志级别正常。
- 不得把 `run/`、测试世界、外部模组 JAR 或 devcheck 日志复制进公开仓库。

## 6. 文档与日志

- 核对版本号、产物名、附魔/配置/协议计数与链接。
- 日志只保留必要证据；不得在公开 Release 中写入本机绝对路径、账号信息或内部交接材料。
- README 只放当前使用说明与最新变更入口；历史技术细节放入 CHANGELOG 或专题文档。

## 7. GitHub Release

- 标签：`v<版本>`，例如 `v1.3.10`。
- 标题：`Summy Enchantment <版本>`。
- 正文：使用 `docs/releases/<版本>.md`。
- 资产：Fabric 与 Forge 两个正式 JAR。
- 发布通道：`Release`，设为 Latest。
- 发布后核对资产大小、SHA-256、下载链接与正文渲染。

## 8. 平台上传材料

每个版本在本地 `JAR/summy-enchantment/_release-<版本>/` 归档：

- 双加载器正式 JAR
- `Release 说明.md`
- `描述-中文.md` / `description-en.md`
- `更新日志.md`
- `元数据填写清单.md`
- `许可设置总表.md`
- `发布校验.md`
- `SHA256SUMS.txt`
- 平台图标
