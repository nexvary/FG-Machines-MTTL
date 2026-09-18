from __future__ import annotations

from homeassistant.components.switch import SwitchEntity
from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant
from homeassistant.helpers.entity import DeviceInfo
from homeassistant.helpers.update_coordinator import CoordinatorEntity

from .const import DOMAIN
from .coordinator import RckCoordinator


async def async_setup_entry(hass: HomeAssistant, entry: ConfigEntry, async_add_entities):
    coordinator: RckCoordinator = hass.data[DOMAIN][entry.entry_id]
    entities = []
    for device in coordinator.data or []:
        mac = str(device.get("mac", ""))
        if not mac:
            continue
        for outlet in range(1, 5):
            entities.append(RckOutletSwitch(coordinator, mac, outlet))
    async_add_entities(entities)


class RckOutletSwitch(CoordinatorEntity[RckCoordinator], SwitchEntity):
    _attr_has_entity_name = True

    def __init__(self, coordinator: RckCoordinator, mac: str, outlet: int) -> None:
        super().__init__(coordinator)
        self._mac = mac
        self._outlet = outlet
        self._attr_unique_id = f"{mac}_outlet_{outlet}"
        self._attr_name = f"Outlet {outlet}"

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
    def is_on(self) -> bool | None:
        device = self.coordinator.device(self._mac) or {}
        telemetry = device.get("telemetry") or {}
        for outlet in telemetry.get("outlets", []):
            if int(outlet.get("channel", 0)) == self._outlet:
                return bool(outlet.get("on"))
        return None

    async def async_turn_on(self, **kwargs) -> None:
        await self.coordinator.api.set_outlet(self._mac, self._outlet, True)
        await self.coordinator.async_request_refresh()

    async def async_turn_off(self, **kwargs) -> None:
        await self.coordinator.api.set_outlet(self._mac, self._outlet, False)
        await self.coordinator.async_request_refresh()
