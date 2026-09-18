from __future__ import annotations

import voluptuous as vol

from homeassistant import config_entries
from homeassistant.core import HomeAssistant
from homeassistant.helpers.aiohttp_client import async_get_clientsession

from .api import RckApi, RckApiError
from .const import CONF_HOST, CONF_TOKEN, DOMAIN


async def _validate(hass: HomeAssistant, host: str, token: str) -> None:
    api = RckApi(async_get_clientsession(hass), host, token)
    await api.health()
    await api.devices()


class ConfigFlow(config_entries.ConfigFlow, domain=DOMAIN):
    VERSION = 1

    async def async_step_user(self, user_input=None):
        errors = {}
        if user_input is not None:
            host = user_input[CONF_HOST].rstrip("/")
            token = user_input[CONF_TOKEN].strip()
            try:
                await _validate(self.hass, host, token)
            except RckApiError:
                errors["base"] = "cannot_connect"
            else:
                await self.async_set_unique_id(host.lower())
                self._abort_if_unique_id_configured()
                return self.async_create_entry(
                    title=f"FG Machines RCK · {host}",
                    data={CONF_HOST: host, CONF_TOKEN: token},
                )

        schema = vol.Schema(
            {
                vol.Required(CONF_HOST, default="http://192.168.1.2:18086"): str,
                vol.Required(CONF_TOKEN): str,
            }
        )
        return self.async_show_form(step_id="user", data_schema=schema, errors=errors)
