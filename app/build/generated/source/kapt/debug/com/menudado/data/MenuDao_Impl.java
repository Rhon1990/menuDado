package com.menudado.data;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.coroutines.FlowUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Long;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class MenuDao_Impl implements MenuDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<MenuEntity> __insertAdapterOfMenuEntity;

  private final EntityDeleteOrUpdateAdapter<MenuEntity> __deleteAdapterOfMenuEntity;

  private final EntityDeleteOrUpdateAdapter<MenuEntity> __updateAdapterOfMenuEntity;

  public MenuDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfMenuEntity = new EntityInsertAdapter<MenuEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `menus` (`id`,`name`,`mealType`,`description`,`notes`,`healthStatus`,`healthReason`,`healthSuggestion`,`calories`,`lastPickedDate`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final MenuEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getName());
        }
        if (entity.getMealType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getMealType());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getDescription());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getNotes());
        }
        if (entity.getHealthStatus() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getHealthStatus());
        }
        if (entity.getHealthReason() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getHealthReason());
        }
        if (entity.getHealthSuggestion() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getHealthSuggestion());
        }
        if (entity.getCalories() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getCalories());
        }
        if (entity.getLastPickedDate() == null) {
          statement.bindNull(10);
        } else {
          statement.bindText(10, entity.getLastPickedDate());
        }
        statement.bindLong(11, entity.getCreatedAt());
      }
    };
    this.__deleteAdapterOfMenuEntity = new EntityDeleteOrUpdateAdapter<MenuEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `menus` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final MenuEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfMenuEntity = new EntityDeleteOrUpdateAdapter<MenuEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `menus` SET `id` = ?,`name` = ?,`mealType` = ?,`description` = ?,`notes` = ?,`healthStatus` = ?,`healthReason` = ?,`healthSuggestion` = ?,`calories` = ?,`lastPickedDate` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final MenuEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getName());
        }
        if (entity.getMealType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getMealType());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getDescription());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getNotes());
        }
        if (entity.getHealthStatus() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getHealthStatus());
        }
        if (entity.getHealthReason() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getHealthReason());
        }
        if (entity.getHealthSuggestion() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getHealthSuggestion());
        }
        if (entity.getCalories() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getCalories());
        }
        if (entity.getLastPickedDate() == null) {
          statement.bindNull(10);
        } else {
          statement.bindText(10, entity.getLastPickedDate());
        }
        statement.bindLong(11, entity.getCreatedAt());
        statement.bindLong(12, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final MenuEntity menu, final Continuation<? super Long> $completion) {
    if (menu == null) throw new NullPointerException();
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      return __insertAdapterOfMenuEntity.insertAndReturnId(_connection, menu);
    }, $completion);
  }

  @Override
  public Object delete(final MenuEntity menu, final Continuation<? super Unit> $completion) {
    if (menu == null) throw new NullPointerException();
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      __deleteAdapterOfMenuEntity.handle(_connection, menu);
      return Unit.INSTANCE;
    }, $completion);
  }

  @Override
  public Object update(final MenuEntity menu, final Continuation<? super Unit> $completion) {
    if (menu == null) throw new NullPointerException();
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      __updateAdapterOfMenuEntity.handle(_connection, menu);
      return Unit.INSTANCE;
    }, $completion);
  }

  @Override
  public Flow<List<MenuEntity>> observeMenus() {
    final String _sql = "SELECT * FROM menus ORDER BY createdAt DESC";
    return FlowUtil.createFlow(__db, false, new String[] {"menus"}, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfMealType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mealType");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfNotes = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "notes");
        final int _columnIndexOfHealthStatus = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "healthStatus");
        final int _columnIndexOfHealthReason = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "healthReason");
        final int _columnIndexOfHealthSuggestion = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "healthSuggestion");
        final int _columnIndexOfCalories = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "calories");
        final int _columnIndexOfLastPickedDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastPickedDate");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createdAt");
        final List<MenuEntity> _result = new ArrayList<MenuEntity>();
        while (_stmt.step()) {
          final MenuEntity _item;
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          final String _tmpMealType;
          if (_stmt.isNull(_columnIndexOfMealType)) {
            _tmpMealType = null;
          } else {
            _tmpMealType = _stmt.getText(_columnIndexOfMealType);
          }
          final String _tmpDescription;
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription);
          }
          final String _tmpNotes;
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null;
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes);
          }
          final String _tmpHealthStatus;
          if (_stmt.isNull(_columnIndexOfHealthStatus)) {
            _tmpHealthStatus = null;
          } else {
            _tmpHealthStatus = _stmt.getText(_columnIndexOfHealthStatus);
          }
          final String _tmpHealthReason;
          if (_stmt.isNull(_columnIndexOfHealthReason)) {
            _tmpHealthReason = null;
          } else {
            _tmpHealthReason = _stmt.getText(_columnIndexOfHealthReason);
          }
          final String _tmpHealthSuggestion;
          if (_stmt.isNull(_columnIndexOfHealthSuggestion)) {
            _tmpHealthSuggestion = null;
          } else {
            _tmpHealthSuggestion = _stmt.getText(_columnIndexOfHealthSuggestion);
          }
          final Integer _tmpCalories;
          if (_stmt.isNull(_columnIndexOfCalories)) {
            _tmpCalories = null;
          } else {
            _tmpCalories = (int) (_stmt.getLong(_columnIndexOfCalories));
          }
          final String _tmpLastPickedDate;
          if (_stmt.isNull(_columnIndexOfLastPickedDate)) {
            _tmpLastPickedDate = null;
          } else {
            _tmpLastPickedDate = _stmt.getText(_columnIndexOfLastPickedDate);
          }
          final long _tmpCreatedAt;
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
          _item = new MenuEntity(_tmpId,_tmpName,_tmpMealType,_tmpDescription,_tmpNotes,_tmpHealthStatus,_tmpHealthReason,_tmpHealthSuggestion,_tmpCalories,_tmpLastPickedDate,_tmpCreatedAt);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
