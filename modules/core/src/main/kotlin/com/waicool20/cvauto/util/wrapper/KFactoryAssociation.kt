package com.waicool20.cvauto.util.wrapper

import boofcv.abst.feature.associate.AssociateDescription
import boofcv.abst.feature.associate.ScoreAssociation
import boofcv.abst.feature.associate.WrapAssociateGreedy
import boofcv.alg.feature.associate.AssociateGreedy
import boofcv.alg.feature.associate.AssociateGreedyBase
import boofcv.alg.feature.associate.AssociateGreedy_MT
import boofcv.concurrency.BoofConcurrency
import boofcv.factory.feature.associate.FactoryAssociation
import boofcv.struct.feature.TupleDesc


object KFactoryAssociation {
    inline fun <reified T: TupleDesc<in T>> scoreEuclidean(squared: Boolean): ScoreAssociation<T> {
        return FactoryAssociation.scoreEuclidean(T::class.java, squared)
    }

    inline fun <reified T: TupleDesc<in T>> greedy(
        score: ScoreAssociation<T>,
        maxError: Double,
        backwardsValidation: Boolean
    ): AssociateDescription<T> {
        return FactoryAssociation.greedy(score, maxError, backwardsValidation)
    }
}