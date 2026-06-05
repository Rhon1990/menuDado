package com.menudado.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&\u00a8\u0006\u0005"}, d2 = {"Lcom/menudado/data/MenuDadoDatabase;", "Landroidx/room/RoomDatabase;", "()V", "menuDao", "Lcom/menudado/data/MenuDao;", "app_debug"})
@androidx.room.Database(entities = {com.menudado.data.MenuEntity.class}, version = 2, exportSchema = false)
public abstract class MenuDadoDatabase extends androidx.room.RoomDatabase {
    
    public MenuDadoDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.menudado.data.MenuDao menuDao();
}