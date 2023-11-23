package bennyhils.inc.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    private String tgId;
    private Integer amount;
    private Long paymentEpochMilli;
}