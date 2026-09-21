package com.freex.vpn

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerJsonContractTest {
    @Test
    fun parsesPublicVpnListShape() {
        val json = JSONObject("""{\"data\":[{\"id\":\"a\",\"country_code\":\"NL\",\"country_name\":\"Netherlands\",\"city\":\"Amsterdam\",\"hostname\":\"vpn.example\",\"port\":443,\"protocol\":\"openvpn\",\"availability_status\":\"online\",\"config_download_url\":\"https://example.com/a.ovpn\"}]}""")
        val server = json.getJSONArray("data").getJSONObject(0)
        assertEquals("openvpn", server.getString("protocol"))
        assertEquals(443, server.getInt("port"))
    }
}
