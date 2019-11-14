package com.waicool20.cvauto.util

import com.waicool20.cvauto.core.Millis
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object Animations {
    class AnimationSequence(
        private val steps: Long,
        private val sequence: Sequence<Double>
    ) : Sequence<Double> {
        override fun iterator() = sequence.iterator()
        fun timed(duration: Millis) = TimedAnimationSequence(steps, duration, sequence)
    }

    private fun AnimationSequence(steps: Long, block: suspend SequenceScope<Double>.() -> Unit) =
        AnimationSequence(steps, sequence(block))

    class TimedAnimationSequence(
        steps: Long,
        duration: Millis,
        sequence: Sequence<Double>
    ) : Sequence<Double> {
        private val iterator = sequence.iterator()
        private val sleepPerStep = duration / steps
        override fun iterator(): Iterator<Double> = object : Iterator<Double> by iterator {
            override fun next(): Double {
                TimeUnit.MILLISECONDS.sleep(sleepPerStep)
                return iterator.next()
            }
        }
    }

    fun Linear(start: Int, final: Int, steps: Long) = Linear(start.toDouble(), final.toDouble(), steps)
    fun Linear(start: Long, final: Long, steps: Long) = Linear(start.toDouble(), final.toDouble(), steps)
    fun Linear(start: Double, final: Double, steps: Long) = AnimationSequence(steps) {
        for (step in 0 until steps) yield(final * step / steps + start)
    }

    fun EaseInQuad(start: Int, final: Int, steps: Long) = EaseInQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseInQuad(start: Long, final: Long, steps: Long) = EaseInQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseInQuad(start: Double, final: Double, steps: Long) = AnimationSequence(steps) {
        for (step in 0 until steps) {
            yield(final * (step.toDouble() / steps).pow(2) + start)
        }
    }

    fun EaseOutQuad(start: Int, final: Int, steps: Long) = EaseOutQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseOutQuad(start: Long, final: Long, steps: Long) = EaseOutQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseOutQuad(start: Double, final: Double, steps: Long) = AnimationSequence(steps) {
        var _step: Double
        for (step in 0 until steps) {
            _step = step.toDouble() / steps
            yield(-final * _step * (_step - 2) + start)
        }
    }

    fun EaseInOutQuad(start: Int, final: Int, steps: Long) = EaseInOutQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseInOutQuad(start: Long, final: Long, steps: Long) = EaseInOutQuad(start.toDouble(), final.toDouble(), steps)
    fun EaseInOutQuad(start: Double, final: Double, steps: Long) = AnimationSequence(steps) {
        var _step: Double
        for (step in 0 until steps) {
            _step = step * 2.0 / steps
            if (_step < 1) {
                yield(final / 2 * _step.pow(2) + start)
            } else {
                _step--
                yield(-final / 2 * (_step * (_step - 2) - 1) + start)
            }
        }
    }
}