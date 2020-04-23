package com.waicool20.cvauto.util.wrapper

import boofcv.abst.feature.associate.AssociateDescription
import boofcv.abst.feature.associate.ScoreAssociation
import boofcv.factory.feature.associate.FactoryAssociation
import boofcv.struct.feature.TupleDesc

/**
 * Wrapper for [KFactoryAssociation]
 */
object KFactoryAssociation {
    /**
     * @see [FactoryAssociation.scoreEuclidean]
     */
    inline fun <reified T : TupleDesc<in T>> scoreEuclidean(squared: Boolean): ScoreAssociation<T> {
        return FactoryAssociation.scoreEuclidean(T::class.java, squared)
    }

    /**
     * @see [FactoryAssociation.greedy]
     */
    inline fun <reified T : TupleDesc<in T>> greedy(
        score: ScoreAssociation<T>,
        maxError: Double,
        backwardsValidation: Boolean
    ): AssociateDescription<T> {
        return FactoryAssociation.greedy(score, maxError, backwardsValidation)
    }
}