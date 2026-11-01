const forbidden = [
  'addLifeShield',
  'clearLifeShield',
  'applyLifesteal',
  'meleeMultiplier',
  'hiddenBladeCooldownRemaining'
]
for (const name of forbidden) {
  try {
    const value = SummyEnchantment[name]
    console.error(`[SE-KJS-CLIENT] forbidden method exposed: ${name} ${typeof value}`)
  } catch (error) {
    console.info(`[SE-KJS-CLIENT] forbidden method hidden: ${name}`)
  }
}

try {
  const leaked = SummyEnchantmentEvents
  console.error(`[SE-KJS-CLIENT] event group exposed: ${typeof leaked}`)
} catch (error) {
  console.info('[SE-KJS-CLIENT] event group hidden')
}

const blockedClasses = [
  'com.summy.enchantment.api.SummyEnchantmentApi',
  'com.summy.enchantment.combat.EnchantmentEffects',
  'com.summy.enchantment.combat.ArrowTraits',
  'com.summy.enchantment.compat.SummyEnchantmentScriptHooks'
]
for (const name of blockedClasses) {
  try {
    Java.loadClass(name)
    console.error(`[SE-KJS-CLIENT] class unexpectedly allowed: ${name}`)
  } catch (error) {
    console.info(`[SE-KJS-CLIENT] class blocked: ${name}`)
  }
}

console.info(`[SE-KJS-CLIENT] version=${SummyEnchantment.apiVersion()} bool=${SummyEnchantment.configBoolean('enable_sky_wish')} boolNumeric=${SummyEnchantment.configBoolean('strength_multiplier_per_level')} default=${SummyEnchantment.configDefault('sky_wish_max_bonus')} step=${SummyEnchantment.configStep('sky_wish_max_bonus')} has=${SummyEnchantment.hasConfig('sky_wish_max_bonus')}`)
