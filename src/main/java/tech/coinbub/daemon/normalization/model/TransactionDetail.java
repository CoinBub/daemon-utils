package tech.coinbub.daemon.normalization.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDetail {
    public String address;
    public BigDecimal amount;
    public BigDecimal fee;
}
