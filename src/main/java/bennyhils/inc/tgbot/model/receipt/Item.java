package bennyhils.inc.tgbot.model.receipt;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Item {
    public String description;
    public String quantity;
    public Amount amount;
    public int vat_code;

    public Item(String description, String quantity, Amount amount, int vat_code) {
        this.description = description;
        this.quantity = quantity;
        this.amount = amount;
        this.vat_code = vat_code;
    }
}
