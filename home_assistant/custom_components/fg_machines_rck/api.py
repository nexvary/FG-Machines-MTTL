from __future__ import annotations

from typing import Any

from aiohttp import ClientSession


class RckApiError(Exception):
    pass


class RckApi:
    def __init__(self, session: ClientSession, base_url: str, token: str) -> None:
        self._session = session
        self._base_url = base_url.rstrip("/")
        self._token = token

    @property
    def headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self._token}", "Accept": "application/json"}

    async def health(self) -> dict[str, Any]:
        return await self._request("GET", "/api/v1/health", authenticated=False)

    async def devices(self) -> list[dict[str, Any]]:
        data = await self._request("GET", "/api/v1/devices")
        return list(data.get("devices", []))

    async def set_outlet(self, mac: str, outlet: int, on: bool) -> None:
        state = "on" if on else "off"
        await self._request(
            "POST", f"/api/v1/devices/{mac}/outlets/{outlet}?state={state}"
        )

    async def history(self, mac: str, hours: int = 24) -> dict[str, Any]:
        return await self._request("GET", f"/api/v1/history/{mac}?hours={hours}")

    async def _request(
        self, method: str, path: str, authenticated: bool = True
    ) -> dict[str, Any]:
        headers = self.headers if authenticated else {"Accept": "application/json"}
        try:
            async with self._session.request(
                method, self._base_url + path, headers=headers, timeout=8
            ) as response:
                text = await response.text()
                if response.status < 200 or response.status >= 300:
                    raise RckApiError(f"HTTP {response.status}: {text}")
                return await response.json()
        except RckApiError:
            raise
        except Exception as err:
            raise RckApiError(str(err)) from err
