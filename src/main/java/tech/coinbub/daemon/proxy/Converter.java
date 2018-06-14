package tech.coinbub.daemon.proxy;

public interface Converter {
    Object marshal(Object from);
    Object unmarshal(Object from);
}
