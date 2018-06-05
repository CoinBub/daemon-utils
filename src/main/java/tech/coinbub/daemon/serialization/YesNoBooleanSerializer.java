package tech.coinbub.daemon.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class YesNoBooleanSerializer extends JsonSerializer<Boolean> {

    @Override
    public void serialize(final Boolean value,
            final JsonGenerator generator,
            final SerializerProvider provider)
            throws IOException,
            JsonProcessingException {
        if (value == null) {
            return;
        }
        if (value) {
            generator.writeString("yes");
            return;
        }
        generator.writeString("no");
    }
    
}
