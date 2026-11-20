package com.ballgame.pro;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;

/**
 * Générateur de sons 100 % Java 21, sans aucune bibliothèque externe.
 * Les sons créés sont des compositions/synthèses originales pour BallGame Pro V2.
 *
 * Depuis le dossier du projet :
 *   java src/main/java/com/ballgame/pro/AudioGenerator.java
 * Ou après compilation :
 *   java -cp dist/BallGame-Pro-V2.jar com.ballgame.pro.AudioGenerator
 *
 * Pour choisir un autre dossier de sortie : ajouter son chemin en argument.
 */
public final class AudioGenerator {
    private static final int SAMPLE_RATE = 22_050;
    private static final double TAU = Math.PI * 2;
    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 16, 1, 2, SAMPLE_RATE, false);

    private AudioGenerator() { }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage : java ... AudioGenerator [dossier_audio]");
            System.exit(2);
        }
        Path destination = args.length == 1
                ? Paths.get(args[0])
                : Paths.get("src", "main", "resources", "audio");
        Files.createDirectories(destination);

        generate(destination, "bounce", 0.13, gliss(460, 780, 0.13, 0.6), 0.56);
        generate(destination, "ui", 0.085, gliss(350, 540, 0.085, 0.5), 0.56);
        generate(destination, "start", 0.45, t ->
                (tone(t, 440, false) + 0.5 * tone(t, 660, false) + 0.4 * tone(t, 880, false))
                        * 0.43 * fade(t, 0.45, 0.017, 0.14), 0.56);
        generate(destination, "power", 0.62, t ->
                (tone(t, 610 + t * 730, false) + 0.32 * tone(t, 915 + t * 600, false))
                        * 0.5 * Math.exp(-2.7 * t) * fade(t, 0.62, 0.015, 0.1), 0.56);
        generate(destination, "hurt", 0.5, gliss(320, 92, 0.5, 0.7), 0.56);
        generate(destination, "gameover", 1.0, t ->
                (0.32 * tone(t, 330, false) + 0.35 * tone(t, 277.18, false)
                        + 0.32 * tone(t, 220, false))
                        * Math.exp(-2.3 * t) * fade(t, 1.0, 0.01, 0.08), 0.56);
        generate(destination, "music", 8.0, AudioGenerator::music, 0.73);

        System.out.println("7 fichiers audio WAV générés en Java 21 dans : "
                + destination.toAbsolutePath().normalize());
    }

    private static void generate(Path directory, String name, double seconds,
                                 DoubleUnaryOperator waveform, double volume) throws IOException {
        int samples = (int) (SAMPLE_RATE * seconds);
        byte[] pcm = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double value = Math.max(-1.0, Math.min(1.0,
                    waveform.applyAsDouble((double) i / SAMPLE_RATE) * volume));
            short sample = (short) (int) (value * Short.MAX_VALUE);
            pcm[i * 2] = (byte) sample;
            pcm[i * 2 + 1] = (byte) (sample >> 8);
        }
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(pcm), FORMAT, samples)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE,
                    directory.resolve(name + ".wav").toFile());
        }
        System.out.printf(Locale.ROOT, "  %-14s (%d échantillons)%n", name + ".wav", samples);
    }

    private static double tone(double t, double frequency, boolean triangle) {
        double phase = TAU * t * frequency;
        return triangle ? 2 / Math.PI * Math.asin(Math.sin(phase)) : Math.sin(phase);
    }

    private static double fade(double t, double duration, double attack, double release) {
        return Math.min(1.0, Math.min(t / Math.max(attack, 0.00001),
                Math.max(0, duration - t) / Math.max(release, 0.00001)));
    }

    private static DoubleUnaryOperator gliss(double startHz, double endHz,
                                             double duration, double weight) {
        return t -> {
            double frequency = startHz + (endHz - startHz) * (t / duration);
            return (tone(t, frequency, false) + 0.2 * tone(t, 2 * frequency, false))
                    * Math.exp(-t * 5 / duration) * fade(t, duration, 0.001, 0.025) * weight;
        };
    }

    private static double music(double t) {
        double[][] chords = {
                {220.00, 261.63, 329.63},
                {174.61, 220.00, 261.63},
                {196.00, 246.94, 293.66},
                {164.81, 196.00, 246.94}
        };
        double[] melody = {
                440, 523.25, 659.25, 523.25, 349.23, 440, 523.25, 440,
                392, 493.88, 587.33, 493.88, 329.63, 392, 493.88, 392
        };
        double[] chord = chords[Math.min(3, (int) (t / 2))];
        double pad = (tone(t, chord[0], true) + tone(t, chord[1], true)
                + tone(t, chord[2], true)) / 3 * 0.23;
        int step = (int) (t / 0.5) % melody.length;
        double noteAge = t % 0.5;
        double note = tone(noteAge, melody[step], true) * (1 - noteAge / 0.55) * 0.23;
        double bassAge = t % 0.5;
        double bass = tone(bassAge, chord[0] / 2, false) * Math.exp(-bassAge * 3) * 0.27;
        double beat = t % 0.5;
        double kick = Math.sin(TAU * (65 * beat + 27 * Math.exp(-22 * beat) * beat))
                * Math.exp(-beat * 28) * 0.24;
        double hi = t % 0.25;
        double hat = Math.sin(hi * 19915) * Math.sin(hi * 14523) * Math.exp(-hi * 65) * 0.055;
        double edge = Math.min(1, Math.min(t / 0.04, (8 - t) / 0.04));
        return (pad + note + bass + kick + hat) * Math.max(0, edge);
    }
}
