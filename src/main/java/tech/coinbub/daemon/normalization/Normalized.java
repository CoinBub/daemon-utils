package tech.coinbub.daemon.normalization;

import tech.coinbub.daemon.normalization.model.Block;
import tech.coinbub.daemon.normalization.model.Transaction;
import java.math.BigDecimal;

public interface Normalized<T> {
    String getSymbol();
    String getblockhash(final Long height);
    Block getblock(final String hash);
    Transaction gettransaction(final String txid);
    String getnewaddress();
    String sendtoaddress(final String address, final BigDecimal amount);
    String sendtoaddress(final String address, final BigDecimal amount, final String comment_from);
    String sendtoaddress(final String address, final BigDecimal amount, final String comment_from, final String comment_to);
}
