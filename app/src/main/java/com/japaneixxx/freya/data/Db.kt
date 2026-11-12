package com.japaneixxx.freya.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey val id: String,
    val itemId: String,
    val institution: String,
    val name: String,
    val type: String,      // BANK | CREDIT
    val subtype: String,
    val balance: Double,
    val currency: String,
    val creditLimit: Double? = null,
    val availableLimit: Double? = null,
    val dueDate: String? = null   // yyyy-MM-dd (vencimento da fatura)
)

@Entity(tableName = "transactions", indices = [Index("accountId"), Index("date")])
data class Tx(
    @PrimaryKey val id: String,
    val accountId: String,
    val date: String,      // yyyy-MM-dd
    val description: String,
    val amount: Double,    // + entrada, - saída
    val pluggyCategory: String,
    val pending: Boolean
)

/** Regra aprendida: descrição normalizada -> categoria escolhida pelo usuário. */
@Entity(tableName = "rules")
data class Rule(@PrimaryKey val ruleKey: String, val category: String)

@Entity(tableName = "budgets")
data class Budget(@PrimaryKey val category: String, val limitAmount: Double)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Double,
    val saved: Double = 0.0
)

/** Anotação e "ignorar nos totais" por transação. */
@Entity(tableName = "tx_meta")
data class TxMeta(@PrimaryKey val txId: String, val note: String = "", val ignored: Boolean = false)

/**
 * Compromisso mensal: conta fixa (internet), variável (luz, água) ou financiamento com parcelas definidas.
 * [kind] nulo = fixa (dados antigos).
 */
@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,              // fixa: valor; variável: estimativa inicial; financiamento: valor da parcela
    val dueDay: Int,
    val lastPaidMonth: String = "",  // "2026-11" quando paga no mês
    val kind: String? = null,        // FIXED | VARIABLE | INSTALLMENT
    val totalInstallments: Int? = null,
    val firstMonth: String? = null,  // mês da 1ª parcela (yyyy-MM)
    val matchText: String? = null    // palavra do extrato usada para dar baixa automática
)

/** Quanto realmente foi pago em cada mês (base da média das contas variáveis). */
@Entity(tableName = "bill_payments", primaryKeys = ["billId", "month"])
data class BillPayment(val billId: Long, val month: String, val amount: Double)

/** Receita prevista (salário etc.): dia fixo do mês ou N-ésimo dia útil. */
@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val day: Int,
    val businessDay: Boolean = false
)

/** Foto diária do saldo, para o gráfico de evolução. */
@Entity(tableName = "snapshots")
data class Snapshot(@PrimaryKey val date: String, val bank: Double, val cards: Double)

/** Investimento trazido pelo Pluggy (renda fixa, fundos, ações...). */
@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey val id: String,
    val itemId: String,
    val institution: String,
    val name: String,
    val type: String,
    val balance: Double,
    val amountOriginal: Double? = null,   // quanto foi aplicado
    val amountProfit: Double? = null,     // lucro acumulado
    val annualRate: Double? = null,       // % ao ano
    val lastTwelveMonthsRate: Double? = null,
    val currency: String = "BRL"
)

/** Bem (ASSET) ou dívida (DEBT) cadastrado à mão: carro, imóvel, financiamento... */
@Entity(tableName = "net_items")
data class NetItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,        // ASSET | DEBT
    val amount: Double
)

/** Categoria com orçamento em modo envelope: a sobra de cada mês acumula para o seguinte. */
@Entity(tableName = "envelopes")
data class Envelope(@PrimaryKey val category: String, val startMonth: String)

/** Foto diária do patrimônio líquido, para o gráfico de evolução. */
@Entity(tableName = "net_snapshots")
data class NetSnapshot(@PrimaryKey val date: String, val total: Double)

