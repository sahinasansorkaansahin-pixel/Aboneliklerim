package com.aboneliklerim.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object PresenceManager {
    private val database by lazy { FirebaseDatabase.getInstance() }
    private val onlineUsersRef by lazy { database.getReference("online_users") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun startTracking() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.d("PresenceManager", "User not logged in, tracking skipped.")
            return
        }
        
        val userPresenceRef = onlineUsersRef.child(uid)
        val connectedRef = database.getReference(".info/connected")
        
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    userPresenceRef.setValue(true)
                    userPresenceRef.onDisconnect().removeValue()
                    Log.d("PresenceManager", "User $uid is now online.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("PresenceManager", "Connected listener cancelled: ${error.message}")
            }
        })
    }

    fun getOnlineCount(onCountChanged: (Int) -> Unit) {
        onlineUsersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                Log.d("PresenceManager", "Online count updated: $count")
                onCountChanged(if (count > 0) count else 1) // Show at least 1 (the current user)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("PresenceManager", "Online count listener cancelled: ${error.message}")
            }
        })
    }
}
