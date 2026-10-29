package com.example.bookkeeping

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Static shortcuts clear their target task. Use a separate empty-affinity task
 * so the existing MainActivity and unsaved entry survive shortcut activation. */
class QuickEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent.action
        if (action == "com.example.bookkeeping.QUICK_EXPENSE" || action == "com.example.bookkeeping.QUICK_INCOME") {
            startActivity(Intent(this, MainActivity::class.java).setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        finish()
    }
}
