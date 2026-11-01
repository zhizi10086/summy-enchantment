# Summy Enchantment KubeJS API

> 适用版本：1.4.0（Fabric 与 Forge 功能一致）。
> KubeJS 是可选依赖；未安装时 SE 不加载任何 KubeJS 类，玩法与没有安装 KubeJS 时一致。

## 支持版本与加载

- 支持 Minecraft 1.20.1 的 KubeJS `2001.6.5` 系列，声明范围为 `>=2001.6.5-build.16 <2001.6.6`；本版以 `2001.6.5-build.16` 编译和验证。
- SE 的 JAR 内含 `kubejs.plugins.txt`，KubeJS 已安装时会自动发现插件。
- 服务端脚本获得完整 `SummyEnchantment` API（43 个方法）和 `SummyEnchantmentEvents` 事件组。
- 客户端脚本获得 24 个只读查询方法；不提供写入、藏剑冷却或任何服务端事件。脚本类过滤器默认封锁整个 `com.summy.enchantment.` 包，仅服务端脚本可精确加载 `SummyEnchantmentApi`。
- 启动脚本不绑定 `SummyEnchantment`，避免在注册表或世界状态尚未就绪时访问依赖对象。

开发期可使用 `-PkubejsModsDir=<目录>` 将官方 KubeJS、Rhino 与 Architectury JAR 加入真实客户端脚本自检；该目录只用于测试，不会进入正式 JAR。可通过 `-PkubejsVersion=<版本>`、`-PrhinoVersion=<版本>`、`-ParchitecturyVersion=<版本>` 切换测试矩阵，默认分别为 `2001.6.5-build.16`、`2001.2.3-build.10`、`9.2.14`。

仓库内置自检脚本位于 `tools/kubejs-selftest/`；存在 `kubejsModsDir` 时，`runClient` / `runServer` 会自动复制到 `run/kubejs`，测试后可安全清理 `run/`。

推荐使用自动断言任务，而不是只运行客户端后人工读日志：

```bash
./gradlew verifyKubeJsSelfTest \
  -Pdevcheck \
  -PdevcheckMode=kubejs \
  -PkubejsModsDir=<KubeJS、Rhino、Architectury 所在目录> \
  -PdevcheckWorldDir=<现成的 SeDevCheckWorld 存档目录>
```

`SeDevCheckWorld` 可由上一次测试保留，也可通过 `devcheckWorldDir` 从仓库外复制；两者都没有时任务会立即报错，不进入 60 秒等待。该模式只执行 KubeJS 桥接自检，不要求安装 Celestial、Apotheosis 或 Majrusz。任务会检查公共 API 放行、内部类封锁、客户端/启动脚本隔离、七个事件和基础 API 返回值；任一标记缺失或出现意外放行时直接失败。

仓库内第三方编译依赖的版本、许可证、来源和 SHA-256 见 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)。

绑定矩阵：

| 脚本类型 | `SummyEnchantment` | `SummyEnchantmentEvents` | 直接加载 API 类 |
| --- | --- | --- | --- |
| `server_scripts` | 完整 API | 可用 | 允许 |
| `client_scripts` | 只读查询 | 不提供 | 不允许 |
| `startup_scripts` | 不提供 | 不提供 | 不允许 |

## 兼容方法

以下九个方法从 1.3.9 保留，签名不变；完整实现仅在服务端绑定中出现。

`SummyEnchantment.apiVersion()` 返回当前公共接口版本，1.4.0 固定为 `1`；只有破坏性接口变更时才递增。

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `SummyEnchantment.level(stack, id)` | `int` | 读取附魔等级；`id` 可写全名或省略 `summy_enchantment:` 的前缀；未知 ID 返回 0 |
| `SummyEnchantment.has(stack, id)` | `boolean` | 等级大于 0 |
| `SummyEnchantment.lifeShield(entity)` | `float` | 当前生命护盾属性值；客户端绑定也可读取，服务端为权威值 |
| `SummyEnchantment.lifeShieldCap(entity)` | `float` | 当前胸甲、最大生命和配置共同决定的生命护盾上限 |
| `SummyEnchantment.addLifeShield(entity, amount)` | `float` | 服务端直接增加护盾点数并返回实际增加量 |
| `SummyEnchantment.clearLifeShield(entity)` | `void` | 服务端清空 SE 生命护盾及 SE 自己的吸收份额 |
| `SummyEnchantment.lifestealHeal(weapon, dealtDamage)` | `float` | 只预览嗜血应回复量，不执行治疗 |
| `SummyEnchantment.applyLifesteal(attacker, weapon, dealtDamage)` | `float` | 服务端按现有嗜血管线执行治疗并返回计算量 |
| `SummyEnchantment.meleeMultiplier(player)` | `float` | 当前近战乘区：力量 × 复仇；死神祝福仍在受伤侧结算 |

