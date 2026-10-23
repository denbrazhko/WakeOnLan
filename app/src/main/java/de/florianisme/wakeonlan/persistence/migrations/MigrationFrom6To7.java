package de.florianisme.wakeonlan.persistence.migrations;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class MigrationFrom6To7 extends Migration {
    public MigrationFrom6To7() { super(6, 7); }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE 'Devices' ADD COLUMN 'ssh_use_key' INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE 'Devices' ADD COLUMN 'ssh_private_key_encrypted' TEXT");
        database.execSQL("ALTER TABLE 'Devices' ADD COLUMN 'ssh_key_passphrase_encrypted' TEXT");
        database.execSQL("ALTER TABLE 'Devices' ADD COLUMN 'ssh_host_fingerprint' TEXT");
    }
}
