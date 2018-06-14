package tech.coinbub.daemon.proxy.converters;

import tech.coinbub.daemon.proxy.Converter;

public class HexConverter implements Converter {

    @Override
    public Object marshal(final Object from) {
        if (from instanceof Integer) {
            return "0x" + Integer.toHexString((Integer) from);
        }
        if (from instanceof Long) {
            return "0x" + Long.toHexString((Long) from);
        }
        return null;
    }

    @Override
    public Object unmarshal(final Object from) {
        return Long.decode(from.toString());
    }
    
}
