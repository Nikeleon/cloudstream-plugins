package com.quyen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class JavHDZPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(JavHDZProvider())
    }
}
