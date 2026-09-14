package com.example.capture.notifications

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils

object SupportedPaymentApps {

  // Comprehensive registry of known Indian and international UPI, Neobank, and Banking package names
  private val KNOWN_PACKAGES: Map<String, String> = mapOf(
    // Major UPI & FinTech apps
    "com.google.android.apps.nbu.paisa.user" to "Google Pay",
    "com.phonepe.app" to "PhonePe",
    "net.one97.paytm" to "Paytm",
    "com.dreamplug.androidapp" to "CRED",
    "in.org.npci.upiapp" to "BHIM",
    "in.amazon.mShop.android.shopping" to "Amazon Pay",
    "com.whatsapp" to "WhatsApp Pay",
    "com.whatsapp.w4b" to "WhatsApp Business",
    "com.navi.finance" to "Navi UPI & Loans",
    "money.jupiter" to "Jupiter Neobank",
    "money.fi.app" to "Fi Money",
    "indefinite.slice" to "Slice UPI",
    "club.supermoney.app" to "Super.money",
    "com.slicepay" to "Slice",
    "com.mobikwik_new" to "MobiKwik",
    "com.samsung.android.spay" to "Samsung Wallet",
    "com.samsung.android.spaymini" to "Samsung Pay Mini",
    "com.freecharge.android" to "Freecharge",
    "com.airtel.myairtel" to "Airtel Payments Bank",
    "com.jio.myjio" to "Jio Payments Bank",
    "com.bharatpe.app" to "BharatPe",
    "com.popclub.app" to "POPclub UPI",
    "com.gokiwi.kiwi" to "Kiwi UPI",
    "com.nextbillion.groww" to "Groww",
    "com.zerodha.kite3" to "Zerodha",
    "in.indmoney" to "INDmoney",

    // Major Public & Private Sector Banks
    "com.snapwork.hdfc" to "HDFC Bank",
    "com.sbi.lotusintouch" to "YONO SBI",
    "com.sbi.SBIFreedomPlus" to "SBI Anywhere",
    "com.icicibank.mobile" to "iMobile Pay",
    "com.axis.mobile" to "Axis Mobile",
    "com.msf.kbank.mobile" to "Kotak 811",
    "com.canarabank.mobility" to "Canara ai1",
    "com.infrasofttech.canarabank" to "Canara Bank",
    "com.fss.ubiretail" to "Union Bank Vyom",
    "com.pnb.pnbone" to "PNB ONE",
    "com.bankofbaroda.mconnect" to "BOB World",
    "com.fedmobile" to "Federal Bank FedMobile",
    "com.idfcfirstbank.optimus" to "IDFC FIRST Bank",
    "com.rblbank.mobank" to "RBL MoBank",
    "com.yesbank" to "YES Bank iris",
    "com.indusind.mobile" to "IndusMobile",
    "com.sc.breeze.in" to "Standard Chartered IN",
    "com.hsbc.in.hsbcpersonalbanking" to "HSBC India",
    "com.dbs.in.dbsmbank" to "DBS digibank",
    "com.bankofindia.mbank" to "Bank of India Mobile",
    "com.cbo.mbank" to "Central Bank of India",
    "com.indianbank.mobile" to "IndOASIS",
    "com.aubank.mobile" to "AU 0101",
    "com.equitas.mobile" to "Equitas Bank",
    "com.ujjivan.mobile" to "Ujjivan Small Finance",
    "com.bandhan.mbandhan" to "Bandhan Bank"
  )

  // Package name keywords that indicate a financial or banking application
  private val FINANCIAL_PACKAGE_KEYWORDS = listOf(
    "upi", "bank", "mobilebanking", "payment", "wallet", "paisa", "money",
    "pay", "cred", "fintech", "finance", "card", "bharatpe", "neobank"
  )

