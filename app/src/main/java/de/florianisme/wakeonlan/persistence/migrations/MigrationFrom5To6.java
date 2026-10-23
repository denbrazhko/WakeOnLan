package de.florianisme.wakeonlan.persistence.migrations;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class MigrationFrom5To6 extends Migration {
    public MigrationFrom5To6() {
        super(5, 6);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE 'Devices' ADD COLUMN 'status_port' INTEGER NOT NULL DEFAULT 22");
    }
}
