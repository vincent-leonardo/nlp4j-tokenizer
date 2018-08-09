import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.token.Token;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vincent Leonardo
 * @since 2018-07-28
 */
public class NLP4JTokenizer {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Need to specify file path");
            return;
        }

        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            String message = String.format("Given path '%s' does not exist.", inputFile.toString());
            System.err.println(message);
            return;
        }
        if (!inputFile.isFile()) {
            String message = String.format("Given path '%s' is not a file.", inputFile.toString());
            System.err.println(message);
            return;
        }

        Tokenizer tokenizer = new EnglishTokenizer();

        InputStream is;
        String fullText;
        try {
            is = new FileInputStream(inputFile);
            fullText = Joiner.on("\n").join(Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            String message = String.format("Failed to read contents from the given file.");
            System.err.println(message);
            return;
        }

        List<List<Token>> sentences = tokenizer.segmentize(is);
        List<List<Map<String, Object>>> map = sentences
                .stream()
                .map(l -> l.stream()
                           .filter(token -> StringUtils.isNotBlank(token.getWordForm()))
                           .map(token -> tokenToMap(token, fullText))
                           .collect(Collectors.toList())
                )
                .collect(Collectors.toList());
        try {
            System.out.println(new ObjectMapper().writeValueAsString(map));
        } catch (JsonProcessingException e) {
            System.err.println("Failed to create result");
        }
    }

    private static Map<String, Object> tokenToMap(Token t, String fullTxt) {
        LinkedHashMap<String, Object> map = Maps.newLinkedHashMap();
        map.put("start", t.getStartOffset());
        map.put("end", t.getEndOffset());
        map.put("text", t.getWordForm());
        String docTxt = fullTxt.substring(t.getStartOffset(), t.getEndOffset());
        if (!docTxt.equalsIgnoreCase(t.getWordForm())) { // Fix sometimes miscalculated offset
            String tmpTxt = fullTxt.substring(t.getStartOffset() + 1, t.getEndOffset() + 1);
            if (tmpTxt.equalsIgnoreCase(t.getWordForm())) {
                docTxt = tmpTxt;
                map.put("start", t.getStartOffset() + 1);
                map.put("end", t.getEndOffset() + 1);
            }
        }
        if (!docTxt.equalsIgnoreCase(t.getWordForm())
            && docTxt.replaceAll("\\s", "").equalsIgnoreCase(t.getWordForm())) {
            map.put("text", docTxt);
        }
        return map;
    }
}