  // Blacklist packages that should never be processed as financial notifications
  private val SYSTEM_BLACKLIST_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.vending",
    "com.google.android.googlequicksearchbox",
    "com.google.android.youtube",
    "com.google.android.gm", // Gmail handled separately if configured, avoid notification noise
    "com.spotify.music",
    "com.netflix.ninja",
    "com.instagram.android",
    "com.facebook.katana",
    "com.twitter.android",
    "com.snapchat.android"
  )

  // Signals that an incoming notification represents an actual money transaction (spend or receive)
  private val FINANCIAL_TRANSACTION_INDICATOR = Regex(
    "(?i)(?:rs\\.?|inr|₹)\\s*[0-9,]+.*(?:debited|credited|paid|spent|transferred|withdrawn|charged|purchase|refund|received|sent|deposited|added\\s*to)"
  )
  private val FINANCIAL_TRANSACTION_INDICATOR_REVERSE = Regex(
    "(?i)(?:debited|credited|paid|spent|transferred|withdrawn|charged|purchase|refund|received|sent|deposited|added\\s*to).*(?:rs\\.?|inr|₹)\\s*[0-9,]+"
  )

  /**
   * Determines if a notification from the given package or with the given content
   * represents a valid financial/payment app or monetary transaction.
   */
  fun isFinancialNotification(packageName: String, notificationContent: String): Boolean {
    val pkg = packageName.lowercase()

    // 1. Immediately drop blacklisted system / non-financial apps
    if (SYSTEM_BLACKLIST_PACKAGES.contains(pkg)) {
      return false
    }

    // 2. Exact match against known banking and UPI packages
    if (KNOWN_PACKAGES.containsKey(pkg)) {
      return true
    }

    // 3. Package name semantic check (e.g. contains 'bank', 'upi', 'pay')
    val isFinancialPackage = FINANCIAL_PACKAGE_KEYWORDS.any { keyword ->
      pkg.contains(keyword)
    }
    if (isFinancialPackage) {
      return true
    }

    // 4. Any app on the device that posts a clear financial spend or receive transaction
    val hasMoneySignal = FINANCIAL_TRANSACTION_INDICATOR.containsMatchIn(notificationContent) ||
      FINANCIAL_TRANSACTION_INDICATOR_REVERSE.containsMatchIn(notificationContent)

    return hasMoneySignal
  }

  /**
   * Backward-compatible check for tests or simple package checks.
   */
  fun isSupported(packageName: String): Boolean {
    val pkg = packageName.lowercase()
    if (SYSTEM_BLACKLIST_PACKAGES.contains(pkg)) return false
    if (KNOWN_PACKAGES.containsKey(pkg)) return true
    return FINANCIAL_PACKAGE_KEYWORDS.any { pkg.contains(it) }
  }

  /**
   * Resolves the human-readable application name using the device's PackageManager
   * (e.g., "Google Pay", "Canara ai1", "Fi Money"), falling back to known registry or package.
   */
  fun getAppName(context: Context, packageName: String): String {
    // 1. Try resolving installed app label on device
    try {
      val pm = context.packageManager
      val appInfo = pm.getApplicationInfo(packageName, 0)
      val label = pm.getApplicationLabel(appInfo).toString()
      if (label.isNotBlank()) {
        return label
      }
    } catch (_: Exception) {
      // Not installed or lookup failed
    }

    // 2. Check known registry
    val known = KNOWN_PACKAGES[packageName.lowercase()]
    if (known != null) return known

    // 3. Clean fallback from package name (e.g., "com.bank.xyz" -> "Bank Xyz")
    val lastSegment = packageName.substringAfterLast('.').replace('_', ' ')
    return lastSegment.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
  }

  /**
   * Backward-compatible method without Context.
   */
  fun getAppName(packageName: String): String {
    return KNOWN_PACKAGES[packageName.lowercase()] ?: packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
  }

  fun isNotificationAccessGranted(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (TextUtils.isEmpty(flat)) return false
    val names = flat.split(":").toTypedArray()
    for (name in names) {
      val cn = ComponentName.unflattenFromString(name)
      if (cn != null && TextUtils.equals(context.packageName, cn.packageName)) {
        return true
      }
    }
    return false
  }
}
