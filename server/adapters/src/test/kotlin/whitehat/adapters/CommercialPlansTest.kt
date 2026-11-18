package whitehat.adapters

import java.math.BigInteger
import kotlin.test.*
import whitehat.domain.*

class CommercialPlansTest {
    @Test fun proposalsNeverPretendEnterpriseCapacityOrUnknownCostsAreZero() {
        assertEquals(listOf("solo", "team", "business", "enterprise"), CommercialPlans.proposals.map { it.id })
        assertNull(CommercialPlans.proposals.last().monthlyAttempts)
        assertNull(CommercialPlans.estimate(10, 100, 7000).last().revenueFloorMicros)
    }
    @Test fun revenueFloorRoundsUpAndRespectsGrossMarginAtEveryTier() {
        for (margin in listOf(0, 1, 7000, 9500)) {
            for (estimate in CommercialPlans.estimate(Long.MAX_VALUE, Long.MAX_VALUE, margin).dropLast(1)) {
                val cost = BigInteger(estimate.estimatedCostMicros!!)
                val revenue = BigInteger(estimate.revenueFloorMicros!!)
                val keep = BigInteger.valueOf((10000-margin).toLong())
                assertTrue(revenue * keep >= cost * BigInteger.valueOf(10000))
                assertTrue((revenue-BigInteger.ONE) * keep < cost * BigInteger.valueOf(10000))
            }
        }
        assertEquals("334", CommercialPlans.estimate(1, 0, 7000).first().revenueFloorMicros)
        assertEquals("0", CommercialPlans.estimate(0, 0, 9500).first().revenueFloorMicros)
    }
    @Test fun invalidPlanningAssumptionsAreRejected() {
        for ((unit, fixed, margin) in listOf(Triple(-1L, 0L, 7000), Triple(0L, -1L, 7000), Triple(1L, 1L, 10000))) {
            assertEquals("INVALID_PLAN_ASSUMPTION", assertFailsWith<Fault> { CommercialPlans.estimate(unit, fixed, margin) }.code)
        }
    }
}
