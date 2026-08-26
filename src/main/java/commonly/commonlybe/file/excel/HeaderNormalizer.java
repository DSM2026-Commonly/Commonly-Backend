package commonly.commonlybe.file.excel;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class HeaderNormalizer {

    private static final Pattern PARENTHESES = Pattern.compile("\\([^)]*\\)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private HeaderNormalizer() {
    }

    public static String normalize(String raw) {
        String noNewline = raw.replace("\n", "").replace("\r", "");
        String noParentheses = PARENTHESES.matcher(noNewline).replaceAll("");
        String noWhitespace = WHITESPACE.matcher(noParentheses).replaceAll("");
        return Normalizer.normalize(noWhitespace, Normalizer.Form.NFC);
    }
}
