package de.florianisme.wakeonlan.persistence;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;

import org.junit.Test;

import de.florianisme.wakeonlan.persistence.mapper.DeviceEntityMapper;
import de.florianisme.wakeonlan.persistence.models.Device;
import de.florianisme.wakeonlan.ui.backup.model.DeviceBackupModel;

public class StatusPortPersistenceTest {
    @Test
    public void customStatusPortSurvivesEntityMapping() {
        Device device = new Device();
        device.port = 9;
        device.statusPort = 22222;
        DeviceEntityMapper mapper = new DeviceEntityMapper();
        Device restored = mapper.entityToModel(mapper.modelToEntity(device));
        assertEquals(22222, restored.statusPort);
        assertEquals(9, restored.port);
    }

    @Test
    public void customStatusPortSurvivesBackupRoundTrip() {
        Device device = new Device();
        device.statusPort = 22222;
        Gson gson = new Gson();
        String json = gson.toJson(new DeviceBackupModel(device));
        Device restored = gson.fromJson(json, DeviceBackupModel.class).toModel();
        assertEquals(22222, restored.statusPort);
    }

    @Test
    public void oldBackupDefaultsToPort22AndPreservesWolPort() {
        Device restored = new Gson().fromJson(
                "{\"status_ip\":\"example.duckdns.org\",\"port\":9}",
                DeviceBackupModel.class).toModel();
        assertEquals(22, restored.statusPort);
        assertEquals(9, restored.port);
    }

}
