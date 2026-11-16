# 更新记录（Changelog）

> 本文档收录 Summy Enchantment 的历史版本变更；当前版本的内容与玩法以 README 和对应 Release 说明为准。
>
> 更新日志只记录行为变化、兼容边界和面向玩家的结果；完整构建与自检矩阵保留在发布核验。

## 1.4.3+1.21.1 变更

> Minecraft 1.21.1 · Fabric Loader 0.20.1+ · Fabric API 0.116.18+1.21.1 · NeoForge 21.1.265+ · Java 21
>
> 本次完成 1.4.3 兼容收口；网络协议继续为 `9`，不新增配置键、附魔 ID 或公开 API。
>
> 客户端与服务端必须使用同一版本，且 `mods/` 中只能保留一份 Summy Enchantment。

### 修改

- **自动熔炼**：改为包装原版掉落列表，保留方块经验、平台掉落回调和后续模组处理；潜行关闭、精准采集互斥，以及高炉或烟熏炉独有配方不触发的行为保持不变。
- **电刀**：链式目标选择增加队友、攻击者已驯服宠物和攻击者拥有的归属实体过滤；没有队伍或归属声明的 NPC 仍按普通可攻击目标处理。

### 修复

- **玩家属性**：Fabric 改为在玩家构造末尾按需追加 `life_shield`，不再整表替换默认属性；NeoForge 继续通过 `EntityAttributeModificationEvent` 添加属性。
- **额外箭**：首次伤害被第三方事件或保护规则拒绝后不再重复调用伤害管线，避免一次命中重复触发第三方事件。
- **生物力量**：关闭生物力量重定向时保留原版力量；已经存在的自定义力量会在后续维护 tick 还原为同等级、同时长的原版力量。
- **灵魂加护与恶咒**：灵魂加护复活后的 40 tick 内不执行恶咒压血；冷却计时、复活音效、粒子和抗性效果保持原有语义。
- **斩杀与复仇**：斩杀报告使用 `hurt` 的真实结果并恢复被拒绝补刀前的无敌帧；复仇窗口在过期、断线和停服时清理。
- **随机附魔**：1.21.1 两树的随机候选过滤改为返回可变 `ArrayList`，避免真实附魔抽取在后续冲突移除时抛出不可变列表异常。

### 兼容

- **Apotheosis 铁砧**：检测到 `apotheosis` 时，NeoForge 整体停用两个铁砧 Mixin，由神化接管成本、扣费和客户端显示；1.21.1 对应版本的真实同装矩阵仍待后续验证。
- **内容规模**：附魔仍 32、配置仍 99 项 / 11 组、网络协议仍 9；Fabric 与 NeoForge 功能对等，无新增配置键、语言键、注册表类型或公开方法签名。

## 1.4.2+1.21.1

### Minecraft 1.21.1 平台版本

- Fabric 与 NeoForge 升级到 Minecraft 1.21.1、Java 21。
- Fabric 使用 Loader 0.20.1、Fabric API 0.116.18+1.21.1；NeoForge 使用 21.1.265。
- 保持 32 个附魔、99 项配置 / 11 组与网络协议 9。
- 完成 Fabric/NeoForge 核心自检、Spears 双加载器、KubeJS NeoForge、SR/SRA 真实入口和服务端组合验证。
- KubeJS 仅提供 NeoForge 1.21.1 可选兼容；Fabric 不包含 KubeJS 插件。
- 正式产物：`summy-enchantment-1.4.2+1.21.1.jar`、`summy-enchantment-1.4.2+1.21.1-neoforge.jar`。

## 1.4.2

### 矛兼容修复

