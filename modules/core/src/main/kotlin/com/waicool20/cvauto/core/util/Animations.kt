/*
 * The MIT License (MIT)
 *
 * Copyright (c) waicool20
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.waicool20.cvauto.core.util

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
        private val steps: Long,
        private val duration: Millis,
        sequence: Sequence<Double>
    ) : Sequence<Double> {
        private val values = sequence.toList()

        override fun iterator(): Iterator<Double> = object : Iterator<Double> {
            private val sleepPerStep = (duration / steps).coerceAtLeast(1)
            private var lastTime = -1L
            private var currentStep = 0

            override fun next(): Double {
                val now = System.currentTimeMillis()
                if (lastTime == -1L) lastTime = now
                val dif = now - lastTime
                lastTime = now
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
