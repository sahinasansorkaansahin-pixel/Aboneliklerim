package com.aboneliklerim.app

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest

object IntegrityHelper {

    fun checkIntegrity(context: Context, cloudProjectNumber: Long, callback: (String?, Exception?) -> Unit) {
        val integrityManager = IntegrityManagerFactory.create(context)

        val integrityTokenRequest = IntegrityTokenRequest.builder()
            .setCloudProjectNumber(cloudProjectNumber)
            .build()

        integrityManager.requestIntegrityToken(integrityTokenRequest)
            .addOnSuccessListener { response ->
                val token = response.token()
                callback(token, null)
            }
            .addOnFailureListener { exception ->
                callback(null, exception)
            }
    }
}
