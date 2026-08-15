package com.ffswitcher.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Account(val uid: String, val password: String)

object AccountStore {

    /**
     * Reads the accounts JSON file (same format used by the Telegram
     * bot version: a JSON array of objects, each with at least "uid"
     * and "password" fields - extra fields like jwt/open_id are ignored).
     */
    fun loadAccounts(path: String): List<Account> {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalStateException("Accounts file not found: $path")
        }
        val text = file.readText(Charsets.UTF_8)
        val root = JSONArray(text.trim().let {
            if (it.startsWith("[")) it else "[$it]"
        })

        val result = mutableListOf<Account>()
        for (i in 0 until root.length()) {
            val obj: JSONObject = root.optJSONObject(i) ?: continue
            if (obj.has("uid") && obj.has("password")) {
                result.add(
                    Account(
                        uid = obj.optString("uid", "N/A"),
                        password = obj.optString("password", "N/A")
                    )
                )
            }
        }
        return result
    }

    /**
     * Writes the given uid/password into the Free Fire guest cache file,
     * in the exact format the game's "Recover" reads:
     *   {"guest_account_info":{"com.garena.msdk.guest_password":"...","com.garena.msdk.guest_uid":"..."}}
     *
     * Uses an atomic write (temp file + rename) so the game never sees
     * a half-written file.
     */
    fun writeCache(path: String, uid: String, password: String): Result<Unit> {
        return try {
            val payload = JSONObject()
            val inner = JSONObject()
            inner.put("com.garena.msdk.guest_password", password)
            inner.put("com.garena.msdk.guest_uid", uid)
            payload.put("guest_account_info", inner)

            val target = File(path)
            val tmp = File(path + ".tmp")
            tmp.writeText(payload.toString(), Charsets.UTF_8)

            if (!tmp.renameTo(target)) {
                // renameTo can fail across filesystems - fall back to copy+delete
                target.writeText(payload.toString(), Charsets.UTF_8)
                tmp.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
