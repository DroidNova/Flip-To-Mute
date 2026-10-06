package com.droidnova.fliptomute.billing

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.data.premium.PremiumStore
import com.droidnova.fliptomute.data.premium.premiumActive
import java.security.KeyPairGenerator
import java.security.Signature
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** "Remove ads" (future features F19): the remembered purchase and the signature check. */
@RunWith(RobolectricTestRunner::class)
class PremiumTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun adsShowUntilAPurchaseIsRemembered_andStayAwayAfterARestart() {
        assertFalse(PremiumStore(context).hasBoughtPremium)
        PremiumStore(context).hasBoughtPremium = true
        assertTrue(PremiumStore(context).hasBoughtPremium)
        // Play said the purchase is gone (a refund): ads return
        PremiumStore(context).hasBoughtPremium = false
        assertFalse(PremiumStore(context).hasBoughtPremium)
    }

    @Test fun premiumNeedsTheSwitchAndAPurchase() {
        assertTrue(premiumActive(enabled = true, bought = true, debugSession = false))
        assertTrue(premiumActive(enabled = true, bought = false, debugSession = true))
        assertFalse(premiumActive(enabled = true, bought = false, debugSession = false))
        // Switched off: nobody is treated as a buyer, whatever was stored
        assertFalse(premiumActive(enabled = false, bought = true, debugSession = true))
    }

    @Test fun aPurchaseSignedByPlayIsAccepted_andATamperedOneIsNot() {
        val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKey = Base64.encodeToString(keys.public.encoded, Base64.NO_WRAP)
        val data = """{"productId":"one_time_remove_ads","purchaseState":0}"""
        val signature = Signature.getInstance("SHA1withRSA").run {
            initSign(keys.private)
            update(data.toByteArray())
            Base64.encodeToString(sign(), Base64.NO_WRAP)
        }

        assertTrue(Security.verifyPurchase(publicKey, data, signature))
        assertFalse(Security.verifyPurchase(publicKey, data.replace("remove_ads", "everything"), signature))
        assertFalse(Security.verifyPurchase(publicKey, data, "not-a-signature"))
        assertFalse(Security.verifyPurchase("", data, signature))
    }
}
