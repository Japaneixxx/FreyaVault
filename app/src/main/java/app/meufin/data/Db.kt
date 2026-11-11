package app.meufin.data

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

/** Conta a pagar recorrente (aluguel, escola, internet...). */
@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val lastPaidMonth: String = ""   // "2026-11" quando paga no mês
)

/** Foto diária do saldo, para o gráfico de evolução. */
@Entity(tableName = "snapshots")
data class Snapshot(@PrimaryKey val date: String, val bank: Double, val cards: Double)

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

    // leituras pontuais (alertas em segundo plano e backup)
    @Query("SELECT * FROM accounts") suspend fun accountsNow(): List<Account>
    @Query("SELECT * FROM transactions") suspend fun txNow(): List<Tx>
    @Query("SELECT * FROM transactions WHERE accountId = 'manual'") suspend fun manualTxNow(): List<Tx>
    @Query("SELECT * FROM rules") suspend fun rulesNow(): List<Rule>
    @Query("SELECT * FROM budgets") suspend fun budgetsNow(): List<Budget>
    @Query("SELECT * FROM goals") suspend fun goalsNow(): List<Goal>
    @Query("SELECT * FROM tx_meta") suspend fun metasNow(): List<TxMeta>
    @Query("SELECT * FROM bills") suspend fun billsNow(): List<Bill>

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

@Database(
    entities = [Account::class, Tx::class, Rule::class, Budget::class, Goal::class,
        TxMeta::class, Bill::class, Snapshot::class],
    version = 3, exportSchema = false
)
abstract class Db : RoomDatabase() {
    abstract fun dao(): FinDao

    companion object {
        @Volatile private var inst: Db? = null
        fun get(ctx: Context): Db = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, Db::class.java, "fin.db")
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigrationFrom(1) // v1 só tinha dados re-sincronizáveis
                .build().also { inst = it }
        }
    }
}
