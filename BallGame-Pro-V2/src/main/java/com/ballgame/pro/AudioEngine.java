package com.ballgame.pro;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.util.HashMap;
import java.util.Map;

/** Original synthesized WAV assets bundled inside the executable JAR. */
final class AudioEngine implements AutoCloseable {
    private final Map<String, Clip> sounds = new HashMap<>();
    private Clip music;
    private boolean musicEnabled = true;
    private boolean effectsEnabled = true;

    AudioEngine() {
        String[] names = {"bounce", "power", "hurt", "start", "gameover", "ui"};
        for (String name : names) {
            Clip clip = load("/audio/" + name + ".wav");
            if (clip != null) sounds.put(name, clip);
        }
        music = load("/audio/music.wav");
        if (music != null && music.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) music.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(-17f, gain.getMaximum())));
        }
    }

    private Clip load(String file) {
        try {
            var resource = AudioEngine.class.getResourceAsStream(file);
            if (resource == null) return null;
            try (var data = new BufferedInputStream(resource);
                 AudioInputStream stream = AudioSystem.getAudioInputStream(data)) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                return clip;
            }
        } catch (Exception ignored) { return null; } // No audio device, no problem.
    }

    void play(String name) {
        if (!effectsEnabled) return;
        Clip clip = sounds.get(name);
        if (clip == null) return;
        try {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception ignored) { }
    }

    void startMusic() {
        if (!musicEnabled || music == null) return;
        try { if (!music.isRunning()) music.loop(Clip.LOOP_CONTINUOUSLY); }
        catch (Exception ignored) { }
    }

    void setMusicEnabled(boolean value) {
        musicEnabled = value;
        if (music == null) return;
        if (value) startMusic(); else music.stop();
    }

    boolean isMusicEnabled() { return musicEnabled; }
    boolean isEffectsEnabled() { return effectsEnabled; }
    void setEffectsEnabled(boolean value) { effectsEnabled = value; }

    @Override public void close() {
        for (Clip clip : sounds.values()) clip.close();
        if (music != null) music.close();
    }
}
