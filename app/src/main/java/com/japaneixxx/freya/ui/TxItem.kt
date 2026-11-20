package com.japaneixxx.freya.ui

import com.japaneixxx.freya.data.Clean
import com.japaneixxx.freya.data.IGNORED_CATEGORIES
import com.japaneixxx.freya.data.Tx
import com.japaneixxx.freya.data.isManualAccountId
import com.japaneixxx.freya.data.sanitize

data class TxItem(
    val tx: Tx,
    val category: String,
    val note: String = "",
    val metaIgnored: Boolean = false
) {
    val ignored: Boolean get() = metaIgnored || category in IGNORED_CATEGORIES
    val manual: Boolean get() = isManualAccountId(tx.accountId)

    /** Nome sem texto redundante do banco + tipo (Pix, débito, fatura...) como sinalização. */
    val clean: Clean by lazy(LazyThreadSafetyMode.NONE) { sanitize(tx.description, tx.amount) }
}
