package tech.coinbub.daemon.normalization.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction {
    public String id;
    public BigDecimal amount;
    public BigDecimal fee;
    public Long time;
    public Long size;
    public Long confirmations;
    public String blockhash;
    public String comment_from;
    public String comment_to;
    public List<TransactionDetail> details;
}