客户端只读绑定包含：`level`、`has`、`lifeShield`、`lifeShieldCap`、`lifestealHeal`，以及下文的元数据和配置查询。`clearLifeShield` 与 `applyLifesteal` 另有核心服务端保护，即使在 Java 层误调用也不会修改客户端战斗状态。

## 附魔元数据

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `enchantmentIds()` | `List<String>` | 全部 SE 附魔完整 ID，按字母排序 |
| `enchantmentMaxLevel(id)` | `int` | 最大等级；未知 ID 返回 0 |
| `enchantmentRarity(id)` | `String` | `common`、`uncommon`、`rare` 或 `very_rare`；未知 ID 返回空字符串 |
| `isTreasure(id)` | `boolean` | 是否为宝藏附魔 |
| `isCursed(id)` | `boolean` | 是否为诅咒附魔 |
| `enchantmentCanApplyTo(stack, id)` | `boolean` | 只检查附魔自身的物品类型规则 |
| `enchantmentCanEnchant(stack, id)` | `boolean` | 同时检查物品类型和现有附魔的双向互斥 |

元数据查询在服务端和客户端绑定中都可用。客户端在游戏注册表尚未完成同步的脚本初始化阶段可能暂时得到空列表或 0；需要可靠元数据时，应在进入世界后的事件或定时回调中查询。

`enchantmentCanApplyTo(stack, id)` 只按附魔自身的物品类型规则判断“这个物品类别是否允许该附魔”；空物品或未知 ID 返回 `false`，短 ID 自动补 `summy_enchantment:`。它不检查目标已有附魔的互斥、现有等级、铁砧成本或宝藏获取途径。该方法也可查询其它模组已注册附魔的完整 ID。

`enchantmentCanEnchant(stack, id)` 在物品类型检查之外，还会双向检查现有附魔互斥。例如已带破空之愿的弓会拒绝本能释放。它仍不检查附魔等级上限、经验成本或宝藏获取途径。

## 服务端状态查询

以下方法依赖服务端权威状态，只存在于 `server_scripts` 绑定：

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `deathEyeStacks(player)` | `int` | 死亡之眼当前层数 |
| `deathEyeMultiplier(player)` | `float` | 当前层数对应的下一次箭矢伤害倍率 |
| `arrowLightnessLevel(arrow)` | `int` | 这支箭当前轻盈等级 |
| `arrowDeathEyeMultiplier(arrow)` | `float` | 这支箭本次命中的死亡之眼倍率 |
| `hiddenBladeChargedTicks(entity)` | `int` | 藏剑当前累积 tick |
| `hiddenBladeBonus(entity)` | `float` | 藏剑当前等效加成，例如 `0.8` 表示 +80% |
| `hiddenBladeChargeProgress(entity)` | `float` | 藏剑充能进度，范围 0～1 |
| `hiddenBladeCooldownRemaining(entity)` | `int` | 藏剑剩余冷却 tick；无冷却时为 0 |
| `deathsBlessingOutgoing(entity)` | `float` | 当前生命损失对应的死神祝福造成倍率 |
| `deathsBlessingIncoming(entity)` | `float` | 当前生命损失对应的死神祝福受伤倍率 |
| `revengeMultiplier(entity)` | `float` | 当前复仇倍率；无窗口时为 1.0 |
| `arrowSpeedMultiplier(weapon)` | `float` | 疾矢对武器射出的箭提供的速度/伤害倍率 |
| `undyingGraceReduction(entity)` | `float` | 当前不死者加护固定减伤点数 |
| `foodLevelCap(entity)` | `int` | 当前饥饿值上限 |
| `sturdyCap(stack)` | `int` | 该物品当前单次耐久损耗上限 |

空实体、空物品或空箭返回中性值；死亡之眼与藏剑状态为 0。
`sturdyCap(stack)` 对没有坚甲的物品返回 `Integer.MAX_VALUE`，表示没有施加单次损耗上限。

