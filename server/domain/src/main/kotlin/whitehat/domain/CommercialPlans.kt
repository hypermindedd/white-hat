package whitehat.domain

import java.math.BigInteger

/** Planning assumptions only. Payment events, not this catalog, must grant paid access. */
data class CommercialPlan(val id: String, val seats: Int?, val monthlyAttempts: Int?)
data class PlanEstimate(val plan: CommercialPlan, val estimatedCostMicros: String?, val revenueFloorMicros: String?)

object CommercialPlans {
    const val version = 1
    val proposals = listOf(
        CommercialPlan("solo", 1, 100),
        CommercialPlan("team", 5, 500),
        CommercialPlan("business", 25, 2000),
        CommercialPlan("enterprise", null, null),
    )

    fun estimate(unitCostMicros: Long, fixedCostMicros: Long, marginBps: Int): List<PlanEstimate> {
        if (unitCostMicros < 0 || fixedCostMicros < 0 || marginBps !in 0..9500) throw Fault(400, "INVALID_PLAN_ASSUMPTION")
        val denominator = BigInteger.valueOf((10000 - marginBps).toLong())
        return proposals.map { plan ->
            if (plan.monthlyAttempts == null) PlanEstimate(plan, null, null)
            else {
                val cost = BigInteger.valueOf(unitCostMicros).multiply(BigInteger.valueOf(plan.monthlyAttempts.toLong()))
                    .add(BigInteger.valueOf(fixedCostMicros))
                // Round UP in integer micro-USD; never underprice through floating-point rounding.
                val numerator = cost.multiply(BigInteger.valueOf(10000))
                val floor = numerator.add(denominator).subtract(BigInteger.ONE).divide(denominator)
                PlanEstimate(plan, cost.toString(), floor.toString())
            }
        }
    }
}
