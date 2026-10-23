package de.florianisme.wakeonlan.persistence.mapper;

import de.florianisme.wakeonlan.persistence.entities.DeviceEntity;
import de.florianisme.wakeonlan.persistence.models.Device;

public class DeviceEntityMapper implements EntityMapper<Device, DeviceEntity> {

    @Override
    public Device entityToModel(DeviceEntity entity) {
        if (entity == null) {
            return new Device();
        }
        Device device = new Device(entity.id, entity.name, entity.macAddress, entity.broadcastAddress, entity.port, entity.statusIp, entity.secureOnPassword,
                entity.enableRemoteShutdown, entity.sshAddress, entity.sshPort, entity.sshUsername, entity.sshPassword, entity.sshCommand);
        device.statusPort = entity.statusPort;
        return device;
    }

    @Override
    public DeviceEntity modelToEntity(Device model) {
        if (model == null) {
            return new DeviceEntity();
        }
        DeviceEntity entity = new DeviceEntity(model.id, model.name, model.macAddress, model.broadcastAddress, model.port, model.statusIp, model.secureOnPassword,
                model.remoteShutdownEnabled, model.sshAddress, model.sshPort, model.sshUsername, model.sshPassword, model.sshCommand);
        entity.statusPort = model.statusPort;
        return entity;
    }
}
