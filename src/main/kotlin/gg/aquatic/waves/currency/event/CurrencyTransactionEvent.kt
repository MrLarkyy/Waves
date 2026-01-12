package gg.aquatic.waves.currency.event

import gg.aquatic.waves.currency.impl.RegisteredCurrency
import java.math.BigDecimal
import java.util.UUID

data class CurrencyTransactionEvent(
    val uuid: UUID,
    val currency: RegisteredCurrency,
    val oldBalance: BigDecimal,
    val newBalance: BigDecimal,
    val change: BigDecimal
)
