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

    private fun AnimationSequence(steps: Long, block: suspend SequenceScope<Double>.() -> Unit) =
        AnimationSequence(steps, sequence(block))

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

    fun Linear(start: Int, final: Int, steps: Long) = Linear(start.toDouble(), final.toDouble(), steps)
    fun Linear(start: Long, final: Long, steps: Long) = Linear(start.toDouble(), final.toDouble(), steps)
    fun Linear(start: Double, final: Double, steps: Long) = AnimationSequence(steps) {
        for (step in 0 until steps) yield((final - start) * step / steps + start)
    }

    fun EaseInQuad(start: Int, final: Int, steps: Long) = EaseInQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseInQuad(start: Long, final: Long, steps: Long) = EaseInQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseInQuad(start: Double, final: Double, steps: Long) = AnimationSequence(steps) {
        for (step in 0 until steps) {
            yield((final - start) * (step.toDouble() / steps).pow(2) + start)
        }
    }

    fun EaseOutQuad(start: Int, final: Int, steps: Long) = EaseOutQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseOutQuad(start: Long, final: Long, steps: Long) = EaseOutQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseOutQuad(start: Double, final: Double, steps: Long) = AnimationSequence(steps) {
        var _step: Double
        for (step in 0 until steps) {
            _step = step.toDouble() / steps
            yield(-(final - start) * _step * (_step - 2) + start)
        }
    }

    fun EaseInOutQuad(start: Int, final: Int, steps: Long) = EaseInOutQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseInOutQuad(start: Long, final: Long, steps: Long) = EaseInOutQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseInOutQuad(start: Double, final: Double, steps: Long) = AnimationSequence(steps) {
        var _step: Double
        for (step in 0 until steps) {
            _step = step * 2.0 / steps
            if (_step < 1) {
                yield((final - start) / 2 * _step.pow(2) + start)
            } else {
                _step--
                yield(-(final - start) / 2 * (_step * (_step - 2) - 1) + start)
            }
        }
    }
}