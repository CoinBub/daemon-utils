package tech.coinbub.daemon.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class YesNoBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(final JsonParser parser,
            final DeserializationContext context) throws IOException, JsonProcessingException {
        return "yes".equals(parser.getText());
    }
    
}
