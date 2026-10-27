# Summy Enchantment KubeJS 基础 API

> 适用版本：1.3.10（Fabric 与 Forge 功能一致，仅映射名不同）。
> KubeJS 是**可选依赖**；未安装时 Summy Enchantment 行为不变，也不会加载任何 KubeJS 类。

## 安装与发现

- 支持的 KubeJS 版本：1.20.1 `2001.6.5` 系列。
- SE 的 jar 内含 `kubejs.plugins.txt`；KubeJS 已安装时会自动发现并绑定全局对象 `SummyEnchantment`。
- Forge / Fabric 都使用同一个脚本入口；KubeJS 的 `server_scripts`、`client_scripts` 均可读取绑定。

未安装 KubeJS 时不需要任何额外配置；SE 不会形成硬依赖。

## 方法

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `SummyEnchantment.level(stack, id)` | `int` | 读取附魔等级；`id` 可写全名或省略 `summy_enchantment:` 的短名；未知 ID 返回 0 |
| `SummyEnchantment.has(stack, id)` | `boolean` | 等级大于 0 |
| `SummyEnchantment.lifeShield(entity)` | `float` | 当前生命护盾属性值 |
| `SummyEnchantment.lifeShieldCap(entity)` | `float` | 当前胸甲等级、最大生命和配置对应的上限 |
| `SummyEnchantment.addLifeShield(entity, amount)` | `float` | 直接增加护盾点数；仅服务端生效；返回实际增加量 |
| `SummyEnchantment.clearLifeShield(entity)` | `void` | 清空生命护盾及我方吸收份额 |
| `SummyEnchantment.lifestealHeal(weapon, dealtDamage)` | `float` | 只预览嗜血应回复量，不执行治疗 |
| `SummyEnchantment.applyLifesteal(attacker, weapon, dealtDamage)` | `float` | 按现有嗜血管线执行治疗；返回计算出的治疗量 |
| `SummyEnchantment.meleeMultiplier(player)` | `float` | 当前近战乘区：力量 × 复仇；死神祝福仍在受伤侧结算 |

## 示例

```js
// 物品/附魔书等级
const sword = Item.of('minecraft:netherite_sword')
  .enchant('summy_enchantment:lifesteal', 3)

console.info(SummyEnchantment.level(sword, 'lifesteal')) // 3
console.info(SummyEnchantment.has(sword, 'summy_enchantment:lifesteal')) // true

// 读取/增加生命护盾（仅服务端；胸甲必须有生命护盾）
PlayerEvents.loggedIn(event => {
  const player = event.player
  console.info(`当前护盾：${SummyEnchantment.lifeShield(player)}`)
  const added = SummyEnchantment.addLifeShield(player, 4.0)
  console.info(`本次实际增加：${added}`)
})

// 预览/执行嗜血（只用于尚未经过 SE 管线的自定义伤害）
const preview = SummyEnchantment.lifestealHeal(sword, 10)
console.info(`嗜血预览：${preview}`)
```

## 口径与边界

- `addLifeShield` 的 `amount` 是**护盾点数**，不是治疗量；需要走正常“治疗 → 生命护盾”链路时使用 `applyLifesteal` 或原版 `player.heal`。
- `addLifeShield` 只在服务端生效，要求胸甲带生命护盾且 `enable_life_shield` 打开；客户端调用返回 0。
- 胸甲同时带 Celestial `celestial_enchantments:life_shield` 时，SE 生命护盾主动退让：`lifeShieldCap` 与 `addLifeShield` 返回 0，`addLifeShield` 不会写入吸收池。
- `applyLifesteal` 只用于**尚未经过 SE 近战 / 三叉戟 / SR 管线**的自定义伤害。正常命中再调用会重复结算。
- `applyLifesteal` 返回计算出的治疗量，不等于最终生命值增量；生命修补可能整段消耗该治疗。
- 本版本不新增 SE 专属 KubeJS 事件、自定义物品兼容注册表、自定义附魔行为 Hook 或配置读写 API。
- 配置仍通过现有 `/summyenchantment set <键> <值>` 修改；KubeJS 可在服务端脚本中用 `server.runCommandSilent(...)` 调用。
- 既有标签仍是官方扩展口：
  - `summy_enchantment:no_dodge`
  - `summy_enchantment:no_deaths_blessing_outgoing`
  - `summy_enchantment:arrow_particles`
  - `summy_enchantment:no_arrow_particles`
