package com.waicool20.cvauto.util

import com.waicool20.cvauto.core.Millis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object Animations {
    class AnimationSequence(
        private val steps: Long,
        private val sequence: Sequence<Double>
    ) : Sequence<Double> {
        override fun iterator() = sequence.iterator()

        fun timed(duration: Millis): Sequence<Double> {
            val _steps = steps.coerceAtMost(duration)
            return TimedAnimationSequence(_steps, duration, sequence)
        }

        fun timedFlow(duration: Millis): Flow<Double> {
            val _steps = steps.coerceAtMost(duration)
            return asFlow().onEach { delay(duration / _steps) }.conflate()
        }
    }

    class TimedAnimationSequence(
        steps: Long,
        duration: Millis,
        sequence: Sequence<Double>
    ) : Sequence<Double> {
        private val values = sequence.toList()
        private val sleepPerStep = duration / steps
        private var lastTime = -1L
        private var currentStep = 0

        override fun iterator(): Iterator<Double> = object : Iterator<Double> {
            override fun next(): Double {
                if (lastTime == -1L) {
                    lastTime = System.currentTimeMillis()
                    TimeUnit.MILLISECONDS.sleep(sleepPerStep)
                    return values[currentStep++]
                }
                val dif = System.currentTimeMillis() - lastTime
                lastTime = System.currentTimeMillis()
                return if (dif < sleepPerStep) {
                    TimeUnit.MILLISECONDS.sleep(sleepPerStep - dif)
                    values[currentStep++]
                } else {
                    currentStep += (dif / sleepPerStep).toInt()
                    currentStep = currentStep.coerceAtMost(values.lastIndex)
                    values[currentStep]
                }
            }

            override fun hasNext(): Boolean {
                return currentStep < values.lastIndex
            }
        }
    }

    private fun AnimationSequence(steps: Long, block: suspend SequenceScope<Double>.() -> Unit) =
        AnimationSequence(steps, sequence(block))

    fun Linear(steps: Long) = AnimationSequence(steps) {
        for (step in 0 until steps) yield(step / steps.toDouble())
    }

    fun EaseInQuad(steps: Long) = AnimationSequence(steps) {
        for (step in 0 until steps) {
            yield((step / steps.toDouble()).pow(2))
        }
    }

    fun EaseOutQuad(steps: Long) = AnimationSequence(steps) {
        var _step: Double
        for (step in 0 until steps) {
            _step = step / steps.toDouble()
            yield(-_step * (_step - 2))
        }
    }

    fun EaseInOutQuad(steps: Long) = AnimationSequence(steps) {
        var _step: Double
        for (step in 0 until steps) {
            _step = step * 2 / steps.toDouble()
            if (_step < 1) {
                yield(0.5 * _step.pow(2))
            } else {
                _step--
                yield(-0.5 * (_step * (_step - 2) - 1))
            }
        }
    }
}