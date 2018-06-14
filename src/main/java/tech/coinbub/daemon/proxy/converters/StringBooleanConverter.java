package tech.coinbub.daemon.proxy.converters;

import tech.coinbub.daemon.proxy.Converter;

public class StringBooleanConverter implements Converter {

    @Override
    public Object marshal(final Object from) {
        return from.toString();
    }

    @Override
    public Object unmarshal(final Object from) {
        return Boolean.parseBoolean(from.toString());
    }
    
}
