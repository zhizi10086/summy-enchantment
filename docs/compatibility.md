# 兼容策略与独立实现记录

> 适用版本：Summy Enchantment 1.3.10（Fabric / Forge）

本文记录与 Celestial Enchantments、Apotheosis、Majrusz's Enchantments 的同功能附魔处理规则，以及生命护盾的独立开发时间线。外部模组均只通过公开注册 ID 识别，不建立编译依赖，也不修改对方代码或配置。

## 兼容规则

| SE 附魔 | 外部附魔 | 规则 |
| --- | --- | --- |
| 生命护盾 `summy_enchantment:life_shield` | Celestial `celestial_enchantments:life_shield` | 胸甲同时存在时，SE 主动退让；停止累积并只撤回 SE 自己剩余的份额，不修改原版或 Celestial 的吸收。移除 Celestial 附魔后自动恢复。 |
| 生命修补 `summy_enchantment:life_mending` | Apotheosis `apotheosis:life_mending` | SE 严格优先。SE 有可修装备时整段治疗仍由 SE 消耗；SE 没有可修装备时，Forge 治疗事件照常继续，Apotheosis 的生命修补及 Curios 修理可正常生效。 |
| 死神祝福 `summy_enchantment:deaths_blessing` | Majrusz `majruszsenchantments:death_wish` | 同一实体带有死神之愿时，SE 的对应侧主动退让：攻击者侧不再乘造成倍率，受击者侧不再享受受伤倍率。双方分别判断。 |

生命护盾与生命修补的 `LivingHealEvent` 先后关系保持不变：SE 生命修补仍在治疗入口最先结算，因此 SE 修理成功时不会触发后续吸收或 Apotheosis 治疗事件。这是已确认的刻意优先级，不是随机事件顺序。

## 独立实现记录

- Celestial 当前 1.3.7 快照提交 `b46b82f`（2026-05-10）已包含 `LifeShield`。
- SE 本地旧版 `enchantment-reforged-1.3.1.jar`（2026-09-18）和 `enchantment-reforged-1.3.3.jar`（2026-09-19）已包含 `SHIELD_TRACK`、`lifeShieldCap`、`onHeal`、`tickLifeShield` 等生命护盾实现。
- SE 公共仓库首次提交 `3f923f1`（2026-10-06）直接包含 1.3.4 的完整生命护盾与更早版本演进说明。
- 本轮对 Celestial、Apotheosis、Majrusz 的兼容核验发生于 2026-10-27，晚于上述实现与构建产物。
- 两版生命护盾实现结构不同：SE 使用自定义玩家属性、实际回复量、半心量化与吸收池记账；Celestial 直接写吸收值并监听 `LivingHealEvent`。

这些记录证明 SE 代码在本次兼容审查前已经独立存在并持续迭代，本次没有借用或复制对方代码；但缺少 2026-10-06 前的完整 Git 历史，不能据此主张 SE 比 Celestial 更早发明生命护盾。

## 许可证

- Summy Enchantment：MIT，许可证正文随 Fabric 与 Forge 发布 JAR 分发。
- Celestial Enchantments：LGPL-2.1。
- Apotheosis：代码 MIT；指定材质的额外条款为 CC-BY-4.0。
- Majrusz's Enchantments / Majrusz Library：源码构建属性声明 MIT。

SE 当前未复制或打包上述模组的源码、语言文本或材质。若未来引入对方代码或资源，必须按其许可证补充相应版权声明和分发义务。

## 验证记录

- 2026-10-27：Fabric 与 Forge 两树 `gradlew build` 均通过；Forge `verifyMixinTargets` 通过。
- 无外部附魔模组的 Forge 专用服务端启动到 `Done`，生命护盾属性自检通过。
- 外部模组的组合验收应在正式 Forge 或 Kilt 实例进行；不要把未经 deobf 的生产 Jar 直接放入 ForgeGradle dev `run/mods`，否则会先被对方 Mixin 的开发映射问题拦截。
- 2026-10-28：Forge 真实客户端同时装载 Celestial Enchantments 1.3.7、Apotheosis 7.4.8、Majrusz's Enchantments 1.10.8，`[SE-DEVCHECK]` 共 12 项、失败 0 项；覆盖 SE/Celestial 生命护盾退让与恢复、SE/Apotheosis 生命修补优先级及回退、SE/Majrusz 死神祝福造成侧与受伤侧单层结算。
- 2026-10-28：Fabric 原生真实客户端装载 Majrusz's Enchantments Fabric 1.10.8 + Majrusz Library Fabric 7.0.8，`[SE-DEVCHECK]` 共 4 项、失败 0 项；造成侧基础 10 点结算为 15 点、受伤侧结算为 9.5 点，确认 SE 退让后只剩 Majrusz 一层。
- 客户端自检使用 `SeDevCheckWorld`，不会在普通存档运行；Forge 与 Fabric 的正式发布产物均排除开发自检包。
