SummyEnchantmentEvents.meleeSettled(event => {
  console.info(`[SE-KJS-EVENT] meleeSettled ${event.entry} ${event.amount}/${event.attackAmount} primary=${event.primarySpellbladeDamage}/${event.primaryLifestealHeal} surprise=${event.surpriseSpellbladeDamage}/${event.surpriseLifestealHeal} source=${event.damageSource}`)
})
SummyEnchantmentEvents.lifeShieldChanged(event => {
  console.info(`[SE-KJS-EVENT] lifeShieldChanged ${event.action} ${event.before}->${event.after}`)
})
SummyEnchantmentEvents.deathEyeChanged(event => {
  console.info(`[SE-KJS-EVENT] deathEyeChanged ${event.action} ${event.before}->${event.after} arrow=${event.arrow} target=${event.target}`)
})
SummyEnchantmentEvents.hiddenBladeConsumed(event => {
  console.info(`[SE-KJS-EVENT] hiddenBladeConsumed ${event.chargedTicks} ${event.bonus} landed=${event.landed}`)
})
SummyEnchantmentEvents.projectileSettled(event => {
  console.info(`[SE-KJS-EVENT] projectileSettled ${event.kind} ${event.amount}/${event.attackAmount} surprise=${event.surprise}`)
})
SummyEnchantmentEvents.electricArcSettled(event => {
  console.info(`[SE-KJS-EVENT] electricArcSettled ${event.chainId} ${event.segmentIndex}/${event.totalSegments} ${event.amount}/${event.attackAmount}`)
})
SummyEnchantmentEvents.electricArcFinished(event => {
  console.info(`[SE-KJS-EVENT] electricArcFinished ${event.chainId} ${event.completedSegments}/${event.totalSegments} ${event.reason}`)
})

ServerEvents.loaded(event => {
  const sword = Item.of('minecraft:netherite_sword').enchant('summy_enchantment:lifesteal', 3)
  console.info(`[SE-KJS-SELFTEST] version=${SummyEnchantment.apiVersion()} ids=${SummyEnchantment.enchantmentIds().size()} config=${SummyEnchantment.configKeys().size()}`)
  console.info(`[SE-KJS-SELFTEST] bool=${SummyEnchantment.configBoolean('enable_sky_wish')} boolNumeric=${SummyEnchantment.configBoolean('strength_multiplier_per_level')} default=${SummyEnchantment.configDefault('sky_wish_max_bonus')} step=${SummyEnchantment.configStep('sky_wish_max_bonus')} has=${SummyEnchantment.hasConfig('sky_wish_max_bonus')}`)
  console.info(`[SE-KJS-SELFTEST] applySword=${SummyEnchantment.enchantmentCanApplyTo(sword, 'lifesteal')} applyBow=${SummyEnchantment.enchantmentCanApplyTo(Item.of('minecraft:bow'), 'lifesteal')}`)
  const skyBow = Item.of('minecraft:bow').enchant('summy_enchantment:sky_wish', 1)
  console.info(`[SE-KJS-SELFTEST] typeCheck=${SummyEnchantment.enchantmentCanApplyTo(skyBow, 'instinctive_release')} fullCheck=${SummyEnchantment.enchantmentCanEnchant(skyBow, 'instinctive_release')}`)

  let apiAllowed = false
  try {
    Java.loadClass('com.summy.enchantment.api.SummyEnchantmentApi')
    apiAllowed = true
  } catch (error) {
    console.error(`[SE-KJS-SELFTEST] public API unexpectedly blocked: ${error}`)
  }
  if (apiAllowed) {
    console.info('[SE-KJS-SELFTEST] public API class load allowed')
  }

  const blockedInternals = [
    'com.summy.enchantment.combat.EnchantmentEffects',
    'com.summy.enchantment.combat.ArrowTraits',
    'com.summy.enchantment.compat.SummyEnchantmentScriptHooks'
  ]
  for (const name of blockedInternals) {
    try {
      Java.loadClass(name)
      console.error(`[SE-KJS-SELFTEST] internal class unexpectedly allowed: ${name}`)
    } catch (error) {
      console.info(`[SE-KJS-SELFTEST] internal class blocked: ${name}`)
    }
  }
})

PlayerEvents.loggedIn(event => {
  const player = event.player
  event.server.scheduleInTicks(1, () => {
    console.info(`[SE-KJS-PLAYER] cooldown=${SummyEnchantment.hiddenBladeCooldownRemaining(player)} strength=${SummyEnchantment.configValue('strength_multiplier_per_level')}`)
  })
})
