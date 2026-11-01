if (typeof SummyEnchantment !== 'undefined') {
  console.error('[SE-KJS-STARTUP] SummyEnchantment unexpectedly bound')
} else {
  console.info('[SE-KJS-STARTUP] SummyEnchantment binding hidden')
}

const blockedClasses = [
  'com.summy.enchantment.api.SummyEnchantmentApi',
  'com.summy.enchantment.combat.EnchantmentEffects',
  'com.summy.enchantment.compat.SummyEnchantmentScriptHooks'
]
for (const name of blockedClasses) {
  try {
    Java.loadClass(name)
    console.error(`[SE-KJS-STARTUP] class unexpectedly allowed: ${name}`)
  } catch (error) {
    console.info(`[SE-KJS-STARTUP] class blocked: ${name}`)
  }
}
