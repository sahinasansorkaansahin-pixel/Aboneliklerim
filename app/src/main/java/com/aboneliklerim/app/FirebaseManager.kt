package com.aboneliklerim.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val lastSyncDate: String = ""
)

object FirebaseManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    init {
        // Step 4: Enable Offline Persistence (Kotlin implementation)
        // Firestore enables persistence by default on Android, 
        // but we can explicitly set cache size if needed.
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    /**
     * Step 1 & 2: Save User Profile and Subscriptions to Firestore
     */
    fun syncDataToCloud(
        context: Context,
        subscriptions: List<Subscription>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure(Exception("User not logged in."))
            return
        }
        
        val syncDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val userRef = db.collection("users").document(uid)
        
        // Prepare data to save
        val data = hashMapOf(
            "profile" to UserProfile(
                name = auth.currentUser?.displayName ?: "User",
                email = auth.currentUser?.email ?: "",
                lastSyncDate = syncDate
            ),
            "subscriptions" to subscriptions
        )

        userRef.set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Step 3: Fetch Data from Firestore after re-install or new device
     */
    fun fetchDataFromCloud(
        onResult: (List<Subscription>?, UserProfile?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure(Exception("User not logged in."))
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Fetch profile
                    val profileMap = document.get("profile") as? Map<String, Any>
                    val profile = profileMap?.let {
                        UserProfile(
                            name = it["name"] as? String ?: "",
                            email = it["email"] as? String ?: "",
                            lastSyncDate = it["lastSyncDate"] as? String ?: ""
                        )
                    }

                    // Fetch subscriptions
                    val subsList = document.get("subscriptions") as? List<Map<String, Any>>
                    val subscriptions = subsList?.map { map ->
                        Subscription(
                            id = map["id"] as? String ?: UUID.randomUUID().toString(),
                            name = map["name"] as? String ?: "",
                            price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                            period = map["period"] as? String ?: "monthly",
                            category = map["category"] as? String ?: "",
                            startDate = map["startDate"] as? String ?: "",
                            currency = map["currency"] as? String ?: "TRY",
                            color = map["color"] as? String ?: "#6366f1",
                            notifyDaysBefore = (map["notifyDaysBefore"] as? Number)?.toInt() ?: 3,
                            notifyTime = map["notifyTime"] as? String ?: "09:00",
                            note = map["note"] as? String ?: "",
                            paymentMethod = map["paymentMethod"] as? String ?: "",
                            isArchived = map["isArchived"] as? Boolean ?: false,
                            sharedWith = (map["sharedWith"] as? Number)?.toInt() ?: 1,
                            sharedContacts = (map["sharedContacts"] as? List<Map<String, Any>>)?.map { contactMap ->
                                SharedContact(
                                    name = contactMap["name"] as? String ?: "",
                                    phoneNumber = contactMap["phoneNumber"] as? String ?: ""
                                )
                            } ?: emptyList()
                        )
                    }
                    onResult(subscriptions, profile)
                } else {
                    onResult(null, null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteCloudData(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure(Exception("User not logged in."))
            return
        }

        db.collection("users").document(uid).update("subscriptions", FieldValue.delete())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // --- GLOBAL CURRENCY CACHE (FIRESTORE) ---
    
    fun getGlobalCurrencyRates(onResult: (Map<String, Double>?, Long) -> Unit) {
        db.collection("global").document("currency_rates").get()
            .addOnSuccessListener { doc ->
                if (documentExists(doc)) {
                    val rates = doc.get("rates") as? Map<String, Double>
                    val timestamp = doc.getLong("last_update") ?: 0L
                    onResult(rates, timestamp)
                } else {
                    onResult(null, 0L)
                }
            }
            .addOnFailureListener { onResult(null, 0L) }
    }

    fun saveGlobalCurrencyRates(rates: Map<String, Double>) {
        val data = hashMapOf(
            "rates" to rates,
            "last_update" to System.currentTimeMillis()
        )
        db.collection("global").document("currency_rates").set(data, SetOptions.merge())
    }

    private fun documentExists(doc: com.google.firebase.firestore.DocumentSnapshot): Boolean = doc.exists()
}
