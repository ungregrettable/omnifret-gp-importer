/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 omnifret-gplayer contributors.
 */

package com.omnifret.gplayer.api

import com.omnifret.gplayer.Settings
import com.omnifret.gplayer.midi.ControllerType
import com.omnifret.gplayer.midi.IMidiFileHandler
import com.omnifret.gplayer.midi.MidiFileGenerator
import com.omnifret.gplayer.midi.MidiTickLookup
import com.omnifret.gplayer.model.Score

// OmniFret's playback integration point. alphaTab walks the score with
// MidiFileGenerator, calling our IMidiFileHandler shim once per event.
// We forward the events to a consumer-supplied PlaybackEventListener,
// which OmniFret implements to drive its existing AudioDsp / AudioEngine
// (Karplus-Strong on Android, AVAudioEngine on iOS).
//
// We don't expose alphaTab's GPlayerSynth/SoundFont/MidiFileSequencer; the
// tick-based event stream is the entire contract.

/**
 * Listener for playback events emitted by [PlaybackBuilder]. All `tick`
 * arguments are in MIDI ticks (parts per quarter note); convert with
 * [tickToSeconds] using the most recent tempo.
 */
public interface PlaybackEventListener {
    /** A new time signature applies starting at [tick]. */
    public fun onTimeSignature(tick: Double, numerator: Int, denominator: Int)

    /** Tempo change (BPM, quarter-note basis) at [tick]. */
    public fun onTempo(tick: Double, bpm: Double)

    /**
     * A note event. [startTick] is the absolute on-time, [lengthTicks]
     * the duration. [midiKey] is 0–127. [velocity] is 0–127.
     */
    public fun onNote(
        track: Int,
        channel: Int,
        startTick: Double,
        lengthTicks: Double,
        midiKey: Int,
        velocity: Int,
    )

    /** Program change (instrument selection on a channel). */
    public fun onProgramChange(track: Int, tick: Double, channel: Int, program: Int) {}

    /** Pitch bend on a channel. [value] is 0–16383, 8192 = no bend. */
    public fun onBend(track: Int, tick: Double, channel: Int, value: Int) {}

    /** Per-note pitch bend (overrides channel bend for this key). */
    public fun onNoteBend(
        track: Int,
        tick: Double,
        channel: Int,
        midiKey: Int,
        value: Int,
    ) {
    }

    /** MIDI controller change (volume, pan, modulation, etc.). */
    public fun onControlChange(
        track: Int,
        tick: Double,
        channel: Int,
        controller: Int,
        value: Int,
    ) {
    }

    /** A rest "event" — useful for cursor sync. */
    public fun onRest(track: Int, tick: Double, channel: Int) {}

    /** Track [track] has no more events past [tick]. */
    public fun onTrackEnd(track: Int, tick: Double) {}
}

/**
 * Helper to convert MIDI ticks to elapsed seconds at a constant tempo.
 * For variable-tempo scores, accumulate this segment-by-segment across
 * each [PlaybackEventListener.onTempo] event.
 */
public fun tickToSeconds(ticks: Double, bpm: Double, ppq: Int = 960): Double {
    if (bpm <= 0.0) return 0.0
    return (ticks / ppq) * (60.0 / bpm)
}

/**
 * Walks a [Score] and forwards every MIDI event to [listener]. Returns
 * a [MidiTickLookup] that the caller can use to map ticks ↔ bars/beats
 * for cursor sync.
 *
 * Generation is synchronous and deterministic. Call from a background
 * dispatcher if you don't want to block the UI thread for large scores.
 */
@OptIn(kotlin.contracts.ExperimentalContracts::class, kotlin.ExperimentalUnsignedTypes::class)
public class PlaybackBuilder(
    private val score: Score,
    private val settings: Settings? = null,
) {
    public fun generate(listener: PlaybackEventListener): MidiTickLookup {
        val handler = ListenerHandler(listener)
        val gen = MidiFileGenerator(score, settings, handler)
        gen.generate()
        return gen.tickLookup
    }
}

@OptIn(kotlin.contracts.ExperimentalContracts::class, kotlin.ExperimentalUnsignedTypes::class)
private class ListenerHandler(
    private val l: PlaybackEventListener,
) : IMidiFileHandler {
    override fun addTimeSignature(
        tick: Double,
        timeSignatureNumerator: Double,
        timeSignatureDenominator: Double,
    ) {
        l.onTimeSignature(tick, timeSignatureNumerator.toInt(), timeSignatureDenominator.toInt())
    }
    override fun addRest(track: Double, tick: Double, channel: Double) {
        l.onRest(track.toInt(), tick, channel.toInt())
    }
    override fun addNote(
        track: Double,
        start: Double,
        length: Double,
        key: Double,
        velocity: Double,
        channel: Double,
    ) {
        l.onNote(
            track.toInt(),
            channel.toInt(),
            start,
            length,
            key.toInt(),
            velocity.toInt(),
        )
    }
    override fun addControlChange(
        track: Double,
        tick: Double,
        channel: Double,
        controller: ControllerType,
        value: Double,
    ) {
        l.onControlChange(track.toInt(), tick, channel.toInt(), controller.value, value.toInt())
    }
    override fun addProgramChange(track: Double, tick: Double, channel: Double, program: Double) {
        l.onProgramChange(track.toInt(), tick, channel.toInt(), program.toInt())
    }
    override fun addTempo(tick: Double, tempo: Double) {
        l.onTempo(tick, tempo)
    }
    override fun addNoteBend(
        track: Double,
        tick: Double,
        channel: Double,
        key: Double,
        value: Double,
    ) {
        l.onNoteBend(track.toInt(), tick, channel.toInt(), key.toInt(), value.toInt())
    }
    override fun addBend(track: Double, tick: Double, channel: Double, value: Double) {
        l.onBend(track.toInt(), tick, channel.toInt(), value.toInt())
    }
    override fun finishTrack(track: Double, tick: Double) {
        l.onTrackEnd(track.toInt(), tick)
    }
    override fun addTickShift(@Suppress("UNUSED_PARAMETER") tickShift: Double) {
        // alphaTab applies grace-beat tick shifts to all subsequent
        // events. The IMidiFileHandler API expects us to track the
        // shift internally — but our listener consumers receive the
        // absolute tick already shifted by the generator, so this is a
        // no-op for the OmniFret API shape.
    }
}
