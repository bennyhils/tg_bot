package bennyhils.inc.tgbot.model.receipt;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Amount {
    public String value;
    public String currency;

    public Amount(String value, String currency) {
        this.value = value;
        this.currency = currency;
    }
}
