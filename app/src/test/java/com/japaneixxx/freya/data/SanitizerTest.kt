package com.japaneixxx.freya.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SanitizerTest {
    @Test fun pixDirectionAndName() {
        assertEquals(Clean("Joao Silva", TxKind.PIX_SENT), sanitize("PIX ENVIADO JOAO SILVA", -10.0))
        assertEquals(Clean("Maria", TxKind.PIX_RECEIVED), sanitize("Pix recebido Maria", 20.0))
    }

    @Test fun debitPurchaseAndCardBill() {
        assertEquals(Clean("Padaria Sao Joao", TxKind.DEBIT_PURCHASE), sanitize("COMPRA NO DEBITO PADARIA SAO JOAO", -12.0))
        assertEquals(TxKind.CARD_BILL, sanitize("PAGAMENTO FATURA CARTAO", -500.0).kind)
    }

    @Test fun installmentIsExtractedFromTheName() {
        val c = sanitize("LOJA XYZ PARC 03/10", -100.0)
        assertEquals("Loja Xyz", c.title); assertEquals("3/10", c.parcel); assertNull(c.kind)
        assertEquals("3/10", sanitize("NETFLIX.COM 03/10", -39.9).parcel)
    }

    @Test fun transferAndWithdrawal() {
        assertEquals(TxKind.TRANSFER_RECEIVED, sanitize("TED RECEBIDA EMPRESA", 1000.0).kind)
        assertEquals(TxKind.WITHDRAWAL, sanitize("SAQUE 24H", -100.0).kind)
    }

    @Test fun kindMapsToCategories() {
        assertEquals(CAT_CARD_PAYMENT, kindCategory(TxKind.CARD_BILL))
        assertEquals(CAT_OWN_TRANSFER, kindCategory(TxKind.APPLY))
        assertEquals("Transferências", kindCategory(TxKind.PIX_SENT))
        assertEquals("Receitas", kindCategory(TxKind.SALARY))
        assertNull(kindCategory(TxKind.DEBIT_PURCHASE))
        assertNull(kindCategory(null))
    }
}
