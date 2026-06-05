package com.menudado.data;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenDelegate;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class MenuDadoDatabase_Impl extends MenuDadoDatabase {
  private volatile MenuDao _menuDao;

  @Override
  @NonNull
  protected RoomOpenDelegate createOpenDelegate() {
    final RoomOpenDelegate _openDelegate = new RoomOpenDelegate(2, "b538c8172685087443a8322f1565fa46", "2e9f2aff68c42b71eec271a5167412e6") {
      @Override
      public void createAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `menus` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `mealType` TEXT NOT NULL, `description` TEXT NOT NULL, `notes` TEXT NOT NULL, `healthStatus` TEXT, `healthReason` TEXT, `healthSuggestion` TEXT, `calories` INTEGER, `lastPickedDate` TEXT, `createdAt` INTEGER NOT NULL)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b538c8172685087443a8322f1565fa46')");
      }

      @Override
      public void dropAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `menus`");
      }

      @Override
      public void onCreate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      public void onOpen(@NonNull final SQLiteConnection connection) {
        internalInitInvalidationTracker(connection);
      }

      @Override
      public void onPreMigrate(@NonNull final SQLiteConnection connection) {
        DBUtil.dropFtsSyncTriggers(connection);
      }

      @Override
      public void onPostMigrate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      @NonNull
      public RoomOpenDelegate.ValidationResult onValidateSchema(
          @NonNull final SQLiteConnection connection) {
        final Map<String, TableInfo.Column> _columnsMenus = new HashMap<String, TableInfo.Column>(11);
        _columnsMenus.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("mealType", new TableInfo.Column("mealType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("healthStatus", new TableInfo.Column("healthStatus", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("healthReason", new TableInfo.Column("healthReason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("healthSuggestion", new TableInfo.Column("healthSuggestion", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("calories", new TableInfo.Column("calories", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("lastPickedDate", new TableInfo.Column("lastPickedDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMenus.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysMenus = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesMenus = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMenus = new TableInfo("menus", _columnsMenus, _foreignKeysMenus, _indicesMenus);
        final TableInfo _existingMenus = TableInfo.read(connection, "menus");
        if (!_infoMenus.equals(_existingMenus)) {
          return new RoomOpenDelegate.ValidationResult(false, "menus(com.menudado.data.MenuEntity).\n"
                  + " Expected:\n" + _infoMenus + "\n"
                  + " Found:\n" + _existingMenus);
        }
        return new RoomOpenDelegate.ValidationResult(true, null);
      }
    };
    return _openDelegate;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final Map<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final Map<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "menus");
  }

  @Override
  public void clearAllTables() {
    super.performClear(false, "menus");
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final Map<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(MenuDao.class, MenuDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final Set<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public MenuDao menuDao() {
    if (_menuDao != null) {
      return _menuDao;
    } else {
      synchronized(this) {
        if(_menuDao == null) {
          _menuDao = new MenuDao_Impl(this);
        }
        return _menuDao;
      }
    }
  }
}
