package com.finance.token.config

import io.micrometer.core.instrument.MeterRegistry
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory

@ApplicationScoped
class MetricsConfig @Inject constructor(registry: MeterRegistry) {
    init {
        // The presence of this bean will let Camel attach micrometer route policies automatically
        MicrometerRoutePolicyFactory().also { it.meterRegistry = registry }
    }
}

