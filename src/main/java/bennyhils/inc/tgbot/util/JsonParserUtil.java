package bennyhils.inc.tgbot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Properties;

@Slf4j
public class JsonParserUtil {

    public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    public static List<String> getStringArray(String propertyName, Properties properties) {
        try {

            return OBJECT_MAPPER.readValue(
                    properties.getProperty(propertyName),
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException e) {
            log.error("Не удалось получить список свойств '{}' с ошибкой: '{}'", propertyName, e.getMessage());

            return null;
        }
    }
}
