package ma.darija.voicebot.tts;

import ma.darija.voicebot.BotConfig;
import ma.darija.voicebot.audio.Mp3Decoder;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** Picks the voice service, cleans the text and returns Discord-ready PCM. */
public class TextToSpeech {

    // Emojis, markdown symbols and links don't sound good when read out loud
    private static final Pattern EMOJI = Pattern.compile("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}\\x{200D}]");
    private static final Pattern MARKDOWN = Pattern.compile("[*_#`~>|]+");
    private static final Pattern LINK = Pattern.compile("https?://\\S+");

    private final BotConfig cfg;
    private final Logger log;
    private final EdgeTts edge;
    private final GoogleTts google = new GoogleTts();

    public TextToSpeech(BotConfig cfg, Logger log) {
        this.cfg = cfg;
        this.log = log;
        this.edge = new EdgeTts(cfg.ttsVoice(), cfg.ttsRate(), cfg.ttsPitch());
    }

    public static String clean(String text) {
        String t = LINK.matcher(text).replaceAll("");
        t = EMOJI.matcher(t).replaceAll("");
        t = MARKDOWN.matcher(t).replaceAll("");
        return t.replaceAll("\\s+", " ").trim();
    }

    /** Returns 48 kHz stereo PCM ready for Discord, or an empty array if there is nothing to say. */
    public byte[] speak(String text) throws IOException, InterruptedException {
        String clean = clean(text);
        if (clean.isEmpty()) return new byte[0];
        byte[] mp3;
        if (cfg.ttsProvider().equals("google")) {
            mp3 = google.synthesize(clean);
        } else {
            try {
                mp3 = edge.synthesize(clean);
            } catch (IOException e) {
                if (!cfg.ttsFallbackGoogle()) throw e;
                log.warning("Edge voice failed, using Google voice instead: " + e.getMessage());
                mp3 = google.synthesize(clean);
            }
        }
        return Mp3Decoder.decodeToDiscordPcm(mp3);
    }
}
