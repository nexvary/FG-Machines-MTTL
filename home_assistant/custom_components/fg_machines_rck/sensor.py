from __future__ import annotations

from dataclasses import dataclass

from homeassistant.components.sensor import SensorEntity, SensorDeviceClass, SensorStateClass
from homeassistant.config_entries import ConfigEntry
from homeassistant.const import UnitOfEnergy, UnitOfPower, UnitOfTemperature
from homeassistant.core import HomeAssistant, callback
from homeassistant.helpers.entity import DeviceInfo
from homeassistant.helpers.update_coordinator import CoordinatorEntity

from .const import DOMAIN
from .coordinator import RckCoordinator


@dataclass(frozen=True)
class SensorDescription:
    key: str
    name: str
    unit: str
    device_class: SensorDeviceClass | None
    state_class: SensorStateClass | None


SENSORS = (
    SensorDescription("total_power_w", "Total power", UnitOfPower.WATT,
                      SensorDeviceClass.POWER, SensorStateClass.MEASUREMENT),
    SensorDescription("total_energy_kwh", "Reported energy", UnitOfEnergy.KILO_WATT_HOUR,
                      SensorDeviceClass.ENERGY, SensorStateClass.TOTAL_INCREASING),
    SensorDescription("max_temperature_c", "Maximum temperature", UnitOfTemperature.CELSIUS,
                      SensorDeviceClass.TEMPERATURE, SensorStateClass.MEASUREMENT),
)


async def async_setup_entry(hass: HomeAssistant, entry: ConfigEntry, async_add_entities):
    coordinator: RckCoordinator = hass.data[DOMAIN][entry.entry_id]
    known: set[str] = set()

    @callback
    def add_new_devices() -> None:
        entities = []
        for device in coordinator.data or []:
            mac = str(device.get("mac", ""))
            if not mac:
                continue
            for description in SENSORS:
                unique = f"{mac}_{description.key}"
                if unique in known:
                    continue
                known.add(unique)
                entities.append(RckSensor(coordinator, mac, description))
        if entities:
            async_add_entities(entities)

    add_new_devices()
    entry.async_on_unload(coordinator.async_add_listener(add_new_devices))


class RckSensor(CoordinatorEntity[RckCoordinator], SensorEntity):
    _attr_has_entity_name = True

    def __init__(self, coordinator: RckCoordinator, mac: str,
                 description: SensorDescription) -> None:
        super().__init__(coordinator)
        self._mac = mac
        self._description = description
        self._attr_unique_id = f"{mac}_{description.key}"
        self._attr_name = description.name
        self._attr_native_unit_of_measurement = description.unit
        self._attr_device_class = description.device_class
        self._attr_state_class = description.state_class

    @property
    def device_info(self) -> DeviceInfo:
        device = self.coordinator.device(self._mac) or {}
        return DeviceInfo(
            identifiers={(DOMAIN, self._mac)},
            name=device.get("name") or f"MTTL-W01 {self._mac}",
            manufacturer="FG Machines / compatible MTTL",
            model="MTTL-W01",
            sw_version=device.get("firmware") or None,
            suggested_area=device.get("room") or None,
        )

    @property
    def available(self) -> bool:
        device = self.coordinator.device(self._mac)
        return bool(device and device.get("connected"))

    @property
    def native_value(self):
        device = self.coordinator.device(self._mac) or {}
        telemetry = device.get("telemetry") or {}
        return telemetry.get(self._description.key)
