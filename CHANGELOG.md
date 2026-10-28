# Changelog

本文件从 1.3.10 起记录发布级变更。1.3.9 及更早版本的完整技术记录仍保留在
Fabric 与 Forge 两份 README 的发布记录中。

## 1.3.10

### 外部附魔兼容

- **生命护盾**：胸甲同时带有 Celestial Enchantments 的 `life_shield` 时，SE 主动退让，停止累积并仅撤回 SE 自己剩余的份额；移除对方附魔后自动恢复。
- **生命修补**：维持 SE 严格优先。SE 有可修装备时消费整次治疗；SE 无可修装备时，Apotheosis `life_mending` 与 Curios 修理照常生效。
- **死神祝福**：同一实体带有 Majrusz's Enchantments 的 `death_wish` 时，SE 对应侧退回中性 `1.0`，避免同公式二次乘算。
- 外部附魔只通过公开注册 ID 懒解析，不建立编译依赖，不修改对方代码、配置、语言文本或材质。

### 发布与验证

- 新增 Fabric / Forge 真实客户端自检设施；正式 JAR 与 sources JAR 排除 `devcheck`。
- Forge 客户端同时验证 Celestial 1.3.7、Apotheosis 7.4.8、Majrusz 1.10.8：12 项、0 失败。
- Fabric 原生客户端验证 Majrusz Fabric 1.10.8 + Library 7.0.8：4 项、0 失败。
- 两树构建通过，Forge `verifyMixinTargets` 的 29 个文件全绿。
- Fabric / Forge 正式 JAR 均包含 MIT 许可证正文；Forge 嵌套 MixinExtras 包含自身许可证。
- Fabric 与 Forge JAR 都内置最终版铁砧、附魔核心与双箭头图标，并在各自元数据中声明。

### 兼容边界

- Celestial 与 Apotheosis 没有 1.20.1 官方 Fabric 版，因此 Fabric 原生兼容实测只覆盖 Majrusz。
- Kilt / Connector 的组合兼容继续作为独立验证面，不属于 1.3.10 GitHub Release 的自动门禁。
