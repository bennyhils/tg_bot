package bennyhils.inc.tgbot.model.receipt;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Receipt {
    public String email;
    public List<Item> items;

    public Receipt(String email, List<Item> items) {
        this.email = email;
        this.items = items;
    }
}
