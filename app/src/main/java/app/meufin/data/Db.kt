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
    @Insert(onConflict = OnCo