## 配置查询

配置查询为只读，服务端和客户端绑定都可用：

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `configKeys()` | `List<String>` | 当前 99 个公开配置键，按字母排序 |
| `configValue(key)` | `double` | 当前逻辑端的值；布尔项返回 `1.0/0.0` |
| `configBoolean(key)` | `boolean` | 仅读取真正的布尔配置；未知键和非布尔键返回 `false` |
| `configDefault(key)` | `double` | 配置默认值；未知键返回 `NaN` |
| `configMin(key)` | `double` | 允许下限 |
| `configMax(key)` | `double` | 允许上限 |
| `configStep(key)` | `double` | 界面与命令使用的步长 |
| `hasConfig(key)` | `boolean` | 配置键是否存在 |
| `configScope(key)` | `String` | `server` 或 `client` |
| `configKind(key)` | `String` | `boolean`、`double`、`integer` 或 `enum` |
| `configCategory(key)` | `String` | 配置枚举中的分类名 |

未知键的数值查询返回 `NaN`，字符串查询返回空字符串，布尔查询返回 `false`。服务端配置由现有同步链路下发；客户端作用域项读取本机自己的值。

不提供 `setConfig` 或 `resetConfig`。服务端脚本可继续使用：

```js
server.runCommandSilent('/summyenchantment set enable_sky_wish false')
server.runCommandSilent('/summyenchantment reset')
```

## 只读事件

事件绑定仅在 `server_scripts` 中提供：

```js
SummyEnchantmentEvents.meleeSettled(event => {})
SummyEnchantmentEvents.lifeShieldChanged(event => {})
SummyEnchantmentEvents.deathEyeChanged(event => {})
SummyEnchantmentEvents.hiddenBladeConsumed(event => {})
SummyEnchantmentEvents.projectileSettled(event => {})
SummyEnchantmentEvents.electricArcSettled(event => {})
SummyEnchantmentEvents.electricArcFinished(event => {})
```

所有事件均为只读完成事件，不支持取消、结果覆盖或修改核心伤害。无监听器时 SE 在进入事件桥后立即短路；监听器异常由 KubeJS 记录，不中断 SE 结算。

事件在服务端战斗结算线程上同步触发。监听器应保持短小，只做计数、记录或必要的非阻塞查询；不要在事件中执行大范围实体扫描、同步磁盘 I/O 或大量命令，否则会直接增加服务器单 tick 耗时。

### `meleeSettled`

普通左键近战、Summy Reliquary 近战或 Backported Spears 命中后触发。

可用字段：`entry`、`attacker`、`target`、`weapon`、`damageSource`、`rawAmount`、`amount`、`attackAmount`、`landed`、`mainHit`、`spellbladeDamage`、`lifestealHeal`、`primarySpellbladeDamage`、`primaryLifestealHeal`、`surpriseSpellbladeDamage`、`surpriseLifestealHeal`、`executed`、`surpriseTriggered`、`surpriseLanded`。

`amount` 与 `attackAmount` 是伤害调用前的攻击侧金额，不是目标最终掉血；目标护甲、保护、吸收及第三方事件不在该值内。`spellbladeDamage` 与 `lifestealHeal` 是主命中和出其不意第二段的总量。Backported Spears 的 `entry` 为 `spearStab` 或 `spearCharge`；其兼容注入点拿不到原始伤害源，因此 `damageSource` 为 `null`。

```js
SummyEnchantmentEvents.meleeSettled(event => {
  if (event.entry === 'heavySlash') {
    console.info(`强力斩击最终金额：${event.amount}`)
  }
})
```

### `lifeShieldChanged`

SE 生命护盾值实际变化后触发。

可用字段：`entity`、`action`、`before`、`after`、`delta`、`cap`。

`action` 可能为：`heal`、`add`、`consume`、`cap`、`restore`、`clear`。

### `deathEyeChanged`

死亡之眼层数实际变化后触发。

可用字段：`player`、`playerId`、`arrow`、`target`、`action`、`before`、`after`、`delta`。

`action` 可能为：`hit`、`missClear`、`reset`。

`hit` 同时携带命中的箭与目标；`missClear` 只携带触发清空的箭；死亡、重生或断线触发的 `reset` 中二者均为 `null`。

### `hiddenBladeConsumed`

带有非零藏剑充能的一刀实际挥出并清空后触发，包括砍空。