@Dao
interface FinDao {
    @Query("SELECT * FROM accounts ORDER BY institution, name") fun accounts(): Flow<List<Account>>
    @Query("SELECT * FROM transactions ORDER BY date DESC, id") fun transactions(): Flow<List<Tx>>
    @Query("SELECT * FROM rules") fun rules(): Flow<List<Rule>>
    @Query("SELECT * FROM budgets") fun budgets(): Flow<List<Budget>>
    @Query("SELECT * FROM goals ORDER BY id") fun goals(): Flow<List<Goal>>
    @Query("SELECT * FROM tx_meta") fun metas(): Flow<List<TxMeta>>
    @Query("SELECT * FROM bills ORDER BY dueDay, name") fun bills(): Flow<List<Bill>>
    @Query("SELECT * FROM snapshots ORDER BY date") fun snapshots(): Flow<List<Snapshot>>
    @Query("SELECT * FROM investments ORDER BY balance DESC") fun investments(): Flow<List<Investment>>
    @Query("SELECT * FROM net_items ORDER BY kind, name") fun netItems(): Flow<List<NetItem>>
    @Query("SELECT * FROM envelopes") fun envelopes(): Flow<List<Envelope>>
    @Query("SELECT * FROM net_snapshots ORDER BY date") fun netSnapshots(): Flow<List<NetSnapshot>>
    @Query("SELECT * FROM bill_payments") fun billPayments(): Flow<List<BillPayment>>
    @Query("SELECT * FROM incomes ORDER BY day, name") fun incomes(): Flow<List<Income>>