- 保留 Backported Spears 1.4.7 的旧公式反解。
- 正式支持 Backported Spears 1.5.0+：不再二次修正上游已修复的基础伤害，同时恢复 SE 的 KubeJS 事件、魔剑、嗜血和左键力量Plus。
- 1.5.0+ 左键目标值为 `力量倍率 × (基础伤害 + 冷却 × 附魔伤害)`；右键保持上游 `基础 + 附魔 + 速度`，不追加力量Plus。
- 其它无法确认的矛版本继续安全放行，但不启用 SE 矛结算。
- 新增 `devcheckMode=spear` 与 `verifySpearSelfTest`，分别验证 1.4.7 和 1.5.0 的伤害、结算及力量Plus。
- 附魔仍 32、配置仍 99 项 / 11 组、协议仍 9；产物：`summy-enchantment-1.4.2.jar`、`summy-enchantment-1.4.2-forge.jar`（1.4.1 保留）。

## 1.4.1

### 兼容性修复

- 轻盈改为包装箭矢 `tick` 的最终纵向速度写入，不再与 Twilight Dusk 等模组的重力常量注入冲突；Twilight Dusk 的幻影箭半重力与 SE 轻盈倍率乘算共存。
- 大胃袋改为组合式包装饥饿封顶结果与能否进食判定，不再替换 `20` 常量；其它模组已扩展的容量不会被压低。
- 不新增配置、附魔、语言键、协议或公开 API；仍为 32 附魔、99 配置 / 11 组、协议 9。
- 产物：`summy-enchantment-1.4.1.jar`、`summy-enchantment-1.4.1-forge.jar`（1.4.0 保留）。

## 1.4.0

### KubeJS API 与事件

- 服务端保留原有九个方法，最终提供 43 个公开方法；新增 `enchantmentCanApplyTo`（物品类型）、`enchantmentCanEnchant`（物品类型 + 双向互斥）、配置元数据、接口版本和藏剑冷却读取。
- 客户端绑定缩减为 24 个只读查询，不允许直接加载含服务端写方法的 API 类；启动脚本不再绑定世界相关 API。
- 脚本类过滤器默认封锁 `com.summy.enchantment.` 全部内部类，仅服务端脚本精确放行公共 API，防止绕过绑定表访问战斗实现。
- 新增七个服务端只读事件：`meleeSettled`、`lifeShieldChanged`、`deathEyeChanged`、`hiddenBladeConsumed`、`projectileSettled`、`electricArcSettled`、`electricArcFinished`。
- `meleeSettled` 增加伤害源、攻击侧金额别名及主命中/出其不意分项；箭矢、三叉戟、矛和电刀均获得只读观测覆盖。
- 投射物和电刀事件补齐 `attackAmount` 别名，事件武器统一为副本；KubeJS 声明范围精确限定为 `>=2001.6.5-build.16 <2001.6.6`。
- 仓库纳入可复用 KubeJS 自检脚本与 `verifyKubeJsSelfTest` 自动断言任务，支持 `-PdevcheckMode=kubejs` 独立运行，并可通过 Gradle 参数切换 KubeJS/Rhino/Architectury 版本。
- 自检支持通过 `-PdevcheckWorldDir` 恢复专用测试世界；缺少世界时立即报错，避免误报为客户端超时。
- 新增 `THIRD_PARTY_NOTICES.md`，登记离线编译与自检依赖的来源、许可证和 SHA-256；这些 JAR 均不进入发布产物。
- `clearLifeShield` 与 `applyLifesteal` 增加逻辑服务端保护。
- `configBoolean` 增加配置类型校验，数值项不会再因值大于等于 0.5 被误报为布尔真。

### 附魔调整

- 破空之愿与本能释放增加双向互斥。
- 附魔台、铁砧、随机组合和 `/enchant` 均不能给同一把弓新增第二项。
- 通过 NBT 或数据包强行写入两项的旧物品保留原有双重效果，不自动清理。
- 中英文附魔描述同步注明互斥，语言键数量不变。

### 兼容边界

- 附魔仍 32 个、配置仍 99 项 / 11 组、网络协议仍 9。
- KubeJS 仍为可选依赖；未安装时不加载插件类。
- 本版不提供脚本自定义附魔行为、可取消事件或配置写入。

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
