package com.chichikolon.xrunning.run.util;

import android.speech.tts.TextToSpeech;

public class TTSUtils {

    private TTSUtils() {
    }

    public static int getTTSStream() {
        return TextToSpeech.Engine.DEFAULT_STREAM;
    }
}