    // leituras pontuais (alertas em segundo plano e backup)
    @Query("SELECT * FROM accounts") suspend fun accountsNow(): List<Account>
    @Query("SELECT * FROM transactions") suspend fun txNow(): List<Tx>
    @Query("SELECT * FROM transactions WHERE accountId = 'manual'") suspend fun manualTxNow(): List<Tx>
    @Query("SELECT * FROM rules") suspend fun rulesNow(): List<Rule>
    @Query("SELECT * FROM budgets") suspend fun budgetsNow(): List<Budget>
    @Query("SELECT * FROM goals") suspend fun goalsNow(): List<Goal>
    @Query("SELECT * FROM tx_meta") suspend fun metasNow(): List<TxMeta>
    @Query("SELECT * FROM bills") suspend fun billsNow(): List<Bill>
    @Query("SELECT * FROM investments") suspend fun investmentsNow(): List<Investment>
    @Query("SELECT * FROM net_items") suspend fun netItemsNow(): List<NetItem>
    @Query("SELECT * FROM envelopes") suspend fun envelopesNow(): List<Envelope>
    @Query("SELECT * FROM bill_payments") suspend fun billPaymentsNow(): List<BillPayment>
    @Query("SELECT * FROM incomes") suspend fun incomesNow(): List<Income>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAccounts(list: List<Account>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTx(list: List<Tx>): List<Long>
    @Update suspend fun updateTx(list: List<Tx>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTx(tx: Tx)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRule(rule: Rule)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBudget(b: Budget)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoal(g: Goal)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeta(m: TxMeta)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBill(b: Bill)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSnapshot(s: Snapshot)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertInvestments(list: List<Investment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putNetItem(i: NetItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEnvelope(e: Envelope)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putNetSnapshot(s: NetSnapshot)
    @Query("DELETE FROM investments WHERE itemId = :itemId") suspend fun deleteInvestmentsOf(itemId: String)
    @Query("DELETE FROM net_items WHERE id = :id") suspend fun deleteNetItem(id: Long)
    @Query("DELETE FROM envelopes WHERE category = :c") suspend fun deleteEnvelope(c: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBillPayment(p: BillPayment)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putIncome(i: Income)
    @Query("DELETE FROM bill_payments WHERE billId = :billId AND month = :month") suspend fun deleteBillPayment(billId: Long, month: String)
    @Query("DELETE FROM bill_payments WHERE billId = :billId") suspend fun deletePaymentsOf(billId: Long)
    @Query("DELETE FROM incomes WHERE id = :id") suspend fun deleteIncome(id: Long)
    @Query("DELETE FROM bill_payments") suspend fun clearBillPayments()
    @Query("DELETE FROM incomes") suspend fun clearIncomes()
    @Query("DELETE FROM investments") suspend fun clearInvestments()
    @Query("DELETE FROM net_items") suspend fun clearNetItems()
    @Query("DELETE FROM envelopes") suspend fun clearEnvelopes()
    @Query("DELETE FROM net_snapshots") suspend fun clearNetSnapshots()

    @Query("SELECT MAX(date) FROM transactions WHERE accountId = :accountId")
    suspend fun lastDate(accountId: String): String?

    @Query("DELETE FROM transactions WHERE accountId = :accountId AND pending = 1")
    suspend fun deletePending(accountId: String)

    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteTx(id: String)
    @Query("DELETE FROM rules WHERE ruleKey = :key") suspend fun deleteRule(key: String)
    @Query("DELETE FROM budgets WHERE category = :c") suspend fun deleteBudget(c: String)
    @Query("DELETE FROM goals WHERE id = :id") suspend fun deleteGoal(id: Long)
    @Query("DELETE FROM bills WHERE id = :id") suspend fun deleteBill(id: Long)

    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM transactions") suspend fun clearTx()
    @Query("DELETE FROM rules") suspend fun clearRules()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM tx_meta") suspend fun clearMetas()
    @Query("DELETE FROM bills") suspend fun clearBills()
    @Query("DELETE FROM snapshots") suspend fun clearSnapshots()
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimit REAL")
        db.execSQL("ALTER TABLE accounts ADD COLUMN availableLimit REAL")
        db.execSQL("ALTER TABLE accounts ADD COLUMN dueDate TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `amount` REAL NOT NULL, `dueDay` INTEGER NOT NULL, " +
                "`lastPaidMonth` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `snapshots` (`date` TEXT NOT NULL, `bank` REAL NOT NULL, " +
                "`cards` REAL NOT NULL, PRIMARY KEY(`date`))"
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `investments` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, " +
                "`institution` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `balance` REAL NOT NULL, " +
                "`amountOriginal` REAL, `amountProfit` REAL, `annualRate` REAL, `lastTwelveMonthsRate` REAL, " +
                "`currency` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `net_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `amount` REAL NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `envelopes` (`category` TEXT NOT NULL, `startMonth` TEXT NOT NULL, " +
                "PRIMARY KEY(`category`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `net_snapshots` (`date` TEXT NOT NULL, `total` REAL NOT NULL, " +
                "PRIMARY KEY(`date`))"
        )
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bills ADD COLUMN kind TEXT")
        db.execSQL("ALTER TABLE bills ADD COLUMN totalInstallments INTEGER")
        db.execSQL("ALTER TABLE bills ADD COLUMN firstMonth TEXT")
        db.execSQL("ALTER TABLE bills ADD COLUMN matchText TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bill_payments` (`billId` INTEGER NOT NULL, `month` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, PRIMARY KEY(`billId`, `month`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `incomes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `amount` REAL NOT NULL, `day` INTEGER NOT NULL, `businessDay` INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [Account::class, Tx::class, Rule::class, Budget::class, Goal::class,
        TxMeta::class, Bill::class, Snapshot::class, Investment::class, NetItem::class,
        Envelope::class, NetSnapshot::class, BillPayment::class, Income::class],
    version = 5, exportSchema = false
)
abstract class Db : RoomDatabase() {
    abstract fun dao(): FinDao

    companion object {
        @Volatile private var inst: Db? = null
        fun get(ctx: Context): Db = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, Db::class.java, "fin.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigrationFrom(1) // v1 só tinha dados re-sincronizáveis
                .build().also { inst = it }
        }
    }
}
