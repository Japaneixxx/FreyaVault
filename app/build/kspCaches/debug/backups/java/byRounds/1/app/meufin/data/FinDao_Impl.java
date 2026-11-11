package app.meufin.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FinDao_Impl implements FinDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Account> __insertionAdapterOfAccount;

  private final EntityInsertionAdapter<Tx> __insertionAdapterOfTx;

  private final EntityInsertionAdapter<Rule> __insertionAdapterOfRule;

  private final EntityInsertionAdapter<Budget> __insertionAdapterOfBudget;

  private final EntityInsertionAdapter<Goal> __insertionAdapterOfGoal;

  private final EntityInsertionAdapter<TxMeta> __insertionAdapterOfTxMeta;

  private final EntityInsertionAdapter<Tx> __insertionAdapterOfTx_1;

  private final EntityDeletionOrUpdateAdapter<Tx> __updateAdapterOfTx;

  private final SharedSQLiteStatement __preparedStmtOfDeletePending;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBudget;

  private final SharedSQLiteStatement __preparedStmtOfDeleteGoal;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTx;

  private final SharedSQLiteStatement __preparedStmtOfClearBudgets;

  private final SharedSQLiteStatement __preparedStmtOfClearGoals;

  private final SharedSQLiteStatement __preparedStmtOfClearMetas;

  private final SharedSQLiteStatement __preparedStmtOfClearAccounts;

  private final SharedSQLiteStatement __preparedStmtOfClearTx;

  private final SharedSQLiteStatement __preparedStmtOfClearRules;

  public FinDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAccount = new EntityInsertionAdapter<Account>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `accounts` (`id`,`itemId`,`institution`,`name`,`type`,`subtype`,`balance`,`currency`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Account entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getItemId());
        statement.bindString(3, entity.getInstitution());
        statement.bindString(4, entity.getName());
        statement.bindString(5, entity.getType());
        statement.bindString(6, entity.getSubtype());
        statement.bindDouble(7, entity.getBalance());
        statement.bindString(8, entity.getCurrency());
      }
    };
    this.__insertionAdapterOfTx = new EntityInsertionAdapter<Tx>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `transactions` (`id`,`accountId`,`date`,`description`,`amount`,`pluggyCategory`,`pending`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Tx entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindString(3, entity.getDate());
        statement.bindString(4, entity.getDescription());
        statement.bindDouble(5, entity.getAmount());
        statement.bindString(6, entity.getPluggyCategory());
        final int _tmp = entity.getPending() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__insertionAdapterOfRule = new EntityInsertionAdapter<Rule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `rules` (`ruleKey`,`category`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Rule entity) {
        statement.bindString(1, entity.getRuleKey());
        statement.bindString(2, entity.getCategory());
      }
    };
    this.__insertionAdapterOfBudget = new EntityInsertionAdapter<Budget>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `budgets` (`category`,`limitAmount`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Budget entity) {
        statement.bindString(1, entity.getCategory());
        statement.bindDouble(2, entity.getLimitAmount());
      }
    };
    this.__insertionAdapterOfGoal = new EntityInsertionAdapter<Goal>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `goals` (`id`,`name`,`target`,`saved`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Goal entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getTarget());
        statement.bindDouble(4, entity.getSaved());
      }
    };
    this.__insertionAdapterOfTxMeta = new EntityInsertionAdapter<TxMeta>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tx_meta` (`txId`,`note`,`ignored`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TxMeta entity) {
        statement.bindString(1, entity.getTxId());
        statement.bindString(2, entity.getNote());
        final int _tmp = entity.getIgnored() ? 1 : 0;
        statement.bindLong(3, _tmp);
      }
    };
    this.__insertionAdapterOfTx_1 = new EntityInsertionAdapter<Tx>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`accountId`,`date`,`description`,`amount`,`pluggyCategory`,`pending`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Tx entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindString(3, entity.getDate());
        statement.bindString(4, entity.getDescription());
        statement.bindDouble(5, entity.getAmount());
        statement.bindString(6, entity.getPluggyCategory());
        final int _tmp = entity.getPending() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__updateAdapterOfTx = new EntityDeletionOrUpdateAdapter<Tx>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `transactions` SET `id` = ?,`accountId` = ?,`date` = ?,`description` = ?,`amount` = ?,`pluggyCategory` = ?,`pending` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Tx entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindString(3, entity.getDate());
        statement.bindString(4, entity.getDescription());
        statement.bindDouble(5, entity.getAmount());
        statement.bindString(6, entity.getPluggyCategory());
        final int _tmp = entity.getPending() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindString(8, entity.getId());
      }
    };
    this.__preparedStmtOfDeletePending = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions WHERE accountId = ? AND pending = 1";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBudget = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM budgets WHERE category = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteGoal = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM goals WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteTx = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearBudgets = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM budgets";
        return _query;
      }
    };
    this.__preparedStmtOfClearGoals = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM goals";
        return _query;
      }
    };
    this.__preparedStmtOfClearMetas = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tx_meta";
        return _query;
      }
    };
    this.__preparedStmtOfClearAccounts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM accounts";
        return _query;
      }
    };
    this.__preparedStmtOfClearTx = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions";
        return _query;
      }
    };
    this.__preparedStmtOfClearRules = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM rules";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAccounts(final List<Account> list,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAccount.insert(list);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTx(final List<Tx> list, final Continuation<? super List<Long>> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        __db.beginTransaction();
        try {
          final List<Long> _result = __insertionAdapterOfTx.insertAndReturnIdsList(list);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putRule(final Rule rule, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRule.insert(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putBudget(final Budget b, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBudget.insert(b);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putGoal(final Goal g, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGoal.insert(g);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putMeta(final TxMeta m, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTxMeta.insert(m);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putTx(final Tx tx, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTx_1.insert(tx);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTx(final List<Tx> list, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTx.handleMultiple(list);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePending(final String accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePending.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeletePending.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBudget(final String c, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBudget.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, c);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteBudget.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteGoal(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteGoal.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteGoal.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTx(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTx.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteTx.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearBudgets(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearBudgets.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearBudgets.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearGoals(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearGoals.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearGoals.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearMetas(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearMetas.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearMetas.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAccounts(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAccounts.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAccounts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearTx(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearTx.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearTx.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearRules(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearRules.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearRules.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Account>> accounts() {
    final String _sql = "SELECT * FROM accounts ORDER BY institution, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"accounts"}, new Callable<List<Account>>() {
      @Override
      @NonNull
      public List<Account> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfInstitution = CursorUtil.getColumnIndexOrThrow(_cursor, "institution");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSubtype = CursorUtil.getColumnIndexOrThrow(_cursor, "subtype");
          final int _cursorIndexOfBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "balance");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final List<Account> _result = new ArrayList<Account>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Account _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpItemId;
            _tmpItemId = _cursor.getString(_cursorIndexOfItemId);
            final String _tmpInstitution;
            _tmpInstitution = _cursor.getString(_cursorIndexOfInstitution);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSubtype;
            _tmpSubtype = _cursor.getString(_cursorIndexOfSubtype);
            final double _tmpBalance;
            _tmpBalance = _cursor.getDouble(_cursorIndexOfBalance);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            _item = new Account(_tmpId,_tmpItemId,_tmpInstitution,_tmpName,_tmpType,_tmpSubtype,_tmpBalance,_tmpCurrency);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Tx>> transactions() {
    final String _sql = "SELECT * FROM transactions ORDER BY date DESC, id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<Tx>>() {
      @Override
      @NonNull
      public List<Tx> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfPluggyCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "pluggyCategory");
          final int _cursorIndexOfPending = CursorUtil.getColumnIndexOrThrow(_cursor, "pending");
          final List<Tx> _result = new ArrayList<Tx>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Tx _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpPluggyCategory;
            _tmpPluggyCategory = _cursor.getString(_cursorIndexOfPluggyCategory);
            final boolean _tmpPending;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPending);
            _tmpPending = _tmp != 0;
            _item = new Tx(_tmpId,_tmpAccountId,_tmpDate,_tmpDescription,_tmpAmount,_tmpPluggyCategory,_tmpPending);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Rule>> rules() {
    final String _sql = "SELECT * FROM rules";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"rules"}, new Callable<List<Rule>>() {
      @Override
      @NonNull
      public List<Rule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRuleKey = CursorUtil.getColumnIndexOrThrow(_cursor, "ruleKey");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final List<Rule> _result = new ArrayList<Rule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Rule _item;
            final String _tmpRuleKey;
            _tmpRuleKey = _cursor.getString(_cursorIndexOfRuleKey);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            _item = new Rule(_tmpRuleKey,_tmpCategory);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object lastDate(final String accountId, final Continuation<? super String> $completion) {
    final String _sql = "SELECT MAX(date) FROM transactions WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            final String _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Budget>> budgets() {
    final String _sql = "SELECT * FROM budgets";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"budgets"}, new Callable<List<Budget>>() {
      @Override
      @NonNull
      public List<Budget> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfLimitAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "limitAmount");
          final List<Budget> _result = new ArrayList<Budget>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Budget _item;
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final double _tmpLimitAmount;
            _tmpLimitAmount = _cursor.getDouble(_cursorIndexOfLimitAmount);
            _item = new Budget(_tmpCategory,_tmpLimitAmount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Goal>> goals() {
    final String _sql = "SELECT * FROM goals ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<List<Goal>>() {
      @Override
      @NonNull
      public List<Goal> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfSaved = CursorUtil.getColumnIndexOrThrow(_cursor, "saved");
          final List<Goal> _result = new ArrayList<Goal>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Goal _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpTarget;
            _tmpTarget = _cursor.getDouble(_cursorIndexOfTarget);
            final double _tmpSaved;
            _tmpSaved = _cursor.getDouble(_cursorIndexOfSaved);
            _item = new Goal(_tmpId,_tmpName,_tmpTarget,_tmpSaved);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TxMeta>> metas() {
    final String _sql = "SELECT * FROM tx_meta";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tx_meta"}, new Callable<List<TxMeta>>() {
      @Override
      @NonNull
      public List<TxMeta> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTxId = CursorUtil.getColumnIndexOrThrow(_cursor, "txId");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "ignored");
          final List<TxMeta> _result = new ArrayList<TxMeta>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TxMeta _item;
            final String _tmpTxId;
            _tmpTxId = _cursor.getString(_cursorIndexOfTxId);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final boolean _tmpIgnored;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIgnored);
            _tmpIgnored = _tmp != 0;
            _item = new TxMeta(_tmpTxId,_tmpNote,_tmpIgnored);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
