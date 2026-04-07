package com.phuc.synctask.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.phuc.synctask.model.Task;

/**
 * Lớp cơ sở dữ liệu Room chính của ứng dụng SyncTask.
 *
 * Sử dụng Singleton Pattern (Double-Checked Locking) để đảm bảo
 * chỉ có duy nhất một instance database trong toàn bộ ứng dụng.
 *
 * Version 2: Thêm trường projectName vào Task entity.
 * Dùng fallbackToDestructiveMigration() trong MVP để tự động reset DB khi schema thay đổi.
 */
@Database(entities = {Task.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Tên file cơ sở dữ liệu SQLite
    private static final String DATABASE_NAME = "synctask_db";

    // Instance duy nhất của database (volatile đảm bảo thread-safe)
    private static volatile AppDatabase INSTANCE;

    /**
     * Phương thức abstract để Room tự động sinh implementation cho TaskDao.
     *
     * @return Đối tượng TaskDao
     */
    public abstract TaskDao taskDao();

    /**
     * Lấy instance duy nhất của AppDatabase (Singleton).
     *
     * @param context Context của ứng dụng (nên dùng ApplicationContext)
     * @return Instance duy nhất của AppDatabase
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME)
                            // Cho phép truy vấn trên Main Thread (chỉ dùng cho MVP/demo)
                            .allowMainThreadQueries()
                            // Tự động xóa DB cũ khi schema thay đổi (MVP only)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