可用字段：`player`、`target`、`landed`、`chargedTicks`、`bonus`、`progress`。

挥空时 `target` 为攻击目标实体、`landed` 为 `false`；若攻击目标本身为空，则 `target` 为 `null`。

### `projectileSettled`

箭矢或投掷三叉戟完成一次伤害尝试后触发。

可用字段：`kind`、`attacker`、`target`、`projectile`、`weapon`、`damageSource`、`rawAmount`、`amount`、`attackAmount`、`landed`、`extraProjectile`、`deathEyeMultiplier`、`spellbladeDamage`、`lifestealHeal`、`surprise`。

`kind` 为 `arrow` 或 `trident`。箭矢没有可直接读取的弓弩物品，`weapon` 为空物品。`extraProjectile` 标识幻影箭或多重射击额外箭。投掷三叉戟的出其不意第二段会额外触发一条 `surprise=true` 事件。`amount` 仍是攻击侧金额。

### `electricArcSettled`

电刀每段实际伤害尝试完成后触发。

可用字段：`chainId`、`segmentIndex`、`totalSegments`、`remainingSegments`、`attacker`、`target`、`weapon`、`damageSource`、`baseAmount`、`amount`、`attackAmount`、`landed`、`spellbladeDamage`、`lifestealHeal`。

`segmentIndex` 从 1 开始；`baseAmount` 是主命中的攻击侧基数，`amount` 是本段经过 0.8 衰减后的攻击侧金额。

### `electricArcFinished`

一条电刀链结束时恰好触发一次。

可用字段：`chainId`、`attacker`、`weapon`、`completedSegments`、`totalSegments`、`reason`。

`reason` 固定为 `completed`、`no_target`、`attacker_offline` 或 `invalid_world`。攻击者离线时 `attacker` 可为 `null`。

## 示例

读取附魔和元数据：

```js
const sword = Item.of('minecraft:netherite_sword')
  .enchant('summy_enchantment:lifesteal', 3)

console.info(SummyEnchantment.level(sword, 'lifesteal')) // 3
console.info(SummyEnchantment.enchantmentMaxLevel('sky_wish')) // 1
console.info(SummyEnchantment.isTreasure('sky_wish')) // true
```

读取状态与配置：

```js
PlayerEvents.tick(event => {
  if (event.player.age % 100 !== 0) return
  const player = event.player
  console.info(`死亡之眼 ${SummyEnchantment.deathEyeStacks(player)} 层`)
  console.info(`藏剑进度 ${SummyEnchantment.hiddenBladeChargeProgress(player)}`)
  console.info(`藏剑冷却 ${SummyEnchantment.hiddenBladeCooldownRemaining(player)} tick`)
  console.info(`破空开关 ${SummyEnchantment.configBoolean('enable_sky_wish')}`)
})
```

安全增加生命护盾：

```js
const added = SummyEnchantment.addLifeShield(player, 4.0)
console.info(`本次实际增加：${added}`)
```

预览和执行自定义伤害对应的嗜血：

```js
const preview = SummyEnchantment.lifestealHeal(sword, 10)
const healed = SummyEnchantment.applyLifesteal(player, sword, 10)
```

## 边界

- `addLifeShield` 的 `amount` 是护盾点数，不是治疗量。
- `applyLifesteal` 只用于尚未经过 SE 近战、三叉戟、SR 管线的自定义伤害；正常命中后再次调用会重复结算。
- `applyLifesteal` 返回计算出的治疗量，不等于最终生命值增量；生命修补可能整段消耗该治疗。
- 事件不提供修改器或取消能力。需要修改伤害时应使用对应的标准事件或显式调用安全的 SE API。
- `amount` 全部表示伤害入口的攻击侧金额，不估算护甲、保护、吸收或第三方限伤后的最终掉血。
- 事件返回的武器是事件创建时的副本，脚本修改 `event.weapon` 不会改动场上原物品。
- 本版本不提供脚本自定义附魔注册、任意伤害公式替换、配置写入或客户端事件；服务端配置仍通过 `/summyenchantment set|reset` 修改。
- 既有数据包标签仍是官方扩展口：`no_dodge`、`no_deaths_blessing_outgoing`、`arrow_particles`、`no_arrow_particles`。
- 当前没有独立的能力字符串列表；脚本可用 `apiVersion()` 判断公共接口代际，并用 `hasConfig()`、`enchantmentIds()` 和绑定方法是否存在进行功能性探测。
